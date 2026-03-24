import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', () => {
  const loading = ref(false)
  const pageTitle = ref('Dashboard')

  function setLoading(value: boolean) {
    loading.value = value
  }

  function setPageTitle(value: string) {
    pageTitle.value = value
  }

  return {
    loading,
    pageTitle,
    setLoading,
    setPageTitle,
  }
})
