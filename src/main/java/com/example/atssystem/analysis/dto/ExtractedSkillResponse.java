package com.example.atssystem.analysis.dto;

import com.example.atssystem.skill.domain.SkillPriority;

import java.util.UUID;

public record ExtractedSkillResponse(
		UUID skillId,
		String name,
		String matchedTerm,
		String evidence,
		SkillPriority priority,
		boolean priorityAmbiguous
) {
}
