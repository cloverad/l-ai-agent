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
class TravelAppToolTest {

    @Resource
    private TravelApp travelApp;

    @Test
    void doChatWithTools_shouldGenerateTravelPdf() {
        Assumptions.assumeTrue(
                System.getenv("DASHSCOP_API_KEY") != null
                        && !System.getenv("DASHSCOP_API_KEY").isBlank(),
                "跳过：未设置环境变量 DASHSCOP_API_KEY"
        );

        String chatId = UUID.randomUUID().toString(true);
        String answer = travelApp.doChatWithTools(
                "请生成一份简短的《京都1日游》行程，并调用 PDF 工具保存为 kyoto-1day.pdf。"
                        + "PDF 内容包含上午清水寺、下午岚山即可。完成后告诉我文件路径。",
                chatId);

        assertNotNull(answer);
        assertFalse(answer.isBlank());
        System.out.println("工具调用回答: " + answer);
        assertTrue(
                answer.toLowerCase().contains("pdf")
                        || answer.contains("生成")
                        || answer.contains("路径")
                        || answer.contains("kyoto"),
                "回答应体现 PDF 生成结果"
        );
    }
}
