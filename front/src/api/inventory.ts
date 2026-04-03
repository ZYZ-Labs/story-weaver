import http from './http'
import type { CharacterInventoryItem, ItemGenerationRequest } from '@/types'

export function getCharacterInventory(projectId: number, characterId: number) {
  return http.get<never, CharacterInventoryItem[]>(`/projects/${projectId}/characters/${characterId}/inventory`)
}

export function addInventoryItem(
  projectId: number,
  characterId: number,
  payload: {
    itemId: number
    quantity?: number
    equipped?: boolean
    durability?: number
    customName?: string
    notes?: string
    sortOrder?: number
  },
) {
  return http.post<never, CharacterInventoryItem>(`/projects/${projectId}/characters/${characterId}/inventory`, payload)
}

export function updateInventoryItem(
  projectId: number,
  characterId: number,
  inventoryItemId: number,
  payload: {
    quantity?: number
    equipped?: boolean
    durability?: number
    customName?: string
    notes?: string
    sortOrder?: number
  },
) {
  return http.put<never, CharacterInventoryItem>(
    `/projects/${projectId}/characters/${characterId}/inventory/${inventoryItemId}`,
    payload,
  )
}

export function deleteInventoryItem(projectId: number, characterId: number, inventoryItemId: number) {
  return http.delete(`/projects/${projectId}/characters/${characterId}/inventory/${inventoryItemId}`)
}

export function generateInventoryItems(projectId: number, characterId: number, payload: ItemGenerationRequest) {
  return http.post<never, CharacterInventoryItem[]>(
    `/projects/${projectId}/characters/${characterId}/inventory/generate-items`,
    payload,
  )
}
