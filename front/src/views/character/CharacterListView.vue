<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import { generateCharacterAttributes } from '@/api/character'
import { generateNameSuggestions } from '@/api/name-suggestion'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import CharacterInventoryDialog from '@/components/CharacterInventoryDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import MarkdownContent from '@/components/MarkdownContent.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import NameSuggestionDialog from '@/components/NameSuggestionDialog.vue'
import PageContainer from '@/components/PageContainer.vue'
import SummaryWorkflowDialog from '@/components/SummaryWorkflowDialog.vue'
import { useCharacterStore } from '@/stores/character'
import { useChapterStore } from '@/stores/chapter'
import { useProjectStore } from '@/stores/project'
import type {
  Character,
  CharacterAttributeSuggestionResult,
  SummaryWorkflowApplyResult,
  SummaryWorkflowOperatorMode,
} from '@/types'
import {
  buildCharacterAttributesJson,
  cloneCharacterAttributeForm,
  createEmptyCharacterAttributeForm,
  parseCharacterAttributes,
} from '@/utils/character-attributes'

const projectStore = useProjectStore()
const characterStore = useCharacterStore()
const chapterStore = useChapterStore()

const dialog = ref(false)
const inventoryDialog = ref(false)
const inventoryCharacter = ref<Character | null>(null)
const deletingId = ref<number | null>(null)
const confirmVisible = ref(false)
const editingId = ref<number | null>(null)
const submitLoading = ref(false)

const nameSuggestionDialog = ref(false)
const nameSuggestionLoading = ref(false)
const nameSuggestions = ref<string[]>([])
const nameSuggestionSourceLabel = ref('')

const attributeSuggestionLoading = ref(false)
const attributeSuggestionSourceLabel = ref('')
const summaryWorkflowVisible = ref(false)
const summaryWorkflowTarget = ref<Character | null>(null)
const summaryWorkflowCreateMode = ref(false)
const editorMode = ref<SummaryWorkflowOperatorMode>('DEFAULT')
const characterCardTabs = reactive<Record<number, string>>({})

const currentProjectId = computed(() => projectStore.selectedProjectId)
const currentProject = computed(() =>
  projectStore.projects.find((item) => item.id === currentProjectId.value) || null,
)
const summaryFirstMode = computed(() => editorMode.value === 'DEFAULT')
const createButtonLabel = computed(() => (summaryFirstMode.value ? '说想法新增人物' : '摘要新增人物'))
const emptyCreateButtonLabel = computed(() => (summaryFirstMode.value ? '说想法创建人物' : '摘要创建人物'))

const projectRoleOptions = ['主角', '群像主角', '配角', '反派', '导师', '重要 NPC', '线索人物', '客串']
const campOptions = ['主角阵营', '伙伴阵营', '中立阵营', '反派阵营', '组织成员', '家族势力']
const traitOptions = ['冷静', '冲动', '理性', '敏感', '果断', '顽强', '温柔', '多疑', '幽默', '骄傲']
const talentOptions = ['战斗天赋', '感知天赋', '谋略天赋', '领导天赋', '潜行天赋', '学习天赋', '交涉天赋']
const skillOptions = ['剑术', '格斗', '射击', '黑客', '谈判', '侦查', '治疗', '魔法', '炼金', '驾驶']
const weaknessOptions = ['怕火', '体力差', '优柔寡断', '情绪失控', '过度自信', '信任过头', '信息闭塞']
const tagOptions = ['成长型', '智谋型', '热血型', '悲剧型', '反差感', '宿命感', '神秘感']
const relationHintOptions = ['导师', '宿敌', '青梅竹马', '队友', '上级', '旧识', '家人', '恩人']

type CharacterArrayField =
  | 'traits'
  | 'talents'
  | 'skills'
  | 'weaknesses'
  | 'equipment'
  | 'tags'
  | 'relations'

const characterTemplates = [
  {
    name: '成长主角',
    description: '适合从弱到强、目标明确、带成长弧线的主角。',
    values: {
      camp: '主角阵营',
      goal: '完成一件必须亲手做到的大事',
      traits: ['顽强', '敏感'],
      talents: ['学习天赋'],
      skills: ['侦查'],
      weaknesses: ['体力差'],
      tags: ['成长型', '热血型'],
    },
  },
  {
    name: '冷面军师',
    description: '适合谋略型角色、参谋、策士或组织二把手。',
    values: {
      camp: '伙伴阵营',
      identity: '军师 / 参谋',
      traits: ['冷静', '理性'],
      talents: ['谋略天赋'],
      skills: ['谈判', '侦查'],
      weaknesses: ['多疑'],
      tags: ['智谋型'],
    },
  },
  {
    name: '天才学者',
    description: '适合研究者、学院派、炼金师、工程师。',
    values: {
      identity: '学者 / 研究者',
      traits: ['理性', '敏感'],
      talents: ['学习天赋', '感知天赋'],
      skills: ['炼金', '治疗'],
      weaknesses: ['体力差'],
      tags: ['神秘感'],
    },
  },
  {
    name: '危险反派',
    description: '适合有压迫感、控制欲或宿命感的关键反派。',
    values: {
      camp: '反派阵营',
      goal: '掌控局势并迫使主角做出选择',
      traits: ['果断', '骄傲', '多疑'],
      talents: ['领导天赋', '谋略天赋'],
      skills: ['谈判', '格斗'],
      weaknesses: ['过度自信'],
      tags: ['宿命感', '反差感'],
    },
  },
]

const form = reactive({
  mode: 'create' as 'create' | 'attach',
  existingCharacterId: null as number | null,
  name: '',
  description: '',
  projectRole: '配角',
  growthArc: '',
  activeStage: '',
  firstAppearanceChapterId: null as number | null,
  isRetired: false,
})

const attributeForm = reactive(createEmptyCharacterAttributeForm())

const charactersWithProfiles = computed(() =>
  characterStore.characters.map((character) => ({
    ...character,
    profile: buildCharacterProfile(character),
    roleLabel: character.roleType || character.projectRole || '配角',
    coreIdentity: resolveCharacterIdentity(character),
    coreGoal: resolveCharacterGoal(character),
    growthArcLabel: resolveCharacterGrowthArc(character),
    activeStageLabel: resolveCharacterActiveStage(character),
    retiredFlag: resolveRetiredFlag(character.isRetired),
  })),
)

const reusableCharacters = computed(() => {
  const currentIds = new Set(characterStore.characters.map((item) => item.id))
  return characterStore.library.filter((item) => !currentIds.has(item.id))
})

const selectedReusableCharacter = computed(() =>
  reusableCharacters.value.find((item) => item.id === form.existingCharacterId) || null,
)

const attributePreview = computed(() => buildCharacterAttributesJson(cloneCharacterAttributeForm(attributeForm)))
const advancedProfilePreview = computed(() =>
  JSON.stringify(
    {
      identity: attributeForm.identity.trim() || undefined,
      coreGoal: attributeForm.goal.trim() || undefined,
      growthArc: form.growthArc.trim() || undefined,
      firstAppearanceChapterId: form.firstAppearanceChapterId || undefined,
      activeStage: form.activeStage.trim() || undefined,
      isRetired: form.isRetired ? 1 : 0,
      camp: attributeForm.camp.trim() || undefined,
      tags: attributeForm.tags.length ? [...attributeForm.tags] : undefined,
      relations: attributeForm.relations.length ? [...attributeForm.relations] : undefined,
    },
    null,
    2,
  ),
)
const chapterOptions = computed(() =>
  chapterStore.chapters.map((item) => ({
    title: `第 ${item.orderNum || '-'} 章 · ${item.title}`,
    value: item.id,
  })),
)

watch(
  currentProjectId,
  async (projectId) => {
    if (!projectId) {
      return
    }
    await Promise.allSettled([
      characterStore.fetchByProject(projectId),
      characterStore.fetchLibrary(),
      chapterStore.fetchByProject(projectId),
    ])
  },
  { immediate: true },
)

onMounted(() => {
  if (!projectStore.projects.length) {
    projectStore.fetchProjects().catch(() => undefined)
  }
})

function parseJsonObject(raw?: string | null) {
  if (!raw?.trim()) {
    return {}
  }
  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

function readTextValue(source: Record<string, unknown>, key: string) {
  const value = source[key]
  return typeof value === 'string' ? value.trim() : ''
}

function readNumberValue(source: Record<string, unknown>, key: string) {
  const value = source[key]
  const normalized = Number(value)
  return Number.isFinite(normalized) ? normalized : null
}

function resolveRetiredFlag(value?: boolean | number | null) {
  return value === true || value === 1
}

function resolveCharacterIdentity(character?: Character | null) {
  return character?.identity?.trim() || ''
}

function resolveCharacterGoal(character?: Character | null) {
  return character?.coreGoal?.trim() || ''
}

function resolveCharacterGrowthArc(character?: Character | null) {
  return character?.growthArc?.trim() || ''
}

function resolveCharacterActiveStage(character?: Character | null) {
  return character?.activeStage?.trim() || ''
}

function buildCharacterProfile(character: Character) {
  const profile = cloneCharacterAttributeForm(parseCharacterAttributes(character.attributes))
  if (resolveCharacterIdentity(character)) {
    profile.identity = resolveCharacterIdentity(character)
  }
  if (resolveCharacterGoal(character)) {
    profile.goal = resolveCharacterGoal(character)
  }
  return profile
}

function getCharacterCardTab(characterId: number) {
  if (!characterCardTabs[characterId]) {
    characterCardTabs[characterId] = 'summary'
  }
  return characterCardTabs[characterId]
}

function fillAttributeForm(character?: Character | null) {
  const parsed = cloneCharacterAttributeForm(parseCharacterAttributes(character?.attributes))
  parsed.identity = resolveCharacterIdentity(character) || parsed.identity
  parsed.goal = resolveCharacterGoal(character) || parsed.goal
  Object.assign(attributeForm, parsed)
}

function fillForm(character?: Character | null) {
  const advancedProfile = parseJsonObject(character?.advancedProfileJson)
  Object.assign(form, {
    mode: 'create',
    existingCharacterId: null,
    name: character?.name || '',
    description: character?.description || '',
    projectRole: character?.roleType || character?.projectRole || '配角',
    growthArc: resolveCharacterGrowthArc(character) || readTextValue(advancedProfile, 'growthArc'),
    activeStage: resolveCharacterActiveStage(character) || readTextValue(advancedProfile, 'activeStage'),
    firstAppearanceChapterId:
      character?.firstAppearanceChapterId ??
      readNumberValue(advancedProfile, 'firstAppearanceChapterId') ??
      null,
    isRetired: resolveRetiredFlag(character?.isRetired),
  })
  fillAttributeForm(character)
  attributeSuggestionSourceLabel.value = ''
}

function openCreateForm() {
  editingId.value = null
  fillForm(null)
  dialog.value = true
}

function openCreateSummaryWorkflow() {
  summaryWorkflowCreateMode.value = true
  summaryWorkflowTarget.value = null
  summaryWorkflowVisible.value = true
}

function openCreate() {
  openCreateSummaryWorkflow()
}

function openAttach() {
  editingId.value = null
  Object.assign(form, {
    mode: 'attach',
    existingCharacterId: reusableCharacters.value[0]?.id || null,
    name: '',
    description: '',
    projectRole: '配角',
    growthArc: '',
    activeStage: '',
    firstAppearanceChapterId: null,
    isRetired: false,
  })
  fillAttributeForm(selectedReusableCharacter.value)
  dialog.value = true
}

function openEditForm(character: Character) {
  editingId.value = character.id
  fillForm(character)
  dialog.value = true
}

function openEdit(character: Character) {
  openSummaryWorkflow(character)
}

function openSummaryWorkflow(character?: Character | null) {
  summaryWorkflowCreateMode.value = !character
  summaryWorkflowTarget.value = character || null
  summaryWorkflowVisible.value = true
}

function handleSummaryWorkflowExpertEditRequest() {
  summaryWorkflowVisible.value = false
  if (summaryWorkflowCreateMode.value) {
    openCreateForm()
    return
  }
  if (summaryWorkflowTarget.value) {
    openEditForm(summaryWorkflowTarget.value)
  }
}

function requestDelete(character: Character) {
  deletingId.value = character.id
  confirmVisible.value = true
}

function openInventory(character: Character) {
  inventoryCharacter.value = character
  inventoryDialog.value = true
}

watch(
  () => form.existingCharacterId,
  () => {
    if (form.mode === 'attach') {
      fillAttributeForm(selectedReusableCharacter.value)
    }
  },
)

async function submit() {
  if (!currentProjectId.value) {
    return
  }

  submitLoading.value = true
  try {
    if (form.mode === 'attach' && !editingId.value) {
      await characterStore.create(currentProjectId.value, {
        existingCharacterId: form.existingCharacterId || undefined,
        projectRole: form.projectRole,
        roleType: form.projectRole,
      })
    } else {
      const payload = {
        name: form.name.trim(),
        description: form.description.trim(),
        identity: attributeForm.identity.trim(),
        coreGoal: attributeForm.goal.trim(),
        growthArc: form.growthArc.trim(),
        firstAppearanceChapterId: form.firstAppearanceChapterId,
        activeStage: form.activeStage.trim(),
        isRetired: form.isRetired,
        attributes: attributePreview.value,
        advancedProfileJson: advancedProfilePreview.value,
        projectRole: form.projectRole,
        roleType: form.projectRole,
      }

      if (editingId.value) {
        await characterStore.update(currentProjectId.value, editingId.value, payload)
      } else {
        await characterStore.create(currentProjectId.value, payload)
      }
    }

    dialog.value = false
  } finally {
    submitLoading.value = false
  }
}

async function handleSummaryWorkflowApplied(_result: SummaryWorkflowApplyResult) {
  if (!currentProjectId.value) {
    return
  }
  await Promise.allSettled([
    characterStore.fetchByProject(currentProjectId.value),
    characterStore.fetchLibrary(),
  ])
}

async function generateCharacterNames() {
  if (!currentProjectId.value) {
    return
  }

  nameSuggestionLoading.value = true
  nameSuggestionDialog.value = true

  try {
    const result = await generateNameSuggestions(currentProjectId.value, {
      entityType: 'character',
      brief:
        [form.description.trim(), attributePreview.value !== '{}' ? attributePreview.value : '']
          .filter(Boolean)
          .join('\n\n') || '请根据当前项目风格生成角色名称。',
      extraRequirements: '名称要适合中文小说人物，并与角色气质一致。',
      count: 6,
    })
    nameSuggestions.value = result.suggestions || []
    nameSuggestionSourceLabel.value = `生成模型：${result.providerName || '未命名服务'} / ${result.modelName || '未命名模型'}`
  } finally {
    nameSuggestionLoading.value = false
  }
}

async function generateAttributeSuggestions() {
  if (!currentProjectId.value) {
    return
  }

  attributeSuggestionLoading.value = true
  try {
    const result = await generateCharacterAttributes(currentProjectId.value, {
      name: form.name.trim(),
      description: form.description.trim(),
      extraRequirements: `项目：${currentProject.value?.name || '当前项目'}。请优先给出可直接用于人物卡的技能、特性、天赋、弱点和关系。`,
    })
    applyAttributeSuggestion(result)
    attributeSuggestionSourceLabel.value = `属性生成模型：${result.providerName || '未命名服务'} / ${result.modelName || '未命名模型'}`
  } finally {
    attributeSuggestionLoading.value = false
  }
}

function applySuggestedName(value: string) {
  form.name = value
  nameSuggestionDialog.value = false
}

function applyAttributeSuggestion(result: CharacterAttributeSuggestionResult) {
  applyTextField('age', result.age)
  applyTextField('gender', result.gender)
  applyTextField('identity', result.identity)
  applyTextField('camp', result.camp)
  applyTextField('goal', result.goal)
  applyTextField('background', result.background)
  applyTextField('appearance', result.appearance)
  applyTextField('notes', result.notes)
  applyListField('traits', result.traits)
  applyListField('talents', result.talents)
  applyListField('skills', result.skills)
  applyListField('weaknesses', result.weaknesses)
  applyListField('equipment', result.equipment)
  applyListField('tags', result.tags)
  applyListField('relations', result.relations)
}

function applyTextField(key: keyof typeof attributeForm, value?: string) {
  if (value?.trim()) {
    ;(attributeForm[key] as string) = value.trim()
  }
}

function applyListField(key: keyof typeof attributeForm, value?: string[]) {
  if (!value?.length) {
    return
  }
  ;(attributeForm[key] as string[]) = [...new Set(value.map((item) => item.trim()).filter(Boolean))]
}

function applyTemplate(template: (typeof characterTemplates)[number]) {
  if (template.values.identity) {
    attributeForm.identity = template.values.identity
  }
  if (template.values.camp) {
    attributeForm.camp = template.values.camp
  }
  if (template.values.goal) {
    attributeForm.goal = template.values.goal
  }

  mergeListField('traits', template.values.traits)
  mergeListField('talents', template.values.talents)
  mergeListField('skills', template.values.skills)
  mergeListField('weaknesses', template.values.weaknesses)
  mergeListField('tags', template.values.tags)
}

function mergeListField(key: CharacterArrayField, values?: string[]) {
  if (!values?.length) {
    return
  }

  attributeForm[key] = [...new Set([...attributeForm[key], ...values])]
}

function toggleListOption(key: CharacterArrayField, value: string) {
  const currentValues = [...attributeForm[key]]
  attributeForm[key] = currentValues.includes(value)
    ? currentValues.filter((item) => item !== value)
    : [...currentValues, value]
}

function clearListOption(key: CharacterArrayField) {
  attributeForm[key] = []
}

async function confirmDelete() {
  if (!currentProjectId.value || !deletingId.value) {
    return
  }

  await characterStore.remove(currentProjectId.value, deletingId.value)
  deletingId.value = null
  confirmVisible.value = false
}

async function refreshCharacterInventorySummary() {
  if (!currentProjectId.value) {
    return
  }
  await characterStore.fetchByProject(currentProjectId.value)
  if (inventoryCharacter.value) {
    inventoryCharacter.value =
      characterStore.characters.find((item) => item.id === inventoryCharacter.value?.id) || inventoryCharacter.value
  }
}
</script>

<template>
  <PageContainer
    title="人物管理"
    description="人物现在以可复用的角色库来管理。一个人物可以关联到多个项目，而当前项目里再单独标注主角、配角、反派等定位。"
  >
    <template #actions>
      <div class="d-flex flex-wrap ga-2 align-center">
        <template v-if="summaryFirstMode">
          <v-chip color="primary" variant="tonal">普通模式</v-chip>
          <div class="text-caption text-medium-emphasis">默认只需要说人物想法，AI 会继续追问并整理。</div>
          <v-btn variant="text" color="secondary" @click="editorMode = 'EXPERT'">切到专家模式</v-btn>
        </template>
        <v-segmented-button v-else v-model="editorMode" color="primary" mandatory>
          <v-btn value="DEFAULT">普通模式</v-btn>
          <v-btn value="EXPERT">专家模式</v-btn>
        </v-segmented-button>
        <v-btn color="primary" prepend-icon="mdi-account-plus-outline" :disabled="!currentProjectId" @click="openCreate">
          {{ createButtonLabel }}
        </v-btn>
        <v-btn variant="outlined" prepend-icon="mdi-link-variant" :disabled="!currentProjectId" @click="openAttach">
          关联已有
        </v-btn>
      </div>
    </template>

    <EmptyState
      v-if="!currentProjectId"
      title="尚未选择项目"
      description="当前人物列表与项目绑定，请先在左侧选中一个小说项目。"
    />

    <EmptyState
      v-else-if="!characterStore.characters.length"
      title="当前项目还没有人物"
      description="可以新建人物，也可以把角色库里已有的人物直接关联到当前项目。"
    >
      <div class="d-flex ga-2">
        <v-btn color="primary" prepend-icon="mdi-account-plus-outline" @click="openCreate">{{ emptyCreateButtonLabel }}</v-btn>
        <v-btn variant="outlined" prepend-icon="mdi-link-variant" @click="openAttach">关联已有</v-btn>
      </div>
    </EmptyState>

    <div v-else>
      <v-alert class="mb-4" type="info" variant="tonal">
        {{
          summaryFirstMode
            ? '当前是普通模式：新增和编辑都先走对话式摘要工作流；你只要给模糊印象，AI 会继续追问并整理。'
            : '当前是专家模式：新增和编辑仍先进入摘要工作流，但会默认使用直填摘要；如需深度字段可切到专家表单。'
        }}
      </v-alert>

      <v-row>
      <v-col v-for="item in charactersWithProfiles" :key="item.id" cols="12" md="6" xl="4">
        <v-card class="soft-panel h-100">
          <v-card-text class="d-flex flex-column h-100">
            <div class="d-flex align-center justify-space-between ga-3">
              <div>
                <div class="text-h6">{{ item.name }}</div>
                <div class="text-caption text-medium-emphasis mt-1">{{ item.roleLabel }}</div>
              </div>
              <v-chip color="secondary" variant="tonal" size="small">
                关联项目 {{ item.projectNames?.length || 1 }}
              </v-chip>
            </div>

            <div class="text-body-2 text-medium-emphasis mt-3">
              {{ item.description || '暂无角色简介。' }}
            </div>

            <v-tabs
              :model-value="getCharacterCardTab(item.id)"
              color="primary"
              density="comfortable"
              class="mt-4"
              @update:model-value="(value) => (characterCardTabs[item.id] = String(value))"
            >
              <v-tab value="summary">Summary</v-tab>
              <v-tab value="canon">Canon</v-tab>
              <v-tab value="state">State</v-tab>
              <v-tab value="history">History</v-tab>
            </v-tabs>

            <v-window :model-value="getCharacterCardTab(item.id)" class="mt-4">
              <v-window-item value="summary">
                <div class="text-caption text-medium-emphasis">人物摘要</div>
                <MarkdownContent
                  class="mt-2"
                  compact
                  :source="item.description"
                  empty-text="当前还没有人物摘要，建议先用摘要工作流补一版。"
                />
                <div class="d-flex flex-wrap ga-2 mt-4">
                  <v-chip
                    v-for="tag in item.profile.tags"
                    :key="tag"
                    size="small"
                    color="secondary"
                    variant="outlined"
                  >
                    {{ tag }}
                  </v-chip>
                  <v-chip
                    v-if="item.retiredFlag"
                    size="small"
                    color="warning"
                    variant="tonal"
                  >
                    已退场
                  </v-chip>
                  <span
                    v-if="!item.profile.tags.length && !item.retiredFlag"
                    class="text-caption text-medium-emphasis"
                  >
                    暂无摘要标签
                  </span>
                </div>
              </v-window-item>

              <v-window-item value="canon">
                <div class="text-caption text-medium-emphasis">角色设定</div>
                <div class="text-caption text-medium-emphasis mt-3">身份：{{ item.coreIdentity || '未填写' }}</div>
                <div class="text-caption text-medium-emphasis mt-1">阵营：{{ item.profile.camp || '未填写' }}</div>
                <div class="text-caption text-medium-emphasis mt-1">目标：{{ item.coreGoal || '未填写' }}</div>
                <div class="text-caption text-medium-emphasis mt-1">成长弧线：{{ item.growthArcLabel || '未填写' }}</div>
                <div class="text-caption text-medium-emphasis mt-1">当前阶段：{{ item.activeStageLabel || '未填写' }}</div>
                <div class="d-flex flex-wrap ga-2 mt-4">
                  <v-chip v-if="item.firstAppearanceChapterId" size="small" variant="outlined">
                    初登场章节 #{{ item.firstAppearanceChapterId }}
                  </v-chip>
                </div>
              </v-window-item>

              <v-window-item value="state">
                <div class="text-caption text-medium-emphasis">当前状态</div>
                <div class="mt-3">
                  <div class="text-caption text-medium-emphasis mb-2">技能</div>
                  <div class="d-flex flex-wrap ga-2">
                    <v-chip
                      v-for="skill in item.profile.skills"
                      :key="skill"
                      size="small"
                      color="primary"
                      variant="tonal"
                    >
                      {{ skill }}
                    </v-chip>
                    <span v-if="!item.profile.skills.length" class="text-caption text-medium-emphasis">暂无技能</span>
                  </div>
                </div>
                <div class="mt-4">
                  <div class="text-caption text-medium-emphasis mb-2">特性 / 天赋 / 弱点</div>
                  <div class="d-flex flex-wrap ga-2">
                    <v-chip
                      v-for="tag in [...item.profile.traits, ...item.profile.talents]"
                      :key="`trait-${tag}`"
                      size="small"
                      color="secondary"
                      variant="outlined"
                    >
                      {{ tag }}
                    </v-chip>
                    <v-chip
                      v-for="weakness in item.profile.weaknesses"
                      :key="`weakness-${weakness}`"
                      size="small"
                      color="error"
                      variant="tonal"
                    >
                      {{ weakness }}
                    </v-chip>
                    <span
                      v-if="![...item.profile.traits, ...item.profile.talents, ...item.profile.weaknesses].length"
                      class="text-caption text-medium-emphasis"
                    >
                      暂无状态标签
                    </span>
                  </div>
                </div>
                <div class="mt-4">
                  <div class="text-caption text-medium-emphasis mb-2">背包摘要</div>
                  <div class="d-flex flex-wrap ga-2">
                    <v-chip size="small" color="primary" variant="tonal">
                      物品 {{ item.inventoryItemCount || 0 }}
                    </v-chip>
                    <v-chip size="small" color="secondary" variant="outlined">
                      已装备 {{ item.equippedItemCount || 0 }}
                    </v-chip>
                    <v-chip size="small" color="warning" variant="outlined">
                      稀有 {{ item.rareItemCount || 0 }}
                    </v-chip>
                  </div>
                </div>
              </v-window-item>

              <v-window-item value="history">
                <div class="text-caption text-medium-emphasis">关联历史</div>
                <div class="d-flex flex-wrap ga-2 mt-3">
                  <v-chip
                    v-for="projectName in item.projectNames"
                    :key="projectName"
                    size="small"
                    variant="outlined"
                  >
                    {{ projectName }}
                  </v-chip>
                  <span v-if="!item.projectNames?.length" class="text-caption text-medium-emphasis">暂无关联项目</span>
                </div>
                <div class="text-caption text-medium-emphasis mt-4">
                  创建时间：{{ item.createTime || '未记录' }}
                </div>
                <div class="text-caption text-medium-emphasis mt-1">
                  更新时间：{{ item.updateTime || '未记录' }}
                </div>
              </v-window-item>
            </v-window>

            <div class="d-flex flex-wrap ga-2 mt-auto pt-4">
              <v-btn color="primary" prepend-icon="mdi-text-box-edit-outline" @click="openSummaryWorkflow(item)">
                摘要优先编辑
              </v-btn>
              <v-btn color="secondary" variant="outlined" @click="openInventory(item)">背包</v-btn>
              <v-btn variant="text" @click="openEdit(item)">编辑</v-btn>
              <v-btn color="error" variant="text" @click="requestDelete(item)">移出项目</v-btn>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
      </v-row>
    </div>

    <v-dialog v-model="dialog" max-width="960">
      <v-card>
        <v-card-title>{{ editingId ? '编辑人物' : form.mode === 'attach' ? '关联已有角色' : '新增人物' }}</v-card-title>
        <v-card-text class="pt-4">
          <v-row>
            <v-col v-if="!editingId" cols="12">
              <v-segmented-button v-model="form.mode" color="primary" mandatory>
                <v-btn value="create">新建人物</v-btn>
                <v-btn value="attach">关联已有</v-btn>
              </v-segmented-button>
            </v-col>

            <template v-if="form.mode === 'attach' && !editingId">
              <v-col cols="12" md="6">
                <v-select
                  v-model="form.existingCharacterId"
                  label="选择可复用人物"
                  :items="reusableCharacters"
                  item-title="name"
                  item-value="id"
                  clearable
                />
              </v-col>
              <v-col cols="12" md="6">
                <v-select
                  v-model="form.projectRole"
                  label="当前项目中的角色定位 / roleType"
                  :items="projectRoleOptions"
                  clearable
                />
              </v-col>
              <v-col cols="12">
                <v-alert type="info" variant="tonal">
                  关联后只是在当前项目里增加一条绑定，不会覆盖这个人物在其他项目里的设定。
                </v-alert>
              </v-col>
              <v-col v-if="selectedReusableCharacter" cols="12">
                <div class="text-subtitle-2 font-weight-medium">角色预览</div>
                <div class="mt-2">
                  <MarkdownContent compact :source="selectedReusableCharacter.description" empty-text="暂无简介" />
                </div>
                <div v-if="selectedReusableCharacter.projectNames?.length" class="d-flex flex-wrap ga-2 mt-3">
                  <v-chip
                    v-for="projectName in selectedReusableCharacter.projectNames"
                    :key="projectName"
                    size="small"
                    variant="outlined"
                  >
                    {{ projectName }}
                  </v-chip>
                </div>
              </v-col>
            </template>

            <template v-else>
              <v-col cols="12" md="4">
                <v-select
                  v-model="form.projectRole"
                  label="当前项目中的角色定位 / roleType"
                  :items="projectRoleOptions"
                  clearable
                />
              </v-col>
              <v-col cols="12" md="8">
                <div class="d-flex ga-2 align-start">
                  <v-text-field v-model="form.name" class="flex-grow-1" label="角色名称" />
                  <v-btn class="mt-2" variant="outlined" @click="generateCharacterNames">AI 生成人名</v-btn>
                </div>
              </v-col>
              <v-col cols="12">
                <MarkdownEditor
                  v-model="form.description"
                  label="角色描述"
                  :rows="5"
                  hint="尽量描述身份、气质、经历或当前定位，下面的属性生成会优先参考这里。"
                  persistent-hint
                  auto-grow
                  preview-empty-text="暂无角色描述"
                />
              </v-col>
              <v-col cols="12" class="pt-0">
                <div class="text-subtitle-2 font-weight-medium">快速模板</div>
                <div class="d-flex flex-wrap ga-2 mt-3">
                  <v-chip
                    v-for="template in characterTemplates"
                    :key="template.name"
                    color="secondary"
                    variant="outlined"
                    @click="applyTemplate(template)"
                  >
                    {{ template.name }}
                  </v-chip>
                </div>
              </v-col>
              <v-col cols="12" class="pt-0">
                <div class="d-flex flex-wrap ga-3 align-center">
                  <v-btn color="primary" variant="tonal" :loading="attributeSuggestionLoading" @click="generateAttributeSuggestions">
                    根据描述生成属性
                  </v-btn>
                  <span class="text-caption text-medium-emphasis">
                    会自动填充年龄、身份、阵营、技能、特性、天赋、弱点等字段。
                  </span>
                </div>
                <v-alert v-if="attributeSuggestionSourceLabel" type="info" variant="tonal" class="mt-3">
                  {{ attributeSuggestionSourceLabel }}
                </v-alert>
              </v-col>

              <v-col cols="12" md="4">
                <v-text-field v-model="attributeForm.age" label="年龄" />
              </v-col>
              <v-col cols="12" md="4">
                <v-select v-model="attributeForm.gender" label="性别" :items="['男', '女', '其他', '未知']" clearable />
              </v-col>
              <v-col cols="12" md="4">
                <v-combobox v-model="attributeForm.camp" label="阵营" :items="campOptions" />
              </v-col>
              <v-col cols="12" md="6">
                <v-text-field v-model="attributeForm.identity" label="身份 / 职业" />
              </v-col>
              <v-col cols="12" md="6">
                <v-text-field v-model="attributeForm.goal" label="核心目标" />
              </v-col>
              <v-col cols="12" md="6">
                <MarkdownEditor
                  v-model="form.growthArc"
                  label="成长弧线"
                  :rows="4"
                  auto-grow
                  preview-empty-text="暂无成长弧线"
                />
              </v-col>
              <v-col cols="12" md="3">
                <v-text-field v-model="form.activeStage" label="当前阶段" placeholder="例如：入局期 / 破局前夜" />
              </v-col>
              <v-col cols="12" md="3">
                <v-select
                  v-model="form.firstAppearanceChapterId"
                  label="初登场章节"
                  :items="chapterOptions"
                  item-title="title"
                  item-value="value"
                  clearable
                />
              </v-col>
              <v-col cols="12" md="3">
                <v-switch v-model="form.isRetired" color="warning" label="是否退场" inset />
              </v-col>
              <v-col cols="12" md="6">
                <MarkdownEditor
                  v-model="attributeForm.background"
                  label="背景"
                  :rows="5"
                  auto-grow
                  preview-empty-text="暂无背景"
                />
              </v-col>
              <v-col cols="12" md="6">
                <MarkdownEditor
                  v-model="attributeForm.appearance"
                  label="外貌"
                  :rows="5"
                  auto-grow
                  preview-empty-text="暂无外貌描写"
                />
              </v-col>

              <v-col cols="12" md="6">
                <div class="d-flex justify-space-between align-center mb-2">
                  <div class="text-subtitle-2">技能</div>
                  <v-btn size="small" variant="text" @click="clearListOption('skills')">清空</v-btn>
                </div>
                <div class="d-flex flex-wrap ga-2 mb-3">
                  <v-chip
                    v-for="option in skillOptions"
                    :key="option"
                    :color="attributeForm.skills.includes(option) ? 'primary' : undefined"
                    :variant="attributeForm.skills.includes(option) ? 'tonal' : 'outlined'"
                    size="small"
                    @click="toggleListOption('skills', option)"
                  >
                    {{ option }}
                  </v-chip>
                </div>
                <v-combobox
                  v-model="attributeForm.skills"
                  label="技能"
                  :items="skillOptions"
                  multiple
                  chips
                  closable-chips
                  clearable
                />
              </v-col>

              <v-col cols="12" md="6">
                <div class="d-flex justify-space-between align-center mb-2">
                  <div class="text-subtitle-2">特性</div>
                  <v-btn size="small" variant="text" @click="clearListOption('traits')">清空</v-btn>
                </div>
                <div class="d-flex flex-wrap ga-2 mb-3">
                  <v-chip
                    v-for="option in traitOptions"
                    :key="option"
                    :color="attributeForm.traits.includes(option) ? 'secondary' : undefined"
                    :variant="attributeForm.traits.includes(option) ? 'tonal' : 'outlined'"
                    size="small"
                    @click="toggleListOption('traits', option)"
                  >
                    {{ option }}
                  </v-chip>
                </div>
                <v-combobox
                  v-model="attributeForm.traits"
                  label="特性"
                  :items="traitOptions"
                  multiple
                  chips
                  closable-chips
                  clearable
                />
              </v-col>

              <v-col cols="12" md="6">
                <div class="d-flex justify-space-between align-center mb-2">
                  <div class="text-subtitle-2">天赋</div>
                  <v-btn size="small" variant="text" @click="clearListOption('talents')">清空</v-btn>
                </div>
                <div class="d-flex flex-wrap ga-2 mb-3">
                  <v-chip
                    v-for="option in talentOptions"
                    :key="option"
                    :color="attributeForm.talents.includes(option) ? 'secondary' : undefined"
                    :variant="attributeForm.talents.includes(option) ? 'tonal' : 'outlined'"
                    size="small"
                    @click="toggleListOption('talents', option)"
                  >
                    {{ option }}
                  </v-chip>
                </div>
                <v-combobox
                  v-model="attributeForm.talents"
                  label="天赋"
                  :items="talentOptions"
                  multiple
                  chips
                  closable-chips
                  clearable
                />
              </v-col>

              <v-col cols="12" md="6">
                <div class="d-flex justify-space-between align-center mb-2">
                  <div class="text-subtitle-2">弱点</div>
                  <v-btn size="small" variant="text" @click="clearListOption('weaknesses')">清空</v-btn>
                </div>
                <div class="d-flex flex-wrap ga-2 mb-3">
                  <v-chip
                    v-for="option in weaknessOptions"
                    :key="option"
                    :color="attributeForm.weaknesses.includes(option) ? 'error' : undefined"
                    :variant="attributeForm.weaknesses.includes(option) ? 'tonal' : 'outlined'"
                    size="small"
                    @click="toggleListOption('weaknesses', option)"
                  >
                    {{ option }}
                  </v-chip>
                </div>
                <v-combobox
                  v-model="attributeForm.weaknesses"
                  label="弱点"
                  :items="weaknessOptions"
                  multiple
                  chips
                  closable-chips
                  clearable
                />
              </v-col>

              <v-col cols="12" md="6">
                <v-combobox
                  v-model="attributeForm.equipment"
                  label="装备"
                  multiple
                  chips
                  closable-chips
                  clearable
                />
              </v-col>

              <v-col cols="12" md="6">
                <div class="d-flex justify-space-between align-center mb-2">
                  <div class="text-subtitle-2">标签</div>
                  <v-btn size="small" variant="text" @click="clearListOption('tags')">清空</v-btn>
                </div>
                <div class="d-flex flex-wrap ga-2 mb-3">
                  <v-chip
                    v-for="option in tagOptions"
                    :key="option"
                    :color="attributeForm.tags.includes(option) ? 'secondary' : undefined"
                    :variant="attributeForm.tags.includes(option) ? 'tonal' : 'outlined'"
                    size="small"
                    @click="toggleListOption('tags', option)"
                  >
                    {{ option }}
                  </v-chip>
                </div>
                <v-combobox
                  v-model="attributeForm.tags"
                  label="标签"
                  :items="tagOptions"
                  multiple
                  chips
                  closable-chips
                  clearable
                />
              </v-col>

              <v-col cols="12">
                <div class="d-flex justify-space-between align-center mb-2">
                  <div class="text-subtitle-2">关系线索</div>
                  <v-btn size="small" variant="text" @click="clearListOption('relations')">清空</v-btn>
                </div>
                <div class="d-flex flex-wrap ga-2 mb-3">
                  <v-chip
                    v-for="option in relationHintOptions"
                    :key="option"
                    :color="attributeForm.relations.includes(option) ? 'secondary' : undefined"
                    :variant="attributeForm.relations.includes(option) ? 'tonal' : 'outlined'"
                    size="small"
                    @click="toggleListOption('relations', option)"
                  >
                    {{ option }}
                  </v-chip>
                </div>
                <v-combobox
                  v-model="attributeForm.relations"
                  label="关系线索"
                  :items="relationHintOptions"
                  multiple
                  chips
                  closable-chips
                  clearable
                />
              </v-col>

              <v-col cols="12">
                <MarkdownEditor
                  v-model="attributeForm.notes"
                  label="备注 / 秘密"
                  :rows="4"
                  auto-grow
                  preview-empty-text="暂无备注"
                />
              </v-col>
              <v-col cols="12">
                <v-textarea
                  :model-value="attributePreview"
                  rows="8"
                  label="自动组装后的属性 JSON"
                  readonly
                  auto-grow
                />
              </v-col>
              <v-col cols="12">
                <v-textarea
                  :model-value="advancedProfilePreview"
                  rows="8"
                  label="自动组装后的高级画像 JSON"
                  readonly
                  auto-grow
                />
              </v-col>
            </template>
          </v-row>
        </v-card-text>
        <v-card-actions class="justify-end">
          <v-btn variant="text" @click="dialog = false">取消</v-btn>
          <v-btn color="primary" :loading="submitLoading" @click="submit">保存</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <ConfirmDialog
      v-model="confirmVisible"
      title="移出项目"
      text="确认把这个人物从当前项目移除吗？人物本体仍会保留在角色库里，供其他项目继续复用。"
      @confirm="confirmDelete"
    />

    <NameSuggestionDialog
      v-model="nameSuggestionDialog"
      title="选择人物名称"
      :loading="nameSuggestionLoading"
      :suggestions="nameSuggestions"
      :source-label="nameSuggestionSourceLabel"
      empty-text="这次没有拿到合适的人物名，可以重新生成。"
      @refresh="generateCharacterNames"
      @select="applySuggestedName"
    />

    <CharacterInventoryDialog
      v-model="inventoryDialog"
      :project-id="currentProjectId"
      :character="inventoryCharacter"
      @changed="refreshCharacterInventorySummary"
    />

    <SummaryWorkflowDialog
      v-model="summaryWorkflowVisible"
      :project-id="currentProjectId"
      target-type="CHARACTER"
      :create-mode="summaryWorkflowCreateMode"
      :initial-operator-mode="editorMode"
      :allow-expert-form-switch="true"
      :target-source-id="summaryWorkflowTarget?.id || null"
      :title="summaryWorkflowTarget?.name || '新人物'"
      target-label="人物"
      :initial-summary="summaryWorkflowTarget?.description || ''"
      @applied="handleSummaryWorkflowApplied"
      @expert-edit-request="handleSummaryWorkflowExpertEditRequest"
    />
  </PageContainer>
</template>
