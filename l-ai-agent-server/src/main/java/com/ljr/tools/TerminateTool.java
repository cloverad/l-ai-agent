package com.ljr.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * 终止工具：智能体完成任务后调用，结束 ReAct 循环。
 */
@Component
public class TerminateTool {

    @Tool(description = "当任务已完成、无需继续调用其他工具时，调用此工具结束。参数无需填写。")
    public String doTerminate() {
        return "任务结束";
    }
}
