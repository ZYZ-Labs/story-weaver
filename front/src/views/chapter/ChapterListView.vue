<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

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
const displayChapterAiLatestRecord = computed<AIWritingRecord | null>(
  () => currentChapterStreamState.value.lastRecord || chapterAiLatestRecord.value || writingStore.records[0] || null,
)
const enabledProviders = computed(() => providerStore.providers.filter((item) => item.enabled === 1))
const defaultProvider = computed(() => {
  const configuredId = settingsStore.getNumberValue('default_ai_provider_id', null)
  if (configuredId) {
    const matched = enabledProviders.value.find((item) => item.id === configuredId)
    if (matched) {
      return matched
    }
  }
  return (
    enabledProviders.value.find((item) => item.isDefault === 1) ||
    enabledProviders.value.find((item) => item.providerType === 'ollama') ||
    enabledProviders.value[0] ||
    null
  )
})
const defaultModelName = computed(
  () => defaultProvider.value?.modelName || settingsStore.getConfigValue('default_ai_model', '由默认 Provider 决定'),
)
const chapterAiProfile = computed(() => resolveOutputLengthProfile(defaultProvider.value, defaultModelName.value))

const tableHeaders = [
  { title: '顺序', key: 'orderNum', width: 88 },
  { title: '标题', key: 'title' },
  { title: '字数', key: 'wordCount', width: 100 },
  { title: '操作', key: 'actions', sortable: false, width: 188 },
]

const toneOptions = ['紧张压迫', '轻松日常', '神秘悬疑', '热血推进', '伤感克制', '史诗宏大']

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
  [defaultProvider, defaultModelName],
  () => {
    const profile = chapterAiProfile.value
    const signature = `${defaultProvider.value?.id || 0}:${defaultModelName.value}:${profile.max}`
    if (signature !== lastChapterAiModelSignature.value) {
      lastChapterAiModelSignature.value = signature
      chapterAiForm.maxTokens = profile.recommended
      return
    }
    chapterAiForm.maxTokens = Math.min(
      Math.max(chapterAiForm.maxTokens || profile.recommended, profile.min),
      profile.max,
    )
  },
  { immediate: true },
)

onMounted(async () => {
  if (!projectStore.projects.length) {
    await projectStore.fetchProjects().catch(() => undefined)
  }
  await Promise.allSettled([settingsStore.fetchAll(), providerStore.fetchAll()])
})

function fillForm(chapter?: Chapter | null) {
  Object.assign(form, {
    title: chapter?.title || '',
    content: chapter?.content || '',
    orderNum: chapter?.orderNum || chapterStore.chapters.length + 1,
    requiredCharacterIds: [...(chapter?.requiredCharacterIds || [])],
  })
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
    draft: '拟生成',
    continue: '续写',
    expand: '扩写',
    rewrite: '重写',
    polish: '润色',
  }
  return mapping[value || ''] || value || '拟生成'
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
      brief: form.content.trim() || form.title.trim() || '请根据当前项目风格生成一个章节标题。',
      extraRequirements: `当前章节顺序：第 ${Number(form.orderNum) || 1} 章。标题要适合中文长篇连载。`,
      count: 6,
    })
    nameSuggestions.value = result.suggestions || []
    nameSuggestionSourceLabel.value = `生成模型：${result.providerName || '未命名服务'} / ${result.modelName || '未命名模型'}`
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
    chapterAiForm.characterVoice.trim() && `人物性格 / 口吻要求：${chapterAiForm.characterVoice.trim()}`,
    chapterAiForm.extraInstruction.trim() && `额外补充：${chapterAiForm.extraInstruction.trim()}`,
    currentPreview.value?.requiredCharacterNames?.length &&
      `本章必须出现人物：${currentPreview.value.requiredCharacterNames.join('、')}`,
  ].filter(Boolean)

  const actionInstruction: Record<typeof action, string> = {
    draft: '请先搭出一版能继续往下写的章节正文初稿。',
    continue: '请承接当前最后一段往后推进，不要重复已经写过的内容。',
    expand: '请在保留已有情节事实的前提下，把当前正文扩成更饱满的版本。',
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
        selectedProviderId: defaultProvider.value?.id || settingsStore.getNumberValue('default_ai_provider_id', null),
        selectedModel: defaultModelName.value === '由默认 Provider 决定' ? '' : defaultModelName.value,
        promptSnapshot: settingsStore.getPromptTemplateByWritingType(writingType),
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
    chapterAiError.value = error instanceof Error ? error.message : 'AI 正文生成失败。'
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
    title="章节管理"
    description="维护当前项目的章节顺序、标题和正文内容。现在可以为章节直接勾选必出人物，右侧 AI 助手也会保留跨页面的进行中内容。"
  >
    <template #actions>
      <v-btn color="primary" prepend-icon="mdi-plus" :disabled="!currentProjectId" @click="openCreate">
        新建章节
      </v-btn>
    </template>

    <EmptyState
      v-if="!currentProjectId"
      title="先选择一个项目"
      description="左侧项目切换后，这里会自动加载该项目的章节。"
    />

    <EmptyState
      v-else-if="!chapterStore.chapters.length"
      title="还没有章节"
      description="先创建第一章，后续人物、剧情和 AI 写作都会围绕它展开。"
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
              章节顺序：{{ currentPreview.orderNum || '-' }} · 字数：{{ getWordCount(currentPreview) }}
            </div>
            <div v-if="currentPreview.requiredCharacterNames?.length" class="d-flex flex-wrap ga-2 mt-3">
              <v-chip
                v-for="name in currentPreview.requiredCharacterNames"
                :key="name"
                size="small"
                color="secondary"
                variant="tonal"
              >
                必读人物：{{ name }}
              </v-chip>
            </div>
            <v-divider class="my-4" />
            <div class="chapter-preview-content">
              <MarkdownContent
                :source="currentPreview.content"
                empty-text="暂无正文。可以直接在下方让 AI 先帮你拟一版初稿。"
              />
            </div>
          </v-card-text>
          <v-card-text v-else class="text-medium-emphasis">
            选择一章后会在这里预览内容。
          </v-card-text>
        </v-card>

        <v-card v-if="currentPreview" class="soft-panel">
          <v-card-title>AI 正文助手</v-card-title>
          <v-card-subtitle>
            默认模型：{{ defaultModelName }}<span v-if="defaultProvider"> · {{ defaultProvider.name }}</span>
          </v-card-subtitle>
          <v-card-text class="pt-4">
            <v-row>
              <v-col cols="12" md="6">
                <v-text-field
                  v-model="chapterAiForm.sceneGoal"
                  label="场景目标"
                  placeholder="例如：让主角第一次发现关键线索"
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
                  label="人物性格 / 口吻要求"
                  placeholder="例如：主角嘴硬但克制，配角说话更冷静"
                />
              </v-col>
              <v-col cols="12">
                <v-textarea
                  v-model="chapterAiForm.extraInstruction"
                  rows="4"
                  label="补充要求"
                  placeholder="例如：节奏不要太快，先把场景和人物关系铺开"
                />
              </v-col>
              <v-col cols="12">
                <div class="d-flex justify-space-between align-center ga-3">
                  <div>
                    <div class="text-subtitle-2 font-weight-medium">目标输出长度</div>
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
                  拟生成正文初稿
                </v-btn>
                <v-btn
                  variant="outlined"
                  :loading="displayChapterAiGenerating"
                  @click="generateChapterAiContent('continue')"
                >
                  {{ currentPreviewHasContent ? '续写当前正文' : '根据设定起草正文' }}
                </v-btn>
                <v-btn
                  variant="outlined"
                  :loading="displayChapterAiGenerating"
                  @click="generateChapterAiContent('expand')"
                >
                  {{ currentPreviewHasContent ? '扩写当前正文' : '先生成可扩写版本' }}
                </v-btn>
              </v-col>
            </v-row>

            <v-alert v-if="!currentPreviewHasContent" type="info" variant="tonal" class="mt-4">
              当前正文还是空的。点“拟生成正文初稿”最合适；如果直接点续写或扩写，也会自动按初稿模式处理。
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
                <MarkdownContent :source="displayChapterAiStreamingContent" empty-text="暂无生成内容" />
              </div>
              <div v-else class="text-medium-emphasis">
                这里会实时显示生成中的正文片段，生成完成后可直接采纳到当前章节。
              </div>
            </div>

            <div v-if="displayChapterAiLatestRecord" class="mt-4">
              <div class="d-flex justify-space-between align-center ga-3">
                <div>
                  <div class="text-subtitle-2 font-weight-medium">
                    最新 AI 结果：{{ getWritingTypeLabel(displayChapterAiLatestRecord.writingType) }}
                  </div>
                  <div class="text-caption text-medium-emphasis mt-1">
                    {{ formatDateTime(displayChapterAiLatestRecord.createTime) }} ·
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
                  AI 生成标题
                </v-btn>
              </div>
            </v-col>
            <v-col cols="12" md="4">
              <v-text-field v-model="form.orderNum" type="number" label="章节顺序" />
            </v-col>
            <v-col cols="12">
              <MarkdownEditor
                v-model="form.content"
                label="正文内容"
                :rows="10"
                preview-empty-text="暂无正文内容"
              />
            </v-col>
            <v-col cols="12">
              <v-select
                v-model="form.requiredCharacterIds"
                label="本章必须出场人物"
                :items="characterStore.characters"
                item-title="name"
                item-value="id"
                multiple
                chips
                closable-chips
                clearable
                hint="先在人物管理里把角色关联到当前项目，这里就能直接勾选本章必须出现的人物。"
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
      text="确认删除这一章吗？删除后它会从章节列表和写作中心中移除。"
      @confirm="confirmDelete"
    />

    <NameSuggestionDialog
      v-model="nameSuggestionDialog"
      title="选择章节标题"
      :loading="nameSuggestionLoading"
      :suggestions="nameSuggestions"
      :source-label="nameSuggestionSourceLabel"
      empty-text="这次没有拿到合适的标题候选，可以再试一次。"
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
