package com.ljr;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("dashscope")
class DashScopeChatModelTest {

    @Resource
    private ChatModel chatModel;

    @Test
    void chatModelShouldReply() {
        Assumptions.assumeTrue(
                System.getenv("DASHSCOP_API_KEY") != null
                        && !System.getenv("DASHSCOP_API_KEY").isBlank(),
                "跳过：未设置环境变量 DASHSCOP_API_KEY"
        );

        String content = chatModel.call(new Prompt("用一句话介绍你自己，说明你是通义千问。"))
                .getResult()
                .getOutput()
                .getText();

        assertNotNull(content);
        assertFalse(content.isBlank());
        System.out.println("DashScope 回复: " + content);
    }
}
