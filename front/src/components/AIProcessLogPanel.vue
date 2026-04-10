<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'

import type { AIWritingStreamLogItem } from '@/types'

const props = withDefaults(
  defineProps<{
    logs?: AIWritingStreamLogItem[]
    loading?: boolean
    title?: string
  }>(),
  {
    logs: () => [],
    loading: false,
    title: '生成流水线日志',
  },
)

const displayLogs = computed(() => props.logs || [])
const logListRef = ref<HTMLElement | null>(null)

function scrollToBottom() {
  const element = logListRef.value
  if (!element) {
    return
  }
  element.scrollTop = element.scrollHeight
}

function syncScrollToBottom() {
  void nextTick(() => {
    scrollToBottom()
  })
}

onMounted(() => {
  syncScrollToBottom()
})

watch(
  () => props.logs?.length || 0,
  () => {
    syncScrollToBottom()
  },
)

watch(
  () => props.loading,
  (loading) => {
    if (loading) {
      syncScrollToBottom()
    }
  },
)

function formatStage(item: AIWritingStreamLogItem) {
  const stageMap: Record<string, string> = {
    director: '总导',
    prepare: '准备',
    context: '背景整理',
    plan: '规划',
    write: '写作',
    check: '检查',
    revise: '修订',
  }
  const statusMap: Record<string, string> = {
    started: '开始',
    completed: '完成',
  }

  const stageLabel = item.stage
    ? stageMap[item.stage] || item.stage
    : item.type === 'error'
      ? '错误'
      : '日志'
  const statusLabel = item.stageStatus ? statusMap[item.stageStatus] || item.stageStatus : ''
  return statusLabel ? `${stageLabel} | ${statusLabel}` : stageLabel
}

function formatMeta(item: AIWritingStreamLogItem) {
  const parts: string[] = []
  if ((item.occurrenceCount || 1) > 1) {
    parts.push(`已重复 ${item.occurrenceCount} 次`)
  }
  if ((item.elapsedSeconds || 0) > 0) {
    parts.push(`已持续 ${item.elapsedSeconds} 秒`)
  }
  return parts.join(' · ')
}
</script>

<template>
  <v-card class="soft-panel">
    <v-card-title>{{ title }}</v-card-title>
    <v-card-text>
      <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-4" />

      <div v-if="displayLogs.length" ref="logListRef" class="log-list">
        <div v-for="item in displayLogs" :key="item.id" class="log-item">
          <div class="d-flex justify-space-between align-center ga-3">
            <div class="text-caption text-medium-emphasis">{{ formatStage(item) }}</div>
            <div v-if="formatMeta(item)" class="text-caption text-medium-emphasis">
              {{ formatMeta(item) }}
            </div>
          </div>
          <div class="text-body-2">{{ item.message || '处理中...' }}</div>
        </div>
      </div>
      <div v-else class="text-medium-emphasis">
        这里展示的是一次结果导向的生成流水线，不是持续会话式正文聊天。总导、准备、背景整理、规划、写作、自检和修订的阶段更新都会显示在这里。
      </div>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.log-list {
  display: grid;
  gap: 12px;
  max-height: 280px;
  overflow: auto;
}

.log-item {
  padding: 12px;
  border-radius: 14px;
  background: rgba(var(--v-theme-surface-variant), 0.24);
}
</style>
