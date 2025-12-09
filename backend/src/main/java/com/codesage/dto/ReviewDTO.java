package com.codesage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO for Review responses to the frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {
    private Long id;
    private String repositoryOwner;
    private String repositoryName;
    private Integer prNumber;
    private String prTitle;
    private String prAuthor;
    private String prUrl;
    private Double qualityScore;
    private String analysisSummary;
    private String aiProvider;
    private String aiModel;
    private String status;
    private List<ReviewIssueDTO> issues;
    private Instant createdAt;
    private Instant updatedAt;
    private String errorMessage;
}
