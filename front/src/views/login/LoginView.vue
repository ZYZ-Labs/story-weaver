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
  <v-container fluid class="fill-height pa-0 login-shell">
    <v-row class="fill-height ma-0">
      <v-col cols="12" lg="7" class="d-none d-lg-flex align-center justify-center login-hero-column">
        <div class="login-hero">
          <div class="login-hero__art" />
          <div class="login-hero__overlay" />
          <div class="login-hero__content">
            <div class="text-overline login-hero__eyebrow">中文长篇创作工作台</div>
            <div class="text-h2 font-weight-bold mt-3 login-hero__title">
              从世界设定到 AI 续写，把小说创作真正串起来。
            </div>
            <div class="text-h6 mt-5 login-hero__subtitle">
              一个面向长篇创作的工作台，覆盖项目、章节、人物、因果、知识库与模型服务。
            </div>
            <v-row class="mt-8" dense>
              <v-col cols="6">
                <v-card class="login-hero__card" rounded="xl">
                  <v-card-text>
                    <div class="text-h5 font-weight-bold">统一上下文</div>
                    <div class="text-body-2 mt-2 login-hero__card-text">
                      项目、章节、人物、因果和 AI 输出共用同一套上下文。
                    </div>
                  </v-card-text>
                </v-card>
              </v-col>
              <v-col cols="6">
                <v-card class="login-hero__card" rounded="xl">
                  <v-card-text>
                    <div class="text-h5 font-weight-bold">采纳后入库</div>
                    <div class="text-body-2 mt-2 login-hero__card-text">
                      AI 先生成草稿，再确认写回章节与知识库。
                    </div>
                  </v-card-text>
                </v-card>
              </v-col>
            </v-row>
          </div>
        </div>
      </v-col>
      <v-col cols="12" lg="5" class="d-flex align-center justify-center login-form-column">
        <v-card width="100%" max-width="520" class="mx-4 soft-panel login-form-card" rounded="xl">
          <v-card-text class="pa-8">
            <div class="text-h4 font-weight-bold">进入织文者</div>
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

<style scoped>
.login-shell {
  background:
    linear-gradient(135deg, rgba(10, 17, 24, 0.42), rgba(10, 17, 24, 0.24)),
    radial-gradient(circle at left top, rgba(185, 95, 48, 0.16), transparent 28%),
    url('/cover.png') center top / 100% auto no-repeat;
}

.login-hero-column,
.login-form-column {
  min-height: 100vh;
}

.login-hero {
  position: relative;
  width: min(100%, 720px);
  min-height: 84vh;
  margin: 24px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 32px;
  box-shadow: 0 24px 60px rgba(46, 61, 78, 0.14);
  backdrop-filter: blur(6px);
}

.login-hero__art,
.login-hero__overlay {
  position: absolute;
  inset: 0;
}

.login-hero__art {
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    linear-gradient(135deg, rgba(17, 30, 43, 0.18), rgba(114, 59, 24, 0.1)),
    linear-gradient(180deg, rgba(255, 248, 239, 0.1), rgba(241, 246, 251, 0.08));
}

.login-hero__overlay {
  background:
    linear-gradient(180deg, rgba(8, 18, 28, 0.12) 0%, rgba(8, 18, 28, 0.56) 100%),
    linear-gradient(135deg, rgba(184, 100, 54, 0.12), transparent 36%);
}

.login-hero__content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  min-height: 84vh;
  padding: 40px;
  color: #f8fbff;
}

.login-hero__eyebrow {
  color: rgba(255, 244, 234, 0.88);
  letter-spacing: 0.16em;
}

.login-hero__title {
  max-width: 12ch;
  line-height: 1.08;
  color: #fff9f4;
}

.login-hero__subtitle {
  max-width: 30rem;
  line-height: 1.5;
  color: rgba(244, 248, 252, 0.9);
}

.login-hero__card {
  color: #14314f;
  backdrop-filter: blur(10px);
  background: linear-gradient(135deg, rgba(255, 251, 246, 0.92), rgba(245, 249, 253, 0.86));
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.login-hero__card-text {
  color: rgba(20, 49, 79, 0.76);
}

.login-form-card {
  box-shadow: 0 18px 46px rgba(46, 61, 78, 0.1);
}

@media (max-width: 1279px) {
  .login-form-column {
    min-height: auto;
    padding: 24px 0;
  }
}
</style>
