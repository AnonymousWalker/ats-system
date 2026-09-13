package com.example.atssystem.analysis;

import com.example.atssystem.analysis.dto.AnalysisResponse;
import com.example.atssystem.analysis.dto.CreateAnalysisRequest;
import com.example.atssystem.analysis.dto.ReviewAnalysisRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

	private final AnalysisService analysisService;

	public AnalysisController(AnalysisService analysisService) {
		this.analysisService = analysisService;
	}

	@PostMapping
	public ResponseEntity<AnalysisResponse> create(@Valid @RequestBody CreateAnalysisRequest request) {
		AnalysisResponse response = analysisService.create(request);
		URI location = URI.create("/api/analyses/" + response.id());
		return ResponseEntity.created(location).body(response);
	}

	@GetMapping("/{id}")
	public AnalysisResponse getById(@PathVariable UUID id) {
		return analysisService.getById(id);
	}

	@PutMapping("/{id}/review")
	public AnalysisResponse review(
			@PathVariable UUID id,
			@Valid @RequestBody ReviewAnalysisRequest request
	) {
		return analysisService.review(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID id) {
		analysisService.delete(id);
	}
}
