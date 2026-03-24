<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import PageContainer from '@/components/PageContainer.vue'
import { useCausalityStore } from '@/stores/causality'
import { useProjectStore } from '@/stores/project'

const projectStore = useProjectStore()
const causalityStore = useCausalityStore()

const projectId = computed(() => projectStore.selectedProjectId)
const dialog = ref(false)
const editingId = ref<number | null>(null)
const deletingId = ref<number | null>(null)
const confirmVisible = ref(false)

const form = reactive({
  name: '',
  description: '',
  causeType: '',
  effectType: '',
  causeEntityId: '',
  effectEntityId: '',
  causeEntityType: '',
  effectEntityType: '',
  relationship: 'causes',
  strength: 50,
  conditions: '',
  tags: '',
  status: 1,
})

watch(
  projectId,
  async (id) => {
    if (id) {
      await causalityStore.fetchByProject(id).catch(() => undefined)
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
    name: '',
    description: '',
    causeType: '',
    effectType: '',
    causeEntityId: '',
    effectEntityId: '',
    causeEntityType: '',
    effectEntityType: '',
    relationship: 'causes',
    strength: 50,
    conditions: '',
    tags: '',
    status: 1,
  })
  dialog.value = true
}

function openEdit(id: number) {
  const target = causalityStore.nodes.find((item) => item.id === id)
  if (!target) return
  editingId.value = id
  Object.assign(form, {
    name: target.name || '',
    description: target.description || '',
    causeType: target.causeType || '',
    effectType: target.effectType || '',
    causeEntityId: target.causeEntityId || '',
    effectEntityId: target.effectEntityId || '',
    causeEntityType: target.causeEntityType || '',
    effectEntityType: target.effectEntityType || '',
    relationship: target.relationship || 'causes',
    strength: target.strength || 50,
    conditions: target.conditions || '',
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
    await causalityStore.update(editingId.value, form)
  } else {
    await causalityStore.create(projectId.value, form)
  }
  dialog.value = false
}

async function confirmDelete() {
  if (!deletingId.value) return
  await causalityStore.remove(deletingId.value)
  confirmVisible.value = false
}
</script>

<template>
  <PageContainer
    title="因果管理"
    description="现在这里已经是可编辑的真实数据页，可以维护原因、结果、关系类型和强度。"
  >
    <template #actions>
      <v-btn color="primary" prepend-icon="mdi-plus" :disabled="!projectId" @click="openCreate">新增因果</v-btn>
    </template>

    <EmptyState
      v-if="!projectId"
      title="先选择项目"
      description="因果关系和项目上下文强绑定，请先选择当前项目。"
    />

    <div v-else class="content-grid two-column">
      <v-card class="soft-panel">
        <v-card-title>因果关系</v-card-title>
        <v-table>
          <thead>
            <tr>
              <th>名称</th>
              <th>关系</th>
              <th>强度</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="node in causalityStore.nodes" :key="node.id">
              <td>{{ node.name || '未命名因果' }}</td>
              <td>{{ node.relationship || '-' }}</td>
              <td>{{ node.strength || 0 }}</td>
              <td>
                <div class="d-flex ga-2">
                  <v-btn size="small" variant="text" @click="openEdit(node.id)">编辑</v-btn>
                  <v-btn size="small" color="error" variant="text" @click="requestDelete(node.id)">删除</v-btn>
                </div>
              </td>
            </tr>
          </tbody>
        </v-table>
      </v-card>

      <v-card class="soft-panel">
        <v-card-title>关系说明</v-card-title>
        <v-list lines="three">
          <v-list-item
            v-for="node in causalityStore.nodes.slice(0, 6)"
            :key="node.id"
            :title="node.name || '未命名因果'"
            :subtitle="node.description || '暂无说明'"
          />
        </v-list>
      </v-card>
    </div>

    <v-dialog v-model="dialog" max-width="900">
      <v-card>
        <v-card-title>{{ editingId ? '编辑因果' : '新增因果' }}</v-card-title>
        <v-card-text class="pt-4">
          <v-row>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.name" label="名称" />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.relationship" label="关系类型" />
            </v-col>
            <v-col cols="12">
              <v-textarea v-model="form.description" label="描述" rows="3" />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.causeType" label="原因类型" />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.effectType" label="结果类型" />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.causeEntityId" label="原因实体ID" />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.effectEntityId" label="结果实体ID" />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.causeEntityType" label="原因实体类型" />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.effectEntityType" label="结果实体类型" />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.strength" type="number" label="强度" />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.tags" label="标签" />
            </v-col>
            <v-col cols="12">
              <v-textarea v-model="form.conditions" label="条件" rows="3" />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions class="justify-end">
          <v-btn variant="text" @click="dialog = false">取消</v-btn>
          <v-btn color="primary" @click="submit">保存</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <ConfirmDialog v-model="confirmVisible" title="删除因果" text="确认删除这条因果关系吗？" @confirm="confirmDelete" />
  </PageContainer>
</template>
