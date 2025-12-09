package com.codesage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ReviewIssue responses to the frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewIssueDTO {
    private Long id;
    private String type;
    private String severity;
    private String filePath;
    private Integer lineNumber;
    private String title;
    private String description;
    private String suggestion;
    private String codeSnippet;
}
