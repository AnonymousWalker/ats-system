package com.example.atssystem.skill.entity;

import com.example.atssystem.analysis.entity.Analysis;
import com.example.atssystem.skill.domain.SkillPriority;
import com.example.atssystem.skill.domain.SkillSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "analysis_reviewed_skills")
public class AnalysisReviewedSkill {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "analysis_id", nullable = false)
	private Analysis analysis;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "skill_id", nullable = false)
	private Skill skill;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private SkillSource source;

	@Enumerated(EnumType.STRING)
	@Column(length = 16)
	private SkillPriority priority;

	@Column(columnDefinition = "TEXT")
	private String evidence;

	protected AnalysisReviewedSkill() {
	}

	public AnalysisReviewedSkill(
			UUID id,
			Analysis analysis,
			Skill skill,
			SkillSource source,
			SkillPriority priority,
			String evidence
	) {
		this.id = id;
		this.analysis = analysis;
		this.skill = skill;
		this.source = source;
		this.priority = priority;
		this.evidence = evidence;
	}

	public UUID getId() {
		return id;
	}

	public Analysis getAnalysis() {
		return analysis;
	}

	public Skill getSkill() {
		return skill;
	}

	public SkillSource getSource() {
		return source;
	}

	public SkillPriority getPriority() {
		return priority;
	}

	public String getEvidence() {
		return evidence;
	}
}
