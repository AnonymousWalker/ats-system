package com.example.atssystem.skill;

import com.example.atssystem.analysis.entity.Analysis;
import com.example.atssystem.skill.entity.AnalysisSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AnalysisSkillRepository extends JpaRepository<AnalysisSkill, UUID> {

	@Query("""
			select s from AnalysisSkill s
			join fetch s.skill
			where s.analysis = :analysis
			order by s.source, s.skill.canonicalName
			""")
	List<AnalysisSkill> findByAnalysisWithSkill(Analysis analysis);
}
