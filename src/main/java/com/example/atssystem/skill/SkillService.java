package com.example.atssystem.skill;

import com.example.atssystem.skill.dto.SkillResponse;
import com.example.atssystem.skill.entity.Skill;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SkillService {

	private final SkillRepository skillRepository;

	public SkillService(SkillRepository skillRepository) {
		this.skillRepository = skillRepository;
	}

	@Transactional(readOnly = true)
	public List<SkillResponse> listCatalog() {
		return skillRepository.findAllByOrderByCanonicalNameAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	private SkillResponse toResponse(Skill skill) {
		return new SkillResponse(skill.getId(), skill.getCanonicalName(), skill.getCategory());
	}
}
