package com.example.atssystem.resume;

import com.example.atssystem.common.ExpiryCalculator;
import com.example.atssystem.common.ResourceNotFoundException;
import com.example.atssystem.resume.dto.CreateResumeRequest;
import com.example.atssystem.resume.dto.ResumeResponse;
import com.example.atssystem.resume.entity.Resume;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ResumeService {

	private final ResumeRepository resumeRepository;

	public ResumeService(ResumeRepository resumeRepository) {
		this.resumeRepository = resumeRepository;
	}

	@Transactional
	public ResumeResponse create(CreateResumeRequest request) {
		Instant createdAt = Instant.now();
		Resume resume = new Resume(
				UUID.randomUUID(),
				request.text().trim(),
				createdAt,
				ExpiryCalculator.defaultExpiry(createdAt)
		);
		resumeRepository.save(resume);
		return toResponse(resume);
	}

	@Transactional(readOnly = true)
	public Resume requireById(UUID id) {
		return resumeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Resume not found: " + id));
	}

	private ResumeResponse toResponse(Resume resume) {
		return new ResumeResponse(resume.getId(), resume.getCreatedAt());
	}
}
