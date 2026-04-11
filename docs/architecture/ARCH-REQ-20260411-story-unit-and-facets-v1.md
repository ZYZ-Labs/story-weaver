# StoryUnit 与 Facets 元模型

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Arch Doc Version: v1
- Status: Drafted
- Created At: 2026-04-11 Asia/Shanghai
- Updated At: 2026-04-11 Asia/Shanghai

## 设计目的

当前系统未来会同时承载：

- 世界观
- 人物
- 势力
- 物品
- 技能
- 剧情
- 因果
- 背包
- 技能状态
- 态度
- 好感度
- 世界影响
- 章节骨架
- 镜头执行
- 读者揭晓状态

这些信息不可能继续依赖“各自独立字段表 + 临时 prompt 拼接”来长期演进。  
因此需要一个统一的、可被 MCP/LSP 稳定读写的基础单位。

这里采用：

- `StoryUnit`

但必须强调：

- `StoryUnit` 不是万能基类
- 不是数据库里的超级大表
- 不是让所有业务对象都继承一个胖实体

它的定位是：

- 统一协议壳
- 统一快照语义
- 统一 facet 挂载入口
- 统一版本与 trace 入口

## 核心术语

### 1. StoryUnit

系统中所有“可被 MCP/LSP 读取、可被编排层引用、可被快照化”的对象基础单元。

### 2. Facet

挂载在 `StoryUnit` 上的不同信息视角。

### 3. StoryPatch

对某个 `StoryUnit` 的局部变更提案，不代表已经确认落库。

### 4. StorySnapshot

在某个时间点，某个或某组 `StoryUnit` 的可回放快照。

### 5. StoryEvent

表示“发生了什么变化”的事件记录，用于驱动状态增量与回放。

## StoryUnit 外壳建议字段

> 说明：以下是协议字段，不要求一开始全部映射到单表。

```json
{
  "unitId": "su_character_15",
  "unitKey": "character:15",
  "unitType": "character",
  "projectId": 28,
  "scope": "project",
  "summaryFacet": {},
  "canonFacetRef": "canon:character:15",
  "stateFacetRef": "state:character:15",
  "relationFacetRef": "relation:character:15",
  "revealFacetRef": "reveal:character:15",
  "executionFacetRef": "execution:character:15",
  "status": "stable",
  "version": 12,
  "snapshotId": "snapshot_20260411_001",
  "sourceTrace": {
    "createdBy": "user",
    "lastUpdatedBy": "summary_apply"
  }
}
```

## Facet 设计

### 1. SummaryFacet

唯一默认面向用户展示的 facet。

职责：

- 承载用户可读摘要
- 承载变更说明
- 作为前端主显示层

建议内容：

- `displayTitle`
- `oneLineSummary`
- `longSummary`
- `stateSummary`
- `relationSummary`
- `changeSummary`
- `pendingQuestions`

硬原则：

- 用户默认只看 `SummaryFacet`
- 不直接看原始结构化字段

### 2. CanonFacet

承载相对稳定、较少频繁变化的设定性事实。

适用对象：

- 世界观
- 人物基础设定
- 物品定义
- 技能定义
- 势力定义
- 地点规则

### 3. StateFacet

承载运行时变化状态。

典型内容：

- 人物当前状态
- 背包内容
- 技能冷却或可用性
- 好感度
- 态度值
- 声望
- 世界影响状态

### 4. RelationFacet

承载对象之间的关系。

典型内容：

- 人物关系
- 势力归属
- 因果挂接
- 物品归属
- 技能归属
- 地点关联

### 5. RevealFacet

承载不同知识视角下的信息边界。

建议分层：

- `systemKnown`
- `authorKnown`
- `readerKnown`
- `unrevealed`

这是后续“系统知道不等于读者知道”的核心承载层。

### 6. ExecutionFacet

承载运行过程中的临时执行信息。

典型内容：

- 章节骨架
- 镜头候选
- 当前 scene handoff
- generation trace
- pending changes

## StoryPatch

`StoryPatch` 是 AI 或系统提出的结构化变更提案，不直接等同于最终写入。

示意：

```json
{
  "patchId": "patch_20260411_001",
  "targetUnitId": "su_character_15",
  "facet": "state",
  "operations": [
    {
      "op": "replace",
      "path": "/attitude/towards_su_character_21",
      "value": "guarded"
    }
  ],
  "summary": "林沉舟对苏晚的态度从陌生转为戒备",
  "source": "scene_outcome",
  "status": "pending_confirmation"
}
```

## StorySnapshot

用于：

- 镜头交接
- 回放
- 调试
- 多 session 共享上下文

推荐范围：

- 章节快照
- 镜头快照
- session 输入快照
- session 输出快照

## StoryEvent

用于记录增量变化，而不是只覆盖最终状态。

例如：

- 获得某物品
- 使用某技能
- 态度变化
- 好感变化
- 世界规则生效
- 某条 open loop 被打开或关闭

## 用户可见层与系统协议层

必须坚持：

- UI 主层只展示 `SummaryFacet`
- 系统内部通过 `Canon / State / Relation / Reveal / Execution` 协作

也就是说：

- `summary -> structured patch` 可由 AI 完成
- `structured facets -> summary` 也可由 AI 回生成
- 用户只确认“摘要变化”，不确认原始 JSON

## 首批优先落地对象族

1. `Project`
2. `Character`
3. `WorldSetting`
4. `Chapter`
5. `Plot`
6. `Causality`

后续再扩：

1. `Item`
2. `Skill`
3. `Faction`
4. `Location`
5. `ReaderState`
6. `SceneExecution`

## 设计约束

- 一开始不强求所有 facet 都落单独表。
- 允许先以服务层协议和组合存储方式落地。
- 但协议命名必须先统一，避免后续各写各的。

## 贡献与署名说明

- “必须有统一基础单元，但不能是万能基类”的判断：用户提出。
- `StoryUnit + Facets + Patch + Snapshot + Event` 的术语收敛、协议壳设计和文档撰写：Codex 完成。
- “用户只看摘要，结构下沉到系统层”的原则：用户与 Codex 共同讨论形成。

