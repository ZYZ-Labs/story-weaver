import { ref } from 'vue'
import { defineStore } from 'pinia'

import * as characterApi from '@/api/character'
import type { Character } from '@/types'

export const useCharacterStore = defineStore('character', () => {
  const characters = ref<Character[]>([])
  const loading = ref(false)

  async function fetchByProject(projectId: number) {
    loading.value = true
    try {
      characters.value = await characterApi.getProjectCharacters(projectId)
    } finally {
      loading.value = false
    }
  }

  async function create(projectId: number, payload: Partial<Character>) {
    const character = await characterApi.createCharacter(projectId, payload)
    characters.value.unshift(character)
  }

  async function update(projectId: number, characterId: number, payload: Partial<Character>) {
    await characterApi.updateCharacter(projectId, characterId, payload)
    const target = characters.value.find((item) => item.id === characterId)
    if (target) {
      Object.assign(target, payload)
    }
  }

  async function remove(projectId: number, characterId: number) {
    await characterApi.deleteCharacter(projectId, characterId)
    characters.value = characters.value.filter((item) => item.id !== characterId)
  }

  return {
    characters,
    loading,
    fetchByProject,
    create,
    update,
    remove,
  }
})
