<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

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
    description="在单页中浏览该项目的世界设定、章节、角色和 AI 相关配置。"
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
            <v-alert v-if="!worldSettings.length" type="info" variant="tonal">
              当前项目还没有世界设定数据，前端接口已经打通，后端补数后这里会直接展示。
            </v-alert>
            <v-timeline v-else density="compact">
              <v-timeline-item
                v-for="item in worldSettings"
                :key="item.id"
                dot-color="primary"
                size="small"
              >
                <div class="text-subtitle-1 font-weight-bold">{{ item.title || item.category || '未命名设定' }}</div>
                <div class="text-body-2 text-medium-emphasis mt-1">{{ item.content || '暂无内容' }}</div>
              </v-timeline-item>
            </v-timeline>
          </v-card-text>
        </v-window-item>

        <v-window-item value="characters">
          <v-list lines="two">
            <v-list-item
              v-for="character in characterStore.characters"
              :key="character.id"
              :title="character.name"
              :subtitle="character.description || '暂无角色描述'"
            />
          </v-list>
        </v-window-item>

        <v-window-item value="chapters">
          <v-list lines="two">
            <v-list-item
              v-for="chapter in chapterStore.chapters"
              :key="chapter.id"
              :title="chapter.title"
              :subtitle="chapter.content || '暂无章节正文'"
            />
          </v-list>
        </v-window-item>
      </v-window>
    </v-card>
  </PageContainer>
</template>
