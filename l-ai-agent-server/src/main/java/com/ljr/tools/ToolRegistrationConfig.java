package com.ljr.tools;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ToolRegistrationConfig {

    /**
     * 统一工具集：供 TravelApp / TravelManus / 后续扩展复用。
     */
    @Bean
    @Primary
    public ToolCallbackProvider travelToolCallbackProvider(
            FileOperationTool fileOperationTool,
            WebSearchTool webSearchTool,
            WebScrapingTool webScrapingTool,
            ResourceDownloadTool resourceDownloadTool,
            TerminalOperationTool terminalOperationTool,
            PDFGenerationTool pdfGenerationTool,
            ImageSearchTool imageSearchTool,
            GetCurrentDateTimeTool getCurrentDateTimeTool,
            TerminateTool terminateTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        fileOperationTool,
                        webSearchTool,
                        webScrapingTool,
                        resourceDownloadTool,
                        terminalOperationTool,
                        pdfGenerationTool,
                        imageSearchTool,
                        getCurrentDateTimeTool,
                        terminateTool
                )
                .build();
    }
}
