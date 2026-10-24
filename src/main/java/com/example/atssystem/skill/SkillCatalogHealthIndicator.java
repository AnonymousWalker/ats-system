package com.example.atssystem.skill;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Readiness check for data the application needs in order to score analyses.
 * The standard datasource indicator separately verifies database connectivity.
 */
@Component
public class SkillCatalogHealthIndicator implements HealthIndicator {

	private final SkillRepository skillRepository;

	public SkillCatalogHealthIndicator(SkillRepository skillRepository) {
		this.skillRepository = skillRepository;
	}

	@Override
	public Health health() {
		long skillCount = skillRepository.count();
		if (skillCount == 0) {
			return Health.down()
					.withDetail("reason", "Skill catalog is empty")
					.build();
		}
		return Health.up()
				.withDetail("skillCount", skillCount)
				.build();
	}
}
