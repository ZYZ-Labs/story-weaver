import type { MenuSection } from '@/types'

export const mainMenuSections: MenuSection[] = [
  {
    key: 'workbench',
    title: '创作台',
    subtitle: '先做当前动作，再下沉到对象细页',
    items: [
      { title: '创作工作台', icon: 'mdi-view-dashboard-outline', to: '/workbench', subtitle: '聚焦当前项目、当前章节与下一步动作' },
      { title: '写作中心', icon: 'mdi-feather', to: '/writing', subtitle: '进入正文编写、摘要流与 AI 续写' },
      { title: '项目总览', icon: 'mdi-bookshelf', to: '/projects', subtitle: '查看项目列表、切换当前项目与进入项目详情' },
    ],
  },
  {
    key: 'story',
    title: '故事台',
    subtitle: '维护故事结构和对象，但不再作为默认主入口',
    items: [
      { title: '章节', icon: 'mdi-file-document-edit-outline', to: '/chapters', subtitle: '维护章节顺序、标题与摘要' },
      { title: '人物', icon: 'mdi-account-group-outline', to: '/characters', subtitle: '维护人物摘要、定位与关键状态' },
      { title: '世界观', icon: 'mdi-earth', to: '/world-settings', subtitle: '维护规则、地点、势力与背景设定' },
      { title: '大纲', icon: 'mdi-format-list-bulleted-square', to: '/outlines', subtitle: '维护项目总纲、卷纲与章节目标' },
      { title: '剧情', icon: 'mdi-source-branch', to: '/plots', subtitle: '整理剧情线、冲突和推进节点' },
      { title: '因果', icon: 'mdi-graph-outline', to: '/causality', subtitle: '维护因果链和触发条件' },
    ],
  },
  {
    key: 'state',
    title: '状态台',
    subtitle: '观察读者已知、章节状态与角色运行时状态',
    items: [
      { title: '状态总览', icon: 'mdi-chart-timeline-variant', to: '/state-center', subtitle: '查看章节状态、揭晓状态与角色运行时状态' },
    ],
  },
  {
    key: 'generation',
    title: '生成台',
    subtitle: '查看骨架、编排、trace 与知识回流',
    items: [
      { title: '生成总览', icon: 'mdi-robot-happy-outline', to: '/generation-center', subtitle: '查看骨架、编排预览、章节审校与 trace' },
      { title: '知识库', icon: 'mdi-database-search-outline', to: '/rag', subtitle: '管理知识条目与检索结果' },
    ],
  },
  {
    key: 'system',
    title: '系统台',
    subtitle: '系统配置、账号与运维能力',
    items: [
      { title: '模型服务', icon: 'mdi-robot-outline', to: '/providers', subtitle: '配置 Ollama 与兼容模型接口', adminOnly: true },
      { title: '账号管理', icon: 'mdi-account-cog-outline', to: '/accounts', subtitle: '管理账号、重置密码与解锁', adminOnly: true },
      { title: '系统设置', icon: 'mdi-cog-outline', to: '/settings', subtitle: '管理模型、提示词与安全策略', adminOnly: true },
      { title: '系统状态', icon: 'mdi-notebook-outline', to: '/system', subtitle: '查看运行状态与操作轨迹', adminOnly: true },
    ],
  },
]
