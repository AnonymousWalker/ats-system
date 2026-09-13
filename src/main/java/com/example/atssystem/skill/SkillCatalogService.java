package com.example.atssystem.skill;

import com.example.atssystem.skill.entity.SkillAlias;
import com.example.atssystem.skill.extraction.CatalogTerm;
import com.example.atssystem.skill.extraction.SkillExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SkillCatalogService {

	private final SkillAliasRepository skillAliasRepository;

	private volatile SkillExtractor cachedExtractor;

	public SkillCatalogService(SkillAliasRepository skillAliasRepository) {
		this.skillAliasRepository = skillAliasRepository;
	}

	@Transactional(readOnly = true)
	public SkillExtractor extractor() {
		SkillExtractor local = cachedExtractor;
		if (local != null) {
			return local;
		}
		synchronized (this) {
			if (cachedExtractor == null) {
				cachedExtractor = new SkillExtractor(loadCatalogTerms());
			}
			return cachedExtractor;
		}
	}

	private List<CatalogTerm> loadCatalogTerms() {
		List<SkillAlias> aliases = skillAliasRepository.findAllWithSkill();
		return aliases.stream()
				.map(alias -> new CatalogTerm(
						alias.getSkill().getId(),
						alias.getSkill().getCanonicalName(),
						alias.getAlias()
				))
				.toList();
	}
}
