<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import PageContainer from '@/components/PageContainer.vue'
import { useProjectStore } from '@/stores/project'
import { useWorldSettingStore } from '@/stores/world-setting'
import type { WorldSetting } from '@/types'

const projectStore = useProjectStore()
const worldSettingStore = useWorldSettingStore()

const dialog = ref(false)
const confirmVisible = ref(false)
const editingId = ref<number | null>(null)
const pendingItem = ref<WorldSetting | null>(null)
const pendingAction = ref<'detach' | 'delete'>('detach')
const submitLoading = ref(false)
const actionLoading = ref(false)

const projectId = computed(() => projectStore.selectedProjectId)
const currentProject = computed(() =>
  projectStore.projects.find((item) => item.id === projectId.value) || null,
)

const associatedIdSet = computed(() => new Set(worldSettingStore.items.map((item) => item.id)))
const libraryCandidates = computed(() =>
  worldSettingStore.libraryItems.filter((item) => !associatedIdSet.value.has(item.id)),
)

const categoryOptions = [
  '世界规则',
  '地理版图',
  '势力组织',
  '历史背景',
  '文化习俗',
  '魔法体系',
  '科技体系',
  '关键道具',
]

const form = reactive({
  name: '',
  description: '',
  category: '世界规则',
})

watch(
  projectId,
  async (id) => {
    if (id) {
      await loadData(id)
    }
  },
  { immediate: true },
)

onMounted(async () => {
  if (!projectStore.projects.length) {
    await projectStore.fetchProjects().catch(() => undefined)
  }
  await worldSettingStore.fetchLibrary().catch(() => undefined)
})

function fillForm(setting?: WorldSetting | null) {
  Object.assign(form, {
    name: setting?.name || setting?.title || '',
    description: setting?.description || setting?.content || '',
    category: setting?.category || '世界规则',
  })
}

function openCreate() {
  editingId.value = null
  fillForm(null)
  dialog.value = true
}

function openEdit(setting: WorldSetting) {
  editingId.value = setting.id
  fillForm(setting)
  dialog.value = true
}

function requestDetach(setting: WorldSetting) {
  pendingItem.value = setting
  pendingAction.value = 'detach'
  confirmVisible.value = true
}

function requestDelete(setting: WorldSetting) {
  pendingItem.value = setting
  pendingAction.value = 'delete'
  confirmVisible.value = true
}

async function submit() {
  if (!projectId.value) {
    return
  }

  submitLoading.value = true
  try {
    const payload = {
      projectId: projectId.value,
      name: form.name.trim(),
      description: form.description.trim(),
      category: form.category.trim(),
    }

    if (editingId.value) {
      await worldSettingStore.update(editingId.value, payload)
    } else {
      await worldSettingStore.create(payload)
    }

    dialog.value = false
    await loadData(projectId.value)
  } finally {
    submitLoading.value = false
  }
}

async function confirmAction() {
  if (!pendingItem.value || !projectId.value) {
    return
  }

  actionLoading.value = true
  try {
    if (pendingAction.value === 'detach') {
      await worldSettingStore.detach(projectId.value, pendingItem.value.id)
    } else {
      await worldSettingStore.remove(pendingItem.value.id)
    }
    await loadData(projectId.value)
    confirmVisible.value = false
    pendingItem.value = null
  } finally {
    actionLoading.value = false
  }
}

async function attachItem(setting: WorldSetting) {
  if (!projectId.value) {
    return
  }
  actionLoading.value = true
  try {
    await worldSettingStore.attach(projectId.value, setting.id)
    await loadData(projectId.value)
  } finally {
    actionLoading.value = false
  }
}

async function loadData(id: number) {
  await Promise.allSettled([worldSettingStore.fetchByProject(id), worldSettingStore.fetchLibrary()])
}
</script>

<template>
  <PageContainer
    title="世界观管理"
    description="把世界规则、势力、地理、历史和关键设定沉淀成可复用的世界观模型。当前项目可以直接关联已有模型，不需要重复创建。"
  >
    <template #actions>
      <v-btn color="primary" prepend-icon="mdi-plus" :disabled="!projectId" @click="openCreate">
        新建世界观模型
      </v-btn>
    </template>

    <EmptyState
      v-if="!projectId"
      title="先选择一个项目"
      description="世界观模型可以在多个项目之间复用，请先在左侧选择当前项目，再决定要关联哪些模型。"
    />

    <div v-else class="page-grid">
      <v-card class="soft-panel">
        <v-card-title>当前项目已关联世界观</v-card-title>
        <v-card-subtitle>
          {{ currentProject?.name || '当前项目' }} 已关联 {{ worldSettingStore.items.length }} 个世界观模型
        </v-card-subtitle>
        <v-card-text class="pt-4">
          <EmptyState
            v-if="!worldSettingStore.items.length"
            title="当前项目还没有世界观"
            description="你可以先新建一个世界观模型，或者从下方世界观库里直接关联已有模型。"
          />

          <v-row v-else>
            <v-col v-for="item in worldSettingStore.items" :key="item.id" cols="12" md="6" xl="4">
              <v-card class="h-100 border-sm">
                <v-card-text class="d-flex flex-column h-100">
                  <div class="d-flex justify-space-between align-start ga-3">
                    <div>
                      <div class="text-h6">{{ item.name || item.title || '未命名世界观' }}</div>
                      <div class="text-body-2 text-medium-emphasis mt-1">
                        {{ item.category || '未分类' }}
                      </div>
                    </div>
                    <v-chip color="secondary" variant="tonal">
                      关联 {{ item.associationCount || 1 }} 个项目
                    </v-chip>
                  </div>

                  <div class="text-body-2 mt-4">
                    {{ item.description || item.content || '暂无设定描述' }}
                  </div>

                  <div class="d-flex ga-2 mt-auto pt-4">
                    <v-btn variant="outlined" @click="openEdit(item)">编辑</v-btn>
                    <v-btn variant="text" color="warning" @click="requestDetach(item)">取消关联</v-btn>
                    <v-btn variant="text" color="error" @click="requestDelete(item)">彻底删除</v-btn>
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>

      <v-card class="soft-panel">
        <v-card-title>世界观模型库</v-card-title>
        <v-card-subtitle>这里保存你已经创建过的世界观模型，可直接关联到当前项目。</v-card-subtitle>
        <v-card-text class="pt-4">
          <EmptyState
            v-if="!worldSettingStore.libraryItems.length"
            title="还没有世界观模型库"
            description="先新建一个世界观模型，后续它就会自动进入模型库，可在其他项目中复用。"
          />

          <EmptyState
            v-else-if="!libraryCandidates.length"
            title="当前项目已经关联了全部世界观模型"
            description="如果还想扩展设定，可以继续新建新的世界观模型。"
          />

          <v-row v-else>
            <v-col v-for="item in libraryCandidates" :key="item.id" cols="12" md="6" xl="4">
              <v-card class="h-100 border-sm">
                <v-card-text class="d-flex flex-column h-100">
                  <div class="d-flex justify-space-between align-start ga-3">
                    <div>
                      <div class="text-h6">{{ item.name || item.title || '未命名世界观' }}</div>
                      <div class="text-body-2 text-medium-emphasis mt-1">
                        {{ item.category || '未分类' }}
                      </div>
                    </div>
                    <v-chip variant="outlined">
                      已被 {{ item.associationCount || 0 }} 个项目使用
                    </v-chip>
                  </div>

                  <div class="text-body-2 mt-4">
                    {{ item.description || item.content || '暂无设定描述' }}
                  </div>

                  <div class="d-flex ga-2 mt-auto pt-4">
                    <v-btn color="primary" variant="flat" :loading="actionLoading" @click="attachItem(item)">
                      关联到当前项目
                    </v-btn>
                    <v-btn variant="outlined" @click="openEdit(item)">编辑</v-btn>
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>
    </div>

    <v-dialog v-model="dialog" max-width="820">
      <v-card>
        <v-card-title>{{ editingId ? '编辑世界观模型' : '新建世界观模型' }}</v-card-title>
        <v-card-text class="pt-4">
          <v-row>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.name" label="模型名称" />
            </v-col>
            <v-col cols="12" md="6">
              <v-combobox v-model="form.category" label="模型分类" :items="categoryOptions" />
            </v-col>
            <v-col cols="12">
              <v-textarea
                v-model="form.description"
                rows="8"
                label="设定描述"
                hint="可以录入世界规则、地理背景、阵营介绍、历史事件等内容。新建后会自动关联到当前项目，同时进入世界观模型库。"
                persistent-hint
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
      :title="pendingAction === 'detach' ? '取消世界观关联' : '彻底删除世界观模型'"
      :text="
        pendingAction === 'detach'
          ? '这会将当前世界观从当前项目移除，但它仍会保留在世界观模型库里，可继续关联到其他项目。'
          : '这会从世界观模型库中彻底删除该模型，并从所有已关联项目中移除。'
      "
      @confirm="confirmAction"
    />
  </PageContainer>
</template>
