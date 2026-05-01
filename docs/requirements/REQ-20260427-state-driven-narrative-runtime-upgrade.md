# REQ-20260427 状态驱动叙事 Runtime 架构升级

- Req ID: `REQ-20260427-state-driven-narrative-runtime-upgrade`
- Created At: 2026-04-27 Asia/Shanghai
- Status: In Progress

## 背景

- 当前章节工作区虽然已经补上 AI 骨架、顺序锁、continuity state、accepted rollback 和骨架流式日志，但主链仍然是“先生成正文，再从正文里反推状态”。
- 这会导致几个结构性问题：
  - `scene` 既承担事实推进，又承担文本展示，职责过重。
  - continuity 校验再怎么补，也是在为“正文先行”兜底，镜头越多，误杀和漏判风险越高。
  - 现有 `Causality` 更像作者规划元数据 CRUD，不是运行态因果裁判；它无法直接承担“玩家动作 -> 世界结算 -> 状态变化”的真相层。
  - accepted scene 的撤回本质上已经暴露了更合理的方向：真正需要回退的是 checkpoint，而不是一段文本。

## 目标

- 把章节工作区的事实源从“scene 文本”切换到“节点推进 + 世界结算 + checkpoint”。
- 把 `scene` 降级为渲染批次，不再承担运行态真相层。
- 把 `Causality` 降级为 authoring layer 的规划元数据：前置条件、伏笔、回收依赖，不再充当运行态因果主链。
- 为新的节点驱动主链落最小 runtime 对象：
  - `StoryActionIntent`
  - `StoryResolvedTurn`
  - `StoryNodeCheckpoint`
  - `StoryOpenLoop`
- 让未来章节工作区能够基于：
  - 节点骨架
  - 推荐动作 / 自定义动作
  - resolver 结算
  - narrator 渲染
  完成完整闭环。

## 非目标

- 不在本轮直接重做完整多 NPC 常驻 session。
- 不在本轮把旧章节工作区直接全部迁移到 node mode。
- 不在本轮立即废弃现有 scene mode。
- 不在本轮直接实现复杂玩法系统或战斗规则系统。

## 验收标准

- 新需求必须明确区分三层：
  - authoring layer
  - runtime truth layer
  - narrative render layer
- `Causality` 的后续职责边界必须写清楚：保留什么，降级什么，不再承担什么。
- `storyunit` 必须新增可持久化的 runtime 模型与 store 接口，至少包括：
  - `StoryActionIntentStore`
  - `StoryResolvedTurnStore`
  - `StoryNodeCheckpointStore`
  - `StoryOpenLoopStore`
- `ResilientStoryStateStore` 必须能够在 Redis 缺失时回退到内存实现，保证新 runtime 对象可记录、可读取。
- 主入口文档必须说明：当前主需求已从“多 scene 修补”切换为“状态驱动叙事 runtime 架构升级”。
- 必须提供一份独立架构文档，说明：
  - 当前模式的问题
  - 目标模型
  - 分阶段迁移路线
  - 与现有章节工作区、Causality、Story State 的映射关系
