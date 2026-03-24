import { ref } from 'vue'
import { defineStore } from 'pinia'

import * as writingApi from '@/api/ai-writing'
import type { AIWritingRecord, AIWritingRequest } from '@/types'

export const useWritingStore = defineStore('writing', () => {
  const records = ref<AIWritingRecord[]>([])
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
    return record
  }

  async function accept(id: number) {
    const updated = await writingApi.acceptWriting(id)
    const target = records.value.find((item) => item.id === id)
    if (target) {
      Object.assign(target, updated)
    }
  }

  async function reject(id: number) {
    await writingApi.rejectWriting(id)
    const target = records.value.find((item) => item.id === id)
    if (target) {
      target.status = 'rejected'
    }
  }

  return {
    records,
    loading,
    fetchByChapter,
    generate,
    accept,
    reject,
  }
})
