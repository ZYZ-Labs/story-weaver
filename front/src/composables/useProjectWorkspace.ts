import { computed, onMounted, ref, watch } from 'vue'

import { useCharacterStore } from '@/stores/character'
import { useChapterStore } from '@/stores/chapter'
import { useProjectStore } from '@/stores/project'
import { useWorldSettingStore } from '@/stores/world-setting'

export function useProjectWorkspace() {
  const projectStore = useProjectStore()
  const chapterStore = useChapterStore()
  const characterStore = useCharacterStore()
  const worldSettingStore = useWorldSettingStore()

  const activeChapterId = ref<number | null>(null)

  const currentProjectId = computed(() => projectStore.selectedProjectId)
  const activeChapter = computed(
    () =>
      chapterStore.chapters.find((item) => item.id === activeChapterId.value) ||
      chapterStore.currentChapter ||
      null,
  )
  const chapterOptions = computed(() =>
    chapterStore.chapters.map((chapter) => ({
      title: `第 ${chapter.orderNum || '-'} 章 · ${chapter.title}`,
      value: chapter.id,
    })),
  )

  async function ensureProjects() {
    if (!projectStore.projects.length) {
      await projectStore.fetchProjects().catch(() => undefined)
    }
  }

  async function loadProjectContext(projectId: number) {
    await Promise.allSettled([
      chapterStore.fetchByProject(projectId),
      characterStore.fetchByProject(projectId),
      worldSettingStore.fetchByProject(projectId),
    ])

    const nextChapterId = activeChapterId.value && chapterStore.chapters.some((item) => item.id === activeChapterId.value)
      ? activeChapterId.value
      : chapterStore.currentChapter?.id || chapterStore.chapters[0]?.id || null

    activeChapterId.value = nextChapterId

    if (nextChapterId) {
      await chapterStore.fetchDetail(projectId, nextChapterId).catch(() => undefined)
    }
  }

  async function selectChapter(chapterId: number | null) {
    activeChapterId.value = chapterId
    if (currentProjectId.value && chapterId) {
      await chapterStore.fetchDetail(currentProjectId.value, chapterId).catch(() => undefined)
    }
  }

  watch(
    currentProjectId,
    async (projectId) => {
      if (!projectId) {
        activeChapterId.value = null
        return
      }
      await loadProjectContext(projectId)
    },
    { immediate: true },
  )

  onMounted(async () => {
    await ensureProjects()
  })

  return {
    projectStore,
    chapterStore,
    characterStore,
    worldSettingStore,
    currentProjectId,
    activeChapterId,
    activeChapter,
    chapterOptions,
    ensureProjects,
    loadProjectContext,
    selectChapter,
  }
}
