<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import NameSuggestionDialog from '@/components/NameSuggestionDialog.vue'
import PageContainer from '@/components/PageContainer.vue'
import { generateNameSuggestions } from '@/api/name-suggestion'
import { useChapterStore } from '@/stores/chapter'
import { useProjectStore } from '@/stores/project'
import type { Chapter } from '@/types'

const projectStore = useProjectStore()
const chapterStore = useChapterStore()

const dialog = ref(false)
const deletingId = ref<number | null>(null)
const confirmVisible = ref(false)
const editingId = ref<number | null>(null)
const nameSuggestionDialog = ref(false)
const nameSuggestionLoading = ref(false)
const nameSuggestions = ref<string[]>([])
const nameSuggestionSourceLabel = ref('')

const currentProjectId = computed(() => projectStore.selectedProjectId)
const currentPreview = computed(() => chapterStore.currentChapter)

const tableHeaders = [
  { title: '顺序', key: 'orderNum', width: 88 },
  { title: '标题', key: 'title' },
  { title: '字数', key: 'wordCount', width: 100 },
  { title: '操作', key: 'actions', sortable: false, width: 168 },
]

const form = reactive({
  title: '',
  content: '',
  orderNum: 1,
})

watch(
  currentProjectId,
  async (projectId) => {
    if (projectId) {
      await chapterStore.fetchByProject(projectId).catch(() => undefined)
    }
  },
  { immediate: true },
)

onMounted(() => {
  if (!projectStore.projects.length) {
    projectStore.fetchProjects().catch(() => undefined)
  }
})

function fillForm(chapter?: Chapter | null) {
  Object.assign(form, {
    title: chapter?.title || '',
    content: chapter?.content || '',
    orderNum: chapter?.orderNum || chapterStore.chapters.length + 1,
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

async function submit() {
  if (!currentProjectId.value) {
    return
  }

  const payload = {
    ...form,
    orderNum: Number(form.orderNum) || 1,
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
    description="维护当前项目的章节顺序、标题和正文内容。预览区固定在右侧，不会再把左边列表整列拉高。"
  >
    <template #actions>
      <v-btn color="primary" prepend-icon="mdi-plus" :disabled="!currentProjectId" @click="openCreate">
        新建章节
      </v-btn>
    </template>

    <EmptyState
      v-if="!currentProjectId"
      title="先选择一个项目"
      description="左侧导航里切换当前项目后，章节列表会自动加载。"
    />

    <EmptyState
      v-else-if="!chapterStore.chapters.length"
      title="还没有章节"
      description="先创建第一章，后续写作中心就可以直接对它发起续写和改写。"
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

      <v-card class="soft-panel chapter-preview-card">
        <v-card-title>章节预览</v-card-title>
        <v-card-text v-if="currentPreview" class="chapter-preview-body">
          <div class="text-h6">{{ currentPreview.title }}</div>
          <div class="text-body-2 text-medium-emphasis mt-2">
            章节序号：{{ currentPreview.orderNum || '-' }} · 字数：{{ getWordCount(currentPreview) }}
          </div>
          <v-divider class="my-4" />
          <div class="chapter-preview-content">
            {{ currentPreview.content || '暂无正文。' }}
          </div>
        </v-card-text>
        <v-card-text v-else class="text-medium-emphasis">
          选择一章后会在这里预览内容。
        </v-card-text>
      </v-card>
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
              <v-textarea v-model="form.content" rows="10" label="正文内容" />
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
      text="确认删除这章吗？删除后不会再出现在章节列表、写作中心和项目概览里。"
      @confirm="confirmDelete"
    />

    <NameSuggestionDialog
      v-model="nameSuggestionDialog"
      title="选择章节标题"
      :loading="nameSuggestionLoading"
      :suggestions="nameSuggestions"
      :source-label="nameSuggestionSourceLabel"
      empty-text="这次没有拿到可用标题候选，可以重试一次。"
      @refresh="generateTitleSuggestions"
      @select="applySuggestedTitle"
    />
  </PageContainer>
</template>

<style scoped>
.chapter-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.95fr);
  gap: 16px;
  align-items: start;
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
  gap: 0;
}

.chapter-preview-content {
  max-height: 520px;
  overflow: auto;
  white-space: pre-wrap;
  line-height: 1.8;
  padding-right: 4px;
}

@media (max-width: 960px) {
  .chapter-layout {
    grid-template-columns: 1fr;
  }

  .chapter-preview-content,
  .chapter-table :deep(.v-table__wrapper) {
    max-height: none;
  }
}
</style>
