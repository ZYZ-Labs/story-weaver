import http from './http'
import type { Chapter } from '@/types'

export function getProjectChapters(projectId: number) {
  return http.get<never, Chapter[]>(`/projects/${projectId}/chapters`)
}

export function getChapter(projectId: number, chapterId: number) {
  return http.get<never, Chapter>(`/projects/${projectId}/chapters/${chapterId}`)
}

export function createChapter(projectId: number, payload: Partial<Chapter>) {
  return http.post<never, Chapter>(`/projects/${projectId}/chapters`, payload)
}

export function updateChapter(projectId: number, chapterId: number, payload: Partial<Chapter>) {
  return http.put(`/projects/${projectId}/chapters/${chapterId}`, payload)
}

export function deleteChapter(projectId: number, chapterId: number) {
  return http.delete(`/projects/${projectId}/chapters/${chapterId}`)
}
