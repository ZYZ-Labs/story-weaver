import http from './http'
import type { SystemConfig } from '@/types'

export function getSystemConfigs() {
  return http.get<never, SystemConfig[]>('/settings/system-configs')
}

export function saveSystemConfigs(payload: SystemConfig[]) {
  return http.put<never, SystemConfig[]>('/settings/system-configs', payload)
}
