package com.example.atssystem.analysis.dto;

import com.example.atssystem.analysis.scoring.CoverageBreakdown;
import com.example.atssystem.skill.domain.SkillPriority;

import java.util.List;
import java.util.UUID;

public record AnalysisResultResponse(
		boolean scorable,
		Integer score,
		int matchedWeight,
		int totalWeight,
		String scoringVersion,
		String catalogVersion,
		List<ResultSkillResponse> matchedSkills,
		List<ResultSkillResponse> missingSkills,
		CoverageBreakdown requiredCoverage,
		CoverageBreakdown preferredCoverage
) {
	public record ResultSkillResponse(
			UUID skillId,
			String name,
			SkillPriority priority,
			String evidence
	) {
	}
}
