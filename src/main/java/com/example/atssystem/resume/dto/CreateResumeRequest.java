package com.example.atssystem.resume.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateResumeRequest(
		@NotBlank(message = "Resume text is required")
		String text
) {
}
