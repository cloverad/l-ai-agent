package com.ljr.agent;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智能体基类：维护状态、消息上下文与多步执行循环。
 * 流式输出使用 THINK / ANSWER / PDF 标记，供前端折叠思考与下载文件。
 */
@Data
@Slf4j
public abstract class BaseAgent {

    public static final String THINK_START = "<<<THINK_START>>>";
    public static final String THINK_END = "<<<THINK_END>>>";
    public static final String ANSWER_START = "<<<ANSWER_START>>>";
    public static final String ANSWER_END = "<<<ANSWER_END>>>";

    private static final Pattern PDF_MARKER = Pattern.compile("<<<PDF:([^>]+)>>>");

    private String name;
    private String systemPrompt;
    private String nextStepPrompt;
    private AgentState state = AgentState.IDLE;
    private int currentStep = 0;
    private int maxSteps = 10;
    private final List<Message> messageList = new ArrayList<>();
    private final List<String> stepResults = new ArrayList<>();

    public String run(String userPrompt) {
        if (state != AgentState.IDLE) {
            throw new IllegalStateException("智能体当前状态不可运行: " + state);
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new IllegalArgumentException("用户提示词不能为空");
        }
        state = AgentState.RUNNING;
        messageList.add(new org.springframework.ai.chat.messages.UserMessage(userPrompt));
        try {
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                currentStep = i + 1;
                log.info("智能体 {} 执行第 {}/{} 步", name, currentStep, maxSteps);
                String stepResult = step();
                stepResults.add("Step " + currentStep + ": " + stepResult);
            }
            if (currentStep >= maxSteps && state != AgentState.FINISHED) {
                state = AgentState.FINISHED;
                stepResults.add("达到最大步数 " + maxSteps + "，任务终止");
            }
            return String.join("\n", stepResults);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("智能体执行失败", e);
            return "智能体执行失败: " + e.getMessage();
        } finally {
            cleanup();
        }
    }

    public Flux<String> runStream(String userPrompt) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        CompletableFuture.runAsync(() -> {
            try {
                if (state != AgentState.IDLE) {
                    sink.tryEmitError(new IllegalStateException("智能体当前状态不可运行: " + state));
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    sink.tryEmitError(new IllegalArgumentException("用户提示词不能为空"));
                    return;
                }
                state = AgentState.RUNNING;
                messageList.add(new org.springframework.ai.chat.messages.UserMessage(userPrompt));

                sink.tryEmitNext(THINK_START + "\n");
                sink.tryEmitNext("智能体 " + name + " 开始执行任务...\n");

                String finalAnswer = null;
                Set<String> pdfs = new LinkedHashSet<>();

                for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                    currentStep = i + 1;
                    String stepResult = step();
                    collectPdfs(stepResult, pdfs);

                    if (stepResult != null && stepResult.startsWith(ToolCallAgent.FINAL_PREFIX)) {
                        finalAnswer = stepResult.substring(ToolCallAgent.FINAL_PREFIX.length()).trim();
                        sink.tryEmitNext("Step " + currentStep + "：已整理好最终答复。\n");
                        stepResults.add("Step " + currentStep + ": 已整理好最终答复。");
                    } else {
                        String plain = stripMarkers(stepResult);
                        if (StrUtil.isBlank(plain)) {
                            plain = "正在继续处理任务。";
                        }
                        // 限制为约两句：按句号截断
                        plain = shortenPlain(plain);
                        sink.tryEmitNext("Step " + currentStep + "：" + plain + "\n");
                        stepResults.add("Step " + currentStep + ": " + plain);
                    }
                }

                if (this instanceof ToolCallAgent toolAgent) {
                    if (StrUtil.isBlank(finalAnswer) && StrUtil.isNotBlank(toolAgent.getLastFinalAnswer())) {
                        finalAnswer = toolAgent.getLastFinalAnswer();
                    }
                    pdfs.addAll(toolAgent.getGeneratedPdfs());
                }

                if (currentStep >= maxSteps && state != AgentState.FINISHED) {
                    state = AgentState.FINISHED;
                    sink.tryEmitNext("Step " + (currentStep + 1) + "：已达到步骤上限，先给出当前结果。\n");
                }

                sink.tryEmitNext(THINK_END + "\n");
                sink.tryEmitNext(ANSWER_START + "\n");
                sink.tryEmitNext(StrUtil.blankToDefault(finalAnswer, "任务已完成，可查看上方步骤了解过程。") + "\n");
                sink.tryEmitNext(ANSWER_END + "\n");
                for (String pdf : pdfs) {
                    sink.tryEmitNext("<<<PDF:" + pdf + ">>>\n");
                }
                sink.tryEmitComplete();
            } catch (Exception e) {
                state = AgentState.ERROR;
                sink.tryEmitNext(THINK_END + "\n");
                sink.tryEmitNext(ANSWER_START + "\n");
                sink.tryEmitNext("执行失败：" + e.getMessage() + "\n");
                sink.tryEmitNext(ANSWER_END + "\n");
                sink.tryEmitComplete();
            } finally {
                cleanup();
            }
        });
        return sink.asFlux();
    }

    private static void collectPdfs(String text, Set<String> pdfs) {
        if (text == null) {
            return;
        }
        Matcher m = PDF_MARKER.matcher(text);
        while (m.find()) {
            pdfs.add(m.group(1).trim());
        }
    }

    private static String stripMarkers(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("<<<PDF:[^>]+>>>", " ").replaceAll("\\s+", " ").trim();
    }

    private static String shortenPlain(String text) {
        String t = text.trim();
        // 最多保留两句
        int first = indexOfSentenceEnd(t, 0);
        if (first < 0) {
            return t.length() > 80 ? t.substring(0, 80) + "…" : t;
        }
        int second = indexOfSentenceEnd(t, first + 1);
        if (second < 0) {
            return t.substring(0, Math.min(t.length(), first + 1));
        }
        return t.substring(0, Math.min(t.length(), second + 1));
    }

    private static int indexOfSentenceEnd(String t, int from) {
        int best = -1;
        for (char c : new char[]{'。', '！', '？', ';', '；'}) {
            int i = t.indexOf(c, from);
            if (i >= 0 && (best < 0 || i < best)) {
                best = i;
            }
        }
        return best;
    }

    public abstract String step();

    protected void cleanup() {
        // 子类可覆盖做资源清理
    }
}
