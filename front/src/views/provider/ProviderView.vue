<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import PageContainer from '@/components/PageContainer.vue'
import { useProviderStore } from '@/stores/provider'

type ProviderPreset = {
  title: string
  value: string
  providerType: string
  baseUrl: string
  defaultName: string
  models: string[]
  embeddings: string[]
  remark: string
}

const providerStore = useProviderStore()

const dialog = ref(false)
const confirmVisible = ref(false)
const editingId = ref<number | null>(null)
const deletingId = ref<number | null>(null)
const feedbackMessage = ref('')
const feedbackType = ref<'success' | 'error'>('success')
const fetchingCatalog = ref(false)
const remoteModelOptions = ref<string[]>([])
const remoteEmbeddingOptions = ref<string[]>([])

const providerPresets: ProviderPreset[] = [
  {
    title: '本地 Ollama',
    value: 'ollama-local',
    providerType: 'ollama',
    baseUrl: 'http://127.0.0.1:11434/v1',
    defaultName: '本地 Ollama',
    models: ['qwen2.5:14b', 'qwen2.5:7b', 'llama3.1:8b', 'deepseek-r1:14b'],
    embeddings: ['nomic-embed-text', 'bge-m3', 'mxbai-embed-large'],
    remark: '适合本机开发环境，通常不需要额外密钥。',
  },
  {
    title: '局域网 Ollama',
    value: 'ollama-lan',
    providerType: 'ollama',
    baseUrl: 'http://192.168.5.249:11434/v1',
    defaultName: '局域网 Ollama',
    models: ['qwen2.5:14b', 'qwen2.5:7b', 'llama3.1:8b', 'deepseek-r1:14b'],
    embeddings: ['nomic-embed-text', 'bge-m3', 'mxbai-embed-large'],
    remark: '适合团队共享的 Ollama 服务。',
  },
  {
    title: '兼容云端接口',
    value: 'openai-compatible',
    providerType: 'openai-compatible',
    baseUrl: 'http://127.0.0.1:8000/v1',
    defaultName: '兼容云端接口',
    models: ['gpt-4.1', 'gpt-4o-mini', 'gpt-4.1-mini'],
    embeddings: ['text-embedding-3-large', 'text-embedding-3-small'],
    remark: '仅在需要云端模型时再启用，通常需要填写接口密钥。',
  },
]

const form = reactive({
  presetValue: 'ollama-local',
  name: '',
  providerType: 'ollama',
  baseUrl: '',
  apiKey: '',
  modelName: '',
  embeddingModel: '',
  temperature: 0.7,
  topP: 1,
  maxTokens: 4096,
  timeoutSeconds: 60,
  enabled: true,
  isDefault: true,
  remark: '',
})

const currentPreset = computed(() => providerPresets.find((item) => item.value === form.presetValue) || providerPresets[0])
const isOllama = computed(() => form.providerType === 'ollama')
const canDiscover = computed(() => isOllama.value && Boolean(form.baseUrl.trim()))
const modelOptions = computed(() => mergeOptions(currentPreset.value.models, remoteModelOptions.value, [form.modelName]))
const embeddingOptions = computed(() =>
  mergeOptions(currentPreset.value.embeddings, remoteEmbeddingOptions.value, [form.embeddingModel]),
)

onMounted(async () => {
  await providerStore.fetchAll().catch(() => undefined)
  applyPreset('ollama-local')
})

function mergeOptions(...sources: Array<Array<string | undefined>>) {
  const values = new Set<string>()
  for (const source of sources) {
    for (const value of source) {
      if (value?.trim()) {
        values.add(value.trim())
      }
    }
  }
  return Array.from(values)
}

function clearRemoteCatalog() {
  remoteModelOptions.value = []
  remoteEmbeddingOptions.value = []
}

function getProviderTypeLabel(value?: string) {
  const mapping: Record<string, string> = {
    ollama: 'Ollama',
    'openai-compatible': '兼容接口',
    deepseek: 'DeepSeek',
  }
  return mapping[value || ''] || value || '未分类'
}

function applyPreset(presetValue: string) {
  const preset = providerPresets.find((item) => item.value === presetValue)
  if (!preset) return

  clearRemoteCatalog()
  form.presetValue = preset.value
  form.providerType = preset.providerType
  form.name = preset.defaultName
  form.baseUrl = preset.baseUrl
  form.modelName = preset.models[0] || ''
  form.embeddingModel = preset.embeddings[0] || ''
  form.remark = preset.remark
  if (preset.providerType === 'ollama') {
    form.apiKey = ''
  }
}

function resetForm() {
  clearRemoteCatalog()
  Object.assign(form, {
    presetValue: 'ollama-local',
    name: '',
    providerType: 'ollama',
    baseUrl: '',
    apiKey: '',
    modelName: '',
    embeddingModel: '',
    temperature: 0.7,
    topP: 1,
    maxTokens: 4096,
    timeoutSeconds: 60,
    enabled: true,
    isDefault: true,
    remark: '',
  })
  applyPreset('ollama-local')
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialog.value = true
}

function openEdit(id: number) {
  const target = providerStore.providers.find((item) => item.id === id)
  if (!target) return

  const matchedPreset = providerPresets.find(
    (item) => item.providerType === target.providerType && item.baseUrl === (target.baseUrl || ''),
  )

  clearRemoteCatalog()
  editingId.value = id
  Object.assign(form, {
    presetValue: matchedPreset?.value || (target.providerType === 'ollama' ? 'ollama-local' : 'openai-compatible'),
    name: target.name,
    providerType: target.providerType,
    baseUrl: target.baseUrl || '',
    apiKey: '',
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
  if (!form.name.trim() || !form.baseUrl.trim() || !form.modelName.trim()) {
    feedbackType.value = 'error'
    feedbackMessage.value = '请至少填写显示名称、服务地址和对话模型。'
    return
  }

  try {
    const payload = {
      ...form,
      name: form.name.trim(),
      baseUrl: form.baseUrl.trim(),
      modelName: form.modelName.trim(),
      embeddingModel: form.embeddingModel.trim(),
      remark: form.remark.trim(),
      enabled: form.enabled ? 1 : 0,
      isDefault: form.isDefault ? 1 : 0,
      apiKey: isOllama.value ? '' : form.apiKey.trim(),
    }

    if (editingId.value) {
      await providerStore.update(editingId.value, payload)
      feedbackType.value = 'success'
      feedbackMessage.value = '模型服务已更新。'
    } else {
      await providerStore.create(payload)
      feedbackType.value = 'success'
      feedbackMessage.value = '模型服务已创建。'
    }

    dialog.value = false
  } catch (error) {
    feedbackType.value = 'error'
    feedbackMessage.value = error instanceof Error ? error.message : '保存模型服务失败。'
  }
}

async function confirmDelete() {
  if (!deletingId.value) return
  try {
    await providerStore.remove(deletingId.value)
    confirmVisible.value = false
    feedbackType.value = 'success'
    feedbackMessage.value = '模型服务已删除。'
  } catch (error) {
    feedbackType.value = 'error'
    feedbackMessage.value = error instanceof Error ? error.message : '删除模型服务失败。'
  }
}

async function testProvider(id: number) {
  try {
    const result = await providerStore.test(id)
    feedbackType.value = result.success ? 'success' : 'error'
    feedbackMessage.value = result.success ? '连接测试通过。' : '连接测试失败，请检查地址和模型名。'
  } catch (error) {
    feedbackType.value = 'error'
    feedbackMessage.value = error instanceof Error ? error.message : '连接测试失败。'
  }
}

async function discoverRemoteModels() {
  if (!canDiscover.value) {
    feedbackType.value = 'error'
    feedbackMessage.value = '请先填写可访问的 Ollama 服务地址。'
    return
  }

  fetchingCatalog.value = true
  try {
    const result = await providerStore.discover({
      providerType: form.providerType,
      baseUrl: form.baseUrl.trim(),
      apiKey: form.apiKey.trim(),
      timeoutSeconds: form.timeoutSeconds,
    })

    if (!result.success) {
      feedbackType.value = 'error'
      feedbackMessage.value = result.message || '获取模型列表失败。'
      return
    }

    remoteModelOptions.value = result.models || []
    remoteEmbeddingOptions.value = result.embeddingModels || []

    if (result.models.length && (!form.modelName || !result.models.includes(form.modelName))) {
      form.modelName = result.models[0]
    }
    if (result.embeddingModels.length && (!form.embeddingModel || !result.embeddingModels.includes(form.embeddingModel))) {
      form.embeddingModel = result.embeddingModels[0]
    }

    feedbackType.value = 'success'
    feedbackMessage.value = result.message || '连接成功，已获取模型列表。'
  } catch (error) {
    feedbackType.value = 'error'
    feedbackMessage.value = error instanceof Error ? error.message : '获取模型列表失败。'
  } finally {
    fetchingCatalog.value = false
  }
}
</script>

<template>
  <PageContainer
    title="模型服务"
    description="这里优先服务本地和局域网 Ollama。常用情况下只需要填写服务地址、对话模型和向量模型，其余高级参数可以按需展开。"
  >
    <template #actions>
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openCreate">新增模型服务</v-btn>
    </template>

    <v-alert type="info" variant="tonal" class="mb-4">
      推荐先准备 Ollama 环境：
      <code>ollama serve</code>
      <code class="ml-2">ollama pull qwen2.5:14b</code>
      <code class="ml-2">ollama pull nomic-embed-text</code>
    </v-alert>

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
                <div class="text-body-2 text-medium-emphasis mt-2">
                  服务类型：{{ getProviderTypeLabel(provider.providerType) }}
                </div>
              </div>
              <div class="d-flex ga-2">
                <v-chip :color="provider.enabled === 1 ? 'success' : 'default'" variant="tonal">
                  {{ provider.enabled === 1 ? '已启用' : '已停用' }}
                </v-chip>
                <v-chip :color="provider.isDefault === 1 ? 'secondary' : 'default'" variant="tonal">
                  {{ provider.isDefault === 1 ? '默认服务' : '备用服务' }}
                </v-chip>
              </div>
            </div>

            <div class="text-body-2 mt-4">服务地址：{{ provider.baseUrl || '未设置' }}</div>
            <div class="text-body-2 mt-2">对话模型：{{ provider.modelName || '未设置' }}</div>
            <div class="text-body-2 mt-2">向量模型：{{ provider.embeddingModel || '未设置' }}</div>
            <div class="text-caption text-medium-emphasis mt-3">{{ provider.remark || '暂无备注' }}</div>

            <div class="d-flex ga-2 mt-5">
              <v-btn variant="outlined" @click="openEdit(provider.id)">编辑</v-btn>
              <v-btn color="primary" variant="text" @click="testProvider(provider.id)">测试连接</v-btn>
              <v-btn color="error" variant="text" @click="requestDelete(provider.id)">删除</v-btn>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-dialog v-model="dialog" max-width="920">
      <v-card>
        <v-card-title>{{ editingId ? '编辑模型服务' : '新增模型服务' }}</v-card-title>
        <v-card-text class="pt-4">
          <v-row>
            <v-col cols="12">
              <v-select
                :model-value="form.presetValue"
                label="快速预设"
                :items="providerPresets"
                item-title="title"
                item-value="value"
                @update:model-value="(value) => value && applyPreset(String(value))"
              />
            </v-col>

            <v-col cols="12">
              <v-alert type="info" variant="tonal">
                当前预设：{{ currentPreset.title }}。{{ currentPreset.remark }}
              </v-alert>
            </v-col>

            <v-col cols="12" md="6">
              <v-text-field v-model="form.name" label="显示名称" />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.baseUrl" label="服务地址" />
            </v-col>

            <v-col cols="12" md="6">
              <div class="d-flex ga-2 align-start">
                <v-combobox
                  v-model="form.modelName"
                  class="flex-grow-1"
                  label="对话模型"
                  :items="modelOptions"
                  clearable
                />
                <v-btn
                  v-if="isOllama"
                  class="mt-2"
                  variant="outlined"
                  :loading="fetchingCatalog"
                  :disabled="!canDiscover"
                  @click="discoverRemoteModels"
                >
                  获取
                </v-btn>
              </div>
            </v-col>

            <v-col cols="12" md="6">
              <div class="d-flex ga-2 align-start">
                <v-combobox
                  v-model="form.embeddingModel"
                  class="flex-grow-1"
                  label="向量模型"
                  :items="embeddingOptions"
                  clearable
                />
                <v-btn
                  v-if="isOllama"
                  class="mt-2"
                  variant="outlined"
                  :loading="fetchingCatalog"
                  :disabled="!canDiscover"
                  @click="discoverRemoteModels"
                >
                  获取
                </v-btn>
              </div>
            </v-col>

            <v-col v-if="isOllama && (remoteModelOptions.length || remoteEmbeddingOptions.length)" cols="12">
              <v-alert type="success" variant="tonal">
                已获取 {{ remoteModelOptions.length }} 个对话模型，{{ remoteEmbeddingOptions.length }} 个向量模型，可直接从下拉中选择。
              </v-alert>
            </v-col>

            <v-col v-if="!isOllama" cols="12">
              <v-text-field v-model="form.apiKey" label="接口密钥" type="password" />
            </v-col>

            <v-col cols="12" md="6">
              <v-switch v-model="form.enabled" color="primary" label="启用该服务" inset />
            </v-col>
            <v-col cols="12" md="6">
              <v-switch v-model="form.isDefault" color="secondary" label="设为默认服务" inset />
            </v-col>
            <v-col cols="12">
              <v-textarea v-model="form.remark" label="备注" rows="3" />
            </v-col>

            <v-col cols="12">
              <v-expansion-panels variant="accordion">
                <v-expansion-panel>
                  <v-expansion-panel-title>高级参数</v-expansion-panel-title>
                  <v-expansion-panel-text>
                    <v-row>
                      <v-col cols="12" md="3">
                        <v-text-field v-model="form.temperature" label="温度系数" type="number" />
                      </v-col>
                      <v-col cols="12" md="3">
                        <v-text-field v-model="form.topP" label="Top P" type="number" />
                      </v-col>
                      <v-col cols="12" md="3">
                        <v-text-field v-model="form.maxTokens" label="最大输出长度" type="number" />
                      </v-col>
                      <v-col cols="12" md="3">
                        <v-text-field v-model="form.timeoutSeconds" label="超时时间（秒）" type="number" />
                      </v-col>
                    </v-row>
                  </v-expansion-panel-text>
                </v-expansion-panel>
              </v-expansion-panels>
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
      title="删除模型服务"
      text="确认删除这条模型服务配置吗？"
      @confirm="confirmDelete"
    />
  </PageContainer>
</template>
