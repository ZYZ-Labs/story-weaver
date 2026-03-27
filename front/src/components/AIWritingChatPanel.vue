<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import { useWritingChatStore } from '@/stores/writing-chat'
import type { AIWritingChatMessage } from '@/types'

const props = withDefaults(
  defineProps<{
    chapterId?: number | null
    selectedProviderId?: number | null
    selectedModel?: string
    entryPoint?: string
    disabled?: boolean
    title?: string
  }>(),
  {
    chapterId: null,
    selectedProviderId: null,
    selectedModel: '',
    entryPoint: 'writing-center',
    disabled: false,
    title: 'Background Chat',
  },
)

const writingChatStore = useWritingChatStore()
const draftMessage = ref('')

const chatState = computed(() => writingChatStore.getState(props.chapterId))
const session = computed(() => chatState.value.session)
const backgroundMessages = computed(() =>
  (session.value?.messages || []).filter((item) => item.pinnedToBackground),
)

watch(
  () => props.chapterId,
  async (chapterId) => {
    if (!chapterId) {
      return
    }
    await writingChatStore.fetchSession(chapterId).catch(() => undefined)
  },
  { immediate: true },
)

async function sendMessage() {
  if (!props.chapterId || !draftMessage.value.trim() || props.disabled) {
    return
  }
  const content = draftMessage.value.trim()
  draftMessage.value = ''
  await writingChatStore.sendMessage(props.chapterId, {
    content,
    selectedProviderId: props.selectedProviderId ?? null,
    selectedModel: props.selectedModel || '',
    entryPoint: props.entryPoint,
  })
}

async function toggleBackground(message: AIWritingChatMessage) {
  if (!props.chapterId) {
    return
  }
  await writingChatStore.setMessageBackground(
    props.chapterId,
    message.id,
    !message.pinnedToBackground,
  )
}

function roleLabel(role: string) {
  if (role === 'user') return 'You'
  if (role === 'system') return 'System'
  return 'Assistant'
}
</script>

<template>
  <v-card class="soft-panel">
    <v-card-title>{{ title }}</v-card-title>
    <v-card-subtitle>
      Chat here to build background context, then pin any useful message into reusable memory.
    </v-card-subtitle>
    <v-card-text class="pt-4">
      <v-alert v-if="chatState.error" type="error" variant="tonal" class="mb-4">
        {{ chatState.error }}
      </v-alert>

      <div v-if="backgroundMessages.length" class="background-memory mb-4">
        <div class="text-subtitle-2 font-weight-medium mb-2">Pinned Background</div>
        <div class="d-flex flex-column ga-2">
          <div v-for="message in backgroundMessages" :key="`bg-${message.id}`" class="memory-item">
            <div class="text-caption text-medium-emphasis">{{ roleLabel(message.role) }}</div>
            <div class="text-body-2">{{ message.content }}</div>
          </div>
        </div>
      </div>

      <div class="chat-history">
        <div
          v-for="message in session?.messages || []"
          :key="message.id"
          class="chat-message"
          :class="`chat-message--${message.role}`"
        >
          <div class="d-flex justify-space-between align-start ga-3">
            <div>
              <div class="text-caption text-medium-emphasis">
                {{ roleLabel(message.role) }}
                <span v-if="message.compressed"> · compressed</span>
                <span v-if="message.segmentNo"> · segment {{ message.segmentNo }}</span>
              </div>
              <div class="text-body-2">{{ message.content }}</div>
            </div>
            <v-btn
              v-if="message.role !== 'system'"
              size="x-small"
              variant="text"
              @click="toggleBackground(message)"
            >
              {{ message.pinnedToBackground ? 'Unpin' : 'Pin as Background' }}
            </v-btn>
          </div>
        </div>
      </div>

      <v-textarea
        v-model="draftMessage"
        class="mt-4"
        rows="4"
        label="Chat Message"
        placeholder="Ask for scene direction, worldbuilding refinement, character voice, or anything worth saving into background context."
        :disabled="disabled"
      />

      <div class="d-flex justify-space-between align-center ga-3">
        <div class="text-caption text-medium-emphasis">
          Active window: {{ session?.activeWindowChars || 0 }} / {{ session?.maxWindowChars || 0 }}
        </div>
        <v-btn color="primary" :loading="chatState.sending" :disabled="disabled" @click="sendMessage">
          Send to Background Chat
        </v-btn>
      </div>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.background-memory,
.chat-history {
  display: grid;
  gap: 12px;
}

.background-memory {
  padding: 12px;
  border-radius: 16px;
  background: rgba(var(--v-theme-primary), 0.05);
}

.memory-item,
.chat-message {
  padding: 12px;
  border-radius: 14px;
  background: rgba(var(--v-theme-surface-variant), 0.24);
}

.chat-history {
  max-height: 320px;
  overflow: auto;
}

.chat-message--assistant {
  border-left: 3px solid rgba(var(--v-theme-primary), 0.65);
}

.chat-message--user {
  border-left: 3px solid rgba(var(--v-theme-secondary), 0.65);
}

.chat-message--system {
  border-left: 3px solid rgba(var(--v-theme-warning), 0.65);
}
</style>
