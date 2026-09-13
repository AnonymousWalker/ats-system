package com.example.atssystem.skill.extraction;

import com.example.atssystem.skill.domain.SkillPriority;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic skill extractor: longest-term-first complete matching with aliases,
 * evidence snippets, and JD required/preferred suggestion from section headings.
 */
public final class SkillExtractor {

	private static final Pattern HEADING_LINE = Pattern.compile(
			"(?im)^[ \\t]*(?:#{1,6}\\s*)?(?:\\*\\*|__)?\\s*"
					+ "(required(?:\\s+skills?|\\s+qualifications?)?|requirements|must[- ]haves?|"
					+ "minimum\\s+qualifications?|what\\s+you.?ll\\s+need|"
					+ "preferred(?:\\s+skills?|\\s+qualifications?)?|nice[- ]to[- ]haves?|"
					+ "optional(?:\\s+skills?)?|bonus(?:\\s+points?)?|pluses?)"
					+ "\\s*(?:\\*\\*|__)?\\s*:?\\s*$"
	);

	private static final Pattern REQUIRED_HEADING = Pattern.compile(
			"(?i)^(required|requirements|must[- ]have|minimum\\s+qualification|what\\s+you.?ll\\s+need)"
	);

	private static final Pattern PREFERRED_HEADING = Pattern.compile(
			"(?i)^(preferred|nice[- ]to[- ]have|optional|bonus|plus)"
	);

	private final List<PreparedTerm> terms;

	public SkillExtractor(List<CatalogTerm> catalogTerms) {
		this.terms = catalogTerms.stream()
				.map(PreparedTerm::from)
				.sorted(Comparator
						.comparingInt((PreparedTerm t) -> t.normalizedAlias().length())
						.reversed()
						.thenComparing(PreparedTerm::normalizedAlias))
				.toList();
	}

	public List<ExtractedSkill> extractFromResume(String text) {
		return extract(text, false);
	}

	public List<ExtractedSkill> extractFromJobDescription(String text) {
		return extract(text, true);
	}

	private List<ExtractedSkill> extract(String text, boolean classifyPriority) {
		if (text == null || text.isBlank()) {
			return List.of();
		}

		NormalizedText normalized = NormalizedText.of(text);
		boolean[] occupied = new boolean[normalized.normalized().length()];
		Map<UUID, MatchHit> firstHitBySkill = new HashMap<>();

		for (PreparedTerm term : terms) {
			Matcher matcher = term.pattern().matcher(normalized.normalized());
			while (matcher.find()) {
				int start = matcher.start();
				int end = matcher.end();
				if (isOccupied(occupied, start, end)) {
					continue;
				}
				markOccupied(occupied, start, end);

				UUID skillId = term.skillId();
				if (!firstHitBySkill.containsKey(skillId)) {
					String evidence = evidenceSnippet(normalized, start, end);
					firstHitBySkill.put(skillId, new MatchHit(term, evidence, start, end));
				}
			}
		}

		Map<UUID, SkillPriority> priorityBySkill = classifyPriority
				? classifyPriorities(text, firstHitBySkill)
				: Map.of();

		List<ExtractedSkill> results = new ArrayList<>();
		for (MatchHit hit : firstHitBySkill.values()) {
			SkillPriority priority = classifyPriority ? priorityBySkill.get(hit.term().skillId()) : null;
			boolean ambiguous = classifyPriority && priority == null;
			results.add(new ExtractedSkill(
					hit.term().skillId(),
					hit.term().canonicalName(),
					hit.term().displayAlias(),
					hit.evidence(),
					priority,
					ambiguous
			));
		}

		results.sort(Comparator
				.comparing(ExtractedSkill::canonicalName, String.CASE_INSENSITIVE_ORDER)
				.thenComparing(ExtractedSkill::matchedTerm, String.CASE_INSENSITIVE_ORDER));
		return List.copyOf(results);
	}

	private Map<UUID, SkillPriority> classifyPriorities(String originalText, Map<UUID, MatchHit> hits) {
		List<Section> sections = parseSections(originalText);
		Map<UUID, SkillPriority> priorities = new HashMap<>();

		for (Map.Entry<UUID, MatchHit> entry : hits.entrySet()) {
			UUID skillId = entry.getKey();
			Set<SkillPriority> seen = new HashSet<>();

			for (Section section : sections) {
				if (section.priority() == null) {
					continue;
				}
				SkillExtractor sectionExtractor = this;
				List<ExtractedSkill> sectionHits = sectionExtractor.extract(section.body(), false);
				boolean present = sectionHits.stream().anyMatch(s -> s.skillId().equals(skillId));
				if (present) {
					seen.add(section.priority());
				}
			}

			if (seen.size() == 1) {
				priorities.put(skillId, seen.iterator().next());
			}
		}
		return priorities;
	}

	static List<Section> parseSections(String text) {
		Matcher matcher = HEADING_LINE.matcher(text);
		List<HeadingMatch> headings = new ArrayList<>();
		while (matcher.find()) {
			String label = matcher.group(1).replaceAll("\\s+", " ").trim();
			SkillPriority priority = toPriority(label);
			headings.add(new HeadingMatch(matcher.start(), matcher.end(), priority));
		}

		if (headings.isEmpty()) {
			return List.of(new Section(null, text));
		}

		List<Section> sections = new ArrayList<>();
		if (headings.getFirst().start() > 0) {
			sections.add(new Section(null, text.substring(0, headings.getFirst().start())));
		}
		for (int i = 0; i < headings.size(); i++) {
			HeadingMatch heading = headings.get(i);
			int bodyStart = heading.end();
			int bodyEnd = (i + 1 < headings.size()) ? headings.get(i + 1).start() : text.length();
			sections.add(new Section(heading.priority(), text.substring(bodyStart, bodyEnd)));
		}
		return sections;
	}

	private static SkillPriority toPriority(String label) {
		String normalized = label.toLowerCase(Locale.ROOT);
		if (REQUIRED_HEADING.matcher(normalized).find()) {
			return SkillPriority.REQUIRED;
		}
		if (PREFERRED_HEADING.matcher(normalized).find()) {
			return SkillPriority.PREFERRED;
		}
		return null;
	}

	private static boolean isOccupied(boolean[] occupied, int start, int end) {
		for (int i = start; i < end; i++) {
			if (occupied[i]) {
				return true;
			}
		}
		return false;
	}

	private static void markOccupied(boolean[] occupied, int start, int end) {
		for (int i = start; i < end; i++) {
			occupied[i] = true;
		}
	}

	private static String evidenceSnippet(NormalizedText normalized, int normStart, int normEnd) {
		int originalStart = normalized.mapToOriginal(normStart);
		int originalEnd = normalized.mapToOriginal(normEnd);
		String original = normalized.original();

		int snippetStart = Math.max(0, originalStart - 40);
		int snippetEnd = Math.min(original.length(), originalEnd + 40);

		while (snippetStart > 0 && !Character.isWhitespace(original.charAt(snippetStart - 1))) {
			snippetStart--;
			if (originalStart - snippetStart > 60) {
				break;
			}
		}
		while (snippetEnd < original.length() && !Character.isWhitespace(original.charAt(snippetEnd))) {
			snippetEnd++;
			if (snippetEnd - originalEnd > 60) {
				break;
			}
		}

		String snippet = original.substring(snippetStart, snippetEnd).replaceAll("\\s+", " ").trim();
		if (snippetStart > 0) {
			snippet = "…" + snippet;
		}
		if (snippetEnd < original.length()) {
			snippet = snippet + "…";
		}
		return snippet;
	}

	record Section(SkillPriority priority, String body) {
	}

	private record HeadingMatch(int start, int end, SkillPriority priority) {
	}

	private record MatchHit(PreparedTerm term, String evidence, int start, int end) {
	}

	private record PreparedTerm(
			UUID skillId,
			String canonicalName,
			String displayAlias,
			String normalizedAlias,
			Pattern pattern
	) {
		static PreparedTerm from(CatalogTerm term) {
			String normalizedAlias = TextNormalizer.normalize(term.alias());
			Pattern pattern = buildCompleteTermPattern(normalizedAlias);
			return new PreparedTerm(
					term.skillId(),
					term.canonicalName(),
					term.alias(),
					normalizedAlias,
					pattern
			);
		}
	}

	/**
	 * Complete-term matching that preserves C++, C#, .NET and multi-word aliases.
	 * Boundaries are non-alphanumeric (or string edges), so "java" does not match inside "javascript"
	 * when combined with longest-first occupancy.
	 */
	static Pattern buildCompleteTermPattern(String normalizedAlias) {
		String escaped = Pattern.quote(normalizedAlias);
		// Allow optional whitespace variation already collapsed in normalized text.
		return Pattern.compile("(?<!\\p{Alnum})" + escaped + "(?!\\p{Alnum})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	}

	/**
	 * Maps normalized text indices back to original text for evidence snippets.
	 */
	static final class NormalizedText {
		private final String original;
		private final String normalized;
		private final int[] normToOriginal;

		private NormalizedText(String original, String normalized, int[] normToOriginal) {
			this.original = original;
			this.normalized = normalized;
			this.normToOriginal = normToOriginal;
		}

		static NormalizedText of(String original) {
			Objects.requireNonNull(original, "original");
			StringBuilder normalized = new StringBuilder(original.length());
			List<Integer> mapping = new ArrayList<>(original.length());

			boolean lastWasSpace = false;
			for (int i = 0; i < original.length(); ) {
				int cp = original.codePointAt(i);
				int charCount = Character.charCount(cp);
				if (Character.isWhitespace(cp)) {
					if (!lastWasSpace && !normalized.isEmpty()) {
						normalized.append(' ');
						mapping.add(i);
						lastWasSpace = true;
					}
				} else {
					String lower = new String(Character.toChars(Character.toLowerCase(cp)));
					normalized.append(lower);
					for (int k = 0; k < lower.length(); k++) {
						mapping.add(i);
					}
					lastWasSpace = false;
				}
				i += charCount;
			}

			while (!normalized.isEmpty() && normalized.charAt(normalized.length() - 1) == ' ') {
				normalized.setLength(normalized.length() - 1);
				mapping.removeLast();
			}

			int[] normToOriginal = mapping.stream().mapToInt(Integer::intValue).toArray();
			return new NormalizedText(original, normalized.toString(), normToOriginal);
		}

		String original() {
			return original;
		}

		String normalized() {
			return normalized;
		}

		int mapToOriginal(int normalizedIndex) {
			if (normalizedIndex <= 0) {
				return 0;
			}
			if (normalizedIndex >= normToOriginal.length) {
				return original.length();
			}
			return normToOriginal[normalizedIndex];
		}
	}
}
