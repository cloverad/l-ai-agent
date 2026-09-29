package com.ljr.agent;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.Arrays;

@Configuration
public class AgentConfig {

    /**
     * 每次对话新建一个 Manus 实例，避免多请求共享状态。
     */
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public TravelManus travelManus(ChatModel chatModel, ToolCallbackProvider travelToolCallbackProvider) {
        ToolCallback[] toolCallbacks = travelToolCallbackProvider.getToolCallbacks();
        ToolCallingManager toolCallingManager = DefaultToolCallingManager.builder()
                .toolCallbackResolver(new StaticToolCallbackResolver(Arrays.asList(toolCallbacks)))
                .build();
        return new TravelManus(toolCallbacks, chatModel, toolCallingManager);
    }
}
