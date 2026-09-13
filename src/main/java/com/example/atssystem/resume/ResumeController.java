package com.example.atssystem.resume;

import com.example.atssystem.resume.dto.CreateResumeRequest;
import com.example.atssystem.resume.dto.ResumeResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

	private final ResumeService resumeService;

	public ResumeController(ResumeService resumeService) {
		this.resumeService = resumeService;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResumeResponse> createFromText(@Valid @RequestBody CreateResumeRequest request) {
		ResumeResponse response = resumeService.createFromText(request);
		return created(response);
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResumeResponse> createFromUpload(@RequestPart("file") MultipartFile file) {
		ResumeResponse response = resumeService.createFromUpload(file);
		return created(response);
	}

	private static ResponseEntity<ResumeResponse> created(ResumeResponse response) {
		URI location = URI.create("/api/resumes/" + response.id());
		return ResponseEntity.created(location).body(response);
	}
}
