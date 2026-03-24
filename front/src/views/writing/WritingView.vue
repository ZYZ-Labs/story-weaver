<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import EmptyState from '@/components/EmptyState.vue'
import PageContainer from '@/components/PageContainer.vue'
import { useChapterStore } from '@/stores/chapter'
import { useProjectStore } from '@/stores/project'
import { useWritingStore } from '@/stores/writing'
import { formatDateTime } from '@/utils/format'

const projectStore = useProjectStore()
const chapterStore = useChapterStore()
const writingStore = useWritingStore()
const generating = ref(false)
const errorMessage = ref('')

const projectId = computed(() => projectStore.selectedProjectId)
const chapterId = computed(() => chapterStore.currentChapter?.id || null)

const draftForm = reactive({
  writingType: 'continue',
  userInstruction: '',
  maxTokens: 600,
})

watch(
  projectId,
  async (id) => {
    if (id) {
      await chapterStore.fetchByProject(id).catch(() => undefined)
    }
  },
  { immediate: true },
)

watch(
  chapterId,
  async (id) => {
    if (id) {
      await writingStore.fetchByChapter(id).catch(() => undefined)
    }
  },
  { immediate: true },
)

onMounted(() => {
  if (!projectStore.projects.length) {
    projectStore.fetchProjects().catch(() => undefined)
  }
})

async function generate() {
  if (!chapterStore.currentChapter?.id) {
    return
  }

  generating.value = true
  errorMessage.value = ''
  try {
    await writingStore.generate({
      chapterId: chapterStore.currentChapter.id,
      currentContent: chapterStore.currentChapter.content || '',
      userInstruction: draftForm.userInstruction,
      writingType: draftForm.writingType,
      maxTokens: draftForm.maxTokens,
    })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'AI 生成失败'
  } finally {
    generating.value = false
  }
}
</script>

<template>
  <PageContainer
    title="写作中心"
    description="按照规格里的三栏思路先做成双栏工作台，已支持选章、编辑正文、发起 AI 续写并查看生成记录。"
  >
    <EmptyState
      v-if="!projectId"
      title="需要先选择项目"
      description="写作中心依赖当前项目和章节上下文，请先在左侧项目选择器中选中一个项目。"
    />

    <div v-else class="content-grid two-column">
      <v-card class="soft-panel">
        <v-card-title>正文编辑</v-card-title>
        <v-card-text>
          <v-select
            label="当前章节"
            item-title="title"
            item-value="id"
            :items="chapterStore.chapters"
            :model-value="chapterId"
            @update:model-value="
              (id) => {
                const target = chapterStore.chapters.find((item) => item.id === id)
                if (target) chapterStore.currentChapter = target
              }
            "
          />
          <v-textarea
            v-model="chapterStore.currentChapter!.content"
            label="章节正文"
            rows="18"
            class="mt-4"
            :disabled="!chapterStore.currentChapter"
          />
          <div class="d-flex justify-space-between align-center mt-4">
            <div class="text-body-2 text-medium-emphasis">
              章节字数：{{ chapterStore.currentChapter?.wordCount || chapterStore.currentChapter?.content?.length || 0 }}
            </div>
            <v-btn
              color="primary"
              variant="outlined"
              :disabled="!projectId || !chapterStore.currentChapter?.id"
              @click="
                chapterStore.update(projectId!, chapterStore.currentChapter!.id, {
                  content: chapterStore.currentChapter?.content,
                  title: chapterStore.currentChapter?.title,
                  orderNum: chapterStore.currentChapter?.orderNum,
                })
              "
            >
              保存正文
            </v-btn>
          </div>
        </v-card-text>
      </v-card>

      <div class="content-grid">
        <v-card class="soft-panel">
          <v-card-title>AI 操作</v-card-title>
          <v-card-text>
            <v-select
              v-model="draftForm.writingType"
              label="任务类型"
              :items="[
                { title: '续写', value: 'continue' },
                { title: '润色', value: 'polish' },
                { title: '扩写', value: 'expand' },
                { title: '改写', value: 'rewrite' },
              ]"
              item-title="title"
              item-value="value"
            />
            <v-text-field v-model="draftForm.maxTokens" label="最大 Tokens" type="number" class="mt-4" />
            <v-textarea
              v-model="draftForm.userInstruction"
              rows="5"
              label="附加指令"
              class="mt-4"
              placeholder="例如：保持第一人称口吻，把冲突推进到新的反转点。"
            />
            <v-alert v-if="errorMessage" type="error" variant="tonal" class="mt-4">
              {{ errorMessage }}
            </v-alert>
            <v-btn
              block
              size="large"
              color="primary"
              class="mt-4"
              :loading="generating"
              :disabled="!chapterStore.currentChapter?.id"
              @click="generate"
            >
              发起 AI 生成
            </v-btn>
          </v-card-text>
        </v-card>

        <v-card class="soft-panel">
          <v-card-title>最近生成记录</v-card-title>
          <v-list v-if="writingStore.records.length" lines="three">
            <v-list-item
              v-for="record in writingStore.records"
              :key="record.id"
              :title="`${record.writingType} · ${record.status || 'draft'}`"
              :subtitle="record.generatedContent"
            >
              <template #append>
                <div class="d-flex flex-column align-end ga-2">
                  <span class="text-caption text-medium-emphasis">{{ formatDateTime(record.createTime) }}</span>
                  <div class="d-flex ga-2">
                    <v-btn size="small" color="primary" variant="text" @click="writingStore.accept(record.id)">
                      采纳
                    </v-btn>
                    <v-btn size="small" color="error" variant="text" @click="writingStore.reject(record.id)">
                      拒绝
                    </v-btn>
                  </div>
                </div>
              </template>
            </v-list-item>
          </v-list>
          <v-card-text v-else class="text-medium-emphasis">还没有 AI 草稿记录，先试一次生成。</v-card-text>
        </v-card>
      </div>
    </div>
  </PageContainer>
</template>
