<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { mainMenuSections } from '@/router/menu'
import { useAuthStore } from '@/stores/auth'
import { useProjectStore } from '@/stores/project'

const drawer = ref(true)
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const projectStore = useProjectStore()

const currentUserName = computed(() => authStore.user?.nickname || authStore.user?.username || '创作者')
const currentUserRoleLabel = computed(() => (authStore.isAdmin ? '管理员' : '创作者'))
const visibleMenuSections = computed(() =>
  mainMenuSections
    .map((section) => ({
      ...section,
      items: section.items.filter((item) => !item.adminOnly || authStore.isAdmin),
    }))
    .filter((section) => section.items.length > 0),
)
const activeMenuTitle = computed(() => {
  for (const section of visibleMenuSections.value) {
    const matched = section.items.find((item) => routeMatches(item.to))
    if (matched) {
      return matched.title
    }
  }
  return typeof route.meta.title === 'string' ? route.meta.title : '织文者'
})
const activeSectionTitle = computed(() => {
  for (const section of visibleMenuSections.value) {
    if (section.items.some((item) => routeMatches(item.to))) {
      return section.title
    }
  }
  return '创作台'
})

async function ensureProjects() {
  if (!projectStore.projects.length && authStore.isAuthenticated) {
    await projectStore.fetchProjects().catch(() => undefined)
  }
}

function routeMatches(target: string) {
  return route.path === target || route.path.startsWith(`${target}/`)
}

function logout() {
  authStore.signOut()
  router.push({ name: 'login' })
}

onMounted(() => {
  ensureProjects()
})
</script>

<template>
  <v-layout class="min-h-screen">
    <v-navigation-drawer v-model="drawer" width="312" class="border-e app-drawer" elevation="0">
      <div class="pa-5 pb-4">
        <div class="text-overline text-secondary">Stateful Story Platform</div>
        <div class="text-h5 font-weight-bold mt-1">织文者</div>
        <div class="text-body-2 text-medium-emphasis mt-2">
          主入口先服务当前创作动作，再下沉到故事结构、状态与生成细节。
        </div>
      </div>

      <div class="px-3 pb-2">
        <v-card class="soft-panel">
          <v-card-text>
            <div class="text-caption text-medium-emphasis">当前项目</div>
            <v-select
              class="mt-2"
              hide-details
              item-title="name"
              item-value="id"
              :items="projectStore.projects"
              :model-value="projectStore.selectedProjectId"
              placeholder="请选择项目"
              @update:model-value="projectStore.setCurrentProject"
            />
          </v-card-text>
        </v-card>
      </div>

      <div class="px-2 pb-4">
        <div
          v-for="section in visibleMenuSections"
          :key="section.key"
          class="menu-section"
        >
          <div class="menu-section-heading">{{ section.title }}</div>
          <div v-if="section.subtitle" class="menu-section-subtitle">
            {{ section.subtitle }}
          </div>
          <v-list nav class="px-0 pt-1">
            <v-list-item
              v-for="item in section.items"
              :key="item.to"
              :prepend-icon="item.icon"
              :title="item.title"
              :subtitle="item.subtitle"
              :active="routeMatches(item.to)"
              rounded="xl"
              :to="item.to"
            />
          </v-list>
        </div>
      </div>
    </v-navigation-drawer>

    <v-main>
      <v-app-bar elevation="0" color="transparent">
        <v-app-bar-nav-icon @click="drawer = !drawer" />
        <div>
          <div class="text-caption text-medium-emphasis">{{ activeSectionTitle }}</div>
          <div class="text-h6 font-weight-bold">{{ activeMenuTitle }}</div>
        </div>
        <v-spacer />
        <v-chip color="secondary" variant="tonal" prepend-icon="mdi-account-circle-outline">
          {{ currentUserName }} · {{ currentUserRoleLabel }}
        </v-chip>
        <v-btn icon="mdi-logout" variant="text" @click="logout" />
      </v-app-bar>

      <v-container fluid class="pa-4 pa-md-6">
        <router-view />
      </v-container>
    </v-main>
  </v-layout>
</template>
