import type { MenuItem } from '@/types'

export const mainMenu: MenuItem[] = [
  { title: '总览', icon: 'mdi-view-dashboard-outline', to: '/dashboard', subtitle: '查看创作进度与项目概况' },
  { title: '项目管理', icon: 'mdi-bookshelf', to: '/projects', subtitle: '维护项目列表与项目详情' },
  { title: '章节管理', icon: 'mdi-file-document-edit-outline', to: '/chapters', subtitle: '维护章节顺序、标题与正文' },
  { title: '人物管理', icon: 'mdi-account-group-outline', to: '/characters', subtitle: '维护角色卡与项目人物定位' },
  { title: '世界观管理', icon: 'mdi-earth', to: '/world-settings', subtitle: '维护世界规则、地点、势力与历史' },
  { title: '大纲管理', icon: 'mdi-format-list-bulleted-square', to: '/outlines', subtitle: '维护项目总纲、章节大纲与写作节拍' },
  { title: '剧情管理', icon: 'mdi-source-branch', to: '/plots', subtitle: '整理剧情线、冲突与推进节点' },
  { title: '因果管理', icon: 'mdi-graph-outline', to: '/causality', subtitle: '维护因果链条与触发条件' },
  { title: '写作中心', icon: 'mdi-feather', to: '/writing', subtitle: '结合 AI 生成、续写、改写与润色' },
  { title: '知识库', icon: 'mdi-database-search-outline', to: '/rag', subtitle: '管理知识条目与检索结果' },
  { title: '模型服务', icon: 'mdi-robot-outline', to: '/providers', subtitle: '配置 Ollama 与兼容模型接口', adminOnly: true },
  { title: '账号管理', icon: 'mdi-account-cog-outline', to: '/accounts', subtitle: '管理账号、重置密码与解锁', adminOnly: true },
  { title: '系统设置', icon: 'mdi-cog-outline', to: '/settings', subtitle: '管理模型、提示词与安全策略', adminOnly: true },
  { title: '系统状态', icon: 'mdi-notebook-outline', to: '/system', subtitle: '查看运行状态与操作轨迹', adminOnly: true },
]
