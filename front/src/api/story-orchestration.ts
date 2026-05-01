import http from './http'
import { appEnv } from '@/utils/env'
import { readStorage, storageKeys } from '@/utils/storage'
import type {
  ChapterNodeRuntimeView,
  ChapterSkeletonStreamEvent,
  ChapterSkeletonSceneUpdateRequest,
  ChapterExecutionReviewView,
  ChapterSkeletonView,
  DirectorCandidateView,
  NodeResolutionResultView,
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

type SkeletonStreamHandlers = {
  onEvent?: (event: ChapterSkeletonStreamEvent) => void
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

export function getChapterNodePreview(projectId: number, chapterId: number) {
  return http.get<never, ChapterNodeRuntimeView>(
    `/story-orchestration/projects/${projectId}/chapters/${chapterId}/node-preview`,
  )
}

export function generateChapterSkeleton(projectId: number, chapterId: number, forceRefresh = false) {
  return http.post<never, ChapterSkeletonView>(
    `/story-orchestration/projects/${projectId}/chapters/${chapterId}/skeleton-generate?forceRefresh=${forceRefresh}`,
  )
}

export async function streamGenerateChapterSkeleton(
  projectId: number,
  chapterId: number,
  forceRefresh = false,
  handlers: SkeletonStreamHandlers = {},
) {
  const token = readStorage<string | null>(storageKeys.token, null)
  const response = await fetch(
    `${appEnv.apiBaseUrl}/story-orchestration/projects/${projectId}/chapters/${chapterId}/skeleton-generate-stream?forceRefresh=${forceRefresh}`,
    {
      method: 'POST',
      headers: {
        Accept: 'text/event-stream',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      cache: 'no-store',
    },
  )

  if (!response.ok) {
    throw new Error(await readErrorMessage(response))
  }

  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('No stream body was returned by the server.')
  }

  const decoder = new TextDecoder()
  let buffer = ''
  let completedSkeleton: ChapterSkeletonView | null = null
  let lastEvent: ChapterSkeletonStreamEvent | null = null

  while (true) {
    let done = false
    let value: Uint8Array | undefined
    try {
      const readResult = await reader.read()
      done = readResult.done
      value = readResult.value
    } catch (error) {
      const recoveredTailEvent = parseSkeletonSseEvent(buffer.trim())
      if (recoveredTailEvent) {
        lastEvent = recoveredTailEvent
        handlers.onEvent?.(recoveredTailEvent)
        if (recoveredTailEvent.type === 'error') {
          throw new Error(recoveredTailEvent.message || '镜头骨架生成失败。')
        }
        if (recoveredTailEvent.type === 'complete' && recoveredTailEvent.skeleton) {
          return recoveredTailEvent.skeleton
        }
      }
      if (completedSkeleton) {
        return completedSkeleton
      }
      throw resolveSkeletonStreamReadError(error, lastEvent, '镜头骨架生成连接中断，请稍后重试')
    }

    buffer += decoder.decode(value || new Uint8Array(), { stream: !done }).replace(/\r\n/g, '\n')

    let separatorIndex = buffer.indexOf('\n\n')
    while (separatorIndex >= 0) {
      const rawEvent = buffer.slice(0, separatorIndex)
      buffer = buffer.slice(separatorIndex + 2)
      const event = parseSkeletonSseEvent(rawEvent)
      if (event) {
        lastEvent = event
        handlers.onEvent?.(event)
        if (event.type === 'error') {
          throw new Error(event.message || '镜头骨架生成失败。')
        }
        if (event.type === 'complete' && event.skeleton) {
          completedSkeleton = event.skeleton
          return completedSkeleton
        }
      }
      separatorIndex = buffer.indexOf('\n\n')
    }

    if (done) {
      if (completedSkeleton) {
        return completedSkeleton
      }
      break
    }
  }

  const tailEvent = parseSkeletonSseEvent(buffer.trim())
  if (tailEvent) {
    lastEvent = tailEvent
    handlers.onEvent?.(tailEvent)
    if (tailEvent.type === 'error') {
      throw new Error(tailEvent.message || '镜头骨架生成失败。')
    }
    if (tailEvent.type === 'complete' && tailEvent.skeleton) {
      completedSkeleton = tailEvent.skeleton
      return completedSkeleton
    }
  }

  if (!completedSkeleton) {
    if (lastEvent?.type === 'error' && lastEvent.message) {
      throw new Error(lastEvent.message)
    }
    throw new Error('镜头骨架生成流提前结束，未返回完整结果。')
  }

  return completedSkeleton
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

export function resolveChapterNodeAction(
  projectId: number,
  chapterId: number,
  payload: {
    nodeId?: string
    checkpointId?: string
    selectedOptionId?: string
    customAction?: string
  },
) {
  return http.post<typeof payload, NodeResolutionResultView>(
    `/story-orchestration/projects/${projectId}/chapters/${chapterId}/node-actions/resolve`,
    payload,
  )
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

function parseSkeletonSseEvent(rawEvent: string) {
  const lines = rawEvent.split('\n')
  let eventName = 'message'
  const dataLines: string[] = []

  for (const line of lines) {
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim()
      continue
    }
    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart())
    }
  }

  if (!dataLines.length) {
    return null
  }

  const payloadText = dataLines.join('\n')
  try {
    const payload = JSON.parse(payloadText) as Record<string, unknown>
    return {
      ...(payload as Record<string, unknown>),
      type: eventName,
    } as ChapterSkeletonStreamEvent
  } catch {
    return {
      type: eventName,
      message: payloadText,
    } satisfies ChapterSkeletonStreamEvent
  }
}

function resolveSkeletonStreamReadError(
  error: unknown,
  lastEvent: ChapterSkeletonStreamEvent | null,
  fallback: string,
) {
  if (lastEvent?.type === 'error' && lastEvent.message) {
    return new Error(lastEvent.message)
  }
  if (error instanceof Error) {
    const normalized = error.message.trim().toLowerCase()
    if (
      normalized.includes('networkerror')
      || normalized.includes('network error')
      || normalized.includes('failed to fetch')
      || normalized.includes('load failed')
      || normalized.includes('terminated')
    ) {
      return new Error(fallback)
    }
    return error
  }
  return new Error(fallback)
}

async function readErrorMessage(response: Response) {
  const text = await response.text()
  if (!text) {
    return '镜头骨架生成失败。'
  }

  try {
    const payload = JSON.parse(text) as { message?: string }
    return payload.message || text
  } catch {
    return text
  }
}
