import http from './http'
import type { AIWritingRecord, AIWritingRequest } from '@/types'

export function generateWriting(payload: AIWritingRequest) {
  return http.post<never, AIWritingRecord>('/ai-writing/generate', payload)
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
