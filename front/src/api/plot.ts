import http from './http'
import type { Plot } from '@/types'

export function getPlots(projectId: number) {
  return http.get<never, Plot[]>(`/projects/${projectId}/plotlines`)
}

export function getPlot(id: number) {
  return http.get<never, Plot>(`/plotlines/${id}`)
}

export function createPlot(projectId: number, payload: Partial<Plot>) {
  return http.post<never, Plot>(`/projects/${projectId}/plotlines`, payload)
}

export function updatePlot(id: number, payload: Partial<Plot>) {
  return http.put(`/plotlines/${id}`, payload)
}

export function deletePlot(id: number) {
  return http.delete(`/plotlines/${id}`)
}
