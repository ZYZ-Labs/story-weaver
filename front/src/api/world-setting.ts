import http from './http'
import type { WorldSetting } from '@/types'

export function getWorldSettings(projectId: number) {
  return http.get<never, WorldSetting[]>(`/world-settings/project/${projectId}`)
}
