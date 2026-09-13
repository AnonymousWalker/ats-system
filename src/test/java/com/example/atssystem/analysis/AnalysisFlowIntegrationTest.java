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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalysisFlowIntegrationTest extends PostgresIntegrationTestSupport {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void createsResumeJobAndDraftAnalysis() throws Exception {
		MvcResult resumeResult = mockMvc.perform(post("/api/resumes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"text":"Built REST APIs with Java and Spring Boot."}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.id").exists())
				.andReturn();

		String resumeId = readJsonField(resumeResult, "id");

		MvcResult jobResult = mockMvc.perform(post("/api/jobs")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"text":"Looking for a backend engineer with Spring Boot experience."}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.id").exists())
				.andReturn();

		String jobId = readJsonField(jobResult, "id");

		MvcResult analysisResult = mockMvc.perform(post("/api/analyses")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"resumeId":"%s","jobId":"%s"}
								""".formatted(resumeId, jobId)))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andExpect(jsonPath("$.resumeId").value(resumeId))
				.andExpect(jsonPath("$.jobId").value(jobId))
				.andExpect(jsonPath("$.resumeSkills[*].name").value(org.hamcrest.Matchers.hasItems("Java", "Spring Boot", "REST")))
				.andExpect(jsonPath("$.jobSkills[*].name").value(org.hamcrest.Matchers.hasItem("Spring Boot")))
				.andReturn();

		String analysisId = readJsonField(analysisResult, "id");

		mockMvc.perform(get("/api/analyses/" + analysisId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andExpect(jsonPath("$.resumeId").value(resumeId))
				.andExpect(jsonPath("$.jobId").value(jobId))
				.andExpect(jsonPath("$.resumeSkills").isArray())
				.andExpect(jsonPath("$.jobSkills").isArray())
				.andExpect(jsonPath("$.score").doesNotExist());
	}

	@Test
	void extractsAliasesAndPriorityHints() throws Exception {
		MvcResult resumeResult = mockMvc.perform(post("/api/resumes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"text":"Production experience with Postgres, C++, and JavaScript."}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String resumeId = readJsonField(resumeResult, "id");

		MvcResult jobResult = mockMvc.perform(post("/api/jobs")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"text":"Required:\\nPostgreSQL\\nJava\\n\\nPreferred:\\nDocker\\n"}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String jobId = readJsonField(jobResult, "id");

		mockMvc.perform(post("/api/analyses")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"resumeId":"%s","jobId":"%s"}
								""".formatted(resumeId, jobId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.resumeSkills[*].name").value(org.hamcrest.Matchers.hasItems("PostgreSQL", "C++", "JavaScript")))
				.andExpect(jsonPath("$.jobSkills[?(@.name=='PostgreSQL')].priority").value(org.hamcrest.Matchers.hasItem("REQUIRED")))
				.andExpect(jsonPath("$.jobSkills[?(@.name=='Docker')].priority").value(org.hamcrest.Matchers.hasItem("PREFERRED")));
	}

	@Test
	void rejectsAnalysisForUnknownResume() throws Exception {
		mockMvc.perform(post("/api/analyses")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "resumeId":"00000000-0000-0000-0000-000000000001",
								  "jobId":"00000000-0000-0000-0000-000000000002"
								}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Resume not found"));
	}

	@Test
	void rejectsAnalysisForUnknownJob() throws Exception {
		MvcResult resumeResult = mockMvc.perform(post("/api/resumes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"text":"Backend engineer with PostgreSQL experience."}
								"""))
				.andExpect(status().isCreated())
				.andReturn();

		String resumeId = readJsonField(resumeResult, "id");

		mockMvc.perform(post("/api/analyses")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "resumeId":"%s",
								  "jobId":"00000000-0000-0000-0000-000000000002"
								}
								""".formatted(resumeId)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Job description not found"));
	}

	@Test
	void rejectsBlankResumeText() throws Exception {
		mockMvc.perform(post("/api/resumes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"text":"   "}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("text"));
	}

	@Test
	void persistsRecordsAcrossRequests() throws Exception {
		MvcResult resumeResult = mockMvc.perform(post("/api/resumes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"text":"Persisted resume text."}
								"""))
				.andExpect(status().isCreated())
				.andReturn();

		String resumeId = readJsonField(resumeResult, "id");

		MvcResult jobResult = mockMvc.perform(post("/api/jobs")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"text":"Persisted job description text."}
								"""))
				.andExpect(status().isCreated())
				.andReturn();

		String jobId = readJsonField(jobResult, "id");

		MvcResult analysisResult = mockMvc.perform(post("/api/analyses")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"resumeId":"%s","jobId":"%s"}
								""".formatted(resumeId, jobId)))
				.andExpect(status().isCreated())
				.andReturn();

		String analysisId = readJsonField(analysisResult, "id");

		mockMvc.perform(get("/api/analyses/" + analysisId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resumeId").value(resumeId))
				.andExpect(jsonPath("$.jobId").value(jobId));
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
