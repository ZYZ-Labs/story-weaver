<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { getWorldSettings } from '@/api/world-setting'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import PageContainer from '@/components/PageContainer.vue'
import { useProjectStore } from '@/stores/project'
import { useWorldSettingStore } from '@/stores/world-setting'
import { compactText, formatDateTime } from '@/utils/format'

const projectStore = useProjectStore()
const worldSettingStore = useWorldSettingStore()
const router = useRouter()

const dialog = ref(false)
const deletingId = ref<number | null>(null)
const confirmVisible = ref(false)
const editingId = ref<number | null>(null)
const submitLoading = ref(false)

const form = reactive({
  name: '',
  description: '',
  genre: '',
  tags: '',
  worldSettingIds: [] as number[],
})

const worldSettingOptions = computed(() =>
  worldSettingStore.libraryItems.map((item) => ({
    title: item.name || item.title || '未命名世界观',
    value: item.id,
    subtitle: item.category || '未分类',
  })),
)

function resetForm() {
  Object.assign(form, {
    name: '',
    description: '',
    genre: '',
    tags: '',
    worldSettingIds: [],
  })
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialog.value = true
}

async function openEdit(projectId: number) {
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
    worldSettingIds: project.worldSettingIds ? [...project.worldSettingIds] : [],
  })

  if (!project.worldSettingIds) {
    const associatedSettings = await getWorldSettings(projectId).catch(() => [])
    form.worldSettingIds = associatedSettings.map((item) => item.id)
  }
  dialog.value = true
}

function promptDelete(projectId: number) {
  deletingId.value = projectId
  confirmVisible.value = true
}

async function submit() {
  submitLoading.value = true
  try {
    const payload = {
      name: form.name.trim(),
      description: form.description.trim(),
      genre: form.genre.trim(),
      tags: form.tags.trim(),
      worldSettingIds: [...form.worldSettingIds],
    }

    if (editingId.value) {
      await projectStore.update(editingId.value, payload)
    } else {
      await projectStore.create(payload)
    }
    dialog.value = false
  } finally {
    submitLoading.value = false
  }
}

async function confirmDelete() {
  if (!deletingId.value) {
    return
  }

  await projectStore.remove(deletingId.value)
  confirmVisible.value = false
  deletingId.value = null
}

onMounted(async () => {
  await Promise.allSettled([projectStore.fetchProjects(), worldSettingStore.fetchLibrary()])
})
</script>

<template>
  <PageContainer
    title="项目管理"
    description="从这里进入小说项目的创建、筛选、编辑和详情页。现在项目可以直接关联已有世界观模型，不需要每次重新录入。"
  >
    <template #actions>
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openCreate">新建项目</v-btn>
    </template>

    <EmptyState
      v-if="!projectStore.projects.length"
      title="还没有项目"
      description="先创建第一个小说项目，章节、人物、世界观和写作中心都会围绕它展开。"
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

            <div class="mt-4">
              <div class="text-caption text-medium-emphasis">关联世界观</div>
              <div class="d-flex flex-wrap ga-2 mt-2">
                <v-chip
                  v-for="name in (project.worldSettingNames || []).slice(0, 3)"
                  :key="name"
                  color="secondary"
                  size="small"
                  variant="tonal"
                >
                  {{ name }}
                </v-chip>
                <span
                  v-if="!project.worldSettingNames?.length"
                  class="text-caption text-medium-emphasis"
                >
                  暂未关联已有世界观
                </span>
                <v-chip
                  v-else-if="project.worldSettingNames.length > 3"
                  size="small"
                  variant="outlined"
                >
                  +{{ project.worldSettingNames.length - 3 }}
                </v-chip>
              </div>
            </div>

            <v-spacer />

            <div class="d-flex ga-2 mt-6">
              <v-btn color="primary" variant="flat" @click="router.push(`/projects/${project.id}`)">
                查看详情
              </v-btn>
              <v-btn variant="outlined" @click="openEdit(project.id)">编辑</v-btn>
              <v-btn color="error" variant="text" @click="promptDelete(project.id)">删除</v-btn>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-dialog v-model="dialog" max-width="760">
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
              <v-text-field
                v-model="form.tags"
                label="标签"
                hint="多个标签可用逗号分隔"
                persistent-hint
              />
            </v-col>
            <v-col cols="12">
              <v-textarea v-model="form.description" label="项目简介" rows="4" />
            </v-col>
            <v-col cols="12">
              <v-autocomplete
                v-model="form.worldSettingIds"
                label="关联已有世界观"
                :items="worldSettingOptions"
                item-title="title"
                item-value="value"
                multiple
                chips
                closable-chips
                clearable
                hint="可直接选择已有世界观模型，保存后会自动关联到当前项目。"
                persistent-hint
                no-data-text="当前还没有可选的世界观模型"
              />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions class="justify-end">
          <v-btn variant="text" @click="dialog = false">取消</v-btn>
          <v-btn color="primary" :loading="submitLoading" @click="submit">保存</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <ConfirmDialog
      v-model="confirmVisible"
      title="删除项目"
      text="删除后项目入口会消失，但世界观模型仍会保留在你的世界观库中，可继续关联到其他项目。"
      @confirm="confirmDelete"
    />
  </PageContainer>
</template>
