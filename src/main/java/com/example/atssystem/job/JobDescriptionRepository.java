package com.example.atssystem.job;

import com.example.atssystem.job.entity.JobDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.UUID;

public interface JobDescriptionRepository extends JpaRepository<JobDescription, UUID> {

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			delete from JobDescription j
			where j.expiresAt < :cutoff
			  and not exists (select 1 from Analysis a where a.jobDescription = j)
			""")
	int deleteExpiredUnreferenced(Instant cutoff);
}
