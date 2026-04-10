<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import MarkdownContent from '@/components/MarkdownContent.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import PageContainer from '@/components/PageContainer.vue'
import { useCausalityStore } from '@/stores/causality'
import { useChapterStore } from '@/stores/chapter'
import { useCharacterStore } from '@/stores/character'
import { useOutlineStore } from '@/stores/outline'
import { usePlotStore } from '@/stores/plot'
import { useProjectStore } from '@/stores/project'
import { useWorldSettingStore } from '@/stores/world-setting'
import type { Outline } from '@/types'

const projectStore = useProjectStore()
const outlineStore = useOutlineStore()
const chapterStore = useChapterStore()
const characterStore = useCharacterStore()
const plotStore = usePlotStore()
const causalityStore = useCausalityStore()
const worldSettingStore = useWorldSettingStore()

const projectId = computed(() => projectStore.selectedProjectId)
const dialog = ref(false)
const confirmVisible = ref(false)
const editingId = ref<number | null>(null)
const deletingId = ref<number | null>(null)

const lastAutoTitle = ref('')
const lastAutoSummary = ref('')
const lastAutoOrder = ref<number | null>(null)

const statusOptions = [
  { title: '规划中', value: 0 },
  { title: '待写作', value: 1 },
  { title: '已成稿', value: 2 },
  { title: '已归档', value: 3 },
]

const outlineTypeOptions = [
  { title: '全局总纲', value: 'global' },
  { title: '卷级大纲', value: 'volume' },
  { title: '章节大纲', value: 'chapter' },
]

const form = reactive({
  outlineType: 'chapter',
  parentOutlineId: null as number | null,
  chapterId: null as number | null,
  generatedChapterId: null as number | null,
  title: '',
  summary: '',
  content: '',
  stageGoal: '',
  keyConflict: '',
  turningPoints: '',
  expectedEnding: '',
  focusCharacterIds: [] as number[],
  relatedPlotIds: [] as number[],
  relatedCausalityIds: [] as number[],
  relatedWorldSettingIds: [] as number[],
  status: 0,
  orderNum: 1,
})

const outlineLookup = computed(() =>
  outlineStore.outlines.reduce<Record<number, Outline>>((accumulator, item) => {
    accumulator[item.id] = item
    return accumulator
  }, {}),
)

const orderedOutlines = computed(() =>
  [...outlineStore.outlines].sort((left, right) => {
    const leftRoot = left.rootOutlineId ?? left.id
    const rightRoot = right.rootOutlineId ?? right.id
    if (leftRoot !== rightRoot) {
      return leftRoot - rightRoot
    }
    const leftParent = left.parentOutlineId ?? 0
    const rightParent = right.parentOutlineId ?? 0
    if (leftParent !== rightParent) {
      return leftParent - rightParent
    }
    const leftOrder = left.orderNum ?? Number.MAX_SAFE_INTEGER
    const rightOrder = right.orderNum ?? Number.MAX_SAFE_INTEGER
    if (leftOrder !== rightOrder) {
      return leftOrder - rightOrder
    }
    return left.id - right.id
  }),
)

const chapterOptions = computed(() =>
  chapterStore.chapters.map((item) => ({
    title: `第 ${item.orderNum || '-'} 章 · ${item.title}`,
    value: item.id,
  })),
)
const characterOptions = computed(() =>
  characterStore.characters.map((item) => ({
    title: item.name,
    value: item.id,
  })),
)
const plotOptions = computed(() =>
  plotStore.plotlines.map((item) => ({
    title: item.title || `剧情 #${item.id}`,
    value: item.id,
  })),
)
const causalityOptions = computed(() =>
  causalityStore.nodes.map((item) => ({
    title: item.name || item.relationship || `因果 #${item.id}`,
    value: item.id,
  })),
)
const worldSettingOptions = computed(() =>
  worldSettingStore.items.map((item) => ({
    title: item.name || item.title || `设定 #${item.id}`,
    value: item.id,
  })),
)
const parentOutlineOptions = computed(() =>
  orderedOutlines.value
    .filter((item) => item.id !== editingId.value && canUseAsParent(item, form.outlineType))
    .map((item) => ({
      title: `${getOutlineTypeLabel(item.outlineType, resolveOutlineChapterId(item))} · ${item.title || `大纲 #${item.id}`}`,
      value: item.id,
    })),
)

watch(
  projectId,
  async (id) => {
    if (!id) {
      return
    }
    await Promise.allSettled([
      outlineStore.fetchByProject(id),
      chapterStore.fetchByProject(id),
      characterStore.fetchByProject(id),
      plotStore.fetchByProject(id),
      causalityStore.fetchByProject(id),
      worldSettingStore.fetchByProject(id),
    ])
  },
  { immediate: true },
)

watch(
  () => form.outlineType,
  (outlineType) => {
    if (outlineType === 'chapter') {
      if (!form.generatedChapterId) {
        form.generatedChapterId = chapterStore.chapters[0]?.id ?? null
      }
      form.chapterId = form.generatedChapterId
      return
    }
    form.chapterId = null
    form.generatedChapterId = null
  },
)

watch(
  () => form.generatedChapterId,
  (chapterId) => {
    form.chapterId = chapterId

    if (form.outlineType !== 'chapter') {
      return
    }

    const chapter = chapterStore.chapters.find((item) => item.id === chapterId)
    if (!chapter) {
      return
    }

    const autoTitle = `第 ${chapter.orderNum || '?'} 章大纲 · ${chapter.title}`
    const autoSummary = chapter.summary?.trim()
      ? `围绕章节《${chapter.title}》推进：${chapter.summary.slice(0, 80)}`
      : chapter.content?.trim()
        ? `延续章节《${chapter.title}》当前素材，梳理目标、冲突与关键转折：${chapter.content.slice(0, 80)}`
        : `围绕章节《${chapter.title}》规划目标、冲突推进、节拍和收束方式。`

    if (!form.title || form.title === lastAutoTitle.value) {
      form.title = autoTitle
      lastAutoTitle.value = autoTitle
    }

    if (!form.summary || form.summary === lastAutoSummary.value) {
      form.summary = autoSummary
      lastAutoSummary.value = autoSummary
    }

    if (!form.orderNum || form.orderNum === lastAutoOrder.value) {
      form.orderNum = chapter.orderNum || outlineStore.outlines.length + 1
      lastAutoOrder.value = form.orderNum
    }
  },
)

onMounted(() => {
  if (!projectStore.projects.length) {
    projectStore.fetchProjects().catch(() => undefined)
  }
})

function resetForm() {
  const defaultOutlineType = chapterStore.chapters.length ? 'chapter' : 'global'
  const defaultChapterId = defaultOutlineType === 'chapter' ? (chapterStore.chapters[0]?.id ?? null) : null
  Object.assign(form, {
    outlineType: defaultOutlineType,
    parentOutlineId: null,
    chapterId: defaultChapterId,
    generatedChapterId: defaultChapterId,
    title: '',
    summary: '',
    content: '',
    stageGoal: '',
    keyConflict: '',
    turningPoints: '',
    expectedEnding: '',
    focusCharacterIds: [],
    relatedPlotIds: [],
    relatedCausalityIds: [],
    relatedWorldSettingIds: [],
    status: 0,
    orderNum: outlineStore.outlines.length + 1,
  })
  lastAutoTitle.value = ''
  lastAutoSummary.value = ''
  lastAutoOrder.value = null
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialog.value = true
}

function openEdit(outline: Outline) {
  const outlineType = normalizeOutlineType(outline.outlineType, resolveOutlineChapterId(outline))
  const chapterId = resolveOutlineChapterId(outline)
  editingId.value = outline.id
  Object.assign(form, {
    outlineType,
    parentOutlineId: outline.parentOutlineId ?? null,
    chapterId,
    generatedChapterId: chapterId,
    title: outline.title || '',
    summary: outline.summary || '',
    content: outline.content || '',
    stageGoal: outline.stageGoal || '',
    keyConflict: outline.keyConflict || '',
    turningPoints: outline.turningPoints || '',
    expectedEnding: outline.expectedEnding || '',
    focusCharacterIds: normalizeNumberList(outline.focusCharacterIdList ?? outline.focusCharacterIds),
    relatedPlotIds: normalizeNumberList(outline.relatedPlotIdList ?? outline.relatedPlotIds),
    relatedCausalityIds: normalizeNumberList(outline.relatedCausalityIdList ?? outline.relatedCausalityIds),
    relatedWorldSettingIds: normalizeNumberList(
      outline.relatedWorldSettingIdList ?? outline.relatedWorldSettingIds,
    ),
    status: outline.status ?? 0,
    orderNum: outline.orderNum || 1,
  })
  lastAutoTitle.value = form.title
  lastAutoSummary.value = form.summary
  lastAutoOrder.value = form.orderNum
  dialog.value = true
}

function requestDelete(id: number) {
  deletingId.value = id
  confirmVisible.value = true
}

function normalizeOutlineType(value?: string | null, chapterId?: number | null) {
  if (value === 'global' || value === 'volume' || value === 'chapter') {
    return value
  }
  return chapterId ? 'chapter' : 'global'
}

function normalizeNumberList(value: unknown) {
  if (Array.isArray(value)) {
    return value
      .map((item) => Number(item))
      .filter((item) => Number.isFinite(item))
  }
  if (typeof value === 'string') {
    return value
      .split(/[\s,，[\]]+/)
      .map((item) => Number(item.trim()))
      .filter((item) => Number.isFinite(item))
  }
  return [] as number[]
}

function resolveOutlineChapterId(outline: Outline) {
  return outline.generatedChapterId ?? outline.chapterId ?? null
}

function canUseAsParent(outline: Outline, childType: string) {
  const parentType = normalizeOutlineType(outline.outlineType, resolveOutlineChapterId(outline))
  if (parentType === 'chapter') {
    return false
  }
  if (parentType === 'volume') {
    return childType === 'chapter'
  }
  return parentType === 'global'
}

function getStatusLabel(status?: number) {
  return statusOptions.find((item) => item.value === status)?.title || '未标记'
}

function getOutlineTypeLabel(outlineType?: string | null, chapterId?: number | null) {
  return outlineTypeOptions.find((item) => item.value === normalizeOutlineType(outlineType, chapterId))?.title || '章节大纲'
}

function getParentOutlineLabel(outline: Outline) {
  if (!outline.parentOutlineId) {
    return '顶层节点'
  }
  const parent = outlineLookup.value[outline.parentOutlineId]
  return parent?.title || `父级 #${outline.parentOutlineId}`
}

function getOutlineTargetLabel(outline: Outline) {
  const chapterId = resolveOutlineChapterId(outline)
  if (!chapterId) {
    return '未绑定章节'
  }
  return outline.chapterTitle || `章节 #${chapterId}`
}

async function submit() {
  if (!projectId.value) {
    return
  }

  const payload = {
    outlineType: form.outlineType,
    parentOutlineId: form.parentOutlineId,
    chapterId: form.outlineType === 'chapter' ? form.generatedChapterId : null,
    generatedChapterId: form.outlineType === 'chapter' ? form.generatedChapterId : null,
    title: form.title,
    summary: form.summary,
    content: form.content,
    stageGoal: form.stageGoal,
    keyConflict: form.keyConflict,
    turningPoints: form.turningPoints,
    expectedEnding: form.expectedEnding,
    focusCharacterIds: [...form.focusCharacterIds],
    relatedPlotIds: [...form.relatedPlotIds],
    relatedCausalityIds: [...form.relatedCausalityIds],
    relatedWorldSettingIds: [...form.relatedWorldSettingIds],
    status: form.status,
    orderNum: Number(form.orderNum) || 1,
  }

  if (editingId.value) {
    await outlineStore.update(projectId.value, editingId.value, payload)
  } else {
    await outlineStore.create(projectId.value, payload)
  }
  dialog.value = false
}

async function confirmDelete() {
  if (!projectId.value || !deletingId.value) {
    return
  }
  await outlineStore.remove(projectId.value, deletingId.value)
  confirmVisible.value = false
}
</script>

<template>
  <PageContainer
    title="大纲管理"
    description="把全局总纲、卷级大纲和章节大纲整理成树型结构，并把人物、剧情、因果、世界观一起挂到 AI 写作上下文里。"
  >
    <template #actions>
      <v-btn color="primary" prepend-icon="mdi-plus" :disabled="!projectId" @click="openCreate">
        新建大纲
      </v-btn>
    </template>

    <EmptyState
      v-if="!projectId"
      title="先选择一个项目"
      description="大纲与项目强绑定。请先在左侧切换到一个小说项目，再开始规划全局总纲、卷纲或章节大纲。"
    />

    <template v-else>
      <v-alert type="info" variant="tonal" class="mb-4">
        现在支持三层结构：全局总纲负责总方向，卷级大纲负责阶段推进，章节大纲负责落到单章写作。章节大纲会优先被 AI 总导读取。
      </v-alert>

      <EmptyState
        v-if="!outlineStore.outlines.length"
        title="还没有大纲"
        description="可以先建一个全局总纲，也可以直接从章节开始建单章大纲。"
      >
        <v-btn color="primary" prepend-icon="mdi-plus" @click="openCreate">创建第一条大纲</v-btn>
      </EmptyState>

      <v-row v-else>
        <v-col v-for="outline in orderedOutlines" :key="outline.id" cols="12" md="6">
          <v-card class="soft-panel h-100">
            <v-card-text>
              <div class="d-flex justify-space-between align-start ga-3">
                <div>
                  <div class="text-h6">{{ outline.title || '未命名大纲' }}</div>
                  <div class="d-flex flex-wrap ga-2 mt-3">
                    <v-chip size="small" color="primary" variant="tonal">
                      {{ getOutlineTypeLabel(outline.outlineType, resolveOutlineChapterId(outline)) }}
                    </v-chip>
                    <v-chip size="small" variant="outlined">
                      {{ getStatusLabel(outline.status) }}
                    </v-chip>
                  </div>
                </div>
              </div>

              <div class="text-caption text-medium-emphasis mt-3">
                父级：{{ getParentOutlineLabel(outline) }} | 绑定章节：{{ getOutlineTargetLabel(outline) }} | 顺序：{{ outline.orderNum || '-' }}
              </div>

              <div class="mt-4">
                <MarkdownContent :source="outline.summary" empty-text="暂无摘要" compact />
              </div>

              <div v-if="outline.stageGoal" class="mt-4">
                <div class="text-body-2 font-weight-medium mb-2">阶段目标</div>
                <MarkdownContent :source="outline.stageGoal" compact />
              </div>
              <div v-if="outline.keyConflict" class="mt-3">
                <div class="text-body-2 font-weight-medium mb-2">核心冲突</div>
                <MarkdownContent :source="outline.keyConflict" compact />
              </div>
              <div v-if="outline.turningPoints" class="mt-3">
                <div class="text-body-2 font-weight-medium mb-2">关键转折</div>
                <MarkdownContent :source="outline.turningPoints" compact />
              </div>
              <div v-if="outline.expectedEnding" class="mt-3">
                <div class="text-body-2 font-weight-medium mb-2">收束方向</div>
                <MarkdownContent :source="outline.expectedEnding" compact />
              </div>

              <div v-if="outline.focusCharacterNames?.length" class="d-flex flex-wrap ga-2 mt-4">
                <v-chip
                  v-for="name in outline.focusCharacterNames"
                  :key="name"
                  size="small"
                  color="secondary"
                  variant="tonal"
                >
                  {{ name }}
                </v-chip>
              </div>

              <div v-if="outline.relatedPlotTitles?.length" class="text-caption text-medium-emphasis mt-4">
                关联剧情：{{ outline.relatedPlotTitles.join('、') }}
              </div>
              <div v-if="outline.relatedCausalityNames?.length" class="text-caption text-medium-emphasis mt-2">
                关联因果：{{ outline.relatedCausalityNames.join('、') }}
              </div>
              <div v-if="outline.relatedWorldSettingNames?.length" class="text-caption text-medium-emphasis mt-2">
                关联世界观：{{ outline.relatedWorldSettingNames.join('、') }}
              </div>

              <div class="d-flex ga-2 mt-5">
                <v-btn variant="outlined" @click="openEdit(outline)">编辑</v-btn>
                <v-btn color="error" variant="text" @click="requestDelete(outline.id)">删除</v-btn>
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </template>

    <v-dialog v-model="dialog" max-width="1040">
      <v-card>
        <v-card-title>{{ editingId ? '编辑大纲' : '新建大纲' }}</v-card-title>
        <v-card-text class="pt-4">
          <v-row>
            <v-col cols="12" md="4">
              <v-select
                v-model="form.outlineType"
                label="大纲类型"
                :items="outlineTypeOptions"
                item-title="title"
                item-value="value"
              />
            </v-col>
            <v-col cols="12" md="4">
              <v-select
                v-model="form.parentOutlineId"
                label="父级大纲"
                :items="parentOutlineOptions"
                item-title="title"
                item-value="value"
                clearable
              />
            </v-col>
            <v-col cols="12" md="4">
              <v-select
                v-model="form.status"
                label="状态"
                :items="statusOptions"
                item-title="title"
                item-value="value"
              />
            </v-col>

            <v-col v-if="form.outlineType === 'chapter'" cols="12" md="6">
              <v-select
                v-model="form.generatedChapterId"
                label="绑定章节"
                :items="chapterOptions"
                item-title="title"
                item-value="value"
                clearable
              />
            </v-col>
            <v-col cols="12" :md="form.outlineType === 'chapter' ? 6 : 4">
              <v-text-field v-model="form.orderNum" type="number" label="排序号" />
            </v-col>

            <v-col cols="12">
              <v-text-field v-model="form.title" label="大纲标题" />
            </v-col>
            <v-col cols="12">
              <MarkdownEditor v-model="form.summary" label="摘要" :rows="3" preview-empty-text="暂无摘要" />
            </v-col>
            <v-col cols="12" md="6">
              <MarkdownEditor v-model="form.stageGoal" label="阶段目标" :rows="3" preview-empty-text="暂无阶段目标" />
            </v-col>
            <v-col cols="12" md="6">
              <MarkdownEditor v-model="form.keyConflict" label="核心冲突" :rows="3" preview-empty-text="暂无核心冲突" />
            </v-col>
            <v-col cols="12" md="6">
              <MarkdownEditor v-model="form.turningPoints" label="关键转折" :rows="3" preview-empty-text="暂无关键转折" />
            </v-col>
            <v-col cols="12" md="6">
              <MarkdownEditor v-model="form.expectedEnding" label="收束方向" :rows="3" preview-empty-text="暂无收束方向" />
            </v-col>
            <v-col cols="12">
              <MarkdownEditor
                v-model="form.content"
                label="详细大纲正文"
                :rows="8"
                placeholder="可以按阶段、场景、节拍或事件顺序展开详细大纲。"
                preview-empty-text="暂无详细大纲正文"
              />
            </v-col>
            <v-col cols="12" md="3">
              <v-select
                v-model="form.focusCharacterIds"
                label="聚焦人物"
                :items="characterOptions"
                item-title="title"
                item-value="value"
                multiple
                chips
                closable-chips
                clearable
              />
            </v-col>
            <v-col cols="12" md="3">
              <v-select
                v-model="form.relatedPlotIds"
                label="关联剧情节点"
                :items="plotOptions"
                item-title="title"
                item-value="value"
                multiple
                chips
                closable-chips
                clearable
              />
            </v-col>
            <v-col cols="12" md="3">
              <v-select
                v-model="form.relatedCausalityIds"
                label="关联因果链"
                :items="causalityOptions"
                item-title="title"
                item-value="value"
                multiple
                chips
                closable-chips
                clearable
              />
            </v-col>
            <v-col cols="12" md="3">
              <v-select
                v-model="form.relatedWorldSettingIds"
                label="关联世界观"
                :items="worldSettingOptions"
                item-title="title"
                item-value="value"
                multiple
                chips
                closable-chips
                clearable
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
      title="删除大纲"
      text="确认删除这条大纲吗？删除后它将不再参与 AI 写作上下文。"
      @confirm="confirmDelete"
    />
  </PageContainer>
</template>
