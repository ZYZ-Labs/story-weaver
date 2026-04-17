# Story 平台升级 Phase 1A 详细实施计划：协议先行与细分包边界骨架

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Plan ID: PLAN-REQ-20260411-stateful-story-platform-upgrade-phase1-foundation-v1
- Status: Completed
- Created At: 2026-04-11 Asia/Shanghai
- Updated At: 2026-04-12 Asia/Shanghai

## 本轮目标

在不立即打散现有单模块工程的前提下，先把未来平台升级最关键的协议骨架和细分包边界冻结下来，为后续的：

- 模块化单体拆分
- MCP / State Server 接入
- 多 session 编排
- Summary First 对象工作流

建立一层稳定基础。

本轮原则：

- 先定义协议和边界
- 再迁移已有业务
- 不在第一轮同时改动写作主链和对象主链

## 为什么先做这一步

如果一开始就直接拆 Maven 多模块、改 Controller、改对象页、改写作链路，风险太高。  
当前最合理的第一步是：

- 保持现有可运行代码结构
- 在其内部先建立“未来细模块的影子边界”
- 让后续所有开发都围绕统一协议继续推进

一句话说：

- 先做“内部架构线”
- 暂不做“大规模功能迁移线”

## 本轮范围

### 范围内

- 定义新的协议包与命名规则
- 落 `StoryUnit`、`Facet`、`Patch`、`Snapshot`、`StoryEvent` 的代码骨架
- 定义首批枚举、接口、基础值对象
- 定义旧实体到新协议层的 adapter 接口
- 定义 MCP/State Server 第一批 read service 接口
- 定义多 session 编排需要的最小输入输出协议

### 范围外

- 不立即执行粗粒度工程模块拆分
- 不立即改前端页面
- 不立即改数据库结构
- 不立即把写作主链切到新协议层
- 不立即实现 MCP/State Server 运行服务

## 当前实施快照

首批代码骨架已经完成并迁移到 `story-storyunit/src/main/java/com/storyweaver/storyunit/`，当前已完成：

- `model`
  - `StoryUnit`
  - `StoryUnitRef`
  - `StoryUnitVersion`
  - `StorySourceTrace`
  - `StoryUnitType`
  - `FacetType`
  - `StoryUnitStatus`
  - `StoryScope`
- `facet`
  - `SummaryFacet`
  - `CanonFacet`
  - `StateFacet`
  - `RelationFacet`
  - `RevealFacet`
  - `ExecutionFacet`
  - `ReaderRevealState`
- `patch`
  - `StoryPatch`
  - `PatchOperation`
  - `PatchStatus`
  - `PatchOperationType`
- `snapshot`
  - `StorySnapshot`
  - `SnapshotScope`
- `event`
  - `StoryEvent`
  - `StoryEventType`
- `adapter`
  - `StoryUnitAdapter`
  - `AbstractStoryUnitAdapter`
  - 五类实体 adapter 接口与默认骨架实现
- `service`
  - `StoryUnitQueryService`
  - `StorySnapshotQueryService`
  - `ReaderRevealStateQueryService`
  - `SceneExecutionStateQueryService`
- `session`
  - `SessionRole`
  - `DirectorCandidate`
  - `SelectionDecision`
  - `WriterExecutionBrief`
  - `ReviewDecision`
  - `SceneExecutionState`
  - 相关枚举与 issue/rejection 辅助协议

当前实现定位：

- 这是协议冻结和命名冻结，不是业务迁移
- 这批代码允许 `Phase 1B` 开始拆模块，也允许后续 generation / MCP / State Server 从统一契约起步
- 当前状态：
  - `Phase 1A` 已完成
  - 后续不再回到本阶段补零散协议命名

## 建议代码落点

在当前 `backend` 内先建立这些包：

- `com.storyweaver.storyunit`
- `com.storyweaver.storyunit.model`
- `com.storyweaver.storyunit.facet`
- `com.storyweaver.storyunit.patch`
- `com.storyweaver.storyunit.snapshot`
- `com.storyweaver.storyunit.event`
- `com.storyweaver.storyunit.adapter`
- `com.storyweaver.storyunit.service`
- `com.storyweaver.storyunit.session`

说明：

- 这是“未来细模块边界的包级预演”
- 后续真正拆模块时，这些包可整体迁移

## Phase 1A 与 Phase 1B 的关系

本阶段只做：

- 细分包
- 协议骨架
- 接口命名冻结

下一阶段 `Phase 1B` 将尽早进入：

- 粗粒度模块拆分

建议的粗粒度模块拆分顺序：

1. `story-weaver-boot`
2. `story-domain`
3. `story-web`
4. `story-infra`
5. `story-provider`
6. `story-generation`
7. `story-storyunit`
8. 预留 `story-mcp`
9. 预留 `story-lsp`

说明：

- 模块先粗拆
- 包先细分
- 后续再按成熟度继续细拆模块

## 具体任务拆分

### Task 1. 冻结核心枚举与类型名

需要落地：

- `StoryUnitType`
- `FacetType`
- `StoryUnitStatus`
- `PatchStatus`
- `SnapshotScope`
- `SessionRole`

验收：

- 所有术语不再散落在文档和代码里多套命名

### Task 2. 落 `StoryUnit` 外壳协议

需要落地：

- `StoryUnit`
- `StoryUnitRef`
- `StoryUnitVersion`
- `StorySourceTrace`

目标：

- 给后续所有对象提供统一引用壳

### Task 3. 落 Facet 协议

至少包括：

- `SummaryFacet`
- `CanonFacet`
- `StateFacet`
- `RelationFacet`
- `RevealFacet`
- `ExecutionFacet`

说明：

- 第一轮可以是 interface / record / VO 形式
- 不要求全部有存储实现

### Task 4. 落 Patch / Snapshot / Event 协议

需要落地：

- `StoryPatch`
- `PatchOperation`
- `StorySnapshot`
- `StoryEvent`
- `StoryEventType`

目标：

- 把未来状态变化、镜头交接、生成 trace 的承载层先定下来

### Task 5. 落 Adapter 接口

至少定义：

- `CharacterStoryUnitAdapter`
- `WorldSettingStoryUnitAdapter`
- `ChapterStoryUnitAdapter`
- `PlotStoryUnitAdapter`
- `CausalityStoryUnitAdapter`

说明：

- 第一轮只定义 adapter 接口和最小默认实现
- 不要求完成所有实体回填

### Task 6. 落只读服务接口

至少定义：

- `StoryUnitQueryService`
- `StorySnapshotQueryService`
- `ReaderRevealStateQueryService`
- `SceneExecutionStateQueryService`

目标：

- 给未来 MCP/State Server 读取侧固定接口

### Task 7. 落多 session 最小协议

至少定义：

- `DirectorCandidate`
- `SelectionDecision`
- `WriterExecutionBrief`
- `ReviewDecision`
- `SceneExecutionState`

目标：

- 给后续总导/选择器/写手/审校器编排先固定契约

### Task 8. 为粗粒度模块拆分做包级映射说明

需要在本轮文档或注释中明确：

- 哪些包未来归入 `story-storyunit`
- 哪些包未来归入 `story-generation`
- 哪些包未来归入 `story-provider`
- 哪些包未来归入 `story-web`
- 哪些包未来归入 `story-infra`

目标：

- 让 `Phase 1B` 拆模块时尽量以“搬运”为主，而不是再次重构命名和依赖

当前映射口径：

- `com.storyweaver.storyunit.*` 未来整体归入 `story-storyunit`
- `com.storyweaver.story.generation.*` 未来整体归入 `story-generation`
- `com.storyweaver.ai.*` 与 provider 兼容层未来优先拆入 `story-provider`
- `com.storyweaver.controller.*` 与 web DTO/VO 继续归入 `story-web`
- `com.storyweaver.repository.*`、MyBatis/配置/存储侧继续归入 `story-infra`

## 验证方式

### 代码级验证

- `mvn -DskipTests compile`
- `git diff --check`

当前结果：

- `mvn -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -DskipTests compile` 已通过
- `git diff --check` 已通过

### 架构级验证

- 新增协议包命名与文档术语一致
- 后续 Phase 1B 可以直接基于这些接口开始接旧实体

## 风险

- 如果第一轮把协议做得过厚，后面会难以收缩
- 如果第一轮协议过薄，后面又会重新散

因此本轮要求：

- 只冻结“必须统一”的东西
- 不抢跑做完整业务实现

## 退出条件

本轮完成后，应满足：

- 代码中已有统一的 `StoryUnit` 协议骨架
- 首批 facet、patch、snapshot、event 类型已存在
- 首批 adapter 和 query service 接口已存在
- 多 session 协议已在代码里可引用
- 已经为 `Phase 1B` 的粗粒度模块拆分准备好包边界映射
- 后续可以开始 `Phase 1B：粗粒度模块拆分`

## 与后续阶段关系

完成本计划后，下一份详细计划建议为：

- `Phase 1B：粗粒度模块拆分`

再下一步：

- `Phase 1C：旧实体到 StoryUnit 的只读映射与查询装配`

再下一步：

- `Phase 1D：MCP/State Server 只读接口与本地调试入口`

## 贡献与署名说明

- “模块先粗拆、包先细分”的实施方向：用户提出并由 Codex 收敛为阶段计划。
- 平台升级方向和需要先行冻结边界的诉求：用户提出。
- 本实施计划正文与任务拆分：Codex 完成。
