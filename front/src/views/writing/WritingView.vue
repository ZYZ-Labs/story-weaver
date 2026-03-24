<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import EmptyState from '@/components/EmptyState.vue'
import PageContainer from '@/components/PageContainer.vue'
import { useChapterStore } from '@/stores/chapter'
import { useProjectStore } from '@/stores/project'
import { useProviderStore } from '@/stores/provider'
import { useSettingsStore } from '@/stores/settings'
import { useWritingStore } from '@/stores/writing'
import { formatDateTime } from '@/utils/format'

const providerModelLibrary: Record<string, string[]> = {
  ollama: ['qwen2.5:14b', 'qwen2.5:7b', 'llama3.1:8b', 'deepseek-r1:14b'],
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

const projectId = computed(() => projectStore.selectedProjectId)
const chapterId = computed(() => chapterStore.currentChapter?.id || null)
const enabledProviders = computed(() => providerStore.providers.filter((item) => item.enabled === 1))
const currentPromptTemplate = computed(() => settingsStore.getPromptTemplateByWritingType(draftForm.writingType))
const selectedProvider = computed(() =>
  enabledProviders.value.find((item) => item.id === draftForm.selectedProviderId) || null,
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
  const defaultModel = settingsStore.getConfigValue('default_ai_model')
  if (defaultModel) {
    values.add(defaultModel)
  }
  return Array.from(values)
})
const isOllamaSelected = computed(() => selectedProvider.value?.providerType === 'ollama')

const draftForm = reactive({
  writingType: 'continue',
  userInstruction: '',
  maxTokens: 600,
  selectedProviderId: null as number | null,
  selectedModel: '',
})

watch(
  projectId,
  async (id) => {
    if (id) {
      await Promise.allSettled([
        chapterStore.fetchByProject(id),
        writingStore.fetchByProject(id),
      ])
    }
  },
  { immediate: true },
)

watch(
  chapterId,
  async (id) => {
    if (id) {
      await writingStore.fetchByChapter(id).catch(() => undefined)
    }
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
  () => draftForm.selectedProviderId,
  () => {
    const provider = selectedProvider.value
    const fallbackModel = provider?.modelName || settingsStore.getConfigValue('default_ai_model', 'qwen2.5:14b')
    if (!draftForm.selectedModel || !providerModelOptions.value.includes(draftForm.selectedModel)) {
      draftForm.selectedModel = fallbackModel
    }
  },
)

onMounted(async () => {
  if (!projectStore.projects.length) {
    await projectStore.fetchProjects().catch(() => undefined)
  }
  await Promise.allSettled([settingsStore.fetchAll(), providerStore.fetchAll()])
  applyProviderDefaults()
})

function getPreferredProviderId() {
  const configuredId = settingsStore.getNumberValue('default_ai_provider_id', null)
  if (configuredId && enabledProviders.value.some((item) => item.id === configuredId)) {
    return configuredId
  }

  const defaultProvider = enabledProviders.value.find((item) => item.isDefault === 1)
  const ollamaProvider = enabledProviders.value.find((item) => item.providerType === 'ollama')
  return ollamaProvider?.id ?? defaultProvider?.id ?? enabledProviders.value[0]?.id ?? null
}

function applyProviderDefaults() {
  if (!enabledProviders.value.length) return

  if (!draftForm.selectedProviderId || !enabledProviders.value.some((item) => item.id === draftForm.selectedProviderId)) {
    draftForm.selectedProviderId = getPreferredProviderId()
  }

  const provider = selectedProvider.value
  if (!draftForm.selectedModel) {
    draftForm.selectedModel = provider?.modelName || settingsStore.getConfigValue('default_ai_model', 'qwen2.5:14b')
  }
}

function buildPromptSnapshot() {
  return currentPromptTemplate.value.trim()
}

function buildInstruction() {
  const template = buildPromptSnapshot()
  const manual = draftForm.userInstruction.trim()
  if (template && manual) {
    return `${template}\n\n附加要求：${manual}`
  }
  return template || manual
}

function getWritingTypeLabel(value: string) {
  const mapping: Record<string, string> = {
    continue: '续写',
    polish: '润色',
    expand: '扩写',
    rewrite: '改写',
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
  return providerStore.providers.find((item) => item.id === providerId)?.name || '未指定模型服务'
}

async function generate() {
  if (!chapterStore.currentChapter?.id) return

  generating.value = true
  errorMessage.value = ''
  try {
    await writingStore.generate({
      chapterId: chapterStore.currentChapter.id,
      currentContent: chapterStore.currentChapter.content || '',
      userInstruction: buildInstruction(),
      writingType: draftForm.writingType,
      maxTokens: draftForm.maxTokens,
      selectedProviderId: draftForm.selectedProviderId,
      selectedModel: draftForm.selectedModel,
      promptSnapshot: buildPromptSnapshot(),
    })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'AI 生成失败。'
  } finally {
    generating.value = false
  }
}

async function saveCurrentChapter() {
  if (!projectId.value || !chapterStore.currentChapter?.id) return
  await chapterStore.update(projectId.value, chapterStore.currentChapter.id, {
    content: chapterStore.currentChapter.content,
    title: chapterStore.currentChapter.title,
    orderNum: chapterStore.currentChapter.orderNum,
  })
}
</script>

<template>
  <PageContainer
    title="写作中心"
    description="这里会优先使用 Ollama 模型服务。生成记录会保存所选服务、模型和提示词快照，采纳草稿后还会自动同步到知识库。"
  >
    <EmptyState
      v-if="!projectId"
      title="需要先选择项目"
      description="写作中心依赖当前项目和章节上下文，请先在左侧项目选择器中选中一个项目。"
    />

    <div v-else class="content-grid two-column">
      <v-card class="soft-panel">
        <v-card-title>正文编辑</v-card-title>
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
          <v-textarea
            :model-value="chapterStore.currentChapter?.content || ''"
            label="章节正文"
            rows="18"
            class="mt-4"
            :disabled="!chapterStore.currentChapter"
            @update:model-value="
              (value) => {
                if (chapterStore.currentChapter) {
                  chapterStore.currentChapter.content = value
                }
              }
            "
          />
          <div class="d-flex justify-space-between align-center mt-4">
            <div class="text-body-2 text-medium-emphasis">
              章节字数：{{ chapterStore.currentChapter?.wordCount || chapterStore.currentChapter?.content?.length || 0 }}
            </div>
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

      <div class="content-grid">
        <v-card class="soft-panel">
          <v-card-title>AI 生成配置</v-card-title>
          <v-card-text>
            <v-select
              v-model="draftForm.writingType"
              label="任务类型"
              :items="[
                { title: '续写', value: 'continue' },
                { title: '润色', value: 'polish' },
                { title: '扩写', value: 'expand' },
                { title: '改写', value: 'rewrite' },
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

            <v-text-field v-model="draftForm.maxTokens" label="最大输出长度" type="number" class="mt-2" />

            <v-alert type="info" variant="tonal" class="mt-4">
              当前模型服务：{{ selectedProvider?.name || '未设置' }}，对话模型：{{ draftForm.selectedModel || '未设置' }}
            </v-alert>

            <v-alert v-if="isOllamaSelected" type="success" variant="tonal" class="mt-4">
              当前使用 Ollama。只要服务地址和模型名可用，就可以直接开始生成。
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
              label="附加要求"
              class="mt-4"
              placeholder="例如：保持第一人称口吻，把冲突推进到新的反转点。"
            />

            <v-alert v-if="errorMessage" type="error" variant="tonal" class="mt-4">
              {{ errorMessage }}
            </v-alert>

            <v-alert type="success" variant="tonal" class="mt-4">
              采纳 AI 草稿后，内容会自动同步到当前项目的知识库。
            </v-alert>

            <v-btn
              block
              size="large"
              color="primary"
              class="mt-4"
              :loading="generating"
              :disabled="!chapterStore.currentChapter?.id || !draftForm.selectedProviderId"
              @click="generate"
            >
              发起 AI 生成
            </v-btn>
          </v-card-text>
        </v-card>

        <v-card class="soft-panel">
          <v-card-title>最近生成记录</v-card-title>
          <v-list v-if="writingStore.records.length" lines="three">
            <v-list-item
              v-for="record in writingStore.records"
              :key="record.id"
              :title="`${getWritingTypeLabel(record.writingType)} · ${getRecordStatusLabel(record.status)}`"
              :subtitle="record.generatedContent"
            >
              <template #append>
                <div class="d-flex flex-column align-end ga-2">
                  <div class="d-flex ga-2">
                    <v-chip size="small" variant="tonal">{{ getProviderName(record.selectedProviderId) }}</v-chip>
                    <v-chip size="small" color="primary" variant="tonal">
                      {{ record.selectedModel || '未指定模型' }}
                    </v-chip>
                  </div>
                  <span class="text-caption text-medium-emphasis">{{ formatDateTime(record.createTime) }}</span>
                  <div class="d-flex ga-2">
                    <v-btn size="small" color="primary" variant="text" @click="writingStore.accept(record.id)">
                      采纳
                    </v-btn>
                    <v-btn size="small" color="error" variant="text" @click="writingStore.reject(record.id)">
                      拒绝
                    </v-btn>
                  </div>
                </div>
              </template>
            </v-list-item>
          </v-list>
          <v-card-text v-else class="text-medium-emphasis">
            还没有 AI 草稿记录，先发起一次生成。
          </v-card-text>
        </v-card>
      </div>
    </div>
  </PageContainer>
</template>
