package com.example.atssystem.resume.dto;

import java.time.Instant;
import java.util.UUID;

public record ResumeResponse(UUID id, Instant createdAt) {
}
