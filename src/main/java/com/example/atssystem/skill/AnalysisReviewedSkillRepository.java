package com.example.atssystem.skill;

import com.example.atssystem.analysis.entity.Analysis;
import com.example.atssystem.skill.entity.AnalysisReviewedSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AnalysisReviewedSkillRepository extends JpaRepository<AnalysisReviewedSkill, UUID> {

	@Query("""
			select s from AnalysisReviewedSkill s
			join fetch s.skill
			where s.analysis = :analysis
			order by s.source, s.skill.canonicalName
			""")
	List<AnalysisReviewedSkill> findByAnalysisWithSkill(Analysis analysis);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from AnalysisReviewedSkill s where s.analysis = :analysis")
	void deleteByAnalysis(Analysis analysis);
}
