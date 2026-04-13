# Story 平台升级 Phase 4 详细实施计划：Read-Only MCP 与 LSP 基础设施

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Plan ID: PLAN-REQ-20260411-stateful-story-platform-upgrade-phase4-readonly-mcp-lsp-v1
- Status: Draft
- Created At: 2026-04-13 Asia/Shanghai
- Updated At: 2026-04-13 Asia/Shanghai

## 本轮目标

在 `Phase 3` 已完成 `Summary First` 对象工作流的基础上，开始建设只读 `MCP / LSP` 基础设施，为后续多 session 编排、章节骨架、镜头执行和状态交接提供稳定的外部上下文读取能力。

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

### `Phase 4.3` 工具化出口与最小联调

目标：

- 把首批查询服务暴露成可供编排层消费的工具化出口

交付：

- 首批只读工具入口
- 标准异常口径
- 最小联调样例

退出条件：

- 应用层可通过“工具调用风格”读取上下文，而不是继续直接拼 repository 查询

### `Phase 4.4` 收口与下一阶段准备

目标：

- 明确 `Phase 5` 的多 session 编排如何消费这些只读上下文

交付：

- 与 `Phase 5` 的接口边界说明
- 测试结论
- 进度和 agent context 更新

退出条件：

- `Phase 5` 可直接基于这批只读能力进入实现

## 首批只读工具建议

- `get_project_brief`
- `get_story_unit_summary`
- `get_chapter_anchor_bundle`
- `get_reader_known_state`
- `get_character_runtime_state`
- `get_recent_story_progress`

## 建议代码落点

- 协议层：
  - `story-storyunit`
- 应用层查询服务：
  - `backend`
- 后续工具模块承载位：
  - `story-mcp`
  - `story-lsp`

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

- 首批只读工具协议已冻结
- 统一查询服务已落地
- 至少一条真实上下文读取链已完成联调
- 主进度和 agent context 已切到 `Phase 4`

## 贡献与署名说明

- “先读稳定，再做多 session”的方向：用户与 Codex 共同确认。
- `Phase 4` 的实施拆分、工具清单、服务边界和退出条件：Codex 编写。
