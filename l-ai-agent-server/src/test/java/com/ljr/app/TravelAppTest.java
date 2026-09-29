package com.ljr.app;

import cn.hutool.core.lang.UUID;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("dashscope")
class TravelAppTest {

    @Resource
    private TravelApp travelApp;

    @Test
    void doChat_shouldKeepMultiTurnContext() {
        Assumptions.assumeTrue(
                System.getenv("DASHSCOP_API_KEY") != null
                        && !System.getenv("DASHSCOP_API_KEY").isBlank(),
                "跳过：未设置环境变量 DASHSCOP_API_KEY"
        );

        String chatId = UUID.randomUUID().toString(true);

        String reply1 = travelApp.doChat("我想去京都玩，大概 3 天，预算中等。", chatId);
        assertNotNull(reply1);
        assertFalse(reply1.isBlank());
        System.out.println("第1轮: " + reply1);

        String reply2 = travelApp.doChat("我更喜欢寺庙和美食，不太想逛街。根据前面说的继续规划。", chatId);
        assertNotNull(reply2);
        assertFalse(reply2.isBlank());
        System.out.println("第2轮: " + reply2);
    }

    @Test
    void doChatWithReport_shouldReturnStructuredReport() {
        Assumptions.assumeTrue(
                System.getenv("DASHSCOP_API_KEY") != null
                        && !System.getenv("DASHSCOP_API_KEY").isBlank(),
                "跳过：未设置环境变量 DASHSCOP_API_KEY"
        );

        String chatId = UUID.randomUUID().toString(true);
        TravelApp.TravelReport report = travelApp.doChatWithReport(
                "帮我做一份东京 2 日精华游行程报告，偏人文景点。", chatId);

        assertNotNull(report);
        assertNotNull(report.title());
        assertNotNull(report.suggestions());
        assertTrue(report.suggestions().size() > 0);
        System.out.println("报告标题: " + report.title());
        report.suggestions().forEach(s -> System.out.println("- " + s));
    }
}
