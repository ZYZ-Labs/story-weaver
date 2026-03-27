import { ref } from 'vue'
import { defineStore } from 'pinia'

import * as writingChatApi from '@/api/ai-writing-chat'
import type { AIWritingChatMessageRequest, AIWritingChatSession } from '@/types'

type ChatState = {
  loading: boolean
  sending: boolean
  error: string
  session: AIWritingChatSession | null
}

export const useWritingChatStore = defineStore('writing-chat', () => {
  const chatStates = ref<Record<number, ChatState>>({})

  function createEmptyState(): ChatState {
    return {
      loading: false,
      sending: false,
      error: '',
      session: null,
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
      state.error = error instanceof Error ? error.message : 'Failed to load writing chat'
      throw error
    } finally {
      state.loading = false
    }
  }

  async function sendMessage(chapterId: number, payload: AIWritingChatMessageRequest) {
    const state = ensureState(chapterId)
    state.sending = true
    state.error = ''
    try {
      state.session = await writingChatApi.sendWritingChatMessage(chapterId, payload)
      return state.session
    } catch (error) {
      state.error = error instanceof Error ? error.message : 'Failed to send writing chat message'
      throw error
    } finally {
      state.sending = false
    }
  }

  async function setMessageBackground(chapterId: number, messageId: number, pinned: boolean) {
    const state = ensureState(chapterId)
    state.error = ''
    try {
      state.session = await writingChatApi.setWritingChatMessageBackground(messageId, pinned)
      return state.session
    } catch (error) {
      state.error = error instanceof Error ? error.message : 'Failed to update background memory'
      throw error
    }
  }

  function ensureState(chapterId: number) {
    if (!chatStates.value[chapterId]) {
      chatStates.value[chapterId] = createEmptyState()
    }
    return chatStates.value[chapterId]
  }

  return {
    chatStates,
    getState,
    fetchSession,
    sendMessage,
    setMessageBackground,
  }
})
