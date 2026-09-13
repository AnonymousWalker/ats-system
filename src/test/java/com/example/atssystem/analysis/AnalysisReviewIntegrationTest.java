package com.example.atssystem.analysis;

import com.example.atssystem.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalysisReviewIntegrationTest extends PostgresIntegrationTestSupport {

	private static final String JAVA = "11111111-1111-1111-1111-111111110001";
	private static final String POSTGRES = "11111111-1111-1111-1111-11111111000c";
	private static final String DOCKER = "11111111-1111-1111-1111-111111110016";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void reviewProducesFortyPercentForOneRequiredOfTwoRequiredPlusPreferred() throws Exception {
		String analysisId = createDraftAnalysis(
				"Backend engineer using Java.",
				"Required:\nJava\nPostgreSQL\n\nPreferred:\nDocker\n"
		);

		mockMvc.perform(put("/api/analyses/" + analysisId + "/review")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "resumeSkills": [
								    {"skillId":"%s","evidence":"Backend engineer using Java."}
								  ],
								  "jobSkills": [
								    {"skillId":"%s","priority":"REQUIRED"},
								    {"skillId":"%s","priority":"REQUIRED"},
								    {"skillId":"%s","priority":"PREFERRED"}
								  ]
								}
								""".formatted(JAVA, JAVA, POSTGRES, DOCKER)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REVIEWED"))
				.andExpect(jsonPath("$.result.scorable").value(true))
				.andExpect(jsonPath("$.result.score").value(40))
				.andExpect(jsonPath("$.result.matchedWeight").value(2))
				.andExpect(jsonPath("$.result.totalWeight").value(5))
				.andExpect(jsonPath("$.result.matchedSkills[0].name").value("Java"))
				.andExpect(jsonPath("$.result.missingSkills.length()").value(2))
				.andExpect(jsonPath("$.result.requiredCoverage.matched").value(1))
				.andExpect(jsonPath("$.result.requiredCoverage.total").value(2))
				.andExpect(jsonPath("$.result.preferredCoverage.matched").value(0))
				.andExpect(jsonPath("$.result.preferredCoverage.total").value(1))
				.andExpect(jsonPath("$.result.scoringVersion").value("1"))
				.andExpect(jsonPath("$.result.catalogVersion").value("1"))
				.andExpect(jsonPath("$.reviewedResumeSkills.length()").value(1))
				.andExpect(jsonPath("$.resumeSkills").isArray());

		mockMvc.perform(get("/api/analyses/" + analysisId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REVIEWED"))
				.andExpect(jsonPath("$.result.score").value(40));
	}

	@Test
	void reviewWithZeroJobSkillsIsUnscorable() throws Exception {
		String analysisId = createDraftAnalysis(
				"Java developer",
				"A flexible role with no listed skills."
		);

		mockMvc.perform(put("/api/analyses/" + analysisId + "/review")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "resumeSkills": [{"skillId":"%s"}],
								  "jobSkills": []
								}
								""".formatted(JAVA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REVIEWED"))
				.andExpect(jsonPath("$.result.scorable").value(false))
				.andExpect(jsonPath("$.result.score").doesNotExist());
	}

	@Test
	void reviewRejectsUnknownSkillIds() throws Exception {
		String analysisId = createDraftAnalysis("Java developer", "Need Java");

		mockMvc.perform(put("/api/analyses/" + analysisId + "/review")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "resumeSkills": [{"skillId":"00000000-0000-0000-0000-000000000099"}],
								  "jobSkills": []
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Unknown skillId")));
	}

	private String createDraftAnalysis(String resumeText, String jobText) throws Exception {
		MvcResult resumeResult = mockMvc.perform(post("/api/resumes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"text":"%s"}
								""".formatted(resumeText)))
				.andExpect(status().isCreated())
				.andReturn();
		String resumeId = readJsonField(resumeResult, "id");

		MvcResult jobResult = mockMvc.perform(post("/api/jobs")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"text":"%s"}
								""".formatted(jobText.replace("\n", "\\n"))))
				.andExpect(status().isCreated())
				.andReturn();
		String jobId = readJsonField(jobResult, "id");

		MvcResult analysisResult = mockMvc.perform(post("/api/analyses")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"resumeId":"%s","jobId":"%s"}
								""".formatted(resumeId, jobId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.result").doesNotExist())
				.andReturn();
		return readJsonField(analysisResult, "id");
	}

	private String readJsonField(MvcResult result, String field) throws Exception {
		String json = result.getResponse().getContentAsString();
		int start = json.indexOf("\"" + field + "\":\"") + field.length() + 4;
		int end = json.indexOf('"', start);
		assertThat(start).isGreaterThan(field.length() + 3);
		assertThat(end).isGreaterThan(start);
		return json.substring(start, end);
	}
}
