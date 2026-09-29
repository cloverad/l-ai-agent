package com.ljr.chatmemory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于本地 JSON 文件的对话记忆仓库。
 */
@Component
public class FileBasedChatMemoryRepository implements ChatMemoryRepository {

    private final Path baseDir;
    private final ObjectMapper objectMapper;

    public FileBasedChatMemoryRepository(
            @Value("${app.chat-memory.dir}") String dir,
            ObjectMapper objectMapper) throws IOException {
        this.baseDir = Path.of(dir);
        this.objectMapper = objectMapper;
        Files.createDirectories(baseDir);
    }

    @Override
    public List<String> findConversationIds() {
        try (var stream = Files.list(baseDir)) {
            return stream
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .map(p -> p.getFileName().toString().replace(".json", ""))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("列举对话记忆失败", e);
        }
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        Path file = toPath(conversationId);
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            List<MessageRecord> records = objectMapper.readValue(file.toFile(), new TypeReference<>() {
            });
            List<Message> messages = new ArrayList<>(records.size());
            for (MessageRecord record : records) {
                messages.add(toMessage(record));
            }
            return messages;
        } catch (IOException e) {
            throw new IllegalStateException("读取对话记忆失败: " + conversationId, e);
        }
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        List<MessageRecord> records = messages.stream()
                .map(m -> new MessageRecord(m.getMessageType().getValue(), m.getText()))
                .toList();
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(toPath(conversationId).toFile(), records);
        } catch (IOException e) {
            throw new IllegalStateException("保存对话记忆失败: " + conversationId, e);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        try {
            Files.deleteIfExists(toPath(conversationId));
        } catch (IOException e) {
            throw new IllegalStateException("删除对话记忆失败: " + conversationId, e);
        }
    }

    private Path toPath(String conversationId) {
        String safeId = conversationId.replaceAll("[^a-zA-Z0-9-_]", "_");
        return baseDir.resolve(safeId + ".json");
    }

    private Message toMessage(MessageRecord record) {
        if (record == null || record.type() == null || record.type().isBlank()) {
            throw new IllegalStateException("对话记忆记录缺少 type 字段");
        }
        MessageType type = MessageType.fromValue(record.type());
        String text = record.text() == null ? "" : record.text();
        return switch (type) {
            case USER -> new UserMessage(text);
            case ASSISTANT -> new AssistantMessage(text);
            case SYSTEM -> new SystemMessage(text);
            case TOOL -> new AssistantMessage(text);
        };
    }

    public record MessageRecord(String type, String text) {
    }
}
