import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { public: true, title: '登录' },
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      children: [
        { path: '', redirect: '/dashboard' },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: 'Dashboard' },
        },
        {
          path: 'projects',
          name: 'projects',
          component: () => import('@/views/project/ProjectListView.vue'),
          meta: { title: '项目管理' },
        },
        {
          path: 'projects/:id',
          name: 'project-detail',
          component: () => import('@/views/project/ProjectDetailView.vue'),
          meta: { title: '项目详情' },
        },
        {
          path: 'chapters',
          name: 'chapters',
          component: () => import('@/views/chapter/ChapterListView.vue'),
          meta: { title: '章节管理' },
        },
        {
          path: 'characters',
          name: 'characters',
          component: () => import('@/views/character/CharacterListView.vue'),
          meta: { title: '人物管理' },
        },
        {
          path: 'plots',
          name: 'plots',
          component: () => import('@/views/plot/PlotView.vue'),
          meta: { title: '剧情管理' },
        },
        {
          path: 'causality',
          name: 'causality',
          component: () => import('@/views/causality/CausalityView.vue'),
          meta: { title: '因果管理' },
        },
        {
          path: 'writing',
          name: 'writing',
          component: () => import('@/views/writing/WritingView.vue'),
          meta: { title: '写作中心' },
        },
        {
          path: 'rag',
          name: 'rag',
          component: () => import('@/views/rag/RagView.vue'),
          meta: { title: 'RAG 知识库' },
        },
        {
          path: 'providers',
          name: 'providers',
          component: () => import('@/views/provider/ProviderView.vue'),
          meta: { title: 'AI Provider' },
        },
        {
          path: 'settings',
          name: 'settings',
          component: () => import('@/views/settings/SettingsView.vue'),
          meta: { title: '系统设置' },
        },
        {
          path: 'system',
          name: 'system',
          component: () => import('@/views/system/SystemView.vue'),
          meta: { title: '系统日志' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/system/NotFoundView.vue'),
      meta: { public: true, title: '页面不存在' },
    },
  ],
})

router.beforeEach((to) => {
  const authStore = useAuthStore()

  if (!to.meta.public && !authStore.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.name === 'login' && authStore.isAuthenticated) {
    return { name: 'dashboard' }
  }

  if (typeof to.meta.title === 'string') {
    document.title = `${to.meta.title} | Story Weaver`
  }

  return true
})

export default router
