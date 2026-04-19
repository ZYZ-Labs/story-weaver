import http from './http'
import type {
  ChapterExecutionReviewView,
  ChapterSkeletonView,
  StorySessionExecutionView,
  StorySessionPreviewView,
} from '@/types'

export function getStorySessionPreview(projectId: number, chapterId: number, sceneId = 'scene-1') {
  return http.get<never, StorySessionPreviewView>(
    `/story-orchestration/projects/${projectId}/chapters/${chapterId}/preview?sceneId=${encodeURIComponent(sceneId)}`,
  )
}

export function getChapterSkeletonPreview(projectId: number, chapterId: number) {
  return http.get<never, ChapterSkeletonView>(
    `/story-orchestration/projects/${projectId}/chapters/${chapterId}/skeleton-preview`,
  )
}

export function getChapterExecutionReview(projectId: number, chapterId: number) {
  return http.get<never, ChapterExecutionReviewView>(
    `/story-orchestration/projects/${projectId}/chapters/${chapterId}/chapter-review`,
  )
}

export function executeStorySession(projectId: number, chapterId: number, sceneId = 'scene-1') {
  return http.post<never, StorySessionExecutionView>(
    `/story-orchestration/projects/${projectId}/chapters/${chapterId}/execute?sceneId=${encodeURIComponent(sceneId)}`,
  )
}
