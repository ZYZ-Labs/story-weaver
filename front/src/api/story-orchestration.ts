import http from './http'
import type {
  ChapterSkeletonSceneUpdateRequest,
  ChapterExecutionReviewView,
  ChapterSkeletonView,
  DirectorCandidateView,
  SessionExecutionTraceItemView,
  StorySessionExecutionView,
  StorySessionPreviewView,
} from '@/types'

function trimText(value: unknown) {
  return typeof value === 'string' ? value.trim() : ''
}

function buildCandidateTitle(candidate: Partial<DirectorCandidateView>) {
  const explicitTitle = trimText((candidate as DirectorCandidateView & { title?: string }).title)
  if (explicitTitle && explicitTitle !== '.') {
    return explicitTitle
  }

  const goal = trimText(candidate.goal)
  if (goal) {
    const normalized = goal
      .replace(/^[。．,.、;；:：·\-\s]+/, '')
      .replace(/[。．,.、;；:：·]+$/g, '')
      .trim()
    if (normalized && normalized !== '.') {
      const punctuationIndex = normalized.search(/[。！？!?；;]/)
      const headline = (punctuationIndex > 0 ? normalized.slice(0, punctuationIndex) : normalized)
        .trim()
        .slice(0, 24)
      if (headline && headline !== '.') {
        return headline
      }
    }
  }

  const type = trimText(candidate.type)
  return type || '未命名候选'
}

function normalizeTraceItem(item: Record<string, unknown>): SessionExecutionTraceItemView {
  const detailsSource = item.details && typeof item.details === 'object'
    ? (item.details as Record<string, unknown>)
    : {}
  const details = Object.fromEntries(
    Object.entries(detailsSource).filter(([, value]) => {
      if (value === null || value === undefined) {
        return false
      }
      if (typeof value === 'string') {
        const normalized = value.trim()
        return normalized.length > 0 && normalized.toLowerCase() !== 'undefined'
      }
      return true
    }),
  )
  const sessionRole = trimText(item.sessionRole) || trimText(item.role) || 'UNKNOWN'
  const message = trimText(item.message) || trimText(item.summary)

  return {
    role: trimText(item.role) || sessionRole,
    sessionRole,
    status: trimText(item.status) || 'UNKNOWN',
    attempt: typeof item.attempt === 'number' ? item.attempt : 1,
    retryable: Boolean(item.retryable),
    summary: trimText(item.summary) || message,
    message,
    details,
  }
}

function normalizeCandidates(candidates: DirectorCandidateView[] | undefined) {
  return (candidates || []).map((candidate) => ({
    ...candidate,
    title: buildCandidateTitle(candidate),
  }))
}

function normalizePreview(preview: StorySessionPreviewView): StorySessionPreviewView {
  return {
    ...preview,
    candidates: normalizeCandidates(preview.candidates),
    trace: {
      ...preview.trace,
      items: (preview.trace?.items || []).map((item) => normalizeTraceItem(item as unknown as Record<string, unknown>)),
    },
  }
}

function normalizeExecution(execution: StorySessionExecutionView): StorySessionExecutionView {
  return {
    ...execution,
    preview: normalizePreview(execution.preview),
    trace: {
      ...execution.trace,
      items: (execution.trace?.items || []).map((item) => normalizeTraceItem(item as unknown as Record<string, unknown>)),
    },
  }
}

export function getStorySessionPreview(projectId: number, chapterId: number, sceneId = 'scene-1') {
  return http.get<never, StorySessionPreviewView>(
    `/story-orchestration/projects/${projectId}/chapters/${chapterId}/preview?sceneId=${encodeURIComponent(sceneId)}`,
  ).then(normalizePreview)
}

export function getChapterSkeletonPreview(projectId: number, chapterId: number) {
  return http.get<never, ChapterSkeletonView>(
    `/story-orchestration/projects/${projectId}/chapters/${chapterId}/skeleton-preview`,
  )
}

export function getChapterExecutionReview(projectId: number, chapterId: number) {
  return http.get<never, ChapterExecutionReviewView>(
    `/story-orchestration/projects/${projectId}/chapters/${chapterId}/chapter-review`,
  )
}

export function executeStorySession(projectId: number, chapterId: number, sceneId = 'scene-1') {
  return http.post<never, StorySessionExecutionView>(
    `/story-orchestration/projects/${projectId}/chapters/${chapterId}/execute?sceneId=${encodeURIComponent(sceneId)}`,
  ).then(normalizeExecution)
}

export function updateChapterSkeletonScene(
  projectId: number,
  chapterId: number,
  sceneId: string,
  payload: ChapterSkeletonSceneUpdateRequest,
) {
  return http.put<never, ChapterSkeletonView>(
    `/story-orchestration/projects/${projectId}/chapters/${chapterId}/skeleton-scenes/${encodeURIComponent(sceneId)}`,
    payload,
  )
}

export function deleteChapterSkeletonScene(projectId: number, chapterId: number, sceneId: string) {
  return http.delete<never, ChapterSkeletonView>(
    `/story-orchestration/projects/${projectId}/chapters/${chapterId}/skeleton-scenes/${encodeURIComponent(sceneId)}`,
  )
}
