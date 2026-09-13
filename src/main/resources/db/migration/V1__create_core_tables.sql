CREATE TABLE resumes (
    id UUID PRIMARY KEY,
    text TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE job_descriptions (
    id UUID PRIMARY KEY,
    text TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE analyses (
    id UUID PRIMARY KEY,
    resume_id UUID NOT NULL REFERENCES resumes (id),
    job_id UUID NOT NULL REFERENCES job_descriptions (id),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_analyses_resume_id ON analyses (resume_id);
CREATE INDEX idx_analyses_job_id ON analyses (job_id);
