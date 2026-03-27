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
  { title: 'Order', key: 'orderNum', width: 88 },
  { title: 'Title', key: 'title' },
  { title: 'Words', key: 'wordCount', width: 100 },
  { title: 'Actions', key: 'actions', sortable: false, width: 188 },
]

const toneOptions = ['Tense', 'Casual', 'Mystery', 'Intense', 'Melancholy', 'Epic']

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
    draft: 'Draft',
    continue: 'Continue',
    expand: 'Expand',
    rewrite: 'Rewrite',
    polish: 'Polish',
  }
  return mapping[value || ''] || value || 'Draft'
}

function getStatusLabel(value?: string) {
  const mapping: Record<string, string> = {
    draft: 'Draft',
    accepted: 'Accepted',
    rejected: 'Rejected',
  }
  return mapping[value || ''] || value || 'Draft'
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
        'Generate a strong chapter title for the current project.',
      extraRequirements: `Current order: chapter ${Number(form.orderNum) || 1}. Return titles that fit long-form Chinese serial fiction.`,
      count: 6,
    })
    nameSuggestions.value = result.suggestions || []
    nameSuggestionSourceLabel.value = `Model: ${result.providerName || 'Unknown provider'} / ${result.modelName || 'Unknown model'}`
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
    chapterAiForm.sceneGoal.trim() && `Scene goal: ${chapterAiForm.sceneGoal.trim()}`,
    chapterAiForm.tone.trim() && `Tone: ${chapterAiForm.tone.trim()}`,
    chapterAiForm.characterVoice.trim() && `Character voice: ${chapterAiForm.characterVoice.trim()}`,
    chapterAiForm.extraInstruction.trim() && `Extra notes: ${chapterAiForm.extraInstruction.trim()}`,
    currentPreview.value?.requiredCharacterNames?.length &&
      `Required characters: ${currentPreview.value.requiredCharacterNames.join(', ')}`,
  ].filter(Boolean)

  const actionInstruction: Record<typeof action, string> = {
    draft: 'Create a workable first draft for this chapter.',
    continue: 'Continue naturally from the current prose without repeating earlier content.',
    expand: 'Expand the current prose while preserving established facts and events.',
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
    chapterAiError.value = error instanceof Error ? error.message : 'AI draft generation failed.'
  } finally {
    chapterAiGenerating.value = false
  }
}

async function acceptLatestAiRecord() {
  if (!currentProjectId.value || !currentPreview.value?.id || !displayChapterAiLatestRecord.value?.id) {
    return
  }

  const updated = await writingStore.accept(displayChapterAiLatestRecord.value.id)
  chapterAiLatestRecord.value = updated
  await chapterStore.fetchDetail(currentProjectId.value, currentPreview.value.id)
  fillLatestRecordFromStore()
}

async function rejectLatestAiRecord() {
  if (!displayChapterAiLatestRecord.value?.id) {
    return
  }

  await writingStore.reject(displayChapterAiLatestRecord.value.id)
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
    title="Chapter Management"
    description="Manage chapter order and prose, then generate a first draft with its own model defaults, live workflow log, and reusable background chat."
  >
    <template #actions>
      <v-btn color="primary" prepend-icon="mdi-plus" :disabled="!currentProjectId" @click="openCreate">
        New chapter
      </v-btn>
    </template>

    <EmptyState
      v-if="!currentProjectId"
      title="Select a project first"
      description="Chapters load after a project is selected from the left-side project switcher."
    />

    <EmptyState
      v-else-if="!chapterStore.chapters.length"
      title="No chapters yet"
      description="Create the first chapter to start writing, plotting, and AI-assisted drafting."
    >
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openCreate">Create chapter</v-btn>
    </EmptyState>

    <div v-else class="chapter-layout">
      <v-card class="soft-panel chapter-list-card">
        <v-card-title>Chapter List</v-card-title>
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
              <v-btn size="small" variant="text" @click.stop="selectChapter(item)">Preview</v-btn>
              <v-btn size="small" variant="text" @click.stop="openEdit(item)">Edit</v-btn>
              <v-btn size="small" color="error" variant="text" @click.stop="requestDelete(item)">Delete</v-btn>
            </div>
          </template>
        </v-data-table>
      </v-card>

      <div class="chapter-preview-stack">
        <v-card class="soft-panel chapter-preview-card">
          <v-card-title>Chapter Preview</v-card-title>
          <v-card-text v-if="currentPreview" class="chapter-preview-body">
            <div class="text-h6">{{ currentPreview.title }}</div>
            <div class="text-body-2 text-medium-emphasis mt-2">
              Order: {{ currentPreview.orderNum || '-' }} | Words: {{ getWordCount(currentPreview) }}
            </div>
            <div v-if="currentPreview.requiredCharacterNames?.length" class="d-flex flex-wrap ga-2 mt-3">
              <v-chip
                v-for="name in currentPreview.requiredCharacterNames"
                :key="name"
                size="small"
                color="secondary"
                variant="tonal"
              >
                Required: {{ name }}
              </v-chip>
            </div>
            <v-divider class="my-4" />
            <div class="chapter-preview-content">
              <MarkdownContent
                :source="currentPreview.content"
                empty-text="No prose yet. Use the assistant below to generate a first draft."
              />
            </div>
          </v-card-text>
          <v-card-text v-else class="text-medium-emphasis">
            Select a chapter to preview its content.
          </v-card-text>
        </v-card>

        <v-card v-if="currentPreview" class="soft-panel">
          <v-card-title>Draft Assistant</v-card-title>
          <v-card-subtitle>
            Provider: {{ selectedDraftProvider?.name || 'Not selected' }} | Model:
            {{ chapterAiForm.selectedModel || 'Not selected' }}
          </v-card-subtitle>
          <v-card-text class="pt-4">
            <v-row>
              <v-col cols="12" md="6">
                <v-select
                  v-model="chapterAiForm.selectedProviderId"
                  label="Provider"
                  :items="enabledProviders"
                  item-title="name"
                  item-value="id"
                />
              </v-col>
              <v-col cols="12" md="6">
                <v-combobox
                  v-model="chapterAiForm.selectedModel"
                  label="Model"
                  :items="providerModelOptions"
                  clearable
                />
              </v-col>
              <v-col cols="12" md="6">
                <v-text-field
                  v-model="chapterAiForm.sceneGoal"
                  label="Scene goal"
                  placeholder="For example: reveal the key clue for the first time."
                />
              </v-col>
              <v-col cols="12" md="6">
                <v-select
                  v-model="chapterAiForm.tone"
                  :items="toneOptions"
                  label="Tone"
                  clearable
                />
              </v-col>
              <v-col cols="12">
                <v-text-field
                  v-model="chapterAiForm.characterVoice"
                  label="Character voice"
                  placeholder="For example: controlled but sharp, calm and observant."
                />
              </v-col>
              <v-col cols="12">
                <v-textarea
                  v-model="chapterAiForm.extraInstruction"
                  rows="4"
                  label="Extra requirements"
                  placeholder="Pacing, POV, foreshadowing, scene constraints, or style notes."
                />
              </v-col>
              <v-col cols="12">
                <div class="d-flex justify-space-between align-center ga-3">
                  <div>
                    <div class="text-subtitle-2 font-weight-medium">Output length</div>
                    <div class="text-caption text-medium-emphasis">
                      {{ chapterAiProfile.description }}, recommended {{ chapterAiProfile.recommended }}
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
                  <span>Short: {{ chapterAiProfile.min }}</span>
                  <span>Recommended: {{ chapterAiProfile.recommended }}</span>
                  <span>Long: {{ chapterAiProfile.max }}</span>
                </div>
              </v-col>
              <v-col cols="12" class="d-flex flex-wrap align-end ga-2">
                <v-btn
                  color="primary"
                  :loading="displayChapterAiGenerating"
                  @click="generateChapterAiContent('draft')"
                >
                  Generate draft
                </v-btn>
                <v-btn
                  variant="outlined"
                  :loading="displayChapterAiGenerating"
                  @click="generateChapterAiContent('continue')"
                >
                  {{ currentPreviewHasContent ? 'Continue prose' : 'Fallback to draft' }}
                </v-btn>
                <v-btn
                  variant="outlined"
                  :loading="displayChapterAiGenerating"
                  @click="generateChapterAiContent('expand')"
                >
                  {{ currentPreviewHasContent ? 'Expand prose' : 'Generate expandable draft' }}
                </v-btn>
              </v-col>
            </v-row>

            <v-alert v-if="!currentPreviewHasContent" type="info" variant="tonal" class="mt-4">
              This chapter is empty, so continue and expand will automatically fall back to draft mode.
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
                <MarkdownContent :source="displayChapterAiStreamingContent" empty-text="No generated content." />
              </div>
              <div v-else class="text-medium-emphasis">
                Generated prose will appear here. The workflow log stays visible even before the final text arrives, so the page feels responsive instead of stuck.
              </div>
            </div>

            <div v-if="displayChapterAiLatestRecord" class="mt-4">
              <div class="d-flex justify-space-between align-center ga-3">
                <div>
                  <div class="text-subtitle-2 font-weight-medium">
                    Latest result: {{ getWritingTypeLabel(displayChapterAiLatestRecord.writingType) }}
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
                    Accept into chapter
                  </v-btn>
                  <v-btn
                    size="small"
                    color="error"
                    variant="text"
                    :disabled="displayChapterAiLatestRecord.status === 'rejected'"
                    @click="rejectLatestAiRecord"
                  >
                    Reject
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
          title="Draft Workflow Log"
        />

        <AIWritingChatPanel
          v-if="currentPreview"
          :chapter-id="currentPreview.id"
          :selected-provider-id="chapterAiForm.selectedProviderId"
          :selected-model="chapterAiForm.selectedModel"
          entry-point="draft"
          :disabled="!currentPreview"
          title="Draft Background Chat"
        />
      </div>
    </div>

    <v-dialog v-model="dialog" max-width="760">
      <v-card>
        <v-card-title>{{ editingId ? 'Edit chapter' : 'Create chapter' }}</v-card-title>
        <v-card-text class="pt-4">
          <v-row>
            <v-col cols="12" md="8">
              <div class="d-flex ga-2 align-start">
                <v-text-field v-model="form.title" class="flex-grow-1" label="Chapter title" />
                <v-btn class="mt-2" variant="outlined" @click="generateTitleSuggestions">
                  AI title ideas
                </v-btn>
              </div>
            </v-col>
            <v-col cols="12" md="4">
              <v-text-field v-model="form.orderNum" type="number" label="Chapter order" />
            </v-col>
            <v-col cols="12">
              <MarkdownEditor
                v-model="form.content"
                label="Chapter content"
                :rows="10"
                preview-empty-text="No prose yet."
              />
            </v-col>
            <v-col cols="12">
              <v-select
                v-model="form.requiredCharacterIds"
                label="Required characters for this chapter"
                :items="characterStore.characters"
                item-title="name"
                item-value="id"
                multiple
                chips
                closable-chips
                clearable
                hint="These characters will be injected as hard constraints during AI generation."
                persistent-hint
              />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions class="justify-end">
          <v-btn variant="text" @click="dialog = false">Cancel</v-btn>
          <v-btn color="primary" @click="submit">Save</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <ConfirmDialog
      v-model="confirmVisible"
      title="Delete chapter"
      text="Are you sure you want to delete this chapter? It will disappear from both chapter management and the writing center."
      @confirm="confirmDelete"
    />

    <NameSuggestionDialog
      v-model="nameSuggestionDialog"
      title="Choose a chapter title"
      :loading="nameSuggestionLoading"
      :suggestions="nameSuggestions"
      :source-label="nameSuggestionSourceLabel"
      empty-text="No title suggestions came back this time. Try again with a stronger content brief."
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
