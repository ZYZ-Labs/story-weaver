import { ref } from 'vue'
import { defineStore } from 'pinia'

import * as userAdminApi from '@/api/user-admin'
import type { ManagedUser } from '@/types'

export const useUserAdminStore = defineStore('user-admin', () => {
  const users = ref<ManagedUser[]>([])
  const loading = ref(false)

  async function fetchAll() {
    loading.value = true
    try {
      users.value = await userAdminApi.getManagedUsers()
    } finally {
      loading.value = false
    }
  }

  async function create(payload: {
    username: string
    password: string
    nickname: string
    email?: string
    roleCode: string
    status: number
  }) {
    const created = await userAdminApi.createManagedUser(payload)
    users.value.push(created)
    users.value.sort((left, right) => left.id - right.id)
    return created
  }

  async function update(
    id: number,
    payload: {
      nickname: string
      email?: string
      roleCode: string
      status: number
    },
  ) {
    const updated = await userAdminApi.updateManagedUser(id, payload)
    const target = users.value.find((item) => item.id === id)
    if (target) {
      Object.assign(target, updated)
    }
    return updated
  }

  async function resetPassword(id: number, newPassword: string) {
    const updated = await userAdminApi.resetManagedUserPassword(id, { newPassword })
    const target = users.value.find((item) => item.id === id)
    if (target) {
      Object.assign(target, updated)
    }
    return updated
  }

  async function unlock(id: number) {
    const updated = await userAdminApi.unlockManagedUser(id)
    const target = users.value.find((item) => item.id === id)
    if (target) {
      Object.assign(target, updated)
    }
    return updated
  }

  return {
    users,
    loading,
    fetchAll,
    create,
    update,
    resetPassword,
    unlock,
  }
})
