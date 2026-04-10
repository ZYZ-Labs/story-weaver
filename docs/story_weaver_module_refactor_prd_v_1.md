# Story Weaver Novel IDE - 模块重构需求文档 v1.0

## 项目目标
对现有小说创作系统核心模块进行结构化优化，解决模块职责重叠、字段语义不清、链路断裂和创建成本过高的问题。

### 目标
1. 建立完整创作链路
2. 强化 AI 上下文引用能力
3. 优化长篇小说管理体验
4. 为 Agent 自动生成内容提供结构化数据基础

---

# 一、整体模块职责重构

## 标准创作链路
```text
世界观管理
→ 大纲管理
→ 剧情管理
→ 因果管理
→ 人物管理
→ 章节管理
→ 写作中心
```

---

# 二、大纲管理模块优化

## 当前问题
- 与章节级内容边界不清
- “关联章节”流程逆序
- 缺少树状层级结构

## 优化需求

### 新增字段
```ts
outlineType: "global" | "volume" | "chapter"
parentOutlineId?: string
relatedWorldModels?: string[]
```

### 字段改名
```text
本章目标 → 本级目标
```

### 结构调整
顶部移除：
```text
关联章节
```

底部新增：
```text
由本大纲生成章节（可选）
```

### 正文模板默认结构
```markdown
## 开场
## 冲突升级
## 转折
## 高潮
## 收束
```

---

# 三、剧情管理模块优化

## 模块职责
剧情事件节点管理（Story Beat）

## 新增字段
```ts
storyBeatType:
  | "main"
  | "side"
  | "foreshadow"
  | "world_event"
  | "climax"
  | "reveal"

storyFunction:
  | "advance_mainline"
  | "character_growth"
  | "conflict_upgrade"
  | "foreshadow"
  | "payoff"

prevBeatId?: string
nextBeatId?: string
```

## 字段改名
```text
解决方案 → 事件结果
```

## 详细内容默认模板
```markdown
## 起因
## 经过
## 高潮
## 结果
## 后续影响
```

---

# 四、因果管理模块优化

## 模块职责
剧情逻辑依赖图谱

## 扩展关系类型
```ts
causalType:
  | "trigger"
  | "lead_to"
  | "block"
  | "reverse"
  | "foreshadow"
  | "payoff"
  | "escalate"
```

## 扩展实体类型
```ts
entityType:
  | "chapter"
  | "story_beat"
  | "character"
  | "organization"
  | "location"
  | "world_rule"
  | "item"
  | "state"
```

## 新增链式字段
```ts
upstreamCauseIds?: string[]
downstreamEffectIds?: string[]
```

## 新增兑现状态
```ts
payoffStatus:
  | "pending"
  | "triggered"
  | "fulfilled"
  | "expired"
```

## 触发条件结构化
```ts
triggerMode:
  | "instant"
  | "delayed"
  | "conditional"
  | "probabilistic"
  | "stage_based"
```

---

# 五、人物管理模块优化

## 当前问题
创建成本过高，字段过载。

## 重构方案
拆分为两阶段。

## 第一阶段：快速建角
```ts
roleType
name
description
identity
coreGoal
```

## 第二阶段：高级属性（折叠）
```ts
background
appearance
skills
traits
talents
weaknesses
tags
relationships
inventory
```

## 新增字段
```ts
growthArc: string
firstAppearanceChapterId?: string
activeStage?: string
isRetired?: boolean
```

## JSON默认折叠
```text
开发者视图（默认折叠）
```

## 背包迁移
迁移到：
```text
角色详情页 → 背包 Tab
```

---

# 六、章节管理模块优化

## 模块职责
章节工作台

## 新增字段
```ts
chapterStatus:
  | "draft"
  | "polishing"
  | "review"
  | "final"
  | "published"
  | "archived"

summary: string
outlineId?: string
storyBeatIds?: string[]
prevChapterId?: string
nextChapterId?: string
mainPOVCharacterId?: string
wordCount?: number
```

## 新增 AI 功能
```text
AI生成初稿
AI续写
AI润色
```

## UI增强
正文区右上角新增：
```text
当前字数
预计阅读时间
```

---

# 七、数据链路要求

必须建立完整引用关系：
```text
世界观
↓
大纲
↓
剧情节点
↓
因果链
↓
章节
↓
正文
```

所有模块必须支持 ID 级联引用。

---

# 八、优先级

## P0（必须优先）
- 大纲层级化
- 剧情节点链路
- 因果链升级
- 章节状态
- 快速建角

## P1（次优先）
- AI章节生成
- 角色成长弧线
- 背包拆分
- 可视化因果图

## P2（增强）
- 自动剧情推荐
- 自动因果补全
- 章节智能摘要

