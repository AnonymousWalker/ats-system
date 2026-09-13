package com.example.atssystem.job;

import com.example.atssystem.common.ExpiryCalculator;
import com.example.atssystem.common.ResourceNotFoundException;
import com.example.atssystem.job.dto.CreateJobRequest;
import com.example.atssystem.job.dto.JobResponse;
import com.example.atssystem.job.entity.JobDescription;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class JobDescriptionService {

	private final JobDescriptionRepository jobDescriptionRepository;

	public JobDescriptionService(JobDescriptionRepository jobDescriptionRepository) {
		this.jobDescriptionRepository = jobDescriptionRepository;
	}

	@Transactional
	public JobResponse create(CreateJobRequest request) {
		Instant createdAt = Instant.now();
		JobDescription jobDescription = new JobDescription(
				UUID.randomUUID(),
				request.text().trim(),
				createdAt,
				ExpiryCalculator.defaultExpiry(createdAt)
		);
		jobDescriptionRepository.save(jobDescription);
		return toResponse(jobDescription);
	}

	@Transactional(readOnly = true)
	public JobDescription requireById(UUID id) {
		return jobDescriptionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Job description not found: " + id));
	}

	private JobResponse toResponse(JobDescription jobDescription) {
		return new JobResponse(jobDescription.getId(), jobDescription.getCreatedAt());
	}
}
