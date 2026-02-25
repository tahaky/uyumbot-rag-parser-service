package com.uyumbot.chunkingservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI chunkingServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RAG Chunking Service API")
                        .description("Document parsing and chunking service for Retrieval-Augmented Generation (RAG) pipelines. " +
                                "Manages documents and their text chunks.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Uyumbot")
                                .url("https://github.com/tahaky/uyumbot-rag-parser-service")));
    }
}
