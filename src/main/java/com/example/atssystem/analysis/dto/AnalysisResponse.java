package com.example.atssystem.analysis.dto;

import com.example.atssystem.analysis.domain.AnalysisStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnalysisResponse(
		UUID id,
		UUID resumeId,
		UUID jobId,
		AnalysisStatus status,
		Instant createdAt,
		Instant reviewedAt,
		List<ExtractedSkillResponse> resumeSkills,
		List<ExtractedSkillResponse> jobSkills,
		List<ReviewedSkillResponse> reviewedResumeSkills,
		List<ReviewedSkillResponse> reviewedJobSkills,
		AnalysisResultResponse result
) {
}
