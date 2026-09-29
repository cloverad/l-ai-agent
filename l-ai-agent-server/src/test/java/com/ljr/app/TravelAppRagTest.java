package com.ljr.app;

import cn.hutool.core.lang.UUID;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("dashscope")
class TravelAppRagTest {

    @Resource
    private TravelApp travelApp;

    @Test
    void doChatWithRag_shouldUseTravelKnowledge() {
        Assumptions.assumeTrue(
                System.getenv("DASHSCOP_API_KEY") != null
                        && !System.getenv("DASHSCOP_API_KEY").isBlank(),
                "跳过：未设置环境变量 DASHSCOP_API_KEY"
        );

        String chatId = UUID.randomUUID().toString(true);
        String answer = travelApp.doChatWithRag(
                "去日本旅游签证一般要提前多久办？入境现金超过多少要申报？", chatId);

        assertNotNull(answer);
        assertFalse(answer.isBlank());
        System.out.println("RAG 回答: " + answer);
    }
}
