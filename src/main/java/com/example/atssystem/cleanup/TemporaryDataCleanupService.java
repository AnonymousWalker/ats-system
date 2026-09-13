package com.example.atssystem.cleanup;

import com.example.atssystem.analysis.AnalysisRepository;
import com.example.atssystem.analysis.entity.Analysis;
import com.example.atssystem.job.JobDescriptionRepository;
import com.example.atssystem.resume.ResumeRepository;
import com.example.atssystem.skill.AnalysisReviewedSkillRepository;
import com.example.atssystem.skill.AnalysisSkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class TemporaryDataCleanupService {

	private static final Logger log = LoggerFactory.getLogger(TemporaryDataCleanupService.class);

	private final AnalysisRepository analysisRepository;
	private final AnalysisSkillRepository analysisSkillRepository;
	private final AnalysisReviewedSkillRepository analysisReviewedSkillRepository;
	private final ResumeRepository resumeRepository;
	private final JobDescriptionRepository jobDescriptionRepository;

	public TemporaryDataCleanupService(
			AnalysisRepository analysisRepository,
			AnalysisSkillRepository analysisSkillRepository,
			AnalysisReviewedSkillRepository analysisReviewedSkillRepository,
			ResumeRepository resumeRepository,
			JobDescriptionRepository jobDescriptionRepository
	) {
		this.analysisRepository = analysisRepository;
		this.analysisSkillRepository = analysisSkillRepository;
		this.analysisReviewedSkillRepository = analysisReviewedSkillRepository;
		this.resumeRepository = resumeRepository;
		this.jobDescriptionRepository = jobDescriptionRepository;
	}

	@Scheduled(cron = "0 0 * * * *")
	@Transactional
	public void cleanupExpired() {
		Instant now = Instant.now();
		List<Analysis> expiredAnalyses = analysisRepository.findExpiredWithInputs(now);
		for (Analysis analysis : expiredAnalyses) {
			analysisReviewedSkillRepository.deleteByAnalysis(analysis);
			analysisSkillRepository.deleteByAnalysis(analysis);
			analysisRepository.delete(analysis);
		}
		analysisRepository.flush();

		int deletedResumes = resumeRepository.deleteExpiredUnreferenced(now);
		int deletedJobs = jobDescriptionRepository.deleteExpiredUnreferenced(now);

		log.info(
				"Cleanup finished: removed {} expired analyses, {} unreferenced resumes, {} unreferenced jobs",
				expiredAnalyses.size(),
				deletedResumes,
				deletedJobs
		);
	}
}
