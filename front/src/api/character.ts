import http from './http'
import type { Character } from '@/types'

export function getProjectCharacters(projectId: number) {
  return http.get<never, Character[]>(`/projects/${projectId}/characters`)
}

export function createCharacter(projectId: number, payload: Partial<Character>) {
  return http.post<never, Character>(`/projects/${projectId}/characters`, payload)
}

export function updateCharacter(projectId: number, characterId: number, payload: Partial<Character>) {
  return http.put(`/projects/${projectId}/characters/${characterId}`, payload)
}

export function deleteCharacter(projectId: number, characterId: number) {
  return http.delete(`/projects/${projectId}/characters/${characterId}`)
}
