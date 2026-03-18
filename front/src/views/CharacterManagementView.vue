<template>
  <v-card>
    <v-card-title>角色管理</v-card-title>
    <v-card-text>
      <div class="d-flex ga-3 align-center mb-3">
        <v-text-field v-model.number="projectId" type="number" label="项目ID" density="compact" hide-details style="max-width: 180px" />
        <v-btn color="primary" size="small" :loading="loading" @click="loadCharacters">加载角色</v-btn>
      </div>
      <v-alert v-if="errorMessage" class="mb-3" type="error" variant="tonal">{{ errorMessage }}</v-alert>
      <v-chip-group>
        <v-chip v-for="character in characters" :key="character.id" color="secondary" class="mr-2">{{ character.name }}</v-chip>
      </v-chip-group>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import apiClient from '../services/api'

interface CharacterItem {
  id: number
  name: string
}

const projectId = ref(1)
const characters = ref<CharacterItem[]>([])
const loading = ref(false)
const errorMessage = ref('')

const loadCharacters = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await apiClient.get(`/projects/${projectId.value}/characters`)
    characters.value = data?.data || []
  } catch (error: any) {
    errorMessage.value = error?.response?.data?.message || error?.message || '加载角色失败'
  } finally {
    loading.value = false
  }
}
</script>
