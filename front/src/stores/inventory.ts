import { ref } from 'vue'
import { defineStore } from 'pinia'

import * as inventoryApi from '@/api/inventory'
import type { CharacterInventoryItem, ItemGenerationRequest } from '@/types'

export const useInventoryStore = defineStore('inventory', () => {
  const inventoryByCharacter = ref<Record<number, CharacterInventoryItem[]>>({})
  const loadingByCharacter = ref<Record<number, boolean>>({})

  function getInventory(characterId: number) {
    return inventoryByCharacter.value[characterId] || []
  }

  function isLoading(characterId: number) {
    return Boolean(loadingByCharacter.value[characterId])
  }

  async function fetchByCharacter(projectId: number, characterId: number) {
    loadingByCharacter.value = { ...loadingByCharacter.value, [characterId]: true }
    try {
      const inventory = await inventoryApi.getCharacterInventory(projectId, characterId)
      inventoryByCharacter.value = { ...inventoryByCharacter.value, [characterId]: inventory }
      return inventory
    } finally {
      loadingByCharacter.value = { ...loadingByCharacter.value, [characterId]: false }
    }
  }

  async function add(projectId: number, characterId: number, payload: Parameters<typeof inventoryApi.addInventoryItem>[2]) {
    const item = await inventoryApi.addInventoryItem(projectId, characterId, payload)
    await fetchByCharacter(projectId, characterId)
    return item
  }

  async function update(
    projectId: number,
    characterId: number,
    inventoryItemId: number,
    payload: Parameters<typeof inventoryApi.updateInventoryItem>[3],
  ) {
    const item = await inventoryApi.updateInventoryItem(projectId, characterId, inventoryItemId, payload)
    await fetchByCharacter(projectId, characterId)
    return item
  }

  async function remove(projectId: number, characterId: number, inventoryItemId: number) {
    await inventoryApi.deleteInventoryItem(projectId, characterId, inventoryItemId)
    await fetchByCharacter(projectId, characterId)
  }

  async function generate(projectId: number, characterId: number, payload: ItemGenerationRequest) {
    const inventory = await inventoryApi.generateInventoryItems(projectId, characterId, payload)
    await fetchByCharacter(projectId, characterId)
    return inventory
  }

  function clear(projectCharacterIds?: number[]) {
    if (!projectCharacterIds?.length) {
      inventoryByCharacter.value = {}
      loadingByCharacter.value = {}
      return
    }

    const nextInventory: Record<number, CharacterInventoryItem[]> = {}
    const nextLoading: Record<number, boolean> = {}
    for (const characterId of projectCharacterIds) {
      if (inventoryByCharacter.value[characterId]) {
        nextInventory[characterId] = inventoryByCharacter.value[characterId]
      }
      if (loadingByCharacter.value[characterId]) {
        nextLoading[characterId] = loadingByCharacter.value[characterId]
      }
    }
    inventoryByCharacter.value = nextInventory
    loadingByCharacter.value = nextLoading
  }

  return {
    inventoryByCharacter,
    loadingByCharacter,
    getInventory,
    isLoading,
    fetchByCharacter,
    add,
    update,
    remove,
    generate,
    clear,
  }
})
