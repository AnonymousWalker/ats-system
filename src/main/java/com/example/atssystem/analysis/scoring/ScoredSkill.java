package com.example.atssystem.analysis.scoring;

import com.example.atssystem.skill.domain.SkillPriority;

import java.util.List;
import java.util.UUID;

public record ScoredSkill(
		UUID skillId,
		String name,
		SkillPriority priority,
		String evidence
) {
}
