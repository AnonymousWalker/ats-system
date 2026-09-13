ALTER TABLE analyses
    ADD COLUMN score INTEGER,
    ADD COLUMN scorable BOOLEAN,
    ADD COLUMN matched_weight INTEGER,
    ADD COLUMN total_weight INTEGER,
    ADD COLUMN required_matched INTEGER,
    ADD COLUMN required_total INTEGER,
    ADD COLUMN preferred_matched INTEGER,
    ADD COLUMN preferred_total INTEGER,
    ADD COLUMN scoring_version VARCHAR(32),
    ADD COLUMN catalog_version VARCHAR(32),
    ADD COLUMN reviewed_at TIMESTAMPTZ;

CREATE TABLE analysis_reviewed_skills (
    id UUID PRIMARY KEY,
    analysis_id UUID NOT NULL REFERENCES analyses (id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills (id),
    source VARCHAR(16) NOT NULL,
    priority VARCHAR(16),
    evidence TEXT,
    CONSTRAINT uq_analysis_reviewed_skill_source UNIQUE (analysis_id, skill_id, source),
    CONSTRAINT chk_reviewed_skill_source CHECK (source IN ('RESUME', 'JOB')),
    CONSTRAINT chk_reviewed_skill_priority CHECK (
        (source = 'RESUME' AND priority IS NULL)
        OR (source = 'JOB' AND priority IN ('REQUIRED', 'PREFERRED'))
    )
);

CREATE INDEX idx_analysis_reviewed_skills_analysis_id ON analysis_reviewed_skills (analysis_id);
