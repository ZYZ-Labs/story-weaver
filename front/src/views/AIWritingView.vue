<template>
  <v-card>
    <v-card-title>AI 写作入口</v-card-title>
    <v-card-text>
      <v-textarea v-model="prompt" label="请输入续写提示词" rows="4" />
      <v-btn color="primary" @click="submitPrompt">开始生成</v-btn>
      <v-alert v-if="message" class="mt-4" type="info" variant="tonal">{{ message }}</v-alert>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import apiClient from '../services/api'

const prompt = ref('')
const message = ref('')

const submitPrompt = async () => {
  if (!prompt.value.trim()) return
  message.value = `已提交提示词到 ${apiClient.defaults.baseURL || 'API 服务'}，等待响应中...`
}
</script>
