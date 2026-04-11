# 模块边界、MCP 与 LSP 运行架构

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Arch Doc Version: v1
- Status: Drafted
- Created At: 2026-04-11 Asia/Shanghai
- Updated At: 2026-04-11 Asia/Shanghai

## 目标

本架构文档解决三个问题：

1. 当前单模块工程如何升级成模块化单体。
2. `MCP Server` 与 `LSP Server` 的职责如何划分。
3. 未来状态、摘要、候选、镜头、trace 应该从哪里读、往哪里写。

## 命名约定

为了避免和常见的 `Language Server Protocol` 混淆，这里把本项目内的 `LSP` 明确定义为：

- `Local Story Protocol Server`

它的职责不是代码补全，而是本地故事状态协议服务。

## 打包策略

### 当前阶段

- 继续保持单部署、单 JAR。
- 先升级为模块化单体。

### 后续可能演进

- `MCP Server` 可独立进程化
- `LSP Server` 可独立进程化
- Provider Adapter 可按需外拆

但在第一阶段，不要求立即拆成多进程。

## 建议模块划分

### 1. `story-weaver-boot`

- Spring Boot 启动入口
- 最终打包聚合层

### 2. `story-domain`

- 领域对象
- 值对象
- 聚合根接口
- 事件类型
- 统一错误码

### 3. `story-canon`

- 世界观
- 人物静态设定
- 势力
- 地点
- 物品定义
- 技能定义
- 剧情与因果静态层

### 4. `story-state`

- 背包
- 技能状态
- 态度
- 好感度
- 关系运行态
- 世界影响状态
- open loops
- reader known state

### 5. `story-generation-orchestrator`

- 章节写作主编排
- session 调用顺序
- trace 汇总
- 快照装配

### 6. `story-director`

- 章节总导
- 镜头候选
- 节奏骨架

### 7. `story-writer`

- 镜头扩写
- 局部重写
- 尾段修补

### 8. `story-reviewer`

- 审校
- 一致性检查
- 结尾完整性
- 命名漂移检查

### 9. `story-mcp-server`

- 对外暴露稳定工具面
- 主要承接上下文读取与受控写回

### 10. `story-lsp-server`

- 本地故事状态协议服务
- 快照、patch、event、state read model

### 11. `story-provider`

- Provider 适配
- 模型路由
- tool calling 兼容
- fallback 策略

### 12. `story-settings`

- 配置项
- 策略开关
- 模型路由设置
- prompt 模板管理

### 13. `story-web`

- Controller
- DTO
- ViewModel
- 对前端和外部的 API 暴露

### 14. `story-infra`

- MyBatis
- Redis
- 对象存储
- 任务调度
- 数据迁移
- trace 持久化

## 依赖方向约束

必须遵守：

- `story-web -> application modules`
- `story-generation-orchestrator -> director/writer/reviewer/mcp/lsp/provider`
- `story-director / writer / reviewer -> domain + mcp/lsp + provider`
- `story-mcp-server -> canon/state/relation/reveal/execution read services`
- `story-lsp-server -> snapshot/patch/event/state services`
- `story-provider` 不依赖前端和业务 controller

不允许：

- 前端 controller 直接拼故事状态
- `AIWritingServiceImpl` 继续演变成平台总中心
- 让 provider 层直接读数据库业务表

## MCP Server 职责

### 适合 MCP 的能力

- 稳定上下文读取
- 受控结构写回
- 挂起新对象
- 读取快照与 trace

### 第一批推荐工具

- `get_project_brief`
- `get_story_unit_summary`
- `get_chapter_anchor_bundle`
- `get_reader_known_state`
- `get_character_runtime_state`
- `get_inventory_state`
- `get_skill_state`
- `get_recent_story_progress`
- `save_generation_snapshot`
- `append_story_event`
- `append_scene_outcome`
- `create_pending_unit`

### 关键原则

- 初期优先只读
- 写回必须受控
- 不把 MCP 当成“模型自由探索数据库”的通道

## LSP Server 职责

### LSP 的核心价值

- 管理运行时状态
- 管理快照
- 管理 patch
- 管理 event
- 管理 session handoff

### LSP 应承载的内容

- `StorySnapshot`
- `StoryPatch`
- `StoryEvent`
- `SceneExecutionState`
- `ReaderRevealState`
- `CharacterStateDelta`
- `OpenLoopState`

### 为什么要单独设 LSP

因为 MCP 更像工具暴露面，而 LSP 更像状态协议和状态读模型。  
MCP 适合“调用什么”，LSP 适合“状态现在是什么”。

## 读写边界矩阵

### 章节总导

- 读：
  - 项目摘要
  - 章节锚点
  - 最近进度
  - Reader Reveal State
- 写：
  - 候选骨架
  - director trace

### 选择器

- 读：
  - 候选列表
  - 当前章节状态
- 写：
  - chosen candidate
  - rejected reasons

### 写手

- 读：
  - chosen brief
  - 当前 scene handoff
  - 角色运行态
  - 背包 / 技能 / 地点状态
- 写：
  - scene outcome
  - pending creates
  - generation snapshot

### 审校器

- 读：
  - chosen brief
  - 正文
  - snapshot
  - reveal state
- 写：
  - review decision
  - reviewer trace

## 迁移建议

### 第一阶段

- 不直接移除现有服务。
- 先增加模块边界和协议层。

### 第二阶段

- 让旧实体通过 adapter 形式映射到 `StoryUnit`。

### 第三阶段

- 把总导、写手、审校链路逐步改成只读 MCP/LSP。

### 第四阶段

- 再做写回和状态增量。

## 贡献与署名说明

- “必须拆模块，并把 MCP/LSP 纳入主架构”的方向：用户提出。
- 模块边界、运行职责、依赖方向和迁移策略整理：Codex 完成。
- MCP/LSP 的边界判断与单 JAR 前提下的实施方式：用户与 Codex 共同讨论形成。

