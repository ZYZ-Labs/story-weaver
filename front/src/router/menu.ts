import type { MenuItem } from '@/types'

export const mainMenu: MenuItem[] = [
  { title: '总览', icon: 'mdi-view-dashboard-outline', to: '/dashboard', subtitle: '总览创作进度' },
  { title: '项目管理', icon: 'mdi-bookshelf', to: '/projects', subtitle: '项目列表与详情' },
  { title: '章节管理', icon: 'mdi-file-document-edit-outline', to: '/chapters', subtitle: '章节结构与正文' },
  { title: '人物管理', icon: 'mdi-account-group-outline', to: '/characters', subtitle: '角色卡与设定' },
  { title: '剧情管理', icon: 'mdi-source-branch', to: '/plots', subtitle: '剧情线与冲突节点' },
  { title: '因果管理', icon: 'mdi-graph-outline', to: '/causality', subtitle: '因果节点与关联强度' },
  { title: '写作中心', icon: 'mdi-feather', to: '/writing', subtitle: '正文编辑与 AI 续写' },
  { title: '知识库', icon: 'mdi-database-search-outline', to: '/rag', subtitle: '知识条目与检索' },
  { title: '模型服务', icon: 'mdi-robot-outline', to: '/providers', subtitle: '主要使用 Ollama' },
  { title: '系统设置', icon: 'mdi-cog-outline', to: '/settings', subtitle: '默认策略与提示词模板' },
  { title: '系统状态', icon: 'mdi-notebook-outline', to: '/system', subtitle: '操作轨迹与运行状态' },
]
