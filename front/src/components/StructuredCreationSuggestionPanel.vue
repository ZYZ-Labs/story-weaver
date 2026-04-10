<script setup lang="ts">
import { computed } from 'vue'

import type { StructuredCreationSuggestion } from '@/types'

const props = withDefaults(
  defineProps<{
    suggestions?: StructuredCreationSuggestion[]
    applyingKeys?: string[]
    title?: string
  }>(),
  {
    suggestions: () => [],
    applyingKeys: () => [],
    title: '待确认新增对象',
  },
)

const emit = defineEmits<{
  (e: 'apply', suggestion: StructuredCreationSuggestion): void
}>()

const visibleSuggestions = computed(() => props.suggestions || [])

function buildKey(suggestion: StructuredCreationSuggestion) {
  return [
    suggestion.entityType,
    suggestion.sourceChapterId || '',
    suggestion.summary || '',
    JSON.stringify(suggestion.candidateFields || {}),
  ].join('|')
}

function getLabel(entityType?: string) {
  const mapping: Record<string, string> = {
    character: '人物',
    causality: '因果',
    plot: '剧情',
  }
  return mapping[entityType || ''] || entityType || '对象'
}
</script>

<template>
  <v-card class="soft-panel">
    <v-card-title>{{ title }}</v-card-title>
    <v-card-text>
      <div v-if="visibleSuggestions.length" class="suggestion-list">
        <div
          v-for="suggestion in visibleSuggestions"
          :key="buildKey(suggestion)"
          class="suggestion-item"
        >
          <div class="d-flex justify-space-between align-start ga-3">
            <div>
              <div class="text-subtitle-2">{{ suggestion.summary || `新增${getLabel(suggestion.entityType)}候选` }}</div>
              <div v-if="suggestion.sourceExcerpt" class="text-body-2 text-medium-emphasis mt-1">
                {{ suggestion.sourceExcerpt }}
              </div>
            </div>
            <v-chip size="small" color="secondary" variant="tonal">
              {{ getLabel(suggestion.entityType) }}
            </v-chip>
          </div>

          <v-btn
            size="small"
            color="primary"
            variant="tonal"
            class="mt-3"
            :loading="applyingKeys.includes(buildKey(suggestion))"
            @click="emit('apply', suggestion)"
          >
            创建待确认档案
          </v-btn>
        </div>
      </div>

      <div v-else class="text-medium-emphasis">
        当前没有待确认的新增人物、因果或剧情对象。
      </div>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.suggestion-list {
  display: grid;
  gap: 12px;
}

.suggestion-item {
  padding: 14px;
  border-radius: 16px;
  background: rgba(var(--v-theme-surface-variant), 0.24);
}
</style>
