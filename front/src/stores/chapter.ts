import { ref } from 'vue'
import { defineStore } from 'pinia'

import * as chapterApi from '@/api/chapter'
import type { Chapter } from '@/types'

export const useChapterStore = defineStore('chapter', () => {
  const chapters = ref<Chapter[]>([])
  const currentChapter = ref<Chapter | null>(null)
  const loading = ref(false)

  async function fetchByProject(projectId: number) {
    loading.value = true
    try {
      chapters.value = await chapterApi.getProjectChapters(projectId)
      currentChapter.value = chapters.value[0] || null
    } finally {
      loading.value = false
    }
  }

  async function fetchDetail(projectId: number, chapterId: number) {
    currentChapter.value = await chapterApi.getChapter(projectId, chapterId)
    return currentChapter.value
  }

  async function create(projectId: number, payload: Partial<Chapter>) {
    const chapter = await chapterApi.createChapter(projectId, payload)
    chapters.value.push(chapter)
    currentChapter.value = chapter
    return chapter
  }

  async function update(projectId: number, chapterId: number, payload: Partial<Chapter>) {
    await chapterApi.updateChapter(projectId, chapterId, payload)
    const chapter = chapters.value.find((item) => item.id === chapterId)
    if (chapter) {
      Object.assign(chapter, payload)
    }
    if (currentChapter.value?.id === chapterId) {
      Object.assign(currentChapter.value, payload)
    }
  }

  async function remove(projectId: number, chapterId: number) {
    await chapterApi.deleteChapter(projectId, chapterId)
    chapters.value = chapters.value.filter((item) => item.id !== chapterId)
    if (currentChapter.value?.id === chapterId) {
      currentChapter.value = chapters.value[0] || null
    }
  }

  return {
    chapters,
    currentChapter,
    loading,
    fetchByProject,
    fetchDetail,
    create,
    update,
    remove,
  }
})
