<script setup lang="ts">
import { computed } from 'vue'

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
    title: 'Process Log',
  },
)

const displayLogs = computed(() => props.logs || [])

function formatStage(item: AIWritingStreamLogItem) {
  if (!item.stage) {
    return item.type.toUpperCase()
  }
  if (!item.stageStatus) {
    return item.stage.toUpperCase()
  }
  return `${item.stage.toUpperCase()} · ${item.stageStatus}`
}
</script>

<template>
  <v-card class="soft-panel">
    <v-card-title>{{ title }}</v-card-title>
    <v-card-text>
      <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-4" />

      <div v-if="displayLogs.length" class="log-list">
        <div v-for="item in displayLogs" :key="item.id" class="log-item">
          <div class="text-caption text-medium-emphasis">{{ formatStage(item) }}</div>
          <div class="text-body-2">{{ item.message || 'Working...' }}</div>
        </div>
      </div>
      <div v-else class="text-medium-emphasis">
        Stage updates will appear here while the model is planning, writing, checking, and revising.
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
