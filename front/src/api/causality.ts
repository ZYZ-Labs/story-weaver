import http from './http'
import type { Causality } from '@/types'

export function getCausalities(projectId: number) {
  return http.get<never, Causality[]>(`/projects/${projectId}/causalities`)
}

export function getCausality(id: number) {
  return http.get<never, Causality>(`/causalities/${id}`)
}

export function createCausality(projectId: number, payload: Partial<Causality>) {
  return http.post<never, Causality>(`/projects/${projectId}/causalities`, payload)
}

export function updateCausality(id: number, payload: Partial<Causality>) {
  return http.put(`/causalities/${id}`, payload)
}

export function deleteCausality(id: number) {
  return http.delete(`/causalities/${id}`)
}
