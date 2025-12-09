package com.codesage.queue;

import com.codesage.model.Review;
import com.codesage.repository.ReviewRepository;
import com.codesage.service.AIService;
import com.codesage.service.GitHubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * RabbitMQ consumer that processes PR analysis requests.
 * Orchestrates AI analysis, database persistence, and GitHub comment posting.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalysisConsumer {

    private final AIService aiService;
    private final GitHubService githubService;
    private final ReviewRepository reviewRepository;

    @RabbitListener(queues = "analysis-queue")
    public void handleAnalysisRequest(Map<String, Object> payload) {
        log.info("Received analysis request from queue");

        try {
            // Extract PR details from webhook payload
            Map<String, Object> pullRequest = (Map<String, Object>) payload.get("pull_request");
            if (pullRequest == null) {
                log.warn("Payload is not a Pull Request event, skipping");
                return;
            }

            String action = (String) payload.get("action");
            if (!"opened".equals(action) && !"synchronize".equals(action)) {
                log.info("Ignoring PR action: {}", action);
                return;
            }

            // Extract PR metadata
            Integer prNumber = (Integer) pullRequest.get("number");
            String prTitle = (String) pullRequest.get("title");
            
            Map<String, Object> user = (Map<String, Object>) pullRequest.get("user");
            String prAuthor = (String) user.get("login");
            
            Map<String, Object> base = (Map<String, Object>) pullRequest.get("base");
            Map<String, Object> repo = (Map<String, Object>) base.get("repo");
            String repoName = (String) repo.get("name");
            
            Map<String, Object> owner = (Map<String, Object>) repo.get("owner");
            String repoOwner = (String) owner.get("login");
            
            String prUrl = (String) pullRequest.get("html_url");

            log.info("Processing PR: {}/{} #{} - {}", repoOwner, repoName, prNumber, prTitle);

            // Check if review already exists
            var existingReview = reviewRepository.findByRepositoryOwnerAndRepositoryNameAndPrNumber(
                    repoOwner, repoName, prNumber);
            
            if (existingReview.isPresent()) {
                log.info("Review already exists for PR #{}, skipping", prNumber);
                return;
            }

            // Create pending review
            Review review = Review.builder()
                    .repositoryOwner(repoOwner)
                    .repositoryName(repoName)
                    .prNumber(prNumber)
                    .prTitle(prTitle)
                    .prAuthor(prAuthor)
                    .prUrl(prUrl)
                    .status(Review.ReviewStatus.PENDING)
                    .qualityScore(0.0)
                    .aiProvider("Pending")
                    .aiModel("Pending")
                    .build();
            
            review = reviewRepository.save(review);
            log.info("Created pending review with ID: {}", review.getId());

            // Fetch PR diff from GitHub
            String diff = githubService.fetchPRDiff(repoOwner, repoName, prNumber);
            
            // Perform AI analysis
            Review analyzedReview = aiService.analyzeCode(diff, repoName, prNumber, prTitle);
            
            // Update review with analysis results
            review.setQualityScore(analyzedReview.getQualityScore());
            review.setAnalysisSummary(analyzedReview.getAnalysisSummary());
            review.setAiProvider(analyzedReview.getAiProvider());
            review.setAiModel(analyzedReview.getAiModel());
            review.setStatus(Review.ReviewStatus.COMPLETED);
            
            // Add issues
            analyzedReview.getIssues().forEach(review::addIssue);
            
            // Save complete review
            review = reviewRepository.save(review);
            log.info("Saved completed review with {} issues", review.getIssues().size());

            // Post comment to GitHub
            String comment = githubService.formatReviewComment(review);
            githubService.postComment(repoOwner, repoName, prNumber, comment);
            
            log.info("Successfully completed analysis for PR #{}", prNumber);

        } catch (Exception e) {
            log.error("Failed to process analysis request", e);
            
            // Try to save failed review if we have enough info
            try {
                Map<String, Object> pullRequest = (Map<String, Object>) payload.get("pull_request");
                if (pullRequest != null) {
                    Integer prNumber = (Integer) pullRequest.get("number");
                    String prTitle = (String) pullRequest.get("title");
                    
                    Map<String, Object> base = (Map<String, Object>) pullRequest.get("base");
                    Map<String, Object> repo = (Map<String, Object>) base.get("repo");
                    String repoName = (String) repo.get("name");
                    Map<String, Object> owner = (Map<String, Object>) repo.get("owner");
                    String repoOwner = (String) owner.get("login");
                    
                    Review failedReview = Review.builder()
                            .repositoryOwner(repoOwner)
                            .repositoryName(repoName)
                            .prNumber(prNumber)
                            .prTitle(prTitle)
                            .status(Review.ReviewStatus.FAILED)
                            .errorMessage(e.getMessage())
                            .qualityScore(0.0)
                            .aiProvider("N/A")
                            .aiModel("N/A")
                            .build();
                    
                    reviewRepository.save(failedReview);
                    log.info("Saved failed review for PR #{}", prNumber);
                }
            } catch (Exception saveError) {
                log.error("Failed to save error review", saveError);
            }
        }
    }
}
