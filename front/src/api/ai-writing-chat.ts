import http from './http'
import { appEnv } from '@/utils/env'
import { readStorage, storageKeys } from '@/utils/storage'
import type {
  AIWritingChatMessageRequest,
  AIWritingChatSession,
  AIWritingChatStreamEvent,
} from '@/types'

export function getWritingChatSession(chapterId: number) {
  return http.get<never, AIWritingChatSession>(`/ai-writing/chat/${chapterId}`)
}

export function sendWritingChatMessage(chapterId: number, payload: AIWritingChatMessageRequest) {
  return http.post<never, AIWritingChatSession>(`/ai-writing/chat/${chapterId}/messages`, payload)
}

type ChatStreamHandlers = {
  onEvent?: (event: AIWritingChatStreamEvent) => void
}

export async function streamWritingChatMessage(
  chapterId: number,
  payload: AIWritingChatMessageRequest,
  handlers: ChatStreamHandlers = {},
) {
  const token = readStorage<string | null>(storageKeys.token, null)
  const response = await fetch(`${appEnv.apiBaseUrl}/ai-writing/chat/${chapterId}/messages/stream`, {
    method: 'POST',
    headers: {
      Accept: 'text/event-stream',
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    cache: 'no-store',
    body: JSON.stringify(payload),
  })

  if (!response.ok) {
    throw new Error(await readErrorMessage(response))
  }

  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('服务端没有返回可用的聊天流。')
  }

  const decoder = new TextDecoder()
  let buffer = ''
  let completedSession: AIWritingChatSession | null = null
  let lastEvent: AIWritingChatStreamEvent | null = null

  while (true) {
    let done = false
    let value: Uint8Array | undefined
    try {
      const readResult = await reader.read()
      done = readResult.done
      value = readResult.value
    } catch (error) {
      const recoveredTailEvent = parseSseEvent(buffer.trim())
      if (recoveredTailEvent) {
        lastEvent = recoveredTailEvent
        handlers.onEvent?.(recoveredTailEvent)
        if (recoveredTailEvent.type === 'error') {
          throw new Error(recoveredTailEvent.message || '背景聊天生成失败')
        }
        if (recoveredTailEvent.type === 'complete' && recoveredTailEvent.session) {
          return recoveredTailEvent.session
        }
      }
      if (completedSession) {
        return completedSession
      }
      throw resolveStreamReadError(error, lastEvent, '背景聊天连接中断，请稍后重试')
    }
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done }).replace(/\r\n/g, '\n')

    let separatorIndex = buffer.indexOf('\n\n')
    while (separatorIndex >= 0) {
      const rawEvent = buffer.slice(0, separatorIndex)
      buffer = buffer.slice(separatorIndex + 2)
      const event = parseSseEvent(rawEvent)
      if (event) {
        lastEvent = event
        handlers.onEvent?.(event)
        if (event.type === 'error') {
          throw new Error(event.message || '背景聊天生成失败')
        }
        if (event.type === 'complete' && event.session) {
          completedSession = event.session
        }
      }
      separatorIndex = buffer.indexOf('\n\n')
    }

    if (done) {
      if (completedSession) {
        return completedSession
      }
      break
    }
  }

  const tailEvent = parseSseEvent(buffer.trim())
  if (tailEvent) {
    lastEvent = tailEvent
    handlers.onEvent?.(tailEvent)
    if (tailEvent.type === 'error') {
      throw new Error(tailEvent.message || '背景聊天生成失败')
    }
    if (tailEvent.type === 'complete' && tailEvent.session) {
      completedSession = tailEvent.session
      return completedSession
    }
  }

  if (!completedSession) {
    if (lastEvent?.type === 'error' && lastEvent.message) {
      throw new Error(lastEvent.message)
    }
    throw new Error('聊天流提前结束，未返回完整会话。')
  }

  return completedSession
}

export function setWritingChatMessageBackground(messageId: number, pinned: boolean) {
  return http.post<never, AIWritingChatSession>(`/ai-writing/chat/messages/${messageId}/background`, { pinned })
}

export function addWritingChatBackgroundNote(chapterId: number, content: string) {
  return http.post<never, AIWritingChatSession>(`/ai-writing/chat/${chapterId}/background-notes`, { content })
}

export function updateWritingChatBackgroundNote(messageId: number, content: string) {
  return http.post<never, AIWritingChatSession>(`/ai-writing/chat/messages/${messageId}/content`, { content })
}

function parseSseEvent(rawEvent: string) {
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
    } as AIWritingChatStreamEvent
  } catch {
    return {
      type: eventName,
      message: payloadText,
    } satisfies AIWritingChatStreamEvent
  }
}

function resolveStreamReadError(error: unknown, lastEvent: AIWritingChatStreamEvent | null, fallback: string) {
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
    return '背景聊天请求失败'
  }

  try {
    const payload = JSON.parse(text) as { message?: string }
    return payload.message || text
  } catch {
    return text
  }
}
