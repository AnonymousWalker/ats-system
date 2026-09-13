package com.example.atssystem.analysis;

import com.example.atssystem.analysis.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {
}
