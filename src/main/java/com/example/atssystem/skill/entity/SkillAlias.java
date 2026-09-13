package com.example.atssystem.skill.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "skill_aliases")
public class SkillAlias {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "skill_id", nullable = false)
	private Skill skill;

	@Column(nullable = false, unique = true, length = 128)
	private String alias;

	protected SkillAlias() {
	}

	public SkillAlias(UUID id, Skill skill, String alias) {
		this.id = id;
		this.skill = skill;
		this.alias = alias;
	}

	public UUID getId() {
		return id;
	}

	public Skill getSkill() {
		return skill;
	}

	public String getAlias() {
		return alias;
	}
}
