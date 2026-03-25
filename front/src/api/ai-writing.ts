import http from './http'
import { appEnv } from '@/utils/env'
import { readStorage, storageKeys } from '@/utils/storage'
import type { AIWritingRecord, AIWritingRequest, AIWritingStreamEvent } from '@/types'

export function generateWriting(payload: AIWritingRequest) {
  return http.post<never, AIWritingRecord>('/ai-writing/generate', payload)
}

type StreamHandlers = {
  onEvent?: (event: AIWritingStreamEvent) => void
}

export async function streamGenerateWriting(payload: AIWritingRequest, handlers: StreamHandlers = {}) {
  const token = readStorage<string | null>(storageKeys.token, null)
  const response = await fetch(`${appEnv.apiBaseUrl}/ai-writing/generate-stream`, {
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
    throw new Error('No stream body was returned by the server.')
  }

  const decoder = new TextDecoder()
  let buffer = ''
  let completedRecord: AIWritingRecord | null = null

  while (true) {
    const { done, value } = await reader.read()
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done }).replace(/\r\n/g, '\n')

    let separatorIndex = buffer.indexOf('\n\n')
    while (separatorIndex >= 0) {
      const rawEvent = buffer.slice(0, separatorIndex)
      buffer = buffer.slice(separatorIndex + 2)
      const event = parseSseEvent(rawEvent)
      if (event) {
        handlers.onEvent?.(event)
        if (event.type === 'error') {
          throw new Error(event.message || 'AI generation failed.')
        }
        if (event.type === 'complete' && event.record) {
          completedRecord = event.record
          await reader.cancel().catch(() => undefined)
          return completedRecord
        }
      }
      separatorIndex = buffer.indexOf('\n\n')
    }

    if (done) {
      break
    }
  }

  const tailEvent = parseSseEvent(buffer.trim())
  if (tailEvent) {
    handlers.onEvent?.(tailEvent)
    if (tailEvent.type === 'error') {
      throw new Error(tailEvent.message || 'AI generation failed.')
    }
    if (tailEvent.type === 'complete' && tailEvent.record) {
      completedRecord = tailEvent.record
      return completedRecord
    }
  }

  if (!completedRecord) {
    throw new Error('The stream ended before a final record was returned.')
  }

  return completedRecord
}

export function getWritingRecords(chapterId: number) {
  return http.get<never, AIWritingRecord[]>(`/ai-writing/chapter/${chapterId}`)
}

export function getProjectWritingRecords(projectId: number) {
  return http.get<never, AIWritingRecord[]>(`/ai-writing/project/${projectId}`)
}

export function acceptWriting(id: number) {
  return http.post<never, AIWritingRecord>(`/ai-writing/${id}/accept`)
}

export function rejectWriting(id: number) {
  return http.post(`/ai-writing/${id}/reject`)
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
    } as AIWritingStreamEvent
  } catch {
    return {
      type: eventName,
      message: payloadText,
    } satisfies AIWritingStreamEvent
  }
}

async function readErrorMessage(response: Response) {
  const text = await response.text()
  if (!text) {
    return 'AI generation failed.'
  }

  try {
    const payload = JSON.parse(text) as { message?: string }
    return payload.message || text
  } catch {
    return text
  }
}
