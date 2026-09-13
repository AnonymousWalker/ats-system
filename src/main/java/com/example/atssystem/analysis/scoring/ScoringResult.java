package com.example.atssystem.analysis.scoring;

import java.util.List;

public record ScoringResult(
		boolean scorable,
		Integer score,
		int matchedWeight,
		int totalWeight,
		String scoringVersion,
		String catalogVersion,
		List<ScoredSkill> matchedSkills,
		List<ScoredSkill> missingSkills,
		CoverageBreakdown requiredCoverage,
		CoverageBreakdown preferredCoverage
) {
}
