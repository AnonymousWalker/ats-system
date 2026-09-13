package com.example.atssystem.skill.extraction;

import java.util.Locale;

public final class TextNormalizer {

	private TextNormalizer() {
	}

	public static String normalize(String value) {
		if (value == null) {
			return "";
		}
		return value
				.toLowerCase(Locale.ROOT)
				.replaceAll("\\s+", " ")
				.trim();
	}
}
