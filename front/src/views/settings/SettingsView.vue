<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import PageContainer from '@/components/PageContainer.vue'
import { useProviderStore } from '@/stores/provider'
import { useSettingsStore } from '@/stores/settings'
import type { SystemConfig } from '@/types'

type PromptFormKey =
  | 'continue'
  | 'expand'
  | 'rewrite'
  | 'polish'
  | 'plot'
  | 'causality'
  | 'ragQuery'
  | 'knowledgeExtract'

const settingsStore = useSettingsStore()
const providerStore = useProviderStore()
const saving = ref(false)
const saveMessage = ref('')
const errorMessage = ref('')

const generalForm = reactive({
  siteName: '',
  siteDescription: '',
  defaultAiModel: '',
  defaultAiProviderId: null as number | null,
  defaultEmbeddingProviderId: null as number | null,
  maxChapterLength: 5000,
  autoSaveInterval: 300,
  ragEnabled: true,
  registrationEnabled: true,
  defaultTheme: 'light',
})

const promptForm = reactive<Record<PromptFormKey, string>>({
  continue: '',
  expand: '',
  rewrite: '',
  polish: '',
  plot: '',
  causality: '',
  ragQuery: '',
  knowledgeExtract: '',
})

const promptTemplateMeta: Array<{
  key: PromptFormKey
  configKey: string
  title: string
  hint: string
}> = [
  { key: 'continue', configKey: 'prompt.continue', title: '续写 Prompt', hint: '写作中心执行续写时使用。' },
  { key: 'expand', configKey: 'prompt.expand', title: '扩写 Prompt', hint: '写作中心执行扩写时使用。' },
  { key: 'rewrite', configKey: 'prompt.rewrite', title: '改写 Prompt', hint: '写作中心执行改写时使用。' },
  { key: 'polish', configKey: 'prompt.polish', title: '润色 Prompt', hint: '写作中心执行润色时使用。' },
  { key: 'plot', configKey: 'prompt.plot', title: '剧情 Prompt', hint: '剧情建议与剧情整理时使用。' },
  { key: 'causality', configKey: 'prompt.causality', title: '因果 Prompt', hint: '因果分析与因果建议时使用。' },
  { key: 'ragQuery', configKey: 'prompt.rag_query', title: 'RAG 检索 Prompt', hint: '检索知识条目前组织召回策略时使用。' },
  { key: 'knowledgeExtract', configKey: 'prompt.knowledge_extract', title: '知识抽取 Prompt', hint: 'AI 草稿接受后同步到 RAG 时作为抽取模板。' },
]

onMounted(async () => {
  await Promise.allSettled([settingsStore.fetchAll(), providerStore.fetchAll()])
  hydrateForms()
})

function hydrateForms() {
  generalForm.siteName = settingsStore.getConfigValue('site_name', 'Story Weaver')
  generalForm.siteDescription = settingsStore.getConfigValue('site_description', 'AI小说创作平台')
  generalForm.defaultAiModel = settingsStore.getConfigValue('default_ai_model', 'gpt-4.1')
  generalForm.defaultAiProviderId = settingsStore.getNumberValue('default_ai_provider_id', providerStore.providers[0]?.id ?? null)
  generalForm.defaultEmbeddingProviderId = settingsStore.getNumberValue(
    'default_embedding_provider_id',
    providerStore.providers[0]?.id ?? null,
  )
  generalForm.maxChapterLength = settingsStore.getNumberValue('max_chapter_length', 5000) ?? 5000
  generalForm.autoSaveInterval = settingsStore.getNumberValue('auto_save_interval', 300) ?? 300
  generalForm.ragEnabled = settingsStore.getBooleanValue('rag_enabled', true)
  generalForm.registrationEnabled = settingsStore.getBooleanValue('registration_enabled', true)
  generalForm.defaultTheme = settingsStore.getConfigValue('default_theme', 'light')

  promptForm.continue = settingsStore.getConfigValue('prompt.continue')
  promptForm.expand = settingsStore.getConfigValue('prompt.expand')
  promptForm.rewrite = settingsStore.getConfigValue('prompt.rewrite')
  promptForm.polish = settingsStore.getConfigValue('prompt.polish')
  promptForm.plot = settingsStore.getConfigValue('prompt.plot')
  promptForm.causality = settingsStore.getConfigValue('prompt.causality')
  promptForm.ragQuery = settingsStore.getConfigValue('prompt.rag_query')
  promptForm.knowledgeExtract = settingsStore.getConfigValue('prompt.knowledge_extract')
}

function buildPayload(): SystemConfig[] {
  return [
    { configKey: 'site_name', configValue: generalForm.siteName, description: '站点名称' },
    { configKey: 'site_description', configValue: generalForm.siteDescription, description: '站点描述' },
    { configKey: 'default_ai_model', configValue: generalForm.defaultAiModel, description: '默认AI模型' },
    { configKey: 'default_ai_provider_id', configValue: String(generalForm.defaultAiProviderId ?? ''), description: '默认AI Provider' },
    {
      configKey: 'default_embedding_provider_id',
      configValue: String(generalForm.defaultEmbeddingProviderId ?? ''),
      description: '默认Embedding Provider',
    },
    { configKey: 'max_chapter_length', configValue: String(generalForm.maxChapterLength), description: '章节最大字数' },
    { configKey: 'auto_save_interval', configValue: String(generalForm.autoSaveInterval), description: '自动保存间隔（秒）' },
    { configKey: 'rag_enabled', configValue: String(generalForm.ragEnabled), description: 'RAG 是否启用' },
    { configKey: 'registration_enabled', configValue: String(generalForm.registrationEnabled), description: '是否允许注册' },
    { configKey: 'default_theme', configValue: generalForm.defaultTheme, description: '默认主题' },
    { configKey: 'prompt.continue', configValue: promptForm.continue, description: '续写 Prompt 模板' },
    { configKey: 'prompt.expand', configValue: promptForm.expand, description: '扩写 Prompt 模板' },
    { configKey: 'prompt.rewrite', configValue: promptForm.rewrite, description: '改写 Prompt 模板' },
    { configKey: 'prompt.polish', configValue: promptForm.polish, description: '润色 Prompt 模板' },
    { configKey: 'prompt.plot', configValue: promptForm.plot, description: '剧情 Prompt 模板' },
    { configKey: 'prompt.causality', configValue: promptForm.causality, description: '因果 Prompt 模板' },
    { configKey: 'prompt.rag_query', configValue: promptForm.ragQuery, description: 'RAG 检索 Prompt 模板' },
    { configKey: 'prompt.knowledge_extract', configValue: promptForm.knowledgeExtract, description: '知识抽取 Prompt 模板' },
  ]
}

async function saveSettings() {
  saving.value = true
  saveMessage.value = ''
  errorMessage.value = ''

  try {
    await settingsStore.saveAll(buildPayload())
    hydrateForms()
    saveMessage.value = '设置已保存'
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '保存设置失败'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <PageContainer
    title="系统设置"
    description="这里已经接成真实的系统配置页，默认 Provider、生成策略和 Prompt 模板都会通过后端 system_config 表持久化。"
  >
    <template #actions>
      <v-btn color="primary" :loading="saving" @click="saveSettings">保存设置</v-btn>
    </template>

    <div class="page-grid">
      <v-alert v-if="saveMessage" type="success" variant="tonal">{{ saveMessage }}</v-alert>
      <v-alert v-if="errorMessage" type="error" variant="tonal">{{ errorMessage }}</v-alert>

      <div class="content-grid two-column">
        <v-card class="soft-panel">
          <v-card-title>基础策略</v-card-title>
          <v-card-text>
            <v-row>
              <v-col cols="12">
                <v-text-field v-model="generalForm.siteName" label="站点名称" />
              </v-col>
              <v-col cols="12">
                <v-text-field v-model="generalForm.siteDescription" label="站点描述" />
              </v-col>
              <v-col cols="12" md="6">
                <v-combobox
                  v-model="generalForm.defaultAiModel"
                  label="默认 AI 模型"
                  :items="['gpt-4.1', 'gpt-4o-mini', 'gpt-4.1-mini', 'deepseek-chat', 'deepseek-reasoner']"
                />
              </v-col>
              <v-col cols="12" md="6">
                <v-select
                  v-model="generalForm.defaultTheme"
                  label="默认主题"
                  :items="[
                    { title: '浅色', value: 'light' },
                    { title: '深色', value: 'dark' },
                  ]"
                  item-title="title"
                  item-value="value"
                />
              </v-col>
              <v-col cols="12" md="6">
                <v-select
                  v-model="generalForm.defaultAiProviderId"
                  label="默认 AI Provider"
                  :items="providerStore.providers"
                  item-title="name"
                  item-value="id"
                  clearable
                />
              </v-col>
              <v-col cols="12" md="6">
                <v-select
                  v-model="generalForm.defaultEmbeddingProviderId"
                  label="默认 Embedding Provider"
                  :items="providerStore.providers"
                  item-title="name"
                  item-value="id"
                  clearable
                />
              </v-col>
              <v-col cols="12" md="6">
                <v-text-field v-model="generalForm.maxChapterLength" label="章节最大字数" type="number" />
              </v-col>
              <v-col cols="12" md="6">
                <v-text-field v-model="generalForm.autoSaveInterval" label="自动保存间隔（秒）" type="number" />
              </v-col>
              <v-col cols="12" md="6">
                <v-switch v-model="generalForm.ragEnabled" color="primary" label="启用 RAG" inset />
              </v-col>
              <v-col cols="12" md="6">
                <v-switch v-model="generalForm.registrationEnabled" color="primary" label="允许注册" inset />
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>

        <v-card class="soft-panel">
          <v-card-title>当前默认策略</v-card-title>
          <v-list lines="two">
            <v-list-item
              title="默认生成 Provider"
              :subtitle="providerStore.providers.find((item) => item.id === generalForm.defaultAiProviderId)?.name || '未设置'"
            />
            <v-list-item
              title="默认 Embedding Provider"
              :subtitle="providerStore.providers.find((item) => item.id === generalForm.defaultEmbeddingProviderId)?.name || '未设置'"
            />
            <v-list-item title="默认模型" :subtitle="generalForm.defaultAiModel || '未设置'" />
            <v-list-item title="注册开关" :subtitle="generalForm.registrationEnabled ? '允许新用户注册' : '当前关闭注册'" />
            <v-list-item title="RAG 状态" :subtitle="generalForm.ragEnabled ? '当前启用 RAG 检索' : '当前关闭 RAG 检索'" />
          </v-list>
        </v-card>
      </div>

      <v-card class="soft-panel">
        <v-card-title>Prompt 模板</v-card-title>
        <v-card-subtitle>
          除了写作中心的续写 / 扩写 / 改写 / 润色外，这里还新增了剧情、因果、RAG 检索和知识抽取模板。
        </v-card-subtitle>
        <v-card-text class="pt-4">
          <v-row>
            <v-col
              v-for="template in promptTemplateMeta"
              :key="template.configKey"
              cols="12"
              md="6"
            >
              <v-textarea
                v-model="promptForm[template.key]"
                :label="template.title"
                :hint="template.hint"
                persistent-hint
                rows="6"
              />
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>
    </div>
  </PageContainer>
</template>
