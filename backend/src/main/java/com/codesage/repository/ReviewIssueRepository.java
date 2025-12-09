package com.codesage.repository;

import com.codesage.model.ReviewIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ReviewIssue entity with queries for issue analytics.
 */
@Repository
public interface ReviewIssueRepository extends JpaRepository<ReviewIssue, Long> {

    /**
     * Find all issues for a specific review
     */
    List<ReviewIssue> findByReviewIdOrderBySeverityDesc(Long reviewId);

    /**
     * Find issues by type
     */
    List<ReviewIssue> findByType(ReviewIssue.IssueType type);

    /**
     * Find issues by severity
     */
    List<ReviewIssue> findBySeverity(ReviewIssue.IssueSeverity severity);

    /**
     * Count issues by type
     */
    @Query("SELECT i.type, COUNT(i) FROM ReviewIssue i GROUP BY i.type")
    List<Object[]> countByType();

    /**
     * Count issues by severity
     */
    @Query("SELECT i.severity, COUNT(i) FROM ReviewIssue i GROUP BY i.severity")
    List<Object[]> countBySeverity();

    /**
     * Find critical and high severity issues
     */
    @Query("SELECT i FROM ReviewIssue i WHERE i.severity IN ('CRITICAL', 'HIGH') ORDER BY i.severity DESC")
    List<ReviewIssue> findCriticalIssues();

    /**
     * Count issues for a specific review
     */
    long countByReviewId(Long reviewId);
}
