package com.example.atssystem.analysis;

import com.example.atssystem.analysis.domain.AnalysisStatus;
import com.example.atssystem.analysis.dto.AnalysisResponse;
import com.example.atssystem.analysis.dto.AnalysisResultResponse;
import com.example.atssystem.analysis.dto.CreateAnalysisRequest;
import com.example.atssystem.analysis.dto.ExtractedSkillResponse;
import com.example.atssystem.analysis.dto.ReviewAnalysisRequest;
import com.example.atssystem.analysis.dto.ReviewedSkillResponse;
import com.example.atssystem.analysis.entity.Analysis;
import com.example.atssystem.analysis.scoring.CoverageBreakdown;
import com.example.atssystem.analysis.scoring.MatchScorer;
import com.example.atssystem.analysis.scoring.ScoredSkill;
import com.example.atssystem.analysis.scoring.ScoringResult;
import com.example.atssystem.common.BadRequestException;
import com.example.atssystem.common.ExpiryCalculator;
import com.example.atssystem.common.ResourceNotFoundException;
import com.example.atssystem.job.JobDescriptionService;
import com.example.atssystem.job.entity.JobDescription;
import com.example.atssystem.resume.ResumeService;
import com.example.atssystem.resume.entity.Resume;
import com.example.atssystem.skill.AnalysisReviewedSkillRepository;
import com.example.atssystem.skill.AnalysisSkillRepository;
import com.example.atssystem.skill.SkillCatalogService;
import com.example.atssystem.skill.SkillRepository;
import com.example.atssystem.skill.domain.SkillSource;
import com.example.atssystem.skill.entity.AnalysisReviewedSkill;
import com.example.atssystem.skill.entity.AnalysisSkill;
import com.example.atssystem.skill.entity.Skill;
import com.example.atssystem.skill.extraction.ExtractedSkill;
import com.example.atssystem.skill.extraction.SkillExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AnalysisService {

	private final AnalysisRepository analysisRepository;
	private final ResumeService resumeService;
	private final JobDescriptionService jobDescriptionService;
	private final SkillCatalogService skillCatalogService;
	private final SkillRepository skillRepository;
	private final AnalysisSkillRepository analysisSkillRepository;
	private final AnalysisReviewedSkillRepository analysisReviewedSkillRepository;

	public AnalysisService(
			AnalysisRepository analysisRepository,
			ResumeService resumeService,
			JobDescriptionService jobDescriptionService,
			SkillCatalogService skillCatalogService,
			SkillRepository skillRepository,
			AnalysisSkillRepository analysisSkillRepository,
			AnalysisReviewedSkillRepository analysisReviewedSkillRepository
	) {
		this.analysisRepository = analysisRepository;
		this.resumeService = resumeService;
		this.jobDescriptionService = jobDescriptionService;
		this.skillCatalogService = skillCatalogService;
		this.skillRepository = skillRepository;
		this.analysisSkillRepository = analysisSkillRepository;
		this.analysisReviewedSkillRepository = analysisReviewedSkillRepository;
	}

	@Transactional
	public AnalysisResponse create(CreateAnalysisRequest request) {
		Resume resume = resumeService.requireById(request.resumeId());
		JobDescription jobDescription = jobDescriptionService.requireById(request.jobId());

		Instant createdAt = Instant.now();
		Analysis analysis = new Analysis(
				UUID.randomUUID(),
				resume,
				jobDescription,
				AnalysisStatus.DRAFT,
				createdAt,
				ExpiryCalculator.earliest(resume.getExpiresAt(), jobDescription.getExpiresAt())
		);
		analysisRepository.save(analysis);

		SkillExtractor extractor = skillCatalogService.extractor();
		List<ExtractedSkill> resumeSkills = extractor.extractFromResume(resume.getText());
		List<ExtractedSkill> jobSkills = extractor.extractFromJobDescription(jobDescription.getText());

		Map<UUID, Skill> skillsById = loadSkills(collectSkillIds(resumeSkills, jobSkills));

		List<AnalysisSkill> persisted = new ArrayList<>();
		persisted.addAll(toExtractedEntities(analysis, resumeSkills, SkillSource.RESUME, skillsById));
		persisted.addAll(toExtractedEntities(analysis, jobSkills, SkillSource.JOB, skillsById));
		analysisSkillRepository.saveAll(persisted);

		return toResponse(analysis, persisted, List.of(), null);
	}

	@Transactional(readOnly = true)
	public AnalysisResponse getById(UUID id) {
		Analysis analysis = requireAnalysis(id);
		List<AnalysisSkill> extracted = analysisSkillRepository.findByAnalysisWithSkill(analysis);
		List<AnalysisReviewedSkill> reviewed = analysisReviewedSkillRepository.findByAnalysisWithSkill(analysis);
		return toResponse(analysis, extracted, reviewed, toResultResponse(analysis, reviewed));
	}

	@Transactional
	public AnalysisResponse review(UUID id, ReviewAnalysisRequest request) {
		Analysis analysis = requireAnalysis(id);

		validateNoDuplicateSkills(request.resumeSkills().stream().map(ReviewAnalysisRequest.ReviewedResumeSkillRequest::skillId).toList(), "resumeSkills");
		validateNoDuplicateSkills(request.jobSkills().stream().map(ReviewAnalysisRequest.ReviewedJobSkillRequest::skillId).toList(), "jobSkills");

		List<UUID> requestedIds = Stream.concat(
				request.resumeSkills().stream().map(ReviewAnalysisRequest.ReviewedResumeSkillRequest::skillId),
				request.jobSkills().stream().map(ReviewAnalysisRequest.ReviewedJobSkillRequest::skillId)
		).distinct().toList();

		Map<UUID, Skill> skillsById = loadSkills(requestedIds);
		if (skillsById.size() != requestedIds.size()) {
			Set<UUID> missing = new HashSet<>(requestedIds);
			missing.removeAll(skillsById.keySet());
			throw new BadRequestException("Unknown skillId(s): " + missing);
		}

		analysisReviewedSkillRepository.deleteByAnalysis(analysis);
		analysisReviewedSkillRepository.flush();

		List<AnalysisReviewedSkill> reviewed = new ArrayList<>();
		for (ReviewAnalysisRequest.ReviewedResumeSkillRequest item : request.resumeSkills()) {
			reviewed.add(new AnalysisReviewedSkill(
					UUID.randomUUID(),
					analysis,
					skillsById.get(item.skillId()),
					SkillSource.RESUME,
					null,
					blankToNull(item.evidence())
			));
		}
		for (ReviewAnalysisRequest.ReviewedJobSkillRequest item : request.jobSkills()) {
			reviewed.add(new AnalysisReviewedSkill(
					UUID.randomUUID(),
					analysis,
					skillsById.get(item.skillId()),
					SkillSource.JOB,
					item.priority(),
					blankToNull(item.evidence())
			));
		}
		analysisReviewedSkillRepository.saveAll(reviewed);

		List<ScoredSkill> resumeForScore = reviewed.stream()
				.filter(skill -> skill.getSource() == SkillSource.RESUME)
				.map(skill -> new ScoredSkill(skill.getSkill().getId(), skill.getSkill().getCanonicalName(), null, skill.getEvidence()))
				.toList();
		List<ScoredSkill> jobForScore = reviewed.stream()
				.filter(skill -> skill.getSource() == SkillSource.JOB)
				.map(skill -> new ScoredSkill(
						skill.getSkill().getId(),
						skill.getSkill().getCanonicalName(),
						skill.getPriority(),
						skill.getEvidence()
				))
				.toList();

		ScoringResult scoringResult = MatchScorer.score(resumeForScore, jobForScore);
		analysis.applyReview(scoringResult, Instant.now());
		analysisRepository.save(analysis);

		List<AnalysisSkill> extracted = analysisSkillRepository.findByAnalysisWithSkill(analysis);
		return toResponse(analysis, extracted, reviewed, toResultResponse(scoringResult));
	}

	private Analysis requireAnalysis(UUID id) {
		return analysisRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Analysis not found: " + id));
	}

	private Map<UUID, Skill> loadSkills(List<UUID> skillIds) {
		if (skillIds.isEmpty()) {
			return Map.of();
		}
		return skillRepository.findAllById(skillIds).stream()
				.collect(Collectors.toMap(Skill::getId, Function.identity()));
	}

	private static List<UUID> collectSkillIds(List<ExtractedSkill> resumeSkills, List<ExtractedSkill> jobSkills) {
		List<UUID> ids = new ArrayList<>();
		resumeSkills.forEach(skill -> ids.add(skill.skillId()));
		jobSkills.forEach(skill -> ids.add(skill.skillId()));
		return ids;
	}

	private static void validateNoDuplicateSkills(List<UUID> skillIds, String field) {
		Set<UUID> seen = new HashSet<>();
		for (UUID skillId : skillIds) {
			if (!seen.add(skillId)) {
				throw new BadRequestException("Duplicate skillId in " + field + ": " + skillId);
			}
		}
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private List<AnalysisSkill> toExtractedEntities(
			Analysis analysis,
			List<ExtractedSkill> extracted,
			SkillSource source,
			Map<UUID, Skill> skillsById
	) {
		List<AnalysisSkill> entities = new ArrayList<>(extracted.size());
		for (ExtractedSkill skill : extracted) {
			Skill catalogSkill = skillsById.get(skill.skillId());
			if (catalogSkill == null) {
				throw new IllegalStateException("Missing catalog skill: " + skill.skillId());
			}
			entities.add(new AnalysisSkill(
					UUID.randomUUID(),
					analysis,
					catalogSkill,
					source,
					source == SkillSource.JOB ? skill.priority() : null,
					source == SkillSource.JOB && skill.priorityAmbiguous(),
					skill.evidence(),
					skill.matchedTerm()
			));
		}
		return entities;
	}

	private AnalysisResponse toResponse(
			Analysis analysis,
			List<AnalysisSkill> extracted,
			List<AnalysisReviewedSkill> reviewed,
			AnalysisResultResponse result
	) {
		List<ExtractedSkillResponse> resumeSkills = extracted.stream()
				.filter(skill -> skill.getSource() == SkillSource.RESUME)
				.map(this::toExtractedResponse)
				.toList();
		List<ExtractedSkillResponse> jobSkills = extracted.stream()
				.filter(skill -> skill.getSource() == SkillSource.JOB)
				.map(this::toExtractedResponse)
				.toList();
		List<ReviewedSkillResponse> reviewedResumeSkills = reviewed.stream()
				.filter(skill -> skill.getSource() == SkillSource.RESUME)
				.map(this::toReviewedResponse)
				.toList();
		List<ReviewedSkillResponse> reviewedJobSkills = reviewed.stream()
				.filter(skill -> skill.getSource() == SkillSource.JOB)
				.map(this::toReviewedResponse)
				.toList();

		return new AnalysisResponse(
				analysis.getId(),
				analysis.getResume().getId(),
				analysis.getJobDescription().getId(),
				analysis.getStatus(),
				analysis.getCreatedAt(),
				analysis.getReviewedAt(),
				resumeSkills,
				jobSkills,
				reviewedResumeSkills,
				reviewedJobSkills,
				result
		);
	}

	private AnalysisResultResponse toResultResponse(Analysis analysis, List<AnalysisReviewedSkill> reviewed) {
		if (analysis.getStatus() != AnalysisStatus.REVIEWED || analysis.getScorable() == null) {
			return null;
		}

		List<AnalysisResultResponse.ResultSkillResponse> matched = new ArrayList<>();
		List<AnalysisResultResponse.ResultSkillResponse> missing = new ArrayList<>();
		Set<UUID> resumeIds = reviewed.stream()
				.filter(skill -> skill.getSource() == SkillSource.RESUME)
				.map(skill -> skill.getSkill().getId())
				.collect(Collectors.toSet());

		for (AnalysisReviewedSkill jobSkill : reviewed) {
			if (jobSkill.getSource() != SkillSource.JOB) {
				continue;
			}
			AnalysisResultResponse.ResultSkillResponse item = new AnalysisResultResponse.ResultSkillResponse(
					jobSkill.getSkill().getId(),
					jobSkill.getSkill().getCanonicalName(),
					jobSkill.getPriority(),
					jobSkill.getEvidence()
			);
			if (resumeIds.contains(jobSkill.getSkill().getId())) {
				matched.add(item);
			} else {
				missing.add(item);
			}
		}

		return new AnalysisResultResponse(
				analysis.getScorable(),
				analysis.getScore(),
				analysis.getMatchedWeight() == null ? 0 : analysis.getMatchedWeight(),
				analysis.getTotalWeight() == null ? 0 : analysis.getTotalWeight(),
				analysis.getScoringVersion(),
				analysis.getCatalogVersion(),
				matched,
				missing,
				CoverageBreakdown.of(
						analysis.getRequiredMatched() == null ? 0 : analysis.getRequiredMatched(),
						analysis.getRequiredTotal() == null ? 0 : analysis.getRequiredTotal()
				),
				CoverageBreakdown.of(
						analysis.getPreferredMatched() == null ? 0 : analysis.getPreferredMatched(),
						analysis.getPreferredTotal() == null ? 0 : analysis.getPreferredTotal()
				)
		);
	}

	private AnalysisResultResponse toResultResponse(ScoringResult scoringResult) {
		return new AnalysisResultResponse(
				scoringResult.scorable(),
				scoringResult.score(),
				scoringResult.matchedWeight(),
				scoringResult.totalWeight(),
				scoringResult.scoringVersion(),
				scoringResult.catalogVersion(),
				scoringResult.matchedSkills().stream()
						.map(skill -> new AnalysisResultResponse.ResultSkillResponse(
								skill.skillId(),
								skill.name(),
								skill.priority(),
								skill.evidence()
						))
						.toList(),
				scoringResult.missingSkills().stream()
						.map(skill -> new AnalysisResultResponse.ResultSkillResponse(
								skill.skillId(),
								skill.name(),
								skill.priority(),
								skill.evidence()
						))
						.toList(),
				scoringResult.requiredCoverage(),
				scoringResult.preferredCoverage()
		);
	}

	private ExtractedSkillResponse toExtractedResponse(AnalysisSkill skill) {
		return new ExtractedSkillResponse(
				skill.getSkill().getId(),
				skill.getSkill().getCanonicalName(),
				skill.getMatchedTerm(),
				skill.getEvidence(),
				skill.getPriority(),
				skill.isPriorityAmbiguous()
		);
	}

	private ReviewedSkillResponse toReviewedResponse(AnalysisReviewedSkill skill) {
		return new ReviewedSkillResponse(
				skill.getSkill().getId(),
				skill.getSkill().getCanonicalName(),
				skill.getPriority(),
				skill.getEvidence()
		);
	}
}
