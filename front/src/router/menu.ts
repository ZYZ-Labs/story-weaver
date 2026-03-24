import type { MenuItem } from '@/types'

export const mainMenu: MenuItem[] = [
  { title: 'Dashboard', icon: 'mdi-view-dashboard-outline', to: '/dashboard', subtitle: '总览创作进度' },
  { title: '项目管理', icon: 'mdi-bookshelf', to: '/projects', subtitle: '项目列表与详情' },
  { title: '章节管理', icon: 'mdi-file-document-edit-outline', to: '/chapters', subtitle: '章节结构与正文' },
  { title: '人物管理', icon: 'mdi-account-group-outline', to: '/characters', subtitle: '角色卡与设定' },
  { title: '剧情管理', icon: 'mdi-source-branch', to: '/plots', subtitle: '剧情线与冲突节点' },
  { title: '因果管理', icon: 'mdi-graph-outline', to: '/causality', subtitle: '因果节点与权重' },
  { title: '写作中心', icon: 'mdi-feather', to: '/writing', subtitle: '正文编辑与 AI 续写' },
  { title: 'RAG 知识库', icon: 'mdi-database-search-outline', to: '/rag', subtitle: '知识片段与检索' },
  { title: 'AI Provider', icon: 'mdi-robot-outline', to: '/providers', subtitle: '模型与能力映射' },
  { title: '系统设置', icon: 'mdi-cog-outline', to: '/settings', subtitle: '用户中心与模板' },
  { title: '系统日志', icon: 'mdi-notebook-outline', to: '/system', subtitle: '操作轨迹与运行状态' },
]
