<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import PageContainer from '@/components/PageContainer.vue'
import { useChapterStore } from '@/stores/chapter'
import { usePlotStore } from '@/stores/plot'
import { useProjectStore } from '@/stores/project'

const projectStore = useProjectStore()
const chapterStore = useChapterStore()
const plotStore = usePlotStore()

const projectId = computed(() => projectStore.selectedProjectId)
const dialog = ref(false)
const deletingId = ref<number | null>(null)
const confirmVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  chapterId: null as number | null,
  title: '',
  description: '',
  content: '',
  plotType: 1,
  sequence: 1,
  characters: '',
  locations: '',
  timeline: '',
  conflicts: '',
  resolutions: '',
  tags: '',
  status: 1,
})

watch(
  projectId,
  async (id) => {
    if (id) {
      await Promise.allSettled([plotStore.fetchByProject(id), chapterStore.fetchByProject(id)])
    }
  },
  { immediate: true },
)

onMounted(() => {
  if (!projectStore.projects.length) {
    projectStore.fetchProjects().catch(() => undefined)
  }
})

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    chapterId: chapterStore.chapters[0]?.id ?? null,
    title: '',
    description: '',
    content: '',
    plotType: 1,
    sequence: plotStore.plotlines.length + 1,
    characters: '',
    locations: '',
    timeline: '',
    conflicts: '',
    resolutions: '',
    tags: '',
    status: 1,
  })
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
    characters: target.characters || '',
    locations: target.locations || '',
    timeline: target.timeline || '',
    conflicts: target.conflicts || '',
    resolutions: target.resolutions || '',
    tags: target.tags || '',
    status: target.status || 1,
  })
  dialog.value = true
}

function requestDelete(id: number) {
  deletingId.value = id
  confirmVisible.value = true
}

async function submit() {
  if (!projectId.value) return
  if (editingId.value) {
    await plotStore.update(editingId.value, form)
  } else {
    await plotStore.create(projectId.value, form)
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
    description="现在已经接到真实后端接口，可以按项目维护 plotline 的标题、冲突、时间线与关联章节。"
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
              <v-chip color="primary" variant="tonal">类型 {{ plot.plotType || 1 }}</v-chip>
            </div>

            <div class="text-body-2 mt-4">冲突：{{ plot.conflicts || '暂无' }}</div>
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

    <v-dialog v-model="dialog" max-width="860">
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
                :items="chapterStore.chapters"
              />
            </v-col>
            <v-col cols="12" md="3">
              <v-text-field v-model="form.plotType" label="剧情类型" type="number" />
            </v-col>
            <v-col cols="12" md="3">
              <v-text-field v-model="form.sequence" label="排序" type="number" />
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
              <v-text-field v-model="form.characters" label="涉及角色" />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.locations" label="涉及地点" />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.timeline" label="时间线" />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.tags" label="标签" />
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
