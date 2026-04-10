import { ref } from 'vue'
import { defineStore } from 'pinia'

import * as characterApi from '@/api/character'
import type { Character } from '@/types'

export const useCharacterStore = defineStore('character', () => {
  const characters = ref<Character[]>([])
  const library = ref<Character[]>([])
  const loading = ref(false)
  const libraryLoading = ref(false)

  async function fetchByProject(projectId: number) {
    loading.value = true
    try {
      characters.value = await characterApi.getProjectCharacters(projectId)
    } catch (error) {
      characters.value = []
      throw error
    } finally {
      loading.value = false
    }
  }

  async function fetchLibrary() {
    libraryLoading.value = true
    try {
      library.value = await characterApi.getCharacterLibrary()
    } finally {
      libraryLoading.value = false
    }
  }

  async function create(
    projectId: number,
    payload: Partial<Character> & { existingCharacterId?: number; projectRole?: string; roleType?: string },
  ) {
    await characterApi.createCharacter(projectId, payload)
    await Promise.allSettled([fetchByProject(projectId), fetchLibrary()])
  }

  async function update(
    projectId: number,
    characterId: number,
    payload: Partial<Character> & { projectRole?: string; roleType?: string },
  ) {
    await characterApi.updateCharacter(projectId, characterId, payload)
    await Promise.allSettled([fetchByProject(projectId), fetchLibrary()])
  }

  async function remove(projectId: number, characterId: number) {
    await characterApi.deleteCharacter(projectId, characterId)
    await Promise.allSettled([fetchByProject(projectId), fetchLibrary()])
  }

  return {
    characters,
    library,
    loading,
    libraryLoading,
    fetchByProject,
    fetchLibrary,
    create,
    update,
    remove,
  }
})
