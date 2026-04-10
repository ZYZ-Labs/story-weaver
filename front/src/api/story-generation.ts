import http from './http'
import type {
  ChapterAnchorBundle,
  GenerationReadiness,
  StructuredCreationApplyRequest,
  StructuredCreationApplyResult,
} from '@/types'

export function getChapterGenerationReadiness(projectId: number, chapterId: number) {
  return http.get<never, GenerationReadiness>(`/projects/${projectId}/chapters/${chapterId}/generation-readiness`)
}

export function getChapterAnchors(projectId: number, chapterId: number) {
  return http.get<never, ChapterAnchorBundle>(`/projects/${projectId}/chapters/${chapterId}/anchors`)
}

export function applyStructuredCreation(projectId: number, payload: StructuredCreationApplyRequest) {
  return http.post<never, StructuredCreationApplyResult>(`/projects/${projectId}/structured-creations/apply`, payload)
}
