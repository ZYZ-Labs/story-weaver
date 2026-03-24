<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import EmptyState from '@/components/EmptyState.vue'
import PageContainer from '@/components/PageContainer.vue'
import { useCharacterStore } from '@/stores/character'
import { useProjectStore } from '@/stores/project'

const projectStore = useProjectStore()
const characterStore = useCharacterStore()
const dialog = ref(false)

const currentProjectId = computed(() => projectStore.selectedProjectId)
const form = reactive({
  name: '',
  description: '',
  attributes: '{\n  "camp": "",\n  "goal": ""\n}',
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

async function submit() {
  if (!currentProjectId.value) {
    return
  }

  await characterStore.create(currentProjectId.value, form)
  dialog.value = false
}
</script>

<template>
  <PageContainer
    title="人物管理"
    description="维护角色卡、属性 JSON 和核心描述，后续可继续扩展到关系图谱与登场轨迹。"
  >
    <template #actions>
      <v-btn color="primary" prepend-icon="mdi-account-plus-outline" :disabled="!currentProjectId" @click="dialog = true">
        添加人物
      </v-btn>
    </template>

    <EmptyState
      v-if="!currentProjectId"
      title="尚未选择项目"
      description="当前人物列表与项目强绑定，请先在左侧选中一个小说项目。"
    />

    <v-row v-else>
      <v-col v-for="character in characterStore.characters" :key="character.id" cols="12" md="6" xl="4">
        <v-card class="soft-panel h-100">
          <v-card-text>
            <div class="d-flex align-center justify-space-between">
              <div class="text-h6">{{ character.name }}</div>
              <v-icon icon="mdi-account-star-outline" color="secondary" />
            </div>
            <div class="text-body-2 text-medium-emphasis mt-3">
              {{ character.description || '暂无角色简介' }}
            </div>
            <v-divider class="my-4" />
            <div class="text-caption text-medium-emphasis">属性</div>
            <pre class="text-body-2 mt-2" style="white-space: pre-wrap">{{ character.attributes || '{}' }}</pre>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-dialog v-model="dialog" max-width="760">
      <v-card>
        <v-card-title>新增人物</v-card-title>
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
  </PageContainer>
</template>
