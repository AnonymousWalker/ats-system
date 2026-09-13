package com.example.atssystem.common;

import java.time.Instant;

public final class ExpiryGuard {

	private ExpiryGuard() {
	}

	public static void ensureActive(Instant expiresAt) {
		if (expiresAt == null || !expiresAt.isAfter(Instant.now())) {
			throw new ResourceExpiredException("This link has expired. Start a new analysis.");
		}
	}
}
