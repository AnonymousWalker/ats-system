package com.example.atssystem.analysis.entity;

import com.example.atssystem.analysis.domain.AnalysisStatus;
import com.example.atssystem.analysis.scoring.ScoringResult;
import com.example.atssystem.job.entity.JobDescription;
import com.example.atssystem.resume.entity.Resume;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analyses")
public class Analysis {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "resume_id", nullable = false)
	private Resume resume;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "job_id", nullable = false)
	private JobDescription jobDescription;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private AnalysisStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column
	private Integer score;

	@Column
	private Boolean scorable;

	@Column(name = "matched_weight")
	private Integer matchedWeight;

	@Column(name = "total_weight")
	private Integer totalWeight;

	@Column(name = "required_matched")
	private Integer requiredMatched;

	@Column(name = "required_total")
	private Integer requiredTotal;

	@Column(name = "preferred_matched")
	private Integer preferredMatched;

	@Column(name = "preferred_total")
	private Integer preferredTotal;

	@Column(name = "scoring_version", length = 32)
	private String scoringVersion;

	@Column(name = "catalog_version", length = 32)
	private String catalogVersion;

	@Column(name = "reviewed_at")
	private Instant reviewedAt;

	protected Analysis() {
	}

	public Analysis(
			UUID id,
			Resume resume,
			JobDescription jobDescription,
			AnalysisStatus status,
			Instant createdAt,
			Instant expiresAt
	) {
		this.id = id;
		this.resume = resume;
		this.jobDescription = jobDescription;
		this.status = status;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}

	public void applyReview(ScoringResult result, Instant reviewedAt) {
		this.status = AnalysisStatus.REVIEWED;
		this.reviewedAt = reviewedAt;
		this.scorable = result.scorable();
		this.score = result.score();
		this.matchedWeight = result.matchedWeight();
		this.totalWeight = result.totalWeight();
		this.requiredMatched = result.requiredCoverage().matched();
		this.requiredTotal = result.requiredCoverage().total();
		this.preferredMatched = result.preferredCoverage().matched();
		this.preferredTotal = result.preferredCoverage().total();
		this.scoringVersion = result.scoringVersion();
		this.catalogVersion = result.catalogVersion();
	}

	public UUID getId() {
		return id;
	}

	public Resume getResume() {
		return resume;
	}

	public JobDescription getJobDescription() {
		return jobDescription;
	}

	public AnalysisStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Integer getScore() {
		return score;
	}

	public Boolean getScorable() {
		return scorable;
	}

	public Integer getMatchedWeight() {
		return matchedWeight;
	}

	public Integer getTotalWeight() {
		return totalWeight;
	}

	public Integer getRequiredMatched() {
		return requiredMatched;
	}

	public Integer getRequiredTotal() {
		return requiredTotal;
	}

	public Integer getPreferredMatched() {
		return preferredMatched;
	}

	public Integer getPreferredTotal() {
		return preferredTotal;
	}

	public String getScoringVersion() {
		return scoringVersion;
	}

	public String getCatalogVersion() {
		return catalogVersion;
	}

	public Instant getReviewedAt() {
		return reviewedAt;
	}
}
