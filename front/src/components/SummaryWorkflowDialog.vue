<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import {
  applySummaryWorkflowProposal,
  createSummaryWorkflowChatTurn,
  createSummaryWorkflowProposal,
} from '@/api/summary-workflow'
import MarkdownContent from '@/components/MarkdownContent.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import type {
  SummaryWorkflowApplyResult,
  SummaryWorkflowChatMessage,
  SummaryWorkflowIntent,
  SummaryWorkflowOperatorMode,
  SummaryWorkflowPreview,
  SummaryWorkflowProposal,
  SummaryWorkflowTargetType,
} from '@/types'

const props = defineProps<{
  modelValue: boolean
  projectId: number | null
  targetType: SummaryWorkflowTargetType
  targetSourceId: number | null
  title: string
  targetLabel: string
  initialSummary: string
  createMode?: boolean
  initialOperatorMode?: SummaryWorkflowOperatorMode
  allowExpertFormSwitch?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  applied: [result: SummaryWorkflowApplyResult]
  'expert-edit-request': []
}>()

const summaryText = ref('')
const proposal = ref<SummaryWorkflowProposal | null>(null)
const preview = ref<SummaryWorkflowPreview | null>(null)
const operatorMode = ref<SummaryWorkflowOperatorMode>('DEFAULT')
const intent = ref<SummaryWorkflowIntent>('UPDATE')
const loading = ref(false)
const applying = ref(false)
const chatLoading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const chatDraft = ref('')
const chatMessages = ref<SummaryWorkflowChatMessage[]>([])
const pendingQuestions = ref<string[]>([])
const readyForPreview = ref(false)

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const normalizedInitialSummary = computed(() => (props.initialSummary || '').trim())
const normalizedSummaryText = computed(() => summaryText.value.trim())
const unchangedSummary = computed(() => normalizedSummaryText.value === normalizedInitialSummary.value)
const isChatMode = computed(() => operatorMode.value === 'DEFAULT')
const workflowTitle = computed(() => (props.createMode ? `摘要新增${props.targetLabel}` : '摘要工作流'))
const workflowSubtitle = computed(() =>
  props.createMode ? `新建${props.targetLabel}` : `${props.targetLabel}：${props.title}`,
)
const canTriggerExpertForm = computed(() => Boolean(props.allowExpertFormSwitch))
const submitButtonLabel = computed(() => {
  if (isChatMode.value) {
    return preview.value ? '重新看整理结果' : '看看整理结果'
  }
  return props.createMode ? '生成创建预览' : '生成结构预览'
})
const summaryEditorLabel = computed(() =>
  isChatMode.value ? `AI 整理出的${props.targetLabel}摘要` : '摘要输入',
)
const normalModeIntentLabel = computed(() => {
  if (props.createMode) {
    return `当前会按“创建${props.targetLabel}”处理`
  }
  if (normalizedInitialSummary.value) {
    return '当前会按“继续完善现有摘要”处理'
  }
  return '当前会按“生成新摘要”处理'
})
const normalModeHelperText = computed(() => {
  if (props.targetType === 'CHARACTER') {
    return '你只需要说人物印象、关系、气质或几个关键片段。'
  }
  if (props.targetType === 'WORLD_SETTING') {
    return '你只需要说这个设定是什么、会影响什么、为什么重要。'
  }
  return '你只需要说这章想写什么、谁出场、准备停在哪。'
})

const canSubmit = computed(
  () =>
    Boolean(props.projectId) &&
    (Boolean(props.targetSourceId) || Boolean(props.createMode)) &&
    Boolean(normalizedSummaryText.value) &&
    (props.createMode || !unchangedSummary.value) &&
    !loading.value &&
    !applying.value &&
    !chatLoading.value,
)

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) {
      return
    }
    resetDialogState()
  },
  { immediate: true },
)

watch(summaryText, () => {
  proposal.value = null
  preview.value = null
  errorMessage.value = ''
  successMessage.value = ''
})

watch(operatorMode, (mode) => {
  proposal.value = null
  preview.value = null
  errorMessage.value = ''
  successMessage.value = ''
  if (mode === 'DEFAULT') {
    intent.value = props.createMode ? 'CREATE' : normalizedInitialSummary.value ? 'REFINE' : 'UPDATE'
    seedChatMessages()
  }
})

function resetDialogState() {
  summaryText.value = props.initialSummary || ''
  proposal.value = null
  preview.value = null
  errorMessage.value = ''
  successMessage.value = ''
  operatorMode.value = props.initialOperatorMode || 'DEFAULT'
  intent.value = props.createMode ? 'CREATE' : normalizedInitialSummary.value ? 'REFINE' : 'UPDATE'
  chatDraft.value = ''
  readyForPreview.value = false
  pendingQuestions.value = defaultPendingQuestions()
  chatMessages.value = []
  if (operatorMode.value === 'DEFAULT') {
    seedChatMessages()
  }
}

function seedChatMessages() {
  if (chatMessages.value.length) {
    return
  }
  chatMessages.value = [
    {
      role: 'assistant',
      content: buildWelcomeMessage(),
    },
  ]
}

function buildWelcomeMessage() {
  const createPrefix = props.createMode ? '现在是新增模式。' : '现在是编辑模式。'
  const base = (() => {
    if (props.targetType === 'CHARACTER') {
      return '你先说这个人物的大概印象就行，比如他是谁、什么气质、和谁有关系。我会边问边帮你整理。'
    }
    if (props.targetType === 'WORLD_SETTING') {
      return '你先说这个设定的大概印象，比如它是什么、会影响谁、为什么重要。我会边问边帮你整理。'
    }
    return '你先说这一章你模糊想写什么，比如谁出场、要发生什么、准备停在哪。我会边问边帮你整理。'
  })()
  return `${createPrefix}${base}`
}

function defaultPendingQuestions() {
  if (props.targetType === 'CHARACTER') {
    return props.createMode
      ? ['这个人物和主角是什么关系？', '他现在最想得到什么？']
      : ['这次你最想改掉这个人物的哪一部分？', '他现在最清晰的目标是什么？']
  }
  if (props.targetType === 'WORLD_SETTING') {
    return ['这个设定到底是什么？', '它会直接影响谁或什么剧情？']
  }
  return ['这一章最少必须发生什么？', '这一章准备停在哪个点上？']
}

async function sendChatMessage() {
  if (!props.projectId || !chatDraft.value.trim()) {
    return
  }

  const content = chatDraft.value.trim()
  chatDraft.value = ''
  const nextMessages: SummaryWorkflowChatMessage[] = [
    ...chatMessages.value,
    {
      role: 'user',
      content,
    },
  ]
  chatMessages.value = nextMessages
  chatLoading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const result = await createSummaryWorkflowChatTurn({
      targetType: props.targetType,
      targetSourceId: props.targetSourceId || undefined,
      projectId: props.projectId,
      title: props.title,
      existingSummary: normalizedInitialSummary.value,
      currentDraftSummary: normalizedSummaryText.value,
      intent: intent.value,
      operatorMode: operatorMode.value,
      messages: nextMessages,
    })
    if (result.assistantMessage) {
      chatMessages.value = [
        ...nextMessages,
        {
          role: 'assistant',
          content: result.assistantMessage,
        },
      ]
    }
    if (result.draftSummary?.trim()) {
      summaryText.value = result.draftSummary.trim()
    }
    pendingQuestions.value = result.pendingQuestions || []
    readyForPreview.value = Boolean(result.readyForPreview)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '摘要对话失败'
  } finally {
    chatLoading.value = false
  }
}

function handleChatComposerKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendChatMessage().catch(() => undefined)
  }
}

async function generatePreview() {
  if (!props.projectId || (!props.targetSourceId && !props.createMode) || !normalizedSummaryText.value) {
    return
  }

  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const result = await createSummaryWorkflowProposal({
      targetType: props.targetType,
      targetSourceId: props.targetSourceId || undefined,
      projectId: props.projectId,
      summaryText: normalizedSummaryText.value,
      intent: intent.value,
      operatorMode: operatorMode.value,
    })
    proposal.value = result.proposal
    preview.value = result.preview
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '生成摘要预览失败'
  } finally {
    loading.value = false
  }
}

async function applyProposal() {
  if (!proposal.value) {
    return
  }

  applying.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const result = await applySummaryWorkflowProposal({
      proposalId: proposal.value.proposalId,
      confirmed: true,
    })
    successMessage.value = props.createMode ? '摘要创建已写回' : '摘要变更已写回'
    emit('applied', result)
    dialogVisible.value = false
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '写回摘要失败'
  } finally {
    applying.value = false
  }
}
</script>

<template>
  <v-dialog v-model="dialogVisible" max-width="1160">
    <v-card>
      <v-card-title>{{ workflowTitle }}</v-card-title>
      <v-card-subtitle>{{ workflowSubtitle }}</v-card-subtitle>
      <v-card-text class="pt-4">
        <div class="d-flex flex-wrap ga-2">
          <v-chip color="primary" variant="tonal">{{ isChatMode ? '1. 说想法' : '1. 写摘要' }}</v-chip>
          <v-chip color="primary" variant="tonal">{{ isChatMode ? '2. AI 整理' : '2. 看预览' }}</v-chip>
          <v-chip color="primary" variant="tonal">{{ isChatMode ? '3. 看变化' : '3. 确认写回' }}</v-chip>
          <v-chip v-if="isChatMode" color="primary" variant="tonal">4. 确认写回</v-chip>
        </div>

        <v-alert class="mt-4" type="info" variant="tonal">
          {{
            isChatMode
              ? `普通模式会先通过对话帮你把模糊想法整理成摘要草稿，再进入结构预览。${createMode ? `当前正在新增${targetLabel}。` : `当前正在修改${targetLabel}。`}`
              : `专家模式直接编辑摘要，并允许随时切到旧表单。${createMode ? `当前正在新增${targetLabel}。` : `当前正在修改${targetLabel}。`}`
          }}
        </v-alert>

        <div class="summary-workflow-options mt-4">
          <v-card variant="outlined">
            <v-card-text class="pt-4">
              <div class="text-caption text-medium-emphasis mb-2">操作模式</div>
              <div v-if="isChatMode" class="d-flex flex-wrap align-center ga-2">
                <v-chip color="primary" variant="tonal">普通模式</v-chip>
                <div class="text-caption text-medium-emphasis">
                  默认只需要说想法，AI 会帮你追问并整理摘要。
                </div>
                <v-btn variant="text" color="secondary" @click="operatorMode = 'EXPERT'">
                  切到专家模式
                </v-btn>
              </div>
              <template v-else>
                <v-segmented-button v-model="operatorMode" color="primary" mandatory>
                  <v-btn value="DEFAULT">普通模式</v-btn>
                  <v-btn value="EXPERT">专家模式</v-btn>
                </v-segmented-button>
                <div class="text-caption text-medium-emphasis mt-2">
                  专家模式直接编辑摘要，并可切回旧表单。
                </div>
              </template>
              <div v-if="canTriggerExpertForm" class="mt-3">
                <v-btn variant="text" color="secondary" @click="emit('expert-edit-request')">
                  切到专家表单
                </v-btn>
              </div>
            </v-card-text>
          </v-card>

          <v-card variant="outlined">
            <v-card-text class="pt-4">
              <div class="text-caption text-medium-emphasis mb-2">本次意图</div>
              <template v-if="isChatMode">
                <v-chip color="secondary" variant="tonal">{{ normalModeIntentLabel }}</v-chip>
                <div class="text-caption text-medium-emphasis mt-2">
                  普通模式不要求你先选结构意图，先把想法说清即可。
                </div>
              </template>
              <template v-else>
                <v-segmented-button v-model="intent" color="primary" mandatory>
                  <v-btn :value="createMode ? 'CREATE' : 'REFINE'">{{ createMode ? '创建对象' : '精修摘要' }}</v-btn>
                  <v-btn value="UPDATE">改写摘要</v-btn>
                  <v-btn value="ENRICH">补充细节</v-btn>
                </v-segmented-button>
                <div class="text-caption text-medium-emphasis mt-2">
                  当前仍以摘要与 SummaryFacet 为主，不会直接联动正文。
                </div>
              </template>
            </v-card-text>
          </v-card>
        </div>

        <div v-if="isChatMode" class="summary-chat-layout mt-4">
          <v-card variant="outlined">
            <v-card-title class="text-subtitle-1">对话采集</v-card-title>
            <v-card-text>
              <div class="summary-chat-timeline">
                <div
                  v-for="(message, index) in chatMessages"
                  :key="`${message.role}-${index}`"
                  :class="['summary-chat-message', `summary-chat-message--${message.role}`]"
                >
                  <div class="summary-chat-message__role">
                    {{ message.role === 'assistant' ? 'AI 助手' : '你' }}
                  </div>
                  <MarkdownContent :source="message.content" compact />
                </div>
              </div>

              <div v-if="pendingQuestions.length" class="mt-4">
                <div class="text-caption text-medium-emphasis mb-2">当前最缺的点</div>
                <div class="d-flex flex-wrap ga-2">
                  <v-chip
                    v-for="question in pendingQuestions"
                    :key="question"
                    size="small"
                    color="secondary"
                    variant="tonal"
                  >
                    {{ question }}
                  </v-chip>
                </div>
              </div>

              <v-textarea
                v-model="chatDraft"
                class="mt-4"
                rows="4"
                auto-grow
                hide-details
                label="继续说你的想法"
                placeholder="比如：她像个表面冷静、其实控制欲很强的导师；这一章我想先让主角意识到自己被盯上了。"
                :disabled="chatLoading"
                @keydown="handleChatComposerKeydown"
              />

              <div class="d-flex justify-space-between align-center mt-3">
                <div class="text-caption text-medium-emphasis">
                  {{ normalModeHelperText }}
                </div>
                <v-btn color="primary" :loading="chatLoading" :disabled="!chatDraft.trim()" @click="sendChatMessage">
                  让 AI 继续整理
                </v-btn>
              </div>
            </v-card-text>
          </v-card>

          <v-card variant="outlined">
            <v-card-title class="text-subtitle-1">摘要草稿</v-card-title>
            <v-card-text>
              <MarkdownEditor
                v-model="summaryText"
                :label="summaryEditorLabel"
                :rows="12"
                hint="普通模式下，这里会随着对话持续更新；你也可以直接微调。"
                persistent-hint
                preview-empty-text="AI 还没有整理出摘要草稿。"
              />
              <div class="d-flex justify-space-between align-center mt-3">
                <div class="text-caption text-medium-emphasis">
                  {{ readyForPreview ? '当前信息已足够生成结构预览。' : '信息还在补全中，但你也可以先生成一次结构预览。' }}
                </div>
              </div>
            </v-card-text>
          </v-card>
        </div>

        <div v-else class="mt-4">
          <MarkdownEditor
            v-model="summaryText"
            :label="summaryEditorLabel"
            :rows="8"
            hint="专家模式直接编辑摘要；如果还要深度改字段，可切到旧表单。"
            persistent-hint
            preview-empty-text="暂无摘要"
          />
        </div>

        <v-alert v-if="errorMessage" class="mt-4" type="error" variant="tonal">
          {{ errorMessage }}
        </v-alert>

        <v-alert
          v-else-if="normalizedSummaryText && unchangedSummary && !createMode"
          class="mt-4"
          type="warning"
          variant="tonal"
        >
          当前摘要与原摘要一致，系统不会生成新的结构提案。请先改动摘要内容。
        </v-alert>

        <v-alert v-if="successMessage" class="mt-4" type="success" variant="tonal">
          {{ successMessage }}
        </v-alert>

        <div v-if="preview" class="mt-6">
          <div class="text-subtitle-1 font-weight-medium">变化预览</div>
          <div class="summary-preview-grid mt-3">
            <v-card variant="outlined">
              <v-card-title class="text-subtitle-2">写回前</v-card-title>
              <v-card-text>
                <div class="text-body-2 font-weight-medium">
                  {{ preview.beforeSummary.displayTitle || title }}
                </div>
                <div class="text-caption text-medium-emphasis mt-2">
                  {{ preview.beforeSummary.oneLineSummary || '暂无一句话摘要' }}
                </div>
                <div class="mt-3">
                  <MarkdownContent
                    :source="preview.beforeSummary.longSummary"
                    empty-text="暂无详细摘要"
                    compact
                  />
                </div>
                <div v-if="preview.beforeSummary.stateSummary" class="text-caption text-medium-emphasis mt-3">
                  {{ preview.beforeSummary.stateSummary }}
                </div>
                <div v-if="preview.beforeSummary.relationSummary" class="text-caption text-medium-emphasis mt-1">
                  {{ preview.beforeSummary.relationSummary }}
                </div>
              </v-card-text>
            </v-card>

            <v-card variant="outlined">
              <v-card-title class="text-subtitle-2">写回后</v-card-title>
              <v-card-text>
                <div class="text-body-2 font-weight-medium">
                  {{ preview.afterSummary.displayTitle || title }}
                </div>
                <div class="text-caption text-medium-emphasis mt-2">
                  {{ preview.afterSummary.oneLineSummary || '暂无一句话摘要' }}
                </div>
                <div class="mt-3">
                  <MarkdownContent
                    :source="preview.afterSummary.longSummary"
                    empty-text="暂无详细摘要"
                    compact
                  />
                </div>
                <div v-if="preview.afterSummary.stateSummary" class="text-caption text-medium-emphasis mt-3">
                  {{ preview.afterSummary.stateSummary }}
                </div>
                <div v-if="preview.afterSummary.relationSummary" class="text-caption text-medium-emphasis mt-1">
                  {{ preview.afterSummary.relationSummary }}
                </div>
              </v-card-text>
            </v-card>
          </div>

          <v-card class="mt-4" variant="tonal">
            <v-card-text>
              <div class="text-subtitle-2">系统说明</div>
              <div class="text-body-2 mt-2">{{ preview.changeSummary }}</div>
              <div class="d-flex flex-wrap ga-2 mt-3">
                <v-chip
                  v-for="facet in preview.affectedFacets"
                  :key="facet"
                  size="small"
                  color="primary"
                  variant="outlined"
                >
                  {{ facet }}
                </v-chip>
              </div>
              <div v-if="preview.warnings.length" class="mt-3">
                <div class="text-caption text-medium-emphasis mb-2">风险提示</div>
                <div class="d-flex flex-wrap ga-2">
                  <v-chip
                    v-for="warning in preview.warnings"
                    :key="warning"
                    size="small"
                    color="warning"
                    variant="tonal"
                  >
                    {{ warning }}
                  </v-chip>
                </div>
              </div>
              <div v-if="proposal?.pendingQuestions?.length" class="mt-3">
                <div class="text-caption text-medium-emphasis mb-2">待补问题</div>
                <div class="d-flex flex-wrap ga-2">
                  <v-chip
                    v-for="question in proposal.pendingQuestions"
                    :key="question"
                    size="small"
                    color="secondary"
                    variant="tonal"
                  >
                    {{ question }}
                  </v-chip>
                </div>
              </div>
              <div
                v-if="proposal?.riskNotes?.length"
                class="mt-3 text-caption text-medium-emphasis"
              >
                {{ proposal.riskNotes.join('；') }}
              </div>
            </v-card-text>
          </v-card>
        </div>
      </v-card-text>
      <v-card-actions class="justify-end flex-wrap ga-2">
        <v-btn variant="text" @click="dialogVisible = false">关闭</v-btn>
        <v-btn color="primary" variant="outlined" :loading="loading" :disabled="!canSubmit" @click="generatePreview">
          {{ submitButtonLabel }}
        </v-btn>
        <v-btn color="primary" :loading="applying" :disabled="!proposal || !preview" @click="applyProposal">
          {{ createMode ? '确认创建' : '确认写回' }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.summary-workflow-options {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.summary-chat-layout,
.summary-preview-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.summary-chat-timeline {
  display: grid;
  gap: 12px;
  max-height: 380px;
  overflow: auto;
}

.summary-chat-message {
  padding: 12px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.03);
}

.summary-chat-message--assistant {
  border-left: 3px solid rgb(var(--v-theme-primary));
}

.summary-chat-message--user {
  border-left: 3px solid rgb(var(--v-theme-secondary));
}

.summary-chat-message__role {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.56);
}

@media (max-width: 960px) {
  .summary-workflow-options,
  .summary-chat-layout,
  .summary-preview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
