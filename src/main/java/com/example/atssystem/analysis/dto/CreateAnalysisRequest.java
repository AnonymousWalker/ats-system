package com.example.atssystem.analysis.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAnalysisRequest(
		@NotNull(message = "resumeId is required")
		UUID resumeId,

		@NotNull(message = "jobId is required")
		UUID jobId
) {
}
