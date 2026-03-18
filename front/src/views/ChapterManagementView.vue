<template>
  <v-card>
    <v-card-title>章节管理</v-card-title>
    <v-card-text>
      <div class="d-flex ga-3 align-center mb-3">
        <v-text-field v-model.number="projectId" type="number" label="项目ID" density="compact" hide-details style="max-width: 180px" />
        <v-btn color="primary" size="small" :loading="loading" @click="loadChapters">加载章节</v-btn>
      </div>
      <v-alert v-if="errorMessage" class="mb-3" type="error" variant="tonal">{{ errorMessage }}</v-alert>
      <v-table>
        <thead>
          <tr>
            <th>章节号</th>
            <th>标题</th>
            <th>状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="chapter in chapters" :key="chapter.id">
            <td>{{ chapter.orderNum }}</td>
            <td>{{ chapter.title }}</td>
            <td>{{ chapter.status }}</td>
          </tr>
        </tbody>
      </v-table>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import apiClient from '../services/api'

interface ChapterItem {
  id: number
  title: string
  orderNum: number
  status: number
}

const projectId = ref(1)
const chapters = ref<ChapterItem[]>([])
const loading = ref(false)
const errorMessage = ref('')

const loadChapters = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await apiClient.get(`/projects/${projectId.value}/chapters`)
    chapters.value = data?.data || []
  } catch (error: any) {
    errorMessage.value = error?.response?.data?.message || error?.message || '加载章节失败'
  } finally {
    loading.value = false
  }
}
</script>
