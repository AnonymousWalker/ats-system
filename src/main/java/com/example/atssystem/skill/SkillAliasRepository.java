package com.example.atssystem.skill;

import com.example.atssystem.skill.entity.SkillAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface SkillAliasRepository extends JpaRepository<SkillAlias, UUID> {

	@Query("""
			select a from SkillAlias a
			join fetch a.skill
			""")
	List<SkillAlias> findAllWithSkill();
}
