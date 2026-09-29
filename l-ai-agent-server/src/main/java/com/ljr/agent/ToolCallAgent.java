package com.ljr.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 具备工具调用能力的 ReAct 智能体。
 * 关闭框架内部自动执行工具，由本类显式控制 think / act。
 * 对外步骤文案保持通俗短句，供前端折叠展示。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    public static final String FINAL_PREFIX = "FINAL:";
    private static final Pattern PDF_MARKER = Pattern.compile("<<<PDF:([^>]+)>>>");

    private final ToolCallback[] availableTools;
    private final ChatModel chatModel;
    private final ToolCallingManager toolCallingManager;

    private ChatResponse toolCallChatResponse;
    private Prompt toolCallPrompt;
    private String lastFinalAnswer;
    private final Set<String> generatedPdfs = new LinkedHashSet<>();

    public ToolCallAgent(ToolCallback[] availableTools, ChatModel chatModel, ToolCallingManager toolCallingManager) {
        this.availableTools = availableTools;
        this.chatModel = chatModel;
        this.toolCallingManager = toolCallingManager;
    }

    @Override
    public boolean think() {
        if (StrUtil.isNotBlank(getNextStepPrompt()) && CollUtil.isNotEmpty(getMessageList())) {
            getMessageList().add(new org.springframework.ai.chat.messages.UserMessage(getNextStepPrompt()));
        }
        List<Message> messageList = getMessageList();
        ToolCallingChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(availableTools)
                .internalToolExecutionEnabled(false)
                .build();
        toolCallPrompt = new Prompt(messageList, chatOptions);
        try {
            toolCallChatResponse = chatModel.call(toolCallPrompt);
            AssistantMessage assistantMessage = toolCallChatResponse.getResult().getOutput();
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            String text = assistantMessage.getText();
            log.info("{} 思考: {}", getName(), StrUtil.blankToDefault(text, "(无文本)"));
            log.info("{} 选择工具数: {}", getName(), toolCallList == null ? 0 : toolCallList.size());
            if (CollUtil.isNotEmpty(toolCallList)) {
                String toolNames = toolCallList.stream()
                        .map(AssistantMessage.ToolCall::name)
                        .collect(Collectors.joining(", "));
                log.info("{} 准备调用工具: {}", getName(), toolNames);
            } else if (StrUtil.isNotBlank(text)) {
                lastFinalAnswer = text;
            }
            messageList.add(assistantMessage);
            return CollUtil.isNotEmpty(toolCallList);
        } catch (Exception e) {
            log.error("{} 思考阶段失败", getName(), e);
            getMessageList().add(new AssistantMessage("思考失败: " + e.getMessage()));
            return false;
        }
    }

    @Override
    public String act() {
        if (toolCallChatResponse == null || toolCallPrompt == null) {
            return "这一步没有可执行的操作。";
        }
        AssistantMessage assistantMessage = toolCallChatResponse.getResult().getOutput();
        List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();
        if (CollUtil.isEmpty(toolCalls)) {
            setState(AgentState.FINISHED);
            String text = StrUtil.blankToDefault(assistantMessage.getText(), "任务已完成。");
            lastFinalAnswer = text;
            return FINAL_PREFIX + text;
        }

        boolean terminate = toolCalls.stream().anyMatch(call ->
                "doTerminate".equals(call.name()) || call.name().endsWith("_doTerminate"));

        String plainStep = describeToolCalls(toolCalls);

        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(toolCallPrompt, toolCallChatResponse);
        getMessageList().clear();
        getMessageList().addAll(toolExecutionResult.conversationHistory());

        collectPdfMarkersFromHistory();

        if (terminate) {
            setState(AgentState.FINISHED);
            if (StrUtil.isBlank(lastFinalAnswer)) {
                lastFinalAnswer = StrUtil.blankToDefault(assistantMessage.getText(),
                        "行程相关任务已处理完成，可查看下方结果。");
            }
            String pdfSuffix = generatedPdfs.isEmpty() ? "" :
                    generatedPdfs.stream().map(n -> "<<<PDF:" + n + ">>>").collect(Collectors.joining("\n", "\n", ""));
            return plainStep + (plainStep.endsWith("。") ? "" : "。") + pdfSuffix;
        }

        String pdfSuffix = generatedPdfs.stream()
                .map(n -> "<<<PDF:" + n + ">>>")
                .collect(Collectors.joining("\n", generatedPdfs.isEmpty() ? "" : "\n", ""));
        return plainStep + pdfSuffix;
    }

    public String getLastFinalAnswer() {
        return lastFinalAnswer;
    }

    public Set<String> getGeneratedPdfs() {
        return generatedPdfs;
    }

    private void collectPdfMarkersFromHistory() {
        for (Message msg : getMessageList()) {
            if (!(msg instanceof ToolResponseMessage tr)) {
                continue;
            }
            for (ToolResponseMessage.ToolResponse r : tr.getResponses()) {
                String data = r.responseData();
                if (data == null) {
                    continue;
                }
                Matcher m = PDF_MARKER.matcher(data);
                while (m.find()) {
                    generatedPdfs.add(m.group(1).trim());
                }
            }
        }
    }

    private String describeToolCalls(List<AssistantMessage.ToolCall> toolCalls) {
        LinkedHashSet<String> phrases = new LinkedHashSet<>();
        for (AssistantMessage.ToolCall call : toolCalls) {
            phrases.add(describeOneTool(call.name(), call.arguments()));
        }
        return String.join("；", phrases) + (phrases.isEmpty() ? "继续处理任务。" : "。");
    }

    private String describeOneTool(String name, String arguments) {
        String n = name == null ? "" : name.toLowerCase();
        String args = StrUtil.blankToDefault(arguments, "");
        if (n.contains("searchweb") || n.contains("search_web")) {
            return "正在上网查找旅行相关信息";
        }
        if (n.contains("searchimages") || n.contains("search_images") || n.contains("imagesearch")) {
            return "正在搜索相关风景图片";
        }
        if (n.contains("generatepdf") || n.contains("pdf")) {
            return "正在生成行程 PDF 文件，方便你保存";
        }
        if (n.contains("getcurrentdatetime") || n.contains("datetime") || n.contains("date")) {
            return "正在确认今天的日期和时间";
        }
        if (n.contains("terminate")) {
            return "目标已完成，正在收尾";
        }
        if (n.contains("download")) {
            return "正在下载需要的资料";
        }
        if (n.contains("scrap") || n.contains("webscrap")) {
            return "正在打开网页核对详细内容";
        }
        if (n.contains("file") || n.contains("write") || n.contains("read")) {
            return "正在整理文字资料";
        }
        if (n.contains("terminal")) {
            return "正在完成本地辅助操作";
        }
        if (args.contains("pdf") || args.contains("PDF")) {
            return "正在处理行程文档";
        }
        return "正在处理下一步旅行安排";
    }
}
