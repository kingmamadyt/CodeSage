package com.codesage.service;

import com.codesage.exception.AIServiceException;
import com.codesage.model.Review;
import com.codesage.model.ReviewIssue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Service for AI-powered code analysis using OpenAI GPT-4 or Claude.
 * Implements retry logic, fallback mechanisms, and structured response parsing.
 */
@Service
@Slf4j
public class AIService {

    private final WebClient openAIClient;
    private final WebClient claudeClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key:#{null}}")
    private String openAIKey;

    @Value("${claude.api.key:#{null}}")
    private String claudeKey;

    @Value("${ai.model.openai:gpt-4}")
    private String openAIModel;

    @Value("${ai.model.claude:claude-3-sonnet-20240229}")
    private String claudeModel;

    public AIService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        // OpenAI client
        this.openAIClient = webClientBuilder
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        // Claude client
        this.claudeClient = webClientBuilder
                .baseUrl("https://api.anthropic.com/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
    }

    /**
     * Analyze code diff and return structured review
     */
    public Review analyzeCode(String codeDiff, String repositoryName, Integer prNumber, String prTitle) {
        log.info("Starting AI analysis for PR #{} in {}", prNumber, repositoryName);

        try {
            String analysis = performAnalysis(codeDiff);
            return parseAnalysisToReview(analysis, repositoryName, prNumber, prTitle);
        } catch (Exception e) {
            log.error("AI analysis failed for PR #{}: {}", prNumber, e.getMessage(), e);
            throw new AIServiceException("Failed to analyze code: " + e.getMessage(), e);
        }
    }

    /**
     * Perform analysis using OpenAI (with Claude fallback)
     */
    private String performAnalysis(String codeDiff) {
        // Try OpenAI first
        if (openAIKey != null && !openAIKey.isEmpty()) {
            try {
                return callOpenAI(codeDiff);
            } catch (Exception e) {
                log.warn("OpenAI analysis failed, trying Claude fallback: {}", e.getMessage());
            }
        }

        // Fallback to Claude
        if (claudeKey != null && !claudeKey.isEmpty()) {
            try {
                return callClaude(codeDiff);
            } catch (Exception e) {
                log.error("Claude analysis also failed: {}", e.getMessage());
                throw new AIServiceException("All AI providers failed", e);
            }
        }

        // No API keys configured - return mock analysis for development
        log.warn("No AI API keys configured. Returning mock analysis.");
        return getMockAnalysis();
    }

    /**
     * Call OpenAI GPT-4 API
     */
    private String callOpenAI(String codeDiff) {
        log.info("Calling OpenAI API with model: {}", openAIModel);

        String prompt = buildAnalysisPrompt(codeDiff);

        Map<String, Object> requestBody = Map.of(
                "model", openAIModel,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "You are an expert code reviewer. Analyze code and provide structured feedback in JSON format."),
                        Map.of("role", "user", "content", prompt)),
                "temperature", 0.3,
                "max_tokens", 2000);

        try {
            String response = openAIClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAIKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return extractOpenAIResponse(response);
        } catch (WebClientResponseException e) {
            log.error("OpenAI API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AIServiceException("OpenAI API error: " + e.getMessage(), e);
        }
    }

    /**
     * Call Claude API
     */
    private String callClaude(String codeDiff) {
        log.info("Calling Claude API with model: {}", claudeModel);

        String prompt = buildAnalysisPrompt(codeDiff);

        Map<String, Object> requestBody = Map.of(
                "model", claudeModel,
                "max_tokens", 2000,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)));

        try {
            String response = claudeClient.post()
                    .uri("/messages")
                    .header("x-api-key", claudeKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return extractClaudeResponse(response);
        } catch (WebClientResponseException e) {
            log.error("Claude API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AIServiceException("Claude API error: " + e.getMessage(), e);
        }
    }

    /**
     * Build structured analysis prompt
     */
    private String buildAnalysisPrompt(String codeDiff) {
        return String.format("""
                Analyze the following code diff and provide a structured code review.

                Return your analysis in JSON format with this structure:
                {
                  "qualityScore": <number 0-10>,
                  "summary": "<brief overall assessment>",
                  "issues": [
                    {
                      "type": "<SECURITY|PERFORMANCE|BUG|CODE_QUALITY|DOCUMENTATION|BEST_PRACTICE>",
                      "severity": "<CRITICAL|HIGH|MEDIUM|LOW|INFO>",
                      "file": "<file path>",
                      "line": <line number or null>,
                      "title": "<short title>",
                      "description": "<detailed description>",
                      "suggestion": "<how to fix>"
                    }
                  ],
                  "strengths": ["<positive aspect 1>", "<positive aspect 2>"]
                }

                Code Diff:
                ```
                %s
                ```

                Focus on:
                - Security vulnerabilities
                - Performance issues
                - Potential bugs
                - Code quality and maintainability
                - Best practices
                - Documentation

                Be concise but thorough. Provide actionable suggestions.
                """, codeDiff);
    }

    /**
     * Extract response from OpenAI API
     */
    private String extractOpenAIResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response", e);
            throw new AIServiceException("Failed to parse AI response", e);
        }
    }

    /**
     * Extract response from Claude API
     */
    private String extractClaudeResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            return root.path("content").get(0).path("text").asText();
        } catch (Exception e) {
            log.error("Failed to parse Claude response", e);
            throw new AIServiceException("Failed to parse AI response", e);
        }
    }

    /**
     * Parse AI analysis response into Review object
     */
    private Review parseAnalysisToReview(String analysis, String repositoryName, Integer prNumber, String prTitle) {
        try {
            // Extract JSON from response (may be wrapped in markdown code blocks)
            String jsonContent = extractJSON(analysis);
            JsonNode root = objectMapper.readTree(jsonContent);

            Review review = Review.builder()
                    .repositoryName(repositoryName)
                    .prNumber(prNumber)
                    .prTitle(prTitle)
                    .qualityScore(root.path("qualityScore").asDouble(7.0))
                    .analysisSummary(root.path("summary").asText("Code review completed"))
                    .aiProvider(openAIKey != null ? "OpenAI" : "Claude")
                    .aiModel(openAIKey != null ? openAIModel : claudeModel)
                    .status(Review.ReviewStatus.COMPLETED)
                    .build();

            // Parse issues
            JsonNode issuesNode = root.path("issues");
            if (issuesNode.isArray()) {
                for (JsonNode issueNode : issuesNode) {
                    ReviewIssue issue = ReviewIssue.builder()
                            .type(ReviewIssue.IssueType.valueOf(issueNode.path("type").asText("CODE_QUALITY")))
                            .severity(ReviewIssue.IssueSeverity.valueOf(issueNode.path("severity").asText("MEDIUM")))
                            .filePath(issueNode.path("file").asText("unknown"))
                            .lineNumber(issueNode.path("line").isNull() ? null : issueNode.path("line").asInt())
                            .title(issueNode.path("title").asText())
                            .description(issueNode.path("description").asText())
                            .suggestion(issueNode.path("suggestion").asText())
                            .build();
                    review.addIssue(issue);
                }
            }

            return review;
        } catch (Exception e) {
            log.error("Failed to parse AI analysis", e);
            throw new AIServiceException("Failed to parse AI analysis", e);
        }
    }

    /**
     * Extract JSON from markdown code blocks
     */
    private String extractJSON(String text) {
        // Remove markdown code blocks if present
        String cleaned = text.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    /**
     * Mock analysis for development without API keys
     */
    private String getMockAnalysis() {
        return """
                {
                  "qualityScore": 8.5,
                  "summary": "Overall good code quality with minor improvements needed",
                  "issues": [
                    {
                      "type": "CODE_QUALITY",
                      "severity": "MEDIUM",
                      "file": "src/main/java/Example.java",
                      "line": 42,
                      "title": "Consider extracting method",
                      "description": "This method is doing too many things. Consider extracting logic into separate methods.",
                      "suggestion": "Break down into smaller, focused methods for better maintainability"
                    }
                  ],
                  "strengths": ["Good test coverage", "Clear variable naming", "Proper error handling"]
                }
                """;
    }
}
