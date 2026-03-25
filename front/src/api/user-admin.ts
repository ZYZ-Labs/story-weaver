import http from './http'
import type { ManagedUser } from '@/types'

export function getManagedUsers() {
  return http.get<never, ManagedUser[]>('/admin/users')
}

export function createManagedUser(payload: {
  username: string
  password: string
  nickname: string
  email?: string
  roleCode: string
  status: number
}) {
  return http.post<never, ManagedUser>('/admin/users', payload)
}

export function updateManagedUser(
  id: number,
  payload: {
    nickname: string
    email?: string
    roleCode: string
    status: number
  },
) {
  return http.put<never, ManagedUser>(`/admin/users/${id}`, payload)
}

export function resetManagedUserPassword(id: number, payload: { newPassword: string }) {
  return http.post<never, ManagedUser>(`/admin/users/${id}/reset-password`, payload)
}

export function unlockManagedUser(id: number) {
  return http.post<never, ManagedUser>(`/admin/users/${id}/unlock`)
}
