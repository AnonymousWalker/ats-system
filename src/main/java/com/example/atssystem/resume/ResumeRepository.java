package com.example.atssystem.resume;

import com.example.atssystem.resume.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			delete from Resume r
			where r.expiresAt < :cutoff
			  and not exists (select 1 from Analysis a where a.resume = r)
			""")
	int deleteExpiredUnreferenced(Instant cutoff);
}
