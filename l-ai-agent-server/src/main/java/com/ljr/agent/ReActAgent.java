package com.ljr.agent;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * ReAct 智能体：每一步先思考（think）再行动（act）。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class ReActAgent extends BaseAgent {

    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if (!shouldAct) {
                setState(AgentState.FINISHED);
                String finalText = "任务已完成。";
                if (this instanceof ToolCallAgent toolAgent) {
                    String answer = toolAgent.getLastFinalAnswer();
                    if (StrUtil.isNotBlank(answer)) {
                        finalText = answer;
                    }
                }
                return ToolCallAgent.FINAL_PREFIX + finalText;
            }
            return act();
        } catch (Exception e) {
            log.error("ReAct 步骤执行失败", e);
            return "这一步遇到问题：" + e.getMessage();
        }
    }

    /**
     * 思考：决定是否需要继续调用工具。
     *
     * @return true 表示需要行动；false 表示可结束
     */
    public abstract boolean think();

    /**
     * 行动：执行工具调用或产出最终结果。
     */
    public abstract String act();
}
