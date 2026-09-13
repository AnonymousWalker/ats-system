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
@Table(name = "skills")
public class Skill {

	@Id
	private UUID id;

	@Column(name = "canonical_name", nullable = false, unique = true, length = 128)
	private String canonicalName;

	@Column(nullable = false, length = 64)
	private String category;

	protected Skill() {
	}

	public Skill(UUID id, String canonicalName, String category) {
		this.id = id;
		this.canonicalName = canonicalName;
		this.category = category;
	}

	public UUID getId() {
		return id;
	}

	public String getCanonicalName() {
		return canonicalName;
	}

	public String getCategory() {
		return category;
	}
}
