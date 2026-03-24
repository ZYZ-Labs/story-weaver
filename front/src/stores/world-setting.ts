import { ref } from 'vue'
import { defineStore } from 'pinia'

import * as worldSettingApi from '@/api/world-setting'
import type { WorldSetting } from '@/types'

export const useWorldSettingStore = defineStore('world-setting', () => {
  const items = ref<WorldSetting[]>([])
  const libraryItems = ref<WorldSetting[]>([])
  const loading = ref(false)

  function mergeProjectItem(nextItem: WorldSetting) {
    const target = items.value.find((item) => item.id === nextItem.id)
    if (target) {
      Object.assign(target, nextItem)
    }
  }

  function mergeLibraryItem(nextItem: WorldSetting) {
    const target = libraryItems.value.find((item) => item.id === nextItem.id)
    if (target) {
      Object.assign(target, nextItem)
      return
    }
    libraryItems.value.unshift(nextItem)
  }

  async function fetchByProject(projectId: number) {
    loading.value = true
    try {
      items.value = await worldSettingApi.getWorldSettings(projectId)
    } catch (error) {
      items.value = []
      throw error
    } finally {
      loading.value = false
    }
  }

  async function fetchLibrary() {
    loading.value = true
    try {
      libraryItems.value = await worldSettingApi.getWorldSettingLibrary()
    } catch (error) {
      libraryItems.value = []
      throw error
    } finally {
      loading.value = false
    }
  }

  async function create(payload: Partial<WorldSetting>) {
    const created = await worldSettingApi.createWorldSetting(payload)
    items.value.unshift(created)
    mergeLibraryItem(created)
    return created
  }

  async function update(id: number, payload: Partial<WorldSetting>) {
    const updated = await worldSettingApi.updateWorldSetting(id, payload)
    mergeProjectItem(updated)
    mergeLibraryItem(updated)
    return updated
  }

  async function remove(id: number) {
    await worldSettingApi.deleteWorldSetting(id)
    items.value = items.value.filter((item) => item.id !== id)
    libraryItems.value = libraryItems.value.filter((item) => item.id !== id)
  }

  async function attach(projectId: number, id: number) {
    await worldSettingApi.attachWorldSettingToProject(id, projectId)
  }

  async function detach(projectId: number, id: number) {
    await worldSettingApi.detachWorldSettingFromProject(id, projectId)
    items.value = items.value.filter((item) => item.id !== id)
  }

  return {
    items,
    libraryItems,
    loading,
    fetchByProject,
    fetchLibrary,
    create,
    update,
    remove,
    attach,
    detach,
  }
})
