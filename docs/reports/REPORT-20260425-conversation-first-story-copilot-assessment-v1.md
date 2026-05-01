# 持续对话式小说共创功能专项评估

- Report ID: REPORT-20260425-conversation-first-story-copilot-assessment-v1
- Created At: 2026-04-25 Asia/Shanghai
- Scope: 评估“从项目创建开始，以持续对话驱动世界观/人物/因果/章节/镜头/正文生成，并带确认与状态回写”的实现方式与架构边界

## 结论

这项能力不需要重做当前 `Story Weaver` 平台底座，但需要新增一层明确的应用级编排架构。

不能只靠现有：

- `summary-workflow`
- `ai-writing/chat`
- `ai-writing`
- `story-orchestration`

在前端简单串联，就宣称已经支持“持续对话式共创”。

原因不是模型能力不够，而是当前系统还缺：

- 项目级持续会话
- 跨对象的待确认动作队列
- 对话判定后的统一写回编排
- 多对象依赖链的状态管理

因此，推荐方向是：

- 保留当前 `StoryUnit / Summary First / Story Context / Story State / AI Writing / AIDirector`
- 在其上新增 `Story Copilot / Conversation Orchestrator` 应用层

这是一次“应用层架构升级”，不是“平台基础设施重写”。

## 现有基础是否可复用

当前已经具备可直接复用的基础件：

### 1. 对象级摘要确认链

现有 `SummaryWorkflowController` 已支持：

- `proposals`
- `chat-turns`
- `previews`
- `apply`

并且人物、世界观、章节已经具备：

- 对话整理
- 结构预览
- 用户确认后写回

这说明“先聊，再整理，再确认，再写回”的模式已经跑通，但仅限单对象。

### 2. 章节级背景聊天

现有 `AIWritingChatController` 已支持：

- 章节级聊天
- 固定背景
- 历史压缩摘要
- 参与正文生成

这说明系统已经能保留一部分持续对话上下文，但当前会话范围被限制在单章节。

### 3. 正文生成前的决策层

现有 `AIDirectorApplicationServiceImpl` 已支持：

- 判定阶段
- 选择上下文模块
- 输出 decision pack
- 工具化读取事实

这说明系统已经有“先判断当前该做什么，再决定调用哪些模块”的架构雏形。

### 4. 状态与编排底座

现有平台已经具备：

- `StoryUnit + Facets`
- `Story Context`
- `Story Orchestration`
- `Story State`

这意味着后续新增共创编排层时，不需要重新发明对象协议、状态读取和章节运行态存储。

## 当前不足

如果目标是“从项目创建开始，一路通过持续对话推动世界观、人物、剧情、因果、章节、镜头、正文”，当前系统有 6 个关键缺口。

### 1. 缺少项目级持续会话

现在的持续会话只有：

- `summary-workflow/chat-turns`
  - 由前端携带最近消息
  - 服务端不持久保存对象级对话会话
- `ai-writing/chat`
  - 会话是 `chapterId` 级别
  - 不是 `projectId` 级别

这不够支撑“从项目创建一路聊到章节与正文”。

### 2. 缺少统一的待确认动作模型

现在的确认动作分散在：

- `summary-workflow/apply`
- `ai-writing/{id}/accept`
- `structured-creations/apply`

它们都是各做各的，还没有统一的：

- 动作类型
- 动作状态
- 动作来源证据
- 动作依赖关系
- 动作撤销/驳回/修改链路

### 3. 缺少跨对象依赖编排

用户一句话可能同时触发：

- 新世界观
- 新人物
- 因果关系补充
- 章节摘要调整
- 镜头规划
- 正文生成

当前系统没有“先后顺序编排器”去判断：

- 应该先生成什么
- 什么必须确认后才能继续
- 什么可以并行建议
- 哪些对象之间存在依赖

### 4. 缺少统一状态回写协议

现在对象写回、正文采纳、镜头推进、章节状态变化分别落在不同服务里。

如果要实现“对话判定 -> 待确认 -> 确认后状态回写 -> UI 同步刷新”，需要统一约束：

- 本次动作写了哪些实体
- 改了哪些摘要/正文/镜头/状态
- 哪些下游模块需要重新评估

### 5. StoryUnit 统一入口还没覆盖完整

`StoryUnitType` 已包含：

- `CHARACTER`
- `WORLD_SETTING`
- `CHAPTER`
- `PLOT`
- `CAUSALITY`
- 其他类型

但当前 `summary-workflow` 实际完整接入的目标处理器只有：

- 人物
- 世界观
- 章节

`PLOT / CAUSALITY / SCENE` 还没有进入同一套摘要确认工作流。

### 6. 现有自动识别能力还偏“后处理”

当前 `StructuredCreationSuggestionServiceImpl` 主要是：

- 从生成后的正文里识别人名候选
- 从正文句子里识别因果候选

这是“生成后识别”，不是“对话中主动规划并等待确认”。

## 推荐架构

推荐新增一个项目级应用层：`Story Copilot / Conversation Orchestrator`。

它不直接替代现有服务，而是位于它们之上。

### 1. 新增的核心职责

这层只负责四件事：

1. 维护项目级持续会话
2. 判断当前对话需要触发哪些小说对象动作
3. 生成待确认动作并驱动确认流
4. 在确认后调用现有领域服务完成真实写回

### 2. 建议新增的核心模型

至少新增以下概念：

- `CopilotSession`
  - 项目级会话
  - 可附带当前章节/当前对象/当前场景 scope
- `CopilotMessage`
  - 用户与助手消息
  - 支持压缩摘要与 pinned memory
- `CopilotActionProposal`
  - 一条待确认动作
  - 例如 `create_world_setting` / `update_character` / `create_causality` / `plan_chapter` / `generate_scene_draft`
- `CopilotActionStatus`
  - `draft`
  - `needs_confirmation`
  - `confirmed`
  - `applied`
  - `rejected`
  - `failed`
- `CopilotMemorySnapshot`
  - 当前会话下已确认的稳定设定、人物约束、写作偏好、未决问题

### 3. 建议的处理链

一轮对话建议统一走下面的链：

1. 用户发消息
2. Copilot 读取项目上下文
3. Planner 判断这轮可能涉及哪些对象与动作
4. 生成自然语言回复
5. 同步生成一个或多个 `ActionProposal`
6. 用户对每个 proposal 执行：
   - 确认
   - 修改
   - 删除
   - 暂缓
7. Copilot 调用现有服务真正写回
8. 写回后刷新 memory / state / pending actions

### 4. 现有服务的推荐复用方式

- 对象摘要创建/修改：
  - 继续复用 `summary-workflow`
- 人物/剧情/因果创建：
  - 继续复用 `structured-creations/apply` 与现有 CRUD
- 章节正文生成：
  - 继续复用 `ai-writing` 与 `ai-writing/chat`
- 镜头规划与执行：
  - 继续复用 `story-orchestration`
- 状态展示与一致性检查：
  - 继续复用 `story-state`

关键原则：

- Copilot 只做“判断与调度”
- 真正写库仍走现有领域服务

不要让一个自由对话接口直接绕过领域边界写所有表。

## 是否需要一次性大改

不建议一次性全量重构。

推荐拆成 3 步。

### Phase A: 先做项目级对话与待确认动作

先覆盖：

- 项目创建期
- 世界观
- 人物
- 章节摘要

这一步暂时不碰：

- 镜头自动编排
- 正文自动生成串联
- 因果/剧情深层联动

目标是先跑通：

- 一条持续对话
- 多对象建议
- 待确认动作队列
- 确认后真实写回

### Phase B: 补齐剧情 / 因果 / 章节规划

在 `StoryUnit` 统一入口上继续补：

- `PLOT`
- `CAUSALITY`
- 章节级规划

让对话式共创从“项目设定层”延伸到“叙事结构层”。

### Phase C: 接入镜头与正文主链

最后再接：

- scene 级 proposal
- `story-orchestration`
- `ai-writing`
- `story-state`

做到：

- 对话触发镜头建议
- 确认后生成正文
- 接受草稿后推进状态

## 是否应该直接复用现有 AIDirector

不建议直接把当前 `AIDirector` 原样拿来做这个功能主入口。

原因：

- 当前 `AIDirectorDecisionRequestDTO` 以 `chapterId` 为中心
- 模块选择逻辑也明显偏章节正文生成
- 它适合做“写作前决策层”，不适合直接承担“项目级共创编排”

但它的模式非常值得复用：

- 先做 planner
- 再选择工具/模块
- 最后输出结构化 decision pack

推荐做法是：

- 保留现有 `AIDirector` 作为“正文生成子导演”
- 新增项目级 `Copilot Planner`

两者形成上下层关系，而不是互相替代。

## 风险与边界

如果不做这层应用级编排，而只是前端把几个接口拼起来，后续大概率会遇到：

- 对话记忆散落在不同页面
- 建议动作无法追踪确认状态
- 同一句话触发多个对象时顺序混乱
- 写回后状态不同步
- 正文生成吃到未确认设定
- 用户以为“已确认”，系统其实只是临时聊天上下文

所以这个需求的真正边界不是“多加一个聊天框”，而是“让对话成为受控的创作编排入口”。

## 最终判断

最终判断如下：

- 不是底层平台重做
- 但需要一次明确的应用层架构升级
- 升级重点不是模型本身，而是：
  - 项目级会话
  - 待确认动作状态机
  - 跨对象编排
  - 统一写回协议

建议后续以独立需求立项，不要直接塞进当前 `REQ-20260425-ai-writing-timeout-root-cause-fix`。

## 贡献与署名说明

- 功能设想与产品目标：用户提出。
- 架构边界识别、现状评估与分阶段落地建议：Codex 完成。
