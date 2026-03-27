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
  { key: 'draft', configKey: 'prompt.draft', title: 'Draft' },
  { key: 'continue', configKey: 'prompt.continue', title: 'Continue' },
  { key: 'expand', configKey: 'prompt.expand', title: 'Expand' },
  { key: 'rewrite', configKey: 'prompt.rewrite', title: 'Rewrite' },
  { key: 'polish', configKey: 'prompt.polish', title: 'Polish' },
  { key: 'plot', configKey: 'prompt.plot', title: 'Plot' },
  { key: 'causality', configKey: 'prompt.causality', title: 'Causality' },
  { key: 'ragQuery', configKey: 'prompt.rag_query', title: 'RAG Query' },
  { key: 'knowledgeExtract', configKey: 'prompt.knowledge_extract', title: 'Knowledge Extract' },
  { key: 'namingChapter', configKey: 'prompt.naming.chapter', title: 'Chapter Naming' },
  { key: 'namingCharacter', configKey: 'prompt.naming.character', title: 'Character Naming' },
  { key: 'characterAttributes', configKey: 'prompt.character_attributes', title: 'Character Attributes' },
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
  form.siteName = settingsStore.getConfigValue('site_name', 'Story Weaver')
  form.siteDescription = settingsStore.getConfigValue('site_description', 'AI fiction workspace')
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
        saveMessage.value = `Refreshed ${target} models.`
        errorMessage.value = ''
      }
    } else if (!silent) {
      errorMessage.value = result.message || 'Refresh failed.'
    }
  } catch (error) {
    if (!silent) {
      errorMessage.value = error instanceof Error ? error.message : 'Refresh failed.'
    }
  } finally {
    refreshing[target] = false
  }
}

function buildPayload(): SystemConfig[] {
  return [
    { configKey: 'site_name', configValue: form.siteName, description: 'Site name' },
    { configKey: 'site_description', configValue: form.siteDescription, description: 'Site description' },
    { configKey: 'default_ai_provider_id', configValue: String(form.defaultAiProviderId ?? ''), description: 'Default provider' },
    { configKey: 'default_ai_model', configValue: form.defaultAiModel, description: 'Default model' },
    { configKey: 'default_embedding_provider_id', configValue: String(form.defaultEmbeddingProviderId ?? ''), description: 'Default embedding provider' },
    { configKey: 'draft_ai_provider_id', configValue: String(form.draftAiProviderId ?? ''), description: 'Draft provider' },
    { configKey: 'draft_ai_model', configValue: form.draftAiModel, description: 'Draft model' },
    { configKey: 'writing_ai_provider_id', configValue: String(form.writingAiProviderId ?? ''), description: 'Writing center provider' },
    { configKey: 'writing_ai_model', configValue: form.writingAiModel, description: 'Writing center model' },
    { configKey: 'naming_ai_provider_id', configValue: String(form.namingAiProviderId ?? ''), description: 'Naming provider' },
    { configKey: 'naming_ai_model', configValue: form.namingAiModel, description: 'Naming model' },
    { configKey: 'max_chapter_length', configValue: String(form.maxChapterLength), description: 'Max chapter length' },
    { configKey: 'auto_save_interval', configValue: String(form.autoSaveInterval), description: 'Auto-save interval' },
    { configKey: 'rag_enabled', configValue: String(form.ragEnabled), description: 'Enable retrieval' },
    { configKey: 'registration_enabled', configValue: String(form.registrationEnabled), description: 'Allow registration' },
    { configKey: 'auth.max_failed_attempts', configValue: String(form.maxFailedAttempts), description: 'Max failed logins' },
    { configKey: 'auth.lock_minutes', configValue: String(form.lockMinutes), description: 'Login lock minutes' },
    { configKey: 'default_theme', configValue: form.defaultTheme, description: 'Theme' },
    { configKey: 'ai.workflow.max_plan_rounds', configValue: String(form.workflowMaxPlanRounds), description: 'Plan rounds' },
    { configKey: 'ai.workflow.max_check_rounds', configValue: String(form.workflowMaxCheckRounds), description: 'Check rounds' },
    { configKey: 'ai.workflow.max_revision_rounds', configValue: String(form.workflowMaxRevisionRounds), description: 'Revision rounds' },
    { configKey: 'ai.workflow.max_tool_calls', configValue: String(form.workflowMaxToolCalls), description: 'Tool calls' },
    { configKey: 'ai.chat.max_active_chars', configValue: String(form.chatMaxActiveChars), description: 'Chat active chars' },
    { configKey: 'ai.chat.keep_recent_messages', configValue: String(form.chatKeepRecentMessages), description: 'Chat recent messages' },
    { configKey: 'prompt.draft', configValue: promptForm.draft, description: 'Draft prompt' },
    { configKey: 'prompt.continue', configValue: promptForm.continue, description: 'Continue prompt' },
    { configKey: 'prompt.expand', configValue: promptForm.expand, description: 'Expand prompt' },
    { configKey: 'prompt.rewrite', configValue: promptForm.rewrite, description: 'Rewrite prompt' },
    { configKey: 'prompt.polish', configValue: promptForm.polish, description: 'Polish prompt' },
    { configKey: 'prompt.plot', configValue: promptForm.plot, description: 'Plot prompt' },
    { configKey: 'prompt.causality', configValue: promptForm.causality, description: 'Causality prompt' },
    { configKey: 'prompt.rag_query', configValue: promptForm.ragQuery, description: 'RAG query prompt' },
    { configKey: 'prompt.knowledge_extract', configValue: promptForm.knowledgeExtract, description: 'Knowledge extract prompt' },
    { configKey: 'prompt.naming.chapter', configValue: promptForm.namingChapter, description: 'Chapter naming prompt' },
    { configKey: 'prompt.naming.character', configValue: promptForm.namingCharacter, description: 'Character naming prompt' },
    { configKey: 'prompt.character_attributes', configValue: promptForm.characterAttributes, description: 'Character attributes prompt' },
  ]
}

async function saveSettings() {
  saving.value = true
  saveMessage.value = ''
  errorMessage.value = ''
  try {
    await settingsStore.saveAll(buildPayload())
    hydrateForms()
    saveMessage.value = 'Settings saved.'
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Save failed.'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <PageContainer
    title="System Settings"
    description="Configure global defaults, draft defaults, writing-center defaults, workflow caps, and chat compression."
  >
    <template #actions>
      <v-btn color="primary" :loading="saving" @click="saveSettings">Save settings</v-btn>
    </template>

    <div class="settings-stack">
      <v-alert v-if="saveMessage" type="success" variant="tonal">{{ saveMessage }}</v-alert>
      <v-alert v-if="errorMessage" type="error" variant="tonal">{{ errorMessage }}</v-alert>

      <div class="settings-grid">
        <v-card class="soft-panel">
          <v-card-title>Base</v-card-title>
          <v-card-text>
            <v-row>
              <v-col cols="12"><v-text-field v-model="form.siteName" label="Site name" /></v-col>
              <v-col cols="12"><v-text-field v-model="form.siteDescription" label="Site description" /></v-col>
              <v-col cols="12" md="6">
                <v-select v-model="form.defaultAiProviderId" label="Default provider" :items="providerStore.providers" item-title="name" item-value="id" clearable />
              </v-col>
              <v-col cols="12" md="6">
                <div class="d-flex ga-2 align-start">
                  <v-combobox v-model="form.defaultAiModel" class="flex-grow-1" label="Default model" :items="modelOptions.default" clearable />
                  <v-btn class="mt-2" variant="outlined" :loading="refreshing.default" @click="refreshModelOptions('default')">Refresh</v-btn>
                </div>
              </v-col>
              <v-col cols="12" md="6">
                <v-select v-model="form.defaultEmbeddingProviderId" label="Embedding provider" :items="providerStore.providers" item-title="name" item-value="id" clearable />
              </v-col>
              <v-col cols="12" md="6">
                <v-select
                  v-model="form.defaultTheme"
                  label="Theme"
                  :items="[{ title: 'Light', value: 'light' }, { title: 'Dark', value: 'dark' }]"
                  item-title="title"
                  item-value="value"
                />
              </v-col>
              <v-col cols="12" md="6"><v-text-field v-model="form.maxChapterLength" label="Max chapter length" type="number" /></v-col>
              <v-col cols="12" md="6"><v-text-field v-model="form.autoSaveInterval" label="Auto-save seconds" type="number" /></v-col>
              <v-col cols="12" md="6"><v-switch v-model="form.ragEnabled" color="primary" label="Enable retrieval" inset /></v-col>
              <v-col cols="12" md="6"><v-switch v-model="form.registrationEnabled" color="primary" label="Allow registration" inset /></v-col>
            </v-row>
          </v-card-text>
        </v-card>

        <v-card class="soft-panel">
          <v-card-title>Model Routing</v-card-title>
          <v-card-text>
            <v-row>
              <v-col cols="12" md="6">
                <v-select v-model="form.draftAiProviderId" label="Draft provider" :items="providerStore.providers" item-title="name" item-value="id" clearable />
              </v-col>
              <v-col cols="12" md="6">
                <div class="d-flex ga-2 align-start">
                  <v-combobox v-model="form.draftAiModel" class="flex-grow-1" label="Draft model" :items="modelOptions.draft" clearable />
                  <v-btn class="mt-2" variant="outlined" :loading="refreshing.draft" @click="refreshModelOptions('draft')">Refresh</v-btn>
                </div>
              </v-col>
              <v-col cols="12" md="6">
                <v-select v-model="form.writingAiProviderId" label="Writing center provider" :items="providerStore.providers" item-title="name" item-value="id" clearable />
              </v-col>
              <v-col cols="12" md="6">
                <div class="d-flex ga-2 align-start">
                  <v-combobox v-model="form.writingAiModel" class="flex-grow-1" label="Writing center model" :items="modelOptions.writing" clearable />
                  <v-btn class="mt-2" variant="outlined" :loading="refreshing.writing" @click="refreshModelOptions('writing')">Refresh</v-btn>
                </div>
              </v-col>
              <v-col cols="12">
                <v-alert type="info" variant="tonal">Recommended: draft on a smaller model, writing center on a stronger model.</v-alert>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </div>

      <div class="settings-grid">
        <v-card class="soft-panel">
          <v-card-title>Workflow</v-card-title>
          <v-card-text>
            <v-row>
              <v-col cols="12" md="6"><v-text-field v-model="form.workflowMaxPlanRounds" label="Plan rounds" type="number" /></v-col>
              <v-col cols="12" md="6"><v-text-field v-model="form.workflowMaxCheckRounds" label="Check rounds" type="number" /></v-col>
              <v-col cols="12" md="6"><v-text-field v-model="form.workflowMaxRevisionRounds" label="Revision rounds" type="number" /></v-col>
              <v-col cols="12" md="6"><v-text-field v-model="form.workflowMaxToolCalls" label="Tool calls" type="number" /></v-col>
            </v-row>
          </v-card-text>
        </v-card>

        <v-card class="soft-panel">
          <v-card-title>Chat Compression</v-card-title>
          <v-card-text>
            <v-row>
              <v-col cols="12" md="6"><v-text-field v-model="form.chatMaxActiveChars" label="Max active chars" type="number" /></v-col>
              <v-col cols="12" md="6"><v-text-field v-model="form.chatKeepRecentMessages" label="Keep recent messages" type="number" /></v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </div>

      <div class="settings-grid">
        <v-card class="soft-panel">
          <v-card-title>Login Security</v-card-title>
          <v-card-text>
            <v-row>
              <v-col cols="12" md="6"><v-text-field v-model="form.maxFailedAttempts" label="Max failed attempts" type="number" /></v-col>
              <v-col cols="12" md="6"><v-text-field v-model="form.lockMinutes" label="Lock minutes" type="number" /></v-col>
            </v-row>
          </v-card-text>
        </v-card>

        <v-card class="soft-panel">
          <v-card-title>Naming Model</v-card-title>
          <v-card-text>
            <v-row>
              <v-col cols="12">
                <v-select v-model="form.namingAiProviderId" label="Naming provider" :items="providerStore.providers" item-title="name" item-value="id" clearable />
              </v-col>
              <v-col cols="12">
                <div class="d-flex ga-2 align-start">
                  <v-combobox v-model="form.namingAiModel" class="flex-grow-1" label="Naming model" :items="modelOptions.naming" clearable />
                  <v-btn class="mt-2" variant="outlined" :loading="refreshing.naming" @click="refreshModelOptions('naming')">Refresh</v-btn>
                </div>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </div>

      <v-card class="soft-panel">
        <v-card-title>Prompt Templates</v-card-title>
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
