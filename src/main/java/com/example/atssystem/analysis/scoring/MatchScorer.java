package com.example.atssystem.analysis.scoring;

import com.example.atssystem.skill.domain.SkillPriority;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class MatchScorer {

	private MatchScorer() {
	}

	public static ScoringResult score(List<ScoredSkill> resumeSkills, List<ScoredSkill> jobSkills) {
		if (jobSkills.isEmpty()) {
			return unscorable();
		}

		Set<UUID> resumeIds = new HashSet<>();
		for (ScoredSkill skill : resumeSkills) {
			resumeIds.add(skill.skillId());
		}

		List<ScoredSkill> matched = new ArrayList<>();
		List<ScoredSkill> missing = new ArrayList<>();
		int matchedWeight = 0;
		int totalWeight = 0;
		int requiredMatched = 0;
		int requiredTotal = 0;
		int preferredMatched = 0;
		int preferredTotal = 0;

		for (ScoredSkill jobSkill : jobSkills) {
			int weight = weightOf(jobSkill.priority());
			totalWeight += weight;

			if (jobSkill.priority() == SkillPriority.REQUIRED) {
				requiredTotal++;
			} else {
				preferredTotal++;
			}

			if (resumeIds.contains(jobSkill.skillId())) {
				matched.add(jobSkill);
				matchedWeight += weight;
				if (jobSkill.priority() == SkillPriority.REQUIRED) {
					requiredMatched++;
				} else {
					preferredMatched++;
				}
			} else {
				missing.add(jobSkill);
			}
		}

		matched.sort(byNameThenPriority());
		missing.sort(byNameThenPriority());

		int score = (int) Math.round(100.0 * matchedWeight / totalWeight);

		return new ScoringResult(
				true,
				score,
				matchedWeight,
				totalWeight,
				ScoringVersions.SCORING_VERSION,
				ScoringVersions.CATALOG_VERSION,
				List.copyOf(matched),
				List.copyOf(missing),
				CoverageBreakdown.of(requiredMatched, requiredTotal),
				CoverageBreakdown.of(preferredMatched, preferredTotal)
		);
	}

	private static ScoringResult unscorable() {
		return new ScoringResult(
				false,
				null,
				0,
				0,
				ScoringVersions.SCORING_VERSION,
				ScoringVersions.CATALOG_VERSION,
				List.of(),
				List.of(),
				CoverageBreakdown.of(0, 0),
				CoverageBreakdown.of(0, 0)
		);
	}

	private static int weightOf(SkillPriority priority) {
		return priority == SkillPriority.REQUIRED
				? ScoringVersions.REQUIRED_WEIGHT
				: ScoringVersions.PREFERRED_WEIGHT;
	}

	private static Comparator<ScoredSkill> byNameThenPriority() {
		return Comparator
				.comparing(ScoredSkill::priority)
				.thenComparing(ScoredSkill::name, String.CASE_INSENSITIVE_ORDER);
	}
}
