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
@Table(name = "analysis_skills")
public class AnalysisSkill {

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

	@Column(name = "priority_ambiguous", nullable = false)
	private boolean priorityAmbiguous;

	@Column(columnDefinition = "TEXT")
	private String evidence;

	@Column(name = "matched_term", nullable = false, length = 128)
	private String matchedTerm;

	protected AnalysisSkill() {
	}

	public AnalysisSkill(
			UUID id,
			Analysis analysis,
			Skill skill,
			SkillSource source,
			SkillPriority priority,
			boolean priorityAmbiguous,
			String evidence,
			String matchedTerm
	) {
		this.id = id;
		this.analysis = analysis;
		this.skill = skill;
		this.source = source;
		this.priority = priority;
		this.priorityAmbiguous = priorityAmbiguous;
		this.evidence = evidence;
		this.matchedTerm = matchedTerm;
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

	public boolean isPriorityAmbiguous() {
		return priorityAmbiguous;
	}

	public String getEvidence() {
		return evidence;
	}

	public String getMatchedTerm() {
		return matchedTerm;
	}
}
