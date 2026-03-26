import http from './http'
import type { Outline } from '@/types'

export function getOutlines(projectId: number) {
  return http.get<never, Outline[]>(`/projects/${projectId}/outlines`)
}

export function getOutline(projectId: number, outlineId: number) {
  return http.get<never, Outline>(`/projects/${projectId}/outlines/${outlineId}`)
}

export function createOutline(projectId: number, payload: Partial<Outline>) {
  return http.post<never, Outline>(`/projects/${projectId}/outlines`, payload)
}

export function updateOutline(projectId: number, outlineId: number, payload: Partial<Outline>) {
  return http.put(`/projects/${projectId}/outlines/${outlineId}`, payload)
}

export function deleteOutline(projectId: number, outlineId: number) {
  return http.delete(`/projects/${projectId}/outlines/${outlineId}`)
}
