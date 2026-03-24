<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import PageContainer from '@/components/PageContainer.vue'
import { useCharacterStore } from '@/stores/character'
import { useProjectStore } from '@/stores/project'
import type { Character } from '@/types'

const projectStore = useProjectStore()
const characterStore = useCharacterStore()

const dialog = ref(false)
const deletingId = ref<number | null>(null)
const confirmVisible = ref(false)
const editingId = ref<number | null>(null)

const currentProjectId = computed(() => projectStore.selectedProjectId)
const form = reactive({
  name: '',
  description: '',
  attributes: '{\n  "阵营": "",\n  "目标": ""\n}',
})

watch(
  currentProjectId,
  async (projectId) => {
    if (projectId) {
      await characterStore.fetchByProject(projectId).catch(() => undefined)
    }
  },
  { immediate: true },
)

onMounted(() => {
  if (!projectStore.projects.length) {
    projectStore.fetchProjects().catch(() => undefined)
  }
})

function formatAttributes(value?: string) {
  if (!value?.trim()) {
    return '{}'
  }

  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

function fillForm(character?: Character | null) {
  Object.assign(form, {
    name: character?.name || '',
    description: character?.description || '',
    attributes: formatAttributes(character?.attributes),
  })
}

function openCreate() {
  editingId.value = null
  fillForm(null)
  dialog.value = true
}

function openEdit(character: Character) {
  editingId.value = character.id
  fillForm(character)
  dialog.value = true
}

function requestDelete(character: Character) {
  deletingId.value = character.id
  confirmVisible.value = true
}

async function submit() {
  if (!currentProjectId.value) {
    return
  }

  const payload = {
    ...form,
    attributes: form.attributes.trim() || '{}',
  }

  if (editingId.value) {
    await characterStore.update(currentProjectId.value, editingId.value, payload)
  } else {
    await characterStore.create(currentProjectId.value, payload)
  }

  dialog.value = false
}

async function confirmDelete() {
  if (!currentProjectId.value || !deletingId.value) {
    return
  }

  await characterStore.remove(currentProjectId.value, deletingId.value)
  deletingId.value = null
  confirmVisible.value = false
}
</script>

<template>
  <PageContainer
    title="人物管理"
    description="维护角色卡、关键描述和扩展属性。现在可以直接在卡片上编辑或删除，避免还要切到别处处理。"
  >
    <template #actions>
      <v-btn color="primary" prepend-icon="mdi-account-plus-outline" :disabled="!currentProjectId" @click="openCreate">
        添加人物
      </v-btn>
    </template>

    <EmptyState
      v-if="!currentProjectId"
      title="尚未选择项目"
      description="当前人物列表与项目强绑定，请先在左侧选中一个小说项目。"
    />

    <EmptyState
      v-else-if="!characterStore.characters.length"
      title="还没有人物"
      description="先创建一个角色，后续剧情、因果和写作中心都可以直接关联它。"
    >
      <v-btn color="primary" prepend-icon="mdi-account-plus-outline" @click="openCreate">创建人物</v-btn>
    </EmptyState>

    <v-row v-else>
      <v-col v-for="character in characterStore.characters" :key="character.id" cols="12" md="6" xl="4">
        <v-card class="soft-panel h-100">
          <v-card-text class="d-flex flex-column h-100">
            <div class="d-flex align-center justify-space-between">
              <div class="text-h6">{{ character.name }}</div>
              <v-icon icon="mdi-account-star-outline" color="secondary" />
            </div>

            <div class="text-body-2 text-medium-emphasis mt-3">
              {{ character.description || '暂无角色简介' }}
            </div>

            <v-divider class="my-4" />

            <div class="text-caption text-medium-emphasis">属性</div>
            <pre class="character-attributes mt-2">{{ formatAttributes(character.attributes) }}</pre>

            <div class="d-flex ga-2 mt-auto pt-4">
              <v-btn variant="outlined" @click="openEdit(character)">编辑</v-btn>
              <v-btn color="error" variant="text" @click="requestDelete(character)">删除</v-btn>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-dialog v-model="dialog" max-width="760">
      <v-card>
        <v-card-title>{{ editingId ? '编辑人物' : '新增人物' }}</v-card-title>
        <v-card-text class="pt-4">
          <v-text-field v-model="form.name" label="角色名" />
          <v-textarea v-model="form.description" rows="4" label="角色描述" class="mt-4" />
          <v-textarea v-model="form.attributes" rows="8" label="属性 JSON" class="mt-4" />
        </v-card-text>
        <v-card-actions class="justify-end">
          <v-btn variant="text" @click="dialog = false">取消</v-btn>
          <v-btn color="primary" @click="submit">保存</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <ConfirmDialog
      v-model="confirmVisible"
      title="删除人物"
      text="确认删除这个人物吗？剧情、因果和知识条目里已经引用的文字不会自动改写。"
      @confirm="confirmDelete"
    />
  </PageContainer>
</template>

<style scoped>
.character-attributes {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
}
</style>
