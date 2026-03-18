import { defineStore } from 'pinia'

export const AUTH_TOKEN_KEY = 'storyweaver.authToken'
export const AUTH_USERNAME_KEY = 'storyweaver.username'

interface AuthUser {
  username: string
}

interface StoryweaverState {
  username: string
  isAuthenticated: boolean
}

export const useStoryweaverStore = defineStore('storyweaver', {
  state: (): StoryweaverState => ({
    username: localStorage.getItem(AUTH_USERNAME_KEY) || 'guest',
    isAuthenticated: Boolean(localStorage.getItem(AUTH_TOKEN_KEY)),
  }),
  getters: {
    authToken: () => localStorage.getItem(AUTH_TOKEN_KEY),
  },
  actions: {
    login(user: AuthUser, token: string) {
      this.username = user.username
      this.isAuthenticated = true
      localStorage.setItem(AUTH_TOKEN_KEY, token)
      localStorage.setItem(AUTH_USERNAME_KEY, user.username)
    },
    logout() {
      this.isAuthenticated = false
      this.username = 'guest'
      localStorage.removeItem(AUTH_TOKEN_KEY)
      localStorage.removeItem(AUTH_USERNAME_KEY)
    },
  },
})
