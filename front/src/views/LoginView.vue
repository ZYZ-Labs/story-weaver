<template>
  <v-card class="mx-auto" max-width="420">
    <v-card-title>登录</v-card-title>
    <v-card-text>
      <v-text-field v-model="username" label="用户名" />
      <v-text-field v-model="password" label="密码" type="password" />
    </v-card-text>
    <v-card-actions>
      <v-btn color="primary" block @click="handleLogin">登录</v-btn>
    </v-card-actions>
  </v-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '../stores/app'

const appStore = useAppStore()
const router = useRouter()
const username = ref('admin')
const password = ref('')

const handleLogin = () => {
  if (!username.value || !password.value) return
  appStore.login(username.value)
  router.push('/projects')
}
</script>
