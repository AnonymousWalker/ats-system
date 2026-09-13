package com.example.atssystem.skill.extraction;

import com.example.atssystem.skill.domain.SkillPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SkillExtractorTest {

	private static final UUID JAVA = UUID.fromString("11111111-1111-1111-1111-111111110001");
	private static final UUID JAVASCRIPT = UUID.fromString("11111111-1111-1111-1111-111111110002");
	private static final UUID POSTGRESQL = UUID.fromString("11111111-1111-1111-1111-11111111000c");
	private static final UUID CPP = UUID.fromString("11111111-1111-1111-1111-111111110005");
	private static final UUID CSHARP = UUID.fromString("11111111-1111-1111-1111-111111110006");
	private static final UUID DOTNET = UUID.fromString("11111111-1111-1111-1111-111111110007");
	private static final UUID SPRING_BOOT = UUID.fromString("11111111-1111-1111-1111-111111110010");
	private static final UUID REACT = UUID.fromString("11111111-1111-1111-1111-111111110012");
	private static final UUID DOCKER = UUID.fromString("11111111-1111-1111-1111-111111110016");

	private SkillExtractor extractor;

	@BeforeEach
	void setUp() {
		extractor = new SkillExtractor(List.of(
				new CatalogTerm(JAVA, "Java", "java"),
				new CatalogTerm(JAVASCRIPT, "JavaScript", "javascript"),
				new CatalogTerm(JAVASCRIPT, "JavaScript", "js"),
				new CatalogTerm(POSTGRESQL, "PostgreSQL", "postgresql"),
				new CatalogTerm(POSTGRESQL, "PostgreSQL", "postgres"),
				new CatalogTerm(CPP, "C++", "c++"),
				new CatalogTerm(CPP, "C++", "cpp"),
				new CatalogTerm(CSHARP, "C#", "c#"),
				new CatalogTerm(DOTNET, ".NET", ".net"),
				new CatalogTerm(DOTNET, ".NET", "dotnet"),
				new CatalogTerm(SPRING_BOOT, "Spring Boot", "spring boot"),
				new CatalogTerm(REACT, "React", "react"),
				new CatalogTerm(DOCKER, "Docker", "docker")
		));
	}

	@Test
	void mapsPostgresAliasToPostgreSQL() {
		List<ExtractedSkill> skills = extractor.extractFromResume("Used Postgres for OLTP workloads.");

		assertThat(skills).hasSize(1);
		assertThat(skills.getFirst().skillId()).isEqualTo(POSTGRESQL);
		assertThat(skills.getFirst().canonicalName()).isEqualTo("PostgreSQL");
		assertThat(skills.getFirst().matchedTerm()).isEqualTo("postgres");
		assertThat(skills.getFirst().evidence()).containsIgnoringCase("Postgres");
	}

	@Test
	void distinguishesJavaFromJavaScript() {
		List<ExtractedSkill> both = extractor.extractFromResume(
				"Built APIs in Java and frontends in JavaScript."
		);
		assertThat(both).extracting(ExtractedSkill::skillId)
				.containsExactlyInAnyOrder(JAVA, JAVASCRIPT);

		List<ExtractedSkill> onlyJs = extractor.extractFromResume("Strong JavaScript and React experience.");
		assertThat(onlyJs).extracting(ExtractedSkill::skillId)
				.contains(JAVASCRIPT)
				.doesNotContain(JAVA);

		List<ExtractedSkill> onlyJava = extractor.extractFromResume("Five years of Java backend work.");
		assertThat(onlyJava).extracting(ExtractedSkill::skillId)
				.contains(JAVA)
				.doesNotContain(JAVASCRIPT);
	}

	@Test
	void preservesCppCsharpAndDotNet() {
		List<ExtractedSkill> skills = extractor.extractFromResume(
				"Experience with C++, C#, and .NET microservices."
		);

		assertThat(skills).extracting(ExtractedSkill::canonicalName)
				.containsExactlyInAnyOrder("C++", "C#", ".NET");
	}

	@Test
	void handlesPunctuationAndCaseVariation() {
		List<ExtractedSkill> skills = extractor.extractFromResume(
				"Skills: JAVA, React.js, DOCKER; spring boot."
		);

		// React.js is not an alias in this fixture catalog; React still matches via complete term "react"
		// inside "React.js" because "." is a non-alnum boundary after "react".
		assertThat(skills).extracting(ExtractedSkill::canonicalName)
				.contains("Java", "React", "Docker", "Spring Boot");
	}

	@Test
	void deduplicatesRepeatedTerms() {
		List<ExtractedSkill> skills = extractor.extractFromResume(
				"Java, java, JAVA — and more Java services."
		);

		assertThat(skills).hasSize(1);
		assertThat(skills.getFirst().canonicalName()).isEqualTo("Java");
	}

	@Test
	void suggestsRequiredAndPreferredFromJdHeadings() {
		String jd = """
				We are hiring a backend engineer.

				Required skills:
				- Java
				- PostgreSQL

				Preferred:
				- Docker
				- React
				""";

		List<ExtractedSkill> skills = extractor.extractFromJobDescription(jd);

		assertThat(find(skills, JAVA).priority()).isEqualTo(SkillPriority.REQUIRED);
		assertThat(find(skills, JAVA).priorityAmbiguous()).isFalse();
		assertThat(find(skills, POSTGRESQL).priority()).isEqualTo(SkillPriority.REQUIRED);
		assertThat(find(skills, DOCKER).priority()).isEqualTo(SkillPriority.PREFERRED);
		assertThat(find(skills, REACT).priority()).isEqualTo(SkillPriority.PREFERRED);
	}

	@Test
	void flagsAmbiguousPriorityWhenNoHeadings() {
		List<ExtractedSkill> skills = extractor.extractFromJobDescription(
				"Looking for Java and Docker experience."
		);

		assertThat(skills).isNotEmpty();
		assertThat(skills).allMatch(ExtractedSkill::priorityAmbiguous);
		assertThat(skills).allMatch(skill -> skill.priority() == null);
	}

	@Test
	void flagsAmbiguousWhenSkillAppearsInBothSections() {
		String jd = """
				Required:
				Java

				Preferred:
				Java, Docker
				""";

		List<ExtractedSkill> skills = extractor.extractFromJobDescription(jd);
		ExtractedSkill java = find(skills, JAVA);

		assertThat(java.priority()).isNull();
		assertThat(java.priorityAmbiguous()).isTrue();
		assertThat(find(skills, DOCKER).priority()).isEqualTo(SkillPriority.PREFERRED);
	}

	@Test
	void mapsJsAliasToJavaScriptNotJava() {
		List<ExtractedSkill> skills = extractor.extractFromResume("Familiar with JS tooling.");

		assertThat(skills).hasSize(1);
		assertThat(skills.getFirst().skillId()).isEqualTo(JAVASCRIPT);
	}

	private static ExtractedSkill find(List<ExtractedSkill> skills, UUID skillId) {
		return skills.stream()
				.filter(skill -> skill.skillId().equals(skillId))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Missing skill " + skillId));
	}
}
