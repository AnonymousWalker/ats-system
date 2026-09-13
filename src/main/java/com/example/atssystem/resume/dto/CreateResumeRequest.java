package com.example.atssystem.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateResumeRequest(
		@NotBlank(message = "Resume text is required")
		@Size(max = 200_000, message = "Resume text must be at most 200000 characters")
		String text
) {
}
