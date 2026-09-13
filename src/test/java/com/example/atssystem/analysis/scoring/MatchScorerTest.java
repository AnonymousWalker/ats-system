package com.example.atssystem.analysis.scoring;

import com.example.atssystem.skill.domain.SkillPriority;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MatchScorerTest {

	private static final UUID JAVA = UUID.fromString("11111111-1111-1111-1111-111111110001");
	private static final UUID POSTGRES = UUID.fromString("11111111-1111-1111-1111-11111111000c");
	private static final UUID DOCKER = UUID.fromString("11111111-1111-1111-1111-111111110016");

	@Test
	void oneRequiredMatchOutOfTwoRequiredAndOnePreferredIsFortyPercent() {
		List<ScoredSkill> resume = List.of(
				new ScoredSkill(JAVA, "Java", null, "Java evidence")
		);
		List<ScoredSkill> job = List.of(
				new ScoredSkill(JAVA, "Java", SkillPriority.REQUIRED, null),
				new ScoredSkill(POSTGRES, "PostgreSQL", SkillPriority.REQUIRED, null),
				new ScoredSkill(DOCKER, "Docker", SkillPriority.PREFERRED, null)
		);

		ScoringResult result = MatchScorer.score(resume, job);

		// total weight = 2 + 2 + 1 = 5; matched = 2; round(100 * 2/5) = 40
		assertThat(result.scorable()).isTrue();
		assertThat(result.score()).isEqualTo(40);
		assertThat(result.matchedWeight()).isEqualTo(2);
		assertThat(result.totalWeight()).isEqualTo(5);
		assertThat(result.matchedSkills()).extracting(ScoredSkill::name).containsExactly("Java");
		assertThat(result.missingSkills()).extracting(ScoredSkill::name)
				.containsExactlyInAnyOrder("PostgreSQL", "Docker");
		assertThat(result.requiredCoverage().matched()).isEqualTo(1);
		assertThat(result.requiredCoverage().total()).isEqualTo(2);
		assertThat(result.preferredCoverage().matched()).isEqualTo(0);
		assertThat(result.preferredCoverage().total()).isEqualTo(1);
		assertThat(result.scoringVersion()).isEqualTo(ScoringVersions.SCORING_VERSION);
		assertThat(result.catalogVersion()).isEqualTo(ScoringVersions.CATALOG_VERSION);
	}

	@Test
	void zeroJobSkillsIsUnscorable() {
		List<ScoredSkill> resume = List.of(
				new ScoredSkill(JAVA, "Java", null, null)
		);

		ScoringResult result = MatchScorer.score(resume, List.of());

		assertThat(result.scorable()).isFalse();
		assertThat(result.score()).isNull();
		assertThat(result.matchedWeight()).isZero();
		assertThat(result.totalWeight()).isZero();
		assertThat(result.matchedSkills()).isEmpty();
		assertThat(result.missingSkills()).isEmpty();
	}

	@Test
	void perfectMatchIsOneHundred() {
		List<ScoredSkill> resume = List.of(
				new ScoredSkill(JAVA, "Java", null, null),
				new ScoredSkill(DOCKER, "Docker", null, null)
		);
		List<ScoredSkill> job = List.of(
				new ScoredSkill(JAVA, "Java", SkillPriority.REQUIRED, null),
				new ScoredSkill(DOCKER, "Docker", SkillPriority.PREFERRED, null)
		);

		ScoringResult result = MatchScorer.score(resume, job);

		assertThat(result.score()).isEqualTo(100);
		assertThat(result.matchedWeight()).isEqualTo(3);
		assertThat(result.totalWeight()).isEqualTo(3);
	}
}
