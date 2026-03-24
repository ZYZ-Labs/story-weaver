<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import EmptyState from '@/components/EmptyState.vue'
import PageContainer from '@/components/PageContainer.vue'
import { getWorldSettings } from '@/api/world-setting'
import { useCharacterStore } from '@/stores/character'
import { useChapterStore } from '@/stores/chapter'
import { useProjectStore } from '@/stores/project'
import type { WorldSetting } from '@/types'

const route = useRoute()
const projectStore = useProjectStore()
const chapterStore = useChapterStore()
const characterStore = useCharacterStore()
const worldSettings = ref<WorldSetting[]>([])
const tab = ref('overview')

const projectId = computed(() => Number(route.params.id))
const project = computed(() => projectStore.projects.find((item) => item.id === projectId.value) || null)

function getWorldSettingTitle(item: WorldSetting) {
  return item.name || item.title || item.category || '未命名设定'
}

function getWorldSettingDescription(item: WorldSetting) {
  return item.description || item.content || '暂无设定内容'
}

onMounted(async () => {
  if (!projectStore.projects.length) {
    await projectStore.fetchProjects().catch(() => undefined)
  }

  projectStore.setCurrentProject(projectId.value)

  await Promise.allSettled([
    chapterStore.fetchByProject(projectId.value),
    characterStore.fetchByProject(projectId.value),
    getWorldSettings(projectId.value).then((data) => {
      worldSettings.value = data
    }),
  ])
})
</script>

<template>
  <PageContainer
    :title="project?.name || '项目详情'"
    description="在单页中快速查看项目的基础信息、世界设定、章节和人物。即使当前还没有任何内容，这里也会明确提示下一步该做什么。"
  >
    <v-card class="soft-panel">
      <v-tabs v-model="tab" color="primary">
        <v-tab value="overview">基本信息</v-tab>
        <v-tab value="world">世界设定</v-tab>
        <v-tab value="characters">人物</v-tab>
        <v-tab value="chapters">章节</v-tab>
      </v-tabs>

      <v-window v-model="tab">
        <v-window-item value="overview">
          <v-card-text class="pa-6">
            <div class="text-h5 font-weight-bold">{{ project?.name || '未找到项目' }}</div>
            <div class="text-body-1 text-medium-emphasis mt-3">
              {{ project?.description || '暂无项目说明。' }}
            </div>

            <v-row class="mt-4">
              <v-col cols="12" md="4">
                <v-sheet class="soft-panel rounded-xl pa-4">
                  <div class="text-overline text-medium-emphasis">题材</div>
                  <div class="text-h6 mt-2">{{ project?.genre || '未设置' }}</div>
                </v-sheet>
              </v-col>
              <v-col cols="12" md="4">
                <v-sheet class="soft-panel rounded-xl pa-4">
                  <div class="text-overline text-medium-emphasis">章节数</div>
                  <div class="text-h6 mt-2">{{ chapterStore.chapters.length }}</div>
                </v-sheet>
              </v-col>
              <v-col cols="12" md="4">
                <v-sheet class="soft-panel rounded-xl pa-4">
                  <div class="text-overline text-medium-emphasis">人物数</div>
                  <div class="text-h6 mt-2">{{ characterStore.characters.length }}</div>
                </v-sheet>
              </v-col>
            </v-row>
          </v-card-text>
        </v-window-item>

        <v-window-item value="world">
          <v-card-text class="pa-6">
            <EmptyState
              v-if="!worldSettings.length"
              title="还没有世界设定"
              description="当前项目还没有录入世界观、阵营、地点或规则设定，后续补充后这里会自动展示。"
            />

            <v-timeline v-else density="compact">
              <v-timeline-item
                v-for="item in worldSettings"
                :key="item.id"
                dot-color="primary"
                size="small"
              >
                <div class="text-subtitle-1 font-weight-bold">{{ getWorldSettingTitle(item) }}</div>
                <div class="text-body-2 text-medium-emphasis mt-1">
                  {{ getWorldSettingDescription(item) }}
                </div>
              </v-timeline-item>
            </v-timeline>
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
                :subtitle="character.description || '暂无角色描述'"
              />
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
                :subtitle="chapter.content || '暂无章节正文'"
              />
            </v-list>
          </v-card-text>
        </v-window-item>
      </v-window>
    </v-card>
  </PageContainer>
</template>
