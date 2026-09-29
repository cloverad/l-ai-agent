package com.ljr.agent;

import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;

/**
 * 旅行场景的自主规划智能体 TravelManus。
 */
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class TravelManus extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
            你是 TravelManus，一位拥有自主规划能力的 AI 旅行管家智能体。
            你可以获取当前时间、联网搜索、抓取网页、读写文件、下载资源、搜索图片、生成 PDF，并在完成后调用终止工具。
            目标：根据用户需求制定可执行的旅行方案，必要时产出文件/PDF，最终给出清晰总结。
            约束：
            1）优先完成用户目标，避免无意义循环；回复像即时管家消息，先给草稿再问最多 1 个问题
            2）工具结果要用于下一步决策
            3）相对时间先 getCurrentDateTime；禁止输出伪工具 JSON
            4）配图调 searchImages，把 <<<MEDIA_GALLERY>>>…<<<END_MEDIA_GALLERY>>> 整段原样放进最终回复；勿截断 JSON；勿无故 downloadResource
            5）任务完成后必须调用 doTerminate 结束；若生成了 PDF，在总结里告知文件名即可（系统会提供下载）
            """;

    private static final String NEXT_STEP_PROMPT = """
            基于目前已有信息，思考下一步最有价值的行动。
            你可以继续调用工具，或在目标已完成后调用 doTerminate 结束任务。
            """;

    public TravelManus(ToolCallback[] availableTools, ChatModel chatModel, ToolCallingManager toolCallingManager) {
        super(availableTools, chatModel, toolCallingManager);
        setName("TravelManus");
        setSystemPrompt(SYSTEM_PROMPT);
        setNextStepPrompt(NEXT_STEP_PROMPT);
        setMaxSteps(8);
        // 放入系统提示
        getMessageList().add(new SystemMessage(SYSTEM_PROMPT));
    }
}
