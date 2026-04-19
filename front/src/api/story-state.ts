import http from './http'
import type { ChapterIncrementalStateView, ReaderRevealStateView } from '@/types'

export function getChapterState(projectId: number, chapterId: number) {
  return http.get<never, ChapterIncrementalStateView>(
    `/story-state/projects/${projectId}/chapters/${chapterId}/chapter-state`,
  )
}

export function getReaderRevealState(projectId: number, chapterId: number) {
  return http.get<never, ReaderRevealStateView>(
    `/story-state/projects/${projectId}/chapters/${chapterId}/reader-reveal-state`,
  )
}
