<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'

import ChapterAnchorPanel from '@/components/ChapterAnchorPanel.vue'
import EmptyState from '@/components/EmptyState.vue'
import GenerationReadinessCard from '@/components/GenerationReadinessCard.vue'
import MarkdownContent from '@/components/MarkdownContent.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import PageContainer from '@/components/PageContainer.vue'
import StatCard from '@/components/StatCard.vue'
import { acceptWriting, generateWriting, rejectWriting } from '@/api/ai-writing'
import { getChapterAnchors, getChapterGenerationReadiness } from '@/api/story-generation'
import {
  deleteChapterSkeletonScene,
  executeStorySession,
  getChapterExecutionReview,
  getChapterSkeletonPreview,
  getStorySessionPreview,
  updateChapterSkeletonScene,
} from '@/api/story-orchestration'
import { getReaderKnownState } from '@/api/story-context'
import { getChapterState, getReaderRevealState } from '@/api/story-state'
import { useProjectWorkspace } from '@/composables/useProjectWorkspace'
import type {
  AIWritingRecord,
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
const sceneMutationLoading = ref(false)
const draftLoading = ref(false)
const sceneId = ref('scene-1')
const editorContent = ref('')
const saveMessage = ref('')
const saveError = ref('')
const executionMessage = ref('')
const executionError = ref('')
const sceneMutationMessage = ref('')
const sceneMutationError = ref('')
const draftMessage = ref('')
const draftError = ref('')
const sceneDraftRecord = ref<AIWritingRecord | null>(null)
const sceneEditorVisible = ref(false)

const readiness = ref<GenerationReadiness | null>(null)
const anchors = ref<ChapterAnchorBundle | null>(null)
const skeleton = ref<ChapterSkeletonView | null>(null)
const review = ref<ChapterExecutionReviewView | null>(null)
const sessionPreview = ref<StorySessionPreviewView | null>(null)
const lastExecution = ref<StorySessionExecutionView | null>(null)
const chapterState = ref<ChapterIncrementalStateView | null>(null)
const readerRevealState = ref<ReaderRevealStateView | null>(null)
const readerKnownState = ref<ReaderKnownStateView | null>(null)

const sceneEditorForm = reactive({
  goal: '',
  readerRevealText: '',
  mustUseAnchorsText: '',
  stopCondition: '',
  targetWords: 900 as number | null,
})

const sceneOptions = computed(() =>
  (skeleton.value?.scenes || []).map((scene) => ({
    title: `${scene.sceneId} · ${scene.goal || scene.status}`,
    value: scene.sceneId,
  })),
)

const currentScene = computed(
  () => skeleton.value?.scenes?.find((scene) => scene.sceneId === sceneId.value) || null,
)

const currentSceneCompleted = computed(() => currentScene.value?.status === 'COMPLETED')

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

function openSceneEditor() {
  if (!currentScene.value) {
    return
  }
  sceneEditorForm.goal = currentScene.value.goal || ''
  sceneEditorForm.readerRevealText = (currentScene.value.readerReveal || []).join('\n')
  sceneEditorForm.mustUseAnchorsText = (currentScene.value.mustUseAnchors || []).join('\n')
  sceneEditorForm.stopCondition = currentScene.value.stopCondition || ''
  sceneEditorForm.targetWords = currentScene.value.targetWords ?? 900
  sceneMutationError.value = ''
  sceneMutationMessage.value = ''
  sceneEditorVisible.value = true
}

function parseMultilineList(value: string) {
  return value
    .split('\n')
    .map((item) => item.trim())
    .filter(Boolean)
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
  if (!window.confirm(`确认仅推进 ${sceneId.value} 的镜头状态吗？这会写入 runtime/handoff，但不会自动生成正文。`)) {
    return
  }
  executeLoading.value = true
  executionMessage.value = ''
  executionError.value = ''
  try {
    const execution = await executeStorySession(currentProjectId.value, activeChapterId.value, sceneId.value)
    lastExecution.value = execution
    executionMessage.value = `已仅推进 ${execution.writeResult.sceneExecutionState.sceneId} 的镜头状态，下一镜头 ${execution.writeResult.handoffSnapshot.toSceneId || '待定'}。`
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

async function saveSceneOverride() {
  if (!currentProjectId.value || !activeChapterId.value || !currentScene.value) {
    return
  }
  sceneMutationLoading.value = true
  sceneMutationError.value = ''
  sceneMutationMessage.value = ''
  try {
    await updateChapterSkeletonScene(currentProjectId.value, activeChapterId.value, currentScene.value.sceneId, {
      goal: sceneEditorForm.goal,
      readerReveal: parseMultilineList(sceneEditorForm.readerRevealText),
      mustUseAnchors: parseMultilineList(sceneEditorForm.mustUseAnchorsText),
      stopCondition: sceneEditorForm.stopCondition,
      targetWords: sceneEditorForm.targetWords,
    })
    sceneEditorVisible.value = false
    sceneMutationMessage.value = `已更新 ${currentScene.value.sceneId} 的骨架。`
    await loadWorkspace(currentProjectId.value, activeChapterId.value)
  } catch (error) {
    sceneMutationError.value = error instanceof Error ? error.message : '更新镜头失败'
  } finally {
    sceneMutationLoading.value = false
  }
}

async function deleteCurrentScene() {
  if (!currentProjectId.value || !activeChapterId.value || !currentScene.value) {
    return
  }
  const warning = currentSceneCompleted.value
    ? `确认删除 ${currentScene.value.sceneId} 吗？该镜头已执行，删除会清理其 runtime/handoff 状态，但不会自动回滚已经写入正文的内容。`
    : `确认删除 ${currentScene.value.sceneId} 吗？`
  if (!window.confirm(warning)) {
    return
  }
  sceneMutationLoading.value = true
  sceneMutationError.value = ''
  sceneMutationMessage.value = ''
  try {
    await deleteChapterSkeletonScene(currentProjectId.value, activeChapterId.value, currentScene.value.sceneId)
    sceneMutationMessage.value = `已删除 ${currentScene.value.sceneId}。`
    await loadWorkspace(currentProjectId.value, activeChapterId.value)
  } catch (error) {
    sceneMutationError.value = error instanceof Error ? error.message : '删除镜头失败'
  } finally {
    sceneMutationLoading.value = false
  }
}

async function generateSceneDraft() {
  if (!activeChapterId.value || !sessionPreview.value) {
    return
  }
  draftLoading.value = true
  draftError.value = ''
  draftMessage.value = ''
  sceneDraftRecord.value = null
  try {
    const brief = sessionPreview.value.writerExecutionBrief
    const continuityNotes = sessionPreview.value.contextPacket?.sceneBindingContext?.reason
    const instructionSections = [
      `请严格围绕当前镜头生成小说正文，不要越过镜头停点。`,
      `【镜头ID】${sceneId.value}`,
      `【镜头目标】${brief.goal || '未定义'}`,
      `【向读者揭晓】${(brief.readerReveal || []).join('；') || '未定义'}`,
      `【必须使用锚点】${(brief.mustUseAnchors || []).join('；') || '未定义'}`,
      `【收束点】${brief.stopCondition || '未定义'}`,
      continuityNotes ? `【承接说明】${continuityNotes}` : '',
    ].filter(Boolean)

    sceneDraftRecord.value = await generateWriting({
      chapterId: activeChapterId.value,
      currentContent: editorContent.value,
      userInstruction: instructionSections.join('\n'),
      writingType: editorContent.value.trim() ? 'continue' : 'draft',
      maxTokens: brief.targetWords || 900,
      entryPoint: 'phase8.chapter-workspace.scene-draft',
    })
    draftMessage.value = `已生成 ${sceneId.value} 的${editorContent.value.trim() ? '续写草稿' : '初稿'}。`
  } catch (error) {
    draftError.value = error instanceof Error ? error.message : '生成镜头草稿失败'
  } finally {
    draftLoading.value = false
  }
}

async function acceptSceneDraft() {
  if (!sceneDraftRecord.value || !currentProjectId.value || !activeChapterId.value) {
    return
  }
  draftLoading.value = true
  draftError.value = ''
  try {
    const acceptedSceneId = sceneId.value
    await acceptWriting(sceneDraftRecord.value.id)
    let executionTip = ''
    try {
      const execution = await executeStorySession(currentProjectId.value, activeChapterId.value, acceptedSceneId)
      lastExecution.value = execution
      executionTip = execution.writeResult.handoffSnapshot.toSceneId
        ? ` 已推进到 ${execution.writeResult.handoffSnapshot.toSceneId}。`
        : ''
      if (execution.writeResult.handoffSnapshot.toSceneId) {
        sceneId.value = execution.writeResult.handoffSnapshot.toSceneId
      }
    } catch (executionErrorValue) {
      executionError.value = executionErrorValue instanceof Error
        ? `正文已写回，但镜头状态推进失败：${executionErrorValue.message}`
        : '正文已写回，但镜头状态推进失败'
    }
    draftMessage.value = `已接受 ${acceptedSceneId} 的草稿并写回章节正文。${executionTip}`.trim()
    await loadWorkspace(currentProjectId.value, activeChapterId.value)
    sceneDraftRecord.value = null
  } catch (error) {
    draftError.value = error instanceof Error ? error.message : '接受草稿失败'
  } finally {
    draftLoading.value = false
  }
}

async function rejectSceneDraft() {
  if (!sceneDraftRecord.value) {
    return
  }
  draftLoading.value = true
  draftError.value = ''
  try {
    await rejectWriting(sceneDraftRecord.value.id)
    draftMessage.value = `已拒绝 ${sceneId.value} 的草稿。`
    sceneDraftRecord.value = null
  } catch (error) {
    draftError.value = error instanceof Error ? error.message : '拒绝草稿失败'
  } finally {
    draftLoading.value = false
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
          :disabled="!activeChapterId || !currentScene"
          @click="executeCurrentScene"
        >
          仅推进镜头状态
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
          <v-card-title class="d-flex justify-space-between align-center ga-3">
            <span>章节骨架与镜头</span>
            <div class="d-flex ga-2 flex-wrap">
              <v-btn
                variant="outlined"
                prepend-icon="mdi-pencil-outline"
                :disabled="!currentScene"
                @click="openSceneEditor"
              >
                编辑当前镜头
              </v-btn>
              <v-btn
                color="error"
                variant="tonal"
                prepend-icon="mdi-delete-outline"
                :disabled="!currentScene"
                :loading="sceneMutationLoading"
                @click="deleteCurrentScene"
              >
                删除当前镜头
              </v-btn>
            </div>
          </v-card-title>
          <v-card-text>
            <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-4" />
            <v-alert v-if="sceneMutationMessage" type="success" variant="tonal" class="mb-4">
              {{ sceneMutationMessage }}
            </v-alert>
            <v-alert v-if="sceneMutationError" type="error" variant="tonal" class="mb-4">
              {{ sceneMutationError }}
            </v-alert>
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
          <v-card-title class="d-flex justify-space-between align-center ga-3">
            <span>当前镜头执行</span>
            <v-btn
              color="secondary"
              prepend-icon="mdi-auto-fix"
              :loading="draftLoading"
              :disabled="!sessionPreview"
              @click="generateSceneDraft"
            >
              {{ editorContent.trim() ? '根据当前镜头继续生成' : '根据当前镜头生成初稿' }}
            </v-btn>
          </v-card-title>
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
            <div class="text-caption text-medium-emphasis mt-3">
              普通主路径：先根据当前镜头生成草稿，接受草稿后再自动推进镜头状态与 handoff。上方按钮只用于手动推进状态，不会生成正文。
            </div>

            <v-alert v-if="executionMessage" type="success" variant="tonal" class="mt-4">
              {{ executionMessage }}
            </v-alert>
            <v-alert v-if="executionError" type="error" variant="tonal" class="mt-4">
              {{ executionError }}
            </v-alert>
            <v-alert v-if="draftMessage" type="success" variant="tonal" class="mt-4">
              {{ draftMessage }}
            </v-alert>
            <v-alert v-if="draftError" type="error" variant="tonal" class="mt-4">
              {{ draftError }}
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

            <template v-if="sceneDraftRecord">
              <v-divider class="my-4" />
              <div class="d-flex justify-space-between align-center ga-3">
                <div class="text-subtitle-2 font-weight-medium">当前镜头草稿</div>
                <div class="d-flex ga-2 flex-wrap">
                  <v-btn
                    color="primary"
                    variant="tonal"
                    prepend-icon="mdi-check"
                    :loading="draftLoading"
                    @click="acceptSceneDraft"
                  >
                  接受写回正文
                  </v-btn>
                  <v-btn
                    color="error"
                    variant="outlined"
                    prepend-icon="mdi-close"
                    :loading="draftLoading"
                    @click="rejectSceneDraft"
                  >
                    拒绝草稿
                  </v-btn>
                </div>
              </div>
              <div class="text-caption text-medium-emphasis mt-2">
                {{ sceneDraftRecord.writingType }} · {{ sceneDraftRecord.selectedModel || '默认模型' }}
              </div>
              <div class="text-caption text-medium-emphasis mt-2">
                接受草稿后会自动完成当前镜头并切到下一镜头；拒绝草稿不会推进镜头状态。
              </div>
              <MarkdownContent class="mt-4" :content="sceneDraftRecord.generatedContent || ''" />
            </template>
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

  <v-dialog v-model="sceneEditorVisible" max-width="760">
    <v-card>
      <v-card-title>编辑当前镜头</v-card-title>
      <v-card-text>
        <div class="text-body-2 text-medium-emphasis mb-4">
          当前镜头：<span class="scene-token">{{ currentScene?.sceneId || sceneId }}</span>
        </div>
        <v-textarea
          v-model="sceneEditorForm.goal"
          label="镜头目标"
          variant="outlined"
          rows="3"
          auto-grow
        />
        <v-textarea
          v-model="sceneEditorForm.readerRevealText"
          label="向读者揭晓"
          variant="outlined"
          rows="3"
          auto-grow
          hint="一行一条"
          persistent-hint
          class="mt-4"
        />
        <v-textarea
          v-model="sceneEditorForm.mustUseAnchorsText"
          label="必须使用锚点"
          variant="outlined"
          rows="3"
          auto-grow
          hint="一行一条"
          persistent-hint
          class="mt-4"
        />
        <v-textarea
          v-model="sceneEditorForm.stopCondition"
          label="镜头停点"
          variant="outlined"
          rows="2"
          auto-grow
          class="mt-4"
        />
        <v-text-field
          v-model.number="sceneEditorForm.targetWords"
          label="目标字数"
          type="number"
          variant="outlined"
          class="mt-4"
        />
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn variant="text" @click="sceneEditorVisible = false">取消</v-btn>
        <v-btn color="primary" :loading="sceneMutationLoading" @click="saveSceneOverride">保存镜头</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
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
