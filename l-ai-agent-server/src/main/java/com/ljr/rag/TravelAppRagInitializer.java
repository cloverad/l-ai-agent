package com.ljr.rag;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时将旅行知识写入 PgVector（仅空库初始化一次）。
 */
@Component
@Slf4j
public class TravelAppRagInitializer {

    private final VectorStore vectorStore;
    private final TravelAppDocumentLoader documentLoader;
    private final JdbcTemplate jdbcTemplate;

    public TravelAppRagInitializer(
            VectorStore vectorStore,
            TravelAppDocumentLoader documentLoader,
            JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.documentLoader = documentLoader;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vector_store", Integer.class);
        if (count != null && count > 0) {
            log.info("PgVector 已有 {} 条向量，跳过知识库初始化", count);
            return;
        }
        List<Document> documents = documentLoader.loadMarkdowns();
        List<Document> splitDocuments = new TokenTextSplitter().apply(documents);
        // 百炼 embedding 单次 batch 上限为 10
        int batchSize = 10;
        for (int i = 0; i < splitDocuments.size(); i += batchSize) {
            int end = Math.min(i + batchSize, splitDocuments.size());
            vectorStore.add(splitDocuments.subList(i, end));
        }
        log.info("已写入 PgVector {} 条文档切片", splitDocuments.size());
    }
}
