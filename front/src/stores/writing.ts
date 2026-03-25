import { ref } from 'vue'
import { defineStore } from 'pinia'

import * as writingApi from '@/api/ai-writing'
import type { AIWritingRecord, AIWritingRequest, AIWritingStreamEvent } from '@/types'

export const useWritingStore = defineStore('writing', () => {
  const records = ref<AIWritingRecord[]>([])
  const projectRecords = ref<AIWritingRecord[]>([])
  const loading = ref(false)

  async function fetchByChapter(chapterId: number) {
    loading.value = true
    try {
      records.value = await writingApi.getWritingRecords(chapterId)
    } finally {
      loading.value = false
    }
  }

  async function generate(payload: AIWritingRequest) {
    const record = await writingApi.generateWriting(payload)
    records.value.unshift(record)
    projectRecords.value.unshift(record)
    return record
  }

  async function generateStream(
    payload: AIWritingRequest,
    handlers: {
      onEvent?: (event: AIWritingStreamEvent) => void
    } = {},
  ) {
    const record = await writingApi.streamGenerateWriting(payload, handlers)
    records.value.unshift(record)
    projectRecords.value.unshift(record)
    return record
  }

  async function fetchByProject(projectId: number) {
    loading.value = true
    try {
      projectRecords.value = await writingApi.getProjectWritingRecords(projectId)
    } finally {
      loading.value = false
    }
  }

  async function accept(id: number) {
    const updated = await writingApi.acceptWriting(id)
    const target = records.value.find((item) => item.id === id)
    const projectTarget = projectRecords.value.find((item) => item.id === id)
    if (target) {
      Object.assign(target, updated)
    }
    if (projectTarget) {
      Object.assign(projectTarget, updated)
    }
    return updated
  }

  async function reject(id: number) {
    await writingApi.rejectWriting(id)
    const target = records.value.find((item) => item.id === id)
    const projectTarget = projectRecords.value.find((item) => item.id === id)
    if (target) {
      target.status = 'rejected'
    }
    if (projectTarget) {
      projectTarget.status = 'rejected'
    }
    return 'rejected'
  }

  return {
    records,
    projectRecords,
    loading,
    fetchByChapter,
    fetchByProject,
    generate,
    generateStream,
    accept,
    reject,
  }
})
