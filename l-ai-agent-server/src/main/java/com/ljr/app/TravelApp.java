package com.ljr.app;

import com.ljr.tools.FileOperationTool;
import com.ljr.tools.GetCurrentDateTimeTool;
import com.ljr.tools.ImageSearchTool;
import com.ljr.tools.PDFGenerationTool;
import com.ljr.tools.ResourceDownloadTool;
import com.ljr.tools.TerminalOperationTool;
import com.ljr.tools.WebScrapingTool;
import com.ljr.tools.WebSearchTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AI 旅行管家应用：多轮对话 + RAG + 工具调用 + MCP + 结构化行程报告。
 */
@Component
@Slf4j
public class TravelApp {

    private static final String SYSTEM_PROMPT = """
            你是旅行管家「旅伴」。回复要像管家即时消息，不要像 FAQ 文档。

            【输出风格】
            1）先给可用内容（草稿行程 / 图片卡片），再问最多 1 个必答问题；其余用默认值（舒适型、经典路线），并说明「不合适再说」。
            2）坏消息不要开场：工具失败压成 1 句附注，紧接替代方案。禁止用「抱歉，工具不可用」当第一句。
            3）多主题用 【图片获取】【行程规划】【交通】等分开，不要挤一段。
            4）标题只用【】，不用 ###；emoji 每段最多 1 个（⚠️👉📄）；一次最多 3 个要点。
            5）表格仅在真正需要对比时使用，且单元格两侧必须有空格：| 渠道 | 说明 |。
            6）每轮末尾最多 1 个问句。

            【行程】信息不全时：先按默认值出一版草稿，再问 1 个关键缺口（通常是出行日期）。先给价值，再收集信息。
            【知识库】有检索结果优先依据检索；不足再补常识，勿编造签证/价格。
            【工具】班次/票价/天气等用 searchWeb。配图只调 searchImages；把工具返回的 <<<MEDIA_GALLERY>>>…<<<END_MEDIA_GALLERY>>> 整段原样粘贴进回复，禁止删改、截断或只贴 items 片段。前端渲染卡片；不要 downloadResource。
            【下载】仅用户明确要求保存本地时才用 downloadResource。
            【严禁】禁止输出 <tool_call>、伪工具 JSON、冗长道歉开场；禁止把残缺的 {"thumb":...} 片段当正文。
            相对时间（今天/明天/下周）依据下方「当前时间基准」推算。
            """;

    private final ChatClient chatClient;
    private final RetrievalAugmentationAdvisor travelRetrievalAugmentationAdvisor;
    private final FileOperationTool fileOperationTool;
    private final WebSearchTool webSearchTool;
    private final WebScrapingTool webScrapingTool;
    private final ResourceDownloadTool resourceDownloadTool;
    private final TerminalOperationTool terminalOperationTool;
    private final PDFGenerationTool pdfGenerationTool;
    private final ImageSearchTool imageSearchTool;
    private final GetCurrentDateTimeTool getCurrentDateTimeTool;
    private final ObjectProvider<SyncMcpToolCallbackProvider> mcpToolCallbackProvider;

    public TravelApp(
            ChatModel chatModel,
            ChatMemory chatMemory,
            SimpleLoggerAdvisor simpleLoggerAdvisor,
            RetrievalAugmentationAdvisor travelRetrievalAugmentationAdvisor,
            FileOperationTool fileOperationTool,
            WebSearchTool webSearchTool,
            WebScrapingTool webScrapingTool,
            ResourceDownloadTool resourceDownloadTool,
            TerminalOperationTool terminalOperationTool,
            PDFGenerationTool pdfGenerationTool,
            ImageSearchTool imageSearchTool,
            GetCurrentDateTimeTool getCurrentDateTimeTool,
            ObjectProvider<SyncMcpToolCallbackProvider> mcpToolCallbackProvider) {
        this.travelRetrievalAugmentationAdvisor = travelRetrievalAugmentationAdvisor;
        this.fileOperationTool = fileOperationTool;
        this.webSearchTool = webSearchTool;
        this.webScrapingTool = webScrapingTool;
        this.resourceDownloadTool = resourceDownloadTool;
        this.terminalOperationTool = terminalOperationTool;
        this.pdfGenerationTool = pdfGenerationTool;
        this.imageSearchTool = imageSearchTool;
        this.getCurrentDateTimeTool = getCurrentDateTimeTool;
        this.mcpToolCallbackProvider = mcpToolCallbackProvider;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        simpleLoggerAdvisor
                )
                .build();
    }

    /** 每次请求注入当前时间，避免 SSE 无工具时模型幻觉输出 tool JSON。 */
    private String systemPromptWithNow() {
        return SYSTEM_PROMPT + "\n\n【当前时间基准】\n" + getCurrentDateTimeTool.getCurrentDateTime();
    }

    public String doChat(String message, String chatId) {
        return chatClient.prompt()
                .system(systemPromptWithNow())
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .content();
    }

    public String doChatWithRag(String message, String chatId) {
        return chatClient.prompt()
                .system(systemPromptWithNow())
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(travelRetrievalAugmentationAdvisor)
                .call()
                .content();
    }

    public String doChatWithTools(String message, String chatId) {
        return chatClient.prompt()
                .system(systemPromptWithNow())
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .tools(
                        fileOperationTool,
                        webSearchTool,
                        webScrapingTool,
                        resourceDownloadTool,
                        terminalOperationTool,
                        pdfGenerationTool,
                        imageSearchTool,
                        getCurrentDateTimeTool
                )
                .call()
                .content();
    }

    /**
     * 使用 MCP 图片搜索（以及本地工具）完成任务。
     * 需开启 profile mcp / APP_MCP_ENABLED=true，并已打包 MCP Server JAR。
     */
    public String doChatWithMcp(String message, String chatId) {
        SyncMcpToolCallbackProvider mcpProvider = mcpToolCallbackProvider.getIfAvailable();
        var prompt = chatClient.prompt()
                .system(systemPromptWithNow())
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .tools(
                        fileOperationTool,
                        webSearchTool,
                        webScrapingTool,
                        resourceDownloadTool,
                        terminalOperationTool,
                        pdfGenerationTool,
                        imageSearchTool,
                        getCurrentDateTimeTool
                );
        if (mcpProvider != null) {
            ToolCallback[] mcpCallbacks = mcpProvider.getToolCallbacks();
            if (mcpCallbacks != null && mcpCallbacks.length > 0) {
                log.info("已加载 MCP 工具 {} 个", mcpCallbacks.length);
                prompt = prompt.toolCallbacks(mcpCallbacks);
            } else {
                log.warn("MCP Client 已启用，但未发现可用工具回调");
            }
        } else {
            log.warn("未启用 MCP Client，将仅使用本地工具（含本地图片搜索）");
        }
        return prompt.call().content();
    }

    /**
     * 前端默认 SSE：先走真实 tool calling（可联网搜索），再把最终答复按块推送。
     * 避免纯 stream 无工具时模型把 &lt;tool_call&gt; 当文本输出。
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return Flux.defer(() -> {
                    String content = doChatWithTools(message, chatId);
                    if (content == null || content.isBlank()) {
                        return Flux.just("抱歉，暂时无法生成回复。");
                    }
                    return Flux.fromIterable(splitForSse(content));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static List<String> splitForSse(String content) {
        List<String> parts = new ArrayList<>();
        int size = 32;
        for (int i = 0; i < content.length(); i += size) {
            parts.add(content.substring(i, Math.min(i + size, content.length())));
        }
        return parts;
    }

    public record TravelReport(String title, List<String> suggestions) {
    }

    public TravelReport doChatWithReport(String message, String chatId) {
        return chatClient.prompt()
                .system(systemPromptWithNow() + "\n请以结构化行程报告回答，包含标题和若干条可执行建议。")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(TravelReport.class);
    }

    /** 供测试/排查：当前可用 MCP 工具名 */
    public List<String> listMcpToolNames() {
        SyncMcpToolCallbackProvider mcpProvider = mcpToolCallbackProvider.getIfAvailable();
        if (mcpProvider == null) {
            return List.of();
        }
        return Arrays.stream(mcpProvider.getToolCallbacks())
                .map(cb -> cb.getToolDefinition().name())
                .toList();
    }
}
