package com.codesage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for dashboard statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private Long totalReviews;
    private Long activePRs;
    private Double avgQualityScore;
    private Long issuesFound;
    private Long criticalIssues;
    private Long highIssues;
    private Long mediumIssues;
    private Long lowIssues;
}
