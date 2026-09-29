package com.ljr.controller;

import com.ljr.agent.TravelManus;
import com.ljr.app.TravelApp;
import com.ljr.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
@Tag(name = "AI 接口")
public class AiController {

    @Resource
    private TravelApp travelApp;

    @Resource
    private ObjectProvider<TravelManus> travelManusProvider;

    @GetMapping("/travel_app/chat/sync")
    @Operation(summary = "旅行管家同步对话")
    public BaseResponse<String> doChatWithTravelAppSync(
            @RequestParam("message") String message,
            @RequestParam("chatId") String chatId) {
        return BaseResponse.success(travelApp.doChatWithTools(message, chatId));
    }

    /**
     * 旅行管家 SSE 流式对话（内含 tool calling，可联网搜索）。
     * 前端可用 EventSource 连接：/api/ai/travel_app/chat/sse
     */
    @GetMapping(value = "/travel_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "旅行管家 SSE 流式对话")
    public SseEmitter doChatWithTravelAppSSE(
            @RequestParam("message") String message,
            @RequestParam("chatId") String chatId) {
        SseEmitter emitter = new SseEmitter(300_000L);
        travelApp.doChatByStream(message, chatId)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        chunk -> sendSafe(emitter, chunk),
                        error -> {
                            sendSafe(emitter, "\n[错误] " + error.getMessage());
                            emitter.completeWithError(error);
                        },
                        emitter::complete
                );
        return emitter;
    }

    @GetMapping("/travel_app/chat/rag")
    @Operation(summary = "旅行管家 RAG 知识库问答")
    public BaseResponse<String> doChatWithTravelAppRag(
            @RequestParam("message") String message,
            @RequestParam("chatId") String chatId) {
        return BaseResponse.success(travelApp.doChatWithRag(message, chatId));
    }

    /**
     * TravelManus 智能体 SSE：分步推送思考与工具执行过程。
     */
    @GetMapping(value = "/manus/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "TravelManus 智能体 SSE")
    public SseEmitter doChatWithManus(@RequestParam("message") String message) {
        TravelManus manus = travelManusProvider.getObject();
        SseEmitter emitter = new SseEmitter(300_000L);
        manus.runStream(message)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        chunk -> sendSafe(emitter, chunk),
                        error -> {
                            sendSafe(emitter, "\n[错误] " + error.getMessage());
                            emitter.completeWithError(error);
                        },
                        emitter::complete
                );
        return emitter;
    }

    private void sendSafe(SseEmitter emitter, String data) {
        try {
            // 避免空 chunk；换行交给 SSE 多 data 行拼回
            if (data == null) {
                return;
            }
            emitter.send(SseEmitter.event().data(data));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
