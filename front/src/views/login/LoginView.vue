<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import * as authApi from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import type { AuthPublicConfig } from '@/types'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const configLoading = ref(false)
const errorMessage = ref('')
const mode = ref<'login' | 'register'>('login')
const publicConfig = ref<AuthPublicConfig>({
  registrationEnabled: false,
  maxFailedAttempts: 5,
  lockMinutes: 30,
})

const form = reactive({
  username: '',
  password: '',
})

const registrationEnabled = computed(() => publicConfig.value.registrationEnabled)
const policyHint = computed(
  () => `连续输错 ${publicConfig.value.maxFailedAttempts} 次密码后，账号将锁定 ${publicConfig.value.lockMinutes} 分钟。`,
)

async function loadPublicConfig() {
  configLoading.value = true
  try {
    publicConfig.value = await authApi.getPublicConfig()
    if (!publicConfig.value.registrationEnabled) {
      mode.value = 'login'
    }
  } catch {
    mode.value = 'login'
  } finally {
    configLoading.value = false
  }
}

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
    errorMessage.value = error instanceof Error ? error.message : '登录失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadPublicConfig()
})
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
            <div class="text-h4 font-weight-bold">进入 Story Weaver</div>
            <div class="text-body-2 text-medium-emphasis mt-2">
              请使用管理员分配的账号登录，页面不再预填任何默认账号或密码。
            </div>

            <v-alert type="info" variant="tonal" class="mt-6">
              <div>{{ policyHint }}</div>
              <div v-if="!registrationEnabled" class="text-caption mt-2">
                当前已关闭公开注册，请联系管理员在“账号管理”中创建新账号。
              </div>
            </v-alert>

            <v-btn-toggle
              v-if="registrationEnabled"
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

            <v-chip v-else class="mt-6" color="secondary" variant="tonal">
              仅开放登录
            </v-chip>

            <v-form class="mt-6" @submit.prevent="submit">
              <v-text-field
                v-model="form.username"
                label="用户名"
                prepend-inner-icon="mdi-account-outline"
                autocomplete="username"
              />
              <v-text-field
                v-model="form.password"
                label="密码"
                type="password"
                prepend-inner-icon="mdi-lock-outline"
                autocomplete="current-password"
              />

              <div class="text-caption text-medium-emphasis mb-4">
                {{
                  registrationEnabled
                    ? '登录使用已有账号；切换到注册后会直接创建新账号并自动登录。'
                    : '外网环境推荐关闭公开注册，仅保留管理员分配账号。'
                }}
              </div>

              <v-alert v-if="errorMessage" type="error" variant="tonal" class="mb-4">
                {{ errorMessage }}
              </v-alert>

              <v-btn block size="large" color="primary" :loading="loading || configLoading" type="submit">
                {{ mode === 'login' || !registrationEnabled ? '登录并进入工作台' : '创建账号' }}
              </v-btn>
            </v-form>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>
