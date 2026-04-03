import { ref } from 'vue'
import { defineStore } from 'pinia'

import * as itemApi from '@/api/item'
import type { Item, ItemGenerationRequest } from '@/types'

export const useItemStore = defineStore('item', () => {
  const items = ref<Item[]>([])
  const loading = ref(false)

  async function fetchByProject(projectId: number) {
    loading.value = true
    try {
      items.value = await itemApi.getProjectItems(projectId)
    } catch (error) {
      items.value = []
      throw error
    } finally {
      loading.value = false
    }
  }

  async function create(projectId: number, payload: Partial<Item>) {
    const item = await itemApi.createProjectItem(projectId, payload)
    await fetchByProject(projectId)
    return item
  }

  async function update(projectId: number, itemId: number, payload: Partial<Item>) {
    const item = await itemApi.updateProjectItem(projectId, itemId, payload)
    await fetchByProject(projectId)
    return item
  }

  async function remove(projectId: number, itemId: number) {
    await itemApi.deleteProjectItem(projectId, itemId)
    items.value = items.value.filter((item) => item.id !== itemId)
  }

  async function generate(projectId: number, payload: ItemGenerationRequest) {
    const createdItems = await itemApi.generateProjectItems(projectId, payload)
    await fetchByProject(projectId)
    return createdItems
  }

  function clear() {
    items.value = []
  }

  return {
    items,
    loading,
    fetchByProject,
    create,
    update,
    remove,
    generate,
    clear,
  }
})
