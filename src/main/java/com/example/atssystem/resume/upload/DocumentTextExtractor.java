package com.example.atssystem.resume.upload;

import com.example.atssystem.common.BadRequestException;
import org.apache.tika.exception.EncryptedDocumentException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

@Component
public class DocumentTextExtractor {

	private static final String PASTE_FALLBACK =
			" Paste the resume text instead, or upload a text-based PDF/DOCX.";

	private static final Set<String> ALLOWED_MEDIA_TYPES = Set.of(
			"application/pdf",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document"
	);

	private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".docx");

	private final Parser parser = new AutoDetectParser();

	public String extract(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException(
					"Resume file is required." + PASTE_FALLBACK
			);
		}

		validateDeclaredSize(file);
		validateFilename(file.getOriginalFilename());

		Path tempFile = null;
		try {
			tempFile = Files.createTempFile("ats-resume-", sanitizeSuffix(file.getOriginalFilename()));
			file.transferTo(tempFile);
			validateStoredSize(tempFile);
			return parseFile(tempFile, file.getOriginalFilename());
		} catch (BadRequestException ex) {
			throw ex;
		} catch (IOException ex) {
			throw new BadRequestException(
					"Could not read the uploaded file." + PASTE_FALLBACK
			);
		} finally {
			deleteQuietly(tempFile);
		}
	}

	private String parseFile(Path tempFile, String originalFilename) {
		Metadata metadata = new Metadata();
		if (originalFilename != null) {
			metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, originalFilename);
		}

		BodyContentHandler handler = new BodyContentHandler(UploadLimits.MAX_EXTRACTED_CHARS);
		ParseContext context = new ParseContext();
		context.set(Parser.class, parser);
		PDFParserConfig pdfConfig = new PDFParserConfig();
		pdfConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.NO_OCR);
		context.set(PDFParserConfig.class, pdfConfig);

		try (InputStream input = Files.newInputStream(tempFile)) {
			parser.parse(input, handler, metadata, context);
		} catch (EncryptedDocumentException ex) {
			throw new BadRequestException(
					"This file appears encrypted or password-protected." + PASTE_FALLBACK
			);
		} catch (SAXException ex) {
			if (WriteLimitReachedException.isWriteLimitReached(ex)) {
				throw new BadRequestException(
						"Extracted text exceeds the " + UploadLimits.MAX_EXTRACTED_CHARS
								+ "-character limit." + PASTE_FALLBACK
				);
			}
			throw new BadRequestException(
					"Could not parse this file. It may be corrupt or unsupported." + PASTE_FALLBACK
			);
		} catch (TikaException | IOException ex) {
			if (isEncryptedMessage(ex)) {
				throw new BadRequestException(
						"This file appears encrypted or password-protected." + PASTE_FALLBACK
				);
			}
			throw new BadRequestException(
					"Could not parse this file. It may be corrupt or unsupported." + PASTE_FALLBACK
			);
		}

		String detectedType = normalizeMediaType(metadata.get(Metadata.CONTENT_TYPE));
		if (detectedType == null || !ALLOWED_MEDIA_TYPES.contains(detectedType)) {
			throw new BadRequestException(
					"Unsupported file type. Upload a PDF or DOCX." + PASTE_FALLBACK
			);
		}

		String text = handler.toString() == null ? "" : handler.toString().trim();
		if (text.isBlank()) {
			throw new BadRequestException(
					"No extractable text found. Image-only scans are not supported." + PASTE_FALLBACK
			);
		}
		if (text.length() > UploadLimits.MAX_EXTRACTED_CHARS) {
			text = text.substring(0, UploadLimits.MAX_EXTRACTED_CHARS);
		}
		return text;
	}

	private void validateDeclaredSize(MultipartFile file) {
		if (file.getSize() > UploadLimits.MAX_FILE_BYTES) {
			throw new BadRequestException(
					"File exceeds the 5 MB upload limit." + PASTE_FALLBACK
			);
		}
	}

	private void validateStoredSize(Path tempFile) throws IOException {
		long size = Files.size(tempFile);
		if (size > UploadLimits.MAX_FILE_BYTES) {
			throw new BadRequestException(
					"File exceeds the 5 MB upload limit." + PASTE_FALLBACK
			);
		}
		if (size == 0) {
			throw new BadRequestException(
					"Uploaded file is empty." + PASTE_FALLBACK
			);
		}
	}

	private void validateFilename(String filename) {
		if (filename == null || filename.isBlank()) {
			throw new BadRequestException(
					"Filename is required for uploads." + PASTE_FALLBACK
			);
		}
		String lower = filename.toLowerCase(Locale.ROOT);
		boolean allowed = ALLOWED_EXTENSIONS.stream().anyMatch(lower::endsWith);
		if (!allowed) {
			throw new BadRequestException(
					"Unsupported file type. Upload a PDF or DOCX." + PASTE_FALLBACK
			);
		}
	}

	private static String sanitizeSuffix(String filename) {
		if (filename == null) {
			return ".upload";
		}
		String lower = filename.toLowerCase(Locale.ROOT);
		if (lower.endsWith(".pdf")) {
			return ".pdf";
		}
		if (lower.endsWith(".docx")) {
			return ".docx";
		}
		return ".upload";
	}

	private static String normalizeMediaType(String contentType) {
		if (contentType == null) {
			return null;
		}
		int semicolon = contentType.indexOf(';');
		String type = semicolon >= 0 ? contentType.substring(0, semicolon) : contentType;
		return type.trim().toLowerCase(Locale.ROOT);
	}

	private static boolean isEncryptedMessage(Exception ex) {
		Throwable current = ex;
		while (current != null) {
			if (current instanceof EncryptedDocumentException) {
				return true;
			}
			String message = current.getMessage();
			if (message != null) {
				String lower = message.toLowerCase(Locale.ROOT);
				if (lower.contains("encrypted") || lower.contains("password")) {
					return true;
				}
			}
			current = current.getCause();
		}
		return false;
	}

	private static void deleteQuietly(Path path) {
		if (path == null) {
			return;
		}
		try {
			Files.deleteIfExists(path);
		} catch (IOException ignored) {
			// best-effort cleanup
		}
	}
}
