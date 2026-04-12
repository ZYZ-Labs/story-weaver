# Story 平台级架构升级 实施计划

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Plan Version: v1
- Status: In Progress
- Created At: 2026-04-11 Asia/Shanghai
- Updated At: 2026-04-12 Asia/Shanghai

## 计划定位

本计划不再服务于单一功能或单一链路修补，而是用于指导未来 6 到 12 周的平台级升级工作。目标是在保持单部署、单 JAR 的前提下，把当前系统升级为可持续演进的模块化单体创作平台。

## 总体阶段

### Phase 0. 文档建制与术语冻结

目标：

- 建立新的主需求、主计划、主进度。
- 定义统一术语：
  - `StoryUnit`
  - `Facet`
  - `Patch`
  - `Snapshot`
  - `StoryEvent`
  - `Scene`
  - `Skeleton`
  - `Reader Reveal State`
- 建立文档治理规则与计划拆分制度。

交付物：

- Requirement
- Master Plan
- Progress
- 元模型文档
- 文档治理文档

退出条件：

- 后续所有开发都可基于统一术语进行讨论与编码。

### Phase 1. 模块化单体拆分

目标：

- 从当前单模块应用演进到模块化单体。

当前拆分节奏：

- `Phase 1A`
  - 包先细分
  - 协议先行
- `Phase 1B`
  - 粗粒度模块拆分
  - 已完成

`Phase 1B` 内部分段：

- `1B.1`
  - 抽出低耦合共享模块，让根工程先站住
- `1B.2`
  - 统一活动文档、构建入口和部署口径
- `1B.3`
  - 冻结下一批粗模块边界，仍归属 `Phase 1B`

当前已完成：

- `1B.1`
  - `story-domain`
  - `story-storyunit`
  - 根 Maven 聚合工程
  - `backend` 作为应用壳保留
- `1B.3` 已启动的部分
  - `story-generation`
  - generation 顶层合同层
  - generation 相关 request DTO
  - `generation.impl` 仍暂留 `backend`
  - `story-provider`
  - `AIProviderService`
  - `ProviderDiscoveryVO`
  - `AIProviderServiceImpl / AIModelRoutingService` 仍暂留 `backend`
  - `story-web`
  - `ApiResponse / ApiErrorResponse`
  - controller / handler 仍暂留 `backend`
  - `story-infra`
  - `repository.* / item mapper`
  - config / security / mapper resources 仍暂留 `backend`

当前未完成但仍属于 `Phase 1B`：

当前判断：

- `Phase 1` 已完成
- `Phase 1B` 的范围只包含粗粒度模块拆分与边界冻结
- 后续实现层迁移不再归属 `Phase 1`

建议模块：

- `story-weaver-boot`
- `story-domain`
- `story-canon`
- `story-state`
- `story-generation-orchestrator`
- `story-director`
- `story-writer`
- `story-reviewer`
- `story-mcp-server`
- `story-lsp-server`
- `story-provider`
- `story-settings`
- `story-web`
- `story-infra`

退出条件：

- 模块边界、依赖方向、打包方式和迁移顺序被明确写入架构文档。

### Phase 2. StoryUnit 与 Facets 元模型落地

目标：

- 建立统一的基础读写单位。
- 让未来所有 MCP/LSP 调用和状态承接都围绕统一协议进行。

核心对象：

- `StoryUnit`
- `SummaryFacet`
- `CanonFacet`
- `StateFacet`
- `RelationFacet`
- `RevealFacet`
- `ExecutionFacet`
- `StoryPatch`
- `StorySnapshot`
- `StoryEvent`

退出条件：

- 至少人物、世界观、章节三个对象族具备统一的 unit + facet 映射定义。

当前状态：

- 已启动
- 详细计划：
  - `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-phase2-storyunit-storage-v1.md`
- `Phase 2.1` 已完成
- `Phase 2.2` 核心实现已完成
- 下一步进入 `Phase 2.3`

### Phase 3. Summary First 对象工作流

目标：

- 重做人物、世界观、物品、技能、剧情等对象的创建和修改方式。

默认流程：

1. 用户输入摘要
2. AI 拆成结构 patch
3. 系统校验
4. 用户确认摘要变化
5. 写入 facet
6. 系统回生成标准摘要

退出条件：

- 前端默认不再要求用户直接面对字段墙。

### Phase 4. Read-Only MCP 与 LSP 基础设施

目标：

- 先做稳定读取，再做写回。

第一批只读工具：

- `get_project_brief`
- `get_story_unit_summary`
- `get_chapter_anchor_bundle`
- `get_reader_known_state`
- `get_character_runtime_state`
- `get_inventory_state`
- `get_skill_state`
- `get_recent_story_progress`

第一批写入工具：

- `save_generation_snapshot`
- `append_story_event`
- `append_scene_outcome`
- `create_pending_unit`

退出条件：

- 应用编排层可通过 MCP/LSP 稳定读取上下文，而不是继续拼超长 prompt。

### Phase 5. 多 Session 编排落地

目标：

- 建立四 session 编排：
  - 总导
  - 选择器
  - 写手
  - 审校器

关键原则：

- 每个 session 只承担单一职责。
- 不共享长聊天历史。
- 共享统一快照和结构化状态。
- 除写手外尽量输出结构化结果。

退出条件：

- session 间的输入输出协议可回放、可追踪、可重试。

### Phase 6. 章节骨架与镜头执行

目标：

- 把单章从“整章一把写”改成“骨架 + 镜头”。

推荐模式：

1. 章节总导产出 3-5 个镜头候选骨架
2. 选择器确定骨架
3. 写手逐镜头执行
4. 每镜头结束写回 `SceneExecutionState`
5. 全章结束后章节级审校

退出条件：

- 镜头之间通过显式 handoff state 交接，而不是隐式承接。

### Phase 7. 增量状态系统

目标：

- 承接故事运行过程中的变化，而不是只存静态 canon。

优先级较高的 state：

- 背包
- 技能状态
- 地点状态
- 人物态度
- 关系变化
- 好感度
- 世界影响
- Open Loops
- Reader Reveal State

退出条件：

- 关键状态变化可通过 event + patch 方式写回与回放。

### Phase 8. 前端信息架构重构

目标：

- 把当前后台式页面重构为创作工作台。

顶层入口建议：

- 创作台
- 故事台
- 状态台
- 生成台
- 系统台

退出条件：

- 普通作者默认只看到摘要和当前创作动作。
- 专家模式可展开结构与调试层。

### Phase 9. 迁移、兼容与回填

目标：

- 兼容现有数据与主链。

重点：

- 旧实体映射为新 `StoryUnit`
- 旧摘要回填
- 旧记录迁移为 `Snapshot / Event`
- 双写和灰度切换

退出条件：

- 新旧主链可并存一段时间，直到新主链稳定接管。

### Phase 10. 测试、观测与回放

目标：

- 建立平台级验证体系。

必须覆盖：

- 固定样本回放
- 镜头级回放
- 多 session trace
- 状态一致性检查
- Reader Reveal 边界验证
- 迁移正确性验证

退出条件：

- 关键项目样本可重复回放并定位问题。

## 详细实施规则

### 规则 1. 每次编码前必须新增本轮详细计划文档

每次开发进入编码前，至少补充：

- 本轮目标
- 涉及模块
- 读写边界
- 风险
- 验证策略
- 回滚策略

### 规则 2. 每个 Epic 默认拆成 3-8 个子计划

- 不允许直接拿总计划进入实现。
- 必须拆到“本轮足够完成并可验证”的粒度。

### 规则 3. 文档和代码并行推进

- 没有文档冻结的边界，不进入大规模代码改造。
- 没有 progress 更新，不结束本轮开发。

## 当前优先顺序

1. 完成主需求、主计划、主进度与核心架构文档。
2. 明确 `StoryUnit + Facets` 元模型与模块边界。
3. 明确前端信息架构和 Summary First 交互。
4. 再进入第一轮可执行的编码子计划。

## 工作量评估

- 平台级最小可用升级：`6-8 周`
- 较完整版本：`8-12 周`
- 文档、迁移、回放、前后端联动全部覆盖的情况下，文件数和计划数都会显著增长。

## 贡献与署名说明

- 原始升级诉求、平台化方向、复杂度判断：用户提出。
- 阶段划分、模块拆分建议、实施顺序和计划文档撰写：Codex 完成。
- 开发流程制度化要求与执行方式：用户与 Codex 共同讨论形成。
