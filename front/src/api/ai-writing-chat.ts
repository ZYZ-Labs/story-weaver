import http from './http'
import type { AIWritingChatMessageRequest, AIWritingChatSession } from '@/types'

export function getWritingChatSession(chapterId: number) {
  return http.get<never, AIWritingChatSession>(`/ai-writing/chat/${chapterId}`)
}

export function sendWritingChatMessage(chapterId: number, payload: AIWritingChatMessageRequest) {
  return http.post<never, AIWritingChatSession>(`/ai-writing/chat/${chapterId}/messages`, payload)
}

export function setWritingChatMessageBackground(messageId: number, pinned: boolean) {
  return http.post<never, AIWritingChatSession>(`/ai-writing/chat/messages/${messageId}/background`, { pinned })
}
