<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import PageContainer from '@/components/PageContainer.vue'
import { useChapterStore } from '@/stores/chapter'
import { useCharacterStore } from '@/stores/character'
import { usePlotStore } from '@/stores/plot'
import { useProjectStore } from '@/stores/project'

const projectStore = useProjectStore()
const chapterStore = useChapterStore()
const characterStore = useCharacterStore()
const plotStore = usePlotStore()

const projectId = computed(() => projectStore.selectedProjectId)
const dialog = ref(false)
const deletingId = ref<number | null>(null)
const confirmVisible = ref(false)
const editingId = ref<number | null>(null)
const lastAutoTitle = ref('')
const lastAutoDescription = ref('')
const lastAutoTimeline = ref('')

const plotTypeOptions = [
  { title: '主线推进', value: 1 },
  { title: '支线事件', value: 2 },
  { title: '冲突升级', value: 3 },
  { title: '伏笔铺设', value: 4 },
  { title: '结果回收', value: 5 },
]

const statusOptions = [
  { title: '规划中', value: 0 },
  { title: '进行中', value: 1 },
  { title: '已完成', value: 2 },
  { title: '已归档', value: 3 },
]

const form = reactive({
  chapterId: null as number | null,
  title: '',
  description: '',
  content: '',
  plotType: 1,
  sequence: 1,
  characters: [] as string[],
  locations: '',
  timeline: '',
  conflicts: '',
  resolutions: '',
  tags: [] as string[],
  status: 1,
})

const chapterOptions = computed(() => chapterStore.chapters)
const characterOptions = computed(() =>
  characterStore.characters.map((item) => ({
    title: item.name,
    value: item.name,
  })),
)

watch(
  projectId,
  async (id) => {
    if (id) {
      await Promise.allSettled([
        plotStore.fetchByProject(id),
        chapterStore.fetchByProject(id),
        characterStore.fetchByProject(id),
      ])
    }
  },
  { immediate: true },
)

watch(
  () => form.chapterId,
  (chapterId) => {
    const chapter = chapterStore.chapters.find((item) => item.id === chapterId)
    if (!chapter) return

    const autoTitle = `围绕《${chapter.title}》的剧情节点`
    const autoDescription = chapter.content
      ? chapter.content.slice(0, 80)
      : `从章节《${chapter.title}》延展出的剧情事件。`
    const autoTimeline = `第${chapter.orderNum || '?'}章 · ${chapter.title}`

    if (!form.title || form.title === lastAutoTitle.value) {
      form.title = autoTitle
      lastAutoTitle.value = autoTitle
    }

    if (!form.description || form.description === lastAutoDescription.value) {
      form.description = autoDescription
      lastAutoDescription.value = autoDescription
    }

    if (!form.timeline || form.timeline === lastAutoTimeline.value) {
      form.timeline = autoTimeline
      lastAutoTimeline.value = autoTimeline
    }
  },
)

onMounted(() => {
  if (!projectStore.projects.length) {
    projectStore.fetchProjects().catch(() => undefined)
  }
})

function resetForm() {
  Object.assign(form, {
    chapterId: chapterStore.chapters[0]?.id ?? null,
    title: '',
    description: '',
    content: '',
    plotType: 1,
    sequence: plotStore.plotlines.length + 1,
    characters: [],
    locations: '',
    timeline: '',
    conflicts: '',
    resolutions: '',
    tags: [],
    status: 1,
  })
  lastAutoTitle.value = ''
  lastAutoDescription.value = ''
  lastAutoTimeline.value = ''
}

function splitCsv(value?: string) {
  if (!value) return []
  return value
    .split(/[,，]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialog.value = true
}

function openEdit(id: number) {
  const target = plotStore.plotlines.find((item) => item.id === id)
  if (!target) return
  editingId.value = id
  Object.assign(form, {
    chapterId: target.chapterId ?? null,
    title: target.title || '',
    description: target.description || '',
    content: target.content || '',
    plotType: target.plotType || 1,
    sequence: target.sequence || 1,
    characters: splitCsv(target.characters),
    locations: target.locations || '',
    timeline: target.timeline || '',
    conflicts: target.conflicts || '',
    resolutions: target.resolutions || '',
    tags: splitCsv(target.tags),
    status: target.status || 1,
  })
  lastAutoTitle.value = form.title
  lastAutoDescription.value = form.description
  lastAutoTimeline.value = form.timeline
  dialog.value = true
}

function requestDelete(id: number) {
  deletingId.value = id
  confirmVisible.value = true
}

function getPlotTypeLabel(plotType?: number) {
  return plotTypeOptions.find((item) => item.value === plotType)?.title || '未分类'
}

async function submit() {
  if (!projectId.value) return

  const payload = {
    ...form,
    characters: form.characters.join(','),
    tags: form.tags.join(','),
  }

  if (editingId.value) {
    await plotStore.update(editingId.value, payload)
  } else {
    await plotStore.create(projectId.value, payload)
  }
  dialog.value = false
}

async function confirmDelete() {
  if (!deletingId.value) return
  await plotStore.remove(deletingId.value)
  confirmVisible.value = false
}
</script>

<template>
  <PageContainer
    title="剧情管理"
    description="剧情页已经接入项目上下文，创建时可以直接选择章节、关联人物和剧情类型，减少重复录入。"
  >
    <template #actions>
      <v-btn color="primary" prepend-icon="mdi-plus" :disabled="!projectId" @click="openCreate">新增剧情</v-btn>
    </template>

    <EmptyState
      v-if="!projectId"
      title="先选择项目"
      description="剧情线和项目强绑定，请先在左侧切换到一个小说项目。"
    />

    <v-row v-else>
      <v-col v-for="plot in plotStore.plotlines" :key="plot.id" cols="12" md="6">
        <v-card class="soft-panel h-100">
          <v-card-text>
            <div class="d-flex justify-space-between align-start ga-3">
              <div>
                <div class="text-h6">{{ plot.title || '未命名剧情' }}</div>
                <div class="text-body-2 text-medium-emphasis mt-2">{{ plot.description || '暂无剧情简介' }}</div>
              </div>
              <v-chip color="primary" variant="tonal">{{ getPlotTypeLabel(plot.plotType) }}</v-chip>
            </div>

            <div class="text-body-2 mt-4">关联人物：{{ plot.characters || '暂无' }}</div>
            <div class="text-body-2 mt-2">冲突：{{ plot.conflicts || '暂无' }}</div>
            <div class="text-body-2 mt-2">解决：{{ plot.resolutions || '暂无' }}</div>
            <div class="text-caption text-medium-emphasis mt-3">时间线：{{ plot.timeline || '未设置' }}</div>

            <div class="d-flex ga-2 mt-5">
              <v-btn variant="outlined" @click="openEdit(plot.id)">编辑</v-btn>
              <v-btn color="error" variant="text" @click="requestDelete(plot.id)">删除</v-btn>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-dialog v-model="dialog" max-width="920">
      <v-card>
        <v-card-title>{{ editingId ? '编辑剧情' : '新增剧情' }}</v-card-title>
        <v-card-text class="pt-4">
          <v-row>
            <v-col cols="12" md="6">
              <v-select
                v-model="form.chapterId"
                label="关联章节"
                item-title="title"
                item-value="id"
                :items="chapterOptions"
              />
            </v-col>
            <v-col cols="12" md="3">
              <v-select
                v-model="form.plotType"
                label="剧情类型"
                :items="plotTypeOptions"
                item-title="title"
                item-value="value"
              />
            </v-col>
            <v-col cols="12" md="3">
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
            <v-col cols="12">
              <v-textarea v-model="form.description" label="简介" rows="3" />
            </v-col>
            <v-col cols="12">
              <v-textarea v-model="form.content" label="详细内容" rows="6" />
            </v-col>
            <v-col cols="12" md="6">
              <v-select
                v-model="form.characters"
                label="涉及角色"
                :items="characterOptions"
                item-title="title"
                item-value="value"
                multiple
                chips
                closable-chips
              />
            </v-col>
            <v-col cols="12" md="6">
              <v-combobox
                v-model="form.tags"
                label="标签"
                multiple
                chips
                closable-chips
              />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.timeline" label="时间线" />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.locations" label="涉及地点" />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.sequence" label="排序" type="number" />
            </v-col>
            <v-col cols="12">
              <v-textarea v-model="form.conflicts" label="冲突" rows="3" />
            </v-col>
            <v-col cols="12">
              <v-textarea v-model="form.resolutions" label="解决方案" rows="3" />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions class="justify-end">
          <v-btn variant="text" @click="dialog = false">取消</v-btn>
          <v-btn color="primary" @click="submit">保存</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <ConfirmDialog v-model="confirmVisible" title="删除剧情" text="确认删除这条剧情线吗？" @confirm="confirmDelete" />
  </PageContainer>
</template>
