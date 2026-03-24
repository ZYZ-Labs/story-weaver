<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'

import PageContainer from '@/components/PageContainer.vue'
import StatCard from '@/components/StatCard.vue'
import { useCausalityStore } from '@/stores/causality'
import { useCharacterStore } from '@/stores/character'
import { useChapterStore } from '@/stores/chapter'
import { useProjectStore } from '@/stores/project'
import { useRagStore } from '@/stores/rag'

const projectStore = useProjectStore()
const chapterStore = useChapterStore()
const characterStore = useCharacterStore()
const causalityStore = useCausalityStore()
const ragStore = useRagStore()

const stats = computed(() => [
  {
    title: '项目总数',
    value: projectStore.projects.length,
    subtitle: '所有创作项目',
    icon: 'mdi-bookshelf',
  },
  {
    title: '章节数量',
    value: chapterStore.chapters.length,
    subtitle: '当前项目已录入章节',
    icon: 'mdi-file-document-edit-outline',
    color: 'secondary',
  },
  {
    title: '人物数量',
    value: characterStore.characters.length,
    subtitle: '角色设定与关系节点',
    icon: 'mdi-account-group-outline',
    color: 'accent',
  },
  {
    title: '因果节点',
    value: causalityStore.nodes.length,
    subtitle: `知识切片 ${ragStore.knowledgeStats.chunks}`,
    icon: 'mdi-graph-outline',
    color: 'warning',
  },
])

watch(
  () => projectStore.selectedProjectId,
  async (projectId) => {
    if (!projectId) return
    await Promise.allSettled([
      chapterStore.fetchByProject(projectId),
      characterStore.fetchByProject(projectId),
      causalityStore.fetchByProject(projectId),
      ragStore.fetchByProject(projectId),
    ])
  },
  { immediate: true },
)

onMounted(async () => {
  if (!projectStore.projects.length) {
    await projectStore.fetchProjects().catch(() => undefined)
  }
})
</script>

<template>
  <PageContainer
    title="总览"
    description="用一个总览面板盯住创作主链路：项目、章节、角色、因果与知识回流。"
  >
    <div class="stats-grid">
      <StatCard
        v-for="item in stats"
        :key="item.title"
        :title="item.title"
        :value="item.value"
        :subtitle="item.subtitle"
        :icon="item.icon"
        :color="item.color"
      />
    </div>

    <div class="content-grid two-column">
      <v-card class="soft-panel">
        <v-card-title>最近创作动态</v-card-title>
        <v-list lines="three">
          <v-list-item title="写作中心" subtitle="可以从章节正文直接发起续写、扩写、润色或改写。" />
          <v-list-item title="知识回流" subtitle="采纳后的章节内容可进入知识切片流程，为后续生成提供上下文。" />
          <v-list-item title="因果管理" subtitle="当前支持把章节、剧情、人物、知识和 AI 草稿串成因果关系。" />
        </v-list>
      </v-card>

      <v-card class="soft-panel">
        <v-card-title>当前项目聚焦</v-card-title>
        <v-card-text>
          <div class="text-h6">
            {{ projectStore.currentProject?.name || '尚未选择项目' }}
          </div>
          <div class="text-body-2 text-medium-emphasis mt-2">
            {{ projectStore.currentProject?.description || '请先创建或选择项目，后续章节与角色模块会自动联动。' }}
          </div>
          <v-divider class="my-4" />
          <div class="d-flex justify-space-between text-body-2">
            <span>知识条目数</span>
            <strong>{{ ragStore.knowledgeStats.documents }}</strong>
          </div>
          <div class="d-flex justify-space-between text-body-2 mt-3">
            <span>索引完成度</span>
            <strong>{{ ragStore.knowledgeStats.indexed }}</strong>
          </div>
        </v-card-text>
      </v-card>
    </div>
  </PageContainer>
</template>
