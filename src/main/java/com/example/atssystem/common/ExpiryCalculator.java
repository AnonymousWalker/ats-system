package com.example.atssystem.common;

import java.time.Instant;

public final class ExpiryCalculator {

	private ExpiryCalculator() {
	}

	public static Instant defaultExpiry(Instant createdAt) {
		return createdAt.plus(ResourceTtl.DEFAULT);
	}

	public static Instant earliest(Instant first, Instant second) {
		return first.isBefore(second) ? first : second;
	}
}
