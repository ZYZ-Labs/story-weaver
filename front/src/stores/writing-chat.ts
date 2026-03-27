import { ref } from 'vue'
import { defineStore } from 'pinia'

import * as writingChatApi from '@/api/ai-writing-chat'
import type { AIWritingChatMessageRequest, AIWritingChatSession, AIWritingChatStreamEvent } from '@/types'

type ChatState = {
  loading: boolean
  sending: boolean
  error: string
  session: AIWritingChatSession | null
  pendingUserMessage: string
  streamingReply: string
  selectedProviderId: number | null
  selectedModel: string
}

export const useWritingChatStore = defineStore('writing-chat', () => {
  const chatStates = ref<Record<number, ChatState>>({})

  function createEmptyState(): ChatState {
    return {
      loading: false,
      sending: false,
      error: '',
      session: null,
      pendingUserMessage: '',
      streamingReply: '',
      selectedProviderId: null,
      selectedModel: '',
    }
  }

  function getState(chapterId?: number | null) {
    if (!chapterId) {
      return createEmptyState()
    }
    return chatStates.value[chapterId] || createEmptyState()
  }

  async function fetchSession(chapterId: number) {
    const state = ensureState(chapterId)
    state.loading = true
    state.error = ''
    try {
      state.session = await writingChatApi.getWritingChatSession(chapterId)
      return state.session
    } catch (error) {
      state.error = error instanceof Error ? error.message : '加载背景聊天失败'
      throw error
    } finally {
      state.loading = false
    }
  }

  async function sendMessage(chapterId: number, payload: AIWritingChatMessageRequest) {
    const state = ensureState(chapterId)
    state.sending = true
    state.error = ''
    state.pendingUserMessage = payload.content.trim()
    state.streamingReply = ''
    state.selectedProviderId = payload.selectedProviderId ?? null
    state.selectedModel = payload.selectedModel || ''
    try {
      state.session = await writingChatApi.streamWritingChatMessage(chapterId, payload, {
        onEvent: (event) => applyStreamEvent(state, event),
      })
      return state.session
    } catch (error) {
      state.error = error instanceof Error ? error.message : '发送背景聊天消息失败'
      await fetchSession(chapterId).catch(() => undefined)
      throw error
    } finally {
      state.sending = false
      state.pendingUserMessage = ''
      state.streamingReply = ''
    }
  }

  async function setMessageBackground(chapterId: number, messageId: number, pinned: boolean) {
    const state = ensureState(chapterId)
    state.error = ''
    try {
      state.session = await writingChatApi.setWritingChatMessageBackground(messageId, pinned)
      return state.session
    } catch (error) {
      state.error = error instanceof Error ? error.message : '更新背景信息失败'
      throw error
    }
  }

  function ensureState(chapterId: number) {
    if (!chatStates.value[chapterId]) {
      chatStates.value[chapterId] = createEmptyState()
    }
    return chatStates.value[chapterId]
  }

  function applyStreamEvent(state: ChatState, event: AIWritingChatStreamEvent) {
    if (event.type === 'meta') {
      state.selectedProviderId = event.selectedProviderId ?? state.selectedProviderId
      state.selectedModel = event.selectedModel || state.selectedModel
      return
    }

    if (event.type === 'chunk' && event.delta) {
      state.streamingReply += event.delta
      return
    }

    if (event.type === 'complete' && event.session) {
      state.session = event.session
      return
    }

    if (event.type === 'error') {
      state.error = event.message || '发送背景聊天消息失败'
    }
  }

  return {
    chatStates,
    getState,
    fetchSession,
    sendMessage,
    setMessageBackground,
  }
})
