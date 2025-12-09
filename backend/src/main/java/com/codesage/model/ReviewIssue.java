package com.codesage.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing an individual issue found during code review.
 * Each issue belongs to a Review and contains details about the problem.
 */
@Entity
@Table(name = "review_issues")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    /**
     * Type of issue: SECURITY, PERFORMANCE, BUG, CODE_QUALITY, DOCUMENTATION
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueType type;

    /**
     * Severity: CRITICAL, HIGH, MEDIUM, LOW, INFO
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueSeverity severity;

    /**
     * File path where the issue was found
     */
    @Column(nullable = false, length = 500)
    private String filePath;

    /**
     * Line number where the issue starts (nullable for file-level issues)
     */
    private Integer lineNumber;

    /**
     * Issue title/summary
     */
    @Column(nullable = false, length = 500)
    private String title;

    /**
     * Detailed description of the issue
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * AI-suggested fix or recommendation
     */
    @Column(columnDefinition = "TEXT")
    private String suggestion;

    /**
     * Code snippet that has the issue (optional)
     */
    @Column(columnDefinition = "TEXT")
    private String codeSnippet;

    public enum IssueType {
        SECURITY,
        PERFORMANCE,
        BUG,
        CODE_QUALITY,
        DOCUMENTATION,
        BEST_PRACTICE
    }

    public enum IssueSeverity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW,
        INFO
    }
}
