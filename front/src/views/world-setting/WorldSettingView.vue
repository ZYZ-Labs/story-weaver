<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import MarkdownContent from '@/components/MarkdownContent.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import PageContainer from '@/components/PageContainer.vue'
import SummaryWorkflowDialog from '@/components/SummaryWorkflowDialog.vue'
import { useProjectStore } from '@/stores/project'
import { useWorldSettingStore } from '@/stores/world-setting'
import type { SummaryWorkflowApplyResult, SummaryWorkflowOperatorMode, WorldSetting } from '@/types'

const projectStore = useProjectStore()
const worldSettingStore = useWorldSettingStore()

const dialog = ref(false)
const confirmVisible = ref(false)
const editingId = ref<number | null>(null)
const pendingItem = ref<WorldSetting | null>(null)
const pendingAction = ref<'detach' | 'delete'>('detach')
const submitLoading = ref(false)
const actionLoading = ref(false)
const summaryWorkflowVisible = ref(false)
const summaryWorkflowTarget = ref<WorldSetting | null>(null)
const summaryWorkflowCreateMode = ref(false)
const editorMode = ref<SummaryWorkflowOperatorMode>('DEFAULT')
const worldSettingCardTabs = reactive<Record<number, string>>({})

const projectId = computed(() => projectStore.selectedProjectId)
const currentProject = computed(() =>
  projectStore.projects.find((item) => item.id === projectId.value) || null,
)
const summaryFirstMode = computed(() => editorMode.value === 'DEFAULT')
const createButtonLabel = computed(() => (summaryFirstMode.value ? '说想法新增世界观' : '摘要新增世界观'))

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

function openCreateForm() {
  editingId.value = null
  fillForm(null)
  dialog.value = true
}

function openCreate() {
  summaryWorkflowCreateMode.value = true
  summaryWorkflowTarget.value = null
  summaryWorkflowVisible.value = true
}

function openEditForm(setting: WorldSetting) {
  editingId.value = setting.id
  fillForm(setting)
  dialog.value = true
}

function openEdit(setting: WorldSetting) {
  openSummaryWorkflow(setting)
}

function openSummaryWorkflow(setting?: WorldSetting | null) {
  summaryWorkflowCreateMode.value = !setting
  summaryWorkflowTarget.value = setting || null
  summaryWorkflowVisible.value = true
}

function handleSummaryWorkflowExpertEditRequest() {
  summaryWorkflowVisible.value = false
  if (summaryWorkflowCreateMode.value) {
    openCreateForm()
    return
  }
  if (summaryWorkflowTarget.value) {
    openEditForm(summaryWorkflowTarget.value)
  }
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

async function handleSummaryWorkflowApplied(_result: SummaryWorkflowApplyResult) {
  if (!projectId.value) {
    return
  }
  await loadData(projectId.value)
}

function getWorldSettingCardTab(settingId: number) {
  if (!worldSettingCardTabs[settingId]) {
    worldSettingCardTabs[settingId] = 'summary'
  }
  return worldSettingCardTabs[settingId]
}
</script>

<template>
  <PageContainer
    title="世界观管理"
    description="把世界规则、势力、地理、历史和关键设定沉淀成可复用的世界观模型。当前项目可以直接关联已有模型，不需要重复创建。"
  >
    <template #actions>
      <div class="d-flex flex-wrap ga-2 align-center">
        <template v-if="summaryFirstMode">
          <v-chip color="primary" variant="tonal">普通模式</v-chip>
          <div class="text-caption text-medium-emphasis">默认只需要说设定想法，AI 会继续追问并整理。</div>
          <v-btn variant="text" color="secondary" @click="editorMode = 'EXPERT'">切到专家模式</v-btn>
        </template>
        <v-segmented-button v-else v-model="editorMode" color="primary" mandatory>
          <v-btn value="DEFAULT">普通模式</v-btn>
          <v-btn value="EXPERT">专家模式</v-btn>
        </v-segmented-button>
        <v-btn color="primary" prepend-icon="mdi-plus" :disabled="!projectId" @click="openCreate">
          {{ createButtonLabel }}
        </v-btn>
      </div>
    </template>

    <EmptyState
      v-if="!projectId"
      title="先选择一个项目"
      description="世界观模型可以在多个项目之间复用，请先在左侧选择当前项目，再决定要关联哪些模型。"
    />

    <div v-else class="page-grid">
      <v-alert class="mb-4" type="info" variant="tonal">
        {{
          summaryFirstMode
            ? '当前是普通模式：新增和编辑都先走对话式摘要工作流；你只需要说模糊印象，AI 会继续追问并整理。'
            : '当前是专家模式：新增和编辑仍先进入摘要工作流，但默认直填摘要；如需分类、名称等细字段可切到专家表单。'
        }}
      </v-alert>

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

                  <div class="mt-4">
                    <v-tabs
                      :model-value="getWorldSettingCardTab(item.id)"
                      color="primary"
                      density="comfortable"
                      @update:model-value="(value) => (worldSettingCardTabs[item.id] = String(value))"
                    >
                      <v-tab value="summary">Summary</v-tab>
                      <v-tab value="canon">Canon</v-tab>
                      <v-tab value="state">State</v-tab>
                      <v-tab value="history">History</v-tab>
                    </v-tabs>

                    <v-window :model-value="getWorldSettingCardTab(item.id)" class="mt-4">
                      <v-window-item value="summary">
                        <div class="text-caption text-medium-emphasis">设定摘要</div>
                        <MarkdownContent
                          class="mt-2"
                          :source="item.description || item.content"
                          empty-text="暂无设定描述"
                          compact
                        />
                      </v-window-item>

                      <v-window-item value="canon">
                        <div class="text-caption text-medium-emphasis">设定基线</div>
                        <div class="text-caption text-medium-emphasis mt-3">
                          名称：{{ item.name || item.title || '未命名世界观' }}
                        </div>
                        <div class="text-caption text-medium-emphasis mt-1">
                          分类：{{ item.category || '未分类' }}
                        </div>
                      </v-window-item>

                      <v-window-item value="state">
                        <div class="text-caption text-medium-emphasis">当前使用状态</div>
                        <div class="d-flex flex-wrap ga-2 mt-3">
                          <v-chip color="secondary" variant="tonal" size="small">
                            关联项目 {{ item.associationCount || 1 }}
                          </v-chip>
                          <v-chip color="primary" variant="outlined" size="small">
                            当前项目已关联
                          </v-chip>
                        </div>
                      </v-window-item>

                      <v-window-item value="history">
                        <div class="text-caption text-medium-emphasis">历史信息</div>
                        <div class="text-caption text-medium-emphasis mt-3">
                          创建时间：{{ item.createTime || '未记录' }}
                        </div>
                        <div class="text-caption text-medium-emphasis mt-1">
                          更新时间：{{ item.updateTime || '未记录' }}
                        </div>
                      </v-window-item>
                    </v-window>
                  </div>

                  <div class="d-flex flex-wrap ga-2 mt-auto pt-4">
                    <v-btn color="primary" prepend-icon="mdi-text-box-edit-outline" @click="openSummaryWorkflow(item)">
                      摘要优先编辑
                    </v-btn>
                    <v-btn variant="text" @click="openEdit(item)">编辑</v-btn>
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

                  <div class="mt-4">
                    <v-tabs
                      :model-value="getWorldSettingCardTab(item.id)"
                      color="primary"
                      density="comfortable"
                      @update:model-value="(value) => (worldSettingCardTabs[item.id] = String(value))"
                    >
                      <v-tab value="summary">Summary</v-tab>
                      <v-tab value="canon">Canon</v-tab>
                      <v-tab value="state">State</v-tab>
                      <v-tab value="history">History</v-tab>
                    </v-tabs>

                    <v-window :model-value="getWorldSettingCardTab(item.id)" class="mt-4">
                      <v-window-item value="summary">
                        <div class="text-caption text-medium-emphasis">设定摘要</div>
                        <MarkdownContent
                          class="mt-2"
                          :source="item.description || item.content"
                          empty-text="暂无设定描述"
                          compact
                        />
                      </v-window-item>

                      <v-window-item value="canon">
                        <div class="text-caption text-medium-emphasis">设定基线</div>
                        <div class="text-caption text-medium-emphasis mt-3">
                          名称：{{ item.name || item.title || '未命名世界观' }}
                        </div>
                        <div class="text-caption text-medium-emphasis mt-1">
                          分类：{{ item.category || '未分类' }}
                        </div>
                      </v-window-item>

                      <v-window-item value="state">
                        <div class="text-caption text-medium-emphasis">当前使用状态</div>
                        <div class="d-flex flex-wrap ga-2 mt-3">
                          <v-chip color="secondary" variant="tonal" size="small">
                            已被 {{ item.associationCount || 0 }} 个项目使用
                          </v-chip>
                          <v-chip color="primary" variant="outlined" size="small">
                            可关联到当前项目
                          </v-chip>
                        </div>
                      </v-window-item>

                      <v-window-item value="history">
                        <div class="text-caption text-medium-emphasis">历史信息</div>
                        <div class="text-caption text-medium-emphasis mt-3">
                          创建时间：{{ item.createTime || '未记录' }}
                        </div>
                        <div class="text-caption text-medium-emphasis mt-1">
                          更新时间：{{ item.updateTime || '未记录' }}
                        </div>
                      </v-window-item>
                    </v-window>
                  </div>

                  <div class="d-flex flex-wrap ga-2 mt-auto pt-4">
                    <v-btn color="primary" variant="flat" :loading="actionLoading" @click="attachItem(item)">
                      关联到当前项目
                    </v-btn>
                    <v-btn
                      color="primary"
                      prepend-icon="mdi-text-box-edit-outline"
                      variant="tonal"
                      @click="openSummaryWorkflow(item)"
                    >
                      摘要优先编辑
                    </v-btn>
                    <v-btn variant="text" @click="openEdit(item)">编辑</v-btn>
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
              <MarkdownEditor
                v-model="form.description"
                label="设定描述"
                :rows="8"
                hint="可以录入世界规则、地理背景、阵营介绍、历史事件等内容。新建后会自动关联到当前项目，同时进入世界观模型库。"
                persistent-hint
                preview-empty-text="暂无设定描述"
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

    <SummaryWorkflowDialog
      v-model="summaryWorkflowVisible"
      :project-id="projectId"
      target-type="WORLD_SETTING"
      :create-mode="summaryWorkflowCreateMode"
      :initial-operator-mode="editorMode"
      :allow-expert-form-switch="true"
      :target-source-id="summaryWorkflowTarget?.id || null"
      :title="summaryWorkflowTarget?.name || summaryWorkflowTarget?.title || '新世界观模型'"
      target-label="世界观模型"
      :initial-summary="summaryWorkflowTarget?.description || summaryWorkflowTarget?.content || ''"
      @applied="handleSummaryWorkflowApplied"
      @expert-edit-request="handleSummaryWorkflowExpertEditRequest"
    />
  </PageContainer>
</template>
