package com.PassFamilyDoddmane.QuizeBackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        String baseUrl,
        String model,
        Double temperature,
        Integer maxTokens,
        Integer seed,
        Double topP,
        Integer connectTimeoutSeconds,
        Integer readTimeoutSeconds,
        Integer maxDocumentChars
) {
    public AiProperties {
        baseUrl = baseUrl == null || baseUrl.isBlank() ? "http://127.0.0.1:1234" : baseUrl;
        model = model == null || model.isBlank() ? "gemma-4-e4b-it" : model;
        temperature = temperature == null ? 0.3 : temperature;
        maxTokens = maxTokens == null ? 2048 : maxTokens;
        topP = topP == null ? 0.9 : topP;
        connectTimeoutSeconds = connectTimeoutSeconds == null ? 5 : connectTimeoutSeconds;
        readTimeoutSeconds = readTimeoutSeconds == null ? 90 : readTimeoutSeconds;
        maxDocumentChars = maxDocumentChars == null ? 6000 : maxDocumentChars;
    }
}
