import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    username: 'admin',
    isAuthenticated: false,
  }),
  actions: {
    login(username: string) {
      this.username = username
      this.isAuthenticated = true
    },
    logout() {
      this.isAuthenticated = false
    },
  },
})
