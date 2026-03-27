<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import PageContainer from '@/components/PageContainer.vue'
import { useProviderStore } from '@/stores/provider'
import { useSettingsStore } from '@/stores/settings'
import type { AIProvider, SystemConfig } from '@/types'

type PromptFormKey =
  | 'draft'
  | 'continue'
  | 'expand'
  | 'rewrite'
  | 'polish'
  | 'plot'
  | 'causality'
  | 'ragQuery'
  | 'knowledgeExtract'
  | 'namingChapter'
  | 'namingCharacter'
  | 'characterAttributes'

type ModelTarget = 'default' | 'draft' | 'writing' | 'naming'

const settingsStore = useSettingsStore()
const providerStore = useProviderStore()

const saving = ref(false)
const saveMessage = ref('')
const errorMessage = ref('')

const providerModelLibrary: Record<string, string[]> = {
  ollama: ['qwen3.5:9b', 'qwen2.5:14b', 'qwen2.5:7b', 'qwen2.5:3b', 'llama3.1:8b'],
  'openai-compatible': ['gpt-4.1', 'gpt-4o-mini', 'gpt-4.1-mini'],
  deepseek: ['deepseek-chat', 'deepseek-reasoner'],
}

const modelOptions = reactive<Record<ModelTarget, string[]>>({
  default: [],
  draft: [],
  writing: [],
  naming: [],
})

const refreshing = reactive<Record<ModelTarget, boolean>>({
  default: false,
  draft: false,
  writing: false,
  naming: false,
})

const form = reactive({
  siteName: '',
  siteDescription: '',
  defaultAiProviderId: null as number | null,
  defaultAiModel: '',
  defaultEmbeddingProviderId: null as number | null,
  draftAiProviderId: null as number | null,
  draftAiModel: '',
  writingAiProviderId: null as number | null,
  writingAiModel: '',
  namingAiProviderId: null as number | null,
  namingAiModel: '',
  maxChapterLength: 5000,
  autoSaveInterval: 300,
  ragEnabled: true,
  registrationEnabled: false,
  maxFailedAttempts: 5,
  lockMinutes: 30,
  defaultTheme: 'light',
  workflowMaxPlanRounds: 1,
  workflowMaxCheckRounds: 1,
  workflowMaxRevisionRounds: 1,
  workflowMaxToolCalls: 2,
  chatMaxActiveChars: 6000,
  chatKeepRecentMessages: 4,
})

const promptForm = reactive<Record<PromptFormKey, string>>({
  draft: '',
  continue: '',
  expand: '',
  rewrite: '',
  polish: '',
  plot: '',
  causality: '',
  ragQuery: '',
  knowledgeExtract: '',
  namingChapter: '',
  namingCharacter: '',
  characterAttributes: '',
})

const promptTemplateMeta: Array<{ key: PromptFormKey; configKey: string; title: string }> = [
  { key: 'draft', configKey: 'prompt.draft', title: '初稿提示词' },
  { key: 'continue', configKey: 'prompt.continue', title: '续写提示词' },
  { key: 'expand', configKey: 'prompt.expand', title: '扩写提示词' },
  { key: 'rewrite', configKey: 'prompt.rewrite', title: '重写提示词' },
  { key: 'polish', configKey: 'prompt.polish', title: '润色提示词' },
  { key: 'plot', configKey: 'prompt.plot', title: '剧情提示词' },
  { key: 'causality', configKey: 'prompt.causality', title: '因果提示词' },
  { key: 'ragQuery', configKey: 'prompt.rag_query', title: '检索提示词' },
  { key: 'knowledgeExtract', configKey: 'prompt.knowledge_extract', title: '知识抽取提示词' },
  { key: 'namingChapter', configKey: 'prompt.naming.chapter', title: '章节命名提示词' },
  { key: 'namingCharacter', configKey: 'prompt.naming.character', title: '人物命名提示词' },
  { key: 'characterAttributes', configKey: 'prompt.character_attributes', title: '人物属性提示词' },
]

const selectedProviders = computed(() => ({
  default: providerStore.providers.find((item) => item.id === form.defaultAiProviderId) || null,
  draft: providerStore.providers.find((item) => item.id === form.draftAiProviderId) || null,
  writing: providerStore.providers.find((item) => item.id === form.writingAiProviderId) || null,
  naming: providerStore.providers.find((item) => item.id === form.namingAiProviderId) || null,
}))

onMounted(async () => {
  await Promise.allSettled([settingsStore.fetchAll(), providerStore.fetchAll()])
  hydrateForms()
  await Promise.allSettled([
    refreshModelOptions('default', true),
    refreshModelOptions('draft', true),
    refreshModelOptions('writing', true),
    refreshModelOptions('naming', true),
  ])
})

watch(() => form.defaultAiProviderId, () => refreshModelOptions('default', true).catch(() => undefined))
watch(() => form.draftAiProviderId, () => refreshModelOptions('draft', true).catch(() => undefined))
watch(() => form.writingAiProviderId, () => refreshModelOptions('writing', true).catch(() => undefined))
watch(() => form.namingAiProviderId, () => refreshModelOptions('naming', true).catch(() => undefined))

function getPreferredProviderId() {
  const defaultProvider = providerStore.providers.find((item) => item.isDefault === 1)
  const ollamaProvider = providerStore.providers.find((item) => item.providerType === 'ollama' && item.enabled === 1)
  return ollamaProvider?.id ?? defaultProvider?.id ?? providerStore.providers[0]?.id ?? null
}

function hydrateForms() {
  const preferredProviderId = getPreferredProviderId()
  form.siteName = settingsStore.getConfigValue('site_name', '织文者')
  form.siteDescription = settingsStore.getConfigValue('site_description', 'AI 长篇创作工作台')
  form.defaultAiProviderId = settingsStore.getNumberValue('default_ai_provider_id', preferredProviderId)
  form.defaultAiModel = settingsStore.getConfigValue('default_ai_model', 'qwen2.5:14b')
  form.defaultEmbeddingProviderId = settingsStore.getNumberValue('default_embedding_provider_id', preferredProviderId)
  form.draftAiProviderId = settingsStore.getNumberValue('draft_ai_provider_id', preferredProviderId)
  form.draftAiModel = settingsStore.getConfigValue('draft_ai_model', 'qwen3.5:9b')
  form.writingAiProviderId = settingsStore.getNumberValue('writing_ai_provider_id', preferredProviderId)
  form.writingAiModel = settingsStore.getConfigValue('writing_ai_model', 'qwen2.5:14b')
  form.namingAiProviderId = settingsStore.getNumberValue('naming_ai_provider_id', preferredProviderId)
  form.namingAiModel = settingsStore.getConfigValue('naming_ai_model', 'qwen2.5:3b')
  form.maxChapterLength = settingsStore.getNumberValue('max_chapter_length', 5000) ?? 5000
  form.autoSaveInterval = settingsStore.getNumberValue('auto_save_interval', 300) ?? 300
  form.ragEnabled = settingsStore.getBooleanValue('rag_enabled', true)
  form.registrationEnabled = settingsStore.getBooleanValue('registration_enabled', false)
  form.maxFailedAttempts = settingsStore.getNumberValue('auth.max_failed_attempts', 5) ?? 5
  form.lockMinutes = settingsStore.getNumberValue('auth.lock_minutes', 30) ?? 30
  form.defaultTheme = settingsStore.getConfigValue('default_theme', 'light')
  form.workflowMaxPlanRounds = settingsStore.getNumberValue('ai.workflow.max_plan_rounds', 1) ?? 1
  form.workflowMaxCheckRounds = settingsStore.getNumberValue('ai.workflow.max_check_rounds', 1) ?? 1
  form.workflowMaxRevisionRounds = settingsStore.getNumberValue('ai.workflow.max_revision_rounds', 1) ?? 1
  form.workflowMaxToolCalls = settingsStore.getNumberValue('ai.workflow.max_tool_calls', 2) ?? 2
  form.chatMaxActiveChars = settingsStore.getNumberValue('ai.chat.max_active_chars', 6000) ?? 6000
  form.chatKeepRecentMessages = settingsStore.getNumberValue('ai.chat.keep_recent_messages', 4) ?? 4

  promptForm.draft = settingsStore.getConfigValue('prompt.draft')
  promptForm.continue = settingsStore.getConfigValue('prompt.continue')
  promptForm.expand = settingsStore.getConfigValue('prompt.expand')
  promptForm.rewrite = settingsStore.getConfigValue('prompt.rewrite')
  promptForm.polish = settingsStore.getConfigValue('prompt.polish')
  promptForm.plot = settingsStore.getConfigValue('prompt.plot')
  promptForm.causality = settingsStore.getConfigValue('prompt.causality')
  promptForm.ragQuery = settingsStore.getConfigValue('prompt.rag_query')
  promptForm.knowledgeExtract = settingsStore.getConfigValue('prompt.knowledge_extract')
  promptForm.namingChapter = settingsStore.getConfigValue('prompt.naming.chapter')
  promptForm.namingCharacter = settingsStore.getConfigValue('prompt.naming.character')
  promptForm.characterAttributes = settingsStore.getConfigValue('prompt.character_attributes')
}

function getModelValue(target: ModelTarget) {
  return (
    {
      default: form.defaultAiModel,
      draft: form.draftAiModel,
      writing: form.writingAiModel,
      naming: form.namingAiModel,
    } as const
  )[target]
}

function setModelValue(target: ModelTarget, value: string) {
  if (target === 'default') form.defaultAiModel = value
  if (target === 'draft') form.draftAiModel = value
  if (target === 'writing') form.writingAiModel = value
  if (target === 'naming') form.namingAiModel = value
}

function buildModelOptions(provider: AIProvider | null, currentValue: string) {
  const values = new Set<string>()
  if (provider?.providerType && providerModelLibrary[provider.providerType]) {
    for (const item of providerModelLibrary[provider.providerType]) values.add(item)
  }
  if (provider?.modelName) values.add(provider.modelName)
  if (currentValue) values.add(currentValue)
  return Array.from(values)
}

async function refreshModelOptions(target: ModelTarget, silent = false) {
  const provider = selectedProviders.value[target]
  modelOptions[target] = buildModelOptions(provider, getModelValue(target))
  if (!provider?.baseUrl) return

  refreshing[target] = true
  try {
    const result = await providerStore.discover({
      providerType: provider.providerType,
      baseUrl: provider.baseUrl,
      apiKey: provider.apiKey,
      timeoutSeconds: provider.timeoutSeconds,
    })
    if (result.success) {
      modelOptions[target] = Array.from(new Set([...modelOptions[target], ...(result.models || [])]))
      if (!modelOptions[target].includes(getModelValue(target))) {
        setModelValue(target, provider.modelName || modelOptions[target][0] || getModelValue(target))
      }
      if (!silent) {
        saveMessage.value = `已刷新${resolveTargetLabel(target)}模型列表`
        errorMessage.value = ''
      }
    } else if (!silent) {
      errorMessage.value = result.message || '刷新模型列表失败'
    }
  } catch (error) {
    if (!silent) {
      errorMessage.value = error instanceof Error ? error.message : '刷新模型列表失败'
    }
  } finally {
    refreshing[target] = false
  }
}

function resolveTargetLabel(target: ModelTarget) {
  const mapping: Record<ModelTarget, string> = {
    default: '默认',
    draft: '初稿',
    writing: '写作中心',
    naming: '命名',
  }
  return mapping[target]
}

function buildPayload(): SystemConfig[] {
  return [
    { configKey: 'site_name', configValue: form.siteName, description: '站点名称' },
    { configKey: 'site_description', configValue: form.siteDescription, description: '站点描述' },
    { configKey: 'default_ai_provider_id', configValue: String(form.defaultAiProviderId ?? ''), description: '默认模型服务' },
    { configKey: 'default_ai_model', configValue: form.defaultAiModel, description: '默认对话模型' },
    { configKey: 'default_embedding_provider_id', configValue: String(form.defaultEmbeddingProviderId ?? ''), description: '默认向量服务' },
    { configKey: 'draft_ai_provider_id', configValue: String(form.draftAiProviderId ?? ''), description: '初稿模型服务' },
    { configKey: 'draft_ai_model', configValue: form.draftAiModel, description: '初稿模型' },
    { configKey: 'writing_ai_provider_id', configValue: String(form.writingAiProviderId ?? ''), description: '写作中心模型服务' },
    { configKey: 'writing_ai_model', configValue: form.writingAiModel, description: '写作中心模型' },
    { configKey: 'naming_ai_provider_id', configValue: String(form.namingAiProviderId ?? ''), description: '命名模型服务' },
    { configKey: 'naming_ai_model', configValue: form.namingAiModel, description: '命名模型' },
    { configKey: 'max_chapter_length', configValue: String(form.maxChapterLength), description: '章节最大字数' },
    { configKey: 'auto_save_interval', configValue: String(form.autoSaveInterval), description: '自动保存间隔' },
    { configKey: 'rag_enabled', configValue: String(form.ragEnabled), description: '是否启用检索' },
    { configKey: 'registration_enabled', configValue: String(form.registrationEnabled), description: '是否允许注册' },
    { configKey: 'auth.max_failed_attempts', configValue: String(form.maxFailedAttempts), description: '最大登录失败次数' },
    { configKey: 'auth.lock_minutes', configValue: String(form.lockMinutes), description: '锁定分钟数' },
    { configKey: 'default_theme', configValue: form.defaultTheme, description: '默认主题' },
    { configKey: 'ai.workflow.max_plan_rounds', configValue: String(form.workflowMaxPlanRounds), description: '规划轮次' },
    { configKey: 'ai.workflow.max_check_rounds', configValue: String(form.workflowMaxCheckRounds), description: '检查轮次' },
    { configKey: 'ai.workflow.max_revision_rounds', configValue: String(form.workflowMaxRevisionRounds), description: '修订轮次' },
    { configKey: 'ai.workflow.max_tool_calls', configValue: String(form.workflowMaxToolCalls), description: '工具调用上限' },
    { configKey: 'ai.chat.max_active_chars', configValue: String(form.chatMaxActiveChars), description: '聊天活动窗口字符数' },
    { configKey: 'ai.chat.keep_recent_messages', configValue: String(form.chatKeepRecentMessages), description: '聊天保留最近消息数' },
    { configKey: 'prompt.draft', configValue: promptForm.draft, description: '初稿提示词' },
    { configKey: 'prompt.continue', configValue: promptForm.continue, description: '续写提示词' },
    { configKey: 'prompt.expand', configValue: promptForm.expand, description: '扩写提示词' },
    { configKey: 'prompt.rewrite', configValue: promptForm.rewrite, description: '重写提示词' },
    { configKey: 'prompt.polish', configValue: promptForm.polish, description: '润色提示词' },
    { configKey: 'prompt.plot', configValue: promptForm.plot, description: '剧情提示词' },
    { configKey: 'prompt.causality', configValue: promptForm.causality, description: '因果提示词' },
    { configKey: 'prompt.rag_query', configValue: promptForm.ragQuery, description: '检索提示词' },
    { configKey: 'prompt.knowledge_extract', configValue: promptForm.knowledgeExtract, description: '知识抽取提示词' },
    { configKey: 'prompt.naming.chapter', configValue: promptForm.namingChapter, description: '章节命名提示词' },
    { configKey: 'prompt.naming.character', configValue: promptForm.namingCharacter, description: '人物命名提示词' },
    { configKey: 'prompt.character_attributes', configValue: promptForm.characterAttributes, description: '人物属性提示词' },
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
    description="统一配置全局默认模型、章节初稿默认、写作中心默认、工作流上限和聊天压缩策略。"
  >
    <template #actions>
      <v-btn color="primary" :loading="saving" @click="saveSettings">保存设置</v-btn>
    </template>

    <div class="settings-stack">
      <v-alert v-if="saveMessage" type="success" variant="tonal">{{ saveMessage }}</v-alert>
      <v-alert v-if="errorMessage" type="error" variant="tonal">{{ errorMessage }}</v-alert>

      <div class="settings-grid">
        <v-card class="soft-panel">
          <v-card-title>基础设置</v-card-title>
          <v-card-text>
            <v-row>
              <v-col cols="12"><v-text-field v-model="form.siteName" label="站点名称" /></v-col>
              <v-col cols="12"><v-text-field v-model="form.siteDescription" label="站点描述" /></v-col>
              <v-col cols="12" md="6">
                <v-select v-model="form.defaultAiProviderId" label="默认模型服务" :items="providerStore.providers" item-title="name" item-value="id" clearable />
              </v-col>
              <v-col cols="12" md="6">
                <div class="d-flex ga-2 align-start">
                  <v-combobox v-model="form.defaultAiModel" class="flex-grow-1" label="默认对话模型" :items="modelOptions.default" clearable />
                  <v-btn class="mt-2" variant="outlined" :loading="refreshing.default" @click="refreshModelOptions('default')">刷新</v-btn>
                </div>
              </v-col>
              <v-col cols="12" md="6">
                <v-select v-model="form.defaultEmbeddingProviderId" label="默认向量服务" :items="providerStore.providers" item-title="name" item-value="id" clearable />
              </v-col>
              <v-col cols="12" md="6">
                <v-select
                  v-model="form.defaultTheme"
                  label="默认主题"
                  :items="[{ title: '浅色', value: 'light' }, { title: '深色', value: 'dark' }]"
                  item-title="title"
                  item-value="value"
                />
              </v-col>
              <v-col cols="12" md="6"><v-text-field v-model="form.maxChapterLength" label="章节最大字数" type="number" /></v-col>
              <v-col cols="12" md="6"><v-text-field v-model="form.autoSaveInterval" label="自动保存间隔（秒）" type="number" /></v-col>
              <v-col cols="12" md="6"><v-switch v-model="form.ragEnabled" color="primary" label="启用知识检索" inset /></v-col>
              <v-col cols="12" md="6"><v-switch v-model="form.registrationEnabled" color="primary" label="允许公开注册" inset /></v-col>
            </v-row>
          </v-card-text>
        </v-card>

        <v-card class="soft-panel">
          <v-card-title>入口模型路由</v-card-title>
          <v-card-text>
            <v-row>
              <v-col cols="12" md="6">
                <v-select v-model="form.draftAiProviderId" label="初稿模型服务" :items="providerStore.providers" item-title="name" item-value="id" clearable />
              </v-col>
              <v-col cols="12" md="6">
                <div class="d-flex ga-2 align-start">
                  <v-combobox v-model="form.draftAiModel" class="flex-grow-1" label="初稿模型" :items="modelOptions.draft" clearable />
                  <v-btn class="mt-2" variant="outlined" :loading="refreshing.draft" @click="refreshModelOptions('draft')">刷新</v-btn>
                </div>
              </v-col>
              <v-col cols="12" md="6">
                <v-select v-model="form.writingAiProviderId" label="写作中心模型服务" :items="providerStore.providers" item-title="name" item-value="id" clearable />
              </v-col>
              <v-col cols="12" md="6">
                <div class="d-flex ga-2 align-start">
                  <v-combobox v-model="form.writingAiModel" class="flex-grow-1" label="写作中心模型" :items="modelOptions.writing" clearable />
                  <v-btn class="mt-2" variant="outlined" :loading="refreshing.writing" @click="refreshModelOptions('writing')">刷新</v-btn>
                </div>
              </v-col>
              <v-col cols="12">
                <v-alert type="info" variant="tonal">建议把章节初稿放在更轻量的模型上，把写作中心放在更强的模型上。</v-alert>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </div>

      <div class="settings-grid">
        <v-card class="soft-panel">
          <v-card-title>工作流限制</v-card-title>
          <v-card-text>
            <v-row>
              <v-col cols="12" md="6"><v-text-field v-model="form.workflowMaxPlanRounds" label="规划轮次" type="number" /></v-col>
              <v-col cols="12" md="6"><v-text-field v-model="form.workflowMaxCheckRounds" label="检查轮次" type="number" /></v-col>
              <v-col cols="12" md="6"><v-text-field v-model="form.workflowMaxRevisionRounds" label="修订轮次" type="number" /></v-col>
              <v-col cols="12" md="6"><v-text-field v-model="form.workflowMaxToolCalls" label="工具调用上限" type="number" /></v-col>
            </v-row>
          </v-card-text>
        </v-card>

        <v-card class="soft-panel">
          <v-card-title>聊天压缩</v-card-title>
          <v-card-text>
            <v-row>
              <v-col cols="12" md="6"><v-text-field v-model="form.chatMaxActiveChars" label="活动窗口最大字符数" type="number" /></v-col>
              <v-col cols="12" md="6"><v-text-field v-model="form.chatKeepRecentMessages" label="保留最近消息数" type="number" /></v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </div>

      <div class="settings-grid">
        <v-card class="soft-panel">
          <v-card-title>登录安全</v-card-title>
          <v-card-text>
            <v-row>
              <v-col cols="12" md="6"><v-text-field v-model="form.maxFailedAttempts" label="最大失败次数" type="number" /></v-col>
              <v-col cols="12" md="6"><v-text-field v-model="form.lockMinutes" label="锁定分钟数" type="number" /></v-col>
            </v-row>
          </v-card-text>
        </v-card>

        <v-card class="soft-panel">
          <v-card-title>命名模型</v-card-title>
          <v-card-text>
            <v-row>
              <v-col cols="12">
                <v-select v-model="form.namingAiProviderId" label="命名模型服务" :items="providerStore.providers" item-title="name" item-value="id" clearable />
              </v-col>
              <v-col cols="12">
                <div class="d-flex ga-2 align-start">
                  <v-combobox v-model="form.namingAiModel" class="flex-grow-1" label="命名模型" :items="modelOptions.naming" clearable />
                  <v-btn class="mt-2" variant="outlined" :loading="refreshing.naming" @click="refreshModelOptions('naming')">刷新</v-btn>
                </div>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </div>

      <v-card class="soft-panel">
        <v-card-title>提示词模板</v-card-title>
        <v-card-text class="pt-4">
          <v-row>
            <v-col v-for="template in promptTemplateMeta" :key="template.configKey" cols="12" md="6">
              <v-textarea v-model="promptForm[template.key]" :label="template.title" rows="6" />
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>
    </div>
  </PageContainer>
</template>

<style scoped>
.settings-stack {
  display: grid;
  gap: 16px;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 960px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
}
</style>
