<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import PageContainer from '@/components/PageContainer.vue'
import { useProjectStore } from '@/stores/project'
import { compactText, formatDateTime } from '@/utils/format'

const projectStore = useProjectStore()
const router = useRouter()
const dialog = ref(false)
const deletingId = ref<number | null>(null)
const confirmVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  name: '',
  description: '',
  genre: '',
  tags: '',
})

function openCreate() {
  editingId.value = null
  Object.assign(form, { name: '', description: '', genre: '', tags: '' })
  dialog.value = true
}

function openEdit(projectId: number) {
  const project = projectStore.projects.find((item) => item.id === projectId)
  if (!project) {
    return
  }

  editingId.value = projectId
  Object.assign(form, {
    name: project.name,
    description: project.description || '',
    genre: project.genre || '',
    tags: project.tags || '',
  })
  dialog.value = true
}

function promptDelete(projectId: number) {
  deletingId.value = projectId
  confirmVisible.value = true
}

async function submit() {
  if (editingId.value) {
    await projectStore.update(editingId.value, form)
  } else {
    await projectStore.create(form)
  }
  dialog.value = false
}

async function confirmDelete() {
  if (!deletingId.value) {
    return
  }

  await projectStore.remove(deletingId.value)
  confirmVisible.value = false
  deletingId.value = null
}

onMounted(() => {
  projectStore.fetchProjects().catch(() => undefined)
})
</script>

<template>
  <PageContainer
    title="项目管理"
    description="从这里进入小说项目的创建、筛选、编辑和详情页，是整套创作系统的根入口。"
  >
    <template #actions>
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openCreate">新建项目</v-btn>
    </template>

    <EmptyState
      v-if="!projectStore.projects.length"
      title="还没有项目"
      description="先创建第一个小说项目，章节、角色、世界设定和写作中心都会围绕它展开。"
    >
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openCreate">创建项目</v-btn>
    </EmptyState>

    <v-row v-else>
      <v-col v-for="project in projectStore.projects" :key="project.id" cols="12" md="6" xl="4">
        <v-card class="soft-panel h-100">
          <v-card-text class="d-flex flex-column h-100">
            <div class="d-flex align-start justify-space-between ga-3">
              <div>
                <div class="text-h6 font-weight-bold">{{ project.name }}</div>
                <div class="text-body-2 text-medium-emphasis mt-2">
                  {{ compactText(project.description, '暂无项目简介') }}
                </div>
              </div>
              <v-chip color="secondary" variant="tonal">{{ project.genre || '未分类' }}</v-chip>
            </div>

            <div class="mt-4 text-caption text-medium-emphasis">
              更新时间：{{ formatDateTime(project.updateTime) }}
            </div>

            <div class="mt-2 text-caption text-medium-emphasis">
              标签：{{ project.tags || '暂无标签' }}
            </div>

            <v-spacer />

            <div class="d-flex ga-2 mt-6">
              <v-btn color="primary" variant="flat" @click="router.push(`/projects/${project.id}`)">
                查看详情
              </v-btn>
              <v-btn variant="outlined" @click="openEdit(project.id)">编辑</v-btn>
              <v-btn color="error" variant="text" @click="promptDelete(project.id)">
                删除
              </v-btn>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-dialog v-model="dialog" max-width="640">
      <v-card>
        <v-card-title>{{ editingId ? '编辑项目' : '新建项目' }}</v-card-title>
        <v-card-text class="pt-4">
          <v-row>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.name" label="项目名称" />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.genre" label="题材类型" />
            </v-col>
            <v-col cols="12">
              <v-text-field v-model="form.tags" label="标签" hint="多个标签可用逗号分隔" persistent-hint />
            </v-col>
            <v-col cols="12">
              <v-textarea v-model="form.description" label="项目简介" rows="4" />
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
      title="删除项目"
      text="删除后该项目下的章节与角色会失去入口，请确认是否继续。"
      @confirm="confirmDelete"
    />
  </PageContainer>
</template>
