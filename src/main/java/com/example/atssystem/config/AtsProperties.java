package com.example.atssystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "ats")
public class AtsProperties {

	private final Cors cors = new Cors();
	private final RateLimit rateLimit = new RateLimit();

	public Cors getCors() {
		return cors;
	}

	public RateLimit getRateLimit() {
		return rateLimit;
	}

	public static class Cors {
		/**
		 * Comma-separated browser origins, e.g. http://localhost:5173,http://localhost:8080
		 */
		private String allowedOrigins = "http://localhost:5173";

		public String getAllowedOrigins() {
			return allowedOrigins;
		}

		public void setAllowedOrigins(String allowedOrigins) {
			this.allowedOrigins = allowedOrigins;
		}

		public List<String> allowedOriginList() {
			return Arrays.stream(allowedOrigins.split(","))
					.map(String::trim)
					.filter(origin -> !origin.isEmpty())
					.toList();
		}
	}

	public static class RateLimit {
		private int createsPerMinute = 30;

		public int getCreatesPerMinute() {
			return createsPerMinute;
		}

		public void setCreatesPerMinute(int createsPerMinute) {
			this.createsPerMinute = createsPerMinute;
		}
	}
}
