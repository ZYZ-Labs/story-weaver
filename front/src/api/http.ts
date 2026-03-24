import axios, { AxiosError } from 'axios'

import router from '@/router'
import { appEnv } from '@/utils/env'
import { clearStorage, readStorage, storageKeys } from '@/utils/storage'

type ApiEnvelope<T> = {
  code?: number
  message?: string
  data?: T
}

const http = axios.create({
  baseURL: appEnv.apiBaseUrl,
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = readStorage<string | null>(storageKeys.token, null)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response): any => {
    const payload = response.data as ApiEnvelope<unknown> | unknown
    if (payload && typeof payload === 'object' && 'code' in payload) {
      const envelope = payload as ApiEnvelope<unknown>
      if (envelope.code && envelope.code >= 400) {
        return Promise.reject(new Error(envelope.message || '请求失败'))
      }
      return envelope.data
    }
    return payload
  },
  async (error: AxiosError<ApiEnvelope<unknown>>) => {
    if (error.response?.status === 401) {
      clearStorage(storageKeys.token)
      clearStorage(storageKeys.user)
      if (router.currentRoute.value.name !== 'login') {
        await router.replace({ name: 'login' })
      }
    }

    const message = error.response?.data?.message || error.message || '网络请求失败'
    return Promise.reject(new Error(message))
  },
)

export default http
