# Helix Runtime 接入 Story Weaver 可行性评估报告

- Report ID: REPORT-20260419-helix-runtime-integration-feasibility-v1
- Created At: 2026-04-19 Asia/Shanghai
- Scope:
  - 评估 [helix-runtime/README.md](/usr/local/project/github/helix-runtime/README.md)
  - 评估 [REQ-20260415-helix-runtime-core.md](/usr/local/project/github/helix-runtime/docs/requirements/REQ-20260415-helix-runtime-core.md)
  - 评估 [PLAN-REQ-20260415-helix-runtime-product-v4.md](/usr/local/project/github/helix-runtime/docs/plans/PLAN-REQ-20260415-helix-runtime-product-v4.md)
  - 结合当前 `story-weaver` 的 `Summary First / StoryUnit / State Server / 多 Session 编排` 主线

## 结论

结论分三层：

1. 不能直接内嵌接入  
`helix-runtime` 现在不是一个可直接嵌入 `story-weaver` 的“状态内核”或“故事编排内核”，而是一个偏通用的 `AI Session Runtime + Provider Router + MCP Server`。

2. 可以作为外部运行时接入  
如果把它定位成“外部 Agent Runtime / MCP Gateway / Provider Runtime”，接入是可行的，而且价值明确。

3. 如果想让它成为 `story-weaver` 的核心运行时，需要先重构  
直接把它拿来承接 `StoryUnit / SceneExecutionState / ChapterSkeleton / StoryPatch / StorySnapshot`，当前形态不够，需要先做适配层和边界重构。

我的判断：

- **直接融合进 `story-weaver` 主内核：不建议**
- **作为外部运行时服务接入：可行，推荐**
- **如果要深度融合，必须先重构：是**

## 我检查过的关键点

Helix Runtime 当前形态：

- MCP / API / Web 统一壳：
  - [main.py](/usr/local/project/github/helix-runtime/helix/main.py)
  - [server.py](/usr/local/project/github/helix-runtime/helix/mcp/server.py)
- 通用会话与状态：
  - [state.py](/usr/local/project/github/helix-runtime/helix/models/state.py)
  - [session.py](/usr/local/project/github/helix-runtime/helix/models/session.py)
  - [state_engine.py](/usr/local/project/github/helix-runtime/helix/core/state_engine.py)
- 通用上下文与工作流：
  - [context_manager.py](/usr/local/project/github/helix-runtime/helix/core/context_manager.py)
  - [workflow_runtime.py](/usr/local/project/github/helix-runtime/helix/core/workflow_runtime.py)
  - [capability_trigger.py](/usr/local/project/github/helix-runtime/helix/core/capability_trigger.py)
- Provider 与 MCP 工具：
  - [base.py](/usr/local/project/github/helix-runtime/helix/providers/base.py)
  - [functions.py](/usr/local/project/github/helix-runtime/helix/mcp/functions.py)
  - [handlers.py](/usr/local/project/github/helix-runtime/helix/mcp/handlers.py)
- 存储：
  - [memory.py](/usr/local/project/github/helix-runtime/helix/storage/memory.py)
  - [sqlite.py](/usr/local/project/github/helix-runtime/helix/storage/sqlite.py)

对应 `story-weaver` 当前主线：

- 需求主线：
  - [REQ-20260411-stateful-story-platform-upgrade.md](/usr/local/project/github/story-weaver/docs/requirements/REQ-20260411-stateful-story-platform-upgrade.md)
- 只读上下文：
  - [StoryContextController.java](/usr/local/project/github/story-weaver/backend/src/main/java/com/storyweaver/controller/StoryContextController.java)
- 编排：
  - [StorySessionOrchestrationController.java](/usr/local/project/github/story-weaver/backend/src/main/java/com/storyweaver/controller/StorySessionOrchestrationController.java)
- 状态：
  - [StoryStateController.java](/usr/local/project/github/story-weaver/backend/src/main/java/com/storyweaver/controller/StoryStateController.java)

## 为什么不能直接接

### 1. 核心状态模型不是一类东西

`helix-runtime` 的核心状态是 `SessionState`：

- `current_topic`
- `current_task`
- `task_status`
- `workflow_step`
- `last_feedback_type`

这是一种“通用任务会话状态”。  
而 `story-weaver` 当前已经落下的是“故事域状态”：

- `StoryUnit + Facets`
- `ReaderRevealState`
- `SceneExecutionState`
- `SceneHandoffSnapshot`
- `StoryEvent`
- `StorySnapshot`
- `StoryPatch`
- `ChapterIncrementalState`

也就是说：

- `helix-runtime` 维护的是“AI 对话/任务运行状态”
- `story-weaver` 维护的是“故事世界与章节执行状态”

这两者不是不能协作，但**不能互相替代**。

### 2. 工作流语义不匹配

`helix-runtime` 当前工作流偏：

- `document workflow`
- `revision workflow`
- `continue / direct / workflow` 触发

而 `story-weaver` 当前工作流已经进化成：

- `director`
- `selector`
- `writer`
- `reviewer`
- `scene skeleton`
- `scene execute`
- `chapter review`

所以如果你直接把 `helix` 的 workflow runtime 嵌进来，会出现一个问题：

- 它能处理“通用 AI 任务”
- 但它不理解“故事镜头级执行”

### 3. MCP 工具层太通用，不是故事域工具层

Helix 当前 MCP 暴露的是：

- `create_session`
- `get_session_state`
- `switch_provider`
- `list_providers`
- `get_session_history`
- `health_check`
- 以及 `helix-chat / helix-code / helix-document / helix-revision`

这套工具更像“通用 AI Runtime 的 MCP 接口”。  
而 `story-weaver` 真正需要的 MCP 工具应该逐渐变成：

- `get_project_brief`
- `get_reader_known_state`
- `get_scene_execution_state`
- `get_chapter_skeleton`
- `list_recent_story_progress`
- `save_scene_outcome`
- `append_story_event`

所以现在两边不是一套工具语义。

### 4. 技术栈和部署边界天然分离

- `helix-runtime`：Python + FastAPI + SQLite/Redis
- `story-weaver`：Java 模块化单体 + Maven + Spring

如果做“直接接入”，你最终还是会变成：

- 两个独立进程
- 两套配置
- 两套存储
- 两套生命周期

那它本质上就已经不是“内嵌”，而是“外接运行时”。

## 现在可行的接入方式

### 方案 A：外部 Runtime 适配接入

这是我推荐的方案。

定位：

- `story-weaver` 继续做故事域主系统和 `State Server`
- `helix-runtime` 做外部 `Agent Runtime / MCP Gateway / Provider Runtime`

职责切分：

- `story-weaver`
  - 继续作为故事数据真源
  - 持有 `StoryUnit / State / Event / Snapshot / Patch / Scene`
- `helix-runtime`
  - 负责通用 AI Session Runtime
  - 负责 Provider 路由
  - 负责外部 MCP 暴露
  - 通过 adapter 读取 `story-weaver` 的 story-context / orchestration / state

这样接，难度中等，可落地。

### 方案 B：只借用 Provider/MCP 能力

定位：

- 不把 `helix-runtime` 作为工作流内核
- 只把它作为一个“AI 运行时代理层”

价值：

- 可以复用它的 Provider 抽象
- 可以复用它的 MCP server 壳
- 但故事工作流仍然由 `story-weaver` 自己主导

这样接，难度较低，但收益也相对有限。

### 方案 C：试图让 Helix 直接成为 Story Weaver 内核

不推荐。

原因：

- 现在它的状态模型、工作流模型、工具模型都不对故事域
- 你会花大量时间做“把通用运行时硬拧成故事引擎”
- 最后代价可能大于收益

## 接入价值评估

如果采用方案 A，我认为有 4 个明确价值：

1. 给第三方 AI 工具暴露统一入口更容易  
`helix-runtime` 天然就是 MCP first。

2. Provider 路由可以独立演进  
把通用 AI Provider 层从故事域中再剥离一层。

3. 可以把“故事域状态”和“通用会话 runtime”职责分开  
这对长期维护是好事。

4. 对未来多 Agent 协作更友好  
多个外部 agent 可以经由 `helix-runtime` 进来，再读写 `story-weaver` 暴露的受控接口。

## 主要接入难点

### 高风险点 1：State Source of Truth

必须明确：

- `story-weaver` 是故事域状态真源
- `helix-runtime` 不能偷偷维护第二套故事状态真源

否则会马上出现：

- 会话状态一套
- 故事状态一套
- 两边漂移

### 高风险点 2：Session 与 Story Context 的桥接

Helix 现在是：

- `session_id -> messages + session_state`

Story Weaver 现在是：

- `projectId + chapterId + sceneId -> story state + orchestration state`

这中间必须有明确 bridge，不然 tool 调用没法稳定落在正确故事上下文上。

### 高风险点 3：Workflow Ownership

必须明确谁拥有：

- 意图识别
- 编排决策
- scene 执行
- reviewer

我的建议是：

- Helix 拥有“通用 runtime 和 MCP”
- Story Weaver 拥有“故事工作流与故事状态”

## 最终判断

我给一个明确评级：

- 直接内嵌接入可行性：`低`
- 作为外部 Runtime 适配接入可行性：`高`
- 当前是否建议直接开始接：`不建议直接接核心链`
- 当前建议：`先按适配层方案重构 helix-runtime，再做受控接入`

## 建议的接入顺序

1. 不动 `story-weaver` 的故事域真源地位  
2. 先把 `helix-runtime` 重构成“可适配的外部 runtime”  
3. 先接只读 story-context  
4. 再接 orchestration preview  
5. 最后再接受控写入

## 对应文档

如果按上面路线做，建议直接参考下一份重构文档：

- [ARCH-20260419-helix-runtime-adapter-refactor-v1.md](/usr/local/project/github/story-weaver/docs/architecture/ARCH-20260419-helix-runtime-adapter-refactor-v1.md)

## 贡献与署名说明

- “评估是否接入 `helix-runtime`”的问题与方向由用户提出
- 代码阅读、兼容性分析、接入分层判断与本报告撰写由 Codex 完成
