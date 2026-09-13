export type SkillPriority = 'REQUIRED' | 'PREFERRED'

export type AnalysisStatus = 'DRAFT' | 'REVIEWED'

export interface CreatedResource {
  id: string
  createdAt: string
}

export interface CatalogSkill {
  id: string
  name: string
  category: string
}

export interface ExtractedSkill {
  skillId: string
  name: string
  matchedTerm: string
  evidence: string | null
  priority: SkillPriority | null
  priorityAmbiguous: boolean
}

export interface ReviewedSkill {
  skillId: string
  name: string
  priority: SkillPriority | null
  evidence: string | null
}

export interface CoverageBreakdown {
  matched: number
  total: number
  percent: number
}

export interface ResultSkill {
  skillId: string
  name: string
  priority: SkillPriority
  evidence: string | null
}

export interface AnalysisResult {
  scorable: boolean
  score: number | null
  matchedWeight: number
  totalWeight: number
  scoringVersion: string
  catalogVersion: string
  matchedSkills: ResultSkill[]
  missingSkills: ResultSkill[]
  requiredCoverage: CoverageBreakdown
  preferredCoverage: CoverageBreakdown
}

export interface Analysis {
  id: string
  resumeId: string
  jobId: string
  status: AnalysisStatus
  createdAt: string
  reviewedAt: string | null
  resumeSkills: ExtractedSkill[]
  jobSkills: ExtractedSkill[]
  reviewedResumeSkills: ReviewedSkill[]
  reviewedJobSkills: ReviewedSkill[]
  result: AnalysisResult | null
}

export interface ReviewDraftResumeSkill {
  skillId: string
  name: string
  evidence: string
}

export interface ReviewDraftJobSkill {
  skillId: string
  name: string
  priority: SkillPriority
  evidence: string
  priorityAmbiguous?: boolean
}

export interface ReviewAnalysisRequest {
  resumeSkills: Array<{ skillId: string; evidence?: string | null }>
  jobSkills: Array<{ skillId: string; priority: SkillPriority; evidence?: string | null }>
}

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function readErrorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as { message?: string }
    if (body.message) {
      return body.message
    }
  } catch {
    // fall through
  }
  return `Request failed (${response.status})`
}

async function postJson<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    throw new ApiError(response.status, await readErrorMessage(response))
  }
  return (await response.json()) as T
}

async function putJson<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(path, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    throw new ApiError(response.status, await readErrorMessage(response))
  }
  return (await response.json()) as T
}

export async function createResumeFromText(text: string): Promise<CreatedResource> {
  return postJson<CreatedResource>('/api/resumes', { text })
}

export async function createResumeFromFile(file: File): Promise<CreatedResource> {
  const form = new FormData()
  form.append('file', file)
  const response = await fetch('/api/resumes', {
    method: 'POST',
    body: form,
  })
  if (!response.ok) {
    throw new ApiError(response.status, await readErrorMessage(response))
  }
  return (await response.json()) as CreatedResource
}

export async function createJob(text: string): Promise<CreatedResource> {
  return postJson<CreatedResource>('/api/jobs', { text })
}

export async function createAnalysis(resumeId: string, jobId: string): Promise<Analysis> {
  return postJson<Analysis>('/api/analyses', { resumeId, jobId })
}

export async function getAnalysis(id: string): Promise<Analysis> {
  const response = await fetch(`/api/analyses/${id}`, { cache: 'no-store' })
  if (!response.ok) {
    throw new ApiError(response.status, await readErrorMessage(response))
  }
  return (await response.json()) as Analysis
}

export async function reviewAnalysis(id: string, body: ReviewAnalysisRequest): Promise<Analysis> {
  return putJson<Analysis>(`/api/analyses/${id}/review`, body)
}

export async function deleteAnalysis(id: string): Promise<void> {
  const response = await fetch(`/api/analyses/${id}`, {
    method: 'DELETE',
    cache: 'no-store',
  })
  if (!response.ok) {
    throw new ApiError(response.status, await readErrorMessage(response))
  }
}

export async function listSkills(): Promise<CatalogSkill[]> {
  const response = await fetch('/api/skills', { cache: 'no-store' })
  if (!response.ok) {
    throw new ApiError(response.status, await readErrorMessage(response))
  }
  return (await response.json()) as CatalogSkill[]
}

export function buildReviewDraft(analysis: Analysis): {
  resumeSkills: ReviewDraftResumeSkill[]
  jobSkills: ReviewDraftJobSkill[]
} {
  const reviewedResume = analysis.reviewedResumeSkills ?? []
  const reviewedJob = analysis.reviewedJobSkills ?? []

  if (analysis.status === 'REVIEWED' && reviewedResume.length + reviewedJob.length > 0) {
    return {
      resumeSkills: reviewedResume.map((skill) => ({
        skillId: skill.skillId,
        name: skill.name,
        evidence: skill.evidence ?? '',
      })),
      jobSkills: reviewedJob.map((skill) => ({
        skillId: skill.skillId,
        name: skill.name,
        priority: skill.priority ?? 'PREFERRED',
        evidence: skill.evidence ?? '',
      })),
    }
  }

  return {
    resumeSkills: (analysis.resumeSkills ?? []).map((skill) => ({
      skillId: skill.skillId,
      name: skill.name,
      evidence: skill.evidence ?? '',
    })),
    jobSkills: (analysis.jobSkills ?? []).map((skill) => ({
      skillId: skill.skillId,
      name: skill.name,
      priority: skill.priority ?? 'PREFERRED',
      evidence: skill.evidence ?? '',
      priorityAmbiguous: skill.priorityAmbiguous || skill.priority == null,
    })),
  }
}
