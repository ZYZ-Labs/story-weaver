<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import EmptyState from '@/components/EmptyState.vue'
import PageContainer from '@/components/PageContainer.vue'
import StatCard from '@/components/StatCard.vue'
import { getCharacterRuntimeState, getReaderKnownState } from '@/api/story-context'
import {
  executeChapterBackfill,
  getChapterBackfillAnalysis,
  getChapterConsistencyCheck,
  getChapterCompatibilitySnapshot,
  getChapterBackfillDryRun,
  getChapterState,
  getProjectBackfillDryRun,
  getProjectBackfillOverview,
  getReaderRevealState,
} from '@/api/story-state'
import { useProjectWorkspace } from '@/composables/useProjectWorkspace'
import type {
  ChapterIncrementalStateView,
  CompatibilityModeView,
  LegacyProjectBackfillDryRunView,
  LegacyProjectBackfillOverviewView,
  MigrationCompatibilitySnapshotView,
  LegacyBackfillExecutionResultView,
  CharacterRuntimeStateView,
  LegacyBackfillDryRunView,
  LegacyChapterBackfillAnalysisView,
  ReaderKnownStateView,
  ReaderRevealStateView,
  StoryConsistencyCheckView,
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
const backfillAnalysis = ref<LegacyChapterBackfillAnalysisView | null>(null)
const backfillDryRun = ref<LegacyBackfillDryRunView | null>(null)
const backfillExecutionResult = ref<LegacyBackfillExecutionResultView | null>(null)
const compatibilitySnapshot = ref<MigrationCompatibilitySnapshotView | null>(null)
const projectBackfillOverview = ref<LegacyProjectBackfillOverviewView | null>(null)
const projectBackfillDryRun = ref<LegacyProjectBackfillDryRunView | null>(null)
const consistencyCheck = ref<StoryConsistencyCheckView | null>(null)
const backfillRunning = ref(false)
const backfillMessage = ref('')
const backfillError = ref('')

const compatibilityFlagMap = computed(() =>
  Object.fromEntries(
    (compatibilitySnapshot.value?.featureFlags || []).map((flag) => {
      const [key, value] = flag.split('=')
      return [key, value]
    }),
  ),
)

const backfillExecuteEnabled = computed(() => compatibilityFlagMap.value.backfillExecuteEnabled !== 'false')
const hasPhase9ChapterData = computed(
  () => !!(backfillAnalysis.value || backfillDryRun.value || compatibilitySnapshot.value),
)

const recommendedMigrationChapters = computed(() =>
  (projectBackfillOverview.value?.chapters || [])
    .filter((item) => item.needsSceneBackfill || item.needsStateBackfill)
    .slice(0, 4)
    .map((item) => ({
      id: item.chapterId,
      title: item.chapterTitle,
    })),
)

const phase9Checklist = computed(() => [
  {
    label: '章节级兼容分析',
    passed: !!backfillAnalysis.value,
  },
  {
    label: '章节级 dry-run',
    passed: !!backfillDryRun.value,
  },
  {
    label: '兼容边界快照',
    passed: !!compatibilitySnapshot.value,
  },
  {
    label: '项目级迁移总览',
    passed: !!projectBackfillOverview.value,
  },
  {
    label: '项目级 dry-run',
    passed: !!projectBackfillDryRun.value,
  },
])

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
      backfillAnalysis.value = null
      backfillDryRun.value = null
      backfillExecutionResult.value = null
      compatibilitySnapshot.value = null
      projectBackfillOverview.value = null
      projectBackfillDryRun.value = null
      consistencyCheck.value = null
      return
  }

  loading.value = true
  try {
    const [stateResult, revealResult, knownResult, analysisResult, dryRunResult, compatibilityResult, overviewResult, projectDryRunResult, consistencyResult] = await Promise.allSettled([
      getChapterState(projectId, chapterId),
      getReaderRevealState(projectId, chapterId),
      getReaderKnownState(projectId, chapterId),
      getChapterBackfillAnalysis(projectId, chapterId),
      getChapterBackfillDryRun(projectId, chapterId),
      getChapterCompatibilitySnapshot(projectId, chapterId),
      getProjectBackfillOverview(projectId),
      getProjectBackfillDryRun(projectId),
      getChapterConsistencyCheck(projectId, chapterId),
    ])

    chapterState.value = stateResult.status === 'fulfilled' ? stateResult.value : null
    readerRevealState.value = revealResult.status === 'fulfilled' ? revealResult.value : null
    readerKnownState.value = knownResult.status === 'fulfilled' ? knownResult.value : null
    backfillAnalysis.value = analysisResult.status === 'fulfilled' ? analysisResult.value : null
    backfillDryRun.value = dryRunResult.status === 'fulfilled' ? dryRunResult.value : null
    compatibilitySnapshot.value = compatibilityResult.status === 'fulfilled' ? compatibilityResult.value : null
    projectBackfillOverview.value = overviewResult.status === 'fulfilled' ? overviewResult.value : null
    projectBackfillDryRun.value = projectDryRunResult.status === 'fulfilled' ? projectDryRunResult.value : null
    consistencyCheck.value = consistencyResult.status === 'fulfilled' ? consistencyResult.value : null

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
      backfillAnalysis.value = null
      backfillDryRun.value = null
      backfillExecutionResult.value = null
      compatibilitySnapshot.value = null
      projectBackfillOverview.value = null
      projectBackfillDryRun.value = null
      consistencyCheck.value = null
      return
    }
    await loadStateCenterData(projectId, chapterId)
  },
  { immediate: true },
)

async function runBackfill() {
  if (!currentProjectId.value || !activeChapterId.value || backfillRunning.value) {
    return
  }
  backfillRunning.value = true
  backfillMessage.value = ''
  backfillError.value = ''
  try {
    backfillExecutionResult.value = await executeChapterBackfill(currentProjectId.value, activeChapterId.value)
    backfillMessage.value = backfillExecutionResult.value.executed
      ? '兼容回填执行完成，状态台已刷新为最新结果。'
      : '当前章节暂不可执行兼容回填。'
    await loadStateCenterData(currentProjectId.value, activeChapterId.value)
  } catch (error) {
    backfillError.value = error instanceof Error ? error.message : '兼容回填执行失败'
  } finally {
    backfillRunning.value = false
  }
}

async function jumpToRecommendedChapter(chapterId: number) {
  await selectChapter(chapterId)
}

function compatibilityModeColor(mode: CompatibilityModeView) {
  switch (mode) {
    case 'NEW_PRIMARY':
      return 'primary'
    case 'DUAL_READ':
      return 'info'
    case 'DUAL_WRITE':
      return 'secondary'
    case 'LEGACY_PRIMARY':
      return 'warning'
    case 'LEGACY_FALLBACK':
      return 'orange'
    default:
      return 'default'
  }
}

function compatibilityModeLabel(mode: CompatibilityModeView) {
  switch (mode) {
    case 'NEW_PRIMARY':
      return '新主链'
    case 'DUAL_READ':
      return '双读'
    case 'DUAL_WRITE':
      return '双写'
    case 'LEGACY_PRIMARY':
      return '旧链主源'
    case 'LEGACY_FALLBACK':
      return '旧链兜底'
    default:
      return '关闭'
  }
}

function consistencySeverityColor(severity: 'INFO' | 'WARNING' | 'ERROR') {
  switch (severity) {
    case 'ERROR':
      return 'error'
    case 'WARNING':
      return 'warning'
    default:
      return 'info'
  }
}
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

    <v-card v-if="projectStore.currentProject && activeChapter" class="soft-panel mt-6">
      <v-card-title>Phase 9 验收提示</v-card-title>
      <v-card-text>
        <div class="text-body-2 text-medium-emphasis">
          这块只服务迁移验收。先确认当前章节有没有 `Phase 9` 数据，再决定要不要切到推荐样本。
        </div>

        <div class="chip-stack mt-4">
          <v-chip
            :color="hasPhase9ChapterData ? 'primary' : 'warning'"
            variant="tonal"
            size="small"
          >
            当前章节：{{ hasPhase9ChapterData ? '有迁移数据' : '暂无迁移数据' }}
          </v-chip>
          <v-chip size="small" color="secondary" variant="outlined">
            当前章节 #{{ activeChapter.id }} · {{ activeChapter.title }}
          </v-chip>
        </div>

        <div class="text-subtitle-2 font-weight-medium mt-5">推荐联调样本</div>
        <div class="chip-stack mt-3">
          <v-btn
            v-for="chapter in recommendedMigrationChapters"
            :key="`recommended-btn-${chapter.id}`"
            size="small"
            variant="outlined"
            color="primary"
            @click="jumpToRecommendedChapter(chapter.id)"
          >
            #{{ chapter.id }} · {{ chapter.title }}
          </v-btn>
          <span v-if="!recommendedMigrationChapters.length" class="text-body-2 text-medium-emphasis">
            当前没有可推荐的迁移样本章节。
          </span>
        </div>

        <div class="text-subtitle-2 font-weight-medium mt-5">当前章节验收清单</div>
        <div class="chip-stack mt-3">
          <v-chip
            v-for="item in phase9Checklist"
            :key="item.label"
            size="small"
            :color="item.passed ? 'secondary' : 'warning'"
            variant="tonal"
          >
            {{ item.label }} · {{ item.passed ? '已返回' : '未返回' }}
          </v-chip>
        </div>
      </v-card-text>
    </v-card>

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

    <v-card v-if="activeChapter" class="soft-panel mt-6">
      <v-card-title>迁移兼容分析</v-card-title>
      <v-card-text>
        <div class="text-body-2 text-medium-emphasis">
          这块服务 `Phase 9`。它不是作者主工作流，而是告诉你：当前章节的旧记录和新状态链之间还差多少。
        </div>

        <div class="chip-stack mt-4">
          <v-chip size="small" color="primary" variant="tonal">
            当前章节 #{{ activeChapter.id }} · {{ activeChapter.title }}
          </v-chip>
          <v-chip
            v-for="chapter in recommendedMigrationChapters"
            :key="`recommended-${chapter.id}`"
            size="small"
            color="secondary"
            variant="outlined"
          >
            推荐样本 #{{ chapter.id }} · {{ chapter.title }}
          </v-chip>
        </div>

        <v-alert
          v-if="!backfillAnalysis && !backfillDryRun && !compatibilitySnapshot"
          type="info"
          variant="tonal"
          class="mt-4"
        >
          当前章节暂时没有可展示的 `Phase 9` 迁移兼容数据。优先切到推荐样本章节再看状态台。
        </v-alert>

        <v-divider class="my-4" />

        <div class="chip-stack">
          <v-chip size="small" color="primary" variant="tonal">
            旧记录 {{ backfillAnalysis?.legacyRecordCount ?? 0 }}
          </v-chip>
          <v-chip size="small" color="secondary" variant="tonal">
            新 scene {{ backfillAnalysis?.derivedSceneCount ?? 0 }}
          </v-chip>
          <v-chip size="small" color="accent" variant="tonal">
            event {{ backfillAnalysis?.eventCount ?? 0 }}
          </v-chip>
          <v-chip size="small" color="info" variant="tonal">
            snapshot {{ backfillAnalysis?.snapshotCount ?? 0 }}
          </v-chip>
          <v-chip size="small" color="warning" variant="tonal">
            patch {{ backfillAnalysis?.patchCount ?? 0 }}
          </v-chip>
        </div>

        <div class="text-subtitle-2 font-weight-medium mt-5">回填判定</div>
        <div class="chip-stack mt-3">
          <v-chip
            :color="backfillAnalysis?.needsSceneBackfill ? 'warning' : 'secondary'"
            variant="tonal"
            size="small"
          >
            scene 回填：{{ backfillAnalysis?.needsSceneBackfill ? '需要' : '不需要' }}
          </v-chip>
          <v-chip
            :color="backfillAnalysis?.needsStateBackfill ? 'warning' : 'secondary'"
            variant="tonal"
            size="small"
          >
            state 回填：{{ backfillAnalysis?.needsStateBackfill ? '需要' : '不需要' }}
          </v-chip>
          <v-chip
            :color="backfillDryRun?.canRunBackfill ? 'primary' : 'default'"
            variant="outlined"
            size="small"
          >
            dry-run：{{ backfillDryRun?.canRunBackfill ? '可执行' : '暂不可执行' }}
          </v-chip>
        </div>

        <div class="text-subtitle-2 font-weight-medium mt-5">建议动作</div>
        <v-list lines="two" density="compact">
          <v-list-item
            v-for="action in backfillDryRun?.actions || []"
            :key="action.actionKey"
            :title="action.title"
            :subtitle="action.blocked ? `${action.description} · 阻塞：${action.blockReason}` : action.description"
          >
            <template #append>
              <div class="d-flex ga-2 align-center">
                <v-chip size="x-small" :color="action.required ? 'warning' : 'default'" variant="tonal">
                  {{ action.required ? '必需' : '可选' }}
                </v-chip>
                <v-chip size="x-small" :color="action.blocked ? 'error' : 'secondary'" variant="outlined">
                  {{ action.blocked ? '阻塞' : '可执行' }}
                </v-chip>
              </div>
            </template>
          </v-list-item>
          <div v-if="!backfillDryRun?.actions?.length" class="text-body-2 text-medium-emphasis mt-3">
            当前还没有可展示的回填 dry-run 动作。
          </div>
        </v-list>

        <div class="text-subtitle-2 font-weight-medium mt-5">兼容风险</div>
        <div class="chip-stack mt-3">
          <v-chip
            v-for="note in backfillDryRun?.riskNotes || backfillAnalysis?.notes || []"
            :key="note"
            size="small"
            color="warning"
            variant="outlined"
          >
            {{ note }}
          </v-chip>
          <span
            v-if="!(backfillDryRun?.riskNotes?.length || backfillAnalysis?.notes?.length)"
            class="text-body-2 text-medium-emphasis"
          >
            当前没有额外兼容风险提示。
          </span>
        </div>

        <v-divider class="my-4" />

        <div class="d-flex flex-wrap ga-3 align-center">
          <v-btn
            color="primary"
            prepend-icon="mdi-database-refresh-outline"
            :loading="backfillRunning"
            :disabled="!backfillDryRun?.canRunBackfill || !backfillExecuteEnabled"
            @click="runBackfill"
          >
            执行兼容回填
          </v-btn>
          <div class="text-body-2 text-medium-emphasis">
            当前阶段只补缺失基线，不覆盖已有 reveal/state，不触碰正文。
            <span v-if="!backfillExecuteEnabled"> 当前环境已关闭回填执行开关。</span>
          </div>
        </div>

        <v-alert v-if="backfillMessage" type="success" variant="tonal" class="mt-4">
          {{ backfillMessage }}
        </v-alert>
        <v-alert v-if="backfillError" type="error" variant="tonal" class="mt-4">
          {{ backfillError }}
        </v-alert>

        <div v-if="backfillExecutionResult" class="mt-5">
          <div class="text-subtitle-2 font-weight-medium">最近一次执行结果</div>
          <div class="chip-stack mt-3">
            <v-chip size="small" color="primary" variant="tonal">
              event +{{ backfillExecutionResult.createdEventCount }}
            </v-chip>
            <v-chip size="small" color="secondary" variant="tonal">
              snapshot +{{ backfillExecutionResult.createdSnapshotCount }}
            </v-chip>
            <v-chip size="small" color="warning" variant="tonal">
              patch +{{ backfillExecutionResult.createdPatchCount }}
            </v-chip>
          </div>
          <div class="text-body-2 text-medium-emphasis mt-3">
            写入 {{ backfillExecutionResult.writtenKeys.length }} 项，跳过 {{ backfillExecutionResult.skippedKeys.length }} 项。
          </div>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-if="projectBackfillOverview" class="soft-panel mt-6">
      <v-card-title>项目级迁移总览</v-card-title>
      <v-card-text>
        <div class="text-body-2 text-medium-emphasis">
          这块是 `Phase 9.4` 的项目验收入口。先看整项目哪些章节仍需回填，再决定是不是逐章执行兼容补基线。
        </div>

        <div class="chip-stack mt-4">
          <v-chip size="small" color="primary" variant="tonal">
            总章节 {{ projectBackfillOverview.totalChapters }}
          </v-chip>
          <v-chip size="small" color="secondary" variant="tonal">
            已分析 {{ projectBackfillOverview.analyzedChapters }}
          </v-chip>
          <v-chip size="small" color="warning" variant="tonal">
            scene 回填 {{ projectBackfillOverview.chaptersNeedingSceneBackfill }}
          </v-chip>
          <v-chip size="small" color="warning" variant="outlined">
            state 回填 {{ projectBackfillOverview.chaptersNeedingStateBackfill }}
          </v-chip>
          <v-chip size="small" color="accent" variant="tonal">
            可执行 {{ projectBackfillOverview.chaptersReadyForBackfill }}
          </v-chip>
        </div>

        <v-list lines="three" density="compact" class="mt-4">
          <v-list-item
            v-for="item in projectBackfillOverview.chapters.slice(0, 6)"
            :key="item.chapterId"
            :title="item.chapterTitle"
            :subtitle="`旧正文记录 ${item.legacyGeneratedRecordCount} · scene 回填 ${item.needsSceneBackfill ? '需要' : '无'} · state 回填 ${item.needsStateBackfill ? '需要' : '无'}`"
          >
            <template #append>
              <v-chip size="x-small" :color="item.canRunBackfill ? 'primary' : 'default'" variant="tonal">
                {{ item.canRunBackfill ? '可回填' : '观察中' }}
              </v-chip>
            </template>
          </v-list-item>
        </v-list>
      </v-card-text>
    </v-card>

    <v-card v-if="projectBackfillDryRun" class="soft-panel mt-6">
      <v-card-title>项目级 dry-run 计划</v-card-title>
      <v-card-text>
        <div class="text-body-2 text-medium-emphasis">
          这块把“整项目接下来怎么迁”说清楚。它仍然只读，不会直接批量写入，只告诉你哪些章节可跑、哪些还被阻塞。
        </div>

        <div class="chip-stack mt-4">
          <v-chip size="small" color="primary" variant="tonal">
            需回填 {{ projectBackfillDryRun.chaptersNeedingBackfill }}
          </v-chip>
          <v-chip size="small" color="secondary" variant="tonal">
            可执行 {{ projectBackfillDryRun.runnableChapters }}
          </v-chip>
          <v-chip size="small" color="warning" variant="tonal">
            阻塞 {{ projectBackfillDryRun.blockedChapters }}
          </v-chip>
        </div>

        <div class="text-subtitle-2 font-weight-medium mt-5">项目级建议动作</div>
        <div class="chip-stack mt-3">
          <v-chip
            v-for="action in projectBackfillDryRun.requiredActions"
            :key="action"
            size="small"
            color="primary"
            variant="outlined"
          >
            {{ action }}
          </v-chip>
          <span v-if="!projectBackfillDryRun.requiredActions.length" class="text-body-2 text-medium-emphasis">
            当前没有新的项目级必需动作。
          </span>
        </div>

        <div class="text-subtitle-2 font-weight-medium mt-5">章节清单</div>
        <v-list lines="three" density="compact" class="mt-3">
          <v-list-item
            v-for="item in projectBackfillDryRun.chapters.slice(0, 6)"
            :key="item.chapterId"
            :title="item.chapterTitle"
            :subtitle="`scene 回填 ${item.needsSceneBackfill ? '需要' : '无'} · state 回填 ${item.needsStateBackfill ? '需要' : '无'} · 动作 ${item.actions.length}`"
          >
            <template #append>
              <v-chip size="x-small" :color="item.canRunBackfill ? 'primary' : 'warning'" variant="tonal">
                {{ item.canRunBackfill ? '可执行' : '阻塞' }}
              </v-chip>
            </template>
          </v-list-item>
        </v-list>

        <div class="text-subtitle-2 font-weight-medium mt-5">dry-run 风险</div>
        <div class="chip-stack mt-3">
          <v-chip
            v-for="note in projectBackfillDryRun.riskNotes"
            :key="note"
            size="small"
            color="warning"
            variant="tonal"
          >
            {{ note }}
          </v-chip>
          <span v-if="!projectBackfillDryRun.riskNotes.length" class="text-body-2 text-medium-emphasis">
            当前没有额外项目级 dry-run 风险提示。
          </span>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-if="consistencyCheck" class="soft-panel mt-6">
      <v-card-title>状态一致性检查</v-card-title>
      <v-card-text>
        <div class="text-body-2 text-medium-emphasis">
          这块是 `Phase 10.2` 的最小诊断入口。它不替代 chapter review，而是把 scene、handoff、event、snapshot、patch 和章节级状态放到同一张检查单里。
        </div>

        <div class="chip-stack mt-4">
          <v-chip size="small" color="primary" variant="tonal">
            scene {{ consistencyCheck.sceneCount }}
          </v-chip>
          <v-chip size="small" color="secondary" variant="tonal">
            completed {{ consistencyCheck.completedSceneCount }}
          </v-chip>
          <v-chip size="small" color="accent" variant="tonal">
            handoff {{ consistencyCheck.handoffCount }}
          </v-chip>
          <v-chip size="small" color="info" variant="tonal">
            event {{ consistencyCheck.eventCount }}
          </v-chip>
          <v-chip size="small" color="info" variant="outlined">
            snapshot {{ consistencyCheck.snapshotCount }}
          </v-chip>
          <v-chip size="small" color="warning" variant="tonal">
            patch {{ consistencyCheck.patchCount }}
          </v-chip>
        </div>

        <div class="chip-stack mt-4">
          <v-chip size="small" :color="consistencyCheck.hasReaderRevealState ? 'secondary' : 'warning'" variant="tonal">
            ReaderReveal {{ consistencyCheck.hasReaderRevealState ? '已建立' : '缺失' }}
          </v-chip>
          <v-chip size="small" :color="consistencyCheck.hasChapterState ? 'secondary' : 'warning'" variant="tonal">
            ChapterState {{ consistencyCheck.hasChapterState ? '已建立' : '缺失' }}
          </v-chip>
          <v-chip
            size="small"
            :color="consistencyCheck.chapterReviewPresent ? 'primary' : 'default'"
            variant="outlined"
          >
            chapter review {{ consistencyCheck.chapterReviewPresent ? consistencyCheck.chapterReviewResult || '已返回' : '未返回' }}
          </v-chip>
        </div>

        <div v-if="consistencyCheck.chapterReviewSummary" class="text-body-2 text-medium-emphasis mt-4">
          {{ consistencyCheck.chapterReviewSummary }}
        </div>

        <div class="text-subtitle-2 font-weight-medium mt-5">一致性问题</div>
        <div class="chip-stack mt-3">
          <v-chip
            v-for="issue in consistencyCheck.issues"
            :key="issue.issueKey"
            size="small"
            :color="consistencySeverityColor(issue.severity)"
            variant="tonal"
          >
            {{ issue.issueKey }} · {{ issue.message }}
          </v-chip>
          <span v-if="!consistencyCheck.issues.length" class="text-body-2 text-medium-emphasis">
            当前没有额外一致性问题。
          </span>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-if="compatibilitySnapshot" class="soft-panel mt-6">
      <v-card-title>灰度边界与开关</v-card-title>
      <v-card-text>
        <div class="text-body-2 text-medium-emphasis">
          这块解决的是“当前到底走哪条链”。页面、API、数据边界在这里明确显示，后面切换不会再靠猜。
        </div>

        <v-divider class="my-4" />

        <div class="text-subtitle-2 font-weight-medium">Feature Flags</div>
        <div class="chip-stack mt-3">
          <v-chip
            v-for="flag in compatibilitySnapshot.featureFlags"
            :key="flag"
            size="small"
            color="primary"
            variant="outlined"
          >
            {{ flag }}
          </v-chip>
        </div>

        <div class="text-subtitle-2 font-weight-medium mt-5">兼容风险</div>
        <div class="chip-stack mt-3">
          <v-chip
            v-for="note in compatibilitySnapshot.riskNotes"
            :key="note"
            size="small"
            color="warning"
            variant="tonal"
          >
            {{ note }}
          </v-chip>
          <span v-if="!compatibilitySnapshot.riskNotes.length" class="text-body-2 text-medium-emphasis">
            当前没有额外灰度风险提示。
          </span>
        </div>

        <div class="panel-grid three-column mt-5">
          <v-card class="soft-panel h-100">
            <v-card-title class="text-subtitle-1">页面边界</v-card-title>
            <v-card-text>
              <v-list lines="three" density="compact">
                <v-list-item
                  v-for="item in compatibilitySnapshot.pageBoundaries"
                  :key="item.boundaryKey"
                  :title="item.displayName"
                  :subtitle="`主链：${item.primaryChain}${item.fallbackChain ? ` · 兜底：${item.fallbackChain}` : ''}`"
                >
                  <template #append>
                    <v-chip size="x-small" :color="compatibilityModeColor(item.mode)" variant="tonal">
                      {{ compatibilityModeLabel(item.mode) }}
                    </v-chip>
                  </template>
                </v-list-item>
              </v-list>
            </v-card-text>
          </v-card>

          <v-card class="soft-panel h-100">
            <v-card-title class="text-subtitle-1">API 边界</v-card-title>
            <v-card-text>
              <v-list lines="three" density="compact">
                <v-list-item
                  v-for="item in compatibilitySnapshot.apiBoundaries"
                  :key="item.boundaryKey"
                  :title="item.displayName"
                  :subtitle="`主链：${item.primaryChain}${item.fallbackChain ? ` · 兜底：${item.fallbackChain}` : ''}`"
                >
                  <template #append>
                    <v-chip size="x-small" :color="compatibilityModeColor(item.mode)" variant="tonal">
                      {{ compatibilityModeLabel(item.mode) }}
                    </v-chip>
                  </template>
                </v-list-item>
              </v-list>
            </v-card-text>
          </v-card>

          <v-card class="soft-panel h-100">
            <v-card-title class="text-subtitle-1">数据边界</v-card-title>
            <v-card-text>
              <v-list lines="three" density="compact">
                <v-list-item
                  v-for="item in compatibilitySnapshot.dataBoundaries"
                  :key="item.boundaryKey"
                  :title="item.displayName"
                  :subtitle="`主链：${item.primaryChain}${item.fallbackChain ? ` · 兜底：${item.fallbackChain}` : ''}`"
                >
                  <template #append>
                    <v-chip size="x-small" :color="compatibilityModeColor(item.mode)" variant="tonal">
                      {{ compatibilityModeLabel(item.mode) }}
                    </v-chip>
                  </template>
                </v-list-item>
              </v-list>
            </v-card-text>
          </v-card>
        </div>
      </v-card-text>
    </v-card>
  </PageContainer>
</template>
