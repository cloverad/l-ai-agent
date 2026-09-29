package com.ljr.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TravelRagConfig {

    @Bean
    public RetrievalAugmentationAdvisor travelRetrievalAugmentationAdvisor(
            VectorStore vectorStore,
            ChatModel chatModel) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .similarityThreshold(0.45d)
                        .topK(4)
                        .build())
                .queryTransformers(RewriteQueryTransformer.builder()
                        .chatClientBuilder(ChatClient.builder(chatModel))
                        .build())
                .build();
    }
}
