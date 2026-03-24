import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import * as settingsApi from '@/api/settings'
import type { SystemConfig } from '@/types'

const promptTemplateKeys = [
  'prompt.continue',
  'prompt.expand',
  'prompt.rewrite',
  'prompt.polish',
  'prompt.plot',
  'prompt.causality',
  'prompt.rag_query',
  'prompt.knowledge_extract',
] as const

type PromptTemplateKey = (typeof promptTemplateKeys)[number]

export const useSettingsStore = defineStore('settings', () => {
  const configs = ref<SystemConfig[]>([])
  const loading = ref(false)

  const configMap = computed(() => {
    const next = new Map<string, SystemConfig>()
    for (const config of configs.value) {
      next.set(config.configKey, config)
    }
    return next
  })

  const promptTemplates = computed(() =>
    promptTemplateKeys.map((key) => configMap.value.get(key)).filter(Boolean) as SystemConfig[],
  )

  async function fetchAll() {
    loading.value = true
    try {
      configs.value = await settingsApi.getSystemConfigs()
    } finally {
      loading.value = false
    }
  }

  async function saveAll(payload: SystemConfig[]) {
    loading.value = true
    try {
      configs.value = await settingsApi.saveSystemConfigs(payload)
    } finally {
      loading.value = false
    }
  }

  function getConfigValue(key: string, fallback = '') {
    return configMap.value.get(key)?.configValue ?? fallback
  }

  function getNumberValue(key: string, fallback: number | null = null) {
    const value = getConfigValue(key)
    if (!value) {
      return fallback
    }
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : fallback
  }

  function getBooleanValue(key: string, fallback = false) {
    const value = getConfigValue(key)
    if (!value) {
      return fallback
    }
    return value === 'true'
  }

  function getPromptTemplateByWritingType(writingType: string) {
    const mapping: Record<string, PromptTemplateKey> = {
      continue: 'prompt.continue',
      expand: 'prompt.expand',
      rewrite: 'prompt.rewrite',
      polish: 'prompt.polish',
    }
    const key = mapping[writingType]
    return key ? getConfigValue(key) : ''
  }

  function getPromptTemplate(key: PromptTemplateKey | string) {
    return getConfigValue(key)
  }

  return {
    configs,
    loading,
    configMap,
    promptTemplates,
    fetchAll,
    saveAll,
    getConfigValue,
    getNumberValue,
    getBooleanValue,
    getPromptTemplateByWritingType,
    getPromptTemplate,
  }
})
