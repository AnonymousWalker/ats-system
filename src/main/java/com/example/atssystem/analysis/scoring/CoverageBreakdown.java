package com.example.atssystem.analysis.scoring;

public record CoverageBreakdown(int matched, int total, int percent) {

	public static CoverageBreakdown of(int matched, int total) {
		int percent = total == 0 ? 0 : (int) Math.round(100.0 * matched / total);
		return new CoverageBreakdown(matched, total, percent);
	}
}
