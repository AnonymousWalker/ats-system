import { useEffect, useMemo, useState, type FormEvent } from 'react'
import {
  ApiError,
  buildReviewDraft,
  createAnalysis,
  createJob,
  createResumeFromFile,
  createResumeFromText,
  deleteAnalysis,
  getAnalysis,
  listSkills,
  reviewAnalysis,
  type Analysis,
  type ReviewDraftJobSkill,
  type ResultSkill,
} from './api/client'
import { SAMPLE_JOB, SAMPLE_RESUME } from './data/samples'
import FilePanel from './FilePanel'
import ScoreGauge from './ScoreGauge'

const MAX_FILE_BYTES = 5 * 1024 * 1024
const RESUME_ACCEPT = '.pdf,.docx,.txt,.md'
const JOB_ACCEPT = '.txt,.md'

type Run = {
  id: string
  createdAt: string
  resumeLabel: string
  jobLabel: string
  score: number | null
}

function isPlainText(file: File): boolean {
  const name = file.name.toLowerCase()
  return name.endsWith('.txt') || name.endsWith('.md')
}

function ensureSize(file: File, label: string) {
  if (file.size > MAX_FILE_BYTES) {
    throw new Error(`${label} file exceeds the 5 MB limit.`)
  }
}

async function createResume(file: File | null, text: string) {
  if (!file) {
    return createResumeFromText(text.trim())
  }
  ensureSize(file, 'Resume')
  if (isPlainText(file)) {
    return createResumeFromText((await file.text()).trim())
  }
  return createResumeFromFile(file)
}

async function readJobText(file: File | null, text: string): Promise<string> {
  if (!file) {
    return text.trim()
  }
  ensureSize(file, 'Job description')
  if (!isPlainText(file)) {
    throw new Error('Job description files must be .txt or .md. Paste the text instead.')
  }
  return (await file.text()).trim()
}

function submitReview(analysis: Analysis, jobSkills: ReviewDraftJobSkill[]) {
  const { resumeSkills } = buildReviewDraft(analysis)
  return reviewAnalysis(analysis.id, {
    resumeSkills: resumeSkills.map((skill) => ({
      skillId: skill.skillId,
      evidence: skill.evidence || null,
    })),
    jobSkills: jobSkills.map((skill) => ({
      skillId: skill.skillId,
      priority: skill.priority,
      evidence: skill.evidence || null,
    })),
  })
}

function errorMessage(err: unknown, fallback: string): string {
  if (err instanceof ApiError || err instanceof Error) {
    return err.message
  }
  return fallback
}

export default function App() {
  const [resumeFile, setResumeFile] = useState<File | null>(null)
  const [jdFile, setJdFile] = useState<File | null>(null)
  const [resumeText, setResumeText] = useState('')
  const [jdText, setJdText] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [current, setCurrent] = useState<Analysis | null>(null)
  const [currentRun, setCurrentRun] = useState<Run | null>(null)
  const [jobSkills, setJobSkills] = useState<ReviewDraftJobSkill[]>([])
  const [dirty, setDirty] = useState(false)
  const [history, setHistory] = useState<Run[]>([])
  const [healthy, setHealthy] = useState<boolean | null>(null)

  const canSubmit = useMemo(
    () => Boolean((resumeFile || resumeText.trim()) && (jdFile || jdText.trim()) && !busy),
    [resumeFile, jdFile, resumeText, jdText, busy],
  )

  useEffect(() => {
    listSkills()
      .then(() => setHealthy(true))
      .catch(() => setHealthy(false))
  }, [])

  function show(analysis: Analysis, run: Run, skills: ReviewDraftJobSkill[]) {
    const updated = { ...run, score: analysis.result?.score ?? null }
    setCurrent(analysis)
    setCurrentRun(updated)
    setJobSkills(skills)
    setDirty(false)
    setHistory((items) => [updated, ...items.filter((item) => item.id !== updated.id)])
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setBusy(true)

    try {
      const jobText = await readJobText(jdFile, jdText)
      const resume = await createResume(resumeFile, resumeText)
      const job = await createJob(jobText)
      const created = await createAnalysis(resume.id, job.id)
      const draft = buildReviewDraft(created)
      const reviewed = await submitReview(created, draft.jobSkills)
      show(
        reviewed,
        {
          id: reviewed.id,
          createdAt: reviewed.createdAt,
          resumeLabel: resumeFile?.name ?? 'Pasted resume',
          jobLabel: jdFile?.name ?? 'pasted job description',
          score: null,
        },
        draft.jobSkills,
      )
    } catch (err) {
      setError(errorMessage(err, 'Scoring failed.'))
    } finally {
      setBusy(false)
    }
  }

  async function onRescore() {
    if (!current || !currentRun) {
      return
    }
    setError(null)
    setBusy(true)
    try {
      show(await submitReview(current, jobSkills), currentRun, jobSkills)
    } catch (err) {
      setError(errorMessage(err, 'Rescoring failed.'))
    } finally {
      setBusy(false)
    }
  }

  async function onSelect(run: Run) {
    setError(null)
    try {
      const analysis = await getAnalysis(run.id)
      show(analysis, run, buildReviewDraft(analysis).jobSkills)
    } catch (err) {
      setError(errorMessage(err, 'Could not load that evaluation.'))
      setHistory((items) => items.filter((item) => item.id !== run.id))
    }
  }

  async function onDelete() {
    if (!current) {
      return
    }
    setError(null)
    setBusy(true)
    try {
      await deleteAnalysis(current.id)
      setHistory((items) => items.filter((item) => item.id !== current.id))
      setCurrent(null)
      setCurrentRun(null)
      setJobSkills([])
    } catch (err) {
      setError(errorMessage(err, 'Could not delete analysis.'))
    } finally {
      setBusy(false)
    }
  }

  function togglePriority(skillId: string) {
    setJobSkills((skills) =>
      skills.map((skill) =>
        skill.skillId === skillId
          ? {
              ...skill,
              priority: skill.priority === 'REQUIRED' ? 'PREFERRED' : 'REQUIRED',
              priorityAmbiguous: false,
            }
          : skill,
      ),
    )
    setDirty(true)
  }

  function clearInputs() {
    setResumeFile(null)
    setJdFile(null)
    setResumeText('')
    setJdText('')
    setError(null)
  }

  function loadSample() {
    clearInputs()
    setResumeText(SAMPLE_RESUME)
    setJdText(SAMPLE_JOB)
  }

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <div className="mark">
            <span />
            Resume–JD Matcher
          </div>
          <h1>Score a resume against the role.</h1>
          <p className="lede">
            Upload or paste a resume and a job description. Skills are extracted deterministically
            and weighted Required ×2, Preferred ×1.
          </p>
        </div>
        <div className="health">
          <i className={`dot ${healthy ? 'on' : healthy === false ? 'off' : ''}`} />
          {healthy === null ? 'Checking API' : healthy ? 'API connected' : 'API offline'}
        </div>
      </header>

      <div className="layout">
        <form className="card card-pad" onSubmit={onSubmit}>
          <p className="section-title">New evaluation</p>
          <div className="pair">
            <FilePanel
              title="Resume"
              formats="pdf, docx, txt, md"
              accept={RESUME_ACCEPT}
              file={resumeFile}
              text={resumeText}
              disabled={busy}
              onFile={setResumeFile}
              onText={setResumeText}
            />
            <FilePanel
              title="Job description"
              formats="txt, md"
              accept={JOB_ACCEPT}
              file={jdFile}
              text={jdText}
              disabled={busy}
              onFile={setJdFile}
              onText={setJdText}
            />
          </div>
          <div className="actions">
            <button className="primary" type="submit" disabled={!canSubmit}>
              {busy ? 'Scoring…' : 'Run scoring'}
            </button>
            <button className="ghost" type="button" disabled={busy} onClick={loadSample}>
              Use sample
            </button>
            <button className="ghost" type="button" disabled={busy} onClick={clearInputs}>
              Clear
            </button>
          </div>
          {error ? (
            <div className="error" role="alert">
              {error}
            </div>
          ) : null}
        </form>

        <section className="card card-pad">
          {current && currentRun ? (
            <ResultPanel
              analysis={current}
              run={currentRun}
              jobSkills={jobSkills}
              dirty={dirty}
              busy={busy}
              onToggle={togglePriority}
              onRescore={onRescore}
              onDelete={onDelete}
            />
          ) : (
            <p className="empty">
              Results appear here after a run. Include Required / Preferred headings in the job
              description for better priority detection.
            </p>
          )}
        </section>
      </div>

      <section className="history">
        <p className="section-title">Recent evaluations</p>
        <div className="history-list">
          {history.length === 0 ? (
            <p className="empty">No evaluations yet in this session.</p>
          ) : (
            history.map((item) => (
              <button
                key={item.id}
                className={`history-item ${currentRun?.id === item.id ? 'selected' : ''}`}
                type="button"
                disabled={busy}
                onClick={() => void onSelect(item)}
              >
                <div>
                  <b>{item.resumeLabel}</b>
                  <span>
                    {new Date(item.createdAt).toLocaleString()} · vs {item.jobLabel}
                  </span>
                </div>
                <div className="history-score">{item.score ?? '—'}</div>
              </button>
            ))
          )}
        </div>
      </section>
    </div>
  )
}

function summarize(score: number): string {
  if (score >= 75) {
    return 'Strong match'
  }
  if (score >= 50) {
    return 'Partial match'
  }
  return 'Weak match'
}

function SkillChips({ skills, tone }: { skills: ResultSkill[]; tone: 'match' | 'miss' }) {
  if (skills.length === 0) {
    return <span className="empty">None</span>
  }
  return (
    <>
      {skills.map((skill) => (
        <span className={`chip ${tone}`} key={skill.skillId}>
          {skill.name}
          <span className="tag">{skill.priority === 'REQUIRED' ? 'req' : 'pref'}</span>
        </span>
      ))}
    </>
  )
}

type ResultPanelProps = {
  analysis: Analysis
  run: Run
  jobSkills: ReviewDraftJobSkill[]
  dirty: boolean
  busy: boolean
  onToggle: (skillId: string) => void
  onRescore: () => void
  onDelete: () => void
}

function ResultPanel({
  analysis,
  run,
  jobSkills,
  dirty,
  busy,
  onToggle,
  onRescore,
  onDelete,
}: ResultPanelProps) {
  const result = analysis.result
  const resumeSkills = analysis.reviewedResumeSkills.length
    ? analysis.reviewedResumeSkills
    : analysis.resumeSkills

  return (
    <div>
      <div className="result-head">
        <div className="score-copy">
          <p className="section-title">Match score</p>
          <h2>
            {result?.scorable && result.score != null
              ? summarize(result.score)
              : 'No job skills to score against'}
          </h2>
          <p>
            {run.resumeLabel} scored against {run.jobLabel}.
          </p>
          {result?.scorable ? (
            <div className="meta">
              score = round(100 × {result.matchedWeight} / {result.totalWeight}) · Required{' '}
              {result.requiredCoverage.matched}/{result.requiredCoverage.total} · Preferred{' '}
              {result.preferredCoverage.matched}/{result.preferredCoverage.total}
            </div>
          ) : null}
        </div>
        {result?.scorable && result.score != null ? <ScoreGauge score={result.score} /> : null}
      </div>

      {result ? (
        <div className="skill-grid">
          <div className="skill-col">
            <h3>Matched</h3>
            <div className="chips">
              <SkillChips skills={result.matchedSkills} tone="match" />
            </div>
          </div>
          <div className="skill-col">
            <h3>Missing</h3>
            <div className="chips">
              <SkillChips skills={result.missingSkills} tone="miss" />
            </div>
          </div>
        </div>
      ) : null}

      <div className="skill-col block">
        <h3>Job skills</h3>
        <p className="help">
          Click a skill to switch Required ⇄ Preferred. Dashed ones had no clear heading.
        </p>
        <div className="chips">
          {jobSkills.length === 0 ? (
            <span className="empty">None found</span>
          ) : (
            jobSkills.map((skill) => (
              <button
                key={skill.skillId}
                type="button"
                className={`chip toggle ${skill.priority === 'REQUIRED' ? 'required' : 'preferred'} ${
                  skill.priorityAmbiguous ? 'ambiguous' : ''
                }`}
                disabled={busy}
                onClick={() => onToggle(skill.skillId)}
              >
                {skill.name}
                <span className="tag">{skill.priority === 'REQUIRED' ? 'req' : 'pref'}</span>
              </button>
            ))
          )}
        </div>
      </div>

      <div className="skill-col block">
        <h3>Resume skills found</h3>
        <div className="chips">
          {resumeSkills.length === 0 ? (
            <span className="empty">None found</span>
          ) : (
            resumeSkills.map((skill) => (
              <span className="chip" key={skill.skillId}>
                {skill.name}
              </span>
            ))
          )}
        </div>
      </div>

      <div className="actions">
        {dirty ? (
          <button className="primary" type="button" disabled={busy} onClick={onRescore}>
            {busy ? 'Scoring…' : 'Rescore'}
          </button>
        ) : null}
        <button className="ghost danger" type="button" disabled={busy} onClick={onDelete}>
          Delete analysis
        </button>
      </div>
    </div>
  )
}
