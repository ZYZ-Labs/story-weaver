<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import { getDirectorDecision } from '@/api/ai-director'
import type {
  AIDirectorDecision,
  AIDirectorDecisionPack,
  AIDirectorSelectedModule,
  AIDirectorToolTrace,
} from '@/types'
import { formatDateTime } from '@/utils/format'

const props = withDefaults(
  defineProps<{
    decisionId?: number | null
    loading?: boolean
    title?: string
    emptyText?: string
  }>(),
  {
    decisionId: null,
    loading: false,
    title: '总导决策摘要',
    emptyText: '当前还没有可展示的总导决策。发起一次生成后，这里会显示阶段、模块、约束和工具调用摘要。',
  },
)

const decision = ref<AIDirectorDecision | null>(null)
const loadingDecision = ref(false)
const errorMessage = ref('')
let activeFetchId = 0

const decisionPack = computed<AIDirectorDecisionPack | null>(() => {
  const pack = decision.value?.decisionPack
  return pack && typeof pack === 'object' ? pack : null
})

const selectedModules = computed<AIDirectorSelectedModule[]>(() => {
  const items = decisionPack.value?.selectedModules
  if (!Array.isArray(items)) {
    return []
  }
  return items.filter(
    (item): item is AIDirectorSelectedModule =>
      !!item && typeof item === 'object' && typeof item.module === 'string' && item.module.trim().length > 0,
  )
})

const requiredFacts = computed(() => readStringArray(decisionPack.value?.requiredFacts))
const prohibitedMoves = computed(() => readStringArray(decisionPack.value?.prohibitedMoves))
const writerHints = computed(() => readStringArray(decisionPack.value?.writerHints))
const toolTrace = computed<AIDirectorToolTrace[]>(() => {
  const items = decision.value?.toolTrace
  if (!Array.isArray(items)) {
    return []
  }
  return items.filter((item): item is AIDirectorToolTrace => !!item && typeof item === 'object')
})
const toolNames = computed(() => {
  const values = new Set<string>()
  for (const item of toolTrace.value) {
    const name = item.name?.trim()
    if (name) {
      values.add(name)
    }
  }
  return Array.from(values)
})
const visibleLoading = computed(() => loadingDecision.value || (props.loading && !decision.value))

watch(
  () => props.decisionId,
  async (decisionId) => {
    const fetchId = ++activeFetchId
    errorMessage.value = ''

    if (!decisionId) {
      decision.value = null
      loadingDecision.value = false
      return
    }

    decision.value = null
    loadingDecision.value = true
    try {
      const latest = await getDirectorDecision(decisionId)
      if (fetchId !== activeFetchId) {
        return
      }
      decision.value = latest
    } catch (error) {
      if (fetchId !== activeFetchId) {
        return
      }
      decision.value = null
      errorMessage.value = error instanceof Error ? error.message : '读取总导决策失败'
    } finally {
      if (fetchId === activeFetchId) {
        loadingDecision.value = false
      }
    }
  },
  { immediate: true },
)

function readStringArray(value: unknown) {
  if (!Array.isArray(value)) {
    return []
  }
  return value
    .filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
    .map((item) => item.trim())
}

function formatStage(value?: string | null) {
  const mapping: Record<string, string> = {
    opening: '开场',
    setup: '铺垫',
    advancement: '推进',
    turning: '转折',
    convergence: '收束',
    polish: '润色',
  }
  return mapping[value || ''] || value || '未标记'
}

function formatWritingMode(value?: string | null) {
  const mapping: Record<string, string> = {
    draft: '初稿',
    continue: '续写',
    expand: '扩写',
    rewrite: '重写',
    polish: '润色',
  }
  return mapping[value || ''] || value || '未标记'
}

function formatStatus(value?: string | null) {
  const mapping: Record<string, string> = {
    generated: '已生成',
    applied: '已应用',
    fallback: '回退',
    failed: '失败',
  }
  return mapping[value || ''] || value || '未知'
}

function formatModuleName(value: string) {
  const mapping: Record<string, string> = {
    chapter_snapshot: '章节快照',
    outline: '章节大纲',
    plot: '剧情节点',
    causality: '因果链',
    world_setting: '世界观',
    required_characters: '必出人物',
    character_inventory: '人物背包',
    knowledge: '知识片段',
    chat_background: '背景聊天',
  }
  return mapping[value] || value
}

function formatModuleChip(module: AIDirectorSelectedModule) {
  const details: string[] = []
  if (module.required) {
    details.push('必选')
  }
  if (typeof module.topK === 'number' && module.topK > 0) {
    details.push(`Top ${module.topK}`)
  }
  if (typeof module.weight === 'number') {
    details.push(`权重 ${module.weight.toFixed(2)}`)
  }
  return details.length
    ? `${formatModuleName(module.module)} · ${details.join(' / ')}`
    : formatModuleName(module.module)
}

function formatTraceBody(value?: string) {
  const normalized = (value || '').trim()
  if (!normalized) {
    return '暂无'
  }
  return normalized.length > 500 ? `${normalized.slice(0, 500)}...` : normalized
}
</script>

<template>
  <v-card class="soft-panel">
    <v-card-title>{{ title }}</v-card-title>
    <v-card-text>
      <v-progress-linear v-if="visibleLoading" indeterminate color="primary" class="mb-4" />

      <v-alert v-if="errorMessage" type="warning" variant="tonal" class="mb-4">
        {{ errorMessage }}
      </v-alert>

      <template v-if="decision">
        <div class="d-flex flex-wrap ga-2">
          <v-chip size="small" color="primary" variant="tonal">
            阶段：{{ formatStage(decision.stage) }}
          </v-chip>
          <v-chip size="small" color="secondary" variant="tonal">
            模式：{{ formatWritingMode(decision.writingMode) }}
          </v-chip>
          <v-chip size="small" variant="tonal">
            状态：{{ formatStatus(decision.status) }}
          </v-chip>
          <v-chip v-if="decision.targetWordCount" size="small" variant="tonal">
            目标字数：{{ decision.targetWordCount }}
          </v-chip>
        </div>

        <div class="text-body-2 mt-4">
          {{ decision.decisionSummary || decisionPack?.decisionSummary || '本轮总导没有返回额外摘要。' }}
        </div>

        <div class="text-caption text-medium-emphasis mt-3">
          {{ formatDateTime(decision.createTime) }} | 模型 {{ decision.selectedModel || '自动选择' }}
        </div>

        <div v-if="selectedModules.length" class="mt-4">
          <div class="text-subtitle-2 font-weight-medium mb-2">已选模块</div>
          <div class="d-flex flex-wrap ga-2">
            <v-chip
              v-for="module in selectedModules"
              :key="`${module.module}-${module.topK || 0}`"
              size="small"
              color="primary"
              variant="outlined"
            >
              {{ formatModuleChip(module) }}
            </v-chip>
          </div>
        </div>

        <div
          v-if="requiredFacts.length || prohibitedMoves.length || writerHints.length"
          class="director-grid mt-4"
        >
          <div v-if="requiredFacts.length" class="director-section">
            <div class="text-subtitle-2 font-weight-medium mb-2">硬约束</div>
            <div v-for="item in requiredFacts" :key="item" class="text-body-2 director-line">
              {{ item }}
            </div>
          </div>

          <div v-if="prohibitedMoves.length" class="director-section">
            <div class="text-subtitle-2 font-weight-medium mb-2">禁止事项</div>
            <div v-for="item in prohibitedMoves" :key="item" class="text-body-2 director-line">
              {{ item }}
            </div>
          </div>

          <div v-if="writerHints.length" class="director-section">
            <div class="text-subtitle-2 font-weight-medium mb-2">写作提示</div>
            <div v-for="item in writerHints" :key="item" class="text-body-2 director-line">
              {{ item }}
            </div>
          </div>
        </div>

        <v-alert
          v-if="decision.status === 'fallback' || decision.errorMessage"
          type="warning"
          variant="tonal"
          class="mt-4"
        >
          {{
            decision.status === 'fallback'
              ? '本轮总导回退到了启发式决策。'
              : decision.errorMessage
          }}
        </v-alert>

        <v-expansion-panels v-if="toolTrace.length" variant="accordion" class="mt-4">
          <v-expansion-panel>
            <v-expansion-panel-title>
              调试日志
              <span class="text-caption text-medium-emphasis ml-2">
                {{ toolTrace.length }} 次工具调用
                <template v-if="toolNames.length">· {{ toolNames.join('、') }}</template>
              </span>
            </v-expansion-panel-title>
            <v-expansion-panel-text>
              <div
                v-for="(trace, index) in toolTrace"
                :key="trace.id || `${trace.name || 'trace'}-${index}`"
                class="trace-item"
              >
                <div class="text-subtitle-2 font-weight-medium">
                  {{ trace.name || '未命名工具' }}
                </div>
                <div class="text-caption text-medium-emphasis mt-1">参数</div>
                <pre class="trace-block">{{ formatTraceBody(trace.argumentsJson) }}</pre>
                <div class="text-caption text-medium-emphasis mt-2">结果</div>
                <pre class="trace-block">{{ formatTraceBody(trace.resultJson) }}</pre>
              </div>
            </v-expansion-panel-text>
          </v-expansion-panel>
        </v-expansion-panels>
      </template>

      <div v-else class="text-medium-emphasis">
        {{ visibleLoading ? '正在读取总导决策...' : emptyText }}
      </div>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.director-grid {
  display: grid;
  gap: 12px;
}

.director-section {
  padding: 12px;
  border-radius: 12px;
  background: rgba(var(--v-theme-surface-variant), 0.28);
}

.director-line + .director-line {
  margin-top: 8px;
}

.trace-item + .trace-item {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(var(--v-theme-on-surface), 0.08);
}

.trace-block {
  margin: 6px 0 0;
  padding: 12px;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-word;
  border-radius: 12px;
  background: rgba(var(--v-theme-surface-variant), 0.24);
  font-size: 12px;
  line-height: 1.5;
}
</style>
