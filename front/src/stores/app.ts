import { defineStore } from 'pinia'

const TOKEN_KEY = 'authToken'
const USERNAME_KEY = 'username'

export const useAppStore = defineStore('app', {
  state: () => ({
    username: localStorage.getItem(USERNAME_KEY) || 'guest',
    isAuthenticated: Boolean(localStorage.getItem(TOKEN_KEY)),
  }),
  actions: {
    login(username: string, token: string) {
      this.username = username
      this.isAuthenticated = true
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(USERNAME_KEY, username)
    },
    logout() {
      this.isAuthenticated = false
      this.username = 'guest'
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USERNAME_KEY)
    },
  },
})
