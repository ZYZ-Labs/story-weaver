import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import * as projectApi from '@/api/project'
import type { Project } from '@/types'
import { clearStorage, readStorage, storageKeys, writeStorage } from '@/utils/storage'

const selectedProjectId = ref<number | null>(readStorage<number | null>(storageKeys.projectId, null))

export const useProjectStore = defineStore('project', () => {
  const projects = ref<Project[]>([])
  const loading = ref(false)

  const currentProject = computed(() =>
    projects.value.find((project) => project.id === selectedProjectId.value) || null,
  )

  async function fetchProjects() {
    loading.value = true
    try {
      projects.value = await projectApi.getProjects()
      if (!currentProject.value && projects.value.length > 0) {
        setCurrentProject(projects.value[0].id)
      }
    } finally {
      loading.value = false
    }
  }

  async function create(payload: Partial<Project>) {
    const project = await projectApi.createProject(payload)
    projects.value.unshift(project)
    setCurrentProject(project.id)
    return project
  }

  async function update(id: number, payload: Partial<Project>) {
    await projectApi.updateProject(id, payload)
    const target = projects.value.find((item) => item.id === id)
    if (target) {
      Object.assign(target, payload)
    }
  }

  async function remove(id: number) {
    await projectApi.deleteProject(id)
    projects.value = projects.value.filter((item) => item.id !== id)
    if (selectedProjectId.value === id) {
      const nextId = projects.value[0]?.id ?? null
      setCurrentProject(nextId)
    }
  }

  function setCurrentProject(id: number | null) {
    selectedProjectId.value = id
    if (id === null) {
      clearStorage(storageKeys.projectId)
      return
    }
    writeStorage(storageKeys.projectId, id)
  }

  return {
    projects,
    loading,
    currentProject,
    selectedProjectId,
    fetchProjects,
    create,
    update,
    remove,
    setCurrentProject,
  }
})
