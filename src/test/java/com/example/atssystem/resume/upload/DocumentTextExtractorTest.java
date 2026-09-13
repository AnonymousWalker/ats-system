package com.example.atssystem.resume.upload;

import com.example.atssystem.common.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentTextExtractorTest {

	private final DocumentTextExtractor extractor = new DocumentTextExtractor();

	@TempDir
	Path tempDir;

	@Test
	void extractsTextFromPdf() throws Exception {
		Path pdf = ResumeFixtureFactory.writeSamplePdf(tempDir);
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"sample-resume.pdf",
				"application/pdf",
				Files.readAllBytes(pdf)
		);

		String text = extractor.extract(file);

		assertThat(text).contains("Java").contains("Spring Boot").contains("PostgreSQL");
	}

	@Test
	void extractsTextFromDocx() throws Exception {
		Path docx = ResumeFixtureFactory.writeSampleDocx(tempDir);
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"sample-resume.docx",
				"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
				Files.readAllBytes(docx)
		);

		String text = extractor.extract(file);

		assertThat(text).contains("Java").contains("Spring Boot").contains("PostgreSQL");
	}

	@Test
	void rejectsCorruptPdf() throws Exception {
		Path corrupt = ResumeFixtureFactory.writeCorruptPdf(tempDir);
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"corrupt.pdf",
				"application/pdf",
				Files.readAllBytes(corrupt)
		);

		assertThatThrownBy(() -> extractor.extract(file))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("corrupt")
				.hasMessageContaining("Paste the resume text");
	}

	@Test
	void rejectsEncryptedPdf() throws Exception {
		Path encrypted = ResumeFixtureFactory.writeEncryptedPdf(tempDir);
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"encrypted.pdf",
				"application/pdf",
				Files.readAllBytes(encrypted)
		);

		assertThatThrownBy(() -> extractor.extract(file))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("encrypted")
				.hasMessageContaining("Paste the resume text");
	}

	@Test
	void rejectsImageOnlyPdf() throws Exception {
		Path imageOnly = ResumeFixtureFactory.writeImageOnlyPdf(tempDir);
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"image-only.pdf",
				"application/pdf",
				Files.readAllBytes(imageOnly)
		);

		assertThatThrownBy(() -> extractor.extract(file))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("No extractable text")
				.hasMessageContaining("Paste the resume text");
	}

	@Test
	void rejectsUnsupportedExtension() throws Exception {
		Path txt = ResumeFixtureFactory.writeUnsupportedTxt(tempDir);
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"resume.txt",
				"text/plain",
				Files.readAllBytes(txt)
		);

		assertThatThrownBy(() -> extractor.extract(file))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("Unsupported file type");
	}
}
