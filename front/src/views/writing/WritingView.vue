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
    draft: '生成初稿',
    continue: '继续写作',
    expand: '扩写正文',
    polish: '润色正文',
    rewrite: '重写正文',
  }
  return mapping[resolvedWritingType.value] || '发起生成'
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
    parts.push(`本章必出人物：${currentRequiredCharacters.value.join('、')}`)
  }

  if (draftForm.userInstruction.trim()) {
    parts.push(`补充要求：\n${draftForm.userInstruction.trim()}`)
  }

  return parts.filter(Boolean).join('\n\n')
}

function getWritingTypeLabel(value: string) {
  const mapping: Record<string, string> = {
    draft: '初稿',
    continue: '续写',
    polish: '润色',
    expand: '扩写',
    rewrite: '重写',
  }
  return mapping[value] || value
}

function getRecordStatusLabel(value?: string) {
  const mapping: Record<string, string> = {
    draft: '草稿',
    accepted: '已采纳',
    rejected: '已拒绝',
  }
  return mapping[value || ''] || value || '草稿'
}

function getProviderName(providerId?: number | null) {
  return providerStore.providers.find((item) => item.id === providerId)?.name || '自动选择'
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
    errorMessage.value = error instanceof Error ? error.message : 'AI 生成失败'
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
  lastGeneratedRecord.value = writingStore.records[0] || null
  if (projectId.value && chapterStore.currentChapter?.id === record.chapterId) {
    await chapterStore.fetchDetail(projectId.value, record.chapterId).catch(() => undefined)
  }
}

async function rejectRecord(record: AIWritingRecord) {
  await writingStore.reject(record.id)
  lastGeneratedRecord.value = writingStore.records[0] || null
}
</script>

<template>
  <PageContainer
    title="写作中心"
    description="在这里发起受控写作流程，并通过过程日志和背景聊天持续补充上下文。"
  >
    <EmptyState
      v-if="!projectId"
      title="请先选择项目"
      description="写作中心需要先确定当前项目和章节，才能开始生成。"
    />

    <div v-else class="writing-layout">
      <div class="writing-main-stack">
        <v-card class="soft-panel">
          <v-card-title>章节编辑</v-card-title>
          <v-card-text>
            <v-select
              label="当前章节"
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
                必出：{{ name }}
              </v-chip>
            </div>

            <div class="mt-4">
              <MarkdownEditor
                v-model="currentChapterContent"
                label="章节正文"
                :rows="18"
                :disabled="!chapterStore.currentChapter"
                preview-empty-text="当前还没有正文内容。"
              />
            </div>

            <div class="d-flex justify-space-between align-center mt-4">
              <div class="text-body-2 text-medium-emphasis">当前字数：{{ currentWordCount }}</div>
              <v-btn
                color="primary"
                variant="outlined"
                :disabled="!projectId || !chapterStore.currentChapter?.id"
                @click="saveCurrentChapter"
              >
                保存正文
              </v-btn>
            </div>
          </v-card-text>
        </v-card>

        <v-card class="soft-panel">
          <v-card-title>生成预览</v-card-title>
          <v-card-text>
            <v-progress-linear
              v-if="displayGenerating"
              indeterminate
              color="primary"
              class="mb-4"
            />

            <div v-if="displayStreamingContent" class="stream-preview">
              <MarkdownContent :source="displayStreamingContent" empty-text="暂时还没有生成内容。" />
            </div>
            <div v-else class="text-medium-emphasis">
              最终正文会显示在这里。右侧过程日志会先展示规划、写作、自检和修订进度，不会让页面看起来像卡住。
            </div>

            <div v-if="displayLastGeneratedRecord" class="text-caption text-medium-emphasis mt-3">
              最近一次生成：{{ formatDateTime(displayLastGeneratedRecord.createTime) }}
            </div>
          </v-card-text>
        </v-card>

        <v-card class="soft-panel">
          <v-card-title>最近生成记录</v-card-title>
          <v-list v-if="writingStore.records.length" lines="three">
            <v-list-item
              v-for="record in writingStore.records"
              :key="record.id"
              :title="`${getWritingTypeLabel(record.writingType)} · ${getRecordStatusLabel(record.status)}`"
            >
              <template #subtitle>
                <div class="mt-2">
                  <MarkdownContent :source="record.generatedContent" empty-text="暂无生成内容。" compact />
                </div>
              </template>
              <template #append>
                <div class="d-flex flex-column align-end ga-2">
                  <div class="d-flex ga-2">
                    <v-chip size="small" variant="tonal">{{ getProviderName(record.selectedProviderId) }}</v-chip>
                    <v-chip size="small" color="primary" variant="tonal">
                      {{ record.selectedModel || '自动选择' }}
                    </v-chip>
                  </div>
                  <span class="text-caption text-medium-emphasis">
                    {{ formatDateTime(record.createTime) }}
                  </span>
                  <div class="d-flex ga-2">
                    <v-btn size="small" color="primary" variant="text" @click="acceptRecord(record)">
                      采纳
                    </v-btn>
                    <v-btn size="small" color="error" variant="text" @click="rejectRecord(record)">
                      拒绝
                    </v-btn>
                  </div>
                </div>
              </template>
            </v-list-item>
          </v-list>
          <v-card-text v-else class="text-medium-emphasis">
            还没有 AI 生成记录。
          </v-card-text>
        </v-card>
      </div>

      <div class="writing-side-stack">
        <v-card class="soft-panel">
          <v-card-title>生成设置</v-card-title>
          <v-card-text>
            <v-select
              v-model="draftForm.writingType"
              label="任务类型"
              :items="[
                { title: '生成初稿', value: 'draft' },
                { title: '续写正文', value: 'continue' },
                { title: '润色正文', value: 'polish' },
                { title: '扩写正文', value: 'expand' },
                { title: '重写正文', value: 'rewrite' },
              ]"
              item-title="title"
              item-value="value"
            />

            <v-row class="mt-1">
              <v-col cols="12" md="6">
                <v-select
                  v-model="draftForm.selectedProviderId"
                  label="模型服务"
                  :items="enabledProviders"
                  item-title="name"
                  item-value="id"
                />
              </v-col>
              <v-col cols="12" md="6">
                <v-combobox
                  v-model="draftForm.selectedModel"
                  label="对话模型"
                  :items="providerModelOptions"
                  clearable
                />
              </v-col>
            </v-row>

            <div class="mt-4">
              <div class="d-flex justify-space-between align-center ga-3">
                <div>
                  <div class="text-subtitle-2 font-weight-medium">输出长度</div>
                  <div class="text-caption text-medium-emphasis">
                    {{ outputProfile.description }}，推荐值 {{ outputProfile.recommended }}
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
                <span>短：{{ outputProfile.min }}</span>
                <span>推荐：{{ outputProfile.recommended }}</span>
                <span>长：{{ outputProfile.max }}</span>
              </div>
            </div>

            <v-alert type="info" variant="tonal" class="mt-4">
              当前服务：{{ selectedProvider?.name || '未选择' }} | 当前模型：
              {{ draftForm.selectedModel || '未选择' }}
            </v-alert>

            <v-alert v-if="isCurrentChapterEmpty" type="info" variant="tonal" class="mt-4">
              当前章节还是空的，所以续写和扩写会自动回退成初稿模式。
            </v-alert>

            <v-alert v-if="currentRequiredCharacters.length" type="info" variant="tonal" class="mt-4">
              本章必出人物会自动带入生成上下文。
            </v-alert>

            <v-alert v-if="isOllamaSelected" type="success" variant="tonal" class="mt-4">
              当前使用 Ollama，生成过程中会优先以流式方式逐步显示正文。
            </v-alert>

            <v-textarea
              :model-value="currentPromptTemplate"
              rows="5"
              label="当前提示词模板"
              class="mt-4"
              readonly
            />

            <v-textarea
              v-model="draftForm.userInstruction"
              rows="5"
              label="补充要求"
              class="mt-4"
              placeholder="可以补充场景目标、语气、视角、节奏或其他约束。"
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
          title="工作流日志"
        />

        <AIWritingChatPanel
          :chapter-id="chapterId"
          :selected-provider-id="draftForm.selectedProviderId"
          :selected-model="draftForm.selectedModel"
          entry-point="writing-center"
          :disabled="!chapterId"
          title="背景聊天"
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
