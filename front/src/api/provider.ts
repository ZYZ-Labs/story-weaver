import http from './http'
import type { AIProvider, ProviderDiscoveryResult } from '@/types'

export function getProviders() {
  return http.get<never, AIProvider[]>('/providers')
}

export function createProvider(payload: Partial<AIProvider>) {
  return http.post<never, AIProvider>('/providers', payload)
}

export function updateProvider(id: number, payload: Partial<AIProvider>) {
  return http.put<never, AIProvider>(`/providers/${id}`, payload)
}

export function deleteProvider(id: number) {
  return http.delete(`/providers/${id}`)
}

export function testProvider(id: number) {
  return http.post<never, { success: boolean }>(`/providers/${id}/test`)
}

export function discoverProviderModels(payload: Partial<AIProvider>) {
  return http.post<never, ProviderDiscoveryResult>('/providers/discover-models', payload)
}
