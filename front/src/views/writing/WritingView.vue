<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import AIProcessLogPanel from '@/components/AIProcessLogPanel.vue'
import AIWritingChatPanel from '@/components/AIWritingChatPanel.vue'
import EmptyState from '@/components/EmptyState.vue'
import MarkdownContent from '@/components/MarkdownContent.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import PageContainer from '@/components/PageContainer.vue'
import { useChapterStore } from '@/stores/chapter'
import { useProjectStore } from '@/stores/project'
import { useProviderStore } from '@/stores/provider'
import { useSettingsStore } from '@/stores/settings'
import { useWritingStore } from '@/stores/writing'
import type { AIWritingRecord } from '@/types'
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
const writingStore = useWritingStore()
const settingsStore = useSettingsStore()
const providerStore = useProviderStore()

const generating = ref(false)
const errorMessage = ref('')
const streamingContent = ref('')
const lastGeneratedRecord = ref<AIWritingRecord | null>(null)
const lastModelSignature = ref('')

const draftForm = reactive({
  writingType: 'draft',
  userInstruction: '',
  maxTokens: 700,
  selectedProviderId: null as number | null,
  selectedModel: '',
})

const projectId = computed(() => projectStore.selectedProjectId)
const chapterId = computed(() => chapterStore.currentChapter?.id || null)
const isCurrentChapterEmpty = computed(() => !chapterStore.currentChapter?.content?.trim())
const currentRequiredCharacters = computed(() => chapterStore.currentChapter?.requiredCharacterNames || [])
const enabledProviders = computed(() => providerStore.providers.filter((item) => item.enabled === 1))
const selectedProvider = computed(
  () => enabledProviders.value.find((item) => item.id === draftForm.selectedProviderId) || null,
)
const resolvedWritingType = computed(() => {
  if (draftForm.writingType === 'draft') {
    return 'draft'
  }
  if (isCurrentChapterEmpty.value && ['continue', 'expand'].includes(draftForm.writingType)) {
    return 'draft'
  }
  return draftForm.writingType
})
const currentPromptTemplate = computed(() =>
  settingsStore.getPromptTemplateByWritingType(resolvedWritingType.value),
)
const providerModelOptions = computed(() => {
  const values = new Set<string>()
  const provider = selectedProvider.value
  if (provider?.providerType && providerModelLibrary[provider.providerType]) {
    for (const item of providerModelLibrary[provider.providerType]) {
      values.add(item)
    }
  }
  if (provider?.modelName) {
    values.add(provider.modelName)
  }
  const writingModel = settingsStore.getConfigValue('writing_ai_model')
  if (writingModel) {
    values.add(writingModel)
  }
  const defaultModel = settingsStore.getConfigValue('default_ai_model')
  if (defaultModel) {
    values.add(defaultModel)
  }
  if (draftForm.selectedModel) {
    values.add(draftForm.selectedModel)
  }
  return Array.from(values)
})
const outputProfile = computed(() =>
  resolveOutputLengthProfile(
    selectedProvider.value,
    draftForm.selectedModel || selectedProvider.value?.modelName,
  ),
)
const generateButtonLabel = computed(() => {
  const mapping: Record<string, string> = {
    draft: 'Generate Draft',
    continue: 'Continue Chapter',
    expand: 'Expand Chapter',
    polish: 'Polish Chapter',
    rewrite: 'Rewrite Chapter',
  }
  return mapping[resolvedWritingType.value] || 'Generate'
})
const currentWordCount = computed(
  () => chapterStore.currentChapter?.wordCount || chapterStore.currentChapter?.content?.length || 0,
)
const currentChapterContent = computed({
  get: () => chapterStore.currentChapter?.content || '',
  set: (value: string) => {
    if (chapterStore.currentChapter) {
      chapterStore.currentChapter.content = value
    }
  },
})
const isOllamaSelected = computed(() => selectedProvider.value?.providerType === 'ollama')
const sharedStreamState = computed(() => writingStore.getStreamState(chapterId.value))
const displayGenerating = computed(() => sharedStreamState.value.generating || generating.value)
const displayErrorMessage = computed(() => sharedStreamState.value.error || errorMessage.value)
const displayStreamingContent = computed(() => sharedStreamState.value.content || streamingContent.value)
const displayLogs = computed(() => sharedStreamState.value.logs || [])
const displayLastGeneratedRecord = computed<AIWritingRecord | null>(
  () => sharedStreamState.value.lastRecord || lastGeneratedRecord.value || writingStore.records[0] || null,
)

watch(
  projectId,
  async (id) => {
    if (!id) {
      return
    }
    await Promise.allSettled([chapterStore.fetchByProject(id), writingStore.fetchByProject(id)])
  },
  { immediate: true },
)

watch(
  chapterId,
  async (id) => {
    if (!id) {
      return
    }
    await writingStore.fetchByChapter(id).catch(() => undefined)
  },
  { immediate: true },
)

watch(
  [() => settingsStore.configs.length, () => providerStore.providers.length],
  () => {
    applyProviderDefaults()
  },
  { immediate: true },
)

watch(
  [selectedProvider, () => draftForm.selectedModel],
  () => {
    syncMaxTokensWithModel()
  },
  { immediate: true },
)

watch(
  () => draftForm.selectedProviderId,
  () => {
    const provider = selectedProvider.value
    const fallbackModel =
      provider?.modelName ||
      settingsStore.getConfigValue('writing_ai_model') ||
      settingsStore.getConfigValue('default_ai_model', 'qwen3.5:9b')
    if (!draftForm.selectedModel || !providerModelOptions.value.includes(draftForm.selectedModel)) {
      draftForm.selectedModel = fallbackModel
    }
  },
)

watch(
  [() => draftForm.selectedProviderId, () => draftForm.selectedModel],
  () => {
    writeStorage(storageKeys.writingCenterModelPreference, {
      selectedProviderId: draftForm.selectedProviderId,
      selectedModel: draftForm.selectedModel,
    })
  },
)

onMounted(async () => {
  if (!projectStore.projects.length) {
    await projectStore.fetchProjects().catch(() => undefined)
  }
  await Promise.allSettled([settingsStore.fetchAll(), providerStore.fetchAll()])
  applyProviderDefaults()
})

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max)
}

function getPreferredProviderId() {
  const configuredId =
    settingsStore.getNumberValue('writing_ai_provider_id', null) ||
    settingsStore.getNumberValue('default_ai_provider_id', null)
  if (configuredId && enabledProviders.value.some((item) => item.id === configuredId)) {
    return configuredId
  }

  const defaultProvider = enabledProviders.value.find((item) => item.isDefault === 1)
  const ollamaProvider = enabledProviders.value.find((item) => item.providerType === 'ollama')
  return ollamaProvider?.id ?? defaultProvider?.id ?? enabledProviders.value[0]?.id ?? null
}

function applyProviderDefaults() {
  if (!enabledProviders.value.length) {
    return
  }

  const persistedPreference = readStorage<{
    selectedProviderId: number | null
    selectedModel: string
  }>(storageKeys.writingCenterModelPreference, {
    selectedProviderId: null,
    selectedModel: '',
  })

  if (
    !draftForm.selectedProviderId ||
    !enabledProviders.value.some((item) => item.id === draftForm.selectedProviderId)
  ) {
    const persistedProviderId = persistedPreference.selectedProviderId
    draftForm.selectedProviderId =
      persistedProviderId && enabledProviders.value.some((item) => item.id === persistedProviderId)
        ? persistedProviderId
        : getPreferredProviderId()
  }

  if (
    !draftForm.selectedModel ||
    !providerModelOptions.value.includes(draftForm.selectedModel)
  ) {
    draftForm.selectedModel =
      persistedPreference.selectedModel ||
      selectedProvider.value?.modelName ||
      settingsStore.getConfigValue('writing_ai_model') ||
      settingsStore.getConfigValue('default_ai_model', 'qwen3.5:9b')
  }
}

function syncMaxTokensWithModel() {
  const profile = outputProfile.value
  const signature = `${selectedProvider.value?.id || 0}:${draftForm.selectedModel || selectedProvider.value?.modelName || ''}:${profile.max}`
  if (signature !== lastModelSignature.value) {
    lastModelSignature.value = signature
    draftForm.maxTokens = profile.recommended
    return
  }
  draftForm.maxTokens = clamp(draftForm.maxTokens || profile.recommended, profile.min, profile.max)
}

function buildPromptSnapshot() {
  return currentPromptTemplate.value.trim()
}

function buildInstruction() {
  const parts = [buildPromptSnapshot()]

  if (currentRequiredCharacters.value.length) {
    parts.push(`Required characters: ${currentRequiredCharacters.value.join(', ')}`)
  }

  if (draftForm.userInstruction.trim()) {
    parts.push(`Additional requirements:\n${draftForm.userInstruction.trim()}`)
  }

  return parts.filter(Boolean).join('\n\n')
}

function getWritingTypeLabel(value: string) {
  const mapping: Record<string, string> = {
    draft: 'Draft',
    continue: 'Continue',
    polish: 'Polish',
    expand: 'Expand',
    rewrite: 'Rewrite',
  }
  return mapping[value] || value
}

function getRecordStatusLabel(value?: string) {
  const mapping: Record<string, string> = {
    draft: 'Draft',
    accepted: 'Accepted',
    rejected: 'Rejected',
  }
  return mapping[value || ''] || value || 'Draft'
}

function getProviderName(providerId?: number | null) {
  return providerStore.providers.find((item) => item.id === providerId)?.name || 'Auto'
}

async function generate() {
  if (!chapterStore.currentChapter?.id) {
    return
  }

  generating.value = true
  errorMessage.value = ''
  streamingContent.value = ''
  lastGeneratedRecord.value = null

  try {
    const record = await writingStore.generateStream(
      {
        chapterId: chapterStore.currentChapter.id,
        currentContent: chapterStore.currentChapter.content || '',
        userInstruction: buildInstruction(),
        writingType: resolvedWritingType.value,
        maxTokens: draftForm.maxTokens,
        selectedProviderId: draftForm.selectedProviderId,
        selectedModel: draftForm.selectedModel,
        promptSnapshot: buildPromptSnapshot(),
        entryPoint: 'writing-center',
      },
      {
        onEvent: (event) => {
          if (event.type === 'chunk' && event.delta) {
            streamingContent.value += event.delta
          }
        },
      },
    )
    lastGeneratedRecord.value = record
    streamingContent.value = record.generatedContent
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'AI generation failed.'
  } finally {
    generating.value = false
  }
}

async function saveCurrentChapter() {
  if (!projectId.value || !chapterStore.currentChapter?.id) {
    return
  }
  await chapterStore.update(projectId.value, chapterStore.currentChapter.id, {
    content: chapterStore.currentChapter.content,
    title: chapterStore.currentChapter.title,
    orderNum: chapterStore.currentChapter.orderNum,
    requiredCharacterIds: chapterStore.currentChapter.requiredCharacterIds || [],
  })
}

async function acceptRecord(record: AIWritingRecord) {
  await writingStore.accept(record.id)
  if (projectId.value && chapterStore.currentChapter?.id === record.chapterId) {
    await chapterStore.fetchDetail(projectId.value, record.chapterId).catch(() => undefined)
  }
}

async function rejectRecord(record: AIWritingRecord) {
  await writingStore.reject(record.id)
}
</script>

<template>
  <PageContainer
    title="Writing Center"
    description="Generate prose with a controlled workflow, live process logs, and reusable background chat."
  >
    <EmptyState
      v-if="!projectId"
      title="Select a project first"
      description="The writing center needs the current project and chapter before generation can start."
    />

    <div v-else class="writing-layout">
      <div class="writing-main-stack">
        <v-card class="soft-panel">
          <v-card-title>Chapter Editor</v-card-title>
          <v-card-text>
            <v-select
              label="Current chapter"
              item-title="title"
              item-value="id"
              :items="chapterStore.chapters"
              :model-value="chapterId"
              @update:model-value="
                (id) => {
                  const target = chapterStore.chapters.find((item) => item.id === id)
                  if (target) chapterStore.currentChapter = target
                }
              "
            />

            <div v-if="currentRequiredCharacters.length" class="d-flex flex-wrap ga-2 mt-4">
              <v-chip
                v-for="name in currentRequiredCharacters"
                :key="name"
                size="small"
                color="secondary"
                variant="tonal"
              >
                Required: {{ name }}
              </v-chip>
            </div>

            <div class="mt-4">
              <MarkdownEditor
                v-model="currentChapterContent"
                label="Chapter content"
                :rows="18"
                :disabled="!chapterStore.currentChapter"
                preview-empty-text="No chapter content yet."
              />
            </div>

            <div class="d-flex justify-space-between align-center mt-4">
              <div class="text-body-2 text-medium-emphasis">Word count: {{ currentWordCount }}</div>
              <v-btn
                color="primary"
                variant="outlined"
                :disabled="!projectId || !chapterStore.currentChapter?.id"
                @click="saveCurrentChapter"
              >
                Save chapter
              </v-btn>
            </div>
          </v-card-text>
        </v-card>

        <v-card class="soft-panel">
          <v-card-title>Generation Preview</v-card-title>
          <v-card-text>
            <v-progress-linear
              v-if="displayGenerating"
              indeterminate
              color="primary"
              class="mb-4"
            />

            <div v-if="displayStreamingContent" class="stream-preview">
              <MarkdownContent :source="displayStreamingContent" empty-text="No generated content yet." />
            </div>
            <div v-else class="text-medium-emphasis">
              Final prose appears here. The workflow log on the right will keep updating while the model plans, writes, checks, and revises.
            </div>

            <div v-if="displayLastGeneratedRecord" class="text-caption text-medium-emphasis mt-3">
              Last generation: {{ formatDateTime(displayLastGeneratedRecord.createTime) }}
            </div>
          </v-card-text>
        </v-card>

        <v-card class="soft-panel">
          <v-card-title>Recent Generations</v-card-title>
          <v-list v-if="writingStore.records.length" lines="three">
            <v-list-item
              v-for="record in writingStore.records"
              :key="record.id"
              :title="`${getWritingTypeLabel(record.writingType)} · ${getRecordStatusLabel(record.status)}`"
            >
              <template #subtitle>
                <div class="mt-2">
                  <MarkdownContent :source="record.generatedContent" empty-text="No generated content." compact />
                </div>
              </template>
              <template #append>
                <div class="d-flex flex-column align-end ga-2">
                  <div class="d-flex ga-2">
                    <v-chip size="small" variant="tonal">{{ getProviderName(record.selectedProviderId) }}</v-chip>
                    <v-chip size="small" color="primary" variant="tonal">
                      {{ record.selectedModel || 'Auto' }}
                    </v-chip>
                  </div>
                  <span class="text-caption text-medium-emphasis">
                    {{ formatDateTime(record.createTime) }}
                  </span>
                  <div class="d-flex ga-2">
                    <v-btn size="small" color="primary" variant="text" @click="acceptRecord(record)">
                      Accept
                    </v-btn>
                    <v-btn size="small" color="error" variant="text" @click="rejectRecord(record)">
                      Reject
                    </v-btn>
                  </div>
                </div>
              </template>
            </v-list-item>
          </v-list>
          <v-card-text v-else class="text-medium-emphasis">
            No AI generations yet.
          </v-card-text>
        </v-card>
      </div>

      <div class="writing-side-stack">
        <v-card class="soft-panel">
          <v-card-title>Generation Settings</v-card-title>
          <v-card-text>
            <v-select
              v-model="draftForm.writingType"
              label="Task"
              :items="[
                { title: 'Draft', value: 'draft' },
                { title: 'Continue', value: 'continue' },
                { title: 'Polish', value: 'polish' },
                { title: 'Expand', value: 'expand' },
                { title: 'Rewrite', value: 'rewrite' },
              ]"
              item-title="title"
              item-value="value"
            />

            <v-row class="mt-1">
              <v-col cols="12" md="6">
                <v-select
                  v-model="draftForm.selectedProviderId"
                  label="Provider"
                  :items="enabledProviders"
                  item-title="name"
                  item-value="id"
                />
              </v-col>
              <v-col cols="12" md="6">
                <v-combobox
                  v-model="draftForm.selectedModel"
                  label="Model"
                  :items="providerModelOptions"
                  clearable
                />
              </v-col>
            </v-row>

            <div class="mt-4">
              <div class="d-flex justify-space-between align-center ga-3">
                <div>
                  <div class="text-subtitle-2 font-weight-medium">Output length</div>
                  <div class="text-caption text-medium-emphasis">
                    {{ outputProfile.description }}, recommended {{ outputProfile.recommended }}
                  </div>
                </div>
                <v-chip color="primary" variant="tonal">{{ draftForm.maxTokens }}</v-chip>
              </div>

              <v-slider
                v-model="draftForm.maxTokens"
                class="mt-3"
                color="primary"
                thumb-label="always"
                :min="outputProfile.min"
                :max="outputProfile.max"
                :step="outputProfile.step"
              />

              <div class="d-flex justify-space-between text-caption text-medium-emphasis">
                <span>Short: {{ outputProfile.min }}</span>
                <span>Recommended: {{ outputProfile.recommended }}</span>
                <span>Long: {{ outputProfile.max }}</span>
              </div>
            </div>

            <v-alert type="info" variant="tonal" class="mt-4">
              Provider: {{ selectedProvider?.name || 'Not selected' }} | Model:
              {{ draftForm.selectedModel || 'Not selected' }}
            </v-alert>

            <v-alert v-if="isCurrentChapterEmpty" type="info" variant="tonal" class="mt-4">
              This chapter is still empty, so continue and expand will automatically fall back to draft mode.
            </v-alert>

            <v-alert v-if="currentRequiredCharacters.length" type="info" variant="tonal" class="mt-4">
              Required characters will automatically be included in the generation context.
            </v-alert>

            <v-alert v-if="isOllamaSelected" type="success" variant="tonal" class="mt-4">
              Ollama is selected. Streamed prose should appear progressively while generation is running.
            </v-alert>

            <v-textarea
              :model-value="currentPromptTemplate"
              rows="5"
              label="Resolved prompt template"
              class="mt-4"
              readonly
            />

            <v-textarea
              v-model="draftForm.userInstruction"
              rows="5"
              label="Additional requirements"
              class="mt-4"
              placeholder="Scene direction, tone, POV, pacing, or any other constraint."
            />

            <v-alert v-if="displayErrorMessage" type="error" variant="tonal" class="mt-4">
              {{ displayErrorMessage }}
            </v-alert>

            <v-btn
              block
              size="large"
              color="primary"
              class="mt-4"
              :loading="displayGenerating"
              :disabled="!chapterStore.currentChapter?.id || !draftForm.selectedProviderId"
              @click="generate"
            >
              {{ generateButtonLabel }}
            </v-btn>
          </v-card-text>
        </v-card>

        <AIProcessLogPanel
          :logs="displayLogs"
          :loading="displayGenerating"
          title="Workflow Log"
        />

        <AIWritingChatPanel
          :chapter-id="chapterId"
          :selected-provider-id="draftForm.selectedProviderId"
          :selected-model="draftForm.selectedModel"
          entry-point="writing-center"
          :disabled="!chapterId"
          title="Background Chat"
        />
      </div>
    </div>
  </PageContainer>
</template>

<style scoped>
.writing-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(360px, 0.95fr);
  gap: 16px;
  align-items: start;
}

.writing-main-stack,
.writing-side-stack {
  display: grid;
  gap: 16px;
}

.stream-preview {
  max-height: 420px;
  overflow: auto;
  padding: 16px;
  border-radius: 16px;
  background: rgba(var(--v-theme-surface-variant), 0.28);
  white-space: pre-wrap;
  line-height: 1.8;
}

@media (max-width: 1100px) {
  .writing-layout {
    grid-template-columns: 1fr;
  }

  .stream-preview {
    max-height: none;
  }
}
</style>
