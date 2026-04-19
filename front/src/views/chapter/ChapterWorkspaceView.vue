<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import ChapterAnchorPanel from '@/components/ChapterAnchorPanel.vue'
import EmptyState from '@/components/EmptyState.vue'
import GenerationReadinessCard from '@/components/GenerationReadinessCard.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import PageContainer from '@/components/PageContainer.vue'
import StatCard from '@/components/StatCard.vue'
import { getChapterAnchors, getChapterGenerationReadiness } from '@/api/story-generation'
import {
  executeStorySession,
  getChapterExecutionReview,
  getChapterSkeletonPreview,
  getStorySessionPreview,
} from '@/api/story-orchestration'
import { getReaderKnownState } from '@/api/story-context'
import { getChapterState, getReaderRevealState } from '@/api/story-state'
import { useProjectWorkspace } from '@/composables/useProjectWorkspace'
import type {
  ChapterAnchorBundle,
  ChapterExecutionReviewView,
  ChapterIncrementalStateView,
  ChapterSkeletonView,
  GenerationReadiness,
  ReaderKnownStateView,
  ReaderRevealStateView,
  StorySessionExecutionView,
  StorySessionPreviewView,
} from '@/types'

const {
  projectStore,
  chapterStore,
  currentProjectId,
  activeChapterId,
  activeChapter,
  chapterOptions,
  selectChapter,
} = useProjectWorkspace()

const loading = ref(false)
const sceneLoading = ref(false)
const saveLoading = ref(false)
const executeLoading = ref(false)
const sceneId = ref('scene-1')
const editorContent = ref('')
const saveMessage = ref('')
const saveError = ref('')
const executionMessage = ref('')
const executionError = ref('')

const readiness = ref<GenerationReadiness | null>(null)
const anchors = ref<ChapterAnchorBundle | null>(null)
const skeleton = ref<ChapterSkeletonView | null>(null)
const review = ref<ChapterExecutionReviewView | null>(null)
const sessionPreview = ref<StorySessionPreviewView | null>(null)
const lastExecution = ref<StorySessionExecutionView | null>(null)
const chapterState = ref<ChapterIncrementalStateView | null>(null)
const readerRevealState = ref<ReaderRevealStateView | null>(null)
const readerKnownState = ref<ReaderKnownStateView | null>(null)

const sceneOptions = computed(() =>
  (skeleton.value?.scenes || []).map((scene) => ({
    title: `${scene.sceneId} · ${scene.goal || scene.status}`,
    value: scene.sceneId,
  })),
)

const stats = computed(() => [
  {
    title: '正文长度',
    value: editorContent.value.length,
    subtitle: '当前章节正文字符数',
    icon: 'mdi-text-box-outline',
  },
  {
    title: '骨架镜头',
    value: skeleton.value?.sceneCount ?? 0,
    subtitle: '当前章节骨架中的镜头总数',
    icon: 'mdi-file-tree-outline',
    color: 'secondary',
  },
  {
    title: '待推进镜头',
    value: review.value?.traceSummary?.pendingSceneCount ?? 0,
    subtitle: '当前章节仍未完成的镜头数',
    icon: 'mdi-timer-sand-empty',
    color: 'warning',
  },
  {
    title: '未解环',
    value: chapterState.value?.openLoops?.length ?? 0,
    subtitle: '章节状态里的开放回路数量',
    icon: 'mdi-chart-timeline-variant',
    color: 'accent',
  },
])

function resolveSceneId() {
  if (sceneOptions.value.some((item) => item.value === sceneId.value)) {
    return sceneId.value
  }
  return review.value?.traceSummary?.latestSceneId || skeleton.value?.scenes?.[0]?.sceneId || 'scene-1'
}

function updateSceneId(value: unknown) {
  sceneId.value = String(value || 'scene-1')
}

async function loadScenePreview(projectId: number, chapterId: number, targetSceneId: string) {
  sceneLoading.value = true
  try {
    sessionPreview.value = await getStorySessionPreview(projectId, chapterId, targetSceneId).catch(() => null)
  } finally {
    sceneLoading.value = false
  }
}

async function loadWorkspace(projectId: number, chapterId: number) {
  loading.value = true
  try {
    await chapterStore.fetchDetail(projectId, chapterId).catch(() => undefined)

    const [
      readinessResult,
      anchorsResult,
      skeletonResult,
      reviewResult,
      chapterStateResult,
      revealResult,
      knownResult,
    ] = await Promise.allSettled([
      getChapterGenerationReadiness(projectId, chapterId),
      getChapterAnchors(projectId, chapterId),
      getChapterSkeletonPreview(projectId, chapterId),
      getChapterExecutionReview(projectId, chapterId),
      getChapterState(projectId, chapterId),
      getReaderRevealState(projectId, chapterId),
      getReaderKnownState(projectId, chapterId),
    ])

    readiness.value = readinessResult.status === 'fulfilled' ? readinessResult.value : null
    anchors.value = anchorsResult.status === 'fulfilled' ? anchorsResult.value : null
    skeleton.value = skeletonResult.status === 'fulfilled' ? skeletonResult.value : null
    review.value = reviewResult.status === 'fulfilled' ? reviewResult.value : null
    chapterState.value = chapterStateResult.status === 'fulfilled' ? chapterStateResult.value : null
    readerRevealState.value = revealResult.status === 'fulfilled' ? revealResult.value : null
    readerKnownState.value = knownResult.status === 'fulfilled' ? knownResult.value : null

    sceneId.value = resolveSceneId()
    await loadScenePreview(projectId, chapterId, sceneId.value)
  } finally {
    loading.value = false
  }
}

async function saveContent() {
  if (!currentProjectId.value || !activeChapterId.value) {
    return
  }
  saveLoading.value = true
  saveMessage.value = ''
  saveError.value = ''
  try {
    await chapterStore.update(currentProjectId.value, activeChapterId.value, {
      content: editorContent.value,
    })
    saveMessage.value = '正文已保存到当前章节。'
  } catch (error) {
    saveError.value = error instanceof Error ? error.message : '保存正文失败'
  } finally {
    saveLoading.value = false
  }
}

async function executeCurrentScene() {
  if (!currentProjectId.value || !activeChapterId.value) {
    return
  }
  executeLoading.value = true
  executionMessage.value = ''
  executionError.value = ''
  try {
    const execution = await executeStorySession(currentProjectId.value, activeChapterId.value, sceneId.value)
    lastExecution.value = execution
    executionMessage.value = `已执行 ${execution.writeResult.sceneExecutionState.sceneId}，下一镜头 ${execution.writeResult.handoffSnapshot.toSceneId || '待定'}。`
    await loadWorkspace(currentProjectId.value, activeChapterId.value)
    if (execution.writeResult.handoffSnapshot.toSceneId) {
      sceneId.value = execution.writeResult.handoffSnapshot.toSceneId
    }
  } catch (error) {
    executionError.value = error instanceof Error ? error.message : '执行镜头失败'
  } finally {
    executeLoading.value = false
  }
}

watch(
  [currentProjectId, activeChapterId],
  async ([projectId, chapterId]) => {
    if (!projectId || !chapterId) {
      readiness.value = null
      anchors.value = null
      skeleton.value = null
      review.value = null
      sessionPreview.value = null
      chapterState.value = null
      readerRevealState.value = null
      readerKnownState.value = null
      return
    }
    await loadWorkspace(projectId, chapterId)
  },
  { immediate: true },
)

watch(
  () => activeChapter.value?.content,
  (value) => {
    editorContent.value = value || ''
  },
  { immediate: true },
)

watch(sceneId, async (value) => {
  if (!currentProjectId.value || !activeChapterId.value || !value) {
    return
  }
  await loadScenePreview(currentProjectId.value, activeChapterId.value, value)
})
</script>

<template>
  <PageContainer
    title="章节工作区"
    description="把正文、骨架、镜头、状态和 trace 放到一个上下文里。这里先服务当前章节动作，而不是继续在多个旧页面之间跳转。"
  >
    <template #actions>
      <div class="chapter-workspace-actions">
        <v-select
          v-if="chapterOptions.length"
          class="chapter-workspace-actions__chapter"
          hide-details
          density="comfortable"
          variant="outlined"
          :items="chapterOptions"
          :model-value="activeChapterId"
          label="当前章节"
          @update:model-value="selectChapter"
        />
        <v-select
          class="chapter-workspace-actions__scene"
          hide-details
          density="comfortable"
          variant="outlined"
          :items="sceneOptions"
          :model-value="sceneId"
          label="当前镜头"
          @update:model-value="updateSceneId"
        />
        <v-btn
          color="primary"
          prepend-icon="mdi-play-circle-outline"
          :loading="executeLoading"
          :disabled="!activeChapterId"
          @click="executeCurrentScene"
        >
          执行当前镜头
        </v-btn>
        <v-btn variant="outlined" prepend-icon="mdi-feather" to="/writing">
          旧写作中心
        </v-btn>
      </div>
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
      title="先选择一个章节"
      description="章节工作区围绕当前章节工作。先选项目，再选章节。"
      icon="mdi-file-document-edit-outline"
    />

    <div v-else class="panel-grid two-column">
      <div class="chapter-workspace-stack">
        <v-card class="soft-panel">
          <v-card-title class="d-flex justify-space-between align-center ga-3">
            <span>{{ activeChapter.title }}</span>
            <v-chip color="secondary" variant="tonal">
              {{ activeChapter.chapterStatus || 'draft' }}
            </v-chip>
          </v-card-title>
          <v-card-text>
            <div class="text-body-2 text-medium-emphasis">
              顺序：{{ activeChapter.orderNum || '-' }} · POV：{{ activeChapter.mainPovCharacterName || '未指定' }} ·
              字数：{{ activeChapter.wordCount || 0 }}
            </div>
            <div class="text-body-1 mt-4">
              {{ activeChapter.summary || '当前章节还没有摘要，建议先通过摘要工作流补一版。' }}
            </div>
            <div class="chip-stack mt-4">
              <v-chip
                v-for="storyBeat in activeChapter.storyBeatTitles || []"
                :key="storyBeat"
                size="small"
                color="primary"
                variant="outlined"
              >
                节拍：{{ storyBeat }}
              </v-chip>
              <v-chip
                v-for="name in activeChapter.requiredCharacterNames || []"
                :key="name"
                size="small"
                color="secondary"
                variant="tonal"
              >
                必出：{{ name }}
              </v-chip>
            </div>
          </v-card-text>
        </v-card>

        <v-card class="soft-panel">
          <v-card-title class="d-flex justify-space-between align-center ga-3">
            <span>正文编辑</span>
            <v-btn
              color="primary"
              prepend-icon="mdi-content-save-outline"
              :loading="saveLoading"
              @click="saveContent"
            >
              保存正文
            </v-btn>
          </v-card-title>
          <v-card-text>
            <v-alert v-if="saveMessage" type="success" variant="tonal" class="mb-4">
              {{ saveMessage }}
            </v-alert>
            <v-alert v-if="saveError" type="error" variant="tonal" class="mb-4">
              {{ saveError }}
            </v-alert>
            <MarkdownEditor
              v-model="editorContent"
              label="章节正文"
              :rows="20"
              auto-grow
              preview-empty-text="当前还没有正文，可以先执行镜头或直接补写。"
            />
          </v-card-text>
        </v-card>

        <v-card class="soft-panel">
          <v-card-title>章节骨架与镜头</v-card-title>
          <v-card-text>
            <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-4" />
            <div v-if="skeleton?.scenes?.length" class="scene-list">
              <button
                v-for="scene in skeleton.scenes"
                :key="scene.sceneId"
                type="button"
                class="scene-list-item chapter-workspace-scene-button"
                :class="{ 'chapter-workspace-scene-button--active': scene.sceneId === sceneId }"
                @click="sceneId = scene.sceneId"
              >
                <div class="scene-list-item__header">
                  <div class="text-subtitle-1 font-weight-bold scene-list-item__title">
                    {{ scene.sceneId }}
                  </div>
                  <v-chip size="small" color="secondary" variant="tonal" class="scene-list-item__metric">
                    {{ scene.status }}
                  </v-chip>
                </div>
                <div class="text-body-2 mt-2">{{ scene.goal || '当前镜头还没有明确 goal。' }}</div>
                <div class="text-caption text-medium-emphasis mt-3">
                  停点：{{ scene.stopCondition || '未定义' }} · 目标字数：{{ scene.targetWords || 0 }}
                </div>
              </button>
            </div>
            <EmptyState
              v-else
              title="当前章节还没有骨架"
              description="先保证章节摘要和锚点完整，骨架会在这里显示。"
              icon="mdi-file-tree-outline"
            />
          </v-card-text>
        </v-card>
      </div>

      <div class="chapter-workspace-stack">
        <GenerationReadinessCard :readiness="readiness" :loading="loading" title="当前章节生成就绪度" />

        <ChapterAnchorPanel :anchors="anchors" :loading="loading" title="当前章节锚点" />

        <v-card class="soft-panel">
          <v-card-title>当前镜头执行</v-card-title>
          <v-card-text>
            <v-progress-linear v-if="sceneLoading" indeterminate color="primary" class="mb-4" />

            <div class="chapter-workspace-meta-row text-body-2 text-medium-emphasis">
              <span>当前 scene：</span>
              <span class="scene-token">{{ sessionPreview?.contextPacket?.sceneId || sceneId }}</span>
              <span>绑定模式：{{ sessionPreview?.contextPacket?.sceneBindingContext?.mode || '未绑定' }}</span>
            </div>
            <div class="chapter-workspace-meta-row text-body-2 text-medium-emphasis mt-2">
              <span>解析 scene：</span>
              <span class="scene-token">
                {{ sessionPreview?.contextPacket?.sceneBindingContext?.resolvedSceneId || '未解析' }}
              </span>
            </div>

            <v-alert v-if="executionMessage" type="success" variant="tonal" class="mt-4">
              {{ executionMessage }}
            </v-alert>
            <v-alert v-if="executionError" type="error" variant="tonal" class="mt-4">
              {{ executionError }}
            </v-alert>

            <v-divider class="my-4" />

            <div class="text-subtitle-2 font-weight-medium">当前写手 Brief</div>
            <div class="text-body-2 mt-2">
              {{ sessionPreview?.writerExecutionBrief?.goal || '当前镜头还没有写手目标。' }}
            </div>
            <div class="text-caption text-medium-emphasis mt-3">
              停点：{{ sessionPreview?.writerExecutionBrief?.stopCondition || '未定义' }} ·
              目标字数：{{ sessionPreview?.writerExecutionBrief?.targetWords || 0 }}
            </div>

            <div class="text-subtitle-2 font-weight-medium mt-5">最近执行结果</div>
            <div class="text-body-2 mt-2">
              {{
                lastExecution?.writeResult?.sceneExecutionState?.outcomeSummary ||
                sessionPreview?.writerSessionResult?.summary ||
                '当前还没有新的执行结果摘要。'
              }}
            </div>
            <div class="text-caption text-medium-emphasis mt-3">
              Handoff：{{ lastExecution?.writeResult?.handoffSnapshot?.toSceneId || '待执行后生成' }}
            </div>
          </v-card-text>
        </v-card>

        <v-card class="soft-panel">
          <v-card-title>章节状态与揭晓</v-card-title>
          <v-card-text>
            <div class="text-body-2">
              {{ chapterState?.summary || '当前还没有章节状态摘要。' }}
            </div>
            <div class="chip-stack mt-3">
              <v-chip
                v-for="loop in chapterState?.openLoops || []"
                :key="`open-${loop}`"
                size="small"
                color="warning"
                variant="tonal"
              >
                未解环 · {{ loop }}
              </v-chip>
              <v-chip
                v-for="loop in chapterState?.resolvedLoops || []"
                :key="`resolved-${loop}`"
                size="small"
                color="secondary"
                variant="tonal"
              >
                已解环 · {{ loop }}
              </v-chip>
            </div>
            <v-divider class="my-4" />
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
          </v-card-text>
        </v-card>

        <v-card class="soft-panel">
          <v-card-title>章节 Trace 与审校</v-card-title>
          <v-card-text>
            <div class="text-body-2">
              {{ review?.summary || '当前还没有章节级审校摘要。' }}
            </div>
            <div class="chip-stack mt-3">
              <v-chip color="primary" variant="tonal">
                已执行 {{ review?.traceSummary?.executedSceneCount ?? 0 }}
              </v-chip>
              <v-chip color="warning" variant="tonal">
                待推进 {{ review?.traceSummary?.pendingSceneCount ?? 0 }}
              </v-chip>
            </div>
            <v-list lines="two" class="mt-4">
              <v-list-item
                v-for="(item, index) in sessionPreview?.trace?.items || []"
                :key="`${item.sessionRole}-${index}`"
                :title="`${item.sessionRole} · ${item.status}`"
                :subtitle="item.message || '当前没有补充说明。'"
              >
                <template #append>
                  <v-chip size="x-small" variant="outlined">
                    #{{ item.attempt || 1 }}
                  </v-chip>
                </template>
              </v-list-item>
            </v-list>
          </v-card-text>
        </v-card>
      </div>
    </div>
  </PageContainer>
</template>

<style scoped>
.chapter-workspace-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  width: min(100%, 900px);
}

.chapter-workspace-actions__chapter {
  flex: 1 1 320px;
  min-width: 280px;
}

.chapter-workspace-actions__scene {
  flex: 0 0 220px;
  min-width: 180px;
}

.chapter-workspace-stack {
  display: grid;
  gap: 16px;
  align-content: start;
}

.chapter-workspace-scene-button {
  width: 100%;
  text-align: left;
  cursor: pointer;
}

.chapter-workspace-scene-button--active {
  border-color: rgba(30, 77, 120, 0.28);
  box-shadow: inset 0 0 0 1px rgba(30, 77, 120, 0.14);
}

.chapter-workspace-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.scene-token {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  padding: 2px 10px;
  border-radius: 999px;
  background: rgba(30, 77, 120, 0.08);
  color: #1e4d78;
  font-family: 'Roboto Mono', 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.scene-list-item__header {
  display: flex;
  gap: 12px;
  justify-content: space-between;
  align-items: flex-start;
}

.scene-list-item__title {
  min-width: 0;
  flex: 1 1 auto;
  white-space: normal;
  overflow-wrap: anywhere;
  font-family: 'Roboto Mono', 'SFMono-Regular', Consolas, monospace;
}

.scene-list-item__metric {
  flex: 0 0 auto;
}

@media (max-width: 960px) {
  .scene-list-item__header {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 720px) {
  .chapter-workspace-actions {
    width: 100%;
  }

  .chapter-workspace-actions__chapter,
  .chapter-workspace-actions__scene {
    flex: 1 1 100%;
    min-width: 0;
  }
}
</style>
