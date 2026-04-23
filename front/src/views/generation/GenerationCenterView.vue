<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import EmptyState from '@/components/EmptyState.vue'
import PageContainer from '@/components/PageContainer.vue'
import StatCard from '@/components/StatCard.vue'
import { getChapterExecutionReview, getChapterSkeletonPreview, getStorySessionPreview } from '@/api/story-orchestration'
import { useProjectWorkspace } from '@/composables/useProjectWorkspace'
import type { ChapterExecutionReviewView, ChapterSkeletonView, StorySessionPreviewView } from '@/types'

const {
  projectStore,
  currentProjectId,
  activeChapterId,
  activeChapter,
  chapterOptions,
  selectChapter,
} = useProjectWorkspace()

const loading = ref(false)
const sceneId = ref('scene-1')
const sessionPreview = ref<StorySessionPreviewView | null>(null)
const chapterSkeleton = ref<ChapterSkeletonView | null>(null)
const chapterReview = ref<ChapterExecutionReviewView | null>(null)

const stats = computed(() => [
  {
    title: '候选数',
    value: sessionPreview.value?.candidates?.length ?? 0,
    subtitle: '当前总导阶段产出的候选镜头数',
    icon: 'mdi-format-list-bulleted',
  },
  {
    title: '骨架镜头',
    value: chapterSkeleton.value?.sceneCount ?? 0,
    subtitle: '当前章节骨架中的镜头总数',
    icon: 'mdi-file-tree-outline',
    color: 'secondary',
  },
  {
    title: '已执行镜头',
    value: chapterReview.value?.traceSummary?.executedSceneCount ?? 0,
    subtitle: '当前章节已进入执行链的镜头数',
    icon: 'mdi-play-circle-outline',
    color: 'accent',
  },
  {
    title: '待收口镜头',
    value: chapterReview.value?.traceSummary?.pendingSceneCount ?? 0,
    subtitle: '当前章节还需要继续推进的镜头数',
    icon: 'mdi-timer-sand-empty',
    color: 'warning',
  },
])

async function loadGenerationData(projectId: number, chapterId: number | null) {
  if (!chapterId) {
    sessionPreview.value = null
    chapterSkeleton.value = null
    chapterReview.value = null
    return
  }

  loading.value = true
  try {
    const [previewResult, skeletonResult, reviewResult] = await Promise.allSettled([
      getStorySessionPreview(projectId, chapterId, sceneId.value),
      getChapterSkeletonPreview(projectId, chapterId),
      getChapterExecutionReview(projectId, chapterId),
    ])

    sessionPreview.value = previewResult.status === 'fulfilled' ? previewResult.value : null
    chapterSkeleton.value = skeletonResult.status === 'fulfilled' ? skeletonResult.value : null
    chapterReview.value = reviewResult.status === 'fulfilled' ? reviewResult.value : null
  } finally {
    loading.value = false
  }
}

watch(
  [currentProjectId, activeChapterId, sceneId],
  async ([projectId, chapterId]) => {
    if (!projectId) {
      sessionPreview.value = null
      chapterSkeleton.value = null
      chapterReview.value = null
      return
    }
    await loadGenerationData(projectId, chapterId)
  },
  { immediate: true },
)
</script>

<template>
  <PageContainer
    title="生成台"
    description="这里直接观察当前章节的骨架、编排、写手 brief 和章节级审校，而不是只看最终正文。"
  >
    <template #actions>
      <div class="generation-actions">
        <v-select
          v-if="chapterOptions.length"
          class="generation-actions__chapter"
          hide-details
          density="comfortable"
          variant="outlined"
          :items="chapterOptions"
          :model-value="activeChapterId"
          label="观察章节"
          @update:model-value="selectChapter"
        />
        <v-text-field
          v-model="sceneId"
          class="generation-actions__scene"
          hide-details
          density="comfortable"
          variant="outlined"
          label="sceneId"
        />
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
      v-if="!projectStore.currentProject || !activeChapter"
      title="先选择一个章节"
      description="生成台围绕当前章节和当前 scene 工作。先选项目，再选章节。"
      icon="mdi-robot-happy-outline"
    />

    <div v-else class="panel-grid two-column">
      <v-card class="soft-panel">
        <v-card-title>多 Session 编排预览</v-card-title>
        <v-card-text>
          <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-4" />
          <div class="generation-meta-row text-body-2 text-medium-emphasis">
            <span>当前 scene：</span>
            <span class="scene-token">{{ sessionPreview?.contextPacket?.sceneId || sceneId }}</span>
            <span>绑定模式：{{ sessionPreview?.contextPacket?.sceneBindingContext?.mode || '未绑定' }}</span>
          </div>
          <div class="generation-meta-row text-body-2 text-medium-emphasis mt-2">
            <span>解析 scene：</span>
            <span class="scene-token">{{ sessionPreview?.contextPacket?.sceneBindingContext?.resolvedSceneId || '未解析' }}</span>
          </div>

          <v-divider class="my-4" />

          <div class="text-subtitle-2 font-weight-medium">候选</div>
          <div class="scene-list mt-3">
            <div
              v-for="candidate in sessionPreview?.candidates || []"
              :key="candidate.id"
              class="scene-list-item"
            >
              <div class="scene-list-item__header">
                <div class="text-subtitle-1 font-weight-bold scene-list-item__title">
                  {{ candidate.title || candidate.type || candidate.id }}
                </div>
                <v-chip size="small" variant="tonal" color="secondary" class="scene-list-item__metric">
                  {{ candidate.targetWords || 0 }} 字
                </v-chip>
              </div>
              <div class="text-caption text-medium-emphasis mt-2">
                {{ candidate.id }} · {{ candidate.type }}
              </div>
              <div class="text-body-2 mt-2">{{ candidate.goal || '当前候选没有 goal。' }}</div>
              <div class="text-caption text-medium-emphasis mt-3">
                停点：{{ candidate.stopCondition || '未定义' }}
              </div>
            </div>
          </div>

          <v-alert class="mt-4" type="info" variant="tonal">
            已选候选：{{ sessionPreview?.selectionDecision?.candidateId || '暂无' }} ·
            {{ sessionPreview?.selectionDecision?.whyChosen || '当前没有选择说明。' }}
          </v-alert>
        </v-card-text>
      </v-card>

      <v-card class="soft-panel">
        <v-card-title>写手 Brief 与章节审校</v-card-title>
        <v-card-text>
          <div class="text-subtitle-2 font-weight-medium">写手 Brief</div>
          <div class="text-body-2 mt-2">
            {{ sessionPreview?.writerExecutionBrief?.goal || '当前还没有写手 goal。' }}
          </div>
          <div class="text-caption text-medium-emphasis mt-3">
            停点：{{ sessionPreview?.writerExecutionBrief?.stopCondition || '未定义' }} ·
            目标字数：{{ sessionPreview?.writerExecutionBrief?.targetWords || 0 }}
          </div>

          <v-divider class="my-4" />

          <div class="text-subtitle-2 font-weight-medium">章节审校</div>
          <div class="text-body-2 mt-2">
            {{ chapterReview?.summary || '当前还没有章节级审校摘要。' }}
          </div>
          <div class="chip-stack mt-3">
            <v-chip color="primary" variant="tonal">
              已执行 {{ chapterReview?.traceSummary?.executedSceneCount ?? 0 }}
            </v-chip>
            <v-chip color="warning" variant="tonal">
              待推进 {{ chapterReview?.traceSummary?.pendingSceneCount ?? 0 }}
            </v-chip>
          </div>

          <div class="text-subtitle-2 font-weight-medium mt-5">Trace</div>
          <v-list lines="two">
            <v-list-item
              v-for="(item, index) in sessionPreview?.trace?.items || []"
              :key="`${item.sessionRole}-${index}`"
              :title="`${item.sessionRole} · ${item.status}`"
              :subtitle="item.message || item.summary || '当前没有补充说明。'"
            >
              <template #append>
                <v-chip size="x-small" variant="outlined">
                  #{{ item.attempt || 1 }}
                </v-chip>
              </template>
            </v-list-item>
          </v-list>
        </v-card-text>
      </v-card>
    </div>
  </PageContainer>
</template>

<style scoped>
.generation-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.scene-token {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  padding: 2px 10px;
  border-radius: 999px;
  background: rgba(30, 77, 120, 0.08);
  color: #1e4d78;
  font-family: 'Roboto Mono', 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

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

.generation-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  width: min(100%, 640px);
}

.generation-actions__chapter {
  flex: 1 1 320px;
  min-width: 280px;
}

.generation-actions__scene {
  flex: 0 0 220px;
  min-width: 180px;
}

@media (max-width: 720px) {
  .generation-actions {
    width: 100%;
  }

  .generation-actions__chapter,
  .generation-actions__scene {
    flex: 1 1 100%;
    min-width: 0;
  }
}
</style>
