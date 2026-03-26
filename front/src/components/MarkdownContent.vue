<script setup lang="ts">
import { computed } from 'vue'

import { renderMarkdown } from '@/utils/markdown'

const props = withDefaults(
  defineProps<{
    source?: string | null
    emptyText?: string
    compact?: boolean
  }>(),
  {
    source: '',
    emptyText: '暂无内容',
    compact: false,
  },
)

const html = computed(() => renderMarkdown(props.source))
</script>

<template>
  <!-- eslint-disable-next-line vue/no-v-html -->
  <div v-if="html" class="markdown-content" :class="{ compact }" v-html="html" />
  <div v-else class="text-medium-emphasis">{{ emptyText }}</div>
</template>

<style scoped>
.markdown-content {
  line-height: 1.8;
  word-break: break-word;
}

.markdown-content:deep(h1),
.markdown-content:deep(h2),
.markdown-content:deep(h3),
.markdown-content:deep(h4) {
  margin: 0.8em 0 0.4em;
  line-height: 1.35;
}

.markdown-content:deep(h1) {
  font-size: 1.5rem;
}

.markdown-content:deep(h2) {
  font-size: 1.25rem;
}

.markdown-content:deep(h3) {
  font-size: 1.05rem;
}

.markdown-content:deep(p),
.markdown-content:deep(ul),
.markdown-content:deep(ol),
.markdown-content:deep(blockquote),
.markdown-content:deep(pre),
.markdown-content:deep(table) {
  margin: 0.65em 0;
}

.markdown-content:deep(ul),
.markdown-content:deep(ol) {
  padding-left: 1.4rem;
}

.markdown-content:deep(blockquote) {
  padding: 0.75rem 1rem;
  border-left: 4px solid rgba(var(--v-theme-primary), 0.45);
  background: rgba(var(--v-theme-surface-variant), 0.22);
  border-radius: 0 12px 12px 0;
}

.markdown-content:deep(code) {
  padding: 0.12rem 0.36rem;
  border-radius: 6px;
  background: rgba(var(--v-theme-surface-variant), 0.3);
  font-size: 0.92em;
}

.markdown-content:deep(pre) {
  overflow: auto;
  padding: 1rem;
  border-radius: 14px;
  background: rgba(var(--v-theme-surface-variant), 0.28);
}

.markdown-content:deep(pre code) {
  padding: 0;
  background: transparent;
}

.markdown-content:deep(a) {
  color: rgb(var(--v-theme-primary));
  text-decoration: none;
}

.markdown-content:deep(a:hover) {
  text-decoration: underline;
}

.markdown-content:deep(hr) {
  margin: 1rem 0;
  border: 0;
  border-top: 1px solid rgba(var(--v-theme-on-surface), 0.12);
}

.compact {
  font-size: 0.95rem;
}

.compact:deep(p),
.compact:deep(ul),
.compact:deep(ol),
.compact:deep(blockquote),
.compact:deep(pre) {
  margin: 0.45em 0;
}
</style>
