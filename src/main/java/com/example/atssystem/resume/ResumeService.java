package com.example.atssystem.resume;

import com.example.atssystem.common.ExpiryCalculator;
import com.example.atssystem.common.ExpiryGuard;
import com.example.atssystem.common.ResourceNotFoundException;
import com.example.atssystem.resume.dto.CreateResumeRequest;
import com.example.atssystem.resume.dto.ResumeResponse;
import com.example.atssystem.resume.entity.Resume;
import com.example.atssystem.resume.upload.DocumentTextExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@Service
public class ResumeService {

	private final ResumeRepository resumeRepository;
	private final DocumentTextExtractor documentTextExtractor;

	public ResumeService(ResumeRepository resumeRepository, DocumentTextExtractor documentTextExtractor) {
		this.resumeRepository = resumeRepository;
		this.documentTextExtractor = documentTextExtractor;
	}

	@Transactional
	public ResumeResponse createFromText(CreateResumeRequest request) {
		return persist(request.text().trim());
	}

	@Transactional
	public ResumeResponse createFromUpload(MultipartFile file) {
		String text = documentTextExtractor.extract(file);
		return persist(text);
	}

	@Transactional(readOnly = true)
	public Resume requireById(UUID id) {
		Resume resume = resumeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
		ExpiryGuard.ensureActive(resume.getExpiresAt());
		return resume;
	}

	private ResumeResponse persist(String text) {
		Instant createdAt = Instant.now();
		Resume resume = new Resume(
				UUID.randomUUID(),
				text,
				createdAt,
				ExpiryCalculator.defaultExpiry(createdAt)
		);
		resumeRepository.save(resume);
		return toResponse(resume);
	}

	private ResumeResponse toResponse(Resume resume) {
		return new ResumeResponse(resume.getId(), resume.getCreatedAt());
	}
}
