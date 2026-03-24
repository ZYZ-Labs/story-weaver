<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const mode = ref<'login' | 'register'>('login')

const form = reactive({
  username: 'admin',
  password: 'Admin@123456',
})

async function submit() {
  loading.value = true
  errorMessage.value = ''

  try {
    if (mode.value === 'login') {
      await authStore.signIn(form)
    } else {
      await authStore.signUp(form)
    }

    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.push(redirect)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败。'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <v-container fluid class="fill-height pa-0">
    <v-row class="fill-height ma-0">
      <v-col cols="12" lg="7" class="d-none d-lg-flex align-center justify-center">
        <div style="max-width: 620px" class="pa-10">
          <div class="text-overline text-secondary">中文长篇创作工作台</div>
          <div class="text-h2 font-weight-bold mt-3 text-primary">
            从世界设定到 AI 续写，把小说创作真正串起来。
          </div>
          <div class="text-h6 text-medium-emphasis mt-5">
            一个面向长篇创作的工作台，覆盖项目、章节、人物、因果、知识库与模型服务。
          </div>
          <v-row class="mt-8" dense>
            <v-col cols="6">
              <v-card class="soft-panel">
                <v-card-text>
                  <div class="text-h5 font-weight-bold">统一上下文</div>
                  <div class="text-body-2 text-medium-emphasis mt-2">
                    项目、章节、人物、因果和 AI 输出共用同一套上下文。
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
            <v-col cols="6">
              <v-card class="soft-panel">
                <v-card-text>
                  <div class="text-h5 font-weight-bold">采纳后入库</div>
                  <div class="text-body-2 text-medium-emphasis mt-2">
                    AI 先生成草稿，再确认写回章节与知识库。
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </div>
      </v-col>
      <v-col cols="12" lg="5" class="d-flex align-center justify-center">
        <v-card width="100%" max-width="520" class="mx-4 soft-panel">
          <v-card-text class="pa-8">
            <div class="text-h4 font-weight-bold">进入织文者 Story Weaver</div>
            <div class="text-body-2 text-medium-emphasis mt-2">
              默认演示账号已预填：
              <code style="background: rgba(30, 77, 120, 0.08); padding: 2px 6px; border-radius: 6px">admin / Admin@123456</code>
            </div>

            <v-btn-toggle
              v-model="mode"
              mandatory
              class="mt-6"
              divided
              color="primary"
              rounded="lg"
            >
              <v-btn value="login">登录</v-btn>
              <v-btn value="register">注册</v-btn>
            </v-btn-toggle>

            <v-form class="mt-6" @submit.prevent="submit">
              <v-text-field v-model="form.username" label="用户名" prepend-inner-icon="mdi-account-outline" />
              <v-text-field
                v-model="form.password"
                label="密码"
                type="password"
                prepend-inner-icon="mdi-lock-outline"
              />

              <div class="text-caption text-medium-emphasis mb-4">
                登录使用已有账号；切换到注册后会直接创建新用户并自动登录。
              </div>

              <v-alert v-if="errorMessage" type="error" variant="tonal" class="mb-4">
                {{ errorMessage }}
              </v-alert>

              <v-btn block size="large" color="primary" :loading="loading" type="submit">
                {{ mode === 'login' ? '登录并进入工作台' : '创建账号' }}
              </v-btn>
            </v-form>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>
