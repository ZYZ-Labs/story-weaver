# 多 Session 写作管线与镜头执行架构

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Arch Doc Version: v1
- Status: Drafted
- Created At: 2026-04-11 Asia/Shanghai
- Updated At: 2026-04-11 Asia/Shanghai

## 当前问题

当前写作链路虽然已经有：

- prepare
- plan
- write
- check
- revise

但本质仍是“单轮大 prompt 生成 + 局部修补”。  
这会导致：

- 总导过粗，只给全章级视角
- 写手同时承担“决定写什么”和“把它写出来”
- 审校只能事后兜底
- 章节内部缺少镜头级交接

## 核心判断

后续应采用四 session：

- 总导
- 选择器
- 写手
- 审校器

并引入两层结构：

- `Chapter Skeleton`
- `Scene Execution`

## 为什么不是直接全章拆完再写

全章先拆完所有镜头，优点是节奏稳，但缺点是前置成本高、局部漂移会污染整章。  
逐镜头即时决策，优点是灵活，但缺点是容易丢上下文。

因此推荐采用混合式：

1. 章节总导先给出骨架方案
2. 选择器先选章节骨架
3. 写手逐镜头执行
4. 每镜头结束后写回状态
5. 下一镜头再基于最新状态继续推进

## 四 Session 职责

### 1. 总导 Session

职责：

- 不写正文
- 只负责给出章节骨架候选或镜头候选

输出：

- `DirectorCandidate[]`

### 2. 选择器 Session

职责：

- 不创造新候选
- 只在候选中选当前最优

输出：

- `SelectionDecision`

### 3. 写手 Session

职责：

- 不重新决定大方向
- 只把当前 chosen brief 扩成正文

输出：

- `SceneDraft`
- `PendingCreates`
- `SceneOutcome`

### 4. 审校器 Session

职责：

- 不负责创作
- 只负责判断是否通过

输出：

- `ReviewDecision`

## 两层总导

### 章节总导

决定：

- 这章大概由几个镜头组成
- 每个镜头分别承担什么目标
- 章节整体写到哪里停

### 镜头总导

决定：

- 当前下一镜头该写什么
- 该揭晓什么
- 该停在哪里

## 核心协议

### ChapterSkeleton

```json
{
  "chapterId": 31,
  "skeletonId": "skeleton_31_v1",
  "sceneCount": 3,
  "globalStopCondition": "收到邀请并做出决定后停住",
  "scenes": [
    {
      "sceneId": "scene_1",
      "goal": "完成现实状态定向",
      "readerReveal": ["林沉舟已退役两年"],
      "stopCondition": "回到家，进入书房前"
    }
  ]
}
```

### DirectorCandidate

```json
{
  "id": "c1",
  "type": "opening",
  "goal": "完成现实场景和人物状态定向",
  "readerReveal": ["主角退役两年"],
  "mustUseAnchors": ["POV=林沉舟"],
  "forbiddenMoves": ["不要直接进入游戏战斗"],
  "stopCondition": "收到邀请前停住",
  "targetWords": 900
}
```

### SelectionDecision

```json
{
  "chosenCandidateId": "c1",
  "whyChosen": "当前是第一章空章起稿，必须优先读者定向",
  "risks": ["篇幅过长会导致结尾不稳"]
}
```

### SceneExecutionState

```json
{
  "sceneId": "scene_1",
  "status": "completed",
  "readerRevealDelta": ["林沉舟已退役两年"],
  "characterStateDelta": [],
  "openLoopsOpened": ["邀请函是什么来历"],
  "openLoopsClosed": [],
  "handoffLine": "林沉舟走向书房。"
}
```

## 输入输出边界

### 总导输入

- 项目摘要
- 章节 anchor bundle
- 最近章节进度
- Reader Reveal State
- 当前 open loops

### 选择器输入

- candidate list
- 当前章节位置
- 当前镜头状态

### 写手输入

- chosen candidate
- 必要锚点
- 当前 scene handoff
- 当前状态快照

### 审校器输入

- chosen candidate
- 生成结果
- 最新 snapshot

## 防止镜头之间失联的机制

必须至少写回以下状态：

- `sceneGoal`
- `sceneStopCondition`
- `readerRevealDelta`
- `characterStateDelta`
- `openLoopsOpened`
- `openLoopsClosed`
- `handoffLine`

如果这些状态没有写回，下一镜头就会重新退化为 prompt 临时承接。

## Prompt 原则

后续 prompt 不应继续变长，而应变窄：

- 总导 prompt：只产出候选
- 选择器 prompt：只做选择
- 写手 prompt：只写当前镜头
- 审校器 prompt：只做判断

重点不在“文案多强”，而在“协议和状态是否稳定”。

## 失败处理策略

### 如果总导失败

- 用启发式骨架 fallback
- 明确记录 fallback 原因

### 如果选择器失败

- 选最高优先级候选
- 保留失败 trace

### 如果写手失败

- 优先做尾段修补或当前镜头重写

### 如果审校器失败

- 优先镜头级修复
- 再决定是否章节级返工

## 贡献与署名说明

- “总导、选择器、写手应拆成多个 session”的核心方向：用户提出。
- “章节骨架 + 镜头执行 + 状态交接”的收敛方式、协议对象与文档撰写：Codex 完成。
- “如何避免镜头间失联”的判断由用户与 Codex 共同讨论形成。

