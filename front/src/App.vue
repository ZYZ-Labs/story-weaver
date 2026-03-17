<template>
  <v-app>
    <v-app-bar color="primary" density="comfortable">
      <v-app-bar-title>Story Weaver</v-app-bar-title>
      <v-btn variant="text" @click="goLogin">登录</v-btn>
    </v-app-bar>

    <v-navigation-drawer permanent>
      <v-list nav>
        <v-list-item
          v-for="item in navItems"
          :key="item.to"
          :title="item.title"
          :to="item.to"
          link
          router
        />
      </v-list>
    </v-navigation-drawer>

    <v-main>
      <v-container class="py-6">
        <router-view />
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const navItems = [
  { title: '项目列表', to: '/projects' },
  { title: '章节管理', to: '/chapters' },
  { title: '角色管理', to: '/characters' },
  { title: '世界设定', to: '/world' },
  { title: 'AI 写作入口', to: '/ai-writing' },
]

const goLogin = () => {
  router.push('/login')
}
</script>
