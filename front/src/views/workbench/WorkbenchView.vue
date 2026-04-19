<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import EmptyState from '@/components/EmptyState.vue'
import PageContainer from '@/components/PageContainer.vue'
import StatCard from '@/components/StatCard.vue'
import { getProjectBrief, getRecentStoryProgress } from '@/api/story-context'
import { getChapterExecutionReview, getChapterSkeletonPreview } from '@/api/story-orchestration'
import { useProjectWorkspace } from '@/composables/useProjectWorkspace'
import type { ChapterExecutionReviewView, ChapterSkeletonView, ProjectBriefView, RecentStoryProgressView } from '@/types'

const {
  projectStore,
  chapterStore,
  characterStore,
  worldSettingStore,
  currentProjectId,
  activeChapterId,
  activeChapter,
  chapterOptions,
  selectChapter,
} = useProjectWorkspace()

const loading = ref(false)
const projectBrief = ref<ProjectBriefView | null>(null)
const recentProgress = ref<RecentStoryProgressView | null>(null)
const chapterSkeleton = ref<ChapterSkeletonView | null>(null)
const chapterReview = ref<ChapterExecutionReviewView | null>(null)

const stats = computed(() => [
  {
    title: '当前章节',
    value: chapterStore.chapters.length,
    subtitle: '当前项目纳入工作区的章节数',
    icon: 'mdi-file-document-multiple-outline',
  },
  {
    title: '人物',
    value: characterStore.characters.length,
    subtitle: '已进入项目上下文的人物数',
    icon: 'mdi-account-group-outline',
    color: 'secondary',
  },
  {
    title: '世界观',
    value: worldSettingStore.items.length,
    subtitle: '当前项目已关联的设定数',
    icon: 'mdi-earth',
    color: 'accent',
  },
  {
    title: '待推进镜头',
    value: chapterReview.value?.traceSummary?.pendingSceneCount ?? 0,
    subtitle: '当前章节骨架里仍未执行的镜头数',
    icon: 'mdi-timeline-clock-outline',
    color: 'warning',
  },
])

const progressItems = computed(() => recentProgress.value?.items || [])
const chapterStatusSummary = computed(() => {
  if (!chapterReview.value) {
    return '当前章节还没有审校结果，先生成骨架再逐镜头推进。'
  }
  return chapterReview.value.summary || '当前章节已经有可读的执行汇总。'
})

async function loadWorkbenchData(projectId: number, chapterId: number | null) {
  loading.value = true
  try {
    const [briefResult, progressResult] = await Promise.allSettled([
      getProjectBrief(projectId),
      getRecentStoryProgress(projectId, 6),
    ])

    projectBrief.value = briefResult.status === 'fulfilled' ? briefResult.value : null
    recentProgress.value = progressResult.status === 'fulfilled' ? progressResult.value : null

    if (!chapterId) {
      chapterSkeleton.value = null
      chapterReview.value = null
      return
    }

    const [skeletonResult, reviewResult] = await Promise.allSettled([
      getChapterSkeletonPreview(projectId, chapterId),
      getChapterExecutionReview(projectId, chapterId),
    ])
    chapterSkeleton.value = skeletonResult.status === 'fulfilled' ? skeletonResult.value : null
    chapterReview.value = reviewResult.status === 'fulfilled' ? reviewResult.value : null
  } finally {
    loading.value = false
  }
}

watch(
  [currentProjectId, activeChapterId],
  async ([projectId, chapterId]) => {
    if (!projectId) {
      projectBrief.value = null
      recentProgress.value = null
      chapterSkeleton.value = null
      chapterReview.value = null
      return
    }
    await loadWorkbenchData(projectId, chapterId)
  },
  { immediate: true },
)
</script>

<template>
  <PageContainer
    title="创作台"
    description="主入口先服务当前项目和当前章节。普通作者先看当前该写什么，不先被对象列表和字段墙打断。"
  >
    <template #actions>
      <div class="d-flex flex-wrap ga-2 align-center">
        <v-select
          v-if="chapterOptions.length"
          hide-details
          density="comfortable"
          variant="outlined"
          min-width="280"
          :items="chapterOptions"
          :model-value="activeChapterId"
          label="当前章节"
          @update:model-value="selectChapter"
        />
        <v-btn color="primary" prepend-icon="mdi-file-document-plus-outline" to="/chapters">
          说想法新增章节
        </v-btn>
        <v-btn variant="outlined" prepend-icon="mdi-account-plus-outline" to="/characters">
          说想法新增人物
        </v-btn>
        <v-btn variant="outlined" prepend-icon="mdi-earth-plus" to="/world-settings">
          说想法新增世界观
        </v-btn>
      </div>
    </template>

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

    <EmptyState
      v-if="!projectStore.currentProject"
      title="先选择一个项目"
      description="创作台只围绕当前项目展开。先在左侧切换项目，后续章节、状态和生成面板会自动跟上。"
      icon="mdi-bookshelf"
    />

    <div v-else class="panel-grid three-column">
      <v-card class="soft-panel">
        <v-card-title>当前项目简报</v-card-title>
        <v-card-text>
          <div class="text-h5 font-weight-bold">
            {{ projectBrief?.projectTitle || projectStore.currentProject.name }}
          </div>
          <div class="text-body-2 text-medium-emphasis mt-2">
            {{ projectBrief?.logline || '还没有项目 logline，建议先用摘要优先流程补一版项目摘要。' }}
          </div>
          <div class="text-body-1 mt-4">
            {{ projectBrief?.summary || projectStore.currentProject.description || '当前项目还没有稳定的项目级摘要。' }}
          </div>

          <v-divider class="my-4" />

          <div class="text-subtitle-2 font-weight-medium">当前章节焦点</div>
          <div class="text-h6 mt-2">
            {{ activeChapter?.title || '还没有章节' }}
          </div>
          <div class="text-body-2 text-medium-emphasis mt-2">
            {{ activeChapter?.summary || '当前章节还没有摘要，建议优先补章节摘要，再推进骨架和镜头执行。' }}
          </div>
          <div class="d-flex flex-wrap ga-2 mt-4">
            <v-chip color="secondary" variant="tonal">
              {{ activeChapter?.chapterStatus || 'draft' }}
            </v-chip>
            <v-chip color="primary" variant="tonal">
              字数 {{ activeChapter?.wordCount || 0 }}
            </v-chip>
            <v-chip v-if="chapterReview" color="warning" variant="tonal">
              审校 {{ chapterReview.result }}
            </v-chip>
          </div>
          <div class="text-body-2 text-medium-emphasis mt-4">
            {{ chapterStatusSummary }}
          </div>
        </v-card-text>
      </v-card>

      <v-card class="soft-panel">
        <v-card-title>下一步动作</v-card-title>
        <v-card-text class="action-stack">
          <v-btn block color="primary" prepend-icon="mdi-feather" to="/writing">
            打开写作中心
          </v-btn>
          <v-btn block variant="outlined" prepend-icon="mdi-file-tree-outline" to="/chapters">
            进入章节列表
          </v-btn>
          <v-btn block variant="outlined" prepend-icon="mdi-bookshelf" :to="`/projects/${projectStore.currentProject.id}`">
            查看项目详情
          </v-btn>
          <v-alert
            type="info"
            variant="tonal"
            border="start"
            density="comfortable"
          >
            当前入口策略是：先决定这章写什么，再下沉到人物、世界观和状态细节。
          </v-alert>
        </v-card-text>
      </v-card>

      <v-card class="soft-panel">
        <v-card-title>最近进度</v-card-title>
        <v-card-text>
          <v-list v-if="progressItems.length" lines="three">
            <v-list-item
              v-for="item in progressItems"
              :key="`${item.itemType}-${item.refId}-${item.createTime}`"
              :title="item.title || item.itemType"
              :subtitle="item.summary || item.status"
            >
              <template #append>
                <v-chip size="small" variant="outlined">{{ item.status || item.itemType }}</v-chip>
              </template>
            </v-list-item>
          </v-list>
          <EmptyState
            v-else
            title="最近还没有新的进度项"
            description="等章节推进、摘要确认或镜头执行之后，这里会开始显示真正的故事推进轨迹。"
            icon="mdi-timeline-text-outline"
          />
        </v-card-text>
      </v-card>
    </div>

    <div v-if="projectStore.currentProject" class="panel-grid two-column">
      <v-card class="soft-panel">
        <v-card-title>章节骨架预览</v-card-title>
        <v-card-text>
          <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-4" />
          <EmptyState
            v-if="!chapterSkeleton?.scenes?.length"
            title="当前章节还没有骨架"
            description="先补章节摘要或进入生成台查看章节骨架。"
            icon="mdi-timeline-plus-outline"
          />
          <div v-else class="scene-list">
            <div
              v-for="scene in chapterSkeleton.scenes"
              :key="scene.sceneId"
              class="scene-list-item"
            >
              <div class="scene-list-item__header">
                <div class="text-subtitle-1 font-weight-bold scene-list-item__title">
                  {{ scene.sceneId }}
                </div>
                <v-chip size="small" color="secondary" variant="tonal" class="scene-list-item__metric">
                  {{ scene.status }}
                </v-chip>
              </div>
              <div class="text-body-2 mt-2">{{ scene.goal || '当前镜头还没有明确 goal。' }}</div>
              <div class="chip-stack mt-3">
                <v-chip
                  v-for="item in scene.readerReveal"
                  :key="`${scene.sceneId}-reveal-${item}`"
                  size="small"
                  variant="outlined"
                >
                  揭晓 · {{ item }}
                </v-chip>
              </div>
              <div class="text-caption text-medium-emphasis mt-3">
                停点：{{ scene.stopCondition || '未定义' }} · 目标字数：{{ scene.targetWords || 0 }}
              </div>
            </div>
          </div>
        </v-card-text>
      </v-card>

      <v-card class="soft-panel">
        <v-card-title>章节执行状态</v-card-title>
        <v-card-text>
          <div class="text-body-1">
            {{ chapterReview?.summary || '当前章节还没有章节级执行审校结果。' }}
          </div>
          <v-divider class="my-4" />
          <div class="d-flex flex-wrap ga-2">
            <v-chip color="primary" variant="tonal">
              已执行 {{ chapterReview?.traceSummary?.executedSceneCount ?? 0 }}
            </v-chip>
            <v-chip color="secondary" variant="tonal">
              已完成 {{ chapterReview?.traceSummary?.completedSceneCount ?? 0 }}
            </v-chip>
            <v-chip color="warning" variant="tonal">
              待推进 {{ chapterReview?.traceSummary?.pendingSceneCount ?? 0 }}
            </v-chip>
          </div>
          <div class="chip-stack mt-4">
            <v-chip
              v-for="item in chapterReview?.traceSummary?.pendingSceneIds || []"
              :key="`pending-${item}`"
              size="small"
              variant="outlined"
            >
              {{ item }}
            </v-chip>
          </div>
          <v-alert
            v-if="chapterReview?.issues?.length"
            class="mt-4"
            type="warning"
            variant="tonal"
          >
            {{ chapterReview.issues[0]?.message || '当前章节还有待处理问题。' }}
          </v-alert>
        </v-card-text>
      </v-card>
    </div>
  </PageContainer>
</template>

<style scoped>
.scene-list-item__header {
  display: flex;
  gap: 12px;
  justify-content: space-between;
  align-items: flex-start;
}

.scene-list-item__title {
  min-width: 0;
  flex: 1 1 auto;
  white-space: normal;
  overflow-wrap: anywhere;
  font-family: 'Roboto Mono', 'SFMono-Regular', Consolas, monospace;
}

.scene-list-item__metric {
  flex: 0 0 auto;
}

@media (max-width: 960px) {
  .scene-list-item__header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
