package com.example.atssystem.job.dto;

import java.time.Instant;
import java.util.UUID;

public record JobResponse(UUID id, Instant createdAt) {
}
