package com.example.atssystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
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
		private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));

		public List<String> getAllowedOrigins() {
			return allowedOrigins;
		}

		public void setAllowedOrigins(List<String> allowedOrigins) {
			this.allowedOrigins = allowedOrigins;
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
