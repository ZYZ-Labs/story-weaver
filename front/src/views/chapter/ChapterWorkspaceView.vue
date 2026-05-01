<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'

import AIProcessLogPanel from '@/components/AIProcessLogPanel.vue'
import ChapterAnchorPanel from '@/components/ChapterAnchorPanel.vue'
import EmptyState from '@/components/EmptyState.vue'
import GenerationReadinessCard from '@/components/GenerationReadinessCard.vue'
import MarkdownContent from '@/components/MarkdownContent.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import PageContainer from '@/components/PageContainer.vue'
import StatCard from '@/components/StatCard.vue'
import { getChapterAnchors, getChapterGenerationReadiness } from '@/api/story-generation'
import {
  getChapterNodePreview,
  deleteChapterSkeletonScene,
  getChapterExecutionReview,
  getChapterSkeletonPreview,
  getStorySessionPreview,
  resolveChapterNodeAction,
  streamGenerateChapterSkeleton,
  updateChapterSkeletonScene,
} from '@/api/story-orchestration'
import { getReaderKnownState } from '@/api/story-context'
import { getChapterCompatibilitySnapshot, getChapterState, getReaderRevealState } from '@/api/story-state'
import { useProjectWorkspace } from '@/composables/useProjectWorkspace'
import { useWritingStore } from '@/stores/writing'
import type {
  ChapterAnchorBundle,
  ChapterNodeRuntimeView,
  ChapterSkeletonStreamEvent,
  ChapterExecutionReviewView,
  ChapterIncrementalStateView,
  ChapterSkeletonView,
  GenerationReadiness,
  MigrationCompatibilitySnapshotView,
  NodeResolutionResultView,
  ReaderKnownStateView,
  ReaderRevealStateView,
  AIWritingStreamLogItem,
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
const writingStore = useWritingStore()

const loading = ref(false)
const sceneLoading = ref(false)
const skeletonGenerating = ref(false)
const saveLoading = ref(false)
const sceneMutationLoading = ref(false)
const draftLoading = ref(false)
const sceneId = ref('')
const editorContent = ref('')
const saveMessage = ref('')
const saveError = ref('')
const sceneMutationMessage = ref('')
const sceneMutationError = ref('')
const skeletonMessage = ref('')
const skeletonError = ref('')
const skeletonLogs = ref<AIWritingStreamLogItem[]>([])
const draftMessage = ref('')
const draftError = ref('')
const rollbackLoading = ref(false)
const rollbackMessage = ref('')
const rollbackError = ref('')
const runtimeModeLoading = ref(false)
const runtimeModeMessage = ref('')
const runtimeModeError = ref('')
const nodeActionLoading = ref(false)
const nodeActionMessage = ref('')
const nodeActionError = ref('')
const selectedNodeOptionId = ref('')
const customNodeAction = ref('')
const sceneEditorVisible = ref(false)

const readiness = ref<GenerationReadiness | null>(null)
const anchors = ref<ChapterAnchorBundle | null>(null)
const skeleton = ref<ChapterSkeletonView | null>(null)
const review = ref<ChapterExecutionReviewView | null>(null)
const sessionPreview = ref<StorySessionPreviewView | null>(null)
const chapterState = ref<ChapterIncrementalStateView | null>(null)
const readerRevealState = ref<ReaderRevealStateView | null>(null)
const readerKnownState = ref<ReaderKnownStateView | null>(null)
const compatibilitySnapshot = ref<MigrationCompatibilitySnapshotView | null>(null)
const nodeRuntime = ref<ChapterNodeRuntimeView | null>(null)
const lastNodeResolution = ref<NodeResolutionResultView | null>(null)

const sceneEditorForm = reactive({
  goal: '',
  readerRevealText: '',
  mustUseAnchorsText: '',
  stopCondition: '',
  targetWords: 900 as number | null,
})

const hasSkeleton = computed(() => Boolean(skeleton.value?.scenes?.length))
const orderedScenes = computed(() => skeleton.value?.scenes || [])
const unlockedScene = computed(() =>
  orderedScenes.value.find((scene) => scene.status !== 'COMPLETED') || null,
)
const unlockedSceneId = computed(() => unlockedScene.value?.sceneId || '')
const chapterCompleted = computed(() => hasSkeleton.value && !unlockedScene.value)
const selectableSceneIds = computed(() => new Set(
  orderedScenes.value
    .filter((scene) => scene.status === 'COMPLETED' || !unlockedSceneId.value || scene.sceneId === unlockedSceneId.value)
    .map((scene) => scene.sceneId),
))
const lockedSceneIds = computed(() => new Set(
  orderedScenes.value
    .map((scene) => scene.sceneId)
    .filter((item) => !selectableSceneIds.value.has(item)),
))
const completedSceneIds = computed(() =>
  orderedScenes.value
    .filter((scene) => scene.status === 'COMPLETED')
    .map((scene) => scene.sceneId),
)
const latestAcceptedSceneId = computed(() => completedSceneIds.value[completedSceneIds.value.length - 1] || '')
const hasAcceptedScenes = computed(() => completedSceneIds.value.length > 0)
const sceneOptions = computed(() =>
  orderedScenes.value.map((scene) => ({
    title: `${scene.sceneId} · ${scene.goal || scene.status}`,
    value: scene.sceneId,
    props: {
      disabled: lockedSceneIds.value.has(scene.sceneId),
    },
  })),
)

const currentScene = computed(
  () => skeleton.value?.scenes?.find((scene) => scene.sceneId === sceneId.value) || null,
)

const compatibilityFeatureFlags = computed(() => parseFeatureFlags(compatibilitySnapshot.value?.featureFlags || []))
const chapterRuntimeMode = computed(() => {
  const value = activeChapter.value?.narrativeRuntimeMode
  return value === 'node' ? 'node' : 'scene'
})
const isSceneMode = computed(() => chapterRuntimeMode.value === 'scene')
const isNodeMode = computed(() => chapterRuntimeMode.value === 'node')
const nodePreviewEnabled = computed(() => {
  const value = compatibilityFeatureFlags.value.chapterWorkspaceNodePreviewEnabled
  return value == null ? true : value !== 'false'
})
const nodeResolveEnabled = computed(() => compatibilityFeatureFlags.value.chapterWorkspaceNodeResolveEnabled === 'true')
const runtimeModeOptions = computed(() => [
  {
    title: 'Scene Mode',
    value: 'scene',
  },
  {
    title: 'Node Mode',
    value: 'node',
    props: {
      disabled: !nodeResolveEnabled.value && !isNodeMode.value,
    },
  },
])
const orderedNodes = computed(() => nodeRuntime.value?.skeleton?.nodes || [])
const currentNode = computed(
  () => orderedNodes.value.find((node) => node.nodeId === nodeRuntime.value?.currentNodeId) || null,
)
const currentNodeCompleted = computed(() => Boolean(
  currentNode.value && nodeRuntime.value?.completedNodeIds?.includes(currentNode.value.nodeId),
))
const latestNodeCheckpoint = computed(() =>
  nodeRuntime.value?.checkpoints?.[nodeRuntime.value.checkpoints.length - 1] || null,
)
const currentNodeActionOptions = computed(() => currentNode.value?.recommendedActions || [])

const currentSceneCompleted = computed(() => currentScene.value?.status === 'COMPLETED')
const currentSceneUnlocked = computed(() => Boolean(currentScene.value && currentScene.value.sceneId === unlockedSceneId.value))
const currentSceneStreamScopeKey = computed(() =>
  activeChapterId.value && sceneId.value ? `chapter:${activeChapterId.value}:scene:${sceneId.value}` : '',
)
const currentSceneStreamState = computed(() =>
  writingStore.getStreamState(activeChapterId.value || null, currentSceneStreamScopeKey.value),
)
const displaySceneDraftRecord = computed(() => currentSceneStreamState.value.lastRecord)
const displaySceneDraftContent = computed(
  () => displaySceneDraftRecord.value?.generatedContent || currentSceneStreamState.value.content || '',
)
const displaySceneDraftGenerating = computed(() => Boolean(currentSceneStreamState.value.generating))
const displaySceneDraftLogs = computed(() => currentSceneStreamState.value.logs || [])
const currentSceneSequenceHint = computed(() => {
  if (!hasSkeleton.value) {
    return '先生成镜头骨架，再进入顺序写作。'
  }
  if (chapterCompleted.value) {
    return '当前章节所有镜头都已接纳完成。'
  }
  if (currentSceneUnlocked.value) {
    return `当前正在处理 ${unlockedSceneId.value}，接纳后才会解锁下一镜头。`
  }
  if (currentSceneCompleted.value) {
    return `${sceneId.value} 已接纳完成，当前可继续的镜头是 ${unlockedSceneId.value}。`
  }
  return unlockedSceneId.value
    ? `${sceneId.value} 尚未解锁，请先接纳 ${unlockedSceneId.value}。`
    : '当前章节没有可继续处理的镜头。'
})

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

function parseFeatureFlags(flags: string[]) {
  return Object.fromEntries(
    (flags || []).map((item) => {
      const [key, ...rest] = String(item || '').split('=')
      return [key.trim(), rest.join('=').trim()]
    }).filter(([key]) => key),
  ) as Record<string, string>
}

function normalizeRuntimeMode(value: unknown): 'scene' | 'node' {
  return String(value || '').trim().toLowerCase() === 'node' ? 'node' : 'scene'
}

function canSelectScene(targetSceneId: string) {
  return selectableSceneIds.value.has(targetSceneId)
}

function syncCurrentNodeActionSelection() {
  const options = currentNodeActionOptions.value
  if (!options.length) {
    selectedNodeOptionId.value = ''
    return
  }
  if (!options.some((option) => option.optionId === selectedNodeOptionId.value)) {
    selectedNodeOptionId.value = options[0].optionId
  }
}

function nodeCardStatus(nodeId: string) {
  if (nodeRuntime.value?.completedNodeIds?.includes(nodeId)) {
    return '已完成'
  }
  if (nodeRuntime.value?.currentNodeId === nodeId) {
    return '当前节点'
  }
  return '待推进'
}

function getNextSceneId(targetSceneId: string) {
  const index = orderedScenes.value.findIndex((scene) => scene.sceneId === targetSceneId)
  return index >= 0 && index + 1 < orderedScenes.value.length ? orderedScenes.value[index + 1].sceneId : ''
}

function resolveSceneId() {
  if (sceneId.value && canSelectScene(sceneId.value)) {
    return sceneId.value
  }
  if (unlockedSceneId.value) {
    return unlockedSceneId.value
  }
  if (review.value?.traceSummary?.latestSceneId && canSelectScene(review.value.traceSummary.latestSceneId)) {
    return review.value.traceSummary.latestSceneId
  }
  return orderedScenes.value[orderedScenes.value.length - 1]?.sceneId || orderedScenes.value[0]?.sceneId || ''
}

function updateSceneId(value: unknown) {
  const nextSceneId = String(value || '')
  if (nextSceneId && canSelectScene(nextSceneId)) {
    sceneId.value = nextSceneId
  }
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
  if (!targetSceneId || !canSelectScene(targetSceneId)) {
    sessionPreview.value = null
    return
  }
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
    const chapterDetail = await chapterStore.fetchDetail(projectId, chapterId).catch(() => undefined)
    const runtimeMode = normalizeRuntimeMode(chapterDetail?.narrativeRuntimeMode)

    const [
      readinessResult,
      anchorsResult,
      skeletonResult,
      reviewResult,
      chapterStateResult,
      revealResult,
      knownResult,
      compatibilityResult,
      nodeRuntimeResult,
    ] = await Promise.allSettled([
      getChapterGenerationReadiness(projectId, chapterId),
      getChapterAnchors(projectId, chapterId),
      getChapterSkeletonPreview(projectId, chapterId),
      getChapterExecutionReview(projectId, chapterId),
      getChapterState(projectId, chapterId),
      getReaderRevealState(projectId, chapterId),
      getReaderKnownState(projectId, chapterId),
      getChapterCompatibilitySnapshot(projectId, chapterId),
      getChapterNodePreview(projectId, chapterId),
    ])

    readiness.value = readinessResult.status === 'fulfilled' ? readinessResult.value : null
    anchors.value = anchorsResult.status === 'fulfilled' ? anchorsResult.value : null
    skeleton.value = skeletonResult.status === 'fulfilled' ? skeletonResult.value : null
    review.value = reviewResult.status === 'fulfilled' ? reviewResult.value : null
    chapterState.value = chapterStateResult.status === 'fulfilled' ? chapterStateResult.value : null
    readerRevealState.value = revealResult.status === 'fulfilled' ? revealResult.value : null
    readerKnownState.value = knownResult.status === 'fulfilled' ? knownResult.value : null
    compatibilitySnapshot.value = compatibilityResult.status === 'fulfilled' ? compatibilityResult.value : null
    const featureFlags = parseFeatureFlags(compatibilitySnapshot.value?.featureFlags || [])
    const previewAllowed = featureFlags.chapterWorkspaceNodePreviewEnabled == null
      ? true
      : featureFlags.chapterWorkspaceNodePreviewEnabled !== 'false'
    nodeRuntime.value = previewAllowed && skeleton.value?.scenes?.length && nodeRuntimeResult.status === 'fulfilled'
      ? nodeRuntimeResult.value
      : null
    syncCurrentNodeActionSelection()

    sceneId.value = resolveSceneId()
    if (runtimeMode === 'scene' && skeleton.value?.scenes?.length && sceneId.value) {
      await loadScenePreview(projectId, chapterId, sceneId.value)
    } else {
      sessionPreview.value = null
    }
  } finally {
    loading.value = false
  }
}

async function updateRuntimeMode(value: unknown) {
  if (!currentProjectId.value || !activeChapterId.value) {
    return
  }

  const targetMode = normalizeRuntimeMode(value)
  if (targetMode === chapterRuntimeMode.value) {
    return
  }
  if (targetMode === 'node' && !nodeResolveEnabled.value) {
    runtimeModeError.value = 'node runtime 推进兼容开关当前未打开，暂时不能切到 node mode。'
    return
  }

  const warning = targetMode === 'node'
    ? '确认把当前章节切到 node mode 吗？切换后章节工作区会停用 scene 草稿生成/接纳/撤回，改为只允许 node runtime 推进。'
    : '确认把当前章节切回 scene mode 吗？切换后章节工作区会恢复 scene 草稿链。'
  if (!window.confirm(warning)) {
    return
  }

  runtimeModeLoading.value = true
  runtimeModeMessage.value = ''
  runtimeModeError.value = ''
  draftMessage.value = ''
  draftError.value = ''
  nodeActionMessage.value = ''
  nodeActionError.value = ''
  rollbackMessage.value = ''
  rollbackError.value = ''
  try {
    await chapterStore.updateRuntimeMode(currentProjectId.value, activeChapterId.value, targetMode)
    runtimeModeMessage.value = targetMode === 'node'
      ? '当前章节已切到 node mode。后续主链会改为 node runtime。'
      : '当前章节已切回 scene mode。'
    await loadWorkspace(currentProjectId.value, activeChapterId.value)
  } catch (error) {
    runtimeModeError.value = error instanceof Error ? error.message : '切换章节运行模式失败'
  } finally {
    runtimeModeLoading.value = false
  }
}

async function regenerateSkeleton(forceRefresh = false) {
  if (!currentProjectId.value || !activeChapterId.value) {
    return
  }
  skeletonGenerating.value = true
  skeletonLogs.value = []
  skeletonMessage.value = ''
  skeletonError.value = ''
  sceneMutationMessage.value = ''
  sceneMutationError.value = ''
  try {
    const nextSkeleton = await streamGenerateChapterSkeleton(
      currentProjectId.value,
      activeChapterId.value,
      forceRefresh,
      {
        onEvent: handleSkeletonStreamEvent,
      },
    )
    skeleton.value = nextSkeleton
    sceneId.value = nextSkeleton.scenes?.[0]?.sceneId || ''
    skeletonMessage.value = forceRefresh ? '镜头骨架已重新生成。' : '镜头骨架已生成。'
    await loadWorkspace(currentProjectId.value, activeChapterId.value)
  } catch (error) {
    skeletonError.value = error instanceof Error ? error.message : '生成镜头骨架失败'
  } finally {
    skeletonGenerating.value = false
  }
}

function handleSkeletonStreamEvent(event: ChapterSkeletonStreamEvent) {
  if (event.type === 'stage' || event.type === 'log' || event.type === 'error') {
    appendSkeletonLogItem(skeletonLogs.value, event)
  }
}

function appendSkeletonLogItem(logs: AIWritingStreamLogItem[], event: ChapterSkeletonStreamEvent) {
  if (event.type !== 'log') {
    logs.push(toSkeletonLogItem(event))
    return
  }

  const signature = buildSkeletonLogSignature(event)
  for (let index = logs.length - 1; index >= 0; index -= 1) {
    const item = logs[index]
    if (buildSkeletonLogSignature(item) !== signature) {
      continue
    }

    const now = Date.now()
    const firstSeenAt = item.firstSeenAt || now
    item.occurrenceCount = (item.occurrenceCount || 1) + 1
    item.lastSeenAt = now
    item.elapsedSeconds = Math.max(0, Math.floor((now - firstSeenAt) / 1000))
    return
  }

  logs.push(toSkeletonLogItem(event))
}

function toSkeletonLogItem(event: ChapterSkeletonStreamEvent): AIWritingStreamLogItem {
  const now = Date.now()
  return {
    id: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
    type: event.type,
    stage: event.stage,
    stageStatus: event.stageStatus,
    message: event.message,
    occurrenceCount: 1,
    firstSeenAt: now,
    lastSeenAt: now,
    elapsedSeconds: 0,
  }
}

function buildSkeletonLogSignature(item: Pick<AIWritingStreamLogItem, 'type' | 'stage' | 'stageStatus' | 'message'>) {
  return [item.type || '', item.stage || '', item.stageStatus || '', item.message || ''].join('|')
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
    const deletingSceneId = currentScene.value.sceneId
    await deleteChapterSkeletonScene(currentProjectId.value, activeChapterId.value, deletingSceneId)
    writingStore.clearStreamState(activeChapterId.value, `chapter:${activeChapterId.value}:scene:${deletingSceneId}`)
    sceneMutationMessage.value = `已删除 ${deletingSceneId}。`
    await loadWorkspace(currentProjectId.value, activeChapterId.value)
  } catch (error) {
    sceneMutationError.value = error instanceof Error ? error.message : '删除镜头失败'
  } finally {
    sceneMutationLoading.value = false
  }
}

async function generateSceneDraft() {
  if (!isSceneMode.value || !activeChapterId.value || !currentScene.value || !sessionPreview.value || !currentSceneUnlocked.value) {
    return
  }
  draftLoading.value = true
  draftError.value = ''
  draftMessage.value = ''
  rollbackMessage.value = ''
  rollbackError.value = ''
  const targetSceneId = sceneId.value
  try {
    const brief = sessionPreview.value.writerExecutionBrief
    const continuityNotes = brief.continuityNotes || []
    const continuityState = brief.continuityState || null
    const inheritedFacts = continuityState?.carryForwardFacts || []
    const timeAnchors = continuityState?.timeAnchors || []
    const counterpartNames = (continuityState?.counterpartNames?.length
      ? continuityState.counterpartNames
      : continuityState?.expectedNames) || []
    const instructionSections = [
      `请严格围绕当前镜头生成小说正文，不要越过镜头停点。`,
      `【镜头ID】${sceneId.value}`,
      `【镜头目标】${brief.goal || '未定义'}`,
      `【向读者揭晓】${(brief.readerReveal || []).join('；') || '未定义'}`,
      `【必须使用锚点】${(brief.mustUseAnchors || []).join('；') || '未定义'}`,
      `【禁止动作】${(brief.forbiddenMoves || []).join('；') || '不要重复上一镜头，不要抢写下一镜头'}`,
      `【收束点】${brief.stopCondition || '未定义'}`,
      continuityState?.summary || brief.previousSceneSummary
        ? `【上一镜头真实摘要】${continuityState?.summary || brief.previousSceneSummary}`
        : '',
      continuityState?.handoffLine || brief.handoffLine
        ? `【上一镜头真实交接】${continuityState?.handoffLine || brief.handoffLine}`
        : '',
      inheritedFacts.length ? `【必须继承事实】${inheritedFacts.join('；')}` : '',
      timeAnchors.length ? `【时间锚点】${timeAnchors.join('；')}` : '',
      counterpartNames.length ? `【沿用人物称呼】${counterpartNames.join('、')}` : '',
      continuityNotes.length ? `【承接说明】${continuityNotes.join('；')}` : '',
      brief.nextSceneId && brief.nextSceneGoal
        ? `【下一镜头入口预留】下一镜头 ${brief.nextSceneId} 将转入：${brief.nextSceneGoal}。当前镜头只负责把局面送到这个入口前，不要提前展开下一镜头正文。`
        : '',
    ].filter(Boolean)

    const record = await writingStore.generateStream({
      chapterId: activeChapterId.value,
      currentContent: editorContent.value,
      userInstruction: instructionSections.join('\n'),
      writingType: editorContent.value.trim() ? 'continue' : 'draft',
      maxTokens: brief.targetWords || 900,
      entryPoint: 'phase8.chapter-workspace.scene-draft',
      sceneId: targetSceneId,
    }, {
      scopeKey: `chapter:${activeChapterId.value}:scene:${targetSceneId}`,
    })
    draftMessage.value = `已生成 ${targetSceneId} 的${record.writingType === 'continue' ? '续写草稿' : '初稿'}。`
  } catch (error) {
    draftError.value = error instanceof Error ? error.message : '生成镜头草稿失败'
  } finally {
    draftLoading.value = false
  }
}

async function acceptSceneDraft() {
  if (
    !isSceneMode.value
    ||
    !displaySceneDraftRecord.value
    || !currentProjectId.value
    || !activeChapterId.value
    || !sceneId.value
    || !currentSceneUnlocked.value
  ) {
    return
  }
  draftLoading.value = true
  draftError.value = ''
  rollbackMessage.value = ''
  rollbackError.value = ''
  try {
    const acceptedSceneId = sceneId.value
    const nextSceneId = getNextSceneId(acceptedSceneId)
    await writingStore.accept(displaySceneDraftRecord.value.id)
    draftMessage.value = nextSceneId
      ? `已接受 ${acceptedSceneId} 的草稿并写回章节正文，已解锁 ${nextSceneId}。`
      : `已接受 ${acceptedSceneId} 的草稿并写回章节正文，当前章节镜头已全部接纳完成。`
    sceneId.value = ''
    await loadWorkspace(currentProjectId.value, activeChapterId.value)
  } catch (error) {
    draftError.value = error instanceof Error ? error.message : '接受草稿失败'
  } finally {
    draftLoading.value = false
  }
}

async function rejectSceneDraft() {
  if (!isSceneMode.value || !displaySceneDraftRecord.value || !sceneId.value) {
    return
  }
  draftLoading.value = true
  draftError.value = ''
  rollbackMessage.value = ''
  rollbackError.value = ''
  try {
    const rejectedSceneId = sceneId.value
    await writingStore.reject(displaySceneDraftRecord.value.id)
    draftMessage.value = `已拒绝 ${rejectedSceneId} 的草稿。`
  } catch (error) {
    draftError.value = error instanceof Error ? error.message : '拒绝草稿失败'
  } finally {
    draftLoading.value = false
  }
}

function clearSceneStreamStates(sceneIds: string[]) {
  if (!activeChapterId.value) {
    return
  }
  for (const acceptedSceneId of sceneIds) {
    writingStore.clearStreamState(activeChapterId.value, `chapter:${activeChapterId.value}:scene:${acceptedSceneId}`)
  }
}

async function rollbackLatestAcceptedScene() {
  if (!isSceneMode.value || !currentProjectId.value || !activeChapterId.value || !latestAcceptedSceneId.value) {
    return
  }
  const warning = `确认撤回最新已接纳镜头 ${latestAcceptedSceneId.value} 吗？撤回后将恢复章节正文，并重新解锁这个镜头。`
  if (!window.confirm(warning)) {
    return
  }
  rollbackLoading.value = true
  rollbackMessage.value = ''
  rollbackError.value = ''
  draftMessage.value = ''
  draftError.value = ''
  try {
    const result = await writingStore.rollbackLatestScene(activeChapterId.value)
    clearSceneStreamStates(result.rolledBackSceneIds || [])
    await writingStore.fetchByChapter(activeChapterId.value).catch(() => undefined)
    await loadWorkspace(currentProjectId.value, activeChapterId.value)
    rollbackMessage.value = result.message || `已撤回 ${latestAcceptedSceneId.value}。`
  } catch (error) {
    rollbackError.value = error instanceof Error ? error.message : '撤回已接纳镜头失败'
  } finally {
    rollbackLoading.value = false
  }
}

async function rollbackAllAcceptedScenes() {
  if (!isSceneMode.value || !currentProjectId.value || !activeChapterId.value || !hasAcceptedScenes.value) {
    return
  }
  const warning = `确认一次性撤回当前章节全部已接纳镜头吗？这会把正文恢复到镜头写作开始前的版本。`
  if (!window.confirm(warning)) {
    return
  }
  rollbackLoading.value = true
  rollbackMessage.value = ''
  rollbackError.value = ''
  draftMessage.value = ''
  draftError.value = ''
  try {
    const result = await writingStore.rollbackAllScenes(activeChapterId.value)
    clearSceneStreamStates(result.rolledBackSceneIds || [])
    await writingStore.fetchByChapter(activeChapterId.value).catch(() => undefined)
    await loadWorkspace(currentProjectId.value, activeChapterId.value)
    rollbackMessage.value = result.message || '已撤回当前章节全部已接纳镜头。'
  } catch (error) {
    rollbackError.value = error instanceof Error ? error.message : '撤回全部已接纳镜头失败'
  } finally {
    rollbackLoading.value = false
  }
}

async function resolveCurrentNodeAction() {
  if (!currentProjectId.value || !activeChapterId.value || !currentNode.value || !nodeRuntime.value) {
    return
  }
  if (!isNodeMode.value) {
    nodeActionError.value = '当前章节仍处于 scene mode，node runtime 仅支持预览。'
    return
  }
  if (!nodeResolveEnabled.value) {
    nodeActionError.value = 'node runtime 推进当前默认关闭，避免与 scene mode 混写。'
    return
  }

  const trimmedCustomAction = customNodeAction.value.trim()
  if (!selectedNodeOptionId.value && !trimmedCustomAction) {
    nodeActionError.value = '先选择一个推荐动作，或填写自定义动作。'
    return
  }

  nodeActionLoading.value = true
  nodeActionMessage.value = ''
  nodeActionError.value = ''
  try {
    const result = await resolveChapterNodeAction(currentProjectId.value, activeChapterId.value, {
      nodeId: currentNode.value.nodeId,
      checkpointId: nodeRuntime.value.latestCheckpointId || '',
      selectedOptionId: trimmedCustomAction ? '' : selectedNodeOptionId.value,
      customAction: trimmedCustomAction,
    })
    lastNodeResolution.value = result
    customNodeAction.value = ''
    nodeActionMessage.value = `已推进 ${result.nodeId}，并保存 ${result.nextCheckpoint.checkpointId}。`
    await loadWorkspace(currentProjectId.value, activeChapterId.value)
  } catch (error) {
    nodeActionError.value = error instanceof Error ? error.message : '推进当前节点失败'
  } finally {
    nodeActionLoading.value = false
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
      compatibilitySnapshot.value = null
      nodeRuntime.value = null
      lastNodeResolution.value = null
      sceneId.value = ''
      skeletonMessage.value = ''
      skeletonError.value = ''
      skeletonLogs.value = []
      runtimeModeMessage.value = ''
      runtimeModeError.value = ''
      rollbackMessage.value = ''
      rollbackError.value = ''
      nodeActionMessage.value = ''
      nodeActionError.value = ''
      selectedNodeOptionId.value = ''
      customNodeAction.value = ''
      return
    }
    rollbackMessage.value = ''
    rollbackError.value = ''
    nodeActionMessage.value = ''
    nodeActionError.value = ''
    lastNodeResolution.value = null
    await writingStore.fetchByChapter(chapterId).catch(() => undefined)
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
  if (!isSceneMode.value || !currentProjectId.value || !activeChapterId.value || !value || !hasSkeleton.value || !canSelectScene(value)) {
    sessionPreview.value = null
    return
  }
  await loadScenePreview(currentProjectId.value, activeChapterId.value, value)
})

watch(
  () => currentNode.value?.nodeId,
  () => {
    syncCurrentNodeActionSelection()
  },
  { immediate: true },
)
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
          item-props="props"
          :model-value="sceneId"
          label="当前镜头"
          :disabled="!sceneOptions.length"
          @update:model-value="updateSceneId"
        />
        <v-select
          class="chapter-workspace-actions__scene"
          hide-details
          density="comfortable"
          variant="outlined"
          :items="runtimeModeOptions"
          item-props="props"
          :model-value="chapterRuntimeMode"
          :loading="runtimeModeLoading"
          label="运行模式"
          @update:model-value="updateRuntimeMode"
        />
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
              字数：{{ activeChapter.wordCount || 0 }} · Runtime：{{ chapterRuntimeMode }}
            </div>
            <div class="text-body-1 mt-4">
              {{ activeChapter.summary || '当前章节还没有摘要，建议先通过摘要工作流补一版。' }}
            </div>
            <v-alert v-if="runtimeModeMessage" type="success" variant="tonal" class="mt-4">
              {{ runtimeModeMessage }}
            </v-alert>
            <v-alert v-if="runtimeModeError" type="error" variant="tonal" class="mt-4">
              {{ runtimeModeError }}
            </v-alert>
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
              :rows="15"
              class="chapter-content-editor"
              preview-empty-text="当前还没有正文，可以先执行镜头或直接补写。"
            />
          </v-card-text>
        </v-card>

        <v-card class="soft-panel">
          <v-card-title class="d-flex justify-space-between align-center ga-3">
            <span>章节骨架与镜头</span>
            <div class="d-flex ga-2 flex-wrap">
              <v-btn
                color="primary"
                prepend-icon="mdi-auto-fix"
                :loading="skeletonGenerating"
                @click="regenerateSkeleton(hasSkeleton)"
              >
                {{ hasSkeleton ? '重新生成镜头骨架' : '生成镜头骨架' }}
              </v-btn>
              <v-btn
                variant="outlined"
                prepend-icon="mdi-pencil-outline"
                :disabled="skeletonGenerating || !currentScene || currentSceneCompleted || isNodeMode"
                @click="openSceneEditor"
              >
                编辑当前镜头
              </v-btn>
              <v-btn
                color="error"
                variant="tonal"
                prepend-icon="mdi-delete-outline"
                :disabled="skeletonGenerating || !currentScene"
                :loading="sceneMutationLoading"
                @click="deleteCurrentScene"
              >
                删除当前镜头
              </v-btn>
            </div>
          </v-card-title>
          <v-card-text>
            <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-4" />
            <v-alert v-if="skeletonMessage" type="success" variant="tonal" class="mb-4">
              {{ skeletonMessage }}
            </v-alert>
            <v-alert v-if="skeletonError" type="error" variant="tonal" class="mb-4">
              {{ skeletonError }}
            </v-alert>
            <v-alert v-if="sceneMutationMessage" type="success" variant="tonal" class="mb-4">
              {{ sceneMutationMessage }}
            </v-alert>
            <v-alert v-if="sceneMutationError" type="error" variant="tonal" class="mb-4">
              {{ sceneMutationError }}
            </v-alert>
            <AIProcessLogPanel
              v-if="skeletonGenerating || skeletonLogs.length"
              :logs="skeletonLogs"
              :loading="skeletonGenerating"
              title="骨架生成流水线"
            />
            <v-divider v-if="skeletonGenerating || skeletonLogs.length" class="my-4" />
            <div v-if="skeleton?.scenes?.length" class="scene-list">
              <button
                v-for="scene in skeleton.scenes"
                :key="scene.sceneId"
                type="button"
                class="scene-list-item chapter-workspace-scene-button"
                :class="{
                  'chapter-workspace-scene-button--active': scene.sceneId === sceneId,
                  'chapter-workspace-scene-button--locked': lockedSceneIds.has(scene.sceneId),
                }"
                :disabled="!canSelectScene(scene.sceneId)"
                @click="updateSceneId(scene.sceneId)"
              >
                <div class="scene-list-item__header">
                  <div class="text-subtitle-1 font-weight-bold scene-list-item__title">
                    {{ scene.sceneId }}
                  </div>
                  <v-chip size="small" color="secondary" variant="tonal" class="scene-list-item__metric">
                    {{ scene.status }}
                  </v-chip>
                </div>
                <div class="chip-stack mt-2">
                  <v-chip
                    v-if="scene.sceneId === unlockedSceneId"
                    size="x-small"
                    color="primary"
                    variant="tonal"
                  >
                    当前可写
                  </v-chip>
                  <v-chip
                    v-else-if="lockedSceneIds.has(scene.sceneId)"
                    size="x-small"
                    color="warning"
                    variant="outlined"
                  >
                    未解锁
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
              description="先点击“生成镜头骨架”，由 AI 产出当前章节的真实镜头分解。"
              icon="mdi-file-tree-outline"
            />
            <div v-if="skeleton?.planningNotes?.length" class="mt-4">
              <div class="text-caption text-medium-emphasis mb-2">
                当前顺序规则：只能处理第一个未接纳镜头，接纳后才会解锁下一镜头。
              </div>
              <div class="text-caption text-medium-emphasis">规划说明</div>
              <v-list density="compact" lines="one" class="mt-2">
                <v-list-item
                  v-for="(note, index) in skeleton.planningNotes"
                  :key="`planning-note-${index}`"
                  :title="note"
                />
              </v-list>
            </div>
          </v-card-text>
        </v-card>
      </div>

      <div class="chapter-workspace-stack">
        <GenerationReadinessCard :readiness="readiness" :loading="loading" title="当前章节生成就绪度" />

        <ChapterAnchorPanel :anchors="anchors" :loading="loading" title="当前章节锚点" />

        <v-card class="soft-panel">
          <v-card-title class="d-flex justify-space-between align-center ga-3">
            <span>节点 Runtime（Beta）</span>
            <v-chip size="small" color="secondary" variant="tonal">
              {{ isNodeMode ? (nodeResolveEnabled ? '当前主链' : '已切换但全局关闭') : '辅助预览' }}
            </v-chip>
          </v-card-title>
          <v-card-text>
            <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-4" />
            <v-alert
              type="info"
              variant="tonal"
              class="mb-4"
            >
              {{
                isNodeMode
                  ? '当前章节已经切到 node mode。这里的主链是 intent / resolve / checkpoint，后续 narrative render 会挂在这条状态链之后。'
                  : '这条链服务新的状态驱动架构：先结算 intent / resolve / checkpoint，再进入 narrative render。当前正文主链仍是 scene mode。'
              }}
            </v-alert>
            <v-alert
              v-if="!nodeResolveEnabled"
              type="warning"
              variant="tonal"
              class="mb-4"
            >
              {{
                isNodeMode
                  ? '当前章节虽然已经切到 node mode，但全局 resolve 兼容开关仍然关闭，所以现在只能看预览，不能推进。'
                  : 'node runtime 推进当前默认关闭，避免和现有 scene mode 在同一章节上混写状态。'
              }}
            </v-alert>
            <v-alert v-if="nodeActionMessage" type="success" variant="tonal" class="mb-4">
              {{ nodeActionMessage }}
            </v-alert>
            <v-alert v-if="nodeActionError" type="error" variant="tonal" class="mb-4">
              {{ nodeActionError }}
            </v-alert>
            <EmptyState
              v-if="!hasSkeleton"
              title="先生成镜头骨架"
              description="node mode 第一阶段仍通过现有骨架适配生成节点链。"
              icon="mdi-source-branch"
            />
            <EmptyState
              v-else-if="!nodePreviewEnabled"
              title="node runtime 预览当前未开放"
              description="兼容边界当前没有打开 node runtime 预览入口。"
              icon="mdi-lock-outline"
            />
            <EmptyState
              v-else-if="!nodeRuntime"
              title="当前还没有节点运行态"
              description="章节骨架存在，但 node runtime 预览尚未返回结果。"
              icon="mdi-source-branch"
            />
            <template v-else>
              <div class="text-body-2">
                {{ nodeRuntime.skeleton.globalObjective || '当前节点链还没有全局目标。' }}
              </div>
              <div class="chip-stack mt-3">
                <v-chip color="primary" variant="tonal">
                  当前节点 · {{ nodeRuntime.currentNodeId || '已全部完成' }}
                </v-chip>
                <v-chip color="secondary" variant="tonal">
                  已完成 {{ nodeRuntime.completedNodeIds.length }}/{{ nodeRuntime.skeleton.nodeCount }}
                </v-chip>
                <v-chip color="accent" variant="outlined">
                  最新 checkpoint · {{ nodeRuntime.latestCheckpointId || '尚未写入' }}
                </v-chip>
              </div>

              <div class="scene-list mt-4">
                <div
                  v-for="node in nodeRuntime.skeleton.nodes"
                  :key="node.nodeId"
                  class="scene-list-item chapter-workspace-node-card"
                  :class="{
                    'chapter-workspace-node-card--active': node.nodeId === nodeRuntime.currentNodeId,
                  }"
                >
                  <div class="scene-list-item__header">
                    <div class="text-subtitle-1 font-weight-bold scene-list-item__title">
                      {{ node.nodeId }}
                    </div>
                    <v-chip size="small" color="secondary" variant="tonal" class="scene-list-item__metric">
                      {{ nodeCardStatus(node.nodeId) }}
                    </v-chip>
                  </div>
                  <div class="text-body-2 mt-2">{{ node.playerGoal || node.situation || '当前节点缺少目标描述。' }}</div>
                  <div class="text-caption text-medium-emphasis mt-3">
                    停点：{{ node.stopCondition || '未定义' }}
                  </div>
                </div>
              </div>

              <template v-if="currentNode">
                <v-divider class="my-4" />
                <div class="text-subtitle-2 font-weight-medium">当前节点目标</div>
                <div class="text-body-2 mt-2">
                  {{ currentNode.playerGoal || '当前节点缺少 player goal。' }}
                </div>
                <div class="text-body-2 mt-2">
                  局面：{{ currentNode.situation || '当前节点没有补充 situation。' }}
                </div>
                <div class="text-caption text-medium-emphasis mt-3">
                  停点：{{ currentNode.stopCondition || '未定义' }} ·
                  checkpoint：{{ currentNode.checkpointHint || '未定义' }}
                </div>
                <div v-if="currentNode.nextNodeHints.length" class="text-caption text-medium-emphasis mt-2">
                  下一节点预留：{{ currentNode.nextNodeHints.join('；') }}
                </div>

                <div class="text-subtitle-2 font-weight-medium mt-5">推荐动作</div>
                <v-radio-group
                  v-model="selectedNodeOptionId"
                  color="primary"
                  class="mt-2"
                  :disabled="!nodeResolveEnabled || nodeActionLoading || currentNodeCompleted"
                >
                  <v-radio
                    v-for="option in currentNodeActionOptions"
                    :key="option.optionId"
                    :value="option.optionId"
                  >
                    <template #label>
                      <div class="chapter-workspace-node-option">
                        <div class="font-weight-medium">{{ option.label }}</div>
                        <div class="text-body-2 text-medium-emphasis mt-1">{{ option.intentSummary }}</div>
                        <div class="text-caption text-medium-emphasis mt-1">
                          风险：{{ option.riskNote || '未定义' }}
                        </div>
                        <div class="text-caption text-medium-emphasis mt-1">
                          揭晓提示：{{ option.revealHint || '未定义' }}
                        </div>
                      </div>
                    </template>
                  </v-radio>
                </v-radio-group>

                <v-textarea
                  v-if="currentNode.customActionAllowed"
                  v-model="customNodeAction"
                  variant="outlined"
                  rows="3"
                  auto-grow
                  label="自定义动作"
                  hint="可以覆盖推荐动作，但当前默认仍保持只读预览，除非显式打开 node resolve 兼容开关。"
                  persistent-hint
                  class="mt-3"
                  :disabled="!isNodeMode || !nodeResolveEnabled || nodeActionLoading || currentNodeCompleted"
                />

                <v-btn
                  color="primary"
                  prepend-icon="mdi-source-commit-next-local"
                  class="mt-4"
                  :loading="nodeActionLoading"
                  :disabled="!isNodeMode || !nodeResolveEnabled || nodeActionLoading || !currentNode || currentNodeCompleted"
                  @click="resolveCurrentNodeAction"
                >
                  推进当前节点并保存 checkpoint
                </v-btn>
              </template>

              <template v-if="latestNodeCheckpoint">
                <v-divider class="my-4" />
                <div class="text-subtitle-2 font-weight-medium">最新 checkpoint</div>
                <div class="text-body-2 mt-2">
                  {{ latestNodeCheckpoint.worldSummary || '当前还没有 world summary。' }}
                </div>
                <div class="text-caption text-medium-emphasis mt-2">
                  读者摘要：{{ latestNodeCheckpoint.readerSummary || '未定义' }}
                </div>
              </template>

              <template v-if="lastNodeResolution">
                <v-divider class="my-4" />
                <div class="text-subtitle-2 font-weight-medium">最近一次结算结果</div>
                <div class="text-body-2 mt-2">
                  {{ lastNodeResolution.resolvedTurn.resolutionSummary || '当前没有结算摘要。' }}
                </div>
                <div
                  v-if="lastNodeResolution.resolvedTurn.readerRevealDelta.length"
                  class="text-caption text-medium-emphasis mt-2"
                >
                  本轮揭晓：{{ lastNodeResolution.resolvedTurn.readerRevealDelta.join('；') }}
                </div>
              </template>

              <v-divider class="my-4" />
              <div class="text-subtitle-2 font-weight-medium">开放回路</div>
              <div class="chip-stack mt-3">
                <v-chip
                  v-for="loop in nodeRuntime.activeLoops"
                  :key="loop.loopId"
                  size="small"
                  color="warning"
                  variant="tonal"
                >
                  {{ loop.label || loop.loopId }}
                </v-chip>
                <span v-if="!nodeRuntime.activeLoops.length" class="text-caption text-medium-emphasis">
                  当前没有未解开放回路。
                </span>
              </div>
            </template>
          </v-card-text>
        </v-card>

        <v-card class="soft-panel">
          <v-card-title class="d-flex justify-space-between align-center ga-3">
            <span>当前镜头执行</span>
            <div class="d-flex ga-2 flex-wrap">
              <v-btn
                color="warning"
                variant="tonal"
                prepend-icon="mdi-undo"
                :loading="rollbackLoading"
                :disabled="!isSceneMode || !hasAcceptedScenes"
                @click="rollbackLatestAcceptedScene"
              >
                {{ latestAcceptedSceneId ? `撤回 ${latestAcceptedSceneId}` : '撤回上一个已接纳镜头' }}
              </v-btn>
              <v-btn
                color="warning"
                variant="outlined"
                prepend-icon="mdi-undo-variant"
                :loading="rollbackLoading"
                :disabled="!isSceneMode || !hasAcceptedScenes"
                @click="rollbackAllAcceptedScenes"
              >
                一次性撤回全部
              </v-btn>
              <v-btn
                color="secondary"
                prepend-icon="mdi-auto-fix"
                :loading="draftLoading"
                :disabled="!isSceneMode || !currentScene || !sessionPreview || !currentSceneUnlocked || rollbackLoading"
                @click="generateSceneDraft"
              >
                {{ editorContent.trim() ? '根据当前镜头继续生成' : '根据当前镜头生成初稿' }}
              </v-btn>
            </div>
          </v-card-title>
          <v-card-text>
            <v-progress-linear v-if="sceneLoading" indeterminate color="primary" class="mb-4" />
            <v-alert
              v-if="isNodeMode"
              type="info"
              variant="tonal"
              class="mb-4"
            >
              当前章节已经切到 node mode，scene 草稿生成 / 接纳 / 回滚已停用。下方只保留骨架与承上启下信息供对照查看。
            </v-alert>
            <EmptyState
              v-if="!hasSkeleton"
              title="先生成镜头骨架"
              description="章节工作区的多 scene 写作现在依赖 AI 骨架。先生成镜头骨架，再进入当前镜头生成正文。"
              icon="mdi-file-tree-outline"
            />
            <template v-else>
              <template v-if="isSceneMode">
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
                <div class="chapter-workspace-meta-row text-body-2 text-medium-emphasis mt-2">
                  <span>镜头来源：</span>
                  <span>{{ currentScene?.source || '未标记' }}</span>
                </div>
                <div class="text-caption text-medium-emphasis mt-3">
                  {{ currentSceneSequenceHint }}
                </div>
              </template>
              <template v-else>
                <div class="chapter-workspace-meta-row text-body-2 text-medium-emphasis">
                  <span>当前 scene：</span>
                  <span class="scene-token">{{ sceneId || '未选择' }}</span>
                  <span>镜头来源：{{ currentScene?.source || '未标记' }}</span>
                </div>
                <div class="text-caption text-medium-emphasis mt-3">
                  当前章节已经脱离 scene 草稿主链，scene 面板只保留骨架顺序和已接纳历史供对照查看。
                </div>
              </template>
              <v-alert v-if="draftMessage" type="success" variant="tonal" class="mt-4">
                {{ draftMessage }}
              </v-alert>
              <v-alert v-if="draftError" type="error" variant="tonal" class="mt-4">
                {{ draftError }}
              </v-alert>
              <v-alert v-if="rollbackMessage" type="success" variant="tonal" class="mt-4">
                {{ rollbackMessage }}
              </v-alert>
              <v-alert v-if="rollbackError" type="error" variant="tonal" class="mt-4">
                {{ rollbackError }}
              </v-alert>
              <v-alert
                v-if="!chapterCompleted && !currentSceneUnlocked"
                type="info"
                variant="tonal"
                class="mt-4"
              >
                当前只能继续处理 {{ unlockedSceneId }}。已接纳镜头可以回看，但不能从中间开始生成。
              </v-alert>
              <v-alert
                v-if="chapterCompleted"
                type="success"
                variant="tonal"
                class="mt-4"
              >
                当前章节所有镜头都已接纳完成。你仍然可以回看骨架和已完成镜头，但不会再解锁新的 scene。
              </v-alert>

              <v-divider class="my-4" />

              <template v-if="isSceneMode">
                <AIProcessLogPanel
                  :logs="displaySceneDraftLogs"
                  :loading="displaySceneDraftGenerating"
                  title="当前镜头生成流水线"
                />

                <v-divider class="my-4" />

                <div class="text-subtitle-2 font-weight-medium">当前写手 Brief</div>
                <div class="text-body-2 mt-2">
                  {{ sessionPreview?.writerExecutionBrief?.goal || '当前镜头还没有写手目标。' }}
                </div>
                <div class="text-caption text-medium-emphasis mt-3">
                  停点：{{ sessionPreview?.writerExecutionBrief?.stopCondition || '未定义' }} ·
                  目标字数：{{ sessionPreview?.writerExecutionBrief?.targetWords || 0 }}
                </div>
                <div
                  v-if="sessionPreview?.writerExecutionBrief?.previousSceneSummary"
                  class="text-body-2 mt-3"
                >
                  上一镜头摘要：{{ sessionPreview?.writerExecutionBrief?.previousSceneSummary }}
                </div>
                <div
                  v-if="sessionPreview?.writerExecutionBrief?.handoffLine"
                  class="text-body-2 mt-2"
                >
                  上一镜头交接：{{ sessionPreview?.writerExecutionBrief?.handoffLine }}
                </div>
                <div
                  v-if="sessionPreview?.writerExecutionBrief?.continuityState?.carryForwardFacts?.length"
                  class="text-body-2 mt-2"
                >
                  必须继承事实：{{ sessionPreview?.writerExecutionBrief?.continuityState?.carryForwardFacts?.join('；') }}
                </div>
                <div
                  v-if="sessionPreview?.writerExecutionBrief?.continuityState?.timeAnchors?.length"
                  class="text-body-2 mt-2"
                >
                  时间锚点：{{ sessionPreview?.writerExecutionBrief?.continuityState?.timeAnchors?.join('；') }}
                </div>
                <div
                  v-if="sessionPreview?.writerExecutionBrief?.continuityState?.counterpartNames?.length || sessionPreview?.writerExecutionBrief?.continuityState?.expectedNames?.length"
                  class="text-body-2 mt-2"
                >
                  沿用人物称呼：
                  {{
                    (sessionPreview?.writerExecutionBrief?.continuityState?.counterpartNames?.length
                      ? sessionPreview?.writerExecutionBrief?.continuityState?.counterpartNames
                      : sessionPreview?.writerExecutionBrief?.continuityState?.expectedNames || []).join('、')
                  }}
                </div>
                <div
                  v-if="sessionPreview?.writerExecutionBrief?.nextSceneId && sessionPreview?.writerExecutionBrief?.nextSceneGoal"
                  class="text-caption text-medium-emphasis mt-3"
                >
                  下一镜头入口预留：{{ sessionPreview?.writerExecutionBrief?.nextSceneId }} ·
                  {{ sessionPreview?.writerExecutionBrief?.nextSceneGoal }}
                </div>

                <div class="text-subtitle-2 font-weight-medium mt-5">镜头顺序状态</div>
                <div class="text-body-2 mt-2">
                  {{ currentSceneSequenceHint }}
                </div>
                <div class="text-caption text-medium-emphasis mt-3">
                  下一镜头：{{ sessionPreview?.writerExecutionBrief?.nextSceneId || '当前镜头已是本章收束点' }}
                </div>
                <div
                  v-if="hasAcceptedScenes"
                  class="text-caption text-medium-emphasis mt-2"
                >
                  当前已接纳镜头：{{ completedSceneIds.join('、') }}
                </div>

                <template v-if="displaySceneDraftRecord || displaySceneDraftGenerating || displaySceneDraftContent">
                  <v-divider class="my-4" />
                  <div class="d-flex justify-space-between align-center ga-3">
                    <div class="text-subtitle-2 font-weight-medium">当前镜头草稿</div>
                    <div class="d-flex ga-2 flex-wrap">
                      <v-btn
                        color="primary"
                        variant="tonal"
                        prepend-icon="mdi-check"
                        :loading="draftLoading"
                        :disabled="!isSceneMode || !displaySceneDraftRecord || !currentSceneUnlocked"
                        @click="acceptSceneDraft"
                      >
                        接受写回正文
                      </v-btn>
                      <v-btn
                        color="error"
                        variant="outlined"
                        prepend-icon="mdi-close"
                        :loading="draftLoading"
                        :disabled="!isSceneMode || !displaySceneDraftRecord || !currentSceneUnlocked"
                        @click="rejectSceneDraft"
                      >
                        拒绝草稿
                      </v-btn>
                    </div>
                  </div>
                  <div class="text-caption text-medium-emphasis mt-2">
                    {{
                      [
                        sceneId ? `镜头 ${sceneId}` : '',
                        displaySceneDraftRecord?.writingType || (displaySceneDraftGenerating ? '生成中' : ''),
                        displaySceneDraftRecord?.selectedModel || currentSceneStreamState.selectedModel || '',
                      ]
                        .filter(Boolean)
                        .join(' · ')
                    }}
                  </div>
                  <div class="text-caption text-medium-emphasis mt-2">
                    只有接纳草稿才会完成当前镜头并解锁下一镜头；拒绝草稿不会推进镜头状态。
                  </div>
                  <MarkdownContent class="mt-4" :content="displaySceneDraftContent" />
                </template>
              </template>
              <template v-else>
                <div class="text-subtitle-2 font-weight-medium">已接纳 scene 前缀</div>
                <div class="text-body-2 mt-2">
                  {{ hasAcceptedScenes ? completedSceneIds.join('、') : '当前还没有已接纳的 scene 记录。' }}
                </div>
                <div class="text-caption text-medium-emphasis mt-3">
                  当前可见的 skeleton 仍可用于校对节点顺序，但后续正文推进不再依赖 scene brief。
                </div>
              </template>
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
              :subtitle="item.message || item.summary || '当前没有补充说明。'"
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

.chapter-workspace-scene-button:disabled {
  cursor: not-allowed;
}

.chapter-workspace-scene-button--active {
  border-color: rgba(30, 77, 120, 0.28);
  box-shadow: inset 0 0 0 1px rgba(30, 77, 120, 0.14);
}

.chapter-workspace-scene-button--locked {
  opacity: 0.68;
}

.chapter-workspace-node-card {
  cursor: default;
}

.chapter-workspace-node-card--active {
  border-color: rgba(30, 77, 120, 0.28);
  box-shadow: inset 0 0 0 1px rgba(30, 77, 120, 0.14);
}

.chapter-workspace-node-option {
  padding: 6px 0;
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

.chapter-content-editor :deep(.v-textarea textarea) {
  max-height: 420px;
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
