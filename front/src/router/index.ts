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
        { path: '', redirect: '/workbench' },
        {
          path: 'workbench',
          name: 'workbench',
          component: () => import('@/views/workbench/WorkbenchView.vue'),
          meta: { title: '创作台' },
        },
        {
          path: 'chapter-workspace',
          name: 'chapter-workspace',
          component: () => import('@/views/chapter/ChapterWorkspaceView.vue'),
          meta: { title: '章节工作区' },
        },
        {
          path: 'dashboard',
          redirect: '/workbench',
        },
        {
          path: 'state-center',
          name: 'state-center',
          component: () => import('@/views/state/StateCenterView.vue'),
          meta: { title: '状态台' },
        },
        {
          path: 'generation-center',
          name: 'generation-center',
          component: () => import('@/views/generation/GenerationCenterView.vue'),
          meta: { title: '生成台' },
        },
        {
          path: 'projects',
          name: 'projects',
          component: () => import('@/views/project/ProjectListView.vue'),
          meta: { title: '项目总览' },
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
          path: 'world-settings',
          name: 'world-settings',
          component: () => import('@/views/world-setting/WorldSettingView.vue'),
          meta: { title: '世界观管理' },
        },
        {
          path: 'outlines',
          name: 'outlines',
          component: () => import('@/views/outline/OutlineView.vue'),
          meta: { title: '大纲管理' },
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
          meta: { title: '知识库' },
        },
        {
          path: 'providers',
          name: 'providers',
          component: () => import('@/views/provider/ProviderView.vue'),
          meta: { title: '模型服务', requiresAdmin: true },
        },
        {
          path: 'accounts',
          name: 'accounts',
          component: () => import('@/views/account/AccountManagementView.vue'),
          meta: { title: '账号管理', requiresAdmin: true },
        },
        {
          path: 'settings',
          name: 'settings',
          component: () => import('@/views/settings/SettingsView.vue'),
          meta: { title: '系统设置', requiresAdmin: true },
        },
        {
          path: 'system',
          name: 'system',
          component: () => import('@/views/system/SystemView.vue'),
          meta: { title: '系统状态', requiresAdmin: true },
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

  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    return { name: 'workbench' }
  }

  if (to.name === 'login' && authStore.isAuthenticated) {
    return { name: 'workbench' }
  }

  if (typeof to.meta.title === 'string') {
    document.title = `${to.meta.title} | 织文者`
  }

  return true
})

export default router
