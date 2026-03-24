import http from './http'
import type { Project } from '@/types'

export function getProjects() {
  return http.get<never, Project[]>('/projects')
}

export function createProject(payload: Partial<Project>) {
  return http.post<never, Project>('/projects', payload)
}

export function updateProject(id: number, payload: Partial<Project>) {
  return http.put(`/projects/${id}`, payload)
}

export function deleteProject(id: number) {
  return http.delete(`/projects/${id}`)
}
