import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  ApiError,
  buildReviewDraft,
  deleteAnalysis,
  getAnalysis,
  listSkills,
  reviewAnalysis,
  type Analysis,
  type CatalogSkill,
  type ReviewDraftJobSkill,
  type ReviewDraftResumeSkill,
  type ResultSkill,
  type SkillPriority,
} from '../api/client'

function MissingGroup({
  title,
  skills,
}: {
  title: string
  skills: ResultSkill[]
}) {
  if (skills.length === 0) {
    return null
  }
  return (
    <div className="missing-group">
      <h3 className="section-title">{title}</h3>
      <ul className="skill-list">
        {skills.map((skill) => (
          <li key={skill.skillId} className="skill-item">
            <div className="skill-name">{skill.name}</div>
            <div className="skill-meta">Not found in your resume.</div>
          </li>
        ))}
      </ul>
    </div>
  )
}

export function ReviewPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [analysis, setAnalysis] = useState<Analysis | null>(null)
  const [catalog, setCatalog] = useState<CatalogSkill[]>([])
  const [resumeSkills, setResumeSkills] = useState<ReviewDraftResumeSkill[]>([])
  const [jobSkills, setJobSkills] = useState<ReviewDraftJobSkill[]>([])
  const [error, setError] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [resumeToAdd, setResumeToAdd] = useState('')
  const [jobToAdd, setJobToAdd] = useState('')
  const [jobAddPriority, setJobAddPriority] = useState<SkillPriority>('REQUIRED')

  useEffect(() => {
    if (!id) {
      setError('Missing analysis id.')
      setLoading(false)
      return
    }

    let cancelled = false
    setLoading(true)
    setError(null)

    Promise.all([getAnalysis(id), listSkills()])
      .then(([data, skills]) => {
        if (cancelled) {
          return
        }
        setAnalysis(data)
        setCatalog(skills)
        const draft = buildReviewDraft(data)
        setResumeSkills(draft.resumeSkills)
        setJobSkills(draft.jobSkills)
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          if (err instanceof ApiError && err.status === 410) {
            setError(err.message)
          } else {
            setError(err instanceof ApiError ? err.message : 'Could not load analysis.')
          }
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false)
        }
      })

    return () => {
      cancelled = true
    }
  }, [id])

  const resumeOptions = useMemo(
    () => catalog.filter((skill) => !resumeSkills.some((item) => item.skillId === skill.id)),
    [catalog, resumeSkills],
  )
  const jobOptions = useMemo(
    () => catalog.filter((skill) => !jobSkills.some((item) => item.skillId === skill.id)),
    [catalog, jobSkills],
  )

  const missingRequired = analysis?.result?.missingSkills.filter((s) => s.priority === 'REQUIRED') ?? []
  const missingPreferred = analysis?.result?.missingSkills.filter((s) => s.priority === 'PREFERRED') ?? []

  function addResumeSkill() {
    const skill = catalog.find((item) => item.id === resumeToAdd)
    if (!skill) {
      return
    }
    setResumeSkills((current) => [
      ...current,
      { skillId: skill.id, name: skill.name, evidence: '' },
    ])
    setResumeToAdd('')
    setFormError(null)
  }

  function addJobSkill() {
    const skill = catalog.find((item) => item.id === jobToAdd)
    if (!skill) {
      return
    }
    setJobSkills((current) => [
      ...current,
      {
        skillId: skill.id,
        name: skill.name,
        priority: jobAddPriority,
        evidence: '',
      },
    ])
    setJobToAdd('')
    setFormError(null)
  }

  function resetFromExtracted() {
    if (!analysis) {
      return
    }
    const draft = buildReviewDraft({
      ...analysis,
      status: 'DRAFT',
      reviewedResumeSkills: [],
      reviewedJobSkills: [],
      result: null,
    })
    setResumeSkills(draft.resumeSkills)
    setJobSkills(draft.jobSkills)
    setFormError(null)
  }

  async function onDelete() {
    if (!id) {
      return
    }
    const confirmed = window.confirm(
      'Delete this analysis and its unshared resume/job inputs? This cannot be undone.',
    )
    if (!confirmed) {
      return
    }

    setDeleting(true)
    setFormError(null)
    try {
      await deleteAnalysis(id)
      navigate('/')
    } catch (err: unknown) {
      setFormError(err instanceof ApiError ? err.message : 'Could not delete analysis.')
      setDeleting(false)
    }
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (!id) {
      return
    }
    if (jobSkills.some((skill) => !skill.priority)) {
      setFormError('Every job skill needs a Required or Preferred label.')
      return
    }

    setSaving(true)
    setFormError(null)
    setError(null)

    try {
      const updated = await reviewAnalysis(id, {
        resumeSkills: resumeSkills.map((skill) => ({
          skillId: skill.skillId,
          evidence: skill.evidence.trim() || null,
        })),
        jobSkills: jobSkills.map((skill) => ({
          skillId: skill.skillId,
          priority: skill.priority,
          evidence: skill.evidence.trim() || null,
        })),
      })
      setAnalysis(updated)
      const draft = buildReviewDraft(updated)
      setResumeSkills(draft.resumeSkills)
      setJobSkills(draft.jobSkills)
    } catch (err: unknown) {
      setFormError(err instanceof ApiError ? err.message : 'Could not save review.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="app-shell">
      <div className="brand-row">
        <h1 className="brand">
          Resume–JD <span>Matcher</span>
        </h1>
        <Link className="btn btn-ghost" to="/">
          Start over
        </Link>
      </div>

      <header className="page-header">
        <p className="eyebrow">Review & score</p>
        <h1>Correct extracted skills</h1>
        <p>
          Add or remove skills, fix Required/Preferred labels, then calculate. To change resume or
          JD text, start a new analysis.
        </p>
      </header>

      {loading && <p className="status-line loading">Loading analysis…</p>}

      {error && (
        <div className="alert" role="alert">
          {error}
        </div>
      )}

      {!loading && !error && analysis && (
        <form className="stack" onSubmit={onSubmit}>
          <div className="two-col">
            <section className="panel stack-tight" aria-labelledby="resume-skills-heading">
              <div>
                <h2 id="resume-skills-heading" className="section-title">
                  Resume skills
                </h2>
                <p className="section-help">Skills found (or added) for your resume.</p>
              </div>

              {resumeSkills.length === 0 ? (
                <p className="empty-state">No resume skills yet. Add one from the catalog.</p>
              ) : (
                <ul className="skill-list editable">
                  {resumeSkills.map((skill) => (
                    <li key={skill.skillId} className="skill-item editable-item">
                      <div className="skill-row">
                        <div className="skill-name">{skill.name}</div>
                        <button
                          type="button"
                          className="btn btn-ghost danger-text"
                          onClick={() =>
                            setResumeSkills((current) =>
                              current.filter((item) => item.skillId !== skill.skillId),
                            )
                          }
                          disabled={saving}
                        >
                          Remove
                        </button>
                      </div>
                      <label className="field-label" htmlFor={`resume-evidence-${skill.skillId}`}>
                        Evidence
                      </label>
                      <textarea
                        id={`resume-evidence-${skill.skillId}`}
                        className="evidence-input"
                        value={skill.evidence}
                        disabled={saving}
                        onChange={(event) =>
                          setResumeSkills((current) =>
                            current.map((item) =>
                              item.skillId === skill.skillId
                                ? { ...item, evidence: event.target.value }
                                : item,
                            ),
                          )
                        }
                      />
                    </li>
                  ))}
                </ul>
              )}

              <div className="add-row">
                <label className="sr-only" htmlFor="add-resume-skill">
                  Add resume skill
                </label>
                <select
                  id="add-resume-skill"
                  value={resumeToAdd}
                  disabled={saving || resumeOptions.length === 0}
                  onChange={(event) => setResumeToAdd(event.target.value)}
                >
                  <option value="">Add skill…</option>
                  {resumeOptions.map((skill) => (
                    <option key={skill.id} value={skill.id}>
                      {skill.name}
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={addResumeSkill}
                  disabled={saving || !resumeToAdd}
                >
                  Add
                </button>
              </div>
            </section>

            <section className="panel stack-tight" aria-labelledby="job-skills-heading">
              <div>
                <h2 id="job-skills-heading" className="section-title">
                  Job description skills
                </h2>
                <p className="section-help">
                  Required weighs 2, Preferred weighs 1. Fix ambiguous labels before scoring.
                </p>
              </div>

              {jobSkills.length === 0 ? (
                <p className="empty-state">
                  No job skills yet. Add Required/Preferred skills, or score will be unscorable.
                </p>
              ) : (
                <ul className="skill-list editable">
                  {jobSkills.map((skill) => (
                    <li key={skill.skillId} className="skill-item editable-item">
                      <div className="skill-row">
                        <div className="skill-name">
                          {skill.name}
                          {skill.priorityAmbiguous ? (
                            <span className="badge badge-ambiguous">Needs review</span>
                          ) : null}
                        </div>
                        <button
                          type="button"
                          className="btn btn-ghost danger-text"
                          onClick={() =>
                            setJobSkills((current) =>
                              current.filter((item) => item.skillId !== skill.skillId),
                            )
                          }
                          disabled={saving}
                        >
                          Remove
                        </button>
                      </div>
                      <label className="field-label" htmlFor={`job-priority-${skill.skillId}`}>
                        Priority
                      </label>
                      <select
                        id={`job-priority-${skill.skillId}`}
                        value={skill.priority}
                        disabled={saving}
                        onChange={(event) =>
                          setJobSkills((current) =>
                            current.map((item) =>
                              item.skillId === skill.skillId
                                ? {
                                    ...item,
                                    priority: event.target.value as SkillPriority,
                                    priorityAmbiguous: false,
                                  }
                                : item,
                            ),
                          )
                        }
                      >
                        <option value="REQUIRED">Required</option>
                        <option value="PREFERRED">Preferred</option>
                      </select>
                      <label className="field-label" htmlFor={`job-evidence-${skill.skillId}`}>
                        Evidence
                      </label>
                      <textarea
                        id={`job-evidence-${skill.skillId}`}
                        className="evidence-input"
                        value={skill.evidence}
                        disabled={saving}
                        onChange={(event) =>
                          setJobSkills((current) =>
                            current.map((item) =>
                              item.skillId === skill.skillId
                                ? { ...item, evidence: event.target.value }
                                : item,
                            ),
                          )
                        }
                      />
                    </li>
                  ))}
                </ul>
              )}

              <div className="add-row wrap">
                <label className="sr-only" htmlFor="add-job-skill">
                  Add job skill
                </label>
                <select
                  id="add-job-skill"
                  value={jobToAdd}
                  disabled={saving || jobOptions.length === 0}
                  onChange={(event) => setJobToAdd(event.target.value)}
                >
                  <option value="">Add skill…</option>
                  {jobOptions.map((skill) => (
                    <option key={skill.id} value={skill.id}>
                      {skill.name}
                    </option>
                  ))}
                </select>
                <label className="sr-only" htmlFor="add-job-priority">
                  Priority for new skill
                </label>
                <select
                  id="add-job-priority"
                  value={jobAddPriority}
                  disabled={saving}
                  onChange={(event) => setJobAddPriority(event.target.value as SkillPriority)}
                >
                  <option value="REQUIRED">Required</option>
                  <option value="PREFERRED">Preferred</option>
                </select>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={addJobSkill}
                  disabled={saving || !jobToAdd}
                >
                  Add
                </button>
              </div>
            </section>
          </div>

          {formError && (
            <div className="alert" role="alert">
              {formError}
            </div>
          )}

          <div className="actions">
            <button className="btn btn-primary" type="submit" disabled={saving || deleting}>
              {saving
                ? 'Calculating…'
                : analysis.status === 'REVIEWED'
                  ? 'Review again'
                  : 'Calculate score'}
            </button>
            <button
              className="btn btn-secondary"
              type="button"
              onClick={resetFromExtracted}
              disabled={saving || deleting}
            >
              Reset to extraction
            </button>
            <button
              className="btn btn-secondary danger-text"
              type="button"
              onClick={onDelete}
              disabled={saving || deleting}
            >
              {deleting ? 'Deleting…' : 'Delete analysis'}
            </button>
            <Link className="btn btn-ghost" to="/">
              Start over
            </Link>
          </div>

          {analysis.result && (
            <section className="panel results" aria-labelledby="results-heading">
              <h2 id="results-heading" className="section-title">
                Results
              </h2>

              {analysis.result.scorable ? (
                <>
                  <div className="score-block">
                    <p className="score-value">{analysis.result.score}</p>
                    <p className="score-label">Match score</p>
                  </div>
                  <p className="calc-line">
                    score = round(100 × {analysis.result.matchedWeight} /{' '}
                    {analysis.result.totalWeight}) = {analysis.result.score}
                  </p>
                  <p className="section-help">
                    Required weight 2 · Preferred weight 1 · scoring v
                    {analysis.result.scoringVersion} · catalog v{analysis.result.catalogVersion}
                  </p>

                  <div className="coverage-grid">
                    <div>
                      <h3 className="section-title">Required coverage</h3>
                      <p className="coverage-stat">
                        {analysis.result.requiredCoverage.matched}/
                        {analysis.result.requiredCoverage.total} (
                        {analysis.result.requiredCoverage.percent}%)
                      </p>
                    </div>
                    <div>
                      <h3 className="section-title">Preferred coverage</h3>
                      <p className="coverage-stat">
                        {analysis.result.preferredCoverage.matched}/
                        {analysis.result.preferredCoverage.total} (
                        {analysis.result.preferredCoverage.percent}%)
                      </p>
                    </div>
                  </div>

                  <div className="two-col">
                    <div>
                      <h3 className="section-title">Matched skills</h3>
                      {analysis.result.matchedSkills.length === 0 ? (
                        <p className="empty-state">No overlapping skills.</p>
                      ) : (
                        <ul className="skill-list">
                          {analysis.result.matchedSkills.map((skill) => (
                            <li key={skill.skillId} className="skill-item">
                              <div className="skill-name">
                                {skill.name}
                                <span
                                  className={`badge ${
                                    skill.priority === 'REQUIRED'
                                      ? 'badge-required'
                                      : 'badge-preferred'
                                  }`}
                                >
                                  {skill.priority === 'REQUIRED' ? 'Required' : 'Preferred'}
                                </span>
                              </div>
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                    <div>
                      <h3 className="section-title">Missing skills</h3>
                      {analysis.result.missingSkills.length === 0 ? (
                        <p className="empty-state">Nothing missing — full coverage.</p>
                      ) : (
                        <>
                          <MissingGroup title="Required" skills={missingRequired} />
                          <MissingGroup title="Preferred" skills={missingPreferred} />
                        </>
                      )}
                    </div>
                  </div>
                </>
              ) : (
                <p className="empty-state">
                  Unscorable — the job skill list is empty. Add JD skills and review again.
                </p>
              )}
            </section>
          )}
        </form>
      )}
    </div>
  )
}
