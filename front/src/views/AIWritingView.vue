<template>
  <v-card>
    <v-card-title>AI 写作入口</v-card-title>
    <v-card-text>
      <v-text-field v-model.number="chapterId" type="number" label="章节ID" class="mb-2" />
      <v-textarea v-model="currentContent" label="当前内容" rows="4" class="mb-2" />
      <v-textarea v-model="prompt" label="请输入续写提示词" rows="3" class="mb-2" />
      <v-btn color="primary" :loading="loading" @click="submitPrompt">开始生成</v-btn>
      <v-alert v-if="message" class="mt-4" type="info" variant="tonal">{{ message }}</v-alert>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import apiClient from '../services/api'

const chapterId = ref(1)
const currentContent = ref('')
const prompt = ref('')
const message = ref('')
const loading = ref(false)

const submitPrompt = async () => {
  if (!prompt.value.trim()) return

  loading.value = true
  message.value = ''
  try {
    const { data } = await apiClient.post('/ai-writing/generate', {
      chapterId: chapterId.value,
      currentContent: currentContent.value,
      writingType: 'continue',
      userInstruction: prompt.value,
      maxTokens: 500,
    })

    message.value = data?.generatedContent || data?.data?.generatedContent || '生成成功'
  } catch (error: any) {
    message.value = error?.response?.data?.message || error?.message || '生成失败'
  } finally {
    loading.value = false
  }
}
</script>
