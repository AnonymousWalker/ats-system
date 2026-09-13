package com.example.atssystem.resume.upload;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ResumeFixtureFactory {

	public static final String SAMPLE_TEXT =
			"Built REST APIs with Java and Spring Boot. Used PostgreSQL in production.";

	private ResumeFixtureFactory() {
	}

	public static Path writeSamplePdf(Path directory) throws IOException {
		Path path = directory.resolve("sample-resume.pdf");
		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage();
			document.addPage(page);
			try (PDPageContentStream content = new PDPageContentStream(document, page)) {
				content.beginText();
				content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				content.newLineAtOffset(50, 700);
				content.showText(SAMPLE_TEXT);
				content.endText();
			}
			document.save(path.toFile());
		}
		return path;
	}

	public static Path writeSampleDocx(Path directory) throws IOException {
		Path path = directory.resolve("sample-resume.docx");
		try (XWPFDocument document = new XWPFDocument();
			 OutputStream out = Files.newOutputStream(path)) {
			XWPFParagraph paragraph = document.createParagraph();
			XWPFRun run = paragraph.createRun();
			run.setText(SAMPLE_TEXT);
			document.write(out);
		}
		return path;
	}

	public static Path writeCorruptPdf(Path directory) throws IOException {
		Path path = directory.resolve("corrupt.pdf");
		Files.writeString(path, "%PDF-1.4 corrupted-not-a-real-file", StandardCharsets.UTF_8);
		return path;
	}

	public static Path writeEncryptedPdf(Path directory) throws IOException {
		Path path = directory.resolve("encrypted.pdf");
		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage();
			document.addPage(page);
			try (PDPageContentStream content = new PDPageContentStream(document, page)) {
				content.beginText();
				content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				content.newLineAtOffset(50, 700);
				content.showText("Secret resume text");
				content.endText();
			}
			AccessPermission permissions = new AccessPermission();
			StandardProtectionPolicy policy = new StandardProtectionPolicy("owner", "secret", permissions);
			policy.setEncryptionKeyLength(128);
			document.protect(policy);
			document.save(path.toFile());
		}
		return path;
	}

	public static Path writeImageOnlyPdf(Path directory) throws IOException {
		Path path = directory.resolve("image-only.pdf");
		BufferedImage image = new BufferedImage(200, 60, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
		ImageIO.write(image, "png", imageBytes);

		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage();
			document.addPage(page);
			PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageBytes.toByteArray(), "scan");
			try (PDPageContentStream content = new PDPageContentStream(document, page)) {
				content.drawImage(pdImage, 50, 650, 200, 60);
			}
			document.save(path.toFile());
		}
		return path;
	}

	public static Path writeUnsupportedTxt(Path directory) throws IOException {
		Path path = directory.resolve("resume.txt");
		Files.writeString(path, SAMPLE_TEXT, StandardCharsets.UTF_8);
		return path;
	}
}
