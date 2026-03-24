import http from './http'
import type { NameSuggestionResult } from '@/types'

export function generateNameSuggestions(
  projectId: number,
  payload: {
    entityType: 'chapter' | 'character'
    brief?: string
    extraRequirements?: string
    count?: number
  },
) {
  return http.post<never, NameSuggestionResult>(`/projects/${projectId}/name-suggestions`, payload)
}
