package com.example.atssystem.skill.extraction;

import java.util.UUID;

/**
 * A searchable catalog term that resolves to a canonical skill.
 */
public record CatalogTerm(UUID skillId, String canonicalName, String alias) {
}
