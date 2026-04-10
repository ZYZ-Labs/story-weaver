<script setup lang="ts">
import { computed } from 'vue'

import type { GenerationReadiness } from '@/types'

const props = withDefaults(
  defineProps<{
    readiness?: GenerationReadiness | null
    loading?: boolean
    title?: string
  }>(),
  {
    readiness: null,
    loading: false,
    title: '生成就绪度',
  },
)

const statusLabel = computed(() => {
  const value = props.readiness?.status
  const mapping: Record<string, string> = {
    ready: '可生成',
    warning: '需确认',
    blocked: '锚点不足',
  }
  return mapping[value || ''] || value || '未知'
})

const statusColor = computed(() => {
  const value = props.readiness?.status
  if (value === 'ready') return 'success'
  if (value === 'warning') return 'warning'
  return 'error'
})
</script>

<template>
  <v-card class="soft-panel">
    <v-card-title class="d-flex justify-space-between align-center ga-3">
      <span>{{ title }}</span>
      <v-chip
        v-if="readiness"
        size="small"
        :color="statusColor"
        variant="tonal"
      >
        {{ statusLabel }}
        <template v-if="readiness.score !== undefined">
          · {{ readiness.score }}
        </template>
      </v-chip>
    </v-card-title>

    <v-card-text>
      <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-4" />

      <div v-if="readiness">
        <div v-if="readiness.blockingIssues?.length" class="readiness-section">
          <div class="text-subtitle-2">阻塞项</div>
          <ul class="readiness-list">
            <li v-for="item in readiness.blockingIssues" :key="item">{{ item }}</li>
          </ul>
        </div>

        <div v-if="readiness.warnings?.length" class="readiness-section">
          <div class="text-subtitle-2">提醒</div>
          <ul class="readiness-list">
            <li v-for="item in readiness.warnings" :key="item">{{ item }}</li>
          </ul>
        </div>

        <div v-if="readiness.recommendedModules?.length" class="readiness-section">
          <div class="text-subtitle-2">建议补齐</div>
          <div class="d-flex flex-wrap ga-2 mt-2">
            <v-chip
              v-for="item in readiness.recommendedModules"
              :key="item"
              size="small"
              color="secondary"
              variant="tonal"
            >
              {{ item }}
            </v-chip>
          </div>
        </div>
      </div>

      <div v-else class="text-medium-emphasis">
        当前章节的生成就绪度会显示在这里。
      </div>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.readiness-section + .readiness-section {
  margin-top: 16px;
}

.readiness-list {
  margin: 8px 0 0;
  padding-left: 18px;
}

.readiness-list li + li {
  margin-top: 6px;
}
</style>
