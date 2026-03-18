<template>
  <v-card>
    <v-card-title>项目列表</v-card-title>
    <v-card-text>
      <v-btn color="primary" size="small" class="mb-3" :loading="loading" @click="loadProjects">刷新</v-btn>
      <v-alert v-if="errorMessage" class="mb-3" type="error" variant="tonal">{{ errorMessage }}</v-alert>
      <v-list>
        <v-list-item
          v-for="project in projects"
          :key="project.id"
          :title="project.name"
          :subtitle="project.description || '暂无描述'"
        />
      </v-list>
      <v-alert v-if="!loading && projects.length === 0 && !errorMessage" type="info" variant="tonal">暂无项目</v-alert>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import apiClient from '../services/api'

interface ProjectItem {
  id: number
  name: string
  description?: string
}

const projects = ref<ProjectItem[]>([])
const loading = ref(false)
const errorMessage = ref('')

const loadProjects = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await apiClient.get('/projects')
    projects.value = data?.data || []
  } catch (error: any) {
    errorMessage.value = error?.response?.data?.message || error?.message || '加载项目失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadProjects)
</script>
