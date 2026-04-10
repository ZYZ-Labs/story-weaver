<script setup lang="ts">
import type { ChapterAnchorBundle, AIWritingGenerationTrace } from '@/types'

defineProps<{
  anchors?: ChapterAnchorBundle | null
  readerReveal?: AIWritingGenerationTrace['readerReveal'] | null
  loading?: boolean
  title?: string
}>()
</script>

<template>
  <v-card class="soft-panel">
    <v-card-title>{{ title || '章节锚点' }}</v-card-title>
    <v-card-text>
      <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-4" />

      <div v-if="anchors">
        <div v-if="anchors.chapterSummary" class="anchor-section">
          <div class="text-subtitle-2">章节 brief</div>
          <div class="text-body-2 mt-2">{{ anchors.chapterSummary }}</div>
        </div>

        <div v-if="anchors.mainPovCharacterName" class="anchor-section">
          <div class="text-subtitle-2">稳定 POV</div>
          <v-chip size="small" color="primary" variant="tonal" class="mt-2">
            {{ anchors.mainPovCharacterName }}
          </v-chip>
        </div>

        <div v-if="anchors.requiredCharacterNames?.length" class="anchor-section">
          <div class="text-subtitle-2">必出人物</div>
          <div class="d-flex flex-wrap ga-2 mt-2">
            <v-chip
              v-for="item in anchors.requiredCharacterNames"
              :key="item"
              size="small"
              color="secondary"
              variant="tonal"
            >
              {{ item }}
            </v-chip>
          </div>
        </div>

        <div v-if="anchors.storyBeatTitles?.length" class="anchor-section">
          <div class="text-subtitle-2">剧情锚点</div>
          <div class="d-flex flex-wrap ga-2 mt-2">
            <v-chip
              v-for="item in anchors.storyBeatTitles"
              :key="item"
              size="small"
              color="success"
              variant="tonal"
            >
              {{ item }}
            </v-chip>
          </div>
        </div>

        <div v-if="readerReveal?.revealTargets?.length" class="anchor-section">
          <div class="text-subtitle-2">本轮应揭晓</div>
          <ul class="anchor-list">
            <li v-for="item in readerReveal.revealTargets" :key="item">{{ item }}</li>
          </ul>
        </div>

        <div v-if="readerReveal?.forbiddenAssumptions?.length" class="anchor-section">
          <div class="text-subtitle-2">禁止默认前情</div>
          <ul class="anchor-list">
            <li v-for="item in readerReveal.forbiddenAssumptions" :key="item">{{ item }}</li>
          </ul>
        </div>
      </div>

      <div v-else class="text-medium-emphasis">
        当前章节的稳定锚点会显示在这里。
      </div>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.anchor-section + .anchor-section {
  margin-top: 16px;
}

.anchor-list {
  margin: 8px 0 0;
  padding-left: 18px;
}

.anchor-list li + li {
  margin-top: 6px;
}
</style>
