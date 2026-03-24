import { ref } from 'vue'
import { defineStore } from 'pinia'

import * as chapterApi from '@/api/chapter'
import type { Chapter } from '@/types'

function sortChapters(items: Chapter[]) {
  return [...items].sort((left, right) => {
    const leftOrder = left.orderNum ?? Number.MAX_SAFE_INTEGER
    const rightOrder = right.orderNum ?? Number.MAX_SAFE_INTEGER
    if (leftOrder !== rightOrder) {
      return leftOrder - rightOrder
    }
    return (left.id ?? 0) - (right.id ?? 0)
  })
}

export const useChapterStore = defineStore('chapter', () => {
  const chapters = ref<Chapter[]>([])
  const currentChapter = ref<Chapter | null>(null)
  const loading = ref(false)

  async function fetchByProject(projectId: number) {
    loading.value = true
    try {
      chapters.value = sortChapters(await chapterApi.getProjectChapters(projectId))
      currentChapter.value = chapters.value[0] || null
    } catch (error) {
      chapters.value = []
      currentChapter.value = null
      throw error
    } finally {
      loading.value = false
    }
  }

  async function fetchDetail(projectId: number, chapterId: number) {
    const detail = await chapterApi.getChapter(projectId, chapterId)
    currentChapter.value = detail

    const target = chapters.value.find((item) => item.id === chapterId)
    if (target) {
      Object.assign(target, detail)
      chapters.value = sortChapters(chapters.value)
    }

    return currentChapter.value
  }

  async function create(projectId: number, payload: Partial<Chapter>) {
    const chapter = await chapterApi.createChapter(projectId, payload)
    chapters.value = sortChapters([...chapters.value, chapter])
    currentChapter.value = chapter
    return chapter
  }

  async function update(projectId: number, chapterId: number, payload: Partial<Chapter>) {
    await chapterApi.updateChapter(projectId, chapterId, payload)

    const target = chapters.value.find((item) => item.id === chapterId)
    if (target) {
      Object.assign(target, payload)
    }

    if (currentChapter.value?.id === chapterId) {
      Object.assign(currentChapter.value, payload)
    }

    chapters.value = sortChapters(chapters.value)
  }

  async function remove(projectId: number, chapterId: number) {
    const currentIndex = chapters.value.findIndex((item) => item.id === chapterId)
    await chapterApi.deleteChapter(projectId, chapterId)

    chapters.value = chapters.value.filter((item) => item.id !== chapterId)

    if (currentChapter.value?.id === chapterId) {
      currentChapter.value =
        chapters.value[currentIndex] ||
        chapters.value[currentIndex - 1] ||
        chapters.value[0] ||
        null
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
