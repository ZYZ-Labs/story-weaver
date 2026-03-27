<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'

import MarkdownContent from '@/components/MarkdownContent.vue'
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
const backgroundDraft = ref('')
const editingMessageId = ref<number | null>(null)
const editingContent = ref('')
const savingBackgroundId = ref<number | null>(null)
const addingBackground = ref(false)
const historyRef = ref<HTMLElement | null>(null)

const chatState = computed(() => writingChatStore.getState(props.chapterId))
const session = computed(() => chatState.value.session)
const timelineMessages = computed(() => session.value?.messages || [])
const backgroundMessages = computed(() =>
  timelineMessages.value.filter((item) => item.pinnedToBackground && !item.compressed),
)
const pendingUserMessage = computed(() => chatState.value.pendingUserMessage)
const streamingReply = computed(() => chatState.value.streamingReply)
const hasCompressedSummary = computed(() => Boolean(session.value?.compressedSummary?.trim()))
const panelDisabled = computed(() => props.disabled || !props.chapterId)

watch(
  () => props.chapterId,
  async (chapterId) => {
    resetEditorState()
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
  if (panelDisabled.value || !draftMessage.value.trim()) {
    return
  }
  const content = draftMessage.value.trim()
  draftMessage.value = ''
  try {
    await writingChatStore.sendMessage(props.chapterId!, {
      content,
      selectedProviderId: props.selectedProviderId ?? null,
      selectedModel: props.selectedModel || '',
      entryPoint: props.entryPoint,
    })
  } catch {
    draftMessage.value = content
  }
}

async function addBackgroundNote() {
  if (panelDisabled.value || !backgroundDraft.value.trim()) {
    return
  }
  addingBackground.value = true
  const content = backgroundDraft.value.trim()
  try {
    await writingChatStore.addBackgroundNote(props.chapterId!, content)
    backgroundDraft.value = ''
  } finally {
    addingBackground.value = false
  }
}

async function toggleBackground(message: AIWritingChatMessage) {
  if (panelDisabled.value) {
    return
  }
  await writingChatStore.setMessageBackground(
    props.chapterId!,
    message.id,
    !message.pinnedToBackground,
  )
  if (editingMessageId.value === message.id) {
    resetEditorState()
  }
}

function startEdit(message: AIWritingChatMessage) {
  editingMessageId.value = message.id
  editingContent.value = message.content
}

function cancelEdit() {
  resetEditorState()
}

async function saveEdit() {
  if (panelDisabled.value || !editingMessageId.value || !editingContent.value.trim()) {
    return
  }
  savingBackgroundId.value = editingMessageId.value
  try {
    await writingChatStore.updateBackgroundNote(
      props.chapterId!,
      editingMessageId.value,
      editingContent.value.trim(),
    )
    resetEditorState()
  } finally {
    savingBackgroundId.value = null
  }
}

function resetEditorState() {
  editingMessageId.value = null
  editingContent.value = ''
  savingBackgroundId.value = null
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

function isAiOutput(role: string) {
  return role === 'assistant'
}
</script>

<template>
  <v-card class="chat-panel">
    <v-card-title>{{ title }}</v-card-title>
    <v-card-subtitle>
      先在这里讨论设定、人物和场景目标。生成初稿时，固定背景、历史摘要和最近对话会先归纳整理，再参与写作上下文构建。
    </v-card-subtitle>

    <v-card-text class="chat-panel__body">
      <v-alert v-if="chatState.error" type="error" variant="tonal" class="mb-4">
        {{ chatState.error }}
      </v-alert>

      <div class="chat-panel__meta">
        <v-chip size="small" color="primary" variant="tonal">
          固定背景 {{ backgroundMessages.length }}
        </v-chip>
        <v-chip v-if="hasCompressedSummary" size="small" color="secondary" variant="tonal">
          已启用历史压缩摘要
        </v-chip>
        <div class="text-caption text-medium-emphasis">
          活动窗口：{{ session?.activeWindowChars || 0 }} / {{ session?.maxWindowChars || 0 }}
        </div>
      </div>

      <section class="panel-section">
        <div class="panel-section__header">
          <div class="text-subtitle-1 font-weight-medium">固定背景</div>
          <div class="text-caption text-medium-emphasis">
            可以直接手动补充稳定设定，也可以从聊天消息中固定。
          </div>
        </div>

        <div class="background-composer">
          <v-textarea
            v-model="backgroundDraft"
            rows="3"
            auto-grow
            hide-details
            label="新增固定背景"
            placeholder="例如：女主此时仍然不知道玉佩来自太子；本章不要提前揭示幕后身份。"
            :disabled="panelDisabled || addingBackground"
          />
          <div class="background-composer__actions">
            <div class="text-caption text-medium-emphasis">
              这里适合写稳定设定、硬性限制和明确偏好。
            </div>
            <v-btn
              color="primary"
              variant="flat"
              :loading="addingBackground"
              :disabled="panelDisabled"
              @click="addBackgroundNote"
            >
              添加到固定背景
            </v-btn>
          </div>
        </div>

        <div v-if="backgroundMessages.length" class="background-list">
          <article v-for="message in backgroundMessages" :key="`bg-${message.id}`" class="background-item">
            <div class="background-item__meta">
              <span>{{ roleLabel(message.role) }}</span>
              <span v-if="message.segmentNo">分段 {{ message.segmentNo }}</span>
            </div>

            <div v-if="editingMessageId === message.id" class="background-item__editor">
              <v-textarea
                v-model="editingContent"
                rows="3"
                auto-grow
                hide-details
                :disabled="savingBackgroundId === message.id"
              />
              <div class="background-item__actions">
                <v-btn
                  color="primary"
                  variant="flat"
                  size="small"
                  :loading="savingBackgroundId === message.id"
                  @click="saveEdit"
                >
                  保存修改
                </v-btn>
                <v-btn variant="text" size="small" @click="cancelEdit">取消</v-btn>
              </div>
            </div>

            <template v-else>
              <div class="background-item__content">{{ message.content }}</div>
              <div class="background-item__actions">
                <v-btn variant="text" size="small" @click="startEdit(message)">编辑</v-btn>
                <v-btn variant="text" size="small" @click="toggleBackground(message)">
                  移出固定背景
                </v-btn>
              </div>
            </template>
          </article>
        </div>

        <div v-else class="empty-note">
          还没有固定背景。可以先发起聊天，再把有价值的消息固定下来；也可以直接在上方手动添加。
        </div>
      </section>

      <section class="panel-section">
        <div class="panel-section__header">
          <div class="text-subtitle-1 font-weight-medium">聊天讨论</div>
          <div class="text-caption text-medium-emphasis">
            长文本回复会流式显示，避免等待时像卡住。
          </div>
        </div>

        <div ref="historyRef" class="chat-history">
          <div
            v-if="!timelineMessages.length && !pendingUserMessage && !streamingReply"
            class="empty-note empty-note--history"
          >
            这里会保留你和助手围绕本章的讨论记录。
          </div>

          <div
            v-for="message in timelineMessages"
            :key="message.id"
            class="chat-row"
            :class="`chat-row--${message.role}`"
          >
            <div class="chat-bubble" :class="`chat-bubble--${message.role}`">
              <div class="chat-bubble__meta">
                <span>{{ roleLabel(message.role) }}</span>
                <span v-if="message.compressed">已压缩</span>
                <span v-if="message.pinnedToBackground">已固定到背景</span>
              </div>
              <MarkdownContent
                v-if="isAiOutput(message.role)"
                :source="message.content"
                compact
              />
              <div v-else class="bubble-text">{{ message.content }}</div>
              <div v-if="message.role !== 'system'" class="chat-bubble__actions">
                <v-btn variant="text" size="small" @click="toggleBackground(message)">
                  {{ message.pinnedToBackground ? '移出固定背景' : '固定到背景' }}
                </v-btn>
              </div>
            </div>
          </div>

          <div v-if="pendingUserMessage" class="chat-row chat-row--user">
            <div class="chat-bubble chat-bubble--user chat-bubble--pending">
              <div class="chat-bubble__meta">
                <span>你</span>
                <span>发送中</span>
              </div>
              <div class="bubble-text">{{ pendingUserMessage }}</div>
            </div>
          </div>

          <div v-if="chatState.sending" class="chat-row chat-row--assistant">
          <div class="chat-bubble chat-bubble--assistant chat-bubble--streaming">
            <div class="chat-bubble__meta">
              <span>助手</span>
              <span>回复中</span>
            </div>
            <MarkdownContent v-if="streamingReply" :source="streamingReply" compact />
            <div v-else class="chat-typing">
              <span class="chat-typing-dot" />
              <span class="chat-typing-dot" />
              <span class="chat-typing-dot" />
              </div>
            </div>
          </div>
        </div>

        <div class="chat-composer">
          <v-textarea
            v-model="draftMessage"
            rows="4"
            auto-grow
            hide-details
            label="发送聊天消息"
            placeholder="可以讨论本章目标、人物动机、世界观补充或表达风格。按 Enter 发送，Shift + Enter 换行。"
            :disabled="panelDisabled || chatState.sending"
            @keydown="handleComposerKeydown"
          />
          <div class="chat-composer__actions">
            <div class="text-caption text-medium-emphasis">
              从聊天中固定下来的内容，会优先参与后续上下文整理。
            </div>
            <v-btn
              color="primary"
              variant="flat"
              :loading="chatState.sending"
              :disabled="panelDisabled"
              @click="sendMessage"
            >
              发送消息
            </v-btn>
          </div>
        </div>
      </section>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.chat-panel__body {
  display: grid;
  gap: 20px;
}

.chat-panel__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.panel-section {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid rgba(var(--v-theme-outline), 0.18);
  border-radius: 18px;
  background: rgba(var(--v-theme-surface-variant), 0.08);
}

.panel-section__header {
  display: grid;
  gap: 4px;
}

.background-composer,
.chat-composer,
.background-list {
  display: grid;
  gap: 12px;
}

.background-composer,
.chat-composer {
  padding: 14px;
  border-radius: 16px;
  background: rgba(var(--v-theme-surface), 0.88);
  border: 1px solid rgba(var(--v-theme-outline), 0.14);
}

.background-composer__actions,
.chat-composer__actions,
.background-item__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.background-list {
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.background-item {
  display: grid;
  gap: 10px;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid rgba(var(--v-theme-outline), 0.16);
  background: rgba(var(--v-theme-surface), 0.95);
}

.background-item__meta,
.chat-bubble__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
  color: rgba(var(--v-theme-on-surface), 0.68);
}

.background-item__content,
.bubble-text {
  white-space: pre-wrap;
  line-height: 1.75;
}

.background-item__editor {
  display: grid;
  gap: 10px;
}

.chat-history {
  display: grid;
  gap: 10px;
  max-height: 420px;
  overflow: auto;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid rgba(var(--v-theme-outline), 0.14);
  background: rgba(var(--v-theme-surface), 0.96);
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
  display: grid;
  gap: 8px;
  max-width: min(88%, 680px);
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid rgba(var(--v-theme-outline), 0.14);
  background: rgba(var(--v-theme-surface-variant), 0.12);
}

.chat-bubble--assistant {
  background: rgba(var(--v-theme-primary), 0.07);
}

.chat-bubble--user {
  background: rgba(var(--v-theme-secondary), 0.08);
}

.chat-bubble--system {
  max-width: 92%;
  background: rgba(var(--v-theme-warning), 0.08);
}

.chat-bubble--pending,
.chat-bubble--streaming {
  border-style: dashed;
}

.chat-bubble__actions {
  display: flex;
  justify-content: flex-end;
}

.chat-typing {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  min-height: 24px;
}

.chat-typing-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: rgba(var(--v-theme-primary), 0.72);
  animation: typing-pulse 1.2s infinite ease-in-out;
}

.chat-typing-dot:nth-child(2) {
  animation-delay: 0.15s;
}

.chat-typing-dot:nth-child(3) {
  animation-delay: 0.3s;
}

.empty-note {
  padding: 14px;
  border-radius: 14px;
  border: 1px dashed rgba(var(--v-theme-outline), 0.24);
  color: rgba(var(--v-theme-on-surface), 0.68);
  background: rgba(var(--v-theme-surface), 0.72);
}

.empty-note--history {
  min-height: 96px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
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
