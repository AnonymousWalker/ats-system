package com.example.atssystem.job.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_descriptions")
public class JobDescription {

	@Id
	private UUID id;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String text;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	protected JobDescription() {
	}

	public JobDescription(UUID id, String text, Instant createdAt, Instant expiresAt) {
		this.id = id;
		this.text = text;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}

	public UUID getId() {
		return id;
	}

	public String getText() {
		return text;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}
}
