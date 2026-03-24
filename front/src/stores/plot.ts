import { ref } from 'vue'
import { defineStore } from 'pinia'

import * as plotApi from '@/api/plot'
import type { Plot } from '@/types'

export const usePlotStore = defineStore('plot', () => {
  const plotlines = ref<Plot[]>([])
  const loading = ref(false)

  async function fetchByProject(projectId: number) {
    loading.value = true
    try {
      plotlines.value = await plotApi.getPlots(projectId)
    } catch (error) {
      plotlines.value = []
      throw error
    } finally {
      loading.value = false
    }
  }

  async function create(projectId: number, payload: Partial<Plot>) {
    const plot = await plotApi.createPlot(projectId, payload)
    plotlines.value.push(plot)
    return plot
  }

  async function update(id: number, payload: Partial<Plot>) {
    await plotApi.updatePlot(id, payload)
    const target = plotlines.value.find((item) => item.id === id)
    if (target) {
      Object.assign(target, payload)
    }
  }

  async function remove(id: number) {
    await plotApi.deletePlot(id)
    plotlines.value = plotlines.value.filter((item) => item.id !== id)
  }

  return { plotlines, loading, fetchByProject, create, update, remove }
})
