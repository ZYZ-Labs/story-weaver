# ARCH-REQ-20260427 状态驱动叙事 Runtime 架构说明

- Arch ID: `ARCH-REQ-20260427-state-driven-narrative-runtime-v1`
- Related Req: `REQ-20260427-state-driven-narrative-runtime-upgrade`
- Updated At: 2026-04-27 Asia/Shanghai

## 1. 问题定义

当前章节工作区依然是：

`生成 scene 正文 -> 接纳正文 -> 从正文反推状态 -> 用摘要/handoff 约束下一 scene`

这会导致三个结构性问题：

1. `scene` 同时承担事实推进和文本展示，边界不清。
2. continuity 校验只能做“事后纠偏”，不是“事前结算”。
3. 撤回、重渲染、分支推进都绕不开正文，导致状态真相层不稳定。

## 2. 目标模型

目标主链调整为：

`checkpoint -> action intent -> world resolve -> resolved turn -> state delta / open loop / reveal -> narrative render`

### 2.1 三层职责

- `Authoring Layer`
  - 节点骨架
  - 推荐动作
  - 自定义动作约束
  - `Causality`、伏笔、回收依赖

- `Runtime Truth Layer`
  - `StoryActionIntent`
  - `StoryResolvedTurn`
  - `StoryNodeCheckpoint`
  - `StoryOpenLoop`
  - `StoryEvent / StorySnapshot / StoryPatch / ReaderRevealState / ChapterIncrementalState`

- `Narrative Render Layer`
  - narrator session
  - scene/chapter 文本
  - summary / recap

## 3. 现有对象的迁移定位

### 3.1 `scene`

- 保留为渲染批次
- 不再充当运行态事实单元
- 后续可以理解为“一个或多个 resolved turn 的文本回放窗口”

### 3.2 `Causality`

- 保留为作者规划元数据
- 负责：
  - 前置条件
  - 伏笔
  - payoff 依赖
  - 节点间可选路径关系
- 不再负责：
  - 本轮动作是否成功
  - 谁在此刻遇见了谁
  - 本轮读者知道了什么
  - 本轮状态如何变化

### 3.3 `StoryEvent / StorySnapshot / StoryPatch`

- 保留
- 作为 resolved turn 后的通用状态审计和读模型补充
- 但不再单独承担节点推进协议

### 3.4 `ReaderRevealState / ChapterIncrementalState`

- 保留
- 继续作为章节级聚合状态
- 后续由 resolver / checkpoint 主链驱动更新，而不是先写正文再回推

## 4. 第一阶段最小可用版

第一阶段不做多 NPC 常驻 session，只做 node mode 最小闭环：

1. 章节先生成 `node skeleton`
2. 作者选择推荐动作或输入自定义动作
3. 后端记录 `StoryActionIntent`
4. resolver 生成 `StoryResolvedTurn`
5. 保存 `StoryNodeCheckpoint`
6. 更新 `StoryOpenLoop / ReaderRevealState / ChapterIncrementalState`
7. narrator 根据已结算结果生成文本

## 5. 为什么先落 runtime 对象

如果不先把 runtime 真相层落盘，章节工作区即使改成“节点 UI”，本质仍会退回“选项 prompt + 正文生成”的旧逻辑。  
因此第一批必须先有：

- `StoryActionIntent`
- `StoryResolvedTurn`
- `StoryNodeCheckpoint`
- `StoryOpenLoop`

它们是 node mode 的事实底座。

## 6. 与现有多 scene 修补链的关系

- `REQ-20260425` 继续保留为现有 scene mode 的兼容基线和稳定性修补记录。
- `REQ-20260427` 负责新的 node/checkpoint/runtime 主链。
- 在 node mode 真正落地前，scene mode 仍可继续使用，但不再承担最终架构方向。
