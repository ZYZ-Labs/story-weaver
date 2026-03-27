<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'

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
    title: '背景聊天',
  },
)

const writingChatStore = useWritingChatStore()
const draftMessage = ref('')
const historyRef = ref<HTMLElement | null>(null)

const chatState = computed(() => writingChatStore.getState(props.chapterId))
const session = computed(() => chatState.value.session)
const backgroundMessages = computed(() =>
  (session.value?.messages || []).filter((item) => item.pinnedToBackground),
)
const pendingUserMessage = computed(() => chatState.value.pendingUserMessage)
const streamingReply = computed(() => chatState.value.streamingReply)
const hasCompressedSummary = computed(() => Boolean(session.value?.compressedSummary?.trim()))

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

watch(
  [() => session.value?.messages?.length, pendingUserMessage, streamingReply],
  async () => {
    await nextTick()
    historyRef.value?.scrollTo({ top: historyRef.value.scrollHeight, behavior: 'smooth' })
  },
)

async function sendMessage() {
  if (!props.chapterId || !draftMessage.value.trim() || props.disabled) {
    return
  }
  const content = draftMessage.value.trim()
  draftMessage.value = ''
  try {
    await writingChatStore.sendMessage(props.chapterId, {
      content,
      selectedProviderId: props.selectedProviderId ?? null,
      selectedModel: props.selectedModel || '',
      entryPoint: props.entryPoint,
    })
  } catch {
    // 错误状态已经写入 store，这里不再重复抛出
  }
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

function handleComposerKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage().catch(() => undefined)
  }
}

function roleLabel(role: string) {
  if (role === 'user') return '你'
  if (role === 'system') return '系统'
  return '助手'
}
</script>

<template>
  <v-card class="soft-panel">
    <v-card-title>{{ title }}</v-card-title>
    <v-card-subtitle>
      在这里先讨论背景设定、场景方向和人物要求；生成初稿时，固定背景、聊天摘要和最近对话会先归纳整理，再参与后续写作上下文构建。
    </v-card-subtitle>
    <v-card-text class="pt-4">
      <v-alert v-if="chatState.error" type="error" variant="tonal" class="mb-4">
        {{ chatState.error }}
      </v-alert>

      <div class="chat-status-bar mb-4">
        <v-chip size="small" color="primary" variant="tonal">
          已固定背景 {{ backgroundMessages.length }}
        </v-chip>
        <v-chip v-if="hasCompressedSummary" size="small" color="secondary" variant="tonal">
          已启用历史摘要压缩
        </v-chip>
        <div class="text-caption text-medium-emphasis">
          活动窗口：{{ session?.activeWindowChars || 0 }} / {{ session?.maxWindowChars || 0 }}
        </div>
      </div>

      <div ref="historyRef" class="chat-history">
        <div
          v-for="message in session?.messages || []"
          :key="message.id"
          class="chat-row"
          :class="`chat-row--${message.role}`"
        >
          <div class="chat-bubble" :class="`chat-bubble--${message.role}`">
            <div class="d-flex justify-space-between align-start ga-3">
              <div class="chat-bubble__content">
                <div class="text-caption text-medium-emphasis">
                  {{ roleLabel(message.role) }}
                  <span v-if="message.compressed"> | 已压缩</span>
                  <span v-if="message.segmentNo"> | 分段 {{ message.segmentNo }}</span>
                </div>
                <div class="text-body-2 bubble-text">{{ message.content }}</div>
              </div>
              <v-btn
                v-if="message.role !== 'system'"
                size="x-small"
                variant="text"
                class="chat-pin-btn"
                @click="toggleBackground(message)"
              >
                {{ message.pinnedToBackground ? '取消固定' : '固定到背景' }}
              </v-btn>
            </div>
          </div>
        </div>

        <div v-if="pendingUserMessage" class="chat-row chat-row--user">
          <div class="chat-bubble chat-bubble--user chat-bubble--pending">
            <div class="text-caption text-medium-emphasis">你 | 正在发送</div>
            <div class="text-body-2 bubble-text">{{ pendingUserMessage }}</div>
          </div>
        </div>

        <div v-if="chatState.sending" class="chat-row chat-row--assistant">
          <div class="chat-bubble chat-bubble--assistant chat-bubble--streaming">
            <div class="text-caption text-medium-emphasis">
              助手 | 正在回复
            </div>
            <div v-if="streamingReply" class="text-body-2 bubble-text">{{ streamingReply }}</div>
            <div v-else class="chat-typing">
              <span class="chat-typing-dot" />
              <span class="chat-typing-dot" />
              <span class="chat-typing-dot" />
            </div>
          </div>
        </div>
      </div>

      <div v-if="backgroundMessages.length" class="background-memory mt-4">
        <div class="text-subtitle-2 font-weight-medium mb-2">已固定背景速览</div>
        <div class="d-flex flex-column ga-2">
          <div v-for="message in backgroundMessages" :key="`bg-${message.id}`" class="memory-item">
            <div class="text-caption text-medium-emphasis">{{ roleLabel(message.role) }}</div>
            <div class="text-body-2 bubble-text">{{ message.content }}</div>
          </div>
        </div>
      </div>

      <div class="chat-composer mt-4">
        <v-textarea
          v-model="draftMessage"
          rows="4"
          auto-grow
          hide-details
          label="背景聊天消息"
          placeholder="可以讨论场景目标、世界观补充、人物口吻，或任何值得沉淀到背景信息里的内容。按 Enter 发送，Shift + Enter 换行。"
          :disabled="disabled || chatState.sending"
          @keydown="handleComposerKeydown"
        />

        <div class="d-flex justify-space-between align-center ga-3 mt-3">
          <div class="text-caption text-medium-emphasis">
            长文本回复现在会走流式输出，避免等待太久时像卡住。
          </div>
          <v-btn color="primary" :loading="chatState.sending" :disabled="disabled" @click="sendMessage">
            发送到背景聊天
          </v-btn>
        </div>
      </div>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.background-memory,
.chat-history,
.chat-composer {
  display: grid;
  gap: 12px;
}

.chat-status-bar {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.background-memory {
  padding: 12px;
  border-radius: 16px;
  background: rgba(var(--v-theme-primary), 0.05);
}

.memory-item {
  padding: 12px;
  border-radius: 14px;
  background: rgba(var(--v-theme-surface-variant), 0.24);
}

.chat-history {
  max-height: 420px;
  overflow: auto;
  padding: 8px 4px;
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(var(--v-theme-surface-variant), 0.22), rgba(var(--v-theme-surface), 0.18));
}

.chat-row {
  display: flex;
}

.chat-row--assistant {
  justify-content: flex-start;
}

.chat-row--user {
  justify-content: flex-end;
}

.chat-row--system {
  justify-content: center;
}

.chat-bubble {
  max-width: min(88%, 640px);
  padding: 12px 14px;
  border-radius: 18px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.chat-bubble--assistant {
  background: rgba(var(--v-theme-primary), 0.08);
  border-top-left-radius: 6px;
}

.chat-bubble--user {
  background: rgba(var(--v-theme-secondary), 0.1);
  border-top-right-radius: 6px;
}

.chat-bubble--system {
  max-width: 90%;
  background: rgba(var(--v-theme-warning), 0.1);
  border-radius: 14px;
}

.chat-bubble--pending,
.chat-bubble--streaming {
  outline: 1px dashed rgba(var(--v-theme-primary), 0.3);
}

.chat-bubble__content {
  min-width: 0;
}

.bubble-text {
  white-space: pre-wrap;
  line-height: 1.75;
}

.chat-pin-btn {
  flex-shrink: 0;
}

.chat-composer {
  padding: 14px;
  border-radius: 18px;
  background: rgba(var(--v-theme-surface-variant), 0.18);
}

.chat-typing {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 28px;
}

.chat-typing-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: rgba(var(--v-theme-primary), 0.7);
  animation: typing-pulse 1.2s infinite ease-in-out;
}

.chat-typing-dot:nth-child(2) {
  animation-delay: 0.15s;
}

.chat-typing-dot:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes typing-pulse {
  0%,
  80%,
  100% {
    opacity: 0.35;
    transform: translateY(0);
  }

  40% {
    opacity: 1;
    transform: translateY(-2px);
  }
}
</style>
