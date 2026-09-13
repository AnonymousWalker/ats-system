package com.example.atssystem.skill;

import com.example.atssystem.skill.dto.SkillResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

	private final SkillService skillService;

	public SkillController(SkillService skillService) {
		this.skillService = skillService;
	}

	@GetMapping
	public List<SkillResponse> listCatalog() {
		return skillService.listCatalog();
	}
}
