import axios from 'axios'
import { AUTH_TOKEN_KEY } from '../stores/storyweaver'

const fallbackBaseUrl = 'http://localhost:8080/api'

const fallbackBaseUrl = 'http://localhost:8080/api'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || fallbackBaseUrl,
  timeout: 10000,
})

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('authToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export default apiClient
