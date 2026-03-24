<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import PageContainer from '@/components/PageContainer.vue'
import { useCausalityStore } from '@/stores/causality'
import { useChapterStore } from '@/stores/chapter'
import { useCharacterStore } from '@/stores/character'
import { usePlotStore } from '@/stores/plot'
import { useProjectStore } from '@/stores/project'
import { useRagStore } from '@/stores/rag'
import { useSettingsStore } from '@/stores/settings'
import { useWritingStore } from '@/stores/writing'

type SourceOption = {
  value: string
  title: string
  summary: string
  content: string
  meta?: string
}

const projectStore = useProjectStore()
const ragStore = useRagStore()
const chapterStore = useChapterStore()
const characterStore = useCharacterStore()
const plotStore = usePlotStore()
const causalityStore = useCausalityStore()
const writingStore = useWritingStore()
const settingsStore = useSettingsStore()

const projectId = computed(() => projectStore.selectedProjectId)
const dialog = ref(false)
const editingId = ref<number | null>(null)
const deletingId = ref<number | null>(null)
const confirmVisible = ref(false)
const searchText = ref('')
const reindexMessage = ref('')
const lastAutoTitle = ref('')
const lastAutoSummary = ref('')
const lastAutoContent = ref('')

const sourceTypeOptions = [
  { title: '手动录入', value: 'manual_note' },
  { title: '章节正文', value: 'chapter' },
  { title: '剧情事件', value: 'plot' },
  { title: '人物设定', value: 'character' },
  { title: '因果关系', value: 'causality' },
  { title: 'AI 草稿 / 任务', value: 'writing' },
]

const statusOptions = [
  { title: '待入库', value: 'ready' },
  { title: '已索引', value: 'indexed' },
  { title: '归档', value: 'archived' },
]

const form = reactive({
  sourceType: 'manual_note',
  sourceRefId: '',
  title: '',
  contentText: '',
  summary: '',
  status: 'ready',
})

const currentQueryPrompt = computed(() => settingsStore.getPromptTemplate('prompt.rag_query'))
const currentExtractPrompt = computed(() => settingsStore.getPromptTemplate('prompt.knowledge_extract'))
const sourceOptions = computed(() => getSourceOptions(form.sourceType))
const sourceMeta = computed(() => getSourceMeta(form.sourceType, form.sourceRefId))

watch(
  projectId,
  async (id) => {
    if (!id) return
    await Promise.allSettled([
      ragStore.fetchByProject(id),
      chapterStore.fetchByProject(id),
      characterStore.fetchByProject(id),
      plotStore.fetchByProject(id),
      causalityStore.fetchByProject(id),
      writingStore.fetchByProject(id),
      settingsStore.fetchAll(),
    ])
  },
  { immediate: true },
)

watch(
  () => form.sourceType,
  (type) => {
    if (type !== 'manual_note' && !getSourceOptions(type).some((item) => item.value === form.sourceRefId)) {
      form.sourceRefId = ''
    }
    if (type === 'manual_note') {
      form.status = 'ready'
    }
  },
)

watch(
  [() => form.sourceType, () => form.sourceRefId],
  () => {
    syncAutoFields()
  },
)

onMounted(() => {
  if (!projectStore.projects.length) {
    projectStore.fetchProjects().catch(() => undefined)
  }
})

function buildSummary(value: string, maxLength = 120) {
  const normalized = value.replace(/\s+/g, ' ').trim()
  if (!normalized) return ''
  return normalized.length <= maxLength ? normalized : `${normalized.slice(0, maxLength)}...`
}

function getWritingTypeLabel(value?: string) {
  const mapping: Record<string, string> = {
    continue: '续写',
    expand: '扩写',
    rewrite: '改写',
    polish: '润色',
  }
  return mapping[value || ''] || value || '草稿'
}

function getSourceOptions(type: string): SourceOption[] {
  switch (type) {
    case 'chapter':
      return chapterStore.chapters.map((item) => ({
        value: String(item.id),
        title: item.title,
        summary: buildSummary(item.content || ''),
        content: item.content || '',
        meta: item.orderNum ? `第 ${item.orderNum} 章` : '章节',
      }))
    case 'plot':
      return plotStore.plotlines.map((item) => ({
        value: String(item.id),
        title: item.title || '未命名剧情',
        summary: buildSummary(item.description || item.content || ''),
        content: item.content || item.description || '',
        meta: item.timeline || '剧情事件',
      }))
    case 'character':
      return characterStore.characters.map((item) => ({
        value: String(item.id),
        title: item.name,
        summary: buildSummary(item.description || ''),
        content: item.description || '',
        meta: '人物设定',
      }))
    case 'causality':
      return causalityStore.nodes.map((item) => ({
        value: String(item.id),
        title: item.name || '未命名因果',
        summary: buildSummary(item.description || ''),
        content: item.description || '',
        meta: item.relationship || '因果关系',
      }))
    case 'writing':
      return writingStore.projectRecords.map((item) => ({
        value: String(item.id),
        title: `${getWritingTypeLabel(item.writingType)} · ${item.selectedModel || '未指定模型'}`,
        summary: buildSummary(item.generatedContent || item.userInstruction || ''),
        content: item.generatedContent || item.originalContent || '',
        meta: `章节 #${item.chapterId} / ${item.status || 'draft'}`,
      }))
    default:
      return []
  }
}

function getSourceMeta(type: string, sourceRefId: string) {
  return getSourceOptions(type).find((item) => item.value === sourceRefId) || null
}

function syncAutoFields() {
  if (form.sourceType === 'manual_note') return
  const meta = getSourceMeta(form.sourceType, form.sourceRefId)
  if (!meta) return

  if (!form.title || form.title === lastAutoTitle.value) {
    form.title = meta.title
    lastAutoTitle.value = meta.title
  }

  if (!form.summary || form.summary === lastAutoSummary.value) {
    form.summary = meta.summary
    lastAutoSummary.value = meta.summary
  }

  if (!form.contentText || form.contentText === lastAutoContent.value) {
    form.contentText = meta.content
    lastAutoContent.value = meta.content
  }

  if (form.sourceType !== 'manual_note' && form.status === 'ready') {
    form.status = 'indexed'
  }
}

function resetForm() {
  Object.assign(form, {
    sourceType: 'manual_note',
    sourceRefId: '',
    title: '',
    contentText: '',
    summary: '',
    status: 'ready',
  })
  lastAutoTitle.value = ''
  lastAutoSummary.value = ''
  lastAutoContent.value = ''
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialog.value = true
}

function openEdit(id: number) {
  const target = ragStore.documents.find((item) => item.id === id)
  if (!target) return
  editingId.value = id
  Object.assign(form, {
    sourceType: target.sourceType || 'manual_note',
    sourceRefId: target.sourceRefId || '',
    title: target.title,
    contentText: target.contentText || '',
    summary: target.summary || '',
    status: target.status || 'ready',
  })
  lastAutoTitle.value = form.title
  lastAutoSummary.value = form.summary
  lastAutoContent.value = form.contentText
  dialog.value = true
}

function requestDelete(id: number) {
  deletingId.value = id
  confirmVisible.value = true
}

async function submit() {
  if (!projectId.value) return
  const payload = {
    ...form,
    sourceRefId: form.sourceType === 'manual_note' ? '' : form.sourceRefId,
  }

  if (editingId.value) {
    await ragStore.update(editingId.value, payload)
  } else {
    await ragStore.create(projectId.value, payload)
  }
  dialog.value = false
}

async function confirmDelete() {
  if (!deletingId.value) return
  await ragStore.remove(deletingId.value)
  confirmVisible.value = false
}

async function queryKnowledge() {
  if (!projectId.value) return
  if (!searchText.value.trim()) {
    ragStore.queryResults = []
    return
  }
  await ragStore.query(projectId.value, searchText.value)
}

async function reindexKnowledge() {
  if (!projectId.value) return
  const result = await ragStore.reindex(projectId.value)
  reindexMessage.value = `已完成重建索引，文档数：${result.documentCount}`
}

function getStatusColor(status?: string) {
  if (status === 'indexed') return 'success'
  if (status === 'archived') return 'default'
  return 'primary'
}
</script>

<template>
  <PageContainer
    title="RAG 知识库"
    description="知识录入现在支持从章节、剧情事件、人物、因果关系和 AI 草稿任务中直接选择来源，并自动填充标题、摘要和正文。"
  >
    <template #actions>
      <div class="d-flex ga-2">
        <v-btn color="primary" prepend-icon="mdi-plus" :disabled="!projectId" @click="openCreate">新增知识</v-btn>
        <v-btn variant="outlined" :disabled="!projectId" @click="reindexKnowledge">重建索引</v-btn>
      </div>
    </template>

    <EmptyState
      v-if="!projectId"
      title="先选择项目"
      description="RAG 知识条目和项目强绑定，请先从左侧选择当前项目。"
    />

    <div v-else class="page-grid">
      <v-alert v-if="reindexMessage" type="success" variant="tonal">
        {{ reindexMessage }}
      </v-alert>

      <div class="stats-grid">
        <v-card class="soft-panel">
          <v-card-text>
            <div class="text-overline">知识文档</div>
            <div class="text-h4 mt-2">{{ ragStore.knowledgeStats.documents }}</div>
          </v-card-text>
        </v-card>
        <v-card class="soft-panel">
          <v-card-text>
            <div class="text-overline">Chunk 估算</div>
            <div class="text-h4 mt-2">{{ ragStore.knowledgeStats.chunks }}</div>
          </v-card-text>
        </v-card>
        <v-card class="soft-panel">
          <v-card-text>
            <div class="text-overline">Indexed 比例</div>
            <div class="text-h4 mt-2">{{ ragStore.knowledgeStats.indexed }}</div>
          </v-card-text>
        </v-card>
      </div>

      <div class="content-grid two-column">
        <v-card class="soft-panel">
          <v-card-title>当前 Prompt 策略</v-card-title>
          <v-list lines="three">
            <v-list-item title="RAG 检索 Prompt" :subtitle="currentQueryPrompt || '未配置'" />
            <v-list-item title="知识抽取 Prompt" :subtitle="currentExtractPrompt || '未配置'" />
          </v-list>
        </v-card>

        <v-card class="soft-panel">
          <v-card-text class="d-flex ga-3 align-center">
            <v-text-field v-model="searchText" label="检索知识条目" hide-details />
            <v-btn color="primary" @click="queryKnowledge">检索</v-btn>
          </v-card-text>
        </v-card>
      </div>

      <div class="content-grid two-column">
        <v-card class="soft-panel">
          <v-card-title>知识文档</v-card-title>
          <v-list lines="three">
            <v-list-item
              v-for="doc in ragStore.documents"
              :key="doc.id"
              :title="doc.title"
              :subtitle="doc.summary || doc.contentText || '暂无内容'"
            >
              <template #append>
                <div class="d-flex flex-column align-end ga-2">
                  <div class="d-flex ga-2">
                    <v-chip size="small" variant="tonal">{{ doc.sourceType || 'manual' }}</v-chip>
                    <v-chip size="small" :color="getStatusColor(doc.status)" variant="tonal">
                      {{ doc.status || 'ready' }}
                    </v-chip>
                  </div>
                  <div class="d-flex ga-2">
                    <v-btn size="small" variant="text" @click="openEdit(doc.id)">编辑</v-btn>
                    <v-btn size="small" color="error" variant="text" @click="requestDelete(doc.id)">删除</v-btn>
                  </div>
                </div>
              </template>
            </v-list-item>
          </v-list>
        </v-card>

        <v-card class="soft-panel">
          <v-card-title>检索结果</v-card-title>
          <v-list v-if="ragStore.queryResults.length" lines="three">
            <v-list-item
              v-for="doc in ragStore.queryResults"
              :key="doc.id"
              :title="doc.title"
              :subtitle="doc.summary || doc.contentText || '暂无内容'"
            >
              <template #append>
                <v-chip size="small" variant="tonal">{{ doc.sourceType || 'manual' }}</v-chip>
              </template>
            </v-list-item>
          </v-list>
          <v-card-text v-else class="text-medium-emphasis">
            输入关键词后，可以在这里查看命中的知识条目。
          </v-card-text>
        </v-card>
      </div>
    </div>

    <v-dialog v-model="dialog" max-width="920">
      <v-card>
        <v-card-title>{{ editingId ? '编辑知识条目' : '新增知识条目' }}</v-card-title>
        <v-card-text class="pt-4">
          <v-row>
            <v-col cols="12" md="4">
              <v-select
                v-model="form.sourceType"
                label="来源类型"
                :items="sourceTypeOptions"
                item-title="title"
                item-value="value"
              />
            </v-col>
            <v-col v-if="form.sourceType !== 'manual_note'" cols="12" md="4">
              <v-autocomplete
                v-model="form.sourceRefId"
                label="关联来源"
                :items="sourceOptions"
                item-title="title"
                item-value="value"
                clearable
              />
            </v-col>
            <v-col cols="12" :md="form.sourceType !== 'manual_note' ? 4 : 8">
              <v-select
                v-model="form.status"
                label="状态"
                :items="statusOptions"
                item-title="title"
                item-value="value"
              />
            </v-col>
            <v-col cols="12">
              <v-text-field v-model="form.title" label="标题" />
            </v-col>
            <v-col v-if="sourceMeta" cols="12">
              <v-alert type="info" variant="tonal">
                <div class="font-weight-medium">{{ sourceMeta.title }}</div>
                <div class="text-caption text-medium-emphasis mt-1">
                  {{ sourceMeta.meta || '已选择来源' }}
                </div>
                <div class="text-body-2 mt-2">{{ sourceMeta.summary || '暂无摘要' }}</div>
              </v-alert>
            </v-col>
            <v-col cols="12">
              <v-textarea v-model="form.contentText" label="正文" rows="8" />
            </v-col>
            <v-col cols="12">
              <v-textarea v-model="form.summary" label="摘要" rows="3" />
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
      title="删除知识条目"
      text="确认删除这份知识条目吗？"
      @confirm="confirmDelete"
    />
  </PageContainer>
</template>
