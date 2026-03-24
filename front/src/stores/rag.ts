import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import * as ragApi from '@/api/rag'
import type { KnowledgeDocument } from '@/types'

export const useRagStore = defineStore('rag', () => {
  const documents = ref<KnowledgeDocument[]>([])
  const queryResults = ref<KnowledgeDocument[]>([])
  const loading = ref(false)

  const knowledgeStats = computed(() => {
    const chunks = documents.value.reduce((total, item) => {
      const length = item.contentText?.length || 0
      return total + Math.max(1, Math.ceil(length / 300))
    }, 0)
    const indexedCount = documents.value.filter((item) => item.status === 'indexed').length
    const indexed = documents.value.length
      ? `${Math.round((indexedCount / documents.value.length) * 100)}%`
      : '0%'

    return {
      documents: documents.value.length,
      chunks,
      indexed,
    }
  })

  async function fetchByProject(projectId: number) {
    loading.value = true
    try {
      documents.value = await ragApi.getKnowledgeDocuments(projectId)
    } finally {
      loading.value = false
    }
  }

  async function create(projectId: number, payload: Partial<KnowledgeDocument>) {
    const document = await ragApi.createKnowledgeDocument(projectId, payload)
    documents.value.unshift(document)
  }

  async function update(id: number, payload: Partial<KnowledgeDocument>) {
    await ragApi.updateKnowledgeDocument(id, payload)
    const target = documents.value.find((item) => item.id === id)
    if (target) {
      Object.assign(target, payload)
    }
  }

  async function remove(id: number) {
    await ragApi.deleteKnowledgeDocument(id)
    documents.value = documents.value.filter((item) => item.id !== id)
  }

  async function query(projectId: number, query: string) {
    queryResults.value = await ragApi.queryKnowledge(projectId, query)
  }

  async function reindex(projectId: number) {
    return ragApi.reindexKnowledge(projectId)
  }

  return { documents, queryResults, loading, knowledgeStats, fetchByProject, create, update, remove, query, reindex }
})
