package com.example.atssystem.analysis.dto;

import com.example.atssystem.skill.domain.SkillPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReviewAnalysisRequest(
		@NotNull(message = "resumeSkills is required")
		@Valid
		List<ReviewedResumeSkillRequest> resumeSkills,

		@NotNull(message = "jobSkills is required")
		@Valid
		List<ReviewedJobSkillRequest> jobSkills
) {
	public record ReviewedResumeSkillRequest(
			@NotNull(message = "skillId is required")
			UUID skillId,
			String evidence
	) {
	}

	public record ReviewedJobSkillRequest(
			@NotNull(message = "skillId is required")
			UUID skillId,
			@NotNull(message = "priority is required")
			SkillPriority priority,
			String evidence
	) {
	}
}
