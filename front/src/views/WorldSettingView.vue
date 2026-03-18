<template>
  <v-card>
    <v-card-title>世界设定</v-card-title>
    <v-card-text>
      <div class="d-flex ga-3 align-center mb-3">
        <v-text-field v-model.number="projectId" type="number" label="项目ID" density="compact" hide-details style="max-width: 180px" />
        <v-btn color="primary" size="small" :loading="loading" @click="loadSettings">加载设定</v-btn>
      </div>
      <v-alert v-if="errorMessage" class="mb-3" type="error" variant="tonal">{{ errorMessage }}</v-alert>
      <v-list>
        <v-list-item v-for="item in settings" :key="item.id" :title="item.name" :subtitle="item.description || '暂无描述'" />
      </v-list>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import apiClient from '../services/api'

interface WorldSettingItem {
  id: number
  name: string
  description?: string
}

const projectId = ref(1)
const settings = ref<WorldSettingItem[]>([])
const loading = ref(false)
const errorMessage = ref('')

const loadSettings = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await apiClient.get(`/world-settings/project/${projectId.value}`)
    settings.value = data || []
  } catch (error: any) {
    errorMessage.value = error?.response?.data?.message || error?.message || '加载世界设定失败'
  } finally {
    loading.value = false
  }
}
</script>
