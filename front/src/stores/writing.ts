import { ref } from 'vue'
import { defineStore } from 'pinia'

import * as writingApi from '@/api/ai-writing'
import type {
  AIWritingRecord,
  AIWritingRequest,
  AIWritingStreamEvent,
  AIWritingStreamLogItem,
  AIWritingStreamState,
} from '@/types'

export const useWritingStore = defineStore('writing', () => {
  const records = ref<AIWritingRecord[]>([])
  const projectRecords = ref<AIWritingRecord[]>([])
  const streamStates = ref<Record<number, AIWritingStreamState>>({})
  const loading = ref(false)

  function createEmptyStreamState(): AIWritingStreamState {
    return {
      generating: false,
      content: '',
      error: '',
      lastRecord: null,
      selectedProviderId: null,
      selectedModel: '',
      maxTokens: null,
      logs: [],
    }
  }

  function getStreamState(chapterId?: number | null) {
    if (!chapterId) {
      return createEmptyStreamState()
    }
    return streamStates.value[chapterId] || createEmptyStreamState()
  }

  async function fetchByChapter(chapterId: number) {
    loading.value = true
    try {
      records.value = filterPendingRecords(await writingApi.getWritingRecords(chapterId))
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
    const requestId = `${Date.now()}-${Math.random().toString(36).slice(2)}`
    streamStates.value[payload.chapterId] = {
      requestId,
      generating: true,
      content: '',
      error: '',
      lastRecord: null,
      writingType: payload.writingType,
      selectedProviderId: payload.selectedProviderId ?? null,
      selectedModel: payload.selectedModel || '',
      maxTokens: payload.maxTokens ?? null,
      logs: [],
    }

    try {
      const record = await writingApi.streamGenerateWriting(payload, {
        onEvent: (event) => {
          const currentState = streamStates.value[payload.chapterId]
          if (!currentState || currentState.requestId !== requestId) {
            return
          }

          if (event.type === 'meta') {
            currentState.writingType = event.writingType || currentState.writingType
            currentState.selectedProviderId = event.selectedProviderId ?? currentState.selectedProviderId ?? null
            currentState.selectedModel = event.selectedModel || currentState.selectedModel || ''
            currentState.maxTokens = event.maxTokens ?? currentState.maxTokens ?? null
          } else if (event.type === 'stage' || event.type === 'log') {
            appendLogItem(currentState.logs, event)
          } else if (event.type === 'chunk' && event.delta) {
            currentState.content += event.delta
          } else if (event.type === 'replace') {
            currentState.content = event.content || ''
            appendLogItem(currentState.logs, createRevisionAppliedLogEvent())
          } else if (event.type === 'error') {
            currentState.generating = false
            currentState.error = event.message || 'AI 生成失败'
            appendLogItem(currentState.logs, event)
          } else if (event.type === 'complete' && event.record) {
            const completedRecord = mergeCompletedRecord(event.record, currentState.content)
            currentState.generating = false
            currentState.lastRecord = completedRecord
            currentState.content = completedRecord.generatedContent || currentState.content
          }

          handlers.onEvent?.(event)
        },
      })

      const currentState = streamStates.value[payload.chapterId]
      const completedRecord = mergeCompletedRecord(record, currentState?.content || '')
      records.value.unshift(completedRecord)
      projectRecords.value.unshift(completedRecord)
      if (currentState?.requestId === requestId) {
        currentState.generating = false
        currentState.lastRecord = completedRecord
        currentState.content = completedRecord.generatedContent || currentState.content
      }
      return completedRecord
    } catch (error) {
      const currentState = streamStates.value[payload.chapterId]
      if (currentState?.requestId === requestId) {
        currentState.generating = false
        currentState.error = error instanceof Error ? error.message : 'AI 生成失败'
      }
      throw error
    }
  }

  async function fetchByProject(projectId: number) {
    loading.value = true
    try {
      projectRecords.value = filterPendingRecords(await writingApi.getProjectWritingRecords(projectId))
    } finally {
      loading.value = false
    }
  }

  async function accept(id: number) {
    const updated = await writingApi.acceptWriting(id)
    removeResolvedRecord(updated)
    return updated
  }

  async function reject(id: number) {
    const updated = await writingApi.rejectWriting(id)
    removeResolvedRecord(updated)
    return updated
  }

  function filterPendingRecords(items: AIWritingRecord[]) {
    return (items || []).filter((item) => item.status !== 'accepted' && item.status !== 'rejected')
  }

  function removeResolvedRecord(record: AIWritingRecord) {
    records.value = records.value.filter((item) => item.id !== record.id)
    projectRecords.value = projectRecords.value.filter((item) => item.id !== record.id)

    if (record.chapterId) {
      const streamState = streamStates.value[record.chapterId]
      if (streamState?.lastRecord?.id === record.id) {
        streamState.lastRecord = records.value.find((item) => item.chapterId === record.chapterId) || null
      }
    }
  }

  function toLogItem(event: AIWritingStreamEvent): AIWritingStreamLogItem {
    const now = Date.now()
    return {
      id: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
      type: event.type,
      stage: event.stage,
      stageStatus: event.stageStatus,
      message: event.message,
      occurrenceCount: 1,
      firstSeenAt: now,
      lastSeenAt: now,
      elapsedSeconds: 0,
    }
  }

  function appendLogItem(logs: AIWritingStreamLogItem[], event: AIWritingStreamEvent) {
    if (event.type !== 'log') {
      logs.push(toLogItem(event))
      return
    }

    const signature = buildLogSignature(event)
    for (let index = logs.length - 1; index >= 0; index -= 1) {
      const item = logs[index]
      if (buildLogSignature(item) !== signature) {
        continue
      }

      const now = Date.now()
      const firstSeenAt = item.firstSeenAt || now
      item.occurrenceCount = (item.occurrenceCount || 1) + 1
      item.lastSeenAt = now
      item.elapsedSeconds = Math.max(0, Math.floor((now - firstSeenAt) / 1000))
      return
    }

    logs.push(toLogItem(event))
  }

  function createRevisionAppliedLogEvent(): AIWritingStreamEvent {
    return {
      type: 'log',
      stage: 'revise',
      stageStatus: 'completed',
      message: '已应用修订稿，当前预览已更新为修订后的版本。',
    }
  }

  function buildLogSignature(item: Pick<AIWritingStreamLogItem, 'type' | 'stage' | 'stageStatus' | 'message'>) {
    return [item.type || '', item.stage || '', item.stageStatus || '', item.message || ''].join('|')
  }

  function mergeCompletedRecord(record: AIWritingRecord, streamedContent: string) {
    return {
      ...record,
      generatedContent: record.generatedContent || streamedContent,
    }
  }

  return {
    records,
    projectRecords,
    streamStates,
    loading,
    getStreamState,
    fetchByChapter,
    fetchByProject,
    generate,
    generateStream,
    accept,
    reject,
  }
})
