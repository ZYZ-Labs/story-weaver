# Helix Runtime 作为 Story Weaver 外部 Runtime 的重构方案

- Doc ID: ARCH-20260419-helix-runtime-adapter-refactor-v1
- Created At: 2026-04-19 Asia/Shanghai
- Target:
  - [helix-runtime](/usr/local/project/github/helix-runtime)
  - `story-weaver` 与 `helix-runtime` 的外部适配接入

## 目标

不是把 `helix-runtime` 改造成 `story-weaver` 内核。  
而是把它重构成一个可以安全挂在 `story-weaver` 外侧的：

- `Agent Runtime`
- `MCP Gateway`
- `Provider Runtime`

一句话：

- `story-weaver` 管故事
- `helix-runtime` 管外部 AI 运行时

## 重构前提

必须先定死这条边界：

- `story-weaver` 是故事域唯一真源
- `helix-runtime` 不直接管理故事真状态
- `helix-runtime` 只能通过 adapter 读写 `story-weaver`

## 当前不适合直接接入的原因

当前 `helix-runtime` 的几个核心对象都偏“通用对话 runtime”：

- `Session`
- `SessionState`
- `Message`
- `WorkflowRuntime`

缺少这些故事域桥接概念：

- `projectId`
- `chapterId`
- `sceneId`
- `StoryUnitRef`
- `SceneExecutionState`
- `ReaderRevealState`
- `ChapterIncrementalState`

所以必须先补一层 `Story Weaver Adapter Layer`。

## 目标形态

### 重构后职责

`story-weaver`

- 持有：
  - `StoryUnit`
  - `StoryPatch`
  - `StorySnapshot`
  - `StoryEvent`
  - `ReaderRevealState`
  - `ChapterIncrementalState`
  - `SceneExecutionState`
  - `ChapterSkeleton`
  - `StorySessionPreview`

`helix-runtime`

- 持有：
  - Provider Router
  - MCP Transport
  - Session/Message 管理
  - Tool Registry
  - 通用运行日志
  - 外部 agent 会话桥接

### 理想调用链

1. 外部 AI 工具调用 `helix-runtime` MCP
2. `helix-runtime` 通过 adapter 读取 `story-weaver`
3. `story-weaver` 返回：
   - `story-context`
   - `story-orchestration`
   - `story-state`
4. `helix-runtime` 再把这些结果包装为受控工具输出
5. 如果需要写入，也只走显式受控写接口

## 必须新增的适配层

### 1. StoryWeaverApiClient

新增一个明确的 API client，负责访问：

- `/api/story-context/**`
- `/api/story-orchestration/**`
- `/api/story-state/**`

要求：

- token 注入
- 超时控制
- 错误码标准化
- 可重试边界

### 2. StoryContextBinding

为 `helix` 的 session 增加故事绑定上下文：

- `project_id`
- `chapter_id`
- `scene_id`
- `binding_mode`
- `selected_story_unit_refs`

注意：

- 这是“桥接上下文”
- 不是把完整故事状态复制进 helix

### 3. StoryToolRegistry

新增故事域工具注册层，而不是继续只暴露通用会话工具。

第一批工具建议：

- `sw_get_project_brief`
- `sw_get_reader_known_state`
- `sw_get_character_runtime_state`
- `sw_get_recent_story_progress`
- `sw_get_skeleton_preview`
- `sw_get_orchestration_preview`
- `sw_get_chapter_review`
- `sw_get_chapter_state`

第二批工具再考虑受控写：

- `sw_execute_scene`
- `sw_get_chapter_events`
- `sw_get_chapter_snapshots`
- `sw_get_chapter_patches`

## 必须修改的 Helix 内部结构

### 重构 1：SessionState 允许扩展绑定域

当前 `SessionState` 太窄，只表达：

- topic
- task
- workflow step

建议改成两层：

- `runtime_state`
- `external_binding`

至少要能承载：

- `story_binding.project_id`
- `story_binding.chapter_id`
- `story_binding.scene_id`
- `story_binding.bound_tools`

### 重构 2：WorkflowRuntime 不再只面向 document/revision

不要强行让 `story-weaver` 适配 `document/revision`。  
应该让 `helix-runtime` 的 workflow runtime 变成“通用 steps executor”，由 story adapter 决定怎么调。

建议增加：

- `external_workflow`
- `tool_chain_workflow`

### 重构 3：MCP Tools 从“固定列表”转为“注册表”

现在的：

- `create_session`
- `get_session_state`
- `switch_provider`

太静态。

建议改成：

- 核心工具注册表
- 外部域工具注册表
- 风险等级与权限声明

这样 `story-weaver` 相关工具才能优雅挂进去。

### 重构 4：Storage 分层

当前 `MemoryStorage / SQLiteStorage` 存的是：

- session
- messages
- provider config

建议明确不要把故事域状态落进这里。  
只保留：

- runtime session
- tool audit trail
- adapter call trace

## 推荐实施阶段

### Stage A：边界重构

目标：

- 把 `helix-runtime` 从“通用小产品”重构为“可适配 runtime”

动作：

- 抽 `StoryWeaverApiClient`
- 扩 `SessionState`
- 引入 `StoryContextBinding`
- 抽 `StoryToolRegistry`

### Stage B：只读接入

目标：

- 先接 story-context / orchestration preview / state 查询

动作：

- 接：
  - project brief
  - reader known state
  - chapter skeleton preview
  - orchestration preview
  - chapter review
  - chapter state

### Stage C：受控写接入

目标：

- 只接非常明确的受控写动作

动作：

- `execute scene`
- `query events/snapshots/patches`

注意：

- 不要一开始让 helix 直接拥有“自由写故事状态”的能力

### Stage D：外部 AI 工具开放

目标：

- 给 Claude Code / 其他 MCP 客户端暴露故事域工具

动作：

- 文档
- schema version
- 超时/重试/错误模型
- allowlist

## 不建议做的事

### 1. 不要直接把 story-weaver 状态复制进 helix SQLite

这样会形成第二真源。

### 2. 不要让 helix 直接替换 story-weaver 的 orchestrator

现在它没有故事域编排模型。

### 3. 不要先做深写再做只读

正确顺序一定是：

- 只读
- 预览
- 受控写

## 对 Story Weaver 的影响

如果按这个方案接，对 `story-weaver` 的要求其实很清楚：

- 保持现有：
  - `story-context`
  - `story-orchestration`
  - `story-state`
- 后续在 `Phase 9 ~ 10` 再补：
  - 更稳定的 schema
  - 更清晰的错误码
  - 更清晰的 auth/token 策略

也就是说：

- 不需要为了接 helix 推翻当前主线
- 只需要把当前 API 继续产品化

## 复杂度判断

如果走推荐路线：

- 接入难度：`中等`
- 直接深度融合难度：`高`

更准确的说：

- “把 helix 作为外部 runtime 接起来”这件事可做
- “把 helix 改成 story-weaver 的核心引擎”不值得

## 最终建议

建议路线：

1. `story-weaver` 继续按当前 `Phase 8 ~ 10` 主线推进  
2. 不打断当前主线，不强行并入 `helix-runtime`  
3. 未来如果接，按“外部 runtime adapter”模式接  
4. 先重构 `helix-runtime`，再做只读联通

一句话结论：

**Helix Runtime 值得接，但应该作为外挂运行时接，不应该硬塞进 Story Weaver 内核。**

## 贡献与署名说明

- “把 `helix-runtime` 接入 `story-weaver`”的方向与需求由用户提出
- 接入方式判断、风险分层、重构阶段设计与本文档撰写由 Codex 完成
