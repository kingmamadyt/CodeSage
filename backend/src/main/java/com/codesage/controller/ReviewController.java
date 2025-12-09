package com.codesage.controller;

import com.codesage.dto.DashboardStatsDTO;
import com.codesage.dto.ReviewDTO;
import com.codesage.dto.ReviewIssueDTO;
import com.codesage.model.Review;
import com.codesage.model.ReviewIssue;
import com.codesage.repository.ReviewIssueRepository;
import com.codesage.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for review and dashboard APIs.
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final ReviewIssueRepository reviewIssueRepository;

    /**
     * Get all reviews with pagination
     */
    @GetMapping
    public ResponseEntity<Page<ReviewDTO>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching reviews: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviews = reviewRepository.findAll(pageable);
        Page<ReviewDTO> reviewDTOs = reviews.map(this::convertToDTO);

        return ResponseEntity.ok(reviewDTOs);
    }

    /**
     * Get specific review by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReviewDTO> getReviewById(@PathVariable Long id) {
        log.info("Fetching review with id: {}", id);

        return reviewRepository.findById(id)
                .map(this::convertToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get reviews for a specific repository
     */
    @GetMapping("/repo/{owner}/{name}")
    public ResponseEntity<Page<ReviewDTO>> getReviewsByRepository(
            @PathVariable String owner,
            @PathVariable String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching reviews for repository: {}/{}", owner, name);

        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviews = reviewRepository.findByRepositoryOwnerAndRepositoryNameOrderByCreatedAtDesc(
                owner, name, pageable);
        Page<ReviewDTO> reviewDTOs = reviews.map(this::convertToDTO);

        return ResponseEntity.ok(reviewDTOs);
    }

    /**
     * Get dashboard statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        log.info("Fetching dashboard statistics");

        Long totalReviews = reviewRepository.getTotalReviewCount();
        Long activePRs = reviewRepository.getActivePRCount();
        Double avgScore = reviewRepository.getAverageQualityScore();
        Long totalIssues = reviewRepository.getTotalIssuesCount();

        // Count issues by severity
        List<ReviewIssue> criticalIssues = reviewIssueRepository.findBySeverity(ReviewIssue.IssueSeverity.CRITICAL);
        List<ReviewIssue> highIssues = reviewIssueRepository.findBySeverity(ReviewIssue.IssueSeverity.HIGH);
        List<ReviewIssue> mediumIssues = reviewIssueRepository.findBySeverity(ReviewIssue.IssueSeverity.MEDIUM);
        List<ReviewIssue> lowIssues = reviewIssueRepository.findBySeverity(ReviewIssue.IssueSeverity.LOW);

        DashboardStatsDTO stats = DashboardStatsDTO.builder()
                .totalReviews(totalReviews)
                .activePRs(activePRs)
                .avgQualityScore(avgScore != null ? avgScore : 0.0)
                .issuesFound(totalIssues)
                .criticalIssues((long) criticalIssues.size())
                .highIssues((long) highIssues.size())
                .mediumIssues((long) mediumIssues.size())
                .lowIssues((long) lowIssues.size())
                .build();

        return ResponseEntity.ok(stats);
    }

    /**
     * Get recent reviews (last 7 days)
     */
    @GetMapping("/recent")
    public ResponseEntity<List<ReviewDTO>> getRecentReviews() {
        log.info("Fetching recent reviews");

        java.time.Instant sevenDaysAgo = java.time.Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS);
        List<Review> reviews = reviewRepository.findRecentReviews(sevenDaysAgo);
        List<ReviewDTO> reviewDTOs = reviews.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(reviewDTOs);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<java.util.Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(java.util.Map.of(
                "status", "UP",
                "service", "CodeSage Review API",
                "timestamp", java.time.Instant.now().toString(),
                "totalReviews", reviewRepository.count()));
    }

    /**
     * Convert Review entity to DTO
     */
    private ReviewDTO convertToDTO(Review review) {
        List<ReviewIssueDTO> issueDTOs = review.getIssues().stream()
                .map(this::convertIssueToDTO)
                .collect(Collectors.toList());

        return ReviewDTO.builder()
                .id(review.getId())
                .repositoryOwner(review.getRepositoryOwner())
                .repositoryName(review.getRepositoryName())
                .prNumber(review.getPrNumber())
                .prTitle(review.getPrTitle())
                .prAuthor(review.getPrAuthor())
                .prUrl(review.getPrUrl())
                .qualityScore(review.getQualityScore())
                .analysisSummary(review.getAnalysisSummary())
                .aiProvider(review.getAiProvider())
                .aiModel(review.getAiModel())
                .status(review.getStatus().name())
                .issues(issueDTOs)
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .errorMessage(review.getErrorMessage())
                .build();
    }

    /**
     * Convert ReviewIssue entity to DTO
     */
    private ReviewIssueDTO convertIssueToDTO(ReviewIssue issue) {
        return ReviewIssueDTO.builder()
                .id(issue.getId())
                .type(issue.getType().name())
                .severity(issue.getSeverity().name())
                .filePath(issue.getFilePath())
                .lineNumber(issue.getLineNumber())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .suggestion(issue.getSuggestion())
                .codeSnippet(issue.getCodeSnippet())
                .build();
    }

    public static org.slf4j.Logger getLog() {
        return log;
    }

    public ReviewRepository getReviewRepository() {
        return reviewRepository;
    }

    public ReviewIssueRepository getReviewIssueRepository() {
        return reviewIssueRepository;
    }
}
