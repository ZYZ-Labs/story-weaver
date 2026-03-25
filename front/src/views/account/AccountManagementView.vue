<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import EmptyState from '@/components/EmptyState.vue'
import PageContainer from '@/components/PageContainer.vue'
import { useUserAdminStore } from '@/stores/user-admin'
import type { ManagedUser } from '@/types'
import { formatDateTime } from '@/utils/format'

const userAdminStore = useUserAdminStore()

const dialog = ref(false)
const resetPasswordDialog = ref(false)
const editingId = ref<number | null>(null)
const submitLoading = ref(false)
const resetLoading = ref(false)
const unlockingId = ref<number | null>(null)
const resetTarget = ref<ManagedUser | null>(null)
const feedbackMessage = ref('')
const feedbackType = ref<'success' | 'error'>('success')

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  email: '',
  roleCode: 'user',
  status: 1,
})

const passwordForm = reactive({
  newPassword: '',
})

const headers = [
  { title: '账号', key: 'username', sortable: false },
  { title: '角色', key: 'roleCode', sortable: false },
  { title: '状态', key: 'status', sortable: false },
  { title: '安全状态', key: 'security', sortable: false },
  { title: '最近登录', key: 'lastLoginAt', sortable: false },
  { title: '更新时间', key: 'updateTime', sortable: false },
  { title: '操作', key: 'actions', sortable: false, align: 'end' as const },
]

const isCreateMode = computed(() => editingId.value == null)
const hasUsers = computed(() => userAdminStore.users.length > 0)

function generateStrongPassword() {
  const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*'
  let value = ''
  for (let index = 0; index < 14; index += 1) {
    value += alphabet[Math.floor(Math.random() * alphabet.length)]
  }
  return `${value}8A`
}

function resetForm() {
  Object.assign(form, {
    username: '',
    password: '',
    nickname: '',
    email: '',
    roleCode: 'user',
    status: 1,
  })
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialog.value = true
}

function openEdit(user: ManagedUser) {
  editingId.value = user.id
  Object.assign(form, {
    username: user.username,
    password: '',
    nickname: user.nickname,
    email: user.email || '',
    roleCode: user.roleCode,
    status: user.status,
  })
  dialog.value = true
}

function openResetPassword(user: ManagedUser) {
  resetTarget.value = user
  passwordForm.newPassword = ''
  resetPasswordDialog.value = true
}

function setFeedback(type: 'success' | 'error', message: string) {
  feedbackType.value = type
  feedbackMessage.value = message
}

function getRoleLabel(roleCode: string) {
  return roleCode === 'admin' ? '管理员' : '普通账号'
}

function getStatusLabel(status: number) {
  return status === 1 ? '启用中' : '已禁用'
}

function getSecurityText(user: ManagedUser) {
  if (user.locked && user.lockedUntil) {
    return `已锁定至 ${formatDateTime(user.lockedUntil)}`
  }
  if (user.failedLoginAttempts > 0) {
    return `已累计失败 ${user.failedLoginAttempts} 次`
  }
  return '正常'
}

function getSecurityColor(user: ManagedUser) {
  if (user.locked) return 'error'
  if (user.failedLoginAttempts > 0) return 'warning'
  return 'success'
}

async function submit() {
  submitLoading.value = true
  try {
    if (isCreateMode.value) {
      const password = form.password.trim()
      if (!password) {
        throw new Error('请填写初始密码，或点击“生成密码”。')
      }

      await userAdminStore.create({
        username: form.username.trim(),
        password,
        nickname: form.nickname.trim(),
        email: form.email.trim() || undefined,
        roleCode: form.roleCode,
        status: Number(form.status),
      })
      setFeedback('success', `账号 ${form.username.trim()} 创建成功。`)
    } else if (editingId.value) {
      await userAdminStore.update(editingId.value, {
        nickname: form.nickname.trim(),
        email: form.email.trim() || undefined,
        roleCode: form.roleCode,
        status: Number(form.status),
      })
      setFeedback('success', `账号 ${form.username.trim()} 已更新。`)
    }
    dialog.value = false
  } catch (error) {
    setFeedback('error', error instanceof Error ? error.message : '保存账号失败')
  } finally {
    submitLoading.value = false
  }
}

async function submitResetPassword() {
  if (!resetTarget.value) {
    return
  }

  resetLoading.value = true
  try {
    const password = passwordForm.newPassword.trim()
    if (!password) {
      throw new Error('请填写新密码，或点击“生成密码”。')
    }

    await userAdminStore.resetPassword(resetTarget.value.id, password)
    setFeedback('success', `已重置 ${resetTarget.value.username} 的密码。`)
    resetPasswordDialog.value = false
  } catch (error) {
    setFeedback('error', error instanceof Error ? error.message : '重置密码失败')
  } finally {
    resetLoading.value = false
  }
}

async function unlockUser(user: ManagedUser) {
  unlockingId.value = user.id
  try {
    await userAdminStore.unlock(user.id)
    setFeedback('success', `账号 ${user.username} 已解除锁定并清空失败次数。`)
  } catch (error) {
    setFeedback('error', error instanceof Error ? error.message : '解除锁定失败')
  } finally {
    unlockingId.value = null
  }
}

onMounted(async () => {
  await userAdminStore.fetchAll().catch((error: unknown) => {
    setFeedback('error', error instanceof Error ? error.message : '加载账号列表失败')
  })
})
</script>

<template>
  <PageContainer
    title="账号管理"
    description="集中管理管理员和创作者账号，支持创建账号、重置密码、解除锁定和禁用高风险账号。"
  >
    <template #actions>
      <v-btn color="primary" prepend-icon="mdi-account-plus-outline" @click="openCreate">
        新增账号
      </v-btn>
    </template>

    <v-alert v-if="feedbackMessage" :type="feedbackType" variant="tonal">
      {{ feedbackMessage }}
    </v-alert>

    <v-alert type="info" variant="tonal">
      建议外网环境关闭公开注册，仅通过这里创建账号；密码字段默认不再预填，若需要随机密码可手动点击“生成密码”。
    </v-alert>

    <EmptyState
      v-if="!hasUsers"
      title="还没有可管理的账号"
      description="先创建第一个可登录账号，后续即可在这里继续重置密码和维护角色权限。"
      icon="mdi-account-lock-open-outline"
    >
      <v-btn color="primary" prepend-icon="mdi-account-plus-outline" @click="openCreate">
        创建账号
      </v-btn>
    </EmptyState>

    <v-card v-else class="soft-panel">
      <v-data-table
        :headers="headers"
        :items="userAdminStore.users"
        :loading="userAdminStore.loading"
        item-value="id"
        loading-text="正在加载账号列表..."
      >
        <template #[`item.username`]="{ item }">
          <div>
            <div class="font-weight-bold">{{ item.username }}</div>
            <div class="text-caption text-medium-emphasis">{{ item.nickname || '未填写昵称' }}</div>
            <div class="text-caption text-medium-emphasis">{{ item.email || '未填写邮箱' }}</div>
          </div>
        </template>

        <template #[`item.roleCode`]="{ item }">
          <v-chip :color="item.roleCode === 'admin' ? 'secondary' : 'default'" variant="tonal" size="small">
            {{ getRoleLabel(item.roleCode) }}
          </v-chip>
        </template>

        <template #[`item.status`]="{ item }">
          <v-chip :color="item.status === 1 ? 'success' : 'default'" variant="tonal" size="small">
            {{ getStatusLabel(item.status) }}
          </v-chip>
        </template>

        <template #[`item.security`]="{ item }">
          <div class="d-flex flex-column ga-1">
            <v-chip :color="getSecurityColor(item)" variant="tonal" size="small">
              {{ getSecurityText(item) }}
            </v-chip>
            <span class="text-caption text-medium-emphasis">
              最近改密：{{ formatDateTime(item.passwordChangedAt) }}
            </span>
          </div>
        </template>

        <template #[`item.lastLoginAt`]="{ item }">
          <span class="text-body-2">{{ formatDateTime(item.lastLoginAt) }}</span>
        </template>

        <template #[`item.updateTime`]="{ item }">
          <span class="text-body-2">{{ formatDateTime(item.updateTime) }}</span>
        </template>

        <template #[`item.actions`]="{ item }">
          <div class="d-flex justify-end ga-2 flex-wrap">
            <v-btn size="small" variant="outlined" @click="openEdit(item)">编辑</v-btn>
            <v-btn size="small" color="primary" variant="text" @click="openResetPassword(item)">
              重置密码
            </v-btn>
            <v-btn
              v-if="item.locked || item.failedLoginAttempts > 0"
              size="small"
              color="warning"
              variant="text"
              :loading="unlockingId === item.id"
              @click="unlockUser(item)"
            >
              解除锁定
            </v-btn>
          </div>
        </template>
      </v-data-table>
    </v-card>

    <v-dialog v-model="dialog" max-width="760">
      <v-card>
        <v-card-title>{{ isCreateMode ? '新增账号' : '编辑账号' }}</v-card-title>
        <v-card-text class="pt-4">
          <v-row>
            <v-col cols="12" md="6">
              <v-text-field
                v-model="form.username"
                label="用户名"
                :readonly="!isCreateMode"
                :hint="isCreateMode ? '建议使用英文、数字或短横线' : '用户名创建后不再修改'"
                persistent-hint
              />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.nickname" label="昵称" />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.email" label="邮箱" />
            </v-col>
            <v-col cols="12" md="3">
              <v-select
                v-model="form.roleCode"
                label="角色"
                :items="[
                  { title: '管理员', value: 'admin' },
                  { title: '普通账号', value: 'user' },
                ]"
                item-title="title"
                item-value="value"
              />
            </v-col>
            <v-col cols="12" md="3">
              <v-select
                v-model="form.status"
                label="状态"
                :items="[
                  { title: '启用', value: 1 },
                  { title: '禁用', value: 0 },
                ]"
                item-title="title"
                item-value="value"
              />
            </v-col>
            <v-col v-if="isCreateMode" cols="12">
              <div class="d-flex align-start ga-3">
                <v-text-field
                  v-model="form.password"
                  class="flex-grow-1"
                  label="初始密码"
                  type="password"
                  autocomplete="new-password"
                  hint="密码至少 8 位，并同时包含字母和数字。提交前请自行记录。"
                  persistent-hint
                />
                <v-btn class="mt-2" variant="outlined" @click="form.password = generateStrongPassword()">
                  生成密码
                </v-btn>
              </div>
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions class="justify-end">
          <v-btn variant="text" @click="dialog = false">取消</v-btn>
          <v-btn color="primary" :loading="submitLoading" @click="submit">保存</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="resetPasswordDialog" max-width="620">
      <v-card>
        <v-card-title>重置密码</v-card-title>
        <v-card-text class="pt-4">
          <div class="text-body-2 text-medium-emphasis mb-4">
            当前账号：{{ resetTarget?.username }}。重置后旧密码会立即失效，登录失败次数也会一并清空。
          </div>
          <div class="d-flex align-start ga-3">
            <v-text-field
              v-model="passwordForm.newPassword"
              class="flex-grow-1"
              label="新密码"
              type="password"
              autocomplete="new-password"
              hint="默认不预填密码；如需随机密码可点击右侧生成，并在提交前自行记录。"
              persistent-hint
            />
            <v-btn class="mt-2" variant="outlined" @click="passwordForm.newPassword = generateStrongPassword()">
              生成密码
            </v-btn>
          </div>
        </v-card-text>
        <v-card-actions class="justify-end">
          <v-btn variant="text" @click="resetPasswordDialog = false">取消</v-btn>
          <v-btn color="primary" :loading="resetLoading" @click="submitResetPassword">确认重置</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </PageContainer>
</template>
