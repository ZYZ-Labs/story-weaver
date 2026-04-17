# Story 平台升级 Phase 4 详细实施计划：Read-Only MCP 与 State Server 基础设施

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Plan ID: PLAN-REQ-20260411-stateful-story-platform-upgrade-phase4-readonly-mcp-state-server-v1
- Status: Completed
- Created At: 2026-04-13 Asia/Shanghai
- Updated At: 2026-04-17 Asia/Shanghai

## 本轮目标

在 `Phase 3` 已完成 `Summary First` 对象工作流的基础上，开始建设只读 `MCP / State Server` 基础设施，为后续多 session 编排、章节骨架、镜头执行和状态交接提供稳定的外部上下文读取能力。

本阶段只做三件事：

- 冻结首批只读工具与查询协议
- 在应用层建立可复用的只读查询服务
- 为未来 `Phase 5 / Phase 6` 提供统一快照入口

一句话说：

- 先把上下文读稳定
- 暂不在本阶段引入复杂写回

## 当前阶段定位

- 这是 `Phase 4` 的详细实施计划文档
- `Phase 4` 以前置“只读优先”为原则
- 本阶段不追求完整 MCP Server 生态，只追求稳定读模型与工具化出口

## Phase 4 开场动作

在正式进入只读 `MCP / State Server` 查询协议之前，先完成工程收口：

- 将后端共享 Maven 模块从项目根目录统一迁入 `backend/modules/*`
- 保持根工程聚合入口不变，继续使用根 `pom.xml` 统一构建
- 同步修正根 `pom.xml`、子模块 parent 相对路径和 `backend/Dockerfile`

当前已完成：

- `backend/modules/story-domain`
- `backend/modules/story-storyunit`
- `backend/modules/story-generation`
- `backend/modules/story-provider`
- `backend/modules/story-web`
- `backend/modules/story-infra`
- 根工程 `mvn -DskipTests compile` 已通过
- `backend/modules/story-storyunit/src/main/java/com/storyweaver/storyunit/context/*` 已落下首批只读视图与统一查询接口
- `backend/src/main/java/com/storyweaver/storyunit/context/impl/*` 已落下 `Phase 4.2` 首批查询实现骨架

这一步不改变模块职责，只为 `Phase 4.1` 的只读查询服务和未来 `story-mcp / story-lsp` 模块落位打底。

## 本轮原则

- `read-only first`
- 优先复用 `StoryUnit` 与现有读模型
- 上下文读取必须短、稳、可追踪
- 不允许重新回到“大 prompt 临时拼接”模式
- 工具输出必须结构化且面向编排层

## 范围内

- 首批只读工具协议
- `StoryContextQueryService` 族
- 统一查询结果对象与错误口径
- 首批读取对象：
  - `project brief`
  - `story unit summary`
  - `chapter anchor bundle`
  - `reader known state`
  - `character runtime state`
  - `recent story progress`
- 文档、测试与收口标准

## 范围外

- 不在本阶段做写入型 MCP 工具
- 不在本阶段做真正的多 session 编排
- 不在本阶段做章节骨架执行
- 不在本阶段做状态增量写回
- 不在本阶段做 Items / Skills / Factions 全量运行时状态

## 分阶段实施拆分

### `Phase 4.1` 协议冻结与工具清单定稿

目标：

- 冻结首批只读工具清单
- 定义统一请求/响应模型

交付：

- `ProjectBriefView`
- `StoryUnitSummaryView`
- `ChapterAnchorBundleView`
- `ReaderKnownStateView`
- `CharacterRuntimeStateView`
- `RecentStoryProgressView`
- 工具名与参数约定

退出条件：

- 后端、前端、编排层都基于同一批查询对象讨论，不再各自拼上下文

### `Phase 4.2` 应用层查询服务落地

目标：

- 在现有模块化单体中建立统一只读查询服务

交付：

- `StoryContextQueryService`
- `ProjectBriefQueryService`
- `StoryUnitSummaryQueryService`
- `ChapterAnchorQueryService`
- `ReaderKnownStateQueryService`
- `RecentStoryProgressQueryService`

退出条件：

- 首批上下文可由服务端统一读取，不再散落在 controller 或 prompt builder 中

当前进展：

- 已实现：
  - `DefaultProjectBriefQueryService`
  - `DefaultStoryUnitSummaryQueryService`
  - `DefaultChapterAnchorBundleQueryService`
  - `DefaultReaderKnownStateQueryService`
  - `DefaultCharacterRuntimeStateQueryService`
  - `DefaultRecentStoryProgressQueryService`
  - `DefaultStoryContextQueryService`
- 已完成根工程编译验证
- 已完成首批 service 级测试：
  - `DefaultProjectBriefQueryServiceTest`
  - `DefaultStoryUnitSummaryQueryServiceTest`
  - `DefaultStoryContextQueryServiceTest`
- 尚未开始：
  - 对外工具化出口

### `Phase 4.3` 工具化出口与最小联调

目标：

- 把首批查询服务暴露成可供编排层消费的工具化出口

交付：

- 首批只读工具入口
- 标准异常口径
- 最小联调样例

退出条件：

- 应用层可通过“工具调用风格”读取上下文，而不是继续直接拼 repository 查询

当前进展：

- 已新增最小只读查询出口：
  - `GET /api/story-context/projects/{projectId}/brief`
  - `GET /api/story-context/story-units/summary`
  - `GET /api/story-context/projects/{projectId}/chapters/{chapterId}/anchors`
  - `GET /api/story-context/projects/{projectId}/chapters/{chapterId}/reader-known-state`
  - `GET /api/story-context/projects/{projectId}/characters/{characterId}/runtime-state`
  - `GET /api/story-context/projects/{projectId}/progress`
- 对应 controller：
  - `backend/src/main/java/com/storyweaver/controller/StoryContextController.java`
- 已完成根工程编译与首批 service 级测试回归
- 已完成首批 controller 级回归：
  - `backend/src/test/java/com/storyweaver/controller/StoryContextControllerTest.java`
- 已完成首轮真实部署联调：
  - `project brief` 通过
  - `story unit summary` 通过
  - `chapter anchor bundle` 通过
  - `reader known state` 通过
  - `recent story progress` 通过
  - `character runtime state` 发现真实空值缺陷并触发 `HTTP 500`
- 已完成本地修复：
  - `DefaultCharacterRuntimeStateQueryService` 已修复 `List.of(null)` 导致的 `NullPointerException`
  - 已补 `DefaultCharacterRuntimeStateQueryServiceTest`
- 当前剩余项：
  - 部署修复版后复验 `runtime-state`

### `Phase 4.4` 收口与下一阶段准备

目标：

- 明确 `Phase 5` 的多 session 编排如何消费这些只读上下文

交付：

- 与 `Phase 5` 的接口边界说明
- 测试结论
- 进度和 agent context 更新

退出条件：

- `Phase 5` 可直接基于这批只读能力进入实现

当前收口结论：

- `Phase 4` 的开发侧主链已经齐备：
  - 只读视图合同
  - 统一查询服务
  - 最小工具化出口
  - service / controller 两层回归
- `Phase 5` 应优先消费：
  - `StoryContextQueryService`
  - `/api/story-context/*` 只读出口
  - 首批固定工具名：
    - `get_project_brief`
    - `get_story_unit_summary`
    - `get_chapter_anchor_bundle`
    - `get_reader_known_state`
    - `get_character_runtime_state`
    - `get_recent_story_progress`
- 已完成首轮真实部署联调并发现 `runtime-state` 空值缺陷
- 已完成修复版部署后二次联调：
  - `GET /api/story-context/projects/28/brief` -> `200`
  - `GET /api/story-context/story-units/summary` -> `200`
  - `GET /api/story-context/projects/28/chapters/31/anchors` -> `200`
  - `GET /api/story-context/projects/28/chapters/31/reader-known-state` -> `200`
  - `GET /api/story-context/projects/28/characters/15/runtime-state` -> `200`
  - `GET /api/story-context/projects/28/progress` -> `200`
- 当前已无 `Phase 4` 开发缺口，可切入 `Phase 5`

## 首批只读工具建议

- `get_project_brief`
- `get_story_unit_summary`
- `get_chapter_anchor_bundle`
- `get_reader_known_state`
- `get_character_runtime_state`
- `get_recent_story_progress`

## 建议代码落点

- 协议层：
  - `backend/modules/story-storyunit`
- 应用层查询服务：
  - `backend`
- 后续工具模块承载位：
  - `backend/modules/story-mcp`
  - `backend/modules/story-lsp`

## 测试与验证

- 每个只读视图至少一组 service 级测试
- 至少一组项目级组合查询测试
- 至少一组真实项目样本联调

## 风险清单

### 风险 1. 再次退回“散查询 + 大 prompt”

规避：

- 首批工具清单必须冻结
- 后续新上下文优先接到统一查询服务，而不是散插旧链路

### 风险 2. 读模型过厚

规避：

- 每个工具只返回“当前阶段真的需要的最小上下文”
- 不把完整业务实体直接暴露给编排层

### 风险 3. 过早引入写回复杂度

规避：

- 本阶段只做只读
- 写回延后到后续状态层阶段

## 当前阶段出口条件

- 后端共享模块已统一收拢到 `backend/modules/*`
- 首批只读工具协议已冻结
- 统一查询服务已落地
- 已完成本地 service / controller 回归
- 至少一条真实上下文读取链已完成联调
- 主进度和 agent context 已切到 `Phase 4`

## 贡献与署名说明

- “先读稳定，再做多 session”的方向：用户与 Codex 共同确认。
- `Phase 4` 的实施拆分、工具清单、服务边界和退出条件：Codex 编写。
