<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import EmptyState from '@/components/EmptyState.vue'
import PageContainer from '@/components/PageContainer.vue'
import StatCard from '@/components/StatCard.vue'
import { getCharacterRuntimeState, getReaderKnownState } from '@/api/story-context'
import { getChapterState, getReaderRevealState } from '@/api/story-state'
import { useProjectWorkspace } from '@/composables/useProjectWorkspace'
import type {
  ChapterIncrementalStateView,
  CharacterRuntimeStateView,
  ReaderKnownStateView,
  ReaderRevealStateView,
} from '@/types'

const {
  projectStore,
  currentProjectId,
  activeChapterId,
  activeChapter,
  chapterOptions,
  selectChapter,
} = useProjectWorkspace()

const loading = ref(false)
const chapterState = ref<ChapterIncrementalStateView | null>(null)
const readerRevealState = ref<ReaderRevealStateView | null>(null)
const readerKnownState = ref<ReaderKnownStateView | null>(null)
const povRuntimeState = ref<CharacterRuntimeStateView | null>(null)

const stats = computed(() => [
  {
    title: '未解环',
    value: chapterState.value?.openLoops?.length ?? 0,
    subtitle: '当前章节状态里的未收束 loop 数量',
    icon: 'mdi-timeline-alert-outline',
  },
  {
    title: '已解环',
    value: chapterState.value?.resolvedLoops?.length ?? 0,
    subtitle: '当前章节已经明确收束的 loop 数量',
    icon: 'mdi-check-decagram-outline',
    color: 'secondary',
  },
  {
    title: '读者已知',
    value: readerKnownState.value?.knownFacts?.length ?? 0,
    subtitle: '当前读者已经可以合理知道的事实数',
    icon: 'mdi-book-open-variant-outline',
    color: 'accent',
  },
  {
    title: '未揭晓',
    value: readerRevealState.value?.unrevealed?.length ?? 0,
    subtitle: '仍然保留在系统侧但未揭晓的信息数',
    icon: 'mdi-eye-off-outline',
    color: 'warning',
  },
])

async function loadStateCenterData(projectId: number, chapterId: number | null) {
  if (!chapterId) {
    chapterState.value = null
    readerRevealState.value = null
    readerKnownState.value = null
    povRuntimeState.value = null
    return
  }

  loading.value = true
  try {
    const [stateResult, revealResult, knownResult] = await Promise.allSettled([
      getChapterState(projectId, chapterId),
      getReaderRevealState(projectId, chapterId),
      getReaderKnownState(projectId, chapterId),
    ])

    chapterState.value = stateResult.status === 'fulfilled' ? stateResult.value : null
    readerRevealState.value = revealResult.status === 'fulfilled' ? revealResult.value : null
    readerKnownState.value = knownResult.status === 'fulfilled' ? knownResult.value : null

    if (activeChapter.value?.mainPovCharacterId) {
      const runtimeResult = await getCharacterRuntimeState(projectId, activeChapter.value.mainPovCharacterId).catch(() => null)
      povRuntimeState.value = runtimeResult
    } else {
      povRuntimeState.value = null
    }
  } finally {
    loading.value = false
  }
}

watch(
  [currentProjectId, activeChapterId],
  async ([projectId, chapterId]) => {
    if (!projectId) {
      chapterState.value = null
      readerRevealState.value = null
      readerKnownState.value = null
      povRuntimeState.value = null
      return
    }
    await loadStateCenterData(projectId, chapterId)
  },
  { immediate: true },
)
</script>

<template>
  <PageContainer
    title="状态台"
    description="这里不是配置页，而是运行态观测页。先看当前章节状态，再看读者已知和角色运行时状态。"
  >
    <template #actions>
      <v-select
        v-if="chapterOptions.length"
        hide-details
        density="comfortable"
        variant="outlined"
        style="min-width: 280px"
        :items="chapterOptions"
        :model-value="activeChapterId"
        label="观察章节"
        @update:model-value="selectChapter"
      />
    </template>

    <div class="stats-grid">
      <StatCard
        v-for="item in stats"
        :key="item.title"
        :title="item.title"
        :value="item.value"
        :subtitle="item.subtitle"
        :icon="item.icon"
        :color="item.color"
      />
    </div>

    <EmptyState
      v-if="!projectStore.currentProject || !activeChapter"
      title="先选择一个有章节的项目"
      description="状态台只在当前章节上下文里有意义。先选项目，再选章节。"
      icon="mdi-chart-timeline-variant"
    />

    <div v-else class="panel-grid two-column">
      <v-card class="soft-panel">
        <v-card-title>章节状态</v-card-title>
        <v-card-text>
          <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-4" />
          <div class="text-body-1">
            {{ chapterState?.summary || '当前章节还没有稳定的章节级状态摘要。' }}
          </div>
          <v-divider class="my-4" />
          <div class="text-subtitle-2 font-weight-medium">未解环</div>
          <div class="chip-stack mt-3">
            <v-chip
              v-for="loop in chapterState?.openLoops || []"
              :key="`open-${loop}`"
              size="small"
              color="warning"
              variant="tonal"
            >
              {{ loop }}
            </v-chip>
            <span v-if="!chapterState?.openLoops?.length" class="text-body-2 text-medium-emphasis">
              当前没有新的 open loop。
            </span>
          </div>

          <div class="text-subtitle-2 font-weight-medium mt-5">已解环</div>
          <div class="chip-stack mt-3">
            <v-chip
              v-for="loop in chapterState?.resolvedLoops || []"
              :key="`resolved-${loop}`"
              size="small"
              color="secondary"
              variant="tonal"
            >
              {{ loop }}
            </v-chip>
            <span v-if="!chapterState?.resolvedLoops?.length" class="text-body-2 text-medium-emphasis">
              当前还没有新的 resolved loop。
            </span>
          </div>

          <div class="text-subtitle-2 font-weight-medium mt-5">活跃地点</div>
          <div class="chip-stack mt-3">
            <v-chip
              v-for="location in chapterState?.activeLocations || []"
              :key="`location-${location}`"
              size="small"
              variant="outlined"
            >
              {{ location }}
            </v-chip>
          </div>
        </v-card-text>
      </v-card>

      <v-card class="soft-panel">
        <v-card-title>读者揭晓与 POV 状态</v-card-title>
        <v-card-text>
          <div class="text-subtitle-2 font-weight-medium">读者已知</div>
          <div class="chip-stack mt-3">
            <v-chip
              v-for="fact in readerKnownState?.knownFacts || []"
              :key="`known-${fact}`"
              size="small"
              color="primary"
              variant="tonal"
            >
              {{ fact }}
            </v-chip>
          </div>

          <div class="text-subtitle-2 font-weight-medium mt-5">未揭晓</div>
          <div class="chip-stack mt-3">
            <v-chip
              v-for="fact in readerRevealState?.unrevealed || []"
              :key="`unrevealed-${fact}`"
              size="small"
              color="warning"
              variant="outlined"
            >
              {{ fact }}
            </v-chip>
          </div>

          <v-divider class="my-4" />

          <div class="text-subtitle-2 font-weight-medium">主 POV 运行时状态</div>
          <div class="text-h6 mt-2">
            {{ povRuntimeState?.characterName || activeChapter.mainPovCharacterName || '当前章节未设置 POV 人物' }}
          </div>
          <div class="text-body-2 text-medium-emphasis mt-2">
            情绪：{{ povRuntimeState?.emotionalState || '未收集' }} · 态度：{{ povRuntimeState?.attitudeSummary || '未收集' }}
          </div>
          <div class="chip-stack mt-3">
            <v-chip
              v-for="tag in povRuntimeState?.stateTags || []"
              :key="`tag-${tag}`"
              size="small"
              variant="outlined"
            >
              {{ tag }}
            </v-chip>
          </div>
        </v-card-text>
      </v-card>
    </div>
  </PageContainer>
</template>
