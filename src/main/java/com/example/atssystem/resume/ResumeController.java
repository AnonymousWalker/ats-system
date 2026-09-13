package com.example.atssystem.resume;

import com.example.atssystem.resume.dto.CreateResumeRequest;
import com.example.atssystem.resume.dto.ResumeResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

	private final ResumeService resumeService;

	public ResumeController(ResumeService resumeService) {
		this.resumeService = resumeService;
	}

	@PostMapping
	public ResponseEntity<ResumeResponse> create(@Valid @RequestBody CreateResumeRequest request) {
		ResumeResponse response = resumeService.create(request);
		URI location = URI.create("/api/resumes/" + response.id());
		return ResponseEntity.created(location).body(response);
	}
}
