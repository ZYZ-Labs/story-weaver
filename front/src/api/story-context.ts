import http from './http'
import type {
  CharacterRuntimeStateView,
  ProjectBriefView,
  ReaderKnownStateView,
  RecentStoryProgressView,
} from '@/types'

export function getProjectBrief(projectId: number) {
  return http.get<never, ProjectBriefView>(`/story-context/projects/${projectId}/brief`)
}

export function getReaderKnownState(projectId: number, chapterId: number) {
  return http.get<never, ReaderKnownStateView>(
    `/story-context/projects/${projectId}/chapters/${chapterId}/reader-known-state`,
  )
}

export function getCharacterRuntimeState(projectId: number, characterId: number) {
  return http.get<never, CharacterRuntimeStateView>(
    `/story-context/projects/${projectId}/characters/${characterId}/runtime-state`,
  )
}

export function getRecentStoryProgress(projectId: number, limit = 6) {
  return http.get<never, RecentStoryProgressView>(
    `/story-context/projects/${projectId}/progress?limit=${limit}`,
  )
}
