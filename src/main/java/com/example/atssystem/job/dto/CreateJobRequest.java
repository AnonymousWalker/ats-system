package com.example.atssystem.job.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateJobRequest(
		@NotBlank(message = "Job description text is required")
		@Size(max = 200_000, message = "Job description text must be at most 200000 characters")
		String text
) {
}
