<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import EmptyState from '@/components/EmptyState.vue'
import PageContainer from '@/components/PageContainer.vue'
import { useChapterStore } from '@/stores/chapter'
import { useProjectStore } from '@/stores/project'

const projectStore = useProjectStore()
const chapterStore = useChapterStore()
const dialog = ref(false)
const editingId = ref<number | null>(null)

const currentProjectId = computed(() => projectStore.selectedProjectId)
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

function openCreate() {
  editingId.value = null
  Object.assign(form, { title: '', content: '', orderNum: chapterStore.chapters.length + 1 })
  dialog.value = true
}

async function submit() {
  if (!currentProjectId.value) {
    return
  }

  if (editingId.value) {
    await chapterStore.update(currentProjectId.value, editingId.value, form)
  } else {
    await chapterStore.create(currentProjectId.value, form)
  }
  dialog.value = false
}
</script>

<template>
  <PageContainer
    title="章节管理"
    description="围绕当前项目维护章节顺序、标题和正文内容，为写作中心提供可编辑主数据。"
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

    <div v-else class="content-grid two-column">
      <v-card class="soft-panel">
        <v-card-title>章节列表</v-card-title>
        <v-data-table
          :headers="[
            { title: '顺序', key: 'orderNum' },
            { title: '标题', key: 'title' },
            { title: '字数', key: 'wordCount' },
          ]"
          :items="chapterStore.chapters"
          item-value="id"
          hover
          @click:row="(_event: unknown, row: { item: typeof chapterStore.chapters[number] }) => (chapterStore.currentChapter = row.item)"
        />
      </v-card>

      <v-card class="soft-panel">
        <v-card-title>章节预览</v-card-title>
        <v-card-text v-if="chapterStore.currentChapter">
          <div class="text-h6">{{ chapterStore.currentChapter.title }}</div>
          <div class="text-body-2 text-medium-emphasis mt-2">
            章节序号：{{ chapterStore.currentChapter.orderNum || '-' }}
          </div>
          <v-divider class="my-4" />
          <div style="white-space: pre-wrap">{{ chapterStore.currentChapter.content || '暂无正文。' }}</div>
        </v-card-text>
        <v-card-text v-else class="text-medium-emphasis">选择一章后在这里预览内容。</v-card-text>
      </v-card>
    </div>

    <v-dialog v-model="dialog" max-width="760">
      <v-card>
        <v-card-title>{{ editingId ? '编辑章节' : '新建章节' }}</v-card-title>
        <v-card-text class="pt-4">
          <v-row>
            <v-col cols="12" md="8">
              <v-text-field v-model="form.title" label="章节标题" />
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
  </PageContainer>
</template>
