import { ref } from 'vue'
import { defineStore } from 'pinia'

import * as providerApi from '@/api/provider'
import type { AIProvider } from '@/types'

export const useProviderStore = defineStore('provider', () => {
  const providers = ref<AIProvider[]>([])
  const loading = ref(false)

  async function fetchAll() {
    loading.value = true
    try {
      providers.value = await providerApi.getProviders()
    } finally {
      loading.value = false
    }
  }

  async function create(payload: Partial<AIProvider>) {
    const provider = await providerApi.createProvider(payload)
    providers.value.unshift(provider)
  }

  async function update(id: number, payload: Partial<AIProvider>) {
    const updated = await providerApi.updateProvider(id, payload)
    const target = providers.value.find((item) => item.id === id)
    if (target) {
      Object.assign(target, updated)
    }
  }

  async function remove(id: number) {
    await providerApi.deleteProvider(id)
    providers.value = providers.value.filter((item) => item.id !== id)
  }

  async function test(id: number) {
    return providerApi.testProvider(id)
  }

  return { providers, loading, fetchAll, create, update, remove, test }
})
