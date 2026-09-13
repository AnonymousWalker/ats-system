package com.example.atssystem.skill.extraction;

import com.example.atssystem.skill.domain.SkillPriority;

import java.util.UUID;

/**
 * One extracted skill occurrence after deduplication.
 */
public record ExtractedSkill(
		UUID skillId,
		String canonicalName,
		String matchedTerm,
		String evidence,
		SkillPriority priority,
		boolean priorityAmbiguous
) {
}
