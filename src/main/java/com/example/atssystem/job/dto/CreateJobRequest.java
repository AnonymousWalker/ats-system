package com.example.atssystem.job.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateJobRequest(
		@NotBlank(message = "Job description text is required")
		String text
) {
}
