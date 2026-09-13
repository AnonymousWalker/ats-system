package com.example.atssystem.analysis;

import com.example.atssystem.analysis.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {

	boolean existsByResume_Id(UUID resumeId);

	boolean existsByJobDescription_Id(UUID jobId);

	@Query("select a from Analysis a join fetch a.resume join fetch a.jobDescription where a.expiresAt < :cutoff")
	List<Analysis> findExpiredWithInputs(Instant cutoff);
}
