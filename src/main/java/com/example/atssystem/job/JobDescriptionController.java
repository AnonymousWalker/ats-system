package com.example.atssystem.job;

import com.example.atssystem.job.dto.CreateJobRequest;
import com.example.atssystem.job.dto.JobResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/jobs")
public class JobDescriptionController {

	private final JobDescriptionService jobDescriptionService;

	public JobDescriptionController(JobDescriptionService jobDescriptionService) {
		this.jobDescriptionService = jobDescriptionService;
	}

	@PostMapping
	public ResponseEntity<JobResponse> create(@Valid @RequestBody CreateJobRequest request) {
		JobResponse response = jobDescriptionService.create(request);
		URI location = URI.create("/api/jobs/" + response.id());
		return ResponseEntity.created(location).body(response);
	}
}
