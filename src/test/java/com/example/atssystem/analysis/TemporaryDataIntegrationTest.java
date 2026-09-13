package com.example.atssystem.analysis;

import com.example.atssystem.cleanup.TemporaryDataCleanupService;
import com.example.atssystem.job.JobDescriptionRepository;
import com.example.atssystem.resume.ResumeRepository;
import com.example.atssystem.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TemporaryDataIntegrationTest extends PostgresIntegrationTestSupport {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ResumeRepository resumeRepository;

	@Autowired
	private JobDescriptionRepository jobDescriptionRepository;

	@Autowired
	private AnalysisRepository analysisRepository;

	@Autowired
	private TemporaryDataCleanupService cleanupService;

	@Test
	void deleteRemovesAnalysisAndUnsharedInputs() throws Exception {
		Created created = createDraft("Java engineer", "Need Java");

		mockMvc.perform(delete("/api/analyses/" + created.analysisId()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/analyses/" + created.analysisId()))
				.andExpect(status().isNotFound());

		assertThat(analysisRepository.findById(UUID.fromString(created.analysisId()))).isEmpty();
		assertThat(resumeRepository.findById(UUID.fromString(created.resumeId()))).isEmpty();
		assertThat(jobDescriptionRepository.findById(UUID.fromString(created.jobId()))).isEmpty();
	}

	@Test
	void expiredAnalysisIsRejected() throws Exception {
		Created created = createDraft("Spring Boot engineer", "Need Spring Boot");

		jdbcTemplate.update(
				"update analyses set expires_at = now() - interval '1 hour' where id = ?",
				UUID.fromString(created.analysisId())
		);

		mockMvc.perform(get("/api/analyses/" + created.analysisId()))
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("expired")))
				.andExpect(jsonPath("$.path").value("/api/analyses/{id}"));
	}

	@Test
	void cleanupRemovesExpiredAnalysesBeforeUnreferencedInputs() throws Exception {
		Created created = createDraft("Docker experience", "Need Docker");

		jdbcTemplate.update(
				"update analyses set expires_at = now() - interval '1 hour' where id = ?",
				UUID.fromString(created.analysisId())
		);
		jdbcTemplate.update(
				"update resumes set expires_at = now() - interval '1 hour' where id = ?",
				UUID.fromString(created.resumeId())
		);
		jdbcTemplate.update(
				"update job_descriptions set expires_at = now() - interval '1 hour' where id = ?",
				UUID.fromString(created.jobId())
		);

		cleanupService.cleanupExpired();

		assertThat(analysisRepository.findById(UUID.fromString(created.analysisId()))).isEmpty();
		assertThat(resumeRepository.findById(UUID.fromString(created.resumeId()))).isEmpty();
		assertThat(jobDescriptionRepository.findById(UUID.fromString(created.jobId()))).isEmpty();
	}

	@Test
	void apiResponsesDisableCaching() throws Exception {
		Created created = createDraft("Kafka engineer", "Need Kafka");

		mockMvc.perform(get("/api/analyses/" + created.analysisId()))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
				.andExpect(header().string("X-Robots-Tag", "noindex, nofollow"));
	}

	private Created createDraft(String resumeText, String jobText) throws Exception {
		MvcResult resumeResult = mockMvc.perform(post("/api/resumes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"text":"%s"}
								""".formatted(resumeText)))
				.andExpect(status().isCreated())
				.andReturn();
		String resumeId = readId(resumeResult);

		MvcResult jobResult = mockMvc.perform(post("/api/jobs")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"text":"%s"}
								""".formatted(jobText)))
				.andExpect(status().isCreated())
				.andReturn();
		String jobId = readId(jobResult);

		MvcResult analysisResult = mockMvc.perform(post("/api/analyses")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"resumeId":"%s","jobId":"%s"}
								""".formatted(resumeId, jobId)))
				.andExpect(status().isCreated())
				.andReturn();

		return new Created(resumeId, jobId, readId(analysisResult));
	}

	private static String readId(MvcResult result) throws Exception {
		String json = result.getResponse().getContentAsString();
		int start = json.indexOf("\"id\":\"") + 6;
		int end = json.indexOf('"', start);
		return json.substring(start, end);
	}

	private record Created(String resumeId, String jobId, String analysisId) {
	}
}
