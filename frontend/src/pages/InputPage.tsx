import { useId, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  ApiError,
  createAnalysis,
  createJob,
  createResumeFromFile,
  createResumeFromText,
} from '../api/client'
import { SAMPLE_JOB, SAMPLE_RESUME } from '../data/samples'

const MAX_FILE_BYTES = 5 * 1024 * 1024

type ResumeMode = 'paste' | 'upload'

export function InputPage() {
  const navigate = useNavigate()
  const resumeFieldId = useId()
  const jobFieldId = useId()
  const fileFieldId = useId()

  const [resumeMode, setResumeMode] = useState<ResumeMode>('paste')
  const [resumeText, setResumeText] = useState('')
  const [jobText, setJobText] = useState('')
  const [file, setFile] = useState<File | null>(null)
  const [fieldError, setFieldError] = useState<string | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  function loadSample() {
    setResumeMode('paste')
    setResumeText(SAMPLE_RESUME)
    setJobText(SAMPLE_JOB)
    setFile(null)
    setFieldError(null)
    setSubmitError(null)
  }

  function validate(): string | null {
    if (resumeMode === 'paste' && !resumeText.trim()) {
      return 'Paste resume text, or switch to upload.'
    }
    if (resumeMode === 'upload') {
      if (!file) {
        return 'Choose a PDF or DOCX resume file.'
      }
      if (file.size > MAX_FILE_BYTES) {
        return 'File exceeds the 5 MB limit. Use a smaller file or paste text.'
      }
      const name = file.name.toLowerCase()
      if (!name.endsWith('.pdf') && !name.endsWith('.docx')) {
        return 'Only PDF and DOCX uploads are supported.'
      }
    }
    if (!jobText.trim()) {
      return 'Enter a job description.'
    }
    return null
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    const validationError = validate()
    if (validationError) {
      setFieldError(validationError)
      setSubmitError(null)
      return
    }

    setFieldError(null)
    setSubmitError(null)
    setLoading(true)

    try {
      const resume =
        resumeMode === 'upload' && file
          ? await createResumeFromFile(file)
          : await createResumeFromText(resumeText.trim())
      const job = await createJob(jobText.trim())
      const analysis = await createAnalysis(resume.id, job.id)
      navigate(`/analyses/${analysis.id}`)
    } catch (error) {
      const message =
        error instanceof ApiError
          ? error.message
          : 'Something went wrong while creating the analysis.'
      setSubmitError(message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="app-shell">
      <div className="brand-row">
        <h1 className="brand">
          Resume–JD <span>Matcher</span>
        </h1>
        <p className="eyebrow">Deterministic skill match for software roles</p>
      </div>

      <form className="panel stack" onSubmit={onSubmit} noValidate>
        <section>
          <h2 className="section-title">Resume</h2>
          <p className="section-help">
            Paste text or upload a PDF/DOCX. Image-only scans are not supported.
          </p>

          <div className="mode-toggle" role="group" aria-label="Resume input mode">
            <button
              type="button"
              aria-pressed={resumeMode === 'paste'}
              onClick={() => {
                setResumeMode('paste')
                setFieldError(null)
              }}
            >
              Paste text
            </button>
            <button
              type="button"
              aria-pressed={resumeMode === 'upload'}
              onClick={() => {
                setResumeMode('upload')
                setFieldError(null)
              }}
            >
              Upload file
            </button>
          </div>

          {resumeMode === 'paste' ? (
            <>
              <label className="field-label" htmlFor={resumeFieldId}>
                Resume text
              </label>
              <textarea
                id={resumeFieldId}
                value={resumeText}
                onChange={(event) => setResumeText(event.target.value)}
                placeholder="Paste your resume here…"
                disabled={loading}
              />
            </>
          ) : (
            <>
              <label className="field-label" htmlFor={fileFieldId}>
                Resume file (PDF or DOCX, max 5 MB)
              </label>
              <input
                id={fileFieldId}
                type="file"
                accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                disabled={loading}
                onChange={(event) => {
                  setFile(event.target.files?.[0] ?? null)
                  setFieldError(null)
                }}
              />
              {file ? (
                <p className="file-meta">
                  Selected: {file.name} ({Math.ceil(file.size / 1024)} KB)
                </p>
              ) : (
                <p className="file-meta">No file selected.</p>
              )}
            </>
          )}
        </section>

        <section>
          <h2 className="section-title">Job description</h2>
          <p className="section-help">
            Include Required / Preferred headings when you can — they improve extraction.
          </p>
          <label className="field-label" htmlFor={jobFieldId}>
            Job description text
          </label>
          <textarea
            id={jobFieldId}
            value={jobText}
            onChange={(event) => setJobText(event.target.value)}
            placeholder="Paste the job description here…"
            disabled={loading}
          />
        </section>

        {(fieldError || submitError) && (
          <div className="alert" role="alert">
            {fieldError ?? submitError}
          </div>
        )}

        <div className="actions">
          <button className="btn btn-primary" type="submit" disabled={loading}>
            {loading ? 'Analyzing…' : 'Extract skills'}
          </button>
          <button
            className="btn btn-secondary"
            type="button"
            onClick={loadSample}
            disabled={loading}
          >
            Use sample pair
          </button>
          <p className={`status-line ${loading ? 'loading' : ''}`} aria-live="polite">
            {loading ? 'Creating resume, job, and analysis…' : ''}
          </p>
        </div>
      </form>
    </div>
  )
}
