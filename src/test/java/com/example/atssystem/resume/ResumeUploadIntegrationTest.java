package com.example.atssystem.resume;

import com.example.atssystem.resume.upload.ResumeFixtureFactory;
import com.example.atssystem.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResumeUploadIntegrationTest extends PostgresIntegrationTestSupport {

	@Autowired
	private MockMvc mockMvc;

	@TempDir
	Path tempDir;

	@Test
	void jsonTextContractStillWorks() throws Exception {
		mockMvc.perform(post("/api/resumes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"text":"Built REST APIs with Java and Spring Boot."}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.id").exists());
	}

	@Test
	void uploadsPdfAndCreatesResume() throws Exception {
		Path pdf = ResumeFixtureFactory.writeSamplePdf(tempDir);
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"sample-resume.pdf",
				"application/pdf",
				Files.readAllBytes(pdf)
		);

		MvcResult result = mockMvc.perform(multipart("/api/resumes").file(file))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/resumes/")))
				.andExpect(jsonPath("$.id").exists())
				.andReturn();

		assertThat(result.getResponse().getContentAsString()).contains("id");
	}

	@Test
	void uploadsDocxAndCreatesResume() throws Exception {
		Path docx = ResumeFixtureFactory.writeSampleDocx(tempDir);
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"sample-resume.docx",
				"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
				Files.readAllBytes(docx)
		);

		mockMvc.perform(multipart("/api/resumes").file(file))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists());
	}

	@Test
	void rejectsCorruptUploadWithActionableError() throws Exception {
		Path corrupt = ResumeFixtureFactory.writeCorruptPdf(tempDir);
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"corrupt.pdf",
				"application/pdf",
				Files.readAllBytes(corrupt)
		);

		mockMvc.perform(multipart("/api/resumes").file(file))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Paste the resume text")));
	}

	@Test
	void uploadedResumeCanBeUsedInAnalysis() throws Exception {
		Path pdf = ResumeFixtureFactory.writeSamplePdf(tempDir);
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"sample-resume.pdf",
				"application/pdf",
				Files.readAllBytes(pdf)
		);

		MvcResult resumeResult = mockMvc.perform(multipart("/api/resumes").file(file))
				.andExpect(status().isCreated())
				.andReturn();
		String resumeId = readId(resumeResult);

		MvcResult jobResult = mockMvc.perform(post("/api/jobs")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"text":"Required:\\nJava\\nSpring Boot\\n"}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String jobId = readId(jobResult);

		mockMvc.perform(post("/api/analyses")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"resumeId":"%s","jobId":"%s"}
								""".formatted(resumeId, jobId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.resumeSkills[*].name").value(org.hamcrest.Matchers.hasItems("Java", "Spring Boot")));
	}

	private static String readId(MvcResult result) throws Exception {
		String json = result.getResponse().getContentAsString();
		int start = json.indexOf("\"id\":\"") + 6;
		int end = json.indexOf('"', start);
		return json.substring(start, end);
	}
}
