<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import { generateCharacterAttributes } from '@/api/character'
import { generateNameSuggestions } from '@/api/name-suggestion'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import NameSuggestionDialog from '@/components/NameSuggestionDialog.vue'
import PageContainer from '@/components/PageContainer.vue'
import { useCharacterStore } from '@/stores/character'
import { useProjectStore } from '@/stores/project'
import type { Character, CharacterAttributeSuggestionResult } from '@/types'
import {
  buildCharacterAttributesJson,
  cloneCharacterAttributeForm,
  createEmptyCharacterAttributeForm,
  parseCharacterAttributes,
} from '@/utils/character-attributes'

const projectStore = useProjectStore()
const characterStore = useCharacterStore()

const dialog = ref(false)
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

const currentProjectId = computed(() => projectStore.selectedProjectId)
const currentProject = computed(() =>
  projectStore.projects.find((item) => item.id === currentProjectId.value) || null,
)

const form = reactive({
  name: '',
  description: '',
})

const attributeForm = reactive(createEmptyCharacterAttributeForm())

const campOptions = ['主角阵营', '伙伴阵营', '中立阵营', '反派阵营', '组织成员', '家族势力']
const traitOptions = ['冷静', '冲动', '理性', '敏感', '果断', '顽强', '温柔', '多疑', '幽默', '骄傲']
const talentOptions = ['战斗天赋', '感知天赋', '谋略天赋', '领导天赋', '潜行天赋', '学习天赋', '交涉天赋']
const skillOptions = ['剑术', '格斗', '射击', '黑客', '谈判', '侦查', '治疗', '魔法', '炼金', '驾驶']
const weaknessOptions = ['怕火', '体力差', '优柔寡断', '情绪失控', '过度自信', '信任过头', '信息闭塞']
const tagOptions = ['成长型', '智谋型', '热血型', '悲剧型', '反差感', '宿命感', '神秘感']

const charactersWithProfiles = computed(() =>
  characterStore.characters.map((character) => ({
    ...character,
    profile: parseCharacterAttributes(character.attributes),
  })),
)

const attributePreview = computed(() => buildCharacterAttributesJson(cloneCharacterAttributeForm(attributeForm)))

watch(
  currentProjectId,
  async (projectId) => {
    if (projectId) {
      await characterStore.fetchByProject(projectId).catch(() => undefined)
    }
  },
  { immediate: true },
)

onMounted(() => {
  if (!projectStore.projects.length) {
    projectStore.fetchProjects().catch(() => undefined)
  }
})

function fillAttributeForm(character?: Character | null) {
  Object.assign(
    attributeForm,
    cloneCharacterAttributeForm(parseCharacterAttributes(character?.attributes)),
  )
}

function fillForm(character?: Character | null) {
  Object.assign(form, {
    name: character?.name || '',
    description: character?.description || '',
  })
  fillAttributeForm(character)
  attributeSuggestionSourceLabel.value = ''
}

function openCreate() {
  editingId.value = null
  fillForm(null)
  dialog.value = true
}

function openEdit(character: Character) {
  editingId.value = character.id
  fillForm(character)
  dialog.value = true
}

function requestDelete(character: Character) {
  deletingId.value = character.id
  confirmVisible.value = true
}

async function submit() {
  if (!currentProjectId.value) {
    return
  }

  submitLoading.value = true
  try {
    const payload = {
      name: form.name.trim(),
      description: form.description.trim(),
      attributes: attributePreview.value,
    }

    if (editingId.value) {
      await characterStore.update(currentProjectId.value, editingId.value, payload)
    } else {
      await characterStore.create(currentProjectId.value, payload)
    }

    dialog.value = false
  } finally {
    submitLoading.value = false
  }
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
          .join('\n\n') || '请根据当前项目风格生成人物名称',
      extraRequirements: '名称要适合中文小说人物，尽量有辨识度，并与角色气质一致。',
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

async function confirmDelete() {
  if (!currentProjectId.value || !deletingId.value) {
    return
  }

  await characterStore.remove(currentProjectId.value, deletingId.value)
  deletingId.value = null
  confirmVisible.value = false
}
</script>

<template>
  <PageContainer
    title="人物管理"
    description="维护角色卡、描述和结构化属性。现在可以直接通过描述生成技能、特性、天赋等字段，不需要手写 JSON。"
  >
    <template #actions>
      <v-btn color="primary" prepend-icon="mdi-account-plus-outline" :disabled="!currentProjectId" @click="openCreate">
        添加人物
      </v-btn>
    </template>

    <EmptyState
      v-if="!currentProjectId"
      title="尚未选择项目"
      description="当前人物列表与项目绑定，请先在左侧选中一个小说项目。"
    />

    <EmptyState
      v-else-if="!characterStore.characters.length"
      title="还没有人物"
      description="先创建一个角色，后续剧情、因果和写作中心都可以直接关联它。"
    >
      <v-btn color="primary" prepend-icon="mdi-account-plus-outline" @click="openCreate">创建人物</v-btn>
    </EmptyState>

    <v-row v-else>
      <v-col v-for="item in charactersWithProfiles" :key="item.id" cols="12" md="6" xl="4">
        <v-card class="soft-panel h-100">
          <v-card-text class="d-flex flex-column h-100">
            <div class="d-flex align-center justify-space-between">
              <div class="text-h6">{{ item.name }}</div>
              <v-icon icon="mdi-account-star-outline" color="secondary" />
            </div>

            <div class="text-body-2 text-medium-emphasis mt-3">
              {{ item.description || '暂无角色简介' }}
            </div>

            <v-divider class="my-4" />

            <div class="text-caption text-medium-emphasis">
              身份：{{ item.profile.identity || '未填写' }}
            </div>
            <div class="text-caption text-medium-emphasis mt-1">
              阵营：{{ item.profile.camp || '未填写' }}
            </div>
            <div class="text-caption text-medium-emphasis mt-1">
              目标：{{ item.profile.goal || '未填写' }}
            </div>

            <div class="mt-4">
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
              <div class="text-caption text-medium-emphasis mb-2">特性 / 天赋</div>
              <div class="d-flex flex-wrap ga-2">
                <v-chip
                  v-for="tag in [...item.profile.traits, ...item.profile.talents]"
                  :key="tag"
                  size="small"
                  color="secondary"
                  variant="outlined"
                >
                  {{ tag }}
                </v-chip>
                <span
                  v-if="![...item.profile.traits, ...item.profile.talents].length"
                  class="text-caption text-medium-emphasis"
                >
                  暂无特性或天赋
                </span>
              </div>
            </div>

            <div class="d-flex ga-2 mt-auto pt-4">
              <v-btn variant="outlined" @click="openEdit(item)">编辑</v-btn>
              <v-btn color="error" variant="text" @click="requestDelete(item)">删除</v-btn>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-dialog v-model="dialog" max-width="960">
      <v-card>
        <v-card-title>{{ editingId ? '编辑人物' : '新增人物' }}</v-card-title>
        <v-card-text class="pt-4">
          <v-row>
            <v-col cols="12" md="8">
              <div class="d-flex ga-2 align-start">
                <v-text-field v-model="form.name" class="flex-grow-1" label="角色名称" />
                <v-btn class="mt-2" variant="outlined" @click="generateCharacterNames">AI 生成人名</v-btn>
              </div>
            </v-col>
            <v-col cols="12" md="4">
              <v-select
                v-model="attributeForm.gender"
                label="性别"
                :items="['男', '女', '其他', '未知']"
                clearable
              />
            </v-col>
            <v-col cols="12">
              <v-textarea
                v-model="form.description"
                rows="4"
                label="角色描述"
                hint="尽量描述身份、气质、经历或当前定位，下面的属性生成会优先参考这里。"
                persistent-hint
              />
            </v-col>
            <v-col cols="12" class="pt-0">
              <div class="d-flex flex-wrap ga-3 align-center">
                <v-btn color="primary" variant="tonal" :loading="attributeSuggestionLoading" @click="generateAttributeSuggestions">
                  根据描述生成属性
                </v-btn>
                <span class="text-caption text-medium-emphasis">
                  生成后会自动填充年龄、身份、阵营、技能、特性、天赋等字段。
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
              <v-text-field v-model="attributeForm.identity" label="身份 / 职业" />
            </v-col>
            <v-col cols="12" md="4">
              <v-combobox v-model="attributeForm.camp" label="阵营" :items="campOptions" />
            </v-col>
            <v-col cols="12">
              <v-text-field v-model="attributeForm.goal" label="核心目标" />
            </v-col>
            <v-col cols="12" md="6">
              <v-textarea v-model="attributeForm.background" rows="4" label="背景" />
            </v-col>
            <v-col cols="12" md="6">
              <v-textarea v-model="attributeForm.appearance" rows="4" label="外貌" />
            </v-col>
            <v-col cols="12" md="6">
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
              <v-combobox
                v-model="attributeForm.relations"
                label="关系线索"
                multiple
                chips
                closable-chips
                clearable
                hint="可填写“导师”“宿敌”“青梅竹马”“上级”等角色关系关键词。"
                persistent-hint
              />
            </v-col>
            <v-col cols="12">
              <v-textarea v-model="attributeForm.notes" rows="3" label="备注 / 秘密" />
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
      title="删除人物"
      text="确认删除这个人物吗？剧情、因果和知识条目里已经引用的文字不会自动改写。"
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
  </PageContainer>
</template>
