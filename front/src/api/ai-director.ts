import http from './http'
import type { AIDirectorDecision, AIDirectorDecisionRequest } from '@/types'

export function requestDirectorDecision(payload: AIDirectorDecisionRequest) {
  return http.post<never, AIDirectorDecision>('/ai-director/decide', payload)
}

export function getDirectorDecision(decisionId: number) {
  return http.get<never, AIDirectorDecision>(`/ai-director/${decisionId}`)
}

export function getLatestDirectorDecision(chapterId: number) {
  return http.get<never, AIDirectorDecision>(`/ai-director/chapter/${chapterId}/latest`)
}
