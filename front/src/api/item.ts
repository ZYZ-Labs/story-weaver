import http from './http'
import type { Item, ItemGenerationRequest } from '@/types'

export function getProjectItems(projectId: number) {
  return http.get<never, Item[]>(`/projects/${projectId}/items`)
}

export function createProjectItem(projectId: number, payload: Partial<Item>) {
  return http.post<never, Item>(`/projects/${projectId}/items`, payload)
}

export function updateProjectItem(projectId: number, itemId: number, payload: Partial<Item>) {
  return http.put<never, Item>(`/projects/${projectId}/items/${itemId}`, payload)
}

export function deleteProjectItem(projectId: number, itemId: number) {
  return http.delete(`/projects/${projectId}/items/${itemId}`)
}

export function generateProjectItems(projectId: number, payload: ItemGenerationRequest) {
  return http.post<never, Item[]>(`/projects/${projectId}/items/generate`, payload)
}
