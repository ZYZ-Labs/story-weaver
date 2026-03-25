import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import * as authApi from '@/api/auth'
import type { UserProfile } from '@/types'
import { clearStorage, readStorage, storageKeys, writeStorage } from '@/utils/storage'

const tokenState = ref<string | null>(null)
const userState = ref<UserProfile | null>(null)

export const useAuthStore = defineStore('auth', () => {
  const token = computed(() => tokenState.value)
  const user = computed(() => userState.value)
  const isAuthenticated = computed(() => Boolean(tokenState.value))
  const isAdmin = computed(() => {
    if (!userState.value) {
      return false
    }
    return userState.value.roleCode === 'admin' || userState.value.username === 'admin'
  })

  function persist(tokenValue: string | null, userValue: UserProfile | null) {
    tokenState.value = tokenValue
    userState.value = userValue

    if (tokenValue) {
      writeStorage(storageKeys.token, tokenValue)
    } else {
      clearStorage(storageKeys.token)
    }

    if (userValue) {
      writeStorage(storageKeys.user, userValue)
    } else {
      clearStorage(storageKeys.user)
    }
  }

  async function signIn(payload: { username: string; password: string }) {
    const response = await authApi.login(payload)
    persist(response.token, response.user)
    return response
  }

  async function signUp(payload: { username: string; password: string }) {
    const response = await authApi.register(payload)
    persist(response.token, response.user)
    return response
  }

  function signOut() {
    persist(null, null)
  }

  return {
    token,
    user,
    isAuthenticated,
    isAdmin,
    signIn,
    signUp,
    signOut,
  }
})

export function bootstrapAuth() {
  tokenState.value = readStorage<string | null>(storageKeys.token, null)
  userState.value = readStorage<UserProfile | null>(storageKeys.user, null)
}
