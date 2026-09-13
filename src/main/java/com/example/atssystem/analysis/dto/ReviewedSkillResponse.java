package com.example.atssystem.analysis.dto;

import com.example.atssystem.skill.domain.SkillPriority;

import java.util.UUID;

public record ReviewedSkillResponse(
		UUID skillId,
		String name,
		SkillPriority priority,
		String evidence
) {
}
