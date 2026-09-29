package com.ljr.controller;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dashscope")
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void health_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void travelAppSync_shouldRequireParams() throws Exception {
        Assumptions.assumeTrue(
                System.getenv("DASHSCOP_API_KEY") != null
                        && !System.getenv("DASHSCOP_API_KEY").isBlank(),
                "跳过：未设置 DASHSCOP_API_KEY"
        );
        mockMvc.perform(get("/ai/travel_app/chat/sync")
                        .param("message", "你好，用一句话介绍你自己")
                        .param("chatId", "test-chat-1"))
                .andExpect(status().isOk());
    }
}
