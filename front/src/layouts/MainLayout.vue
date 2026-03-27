<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { mainMenu } from '@/router/menu'
import { useAuthStore } from '@/stores/auth'
import { useProjectStore } from '@/stores/project'

const drawer = ref(true)
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const projectStore = useProjectStore()

const currentUserName = computed(() => authStore.user?.nickname || authStore.user?.username || '创作者')
const currentUserRoleLabel = computed(() => (authStore.isAdmin ? '管理员' : '创作者'))
const visibleMenu = computed(() => mainMenu.filter((item) => !item.adminOnly || authStore.isAdmin))

async function ensureProjects() {
  if (!projectStore.projects.length && authStore.isAuthenticated) {
    await projectStore.fetchProjects().catch(() => undefined)
  }
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
    <v-navigation-drawer v-model="drawer" width="296" class="border-e" elevation="0">
      <div class="pa-5">
        <div class="text-overline text-secondary">长篇创作工作台</div>
        <div class="text-h5 font-weight-bold mt-1">织文者</div>
        <div class="text-body-2 text-medium-emphasis mt-2">
          把项目、设定、因果和 AI 写作收拢到一个可持续演进的工作台。
        </div>
      </div>

      <div class="px-3 pb-3">
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

      <v-list nav class="px-2">
        <v-list-item
          v-for="item in visibleMenu"
          :key="item.to"
          :prepend-icon="item.icon"
          :title="item.title"
          :subtitle="item.subtitle"
          :active="route.path === item.to"
          rounded="xl"
          :to="item.to"
        />
      </v-list>
    </v-navigation-drawer>

    <v-main>
      <v-app-bar elevation="0" color="transparent">
        <v-app-bar-nav-icon @click="drawer = !drawer" />
        <div class="text-h6 font-weight-bold">{{ route.meta.title }}</div>
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
