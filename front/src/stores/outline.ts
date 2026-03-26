import { ref } from 'vue'
import { defineStore } from 'pinia'

import * as outlineApi from '@/api/outline'
import type { Outline } from '@/types'

function sortOutlines(items: Outline[]) {
  return [...items].sort((left, right) => {
    const leftOrder = left.orderNum ?? Number.MAX_SAFE_INTEGER
    const rightOrder = right.orderNum ?? Number.MAX_SAFE_INTEGER
    if (leftOrder !== rightOrder) {
      return leftOrder - rightOrder
    }
    return (left.id ?? 0) - (right.id ?? 0)
  })
}

export const useOutlineStore = defineStore('outline', () => {
  const outlines = ref<Outline[]>([])
  const loading = ref(false)

  async function fetchByProject(projectId: number) {
    loading.value = true
    try {
      outlines.value = sortOutlines(await outlineApi.getOutlines(projectId))
    } catch (error) {
      outlines.value = []
      throw error
    } finally {
      loading.value = false
    }
  }

  async function create(projectId: number, payload: Partial<Outline>) {
    const outline = await outlineApi.createOutline(projectId, payload)
    outlines.value = sortOutlines([...outlines.value, outline])
    return outline
  }

  async function update(projectId: number, outlineId: number, payload: Partial<Outline>) {
    await outlineApi.updateOutline(projectId, outlineId, payload)
    const detail = await outlineApi.getOutline(projectId, outlineId)
    outlines.value = sortOutlines(
      outlines.value.map((item) => (item.id === outlineId ? detail : item)),
    )
    return detail
  }

  async function remove(projectId: number, outlineId: number) {
    await outlineApi.deleteOutline(projectId, outlineId)
    outlines.value = outlines.value.filter((item) => item.id !== outlineId)
  }

  return {
    outlines,
    loading,
    fetchByProject,
    create,
    update,
    remove,
  }
})
