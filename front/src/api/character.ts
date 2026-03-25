import http from './http'
import type { Character, CharacterAttributeSuggestionResult } from '@/types'

export function getCharacterLibrary() {
  return http.get<never, Character[]>('/characters/library')
}

export function getProjectCharacters(projectId: number) {
  return http.get<never, Character[]>(`/projects/${projectId}/characters`)
}

export function createCharacter(
  projectId: number,
  payload: Partial<Character> & { existingCharacterId?: number; projectRole?: string },
) {
  return http.post<never, Character>(`/projects/${projectId}/characters`, payload)
}

export function updateCharacter(
  projectId: number,
  characterId: number,
  payload: Partial<Character> & { projectRole?: string },
) {
  return http.put(`/projects/${projectId}/characters/${characterId}`, payload)
}

export function deleteCharacter(projectId: number, characterId: number) {
  return http.delete(`/projects/${projectId}/characters/${characterId}`)
}

export function generateCharacterAttributes(
  projectId: number,
  payload: {
    name?: string
    description?: string
    extraRequirements?: string
  },
) {
  return http.post<never, CharacterAttributeSuggestionResult>(
    `/projects/${projectId}/characters/attribute-suggestions`,
    payload,
  )
}
