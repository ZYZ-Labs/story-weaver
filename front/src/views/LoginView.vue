<template>
  <v-card class="mx-auto" max-width="420">
    <v-card-title>登录</v-card-title>
    <v-card-text>
      <v-text-field v-model="username" label="用户名" />
      <v-text-field v-model="password" label="密码" type="password" />
      <v-alert v-if="errorMessage" class="mt-2" type="error" variant="tonal">{{ errorMessage }}</v-alert>
    </v-card-text>
    <v-card-actions>
      <v-btn color="primary" :loading="loading" block @click="handleLogin">登录</v-btn>
    </v-card-actions>
  </v-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '../stores/app'
import apiClient from '../services/api'

const appStore = useStoryweaverStore()
const router = useRouter()
const username = ref('admin')
const password = ref('admin123')
const loading = ref(false)
const errorMessage = ref('')

const handleLogin = async () => {
  if (!username.value || !password.value) return

  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await apiClient.post('/auth/login', {
      username: username.value,
      password: password.value,
    })

    const token = data?.data?.token as string | undefined
    if (!token) {
      throw new Error('登录成功但未返回 token')
    }

    appStore.login(username.value, token)
    router.push('/projects')
  } catch (error: any) {
    errorMessage.value = error?.response?.data?.message || error?.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>
