<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import MarkdownContent from '@/components/MarkdownContent.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import PageContainer from '@/components/PageContainer.vue'
import { useCausalityStore } from '@/stores/causality'
import { useChapterStore } from '@/stores/chapter'
import { useCharacterStore } from '@/stores/character'
import { usePlotStore } from '@/stores/plot'
import { useProjectStore } from '@/stores/project'
import { useRagStore } from '@/stores/rag'
import { useWritingStore } from '@/stores/writing'

type EntityOption = {
  value: string
  title: string
  summary: string
  meta?: string
}

const projectStore = useProjectStore()
const chapterStore = useChapterStore()
const characterStore = useCharacterStore()
const plotStore = usePlotStore()
const ragStore = useRagStore()
const writingStore = useWritingStore()
const causalityStore = useCausalityStore()

const projectId = computed(() => projectStore.selectedProjectId)
const dialog = ref(false)
const editingId = ref<number | null>(null)
const deletingId = ref<number | null>(null)
const confirmVisible = ref(false)
const lastAutoName = ref('')
const lastAutoDescription = ref('')

const entityTypeOptions = [
  { title: '章节', value: 'chapter' },
  { title: '剧情事件', value: 'plot' },
  { title: '人物', value: 'character' },
  { title: '知识条目', value: 'knowledge' },
  { title: 'AI 草稿 / 任务', value: 'writing' },
  { title: '手动输入', value: 'manual' },
]

const nodeTypeOptions = [
  { title: '事件', value: 'event' },
  { title: '冲突', value: 'conflict' },
  { title: '目标', value: 'goal' },
  { title: '线索', value: 'clue' },
  { title: '转折', value: 'turning_point' },
  { title: '结果', value: 'result' },
]

const relationshipOptions = [
  { title: '触发', value: 'causes' },
  { title: '推动', value: 'escalates' },
  { title: '揭示', value: 'reveals' },
  { title: '阻碍', value: 'blocks' },
  { title: '解决', value: 'resolves' },
  { title: '驱动', value: 'motivates' },
]

const statusOptions = [
  { title: '规划中', value: 0 },
  { title: '生效中', value: 1 },
  { title: '已完成', value: 2 },
  { title: '已废弃', value: 3 },
]

const entityTypeToNodeType: Record<string, string> = {
  chapter: 'event',
  plot: 'conflict',
  character: 'goal',
  knowledge: 'clue',
  writing: 'turning_point',
  manual: 'event',
}

const form = reactive({
  name: '',
  description: '',
  causeType: 'event',
  effectType: 'result',
  causeEntityId: '',
  effectEntityId: '',
  causeEntityType: 'chapter',
  effectEntityType: 'plot',
  relationship: 'causes',
  strength: 60,
  conditions: '',
  tags: [] as string[],
  status: 1,
})

const causeEntityOptions = computed(() => getEntityOptions(form.causeEntityType))
const effectEntityOptions = computed(() => getEntityOptions(form.effectEntityType))
const causeEntityMeta = computed(() => getEntityMeta(form.causeEntityType, form.causeEntityId))
const effectEntityMeta = computed(() => getEntityMeta(form.effectEntityType, form.effectEntityId))

watch(
  projectId,
  async (id) => {
    if (!id) return
    await Promise.allSettled([
      causalityStore.fetchByProject(id),
      chapterStore.fetchByProject(id),
      characterStore.fetchByProject(id),
      plotStore.fetchByProject(id),
      ragStore.fetchByProject(id),
      writingStore.fetchByProject(id),
    ])
  },
  { immediate: true },
)

watch(
  [
    () => form.causeEntityType,
    () => form.causeEntityId,
    () => form.effectEntityType,
    () => form.effectEntityId,
    () => form.relationship,
  ],
  () => {
    syncAutoFields()
  },
)

watch(
  () => form.causeEntityType,
  (type) => {
    if (type !== 'manual' && !getEntityOptions(type).some((item) => item.value === form.causeEntityId)) {
      form.causeEntityId = ''
    }
    form.causeType = entityTypeToNodeType[type] || 'event'
  },
)

watch(
  () => form.effectEntityType,
  (type) => {
    if (type !== 'manual' && !getEntityOptions(type).some((item) => item.value === form.effectEntityId)) {
      form.effectEntityId = ''
    }
    form.effectType = entityTypeToNodeType[type] || 'result'
  },
)

onMounted(() => {
  if (!projectStore.projects.length) {
    projectStore.fetchProjects().catch(() => undefined)
  }
})

function splitCsv(value?: string) {
  if (!value) return []
  return value
    .split(/[,，]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function formatStoredEntityId(type: string, rawId: string) {
  if (!rawId) return ''
  return type === 'manual' ? rawId : `${type}-${rawId}`
}

function parseStoredEntityId(type: string, stored?: string) {
  if (!stored) return ''
  if (type === 'manual') return stored
  const hyphenPrefix = `${type}-`
  const colonPrefix = `${type}:`
  if (stored.startsWith(hyphenPrefix)) {
    return stored.slice(hyphenPrefix.length)
  }
  if (stored.startsWith(colonPrefix)) {
    return stored.slice(colonPrefix.length)
  }
  const parts = stored.split(/[-:]/)
  return parts.length > 1 ? parts.slice(1).join('-') : stored
}

function getWritingTypeLabel(value?: string) {
  const mapping: Record<string, string> = {
    continue: '续写',
    expand: '扩写',
    rewrite: '改写',
    polish: '润色',
    draft: '草稿',
  }
  return mapping[value || ''] || value || '草稿'
}

function getWritingStatusLabel(value?: string) {
  const mapping: Record<string, string> = {
    draft: '草稿',
    accepted: '已采纳',
    rejected: '已拒绝',
  }
  return mapping[value || ''] || value || '草稿'
}

function getEntityOptions(type: string): EntityOption[] {
  switch (type) {
    case 'chapter':
      return chapterStore.chapters.map((item) => ({
        value: String(item.id),
        title: item.title,
        summary: item.content || '',
        meta: item.orderNum ? `第 ${item.orderNum} 章` : '章节',
      }))
    case 'plot':
      return plotStore.plotlines.map((item) => ({
        value: String(item.id),
        title: item.title || '未命名剧情',
        summary: item.description || item.content || '',
        meta: item.timeline || '剧情事件',
      }))
    case 'character':
      return characterStore.characters.map((item) => ({
        value: String(item.id),
        title: item.name,
        summary: item.description || '',
        meta: item.projectRole || '人物',
      }))
    case 'knowledge':
      return ragStore.documents.map((item) => ({
        value: String(item.id),
        title: item.title,
        summary: item.summary || item.contentText || '',
        meta: item.status || '知识条目',
      }))
    case 'writing':
      return writingStore.projectRecords.map((item) => ({
        value: String(item.id),
        title: `${getWritingTypeLabel(item.writingType)} · ${item.selectedModel || '未指定模型'}`,
        summary: item.generatedContent || item.userInstruction || '',
        meta: `章节 #${item.chapterId} / ${getWritingStatusLabel(item.status)}`,
      }))
    default:
      return []
  }
}

function getEntityMeta(type: string, entityId: string) {
  return getEntityOptions(type).find((item) => item.value === entityId) || null
}

function getRelationshipLabel(value: string) {
  return relationshipOptions.find((item) => item.value === value)?.title || value
}

function getStatusLabel(value?: number) {
  return statusOptions.find((item) => item.value === value)?.title || '未知'
}

function getDisplayEntity(type: string, storedId?: string, fallback = '未关联') {
  const entityId = parseStoredEntityId(type, storedId)
  if (!entityId) return fallback
  const meta = getEntityMeta(type, entityId)
  return meta?.title || storedId || fallback
}

function syncAutoFields() {
  const causeMeta = getEntityMeta(form.causeEntityType, form.causeEntityId)
  const effectMeta = getEntityMeta(form.effectEntityType, form.effectEntityId)
  if (!causeMeta && !effectMeta) return

  const relationshipLabel = getRelationshipLabel(form.relationship)
  const autoName = causeMeta && effectMeta
    ? `${causeMeta.title}${relationshipLabel}${effectMeta.title}`
    : causeMeta
      ? `因果：${causeMeta.title}`
      : effectMeta
        ? `因果：${effectMeta.title}`
        : ''

  const summaryParts = [
    causeMeta?.summary ? `原因摘要：${causeMeta.summary.slice(0, 80)}` : '',
    effectMeta?.summary ? `结果摘要：${effectMeta.summary.slice(0, 80)}` : '',
  ].filter(Boolean)

  const autoDescription = causeMeta && effectMeta
    ? `${causeMeta.title}${relationshipLabel}${effectMeta.title}。${summaryParts.join(' ')}`
    : causeMeta?.summary || effectMeta?.summary || ''

  if (autoName && (!form.name || form.name === lastAutoName.value)) {
    form.name = autoName
    lastAutoName.value = autoName
  }

  if (autoDescription && (!form.description || form.description === lastAutoDescription.value)) {
    form.description = autoDescription
    lastAutoDescription.value = autoDescription
  }
}

function resetForm() {
  Object.assign(form, {
    name: '',
    description: '',
    causeType: 'event',
    effectType: 'result',
    causeEntityId: '',
    effectEntityId: '',
    causeEntityType: 'chapter',
    effectEntityType: 'plot',
    relationship: 'causes',
    strength: 60,
    conditions: '',
    tags: [],
    status: 1,
  })
  lastAutoName.value = ''
  lastAutoDescription.value = ''
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialog.value = true
}

function openEdit(id: number) {
  const target = causalityStore.nodes.find((item) => item.id === id)
  if (!target) return

  editingId.value = id
  Object.assign(form, {
    name: target.name || '',
    description: target.description || '',
    causeType: target.causeType || 'event',
    effectType: target.effectType || 'result',
    causeEntityId: parseStoredEntityId(target.causeEntityType || 'manual', target.causeEntityId),
    effectEntityId: parseStoredEntityId(target.effectEntityType || 'manual', target.effectEntityId),
    causeEntityType: target.causeEntityType || 'manual',
    effectEntityType: target.effectEntityType || 'manual',
    relationship: target.relationship || 'causes',
    strength: target.strength ?? 60,
    conditions: target.conditions || '',
    tags: splitCsv(target.tags),
    status: target.status ?? 1,
  })
  lastAutoName.value = form.name
  lastAutoDescription.value = form.description
  dialog.value = true
}

function requestDelete(id: number) {
  deletingId.value = id
  confirmVisible.value = true
}

async function submit() {
  if (!projectId.value) return

  const payload = {
    ...form,
    causeEntityId: formatStoredEntityId(form.causeEntityType, form.causeEntityId),
    effectEntityId: formatStoredEntityId(form.effectEntityType, form.effectEntityId),
    tags: form.tags.join(','),
  }

  if (editingId.value) {
    await causalityStore.update(editingId.value, payload)
  } else {
    await causalityStore.create(projectId.value, payload)
  }
  dialog.value = false
}

async function confirmDelete() {
  if (!deletingId.value) return
  await causalityStore.remove(deletingId.value)
  confirmVisible.value = false
}
</script>

<template>
  <PageContainer
    title="因果管理"
    description="因果关系支持 Markdown 描述与条件编写，并可直接关联章节、剧情、人物、知识条目和 AI 草稿。"
  >
    <template #actions>
      <v-btn color="primary" prepend-icon="mdi-plus" :disabled="!projectId" @click="openCreate">
        新增因果
      </v-btn>
    </template>

    <EmptyState
      v-if="!projectId"
      title="先选择项目"
      description="因果关系会基于当前项目的章节、剧情、人物和知识数据建立关联。"
    />

    <EmptyState
      v-else-if="!causalityStore.nodes.length"
      title="还没有因果关系"
      description="可以先从章节、剧情事件或 AI 草稿中选一对原因和结果，快速建立因果链。"
    />

    <div v-else class="content-grid two-column">
      <v-card class="soft-panel">
        <v-card-title>因果关系列表</v-card-title>
        <v-table>
          <thead>
            <tr>
              <th>名称</th>
              <th>原因</th>
              <th>结果</th>
              <th>关系</th>
              <th>强度</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="node in causalityStore.nodes" :key="node.id">
              <td>{{ node.name || '未命名因果' }}</td>
              <td>{{ getDisplayEntity(node.causeEntityType || 'manual', node.causeEntityId) }}</td>
              <td>{{ getDisplayEntity(node.effectEntityType || 'manual', node.effectEntityId) }}</td>
              <td>{{ getRelationshipLabel(node.relationship || 'causes') }}</td>
              <td>{{ node.strength || 0 }}</td>
              <td>{{ getStatusLabel(node.status) }}</td>
              <td>
                <div class="d-flex ga-2">
                  <v-btn size="small" variant="text" @click="openEdit(node.id)">编辑</v-btn>
                  <v-btn size="small" color="error" variant="text" @click="requestDelete(node.id)">删除</v-btn>
                </div>
              </td>
            </tr>
          </tbody>
        </v-table>
      </v-card>

      <v-card class="soft-panel">
        <v-card-title>最近关系摘要</v-card-title>
        <v-list lines="three">
          <v-list-item
            v-for="node in causalityStore.nodes.slice(0, 8)"
            :key="node.id"
            :title="node.name || '未命名因果'"
          >
            <template #subtitle>
              <MarkdownContent compact :source="node.description" empty-text="暂无描述" />
            </template>
            <template #append>
              <v-chip size="small" variant="tonal">{{ getRelationshipLabel(node.relationship || 'causes') }}</v-chip>
            </template>
          </v-list-item>
        </v-list>
      </v-card>
    </div>

    <v-dialog v-model="dialog" max-width="1040">
      <v-card>
        <v-card-title>{{ editingId ? '编辑因果关系' : '新增因果关系' }}</v-card-title>
        <v-card-text class="pt-4">
          <v-row>
            <v-col cols="12" md="8">
              <v-text-field v-model="form.name" label="关系名称" />
            </v-col>
            <v-col cols="12" md="4">
              <v-select
                v-model="form.relationship"
                label="关系类型"
                :items="relationshipOptions"
                item-title="title"
                item-value="value"
              />
            </v-col>

            <v-col cols="12">
              <MarkdownEditor
                v-model="form.description"
                label="描述"
                :rows="4"
                auto-grow
                preview-empty-text="暂无描述"
              />
            </v-col>

            <v-col cols="12" md="6">
              <v-card variant="outlined" class="h-100">
                <v-card-title class="text-subtitle-1">原因端</v-card-title>
                <v-card-text>
                  <v-row>
                    <v-col cols="12" md="6">
                      <v-select
                        v-model="form.causeEntityType"
                        label="实体类型"
                        :items="entityTypeOptions"
                        item-title="title"
                        item-value="value"
                      />
                    </v-col>
                    <v-col cols="12" md="6">
                      <v-select
                        v-model="form.causeType"
                        label="原因类型"
                        :items="nodeTypeOptions"
                        item-title="title"
                        item-value="value"
                      />
                    </v-col>
                    <v-col cols="12">
                      <v-autocomplete
                        v-if="form.causeEntityType !== 'manual'"
                        v-model="form.causeEntityId"
                        label="关联对象"
                        :items="causeEntityOptions"
                        item-title="title"
                        item-value="value"
                        clearable
                      />
                      <v-text-field
                        v-else
                        v-model="form.causeEntityId"
                        label="手动输入对象标识"
                        placeholder="例如：决战导火索 或 自定义节点"
                      />
                    </v-col>
                    <v-col v-if="causeEntityMeta" cols="12">
                      <v-alert type="info" variant="tonal">
                        <div class="font-weight-medium">{{ causeEntityMeta.title }}</div>
                        <div class="text-caption text-medium-emphasis mt-1">
                          {{ causeEntityMeta.meta || '已选择' }}
                        </div>
                        <div class="mt-2">
                          <MarkdownContent compact :source="causeEntityMeta.summary" empty-text="暂无摘要" />
                        </div>
                      </v-alert>
                    </v-col>
                  </v-row>
                </v-card-text>
              </v-card>
            </v-col>

            <v-col cols="12" md="6">
              <v-card variant="outlined" class="h-100">
                <v-card-title class="text-subtitle-1">结果端</v-card-title>
                <v-card-text>
                  <v-row>
                    <v-col cols="12" md="6">
                      <v-select
                        v-model="form.effectEntityType"
                        label="实体类型"
                        :items="entityTypeOptions"
                        item-title="title"
                        item-value="value"
                      />
                    </v-col>
                    <v-col cols="12" md="6">
                      <v-select
                        v-model="form.effectType"
                        label="结果类型"
                        :items="nodeTypeOptions"
                        item-title="title"
                        item-value="value"
                      />
                    </v-col>
                    <v-col cols="12">
                      <v-autocomplete
                        v-if="form.effectEntityType !== 'manual'"
                        v-model="form.effectEntityId"
                        label="关联对象"
                        :items="effectEntityOptions"
                        item-title="title"
                        item-value="value"
                        clearable
                      />
                      <v-text-field
                        v-else
                        v-model="form.effectEntityId"
                        label="手动输入对象标识"
                        placeholder="例如：决战结果 或 自定义目标"
                      />
                    </v-col>
                    <v-col v-if="effectEntityMeta" cols="12">
                      <v-alert type="success" variant="tonal">
                        <div class="font-weight-medium">{{ effectEntityMeta.title }}</div>
                        <div class="text-caption text-medium-emphasis mt-1">
                          {{ effectEntityMeta.meta || '已选择' }}
                        </div>
                        <div class="mt-2">
                          <MarkdownContent compact :source="effectEntityMeta.summary" empty-text="暂无摘要" />
                        </div>
                      </v-alert>
                    </v-col>
                  </v-row>
                </v-card-text>
              </v-card>
            </v-col>

            <v-col cols="12" md="6">
              <div class="text-subtitle-2 mb-2">影响强度</div>
              <v-slider v-model="form.strength" color="primary" thumb-label min="0" max="100" step="1" />
            </v-col>
            <v-col cols="12" md="6">
              <v-select
                v-model="form.status"
                label="状态"
                :items="statusOptions"
                item-title="title"
                item-value="value"
              />
            </v-col>
            <v-col cols="12" md="6">
              <v-combobox v-model="form.tags" label="标签" multiple chips closable-chips />
            </v-col>
            <v-col cols="12">
              <MarkdownEditor
                v-model="form.conditions"
                label="触发条件"
                :rows="4"
                auto-grow
                preview-empty-text="暂无触发条件"
              />
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
      title="删除因果关系"
      text="确认删除这条因果关系吗？"
      @confirm="confirmDelete"
    />
  </PageContainer>
</template>
