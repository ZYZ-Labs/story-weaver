<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import EmptyState from '@/components/EmptyState.vue'
import MarkdownContent from '@/components/MarkdownContent.vue'
import PageContainer from '@/components/PageContainer.vue'
import {
  attachWorldSettingToProject,
  detachWorldSettingFromProject,
  getWorldSettingLibrary,
  getWorldSettings,
} from '@/api/world-setting'
import { useCharacterStore } from '@/stores/character'
import { useChapterStore } from '@/stores/chapter'
import { useProjectStore } from '@/stores/project'
import type { WorldSetting } from '@/types'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()
const chapterStore = useChapterStore()
const characterStore = useCharacterStore()

const worldSettings = ref<WorldSetting[]>([])
const libraryWorldSettings = ref<WorldSetting[]>([])
const selectedLibraryWorldSettingIds = ref<number[]>([])
const associationLoading = ref(false)
const tab = ref('overview')

const projectId = computed(() => Number(route.params.id))
const project = computed(() => projectStore.projects.find((item) => item.id === projectId.value) || null)

const associatedIdSet = computed(() => new Set(worldSettings.value.map((item) => item.id)))
const libraryCandidates = computed(() =>
  libraryWorldSettings.value
    .filter((item) => !associatedIdSet.value.has(item.id))
    .map((item) => ({
      title: getWorldSettingTitle(item),
      value: item.id,
      subtitle: item.category || '未分类',
    })),
)

function getWorldSettingTitle(item: WorldSetting) {
  return item.name || item.title || item.category || '未命名设定'
}

function getWorldSettingDescription(item: WorldSetting) {
  return item.description || item.content || '暂无设定内容'
}

function syncProjectWorldSettingSummary(items: WorldSetting[]) {
  if (!project.value) {
    return
  }

  project.value.worldSettingIds = items.map((item) => item.id)
  project.value.worldSettingNames = items.map((item) => getWorldSettingTitle(item))
}

async function loadWorldSettingsData(id: number) {
  const [associated, library] = await Promise.all([
    getWorldSettings(id).catch(() => [] as WorldSetting[]),
    getWorldSettingLibrary().catch(() => [] as WorldSetting[]),
  ])

  worldSettings.value = associated
  libraryWorldSettings.value = library
  syncProjectWorldSettingSummary(associated)
}

async function loadProjectData(id: number) {
  await Promise.allSettled([
    chapterStore.fetchByProject(id),
    characterStore.fetchByProject(id),
    loadWorldSettingsData(id),
  ])
}

async function attachSelectedWorldSettings() {
  if (!projectId.value || !selectedLibraryWorldSettingIds.value.length) {
    return
  }

  associationLoading.value = true
  try {
    for (const worldSettingId of selectedLibraryWorldSettingIds.value) {
      await attachWorldSettingToProject(worldSettingId, projectId.value)
    }
    selectedLibraryWorldSettingIds.value = []
    await loadWorldSettingsData(projectId.value)
  } finally {
    associationLoading.value = false
  }
}

async function detachWorldSetting(item: WorldSetting) {
  if (!projectId.value) {
    return
  }

  associationLoading.value = true
  try {
    await detachWorldSettingFromProject(item.id, projectId.value)
    await loadWorldSettingsData(projectId.value)
  } finally {
    associationLoading.value = false
  }
}

watch(
  projectId,
  async (id) => {
    if (!id || Number.isNaN(id)) {
      return
    }

    if (!projectStore.projects.length) {
      await projectStore.fetchProjects().catch(() => undefined)
    }

    projectStore.setCurrentProject(id)
    await loadProjectData(id)
  },
  { immediate: true },
)
</script>

<template>
  <PageContainer
    :title="project?.name || '项目详情'"
    description="在单页中快速查看项目的基础信息、世界观设定、章节和人物。这里也可以直接把已有世界观关联到当前项目，不必重复创建。"
  >
    <v-card class="soft-panel">
      <v-tabs v-model="tab" color="primary">
        <v-tab value="overview">基本信息</v-tab>
        <v-tab value="world">世界观</v-tab>
        <v-tab value="characters">人物</v-tab>
        <v-tab value="chapters">章节</v-tab>
      </v-tabs>

      <v-window v-model="tab">
        <v-window-item value="overview">
          <v-card-text class="pa-6">
            <div class="text-h5 font-weight-bold">{{ project?.name || '未找到项目' }}</div>
            <div class="mt-3">
              <MarkdownContent :source="project?.description" empty-text="暂无项目说明。" />
            </div>

            <v-row class="mt-4">
              <v-col cols="12" md="3">
                <v-sheet class="soft-panel rounded-xl pa-4">
                  <div class="text-overline text-medium-emphasis">题材</div>
                  <div class="text-h6 mt-2">{{ project?.genre || '未设置' }}</div>
                </v-sheet>
              </v-col>
              <v-col cols="12" md="3">
                <v-sheet class="soft-panel rounded-xl pa-4">
                  <div class="text-overline text-medium-emphasis">世界观数</div>
                  <div class="text-h6 mt-2">{{ worldSettings.length }}</div>
                </v-sheet>
              </v-col>
              <v-col cols="12" md="3">
                <v-sheet class="soft-panel rounded-xl pa-4">
                  <div class="text-overline text-medium-emphasis">章节数</div>
                  <div class="text-h6 mt-2">{{ chapterStore.chapters.length }}</div>
                </v-sheet>
              </v-col>
              <v-col cols="12" md="3">
                <v-sheet class="soft-panel rounded-xl pa-4">
                  <div class="text-overline text-medium-emphasis">人物数</div>
                  <div class="text-h6 mt-2">{{ characterStore.characters.length }}</div>
                </v-sheet>
              </v-col>
            </v-row>

            <div class="mt-6">
              <div class="text-subtitle-1 font-weight-medium">已关联世界观</div>
              <div class="d-flex flex-wrap ga-2 mt-3">
                <v-chip
                  v-for="name in project?.worldSettingNames || []"
                  :key="name"
                  color="secondary"
                  variant="tonal"
                >
                  {{ name }}
                </v-chip>
                <span
                  v-if="!project?.worldSettingNames?.length"
                  class="text-body-2 text-medium-emphasis"
                >
                  当前还没有关联世界观，可在“世界观”页签中直接添加已有设定。
                </span>
              </div>
            </div>
          </v-card-text>
        </v-window-item>

        <v-window-item value="world">
          <v-card-text class="pa-6">
            <v-row class="mb-4">
              <v-col cols="12" md="8">
                <v-autocomplete
                  v-model="selectedLibraryWorldSettingIds"
                  :items="libraryCandidates"
                  item-title="title"
                  item-value="value"
                  label="关联已有世界观"
                  multiple
                  chips
                  closable-chips
                  clearable
                  hint="可以直接从你的世界观库里把已有设定挂到当前项目，无需重新创建。"
                  persistent-hint
                  no-data-text="没有可关联的已有世界观"
                />
              </v-col>
              <v-col cols="12" md="4" class="d-flex flex-column justify-end ga-2">
                <v-btn
                  block
                  color="primary"
                  :disabled="!selectedLibraryWorldSettingIds.length"
                  :loading="associationLoading"
                  @click="attachSelectedWorldSettings"
                >
                  关联所选世界观
                </v-btn>
                <v-btn
                  block
                  variant="outlined"
                  @click="router.push('/world-settings')"
                >
                  打开世界观管理
                </v-btn>
              </v-col>
            </v-row>

            <EmptyState
              v-if="!worldSettings.length"
              title="还没有关联世界观"
              description="当前项目还没有挂载世界规则、地点、势力或历史设定。你可以直接从上方关联已有世界观，或者去世界观管理里新建。"
            />

            <v-row v-else>
              <v-col
                v-for="item in worldSettings"
                :key="item.id"
                cols="12"
                md="6"
                xl="4"
              >
                <v-card class="h-100 border-sm">
                  <v-card-text class="d-flex flex-column h-100">
                    <div class="d-flex justify-space-between align-start ga-3">
                      <div>
                        <div class="text-subtitle-1 font-weight-bold">
                          {{ getWorldSettingTitle(item) }}
                        </div>
                        <div class="text-body-2 text-medium-emphasis mt-1">
                          {{ item.category || '未分类' }}
                        </div>
                      </div>
                      <v-chip size="small" variant="tonal" color="secondary">
                        已被 {{ item.associationCount || 1 }} 个项目使用
                      </v-chip>
                    </div>

                    <div class="mt-4">
                      <MarkdownContent :source="getWorldSettingDescription(item)" empty-text="暂无设定内容" compact />
                    </div>

                    <div class="d-flex justify-space-between align-center mt-auto pt-4">
                      <span class="text-caption text-medium-emphasis">
                        可复用世界观模型
                      </span>
                      <v-btn
                        color="warning"
                        variant="text"
                        :loading="associationLoading"
                        @click="detachWorldSetting(item)"
                      >
                        取消关联
                      </v-btn>
                    </div>
                  </v-card-text>
                </v-card>
              </v-col>
            </v-row>
          </v-card-text>
        </v-window-item>

        <v-window-item value="characters">
          <v-card-text class="pa-6">
            <EmptyState
              v-if="!characterStore.characters.length"
              title="还没有人物"
              description="先去人物管理里创建主角、配角或阵营角色，这里会自动同步展示。"
            />

            <v-list v-else lines="two">
              <v-list-item
                v-for="character in characterStore.characters"
                :key="character.id"
                :title="character.name"
              >
                <template #subtitle>
                  <MarkdownContent :source="character.description" empty-text="暂无角色描述" compact />
                </template>
              </v-list-item>
            </v-list>
          </v-card-text>
        </v-window-item>

        <v-window-item value="chapters">
          <v-card-text class="pa-6">
            <EmptyState
              v-if="!chapterStore.chapters.length"
              title="还没有章节"
              description="章节列表为空时这里会保持稳定显示。创建第一章后，这里会自动列出标题和正文摘要。"
            />

            <v-list v-else lines="two">
              <v-list-item
                v-for="chapter in chapterStore.chapters"
                :key="chapter.id"
                :title="chapter.title"
              >
                <template #subtitle>
                  <MarkdownContent :source="chapter.content" empty-text="暂无章节正文" compact />
                </template>
              </v-list-item>
            </v-list>
          </v-card-text>
        </v-window-item>
      </v-window>
    </v-card>
  </PageContainer>
</template>
