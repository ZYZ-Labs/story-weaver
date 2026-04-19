# Story 平台升级 Phase 8 详细实施计划：前端信息架构重构

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Plan ID: PLAN-REQ-20260411-stateful-story-platform-upgrade-phase8-frontend-information-architecture-v1
- Status: In Progress
- Created At: 2026-04-18 Asia/Shanghai
- Updated At: 2026-04-19 Asia/Shanghai

## 本轮目标

在 `Phase 3` 已落下 `Summary First` 入口、`Phase 4 ~ 7` 已提供稳定后端状态协议之后，正式开始前端信息架构重构。

`Phase 8` 的目标不是再加几个按钮，而是把当前偏后台的对象页改造成真正的创作工作台。

一句话说：

- `Phase 7` 提供状态与编排基线
- `Phase 8` 让这些能力变成作者真正能用的界面

## 本轮原则

- 先重构结构，不先追求视觉 polish
- 默认优先普通作者心智，不默认暴露系统词
- 继续坚持 `Summary First`
- 普通模式默认只暴露当前创作动作
- 专家模式作为信息密度开关，不作为权限隔离
- 所有新页面优先消费已稳定的后端协议：
  - `summary-workflow`
  - `story-context`
  - `story-orchestration`
  - `story-state`

## 范围内

- 顶层导航重构
- 工作台页结构
- 章节工作区
- 状态面板
- 生成面板
- 普通模式 / 专家模式分层

## 范围外

- 不在本阶段重做全部视觉主题
- 不在本阶段引入复杂拖拽编排
- 不在本阶段一次性替换所有旧对象页
- 不在本阶段开放完整状态编辑器

## 分阶段实施拆分

### `Phase 8.1` 顶层导航与入口收口

目标：

- 固定顶层 IA：
  - 创作台
  - 故事台
  - 状态台
  - 生成台
  - 系统台
- 把当前分散页面的主入口重新归位

退出条件：

- 用户进入系统后，主路径不再是对象列表，而是创作动作入口

### `Phase 8.2` 章节工作区重构

目标：

- 把现有写作页升级为 `Chapter Workspace`
- 统一：
  - 骨架
  - 镜头
  - 正文
  - 状态
  - trace

退出条件：

- 章节页不再只是正文编辑，而是真正的镜头级工作区

### `Phase 8.3` 对象页 Summary First 重构

目标：

- 人物 / 世界观 / 章节对象页不再以字段墙为主
- 统一成：
  - `Summary`
  - `Canon`
  - `State`
  - `History`

退出条件：

- 普通作者默认只看到摘要和变化说明

### `Phase 8.4` 状态面板与生成面板接入

目标：

- 接入：
  - `story-state`
  - `story-context`
  - `story-orchestration`
- 提供：
  - 状态总览
  - reveal 总览
  - orchestration trace

退出条件：

- 专家模式下可以稳定观察状态链和多 session 编排结果

### `Phase 8.5` 浏览器级验收与旧入口降级

目标：

- 浏览器真实点击验收
- 明确哪些旧入口继续保留
- 明确哪些旧入口降级为二级页

退出条件：

- 前端主入口已切换到新信息架构

## 当前阶段判断

- `Phase 7` 已完成
- `Phase 8` 已启动
- `Phase 8.1` 已完成本地开发并通过：
  - `npm run type-check`
  - `npm run build`
- 当前已落：
  - `/workbench`
  - `/state-center`
  - `/generation-center`
  - 分组导航和旧入口降级
- `Phase 8.1` 已完成第一轮部署与数据联调：
  - 新静态资源已确认上线
  - `创作台 / 状态台 / 生成台` 依赖后端接口均已返回 `200`
  - 当前剩余问题是浏览器烟测认证注入链路，不是页面实现失败
- `Phase 8.1` 已完成人工页面验收与收口修复：
  - `创作台 / 状态台 / 生成台 / 旧对象页二级入口` 已人工验证通过
  - `生成台` 顶部 `sceneId` 输入框压缩问题已修复
- `Phase 8.2` 已完成本地开发并通过：
  - 已新增 `/chapter-workspace`
  - `创作台` 主按钮已改为 `打开章节工作区`
  - `章节管理` 行操作与预览区已新增 `工作区 / 打开工作区`
  - 已统一接入：
    - 正文编辑
    - 章节骨架
    - 当前镜头执行
    - 章节状态与揭晓
    - 章节 trace 与审校
  - 已补齐章节工作区镜头操作闭环：
    - 当前镜头可编辑
    - `PLANNED` 镜头可删除
    - 可直接基于当前镜头生成初稿或继续生成
    - 生成草稿后可直接接受写回正文或拒绝
  - 后端已补齐章节骨架 override 能力：
    - `PUT /api/story-orchestration/projects/{projectId}/chapters/{chapterId}/skeleton-scenes/{sceneId}`
    - `DELETE /api/story-orchestration/projects/{projectId}/chapters/{chapterId}/skeleton-scenes/{sceneId}`
  - 已通过：
    - `mvn -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -DskipTests compile`
    - `mvn test -pl backend -am -Dtest=StorySessionOrchestrationControllerTest,RuleBasedChapterSkeletonPlannerTest,DefaultStorySessionOrchestratorTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2`
    - `npm run type-check`
    - `npm run build`
- 当前下一步是部署并联调 `Phase 8.2` 的 `Chapter Workspace`：
  - 验证镜头编辑/删除
  - 验证根据镜头直接生成初稿
  - 验证草稿接受/拒绝回写

## 建议代码落点

- 视图：
  - `front/src/views/*`
- 工作台相关新页面：
  - `front/src/views/workbench/*`
- 章节工作区：
  - `front/src/views/chapter/*`
  - `front/src/views/writing/*`
- 状态与生成面板：
  - `front/src/components/*Panel.vue`
- API 层：
  - `front/src/api/summary-workflow.ts`
  - `front/src/api/story-context.ts`
  - `front/src/api/story-orchestration.ts`
  - `front/src/api/story-state.ts`

## 验证方式

- `npm run type-check`
- `npm run build`
- 浏览器级真实点击验收
- 重点真实路径：
  - 从首页进入当前章节
  - 打开骨架/镜头
  - 查看状态与 reveal
  - 查看 trace
  - 切换普通/专家模式

## 风险

- 如果继续沿用旧后台布局，只是塞更多面板，前端会更乱
- 如果过早追求视觉效果，结构问题会被掩盖
- 如果不控制信息密度，专家模式会污染普通模式

## 下一步

1. 部署 `Phase 8.2` 前端改动
2. 联调 `Chapter Workspace`
3. 再推进对象页 `Summary First` 重构

## 贡献与署名说明

- “当前前端问题主要是结构而不是样式”的判断来自用户与 Codex 共同讨论
- 本文档的阶段拆分、实施顺序与工程化范围界定由 Codex 完成
