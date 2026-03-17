import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import ProjectListView from '../views/ProjectListView.vue'
import ChapterManagementView from '../views/ChapterManagementView.vue'
import CharacterManagementView from '../views/CharacterManagementView.vue'
import WorldSettingView from '../views/WorldSettingView.vue'
import AIWritingView from '../views/AIWritingView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/projects' },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/projects', name: 'projects', component: ProjectListView },
    { path: '/chapters', name: 'chapters', component: ChapterManagementView },
    { path: '/characters', name: 'characters', component: CharacterManagementView },
    { path: '/world', name: 'world', component: WorldSettingView },
    { path: '/ai-writing', name: 'aiWriting', component: AIWritingView },
  ],
})

export default router
