<script setup lang="ts">
import { ref } from 'vue'

import MarkdownContent from '@/components/MarkdownContent.vue'

withDefaults(
  defineProps<{
    modelValue?: string
    label: string
    rows?: number
    placeholder?: string
    hint?: string
    persistentHint?: boolean
    disabled?: boolean
    autoGrow?: boolean
    previewEmptyText?: string
  }>(),
  {
    modelValue: '',
    rows: 6,
    placeholder: '',
    hint: '',
    persistentHint: false,
    disabled: false,
    autoGrow: false,
    previewEmptyText: '暂无内容',
  },
)

const emit = defineEmits<{
  'update:modelValue': [string]
}>()

const mode = ref<'write' | 'preview'>('write')
</script>

<template>
  <div class="markdown-editor">
    <div class="d-flex justify-space-between align-center mb-2">
      <div class="text-subtitle-2 font-weight-medium">{{ label }}</div>
      <div class="d-flex align-center ga-2">
        <v-chip size="small" color="secondary" variant="tonal">Markdown</v-chip>
        <v-btn-toggle v-model="mode" density="compact" mandatory variant="outlined">
          <v-btn value="write" size="small">编辑</v-btn>
          <v-btn value="preview" size="small">预览</v-btn>
        </v-btn-toggle>
      </div>
    </div>

    <v-textarea
      v-if="mode === 'write'"
      :model-value="modelValue"
      :rows="rows"
      :placeholder="placeholder"
      :hint="hint || '支持 Markdown 语法，如标题、列表、引用、代码块和链接。'"
      :persistent-hint="persistentHint || Boolean(hint)"
      :disabled="disabled"
      :auto-grow="autoGrow"
      @update:model-value="emit('update:modelValue', $event)"
    />

    <v-sheet v-else class="preview-shell rounded-xl pa-4">
      <MarkdownContent :source="modelValue" :empty-text="previewEmptyText" />
    </v-sheet>
  </div>
</template>

<style scoped>
.preview-shell {
  min-height: 180px;
  background: rgba(var(--v-theme-surface-variant), 0.2);
}
</style>
