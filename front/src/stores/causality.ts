import { ref } from 'vue'
import { defineStore } from 'pinia'

import * as causalityApi from '@/api/causality'
import type { Causality } from '@/types'

export const useCausalityStore = defineStore('causality', () => {
  const nodes = ref<Causality[]>([])
  const loading = ref(false)

  async function fetchByProject(projectId: number) {
    loading.value = true
    try {
      nodes.value = await causalityApi.getCausalities(projectId)
    } finally {
      loading.value = false
    }
  }

  async function create(projectId: number, payload: Partial<Causality>) {
    const causality = await causalityApi.createCausality(projectId, payload)
    nodes.value.unshift(causality)
    return causality
  }

  async function update(id: number, payload: Partial<Causality>) {
    await causalityApi.updateCausality(id, payload)
    const target = nodes.value.find((item) => item.id === id)
    if (target) {
      Object.assign(target, payload)
    }
  }

  async function remove(id: number) {
    await causalityApi.deleteCausality(id)
    nodes.value = nodes.value.filter((item) => item.id !== id)
  }

  return { nodes, loading, fetchByProject, create, update, remove }
})
