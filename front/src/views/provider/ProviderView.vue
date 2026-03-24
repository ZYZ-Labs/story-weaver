<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import PageContainer from '@/components/PageContainer.vue'
import { useProviderStore } from '@/stores/provider'

const providerStore = useProviderStore()
const dialog = ref(false)
const deletingId = ref<number | null>(null)
const confirmVisible = ref(false)
const editingId = ref<number | null>(null)
const feedbackMessage = ref('')
const feedbackType = ref<'success' | 'error'>('success')

const providerPresets = [
  {
    title: 'OpenAI Compatible',
    value: 'openai-compatible',
    baseUrl: 'http://127.0.0.1:8000/v1',
    models: ['gpt-4.1', 'gpt-4o-mini', 'gpt-4.1-mini'],
    embeddings: ['text-embedding-3-large', 'text-embedding-3-small'],
  },
  {
    title: 'DeepSeek',
    value: 'deepseek',
    baseUrl: 'https://api.deepseek.com',
    models: ['deepseek-chat', 'deepseek-reasoner'],
    embeddings: ['deepseek-embedding'],
  },
  {
    title: 'Ollama',
    value: 'ollama',
    baseUrl: 'http://localhost:11434/v1',
    models: ['qwen2.5:14b', 'llama3.1:8b', 'deepseek-r1:14b'],
    embeddings: ['nomic-embed-text', 'bge-m3'],
  },
] as const

const form = reactive({
  name: '',
  providerType: 'openai-compatible',
  baseUrl: '',
  apiKey: '',
  modelName: '',
  embeddingModel: '',
  temperature: 0.7,
  topP: 1,
  maxTokens: 4096,
  timeoutSeconds: 60,
  enabled: true,
  isDefault: false,
  remark: '',
})

const currentPreset = computed(() => providerPresets.find((item) => item.value === form.providerType))

onMounted(() => {
  providerStore.fetchAll().catch(() => undefined)
  applyPreset('openai-compatible')
})

watch(
  () => form.providerType,
  (next, previous) => {
    applyPreset(next, previous)
  },
)

function getPreset(type: string) {
  return providerPresets.find((item) => item.value === type)
}

function applyPreset(type: string, previousType?: string) {
  const preset = getPreset(type)
  const previousPreset = previousType ? getPreset(previousType) : null
  if (!preset) return

  if (!form.name || form.name === previousPreset?.title) {
    form.name = preset.title
  }

  if (!form.baseUrl || form.baseUrl === previousPreset?.baseUrl) {
    form.baseUrl = preset.baseUrl
  }

  if (!form.modelName || previousPreset?.models.some((item) => item === form.modelName)) {
    form.modelName = preset.models[0] || ''
  }

  if (!form.embeddingModel || previousPreset?.embeddings.some((item) => item === form.embeddingModel)) {
    form.embeddingModel = preset.embeddings[0] || ''
  }
}

function resetForm() {
  Object.assign(form, {
    name: '',
    providerType: 'openai-compatible',
    baseUrl: '',
    apiKey: '',
    modelName: '',
    embeddingModel: '',
    temperature: 0.7,
    topP: 1,
    maxTokens: 4096,
    timeoutSeconds: 60,
    enabled: true,
    isDefault: false,
    remark: '',
  })
  applyPreset('openai-compatible')
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialog.value = true
}

function openEdit(id: number) {
  const target = providerStore.providers.find((item) => item.id === id)
  if (!target) return
  editingId.value = id
  Object.assign(form, {
    name: target.name,
    providerType: target.providerType,
    baseUrl: target.baseUrl || '',
    apiKey: target.apiKey || '',
    modelName: target.modelName || '',
    embeddingModel: target.embeddingModel || '',
    temperature: target.temperature || 0.7,
    topP: target.topP || 1,
    maxTokens: target.maxTokens || 4096,
    timeoutSeconds: target.timeoutSeconds || 60,
    enabled: target.enabled === 1,
    isDefault: target.isDefault === 1,
    remark: target.remark || '',
  })
  dialog.value = true
}

function requestDelete(id: number) {
  deletingId.value = id
  confirmVisible.value = true
}

async function submit() {
  const payload = {
    ...form,
    enabled: form.enabled ? 1 : 0,
    isDefault: form.isDefault ? 1 : 0,
  }

  if (editingId.value) {
    await providerStore.update(editingId.value, payload)
    feedbackType.value = 'success'
    feedbackMessage.value = 'Provider 已更新'
  } else {
    await providerStore.create(payload)
    feedbackType.value = 'success'
    feedbackMessage.value = 'Provider 已创建'
  }
  dialog.value = false
}

async function confirmDelete() {
  if (!deletingId.value) return
  await providerStore.remove(deletingId.value)
  confirmVisible.value = false
  feedbackType.value = 'success'
  feedbackMessage.value = 'Provider 已删除'
}

async function testProvider(id: number) {
  const result = await providerStore.test(id)
  feedbackType.value = result.success ? 'success' : 'error'
  feedbackMessage.value = result.success ? '连通性测试通过' : '连通性测试失败'
}
</script>

<template>
  <PageContainer
    title="AI Provider"
    description="Provider 页面已经切成预设化配置，创建和编辑时优先用选择器控制类型、模型、向量模型、默认状态与启用状态。"
  >
    <template #actions>
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openCreate">新增 Provider</v-btn>
    </template>

    <v-alert v-if="feedbackMessage" :type="feedbackType" variant="tonal" class="mb-4">
      {{ feedbackMessage }}
    </v-alert>

    <v-row>
      <v-col v-for="provider in providerStore.providers" :key="provider.id" cols="12" md="6">
        <v-card class="soft-panel h-100">
          <v-card-text>
            <div class="d-flex align-start justify-space-between ga-3">
              <div>
                <div class="text-h6">{{ provider.name }}</div>
                <div class="text-body-2 text-medium-emphasis mt-2">模型：{{ provider.modelName || '未设置' }}</div>
              </div>
              <div class="d-flex ga-2">
                <v-chip :color="provider.enabled === 1 ? 'success' : 'default'" variant="tonal">
                  {{ provider.enabled === 1 ? '已启用' : '已停用' }}
                </v-chip>
                <v-chip :color="provider.isDefault === 1 ? 'secondary' : 'primary'" variant="tonal">
                  {{ provider.isDefault === 1 ? '默认' : provider.providerType }}
                </v-chip>
              </div>
            </div>

            <div class="text-body-2 mt-4">Base URL：{{ provider.baseUrl || '未设置' }}</div>
            <div class="text-body-2 mt-2">Embedding：{{ provider.embeddingModel || '未设置' }}</div>
            <div class="text-caption text-medium-emphasis mt-3">{{ provider.remark || '暂无备注' }}</div>

            <div class="d-flex ga-2 mt-5">
              <v-btn variant="outlined" @click="openEdit(provider.id)">编辑</v-btn>
              <v-btn color="primary" variant="text" @click="testProvider(provider.id)">测试</v-btn>
              <v-btn color="error" variant="text" @click="requestDelete(provider.id)">删除</v-btn>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-dialog v-model="dialog" max-width="920">
      <v-card>
        <v-card-title>{{ editingId ? '编辑 Provider' : '新增 Provider' }}</v-card-title>
        <v-card-text class="pt-4">
          <v-row>
            <v-col cols="12" md="6">
              <v-select
                v-model="form.providerType"
                label="Provider 类型"
                :items="providerPresets"
                item-title="title"
                item-value="value"
              />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.name" label="显示名称" />
            </v-col>
            <v-col cols="12">
              <v-text-field v-model="form.baseUrl" label="Base URL" />
            </v-col>
            <v-col cols="12" md="6">
              <v-combobox
                v-model="form.modelName"
                label="主模型"
                :items="currentPreset?.models || []"
                clearable
              />
            </v-col>
            <v-col cols="12" md="6">
              <v-combobox
                v-model="form.embeddingModel"
                label="Embedding 模型"
                :items="currentPreset?.embeddings || []"
                clearable
              />
            </v-col>
            <v-col cols="12">
              <v-text-field v-model="form.apiKey" label="API Key" type="password" />
            </v-col>
            <v-col cols="12" md="3">
              <v-text-field v-model="form.temperature" label="Temperature" type="number" />
            </v-col>
            <v-col cols="12" md="3">
              <v-text-field v-model="form.topP" label="Top P" type="number" />
            </v-col>
            <v-col cols="12" md="3">
              <v-text-field v-model="form.maxTokens" label="Max Tokens" type="number" />
            </v-col>
            <v-col cols="12" md="3">
              <v-text-field v-model="form.timeoutSeconds" label="超时(秒)" type="number" />
            </v-col>
            <v-col cols="12" md="6">
              <v-switch v-model="form.enabled" color="primary" label="启用 Provider" inset />
            </v-col>
            <v-col cols="12" md="6">
              <v-switch v-model="form.isDefault" color="secondary" label="设为默认" inset />
            </v-col>
            <v-col cols="12">
              <v-textarea v-model="form.remark" label="备注" rows="3" />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions class="justify-end">
          <v-btn variant="text" @click="dialog = false">取消</v-btn>
          <v-btn color="primary" @click="submit">保存</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <ConfirmDialog v-model="confirmVisible" title="删除 Provider" text="确认删除这个模型配置吗？" @confirm="confirmDelete" />
  </PageContainer>
</template>
