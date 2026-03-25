import http from './http'
import type { AuthPublicConfig, UserProfile } from '@/types'

export interface AuthPayload {
  token: string
  user: UserProfile
}

export function login(payload: { username: string; password: string }) {
  return http.post<never, AuthPayload>('/auth/login', payload)
}

export function register(payload: { username: string; password: string }) {
  return http.post<never, AuthPayload>('/auth/register', payload)
}

export function getPublicConfig() {
  return http.get<never, AuthPublicConfig>('/auth/public-config')
}
