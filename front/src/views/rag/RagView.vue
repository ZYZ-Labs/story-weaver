<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import PageContainer from '@/components/PageContainer.vue'
import { useProjectStore } from '@/stores/project'
import { useRagStore } from '@/stores/rag'

const projectStore = useProjectStore()
const ragStore = useRagStore()
const projectId = computed(() => projectStore.selectedProjectId)

const dialog = ref(false)
const editingId = ref<number | null>(null)
const deletingId = ref<number | null>(null)
const confirmVisible = ref(false)
const searchText = ref('')

const form = reactive({
  sourceType: 'manual_note',
  sourceRefId: '',
  title: '',
  contentText: '',
  summary: '',
  status: 'ready',
})

watch(
  projectId,
  async (id) => {
    if (id) {
      await ragStore.fetchByProject(id).catch(() => undefined)
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
    sourceType: 'manual_note',
    sourceRefId: '',
    title: '',
    contentText: '',
    summary: '',
    status: 'ready',
  })
  dialog.value = true
}

function openEdit(id: number) {
  const target = ragStore.documents.find((item) => item.id === id)
  if (!target) return
  editingId.value = id
  Object.assign(form, {
    sourceType: target.sourceType || 'manual_note',
    sourceRefId: target.sourceRefId || '',
    title: target.title,
    contentText: target.contentText || '',
    summary: target.summary || '',
    status: target.status || 'ready',
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
    await ragStore.update(editingId.value, form)
  } else {
    await ragStore.create(projectId.value, form)
  }
  dialog.value = false
}

async function confirmDelete() {
  if (!deletingId.value) return
  await ragStore.remove(deletingId.value)
  confirmVisible.value = false
}

async function queryKnowledge() {
  if (!projectId.value || !searchText.value.trim()) return
  await ragStore.query(projectId.value, searchText.value)
}

async function reindexKnowledge() {
  if (!projectId.value) return
  const result = await ragStore.reindex(projectId.value)
  alert(`已完成重建索引，文档数：${result.documentCount}`)
}
</script>

<template>
  <PageContainer
    title="RAG 知识库"
    description="已经切成真实知识文档 CRUD，可录入、维护、检索并触发一次项目级重建索引。"
  >
    <template #actions>
      <div class="d-flex ga-2">
        <v-btn color="primary" prepend-icon="mdi-plus" :disabled="!projectId" @click="openCreate">新增文档</v-btn>
        <v-btn variant="outlined" :disabled="!projectId" @click="reindexKnowledge">重建索引</v-btn>
      </div>
    </template>

    <EmptyState
      v-if="!projectId"
      title="先选择项目"
      description="知识文档是项目级资源，请先从左侧选择要维护的项目。"
    />

    <div v-else class="page-grid">
      <div class="stats-grid">
        <v-card class="soft-panel">
          <v-card-text>
            <div class="text-overline">知识文档</div>
            <div class="text-h4 mt-2">{{ ragStore.knowledgeStats.documents }}</div>
          </v-card-text>
        </v-card>
        <v-card class="soft-panel">
          <v-card-text>
            <div class="text-overline">Chunk 估算</div>
            <div class="text-h4 mt-2">{{ ragStore.knowledgeStats.chunks }}</div>
          </v-card-text>
        </v-card>
        <v-card class="soft-panel">
          <v-card-text>
            <div class="text-overline">Indexed 比例</div>
            <div class="text-h4 mt-2">{{ ragStore.knowledgeStats.indexed }}</div>
          </v-card-text>
        </v-card>
      </div>

      <v-card class="soft-panel">
        <v-card-text class="d-flex ga-3 align-center">
          <v-text-field v-model="searchText" label="检索知识文档" hide-details />
          <v-btn color="primary" @click="queryKnowledge">检索</v-btn>
        </v-card-text>
      </v-card>

      <div class="content-grid two-column">
        <v-card class="soft-panel">
          <v-card-title>知识文档</v-card-title>
          <v-list lines="three">
            <v-list-item
              v-for="doc in ragStore.documents"
              :key="doc.id"
              :title="doc.title"
              :subtitle="doc.summary || doc.contentText || '暂无内容'"
            >
              <template #append>
                <div class="d-flex ga-2">
                  <v-btn size="small" variant="text" @click="openEdit(doc.id)">编辑</v-btn>
                  <v-btn size="small" color="error" variant="text" @click="requestDelete(doc.id)">删除</v-btn>
                </div>
              </template>
            </v-list-item>
          </v-list>
        </v-card>

        <v-card class="soft-panel">
          <v-card-title>检索结果</v-card-title>
          <v-list v-if="ragStore.queryResults.length" lines="three">
            <v-list-item
              v-for="doc in ragStore.queryResults"
              :key="doc.id"
              :title="doc.title"
              :subtitle="doc.summary || doc.contentText || '暂无内容'"
            />
          </v-list>
          <v-card-text v-else class="text-medium-emphasis">输入关键词后可以在这里查看命中的知识文档。</v-card-text>
        </v-card>
      </div>
    </div>

    <v-dialog v-model="dialog" max-width="860">
      <v-card>
        <v-card-title>{{ editingId ? '编辑知识文档' : '新增知识文档' }}</v-card-title>
        <v-card-text class="pt-4">
          <v-row>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.title" label="标题" />
            </v-col>
            <v-col cols="12" md="3">
              <v-text-field v-model="form.sourceType" label="来源类型" />
            </v-col>
            <v-col cols="12" md="3">
              <v-text-field v-model="form.sourceRefId" label="来源ID" />
            </v-col>
            <v-col cols="12">
              <v-textarea v-model="form.contentText" label="正文" rows="8" />
            </v-col>
            <v-col cols="12">
              <v-textarea v-model="form.summary" label="摘要" rows="3" />
            </v-col>
            <v-col cols="12" md="4">
              <v-text-field v-model="form.status" label="状态" />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions class="justify-end">
          <v-btn variant="text" @click="dialog = false">取消</v-btn>
          <v-btn color="primary" @click="submit">保存</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <ConfirmDialog v-model="confirmVisible" title="删除知识文档" text="确认删除这份知识文档吗？" @confirm="confirmDelete" />
  </PageContainer>
</template>
