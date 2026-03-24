import http from './http'
import type { WorldSetting } from '@/types'

export function getWorldSettings(projectId: number) {
  return http.get<never, WorldSetting[]>(`/world-settings/project/${projectId}`)
}

export function getWorldSettingLibrary() {
  return http.get<never, WorldSetting[]>('/world-settings/library')
}

export function getWorldSetting(id: number) {
  return http.get<never, WorldSetting>(`/world-settings/${id}`)
}

export function createWorldSetting(payload: Partial<WorldSetting>) {
  return http.post<never, WorldSetting>('/world-settings', payload)
}

export function updateWorldSetting(id: number, payload: Partial<WorldSetting>) {
  return http.put<never, WorldSetting>(`/world-settings/${id}`, payload)
}

export function deleteWorldSetting(id: number) {
  return http.delete(`/world-settings/${id}`)
}

export function attachWorldSettingToProject(id: number, projectId: number) {
  return http.post(`/world-settings/${id}/projects/${projectId}`)
}

export function detachWorldSettingFromProject(id: number, projectId: number) {
  return http.delete(`/world-settings/${id}/projects/${projectId}`)
}
