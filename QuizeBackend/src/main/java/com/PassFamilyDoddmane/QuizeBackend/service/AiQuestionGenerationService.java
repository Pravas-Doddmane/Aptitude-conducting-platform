package com.PassFamilyDoddmane.QuizeBackend.service;

import com.PassFamilyDoddmane.QuizeBackend.common.exception.BadRequestException;
import com.PassFamilyDoddmane.QuizeBackend.common.exception.ResourceNotFoundException;
import com.PassFamilyDoddmane.QuizeBackend.config.AiProperties;
import com.PassFamilyDoddmane.QuizeBackend.dto.ai.AiQuestionGenerateRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.ai.AiQuestionGenerateResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.question.QuestionOptionRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.question.QuestionRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.question.QuestionResponse;
import com.PassFamilyDoddmane.QuizeBackend.entity.Category;
import com.PassFamilyDoddmane.QuizeBackend.repository.CategoryRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuestionRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AiQuestionGenerationService {

    private final AiProperties aiProperties;
    private final CategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;
    private final QuestionService questionService;
    private final ObjectMapper objectMapper;

    public AiQuestionGenerateResponse generate(AiQuestionGenerateRequest request) {
        validateRequest(request);
        Category category = getActiveCategory(request.categoryId());
        Set<String> usedStems = getUsedStems(category);
        AiGeneratedQuestionList generatedQuestions = requestQuestions(request, category, usedStems, null, null, false);
        return saveGeneratedQuestions(request, category, usedStems, generatedQuestions, false);
    }

    public AiQuestionGenerateResponse generateFromFile(AiQuestionGenerateRequest request, MultipartFile file) {
        validateRequest(request);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Upload a PDF or image file for AI generation");
        }

        Category category = getActiveCategory(request.categoryId());
        Set<String> usedStems = getUsedStems(category);
        UploadedAiSource source = readUploadedSource(file);
        AiGeneratedQuestionList generatedQuestions = requestQuestions(request, category, usedStems, source, file.getOriginalFilename(), true);
        return saveGeneratedQuestions(request, category, usedStems, generatedQuestions, true);
    }

    private Category getActiveCategory(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new BadRequestException("Category is inactive");
        }
        return category;
    }

    private Set<String> getUsedStems(Category category) {
        Set<String> usedStems = new LinkedHashSet<>();
        questionRepository.findByCategoryId(category.getId()).forEach(question -> usedStems.add(normalize(question.getStem())));
        return usedStems;
    }

    private AiQuestionGenerateResponse saveGeneratedQuestions(
            AiQuestionGenerateRequest request,
            Category category,
            Set<String> usedStems,
            AiGeneratedQuestionList generatedQuestions,
            boolean requireCategoryMatch
    ) {
        if (requireCategoryMatch && !Boolean.TRUE.equals(generatedQuestions.categoryMatch())) {
            throw new BadRequestException(generatedQuestions.reason() == null || generatedQuestions.reason().isBlank()
                    ? "AI could not confirm that the uploaded file matches the selected category. Create the correct category or upload the correct file."
                    : generatedQuestions.reason());
        }

        List<AiGeneratedQuestion> generatedQuestionItems = generatedQuestions.questions() == null
                ? List.of()
                : generatedQuestions.questions();
        List<QuestionResponse> createdQuestions = new ArrayList<>();
        int skippedDuplicates = 0;

        for (AiGeneratedQuestion generatedQuestion : generatedQuestionItems) {
            if (!isValidQuestion(generatedQuestion, request.optionsPerQuestion())) {
                continue;
            }

            String normalizedStem = normalize(generatedQuestion.stem());
            if (usedStems.contains(normalizedStem)) {
                skippedDuplicates++;
                continue;
            }

            List<QuestionOptionRequest> options = new ArrayList<>();
            for (int index = 0; index < generatedQuestion.options().size(); index++) {
                AiGeneratedOption option = generatedQuestion.options().get(index);
                options.add(new QuestionOptionRequest(index + 1, option.optionText().trim(), Boolean.TRUE.equals(option.correct())));
            }

            QuestionRequest questionRequest = new QuestionRequest(
                    category.getId(),
                    generatedQuestion.explanation(),
                    null,
                    generatedQuestion.stem().trim(),
                    request.difficultyLevel(),
                    options
            );
            createdQuestions.add(questionService.create(questionRequest));
            usedStems.add(normalizedStem);
        }

        if (requireCategoryMatch && createdQuestions.isEmpty()) {
            throw new BadRequestException("AI could not create valid questions from this file. Create the correct category or upload a clearer matching file.");
        }

        return new AiQuestionGenerateResponse(
                category.getId(),
                category.getName(),
                request.questionCount(),
                createdQuestions.size(),
                skippedDuplicates,
                createdQuestions
        );
    }

    private AiGeneratedQuestionList requestQuestions(
            AiQuestionGenerateRequest request,
            Category category,
            Set<String> existingStems,
            UploadedAiSource source,
            String fileName,
            boolean fromFile
    ) {
        try {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(Duration.ofSeconds(aiProperties.connectTimeoutSeconds()));
            requestFactory.setReadTimeout(Duration.ofSeconds(aiProperties.readTimeoutSeconds()));

            RestClient restClient = RestClient.builder()
                    .baseUrl(aiProperties.baseUrl())
                    .requestFactory(requestFactory)
                    .build();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", aiProperties.model());
            body.put("temperature", aiProperties.temperature());
            body.put("max_tokens", aiProperties.maxTokens());
            body.put("top_p", aiProperties.topP());
            body.put("stream", false);
            body.put("response_format", jsonSchemaResponseFormat());
            if (aiProperties.seed() != null) {
                body.put("seed", aiProperties.seed());
            }
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt(fromFile)),
                    Map.of("role", "user", "content", userMessageContent(request, category, existingStems, source, fileName, fromFile))
            ));

            LmStudioResponse response = restClient.post()
                    .uri("/v1/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(LmStudioResponse.class);

            String content = response == null || response.choices() == null || response.choices().isEmpty()
                    ? null
                    : response.choices().get(0).message().content();
            if (content == null || content.isBlank()) {
                throw new BadRequestException("AI provider returned an empty response");
            }

            return objectMapper.readValue(extractJson(content), AiGeneratedQuestionList.class);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("AI question generation failed. Make sure LM Studio is running at "
                    + aiProperties.baseUrl()
                    + " with model "
                    + aiProperties.model()
                    + ". Details: "
                    + ex.getMessage());
        }
    }

    private void validateRequest(AiQuestionGenerateRequest request) {
        if (request.categoryId() == null) {
            throw new BadRequestException("Category is required");
        }
        if (request.topic() == null || request.topic().isBlank()) {
            throw new BadRequestException("Topic is required");
        }
        if (request.difficultyLevel() == null) {
            throw new BadRequestException("Difficulty is required");
        }
        if (request.questionCount() == null || request.questionCount() < 1 || request.questionCount() > 20) {
            throw new BadRequestException("Question count must be between 1 and 20");
        }
        if (request.optionsPerQuestion() == null || request.optionsPerQuestion() < 2 || request.optionsPerQuestion() > 6) {
            throw new BadRequestException("Options per question must be between 2 and 6");
        }
    }

    private String systemPrompt(boolean fromFile) {
        String categoryRule = fromFile
                ? "If the uploaded content is not clearly about the selected category, return categoryMatch false with a short reason and no questions."
                : "Generate questions only for the selected category.";
        return """
                You generate quiz content for an admin system.
                Return valid compact JSON only. No markdown, no comments, no extra text.
                Each question must have exactly one correct option.
                Do not repeat existing question stems.
                %s
                """.formatted(categoryRule);
    }

    private Object userMessageContent(
            AiQuestionGenerateRequest request,
            Category category,
            Set<String> existingStems,
            UploadedAiSource source,
            String fileName,
            boolean fromFile
    ) {
        String prompt = fromFile
                ? filePrompt(request, category, existingStems, source, fileName)
                : promptOnly(request, category, existingStems);
        if (source == null || source.imageDataUrl() == null) {
            return prompt;
        }
        return List.of(
                Map.of("type", "text", "text", prompt),
                Map.of("type", "image_url", "image_url", Map.of("url", source.imageDataUrl()))
        );
    }

    private String promptOnly(AiQuestionGenerateRequest request, Category category, Set<String> existingStems) {
        return """
                Generate %d quiz questions.
                Category: %s
                Category description: %s
                Topic: %s
                Difficulty: %s
                Options per question: %d
                Existing question stems to avoid: %s

                Return this JSON shape:
                {
                  "categoryMatch": true,
                  "reason": "",
                  "questions": [
                    {
                      "stem": "question text",
                      "explanation": "short explanation",
                      "options": [
                        { "optionText": "answer text", "correct": true }
                      ]
                    }
                  ]
                }
                """.formatted(
                request.questionCount(),
                category.getName(),
                category.getDescription() == null ? "" : category.getDescription(),
                request.topic(),
                request.difficultyLevel().name(),
                request.optionsPerQuestion(),
                existingStems
        );
    }

    private String filePrompt(AiQuestionGenerateRequest request, Category category, Set<String> existingStems, UploadedAiSource source, String fileName) {
        String documentText = limitText(source.text() == null ? "" : source.text());
        return """
                Read the uploaded quiz/question paper content and create questions only if it matches the selected category.
                Selected category: %s
                Category description: %s
                Topic hint: %s
                Difficulty: %s
                Number of questions to create: %d
                Options per question: %d
                Uploaded file name: %s
                Existing question stems to avoid: %s

                If the uploaded content is not clearly related to the selected category, return:
                { "categoryMatch": false, "reason": "Create the correct category or upload the correct file.", "questions": [] }

                If it matches, return:
                {
                  "categoryMatch": true,
                  "reason": "",
                  "questions": [
                    {
                      "stem": "question text",
                      "explanation": "short explanation",
                      "options": [
                        { "optionText": "answer text", "correct": true }
                      ]
                    }
                  ]
                }

                Uploaded text content:
                %s
                """.formatted(
                category.getName(),
                category.getDescription() == null ? "" : category.getDescription(),
                request.topic(),
                request.difficultyLevel().name(),
                request.questionCount(),
                request.optionsPerQuestion(),
                fileName == null ? "" : fileName,
                existingStems,
                documentText
        );
    }

    private UploadedAiSource readUploadedSource(MultipartFile file) {
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            if (contentType.equals("application/pdf") || fileName.endsWith(".pdf")) {
                return new UploadedAiSource(extractPdfText(file), null);
            }
            if (contentType.startsWith("image/")) {
                String dataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(file.getBytes());
                return new UploadedAiSource("", dataUrl);
            }
            if (contentType.startsWith("text/") || fileName.endsWith(".txt")) {
                return new UploadedAiSource(new String(file.getBytes(), StandardCharsets.UTF_8), null);
            }
        } catch (IOException ex) {
            throw new BadRequestException("Unable to read uploaded file: " + ex.getMessage());
        }
        throw new BadRequestException("Only PDF, image, or text files are supported for AI generation");
    }

    private String extractPdfText(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            String text = new PDFTextStripper().getText(document);
            if (text == null || text.isBlank()) {
                throw new BadRequestException("PDF text is empty. Upload a text-based PDF or use an image-capable AI model for photos/scans.");
            }
            return text;
        }
    }

    private String limitText(String text) {
        String cleaned = text.replaceAll("\\s+", " ").trim();
        int maxChars = aiProperties.maxDocumentChars();
        return cleaned.length() <= maxChars ? cleaned : cleaned.substring(0, maxChars);
    }

    private Map<String, Object> jsonSchemaResponseFormat() {
        Map<String, Object> optionSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "optionText", Map.of("type", "string"),
                        "correct", Map.of("type", "boolean")
                ),
                "required", List.of("optionText", "correct")
        );
        Map<String, Object> questionSchema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "stem", Map.of("type", "string"),
                        "explanation", Map.of("type", "string"),
                        "options", Map.of("type", "array", "items", optionSchema)
                ),
                "required", List.of("stem", "explanation", "options")
        );
        Map<String, Object> schema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "categoryMatch", Map.of("type", "boolean"),
                        "reason", Map.of("type", "string"),
                        "questions", Map.of("type", "array", "items", questionSchema)
                ),
                "required", List.of("categoryMatch", "reason", "questions")
        );
        return Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", "quiz_question_generation",
                        "schema", schema
                )
        );
    }

    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BadRequestException("AI provider did not return complete JSON. Try fewer questions or increase app.ai.max-tokens.");
        }
        return content.substring(start, end + 1);
    }

    private boolean isValidQuestion(AiGeneratedQuestion question, int optionsPerQuestion) {
        if (question == null || question.stem() == null || question.stem().isBlank()) {
            return false;
        }
        if (question.options() == null || question.options().size() != optionsPerQuestion) {
            return false;
        }
        long correctCount = question.options().stream()
                .filter(option -> option.optionText() != null && !option.optionText().isBlank())
                .filter(option -> Boolean.TRUE.equals(option.correct()))
                .count();
        return correctCount == 1;
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }

    private record UploadedAiSource(String text, String imageDataUrl) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LmStudioResponse(List<LmStudioChoice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LmStudioChoice(LmStudioMessage message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LmStudioMessage(String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AiGeneratedQuestionList(Boolean categoryMatch, String reason, List<AiGeneratedQuestion> questions) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AiGeneratedQuestion(String stem, String explanation, List<AiGeneratedOption> options) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AiGeneratedOption(String optionText, Boolean correct) {
    }
}
