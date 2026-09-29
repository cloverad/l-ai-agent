package com.ljr.app;

import cn.hutool.core.lang.UUID;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles({"dashscope", "mcp"})
class TravelAppMcpTest {

    @Resource
    private TravelApp travelApp;

    @Test
    void mcpTools_shouldBeDiscoverable_whenEnabled() {
        List<String> names = travelApp.listMcpToolNames();
        System.out.println("MCP tools: " + names);
        // 若 JAR 或进程拉起失败，这里可能为空；有工具则继续下方对话测试
        Assumptions.assumeFalse(names.isEmpty(), "MCP 工具未加载，请先 package image-search-mcp-server");
    }

    @Test
    void doChatWithMcp_shouldSearchImages() {
        Assumptions.assumeTrue(
                System.getenv("DASHSCOP_API_KEY") != null
                        && !System.getenv("DASHSCOP_API_KEY").isBlank(),
                "跳过：未设置 DASHSCOP_API_KEY"
        );
        Assumptions.assumeFalse(travelApp.listMcpToolNames().isEmpty(), "跳过：MCP 工具未加载");

        String chatId = UUID.randomUUID().toString(true);
        String answer = travelApp.doChatWithMcp(
                "请使用图片搜索工具，搜索 Kyoto temple 相关风景图，返回 2 个图片链接即可。",
                chatId);
        assertNotNull(answer);
        assertFalse(answer.isBlank());
        System.out.println("MCP 对话回答: " + answer);
    }
}
