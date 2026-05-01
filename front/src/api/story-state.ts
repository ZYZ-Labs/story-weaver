import http from './http'
import type {
  ChapterIncrementalStateView,
  LegacyChapterBackfillAnalysisView,
  LegacyBackfillDryRunView,
  LegacyBackfillExecutionResultView,
  LegacyProjectBackfillDryRunView,
  LegacyProjectBackfillOverviewView,
  MigrationCompatibilitySnapshotView,
  ReaderRevealStateView,
  StoryConsistencyCheckView,
} from '@/types'

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

export function getChapterBackfillAnalysis(projectId: number, chapterId: number) {
  return http.get<never, LegacyChapterBackfillAnalysisView>(
    `/story-state/projects/${projectId}/chapters/${chapterId}/backfill-analysis`,
  )
}

export function getChapterBackfillDryRun(projectId: number, chapterId: number) {
  return http.get<never, LegacyBackfillDryRunView>(
    `/story-state/projects/${projectId}/chapters/${chapterId}/backfill-dry-run`,
  )
}

export function executeChapterBackfill(projectId: number, chapterId: number) {
  return http.post<never, LegacyBackfillExecutionResultView>(
    `/story-state/projects/${projectId}/chapters/${chapterId}/backfill-execute`,
  )
}

export function getChapterCompatibilitySnapshot(projectId: number, chapterId: number) {
  return http.get<never, MigrationCompatibilitySnapshotView>(
    `/story-state/projects/${projectId}/chapters/${chapterId}/compatibility-snapshot`,
  )
}

export function getProjectBackfillOverview(projectId: number) {
  return http.get<never, LegacyProjectBackfillOverviewView>(
    `/story-state/projects/${projectId}/backfill-overview`,
  )
}

export function getProjectBackfillDryRun(projectId: number) {
  return http.get<never, LegacyProjectBackfillDryRunView>(
    `/story-state/projects/${projectId}/backfill-project-dry-run`,
  )
}

export function getChapterConsistencyCheck(projectId: number, chapterId: number) {
  return http.get<never, StoryConsistencyCheckView>(
    `/story-state/projects/${projectId}/chapters/${chapterId}/consistency-check`,
  )
}
