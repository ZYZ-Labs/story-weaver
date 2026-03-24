<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import PageContainer from '@/components/PageContainer.vue'
import { useProviderStore } from '@/stores/provider'

const providerStore = useProviderStore()
const dialog = ref(false)
const deletingId = ref<number | null>(null)
const confirmVisible = ref(false)
const editingId = ref<number | null>(null)

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
  enabled: 1,
  isDefault: 0,
  remark: '',
})

onMounted(() => {
  providerStore.fetchAll().catch(() => undefined)
})

function openCreate() {
  editingId.value = null
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
    enabled: 1,
    isDefault: 0,
    remark: '',
  })
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
    enabled: target.enabled || 1,
    isDefault: target.isDefault || 0,
    remark: target.remark || '',
  })
  dialog.value = true
}

function requestDelete(id: number) {
  deletingId.value = id
  confirmVisible.value = true
}

async function submit() {
  if (editingId.value) {
    await providerStore.update(editingId.value, form)
  } else {
    await providerStore.create(form)
  }
  dialog.value = false
}

async function confirmDelete() {
  if (!deletingId.value) return
  await providerStore.remove(deletingId.value)
  confirmVisible.value = false
}

async function testProvider(id: number) {
  const result = await providerStore.test(id)
  alert(result.success ? '连通性测试通过' : '连通性测试失败')
}
</script>

<template>
  <PageContainer
    title="AI Provider"
    description="真实 Provider CRUD 已接通，可维护模型服务、默认策略和基础连通性检测。"
  >
    <template #actions>
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openCreate">新增 Provider</v-btn>
    </template>

    <v-row>
      <v-col v-for="provider in providerStore.providers" :key="provider.id" cols="12" md="6">
        <v-card class="soft-panel h-100">
          <v-card-text>
            <div class="d-flex align-start justify-space-between ga-3">
              <div>
                <div class="text-h6">{{ provider.name }}</div>
                <div class="text-body-2 text-medium-emphasis mt-2">模型：{{ provider.modelName || '未设置' }}</div>
              </div>
              <v-chip :color="provider.isDefault ? 'secondary' : 'primary'" variant="tonal">
                {{ provider.isDefault ? '默认' : provider.providerType }}
              </v-chip>
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

    <v-dialog v-model="dialog" max-width="860">
      <v-card>
        <v-card-title>{{ editingId ? '编辑 Provider' : '新增 Provider' }}</v-card-title>
        <v-card-text class="pt-4">
          <v-row>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.name" label="名称" />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.providerType" label="类型" />
            </v-col>
            <v-col cols="12">
              <v-text-field v-model="form.baseUrl" label="Base URL" />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.modelName" label="模型名" />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.embeddingModel" label="Embedding 模型" />
            </v-col>
            <v-col cols="12">
              <v-text-field v-model="form.apiKey" label="API Key" />
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
