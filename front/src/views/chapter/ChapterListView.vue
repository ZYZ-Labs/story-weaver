<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import AIProcessLogPanel from '@/components/AIProcessLogPanel.vue'
import AIWritingChatPanel from '@/components/AIWritingChatPanel.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import MarkdownContent from '@/components/MarkdownContent.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import NameSuggestionDialog from '@/components/NameSuggestionDialog.vue'
import PageContainer from '@/components/PageContainer.vue'
import { generateNameSuggestions } from '@/api/name-suggestion'
import { useCharacterStore } from '@/stores/character'
import { useChapterStore } from '@/stores/chapter'
import { useProjectStore } from '@/stores/project'
import { useProviderStore } from '@/stores/provider'
import { useSettingsStore } from '@/stores/settings'
import { useWritingStore } from '@/stores/writing'
import type { AIWritingRecord, Chapter } from '@/types'
import { resolveOutputLengthProfile } from '@/utils/ai-model'
import { formatDateTime } from '@/utils/format'
import { readStorage, storageKeys, writeStorage } from '@/utils/storage'

const providerModelLibrary: Record<string, string[]> = {
  ollama: ['qwen3.5:9b', 'qwen2.5:14b', 'qwen2.5:7b', 'llama3.1:8b', 'deepseek-r1:14b'],
  'openai-compatible': ['gpt-4.1', 'gpt-4o-mini', 'gpt-4.1-mini'],
  deepseek: ['deepseek-chat', 'deepseek-reasoner'],
}

const projectStore = useProjectStore()
const chapterStore = useChapterStore()
const characterStore = useCharacterStore()
const writingStore = useWritingStore()
const settingsStore = useSettingsStore()
const providerStore = useProviderStore()

const dialog = ref(false)
const deletingId = ref<number | null>(null)
const confirmVisible = ref(false)
const editingId = ref<number | null>(null)
const nameSuggestionDialog = ref(false)
const nameSuggestionLoading = ref(false)
const nameSuggestions = ref<string[]>([])
const nameSuggestionSourceLabel = ref('')
const chapterAiGenerating = ref(false)
const chapterAiError = ref('')
const chapterAiLatestRecord = ref<AIWritingRecord | null>(null)
const chapterAiStreamingContent = ref('')
const lastChapterAiModelSignature = ref('')

const currentProjectId = computed(() => projectStore.selectedProjectId)
const currentPreview = computed(() => chapterStore.currentChapter)
const currentPreviewHasContent = computed(() => Boolean(currentPreview.value?.content?.trim()))
const currentChapterStreamState = computed(() => writingStore.getStreamState(currentPreview.value?.id || null))
const displayChapterAiGenerating = computed(
  () => currentChapterStreamState.value.generating || chapterAiGenerating.value,
)
const displayChapterAiError = computed(() => currentChapterStreamState.value.error || chapterAiError.value)
const displayChapterAiStreamingContent = computed(
  () => currentChapterStreamState.value.content || chapterAiStreamingContent.value,
)
const displayChapterAiLogs = computed(() => currentChapterStreamState.value.logs || [])
const displayChapterAiLatestRecord = computed<AIWritingRecord | null>(
  () => currentChapterStreamState.value.lastRecord || chapterAiLatestRecord.value || writingStore.records[0] || null,
)
const enabledProviders = computed(() => providerStore.providers.filter((item) => item.enabled === 1))
const selectedDraftProvider = computed(
  () => enabledProviders.value.find((item) => item.id === chapterAiForm.selectedProviderId) || null,
)
const providerModelOptions = computed(() => {
  const values = new Set<string>()
  const provider = selectedDraftProvider.value
  if (provider?.providerType && providerModelLibrary[provider.providerType]) {
    for (const item of providerModelLibrary[provider.providerType]) {
      values.add(item)
    }
  }
  if (provider?.modelName) {
    values.add(provider.modelName)
  }
  const draftModel = settingsStore.getConfigValue('draft_ai_model')
  if (draftModel) {
    values.add(draftModel)
  }
  const defaultModel = settingsStore.getConfigValue('default_ai_model')
  if (defaultModel) {
    values.add(defaultModel)
  }
  if (chapterAiForm.selectedModel) {
    values.add(chapterAiForm.selectedModel)
  }
  return Array.from(values)
})
const chapterAiProfile = computed(() =>
  resolveOutputLengthProfile(
    selectedDraftProvider.value,
    chapterAiForm.selectedModel || selectedDraftProvider.value?.modelName,
  ),
)

const tableHeaders = [
  { title: '顺序', key: 'orderNum', width: 88 },
  { title: '标题', key: 'title' },
  { title: '字数', key: 'wordCount', width: 100 },
  { title: '操作', key: 'actions', sortable: false, width: 188 },
]

const toneOptions = ['紧张压迫', '轻松日常', '神秘悬疑', '高压推进', '克制伤感', '史诗宏大']

const form = reactive({
  title: '',
  content: '',
  orderNum: 1,
  requiredCharacterIds: [] as number[],
})

const chapterAiForm = reactive({
  sceneGoal: '',
  tone: '',
  characterVoice: '',
  extraInstruction: '',
  maxTokens: 900,
  selectedProviderId: null as number | null,
  selectedModel: '',
})

watch(
  currentProjectId,
  async (projectId) => {
    if (!projectId) {
      return
    }
    await Promise.allSettled([
      chapterStore.fetchByProject(projectId),
      characterStore.fetchByProject(projectId),
    ])
  },
  { immediate: true },
)

watch(
  () => currentPreview.value?.id,
  async (chapterId) => {
    chapterAiLatestRecord.value = null
    chapterAiError.value = ''
    chapterAiStreamingContent.value = ''

    if (!chapterId) {
      return
    }

    await writingStore.fetchByChapter(chapterId).catch(() => undefined)
    chapterAiLatestRecord.value = writingStore.records[0] || null
  },
  { immediate: true },
)

watch(
  [() => settingsStore.configs.length, () => providerStore.providers.length],
  () => {
    applyDraftProviderDefaults()
  },
  { immediate: true },
)

watch(
  [selectedDraftProvider, () => chapterAiForm.selectedModel],
  () => {
    syncDraftMaxTokens()
  },
  { immediate: true },
)

watch(
  () => chapterAiForm.selectedProviderId,
  () => {
    const provider = selectedDraftProvider.value
    const fallbackModel =
      provider?.modelName ||
      settingsStore.getConfigValue('draft_ai_model') ||
      settingsStore.getConfigValue('default_ai_model', 'qwen3.5:9b')
    if (!chapterAiForm.selectedModel || !providerModelOptions.value.includes(chapterAiForm.selectedModel)) {
      chapterAiForm.selectedModel = fallbackModel
    }
  },
)

watch(
  [() => chapterAiForm.selectedProviderId, () => chapterAiForm.selectedModel],
  () => {
    writeStorage(storageKeys.chapterDraftModelPreference, {
      selectedProviderId: chapterAiForm.selectedProviderId,
      selectedModel: chapterAiForm.selectedModel,
    })
  },
)

onMounted(async () => {
  if (!projectStore.projects.length) {
    await projectStore.fetchProjects().catch(() => undefined)
  }
  await Promise.allSettled([settingsStore.fetchAll(), providerStore.fetchAll()])
  applyDraftProviderDefaults()
})

function fillForm(chapter?: Chapter | null) {
  Object.assign(form, {
    title: chapter?.title || '',
    content: chapter?.content || '',
    orderNum: chapter?.orderNum || chapterStore.chapters.length + 1,
    requiredCharacterIds: [...(chapter?.requiredCharacterIds || [])],
  })
}

function getPreferredDraftProviderId() {
  const configuredId =
    settingsStore.getNumberValue('draft_ai_provider_id', null) ||
    settingsStore.getNumberValue('default_ai_provider_id', null)
  if (configuredId && enabledProviders.value.some((item) => item.id === configuredId)) {
    return configuredId
  }

  const defaultProvider = enabledProviders.value.find((item) => item.isDefault === 1)
  const ollamaProvider = enabledProviders.value.find((item) => item.providerType === 'ollama')
  return ollamaProvider?.id ?? defaultProvider?.id ?? enabledProviders.value[0]?.id ?? null
}

function applyDraftProviderDefaults() {
  if (!enabledProviders.value.length) {
    return
  }

  const persistedPreference = readStorage<{
    selectedProviderId: number | null
    selectedModel: string
  }>(storageKeys.chapterDraftModelPreference, {
    selectedProviderId: null,
    selectedModel: '',
  })

  if (
    !chapterAiForm.selectedProviderId ||
    !enabledProviders.value.some((item) => item.id === chapterAiForm.selectedProviderId)
  ) {
    const persistedProviderId = persistedPreference.selectedProviderId
    chapterAiForm.selectedProviderId =
      persistedProviderId && enabledProviders.value.some((item) => item.id === persistedProviderId)
        ? persistedProviderId
        : getPreferredDraftProviderId()
  }

  if (
    !chapterAiForm.selectedModel ||
    !providerModelOptions.value.includes(chapterAiForm.selectedModel)
  ) {
    chapterAiForm.selectedModel =
      persistedPreference.selectedModel ||
      selectedDraftProvider.value?.modelName ||
      settingsStore.getConfigValue('draft_ai_model') ||
      settingsStore.getConfigValue('default_ai_model', 'qwen3.5:9b')
  }
}

function syncDraftMaxTokens() {
  const profile = chapterAiProfile.value
  const signature = `${selectedDraftProvider.value?.id || 0}:${chapterAiForm.selectedModel || selectedDraftProvider.value?.modelName || ''}:${profile.max}`
  if (signature !== lastChapterAiModelSignature.value) {
    lastChapterAiModelSignature.value = signature
    chapterAiForm.maxTokens = profile.recommended
    return
  }
  chapterAiForm.maxTokens = Math.min(
    Math.max(chapterAiForm.maxTokens || profile.recommended, profile.min),
    profile.max,
  )
}

function openCreate() {
  editingId.value = null
  fillForm(null)
  dialog.value = true
}

function openEdit(chapter: Chapter) {
  editingId.value = chapter.id
  fillForm(chapter)
  dialog.value = true
}

function selectChapter(chapter: Chapter) {
  chapterStore.currentChapter = chapter
}

function requestDelete(chapter: Chapter) {
  deletingId.value = chapter.id
  if (chapterStore.currentChapter?.id !== chapter.id) {
    chapterStore.currentChapter = chapter
  }
  confirmVisible.value = true
}

function getWordCount(chapter: Chapter) {
  if (typeof chapter.wordCount === 'number' && chapter.wordCount > 0) {
    return chapter.wordCount
  }
  return chapter.content?.length || 0
}

function getWritingTypeLabel(value?: string) {
  const mapping: Record<string, string> = {
    draft: '初稿',
    continue: '续写',
    expand: '扩写',
    rewrite: '重写',
    polish: '润色',
  }
  return mapping[value || ''] || value || '初稿'
}

function getStatusLabel(value?: string) {
  const mapping: Record<string, string> = {
    draft: '草稿',
    accepted: '已采纳',
    rejected: '已拒绝',
  }
  return mapping[value || ''] || value || '草稿'
}

function fillLatestRecordFromStore() {
  chapterAiLatestRecord.value = writingStore.records[0] || null
}

async function submit() {
  if (!currentProjectId.value) {
    return
  }

  const payload = {
    ...form,
    orderNum: Number(form.orderNum) || 1,
    requiredCharacterIds: [...form.requiredCharacterIds],
  }

  if (editingId.value) {
    await chapterStore.update(currentProjectId.value, editingId.value, payload)
  } else {
    await chapterStore.create(currentProjectId.value, payload)
  }

  dialog.value = false
}

async function generateTitleSuggestions() {
  if (!currentProjectId.value) {
    return
  }

  nameSuggestionLoading.value = true
  nameSuggestionDialog.value = true

  try {
    const result = await generateNameSuggestions(currentProjectId.value, {
      entityType: 'chapter',
      brief:
        form.content.trim() ||
        form.title.trim() ||
        '请结合当前项目风格，为这个章节生成更贴合连载小说语境的标题。',
      extraRequirements: `当前章节顺序为第 ${Number(form.orderNum) || 1} 章，请返回适合中文长篇连载的章节标题候选。`,
      count: 6,
    })
    nameSuggestions.value = result.suggestions || []
    nameSuggestionSourceLabel.value = `生成模型：${result.providerName || '未知服务'} / ${result.modelName || '未知模型'}`
  } finally {
    nameSuggestionLoading.value = false
  }
}

function applySuggestedTitle(value: string) {
  form.title = value
  nameSuggestionDialog.value = false
}

function buildChapterAiInstruction(action: 'draft' | 'continue' | 'expand') {
  const lines = [
    chapterAiForm.sceneGoal.trim() && `场景目标：${chapterAiForm.sceneGoal.trim()}`,
    chapterAiForm.tone.trim() && `情绪氛围：${chapterAiForm.tone.trim()}`,
    chapterAiForm.characterVoice.trim() && `人物口吻：${chapterAiForm.characterVoice.trim()}`,
    chapterAiForm.extraInstruction.trim() && `补充要求：${chapterAiForm.extraInstruction.trim()}`,
    currentPreview.value?.requiredCharacterNames?.length &&
      `本章必出人物：${currentPreview.value.requiredCharacterNames.join('、')}`,
  ].filter(Boolean)

  const actionInstruction: Record<typeof action, string> = {
    draft: '请先生成一版可继续扩写的章节初稿。',
    continue: '请顺着当前正文自然续写，不要重复已经写过的内容。',
    expand: '请在不改变既有事实的前提下，把当前正文扩写得更完整饱满。',
  }

  lines.unshift(actionInstruction[action])
  return lines.join('\n')
}

function resolveChapterAiWritingType(action: 'draft' | 'continue' | 'expand') {
  if (action === 'draft') {
    return 'draft'
  }
  if (!currentPreview.value?.content?.trim()) {
    return 'draft'
  }
  return action
}

async function generateChapterAiContent(action: 'draft' | 'continue' | 'expand') {
  if (!currentPreview.value?.id) {
    return
  }

  chapterAiGenerating.value = true
  chapterAiError.value = ''
  chapterAiStreamingContent.value = ''
  chapterAiLatestRecord.value = null

  try {
    const writingType = resolveChapterAiWritingType(action)
    const record = await writingStore.generateStream(
      {
        chapterId: currentPreview.value.id,
        currentContent: currentPreview.value.content || '',
        userInstruction: buildChapterAiInstruction(action),
        writingType,
        maxTokens: chapterAiForm.maxTokens,
        selectedProviderId: chapterAiForm.selectedProviderId,
        selectedModel: chapterAiForm.selectedModel,
        promptSnapshot: settingsStore.getPromptTemplateByWritingType(writingType),
        entryPoint: 'draft',
      },
      {
        onEvent: (event) => {
          if (event.type === 'chunk' && event.delta) {
            chapterAiStreamingContent.value += event.delta
          }
        },
      },
    )
    chapterAiLatestRecord.value = record
    chapterAiStreamingContent.value = record.generatedContent
  } catch (error) {
    chapterAiError.value = error instanceof Error ? error.message : 'AI 初稿生成失败'
  } finally {
    chapterAiGenerating.value = false
  }
}

async function acceptLatestAiRecord() {
  if (!currentProjectId.value || !currentPreview.value?.id || !displayChapterAiLatestRecord.value?.id) {
    return
  }

  await writingStore.accept(displayChapterAiLatestRecord.value.id)
  chapterAiLatestRecord.value = writingStore.records[0] || null
  await chapterStore.fetchDetail(currentProjectId.value, currentPreview.value.id)
  fillLatestRecordFromStore()
}

async function rejectLatestAiRecord() {
  if (!displayChapterAiLatestRecord.value?.id) {
    return
  }

  await writingStore.reject(displayChapterAiLatestRecord.value.id)
  chapterAiLatestRecord.value = writingStore.records[0] || null
  fillLatestRecordFromStore()
}

async function confirmDelete() {
  if (!currentProjectId.value || !deletingId.value) {
    return
  }

  await chapterStore.remove(currentProjectId.value, deletingId.value)
  deletingId.value = null
  confirmVisible.value = false
}
</script>

<template>
  <PageContainer
    title="章节管理"
    description="维护章节顺序和正文内容，并在右侧直接生成初稿、查看过程日志、沉淀背景聊天。"
  >
    <template #actions>
      <v-btn color="primary" prepend-icon="mdi-plus" :disabled="!currentProjectId" @click="openCreate">
        新建章节
      </v-btn>
    </template>

    <EmptyState
      v-if="!currentProjectId"
      title="请先选择项目"
      description="选择项目后，这里会自动加载该项目下的章节。"
    />

    <EmptyState
      v-else-if="!chapterStore.chapters.length"
      title="当前还没有章节"
      description="先创建第一章，后续的剧情推进和 AI 初稿都会围绕章节展开。"
    >
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openCreate">创建章节</v-btn>
    </EmptyState>

    <div v-else class="chapter-layout">
      <v-card class="soft-panel chapter-list-card">
        <v-card-title>章节列表</v-card-title>
        <v-data-table
          class="chapter-table"
          :headers="tableHeaders"
          :items="chapterStore.chapters"
          item-value="id"
          hover
          density="comfortable"
        >
          <template #[`item.title`]="{ item }">
            <button class="chapter-row-button" type="button" @click="selectChapter(item)">
              <span class="font-weight-medium">{{ item.title }}</span>
            </button>
          </template>

          <template #[`item.wordCount`]="{ item }">
            {{ getWordCount(item) }}
          </template>

          <template #[`item.actions`]="{ item }">
            <div class="d-flex ga-2 justify-end">
              <v-btn size="small" variant="text" @click.stop="selectChapter(item)">预览</v-btn>
              <v-btn size="small" variant="text" @click.stop="openEdit(item)">编辑</v-btn>
              <v-btn size="small" color="error" variant="text" @click.stop="requestDelete(item)">删除</v-btn>
            </div>
          </template>
        </v-data-table>
      </v-card>

      <div class="chapter-preview-stack">
        <v-card class="soft-panel chapter-preview-card">
          <v-card-title>章节预览</v-card-title>
          <v-card-text v-if="currentPreview" class="chapter-preview-body">
            <div class="text-h6">{{ currentPreview.title }}</div>
            <div class="text-body-2 text-medium-emphasis mt-2">
              顺序：{{ currentPreview.orderNum || '-' }} | 字数：{{ getWordCount(currentPreview) }}
            </div>
            <div v-if="currentPreview.requiredCharacterNames?.length" class="d-flex flex-wrap ga-2 mt-3">
              <v-chip
                v-for="name in currentPreview.requiredCharacterNames"
                :key="name"
                size="small"
                color="secondary"
                variant="tonal"
              >
                必出：{{ name }}
              </v-chip>
            </div>
            <v-divider class="my-4" />
            <div class="chapter-preview-content">
              <MarkdownContent
                :source="currentPreview.content"
                empty-text="当前还没有正文，可以直接在下方让 AI 先生成一版初稿。"
              />
            </div>
          </v-card-text>
          <v-card-text v-else class="text-medium-emphasis">
            请选择一个章节查看内容。
          </v-card-text>
        </v-card>

        <v-card v-if="currentPreview" class="soft-panel">
          <v-card-title>初稿助手</v-card-title>
          <v-card-subtitle>
            当前服务：{{ selectedDraftProvider?.name || '未选择' }} | 当前模型：
            {{ chapterAiForm.selectedModel || '未选择' }}
          </v-card-subtitle>
          <v-card-text class="pt-4">
            <v-row>
              <v-col cols="12" md="6">
                <v-select
                  v-model="chapterAiForm.selectedProviderId"
                  label="模型服务"
                  :items="enabledProviders"
                  item-title="name"
                  item-value="id"
                />
              </v-col>
              <v-col cols="12" md="6">
                <v-combobox
                  v-model="chapterAiForm.selectedModel"
                  label="对话模型"
                  :items="providerModelOptions"
                  clearable
                />
              </v-col>
              <v-col cols="12" md="6">
                <v-text-field
                  v-model="chapterAiForm.sceneGoal"
                  label="场景目标"
                  placeholder="例如：让主角第一次确认关键线索。"
                />
              </v-col>
              <v-col cols="12" md="6">
                <v-select
                  v-model="chapterAiForm.tone"
                  :items="toneOptions"
                  label="情绪氛围"
                  clearable
                />
              </v-col>
              <v-col cols="12">
                <v-text-field
                  v-model="chapterAiForm.characterVoice"
                  label="人物口吻"
                  placeholder="例如：主角冷静克制，配角说话更直接。"
                />
              </v-col>
              <v-col cols="12">
                <v-textarea
                  v-model="chapterAiForm.extraInstruction"
                  rows="4"
                  label="补充要求"
                  placeholder="可以补充节奏、视角、伏笔、场景约束或写法要求。"
                />
              </v-col>
              <v-col cols="12">
                <div class="d-flex justify-space-between align-center ga-3">
                  <div>
                    <div class="text-subtitle-2 font-weight-medium">输出长度</div>
                    <div class="text-caption text-medium-emphasis">
                      {{ chapterAiProfile.description }}，推荐值 {{ chapterAiProfile.recommended }}
                    </div>
                  </div>
                  <v-chip color="primary" variant="tonal">{{ chapterAiForm.maxTokens }}</v-chip>
                </div>
                <v-slider
                  v-model="chapterAiForm.maxTokens"
                  class="mt-3"
                  color="primary"
                  thumb-label="always"
                  :min="chapterAiProfile.min"
                  :max="chapterAiProfile.max"
                  :step="chapterAiProfile.step"
                />
                <div class="d-flex justify-space-between text-caption text-medium-emphasis">
                  <span>短：{{ chapterAiProfile.min }}</span>
                  <span>推荐：{{ chapterAiProfile.recommended }}</span>
                  <span>长：{{ chapterAiProfile.max }}</span>
                </div>
              </v-col>
              <v-col cols="12" class="d-flex flex-wrap align-end ga-2">
                <v-btn
                  color="primary"
                  :loading="displayChapterAiGenerating"
                  @click="generateChapterAiContent('draft')"
                >
                  生成初稿
                </v-btn>
                <v-btn
                  variant="outlined"
                  :loading="displayChapterAiGenerating"
                  @click="generateChapterAiContent('continue')"
                >
                  {{ currentPreviewHasContent ? '续写正文' : '自动回退初稿' }}
                </v-btn>
                <v-btn
                  variant="outlined"
                  :loading="displayChapterAiGenerating"
                  @click="generateChapterAiContent('expand')"
                >
                  {{ currentPreviewHasContent ? '扩写正文' : '先生成可扩写版本' }}
                </v-btn>
              </v-col>
            </v-row>

            <v-alert v-if="!currentPreviewHasContent" type="info" variant="tonal" class="mt-4">
              当前章节正文为空，所以续写和扩写会自动回退成初稿模式。
            </v-alert>

            <v-alert v-if="displayChapterAiError" type="error" variant="tonal" class="mt-4">
              {{ displayChapterAiError }}
            </v-alert>

            <div class="mt-4">
              <v-progress-linear
                v-if="displayChapterAiGenerating"
                indeterminate
                color="primary"
                class="mb-4"
              />

              <div v-if="displayChapterAiStreamingContent" class="chapter-ai-result">
                <MarkdownContent :source="displayChapterAiStreamingContent" empty-text="暂时还没有生成内容。" />
              </div>
              <div v-else class="text-medium-emphasis">
                生成中的正文会显示在这里。右侧过程日志会先展示准备上下文、背景整理、规划、写作、自检和修订进度，不会让页面看起来像卡住。
              </div>
            </div>

            <div v-if="displayChapterAiLatestRecord" class="mt-4">
              <div class="d-flex justify-space-between align-center ga-3">
                <div>
                  <div class="text-subtitle-2 font-weight-medium">
                    最新结果：{{ getWritingTypeLabel(displayChapterAiLatestRecord.writingType) }}
                  </div>
                  <div class="text-caption text-medium-emphasis mt-1">
                    {{ formatDateTime(displayChapterAiLatestRecord.createTime) }} |
                    {{ getStatusLabel(displayChapterAiLatestRecord.status) }}
                  </div>
                </div>
                <div class="d-flex ga-2">
                  <v-btn
                    size="small"
                    color="primary"
                    variant="text"
                    :disabled="displayChapterAiLatestRecord.status === 'accepted'"
                    @click="acceptLatestAiRecord"
                  >
                    采纳到正文
                  </v-btn>
                  <v-btn
                    size="small"
                    color="error"
                    variant="text"
                    :disabled="displayChapterAiLatestRecord.status === 'rejected'"
                    @click="rejectLatestAiRecord"
                  >
                    拒绝
                  </v-btn>
                </div>
              </div>
            </div>
          </v-card-text>
        </v-card>

        <AIProcessLogPanel
          v-if="currentPreview"
          :logs="displayChapterAiLogs"
          :loading="displayChapterAiGenerating"
          title="初稿工作流日志"
        />

        <AIWritingChatPanel
          v-if="currentPreview"
          :chapter-id="currentPreview.id"
          :selected-provider-id="chapterAiForm.selectedProviderId"
          :selected-model="chapterAiForm.selectedModel"
          entry-point="draft"
          :disabled="!currentPreview"
          title="初稿背景聊天"
        />
      </div>
    </div>

    <v-dialog v-model="dialog" max-width="760">
      <v-card>
        <v-card-title>{{ editingId ? '编辑章节' : '新建章节' }}</v-card-title>
        <v-card-text class="pt-4">
          <v-row>
            <v-col cols="12" md="8">
              <div class="d-flex ga-2 align-start">
                <v-text-field v-model="form.title" class="flex-grow-1" label="章节标题" />
                <v-btn class="mt-2" variant="outlined" @click="generateTitleSuggestions">
                  AI 标题建议
                </v-btn>
              </div>
            </v-col>
            <v-col cols="12" md="4">
              <v-text-field v-model="form.orderNum" type="number" label="章节顺序" />
            </v-col>
            <v-col cols="12">
              <MarkdownEditor
                v-model="form.content"
                label="章节正文"
                :rows="10"
                preview-empty-text="当前还没有正文。"
              />
            </v-col>
            <v-col cols="12">
              <v-select
                v-model="form.requiredCharacterIds"
                label="本章必出人物"
                :items="characterStore.characters"
                item-title="name"
                item-value="id"
                multiple
                chips
                closable-chips
                clearable
                hint="这些人物会被当作硬约束自动带入 AI 生成上下文。"
                persistent-hint
              />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions class="justify-end">
          <v-btn variant="text" @click="dialog = false">取消</v-btn>
          <v-btn color="primary" @click="submit">保存</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <ConfirmDialog
      v-model="confirmVisible"
      title="删除章节"
      text="确认删除这个章节吗？删除后，它会同时从章节管理和写作中心中消失。"
      @confirm="confirmDelete"
    />

    <NameSuggestionDialog
      v-model="nameSuggestionDialog"
      title="选择章节标题"
      :loading="nameSuggestionLoading"
      :suggestions="nameSuggestions"
      :source-label="nameSuggestionSourceLabel"
      empty-text="这次没有拿到合适的标题，可以补充更多上下文后再试一次。"
      @refresh="generateTitleSuggestions"
      @select="applySuggestedTitle"
    />
  </PageContainer>
</template>

<style scoped>
.chapter-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(360px, 1fr);
  gap: 16px;
  align-items: start;
}

.chapter-preview-stack {
  display: grid;
  gap: 16px;
}

.chapter-list-card,
.chapter-preview-card {
  overflow: hidden;
}

.chapter-table :deep(.v-table__wrapper) {
  max-height: 620px;
  overflow: auto;
}

.chapter-row-button {
  display: block;
  width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.chapter-preview-body {
  display: grid;
}

.chapter-preview-content {
  max-height: 320px;
  overflow: auto;
  padding-right: 4px;
}

.chapter-ai-result {
  max-height: 300px;
  overflow: auto;
  padding: 16px;
  border-radius: 16px;
  background: rgba(var(--v-theme-surface-variant), 0.28);
}

@media (max-width: 960px) {
  .chapter-layout {
    grid-template-columns: 1fr;
  }

  .chapter-preview-content,
  .chapter-table :deep(.v-table__wrapper),
  .chapter-ai-result {
    max-height: none;
  }
}
</style>
