# Story Weaver 系统能力与链路总览

- Guide ID: GUIDE-20260424-system-capabilities-and-chain-reference-v1
- Scope: 当前主系统能力、关键调用链、核心接口、参数影响、前后端联动说明
- Updated At: 2026-04-24 Asia/Shanghai

## 1. 用途

这份文档给后续模型和维护者一个统一入口，回答三件事：

- 现在系统已经具备哪些核心能力
- 每条主链路实际调用哪些接口、会写哪些状态
- 改某一条链时，前端、后端、文档应该一起动哪些点

不要再把长阶段进度文档当系统说明书。系统能力以本文为准，阶段文档只保留历史过程。

## 2. 当前系统分层

### 2.1 前端主入口

- `创作台`
- `状态台`
- `生成台`
- `章节工作区`
- `故事台`
- `系统台`

### 2.2 后端能力层

- `Summary Workflow`
- `Story Context`
- `Story Orchestration`
- `Story State`
- `Migration / Compatibility`

### 2.3 状态核心

- `StoryUnit + Facets`
- `SceneExecutionState`
- `SceneHandoffSnapshot`
- `StoryEvent`
- `StorySnapshot`
- `StoryPatch`
- `ReaderRevealState`
- `ChapterIncrementalState`

## 3. 通用约束

### 3.1 认证

当前主链接口默认都要求：

- `Authorization: Bearer <token>`

没有 Bearer token 时，多数接口直接返回 `401`。

### 3.2 当前固定样本

当前推荐项目：

- `旧日王座 / projectId=28`

当前固定回放章节：

- `#32 算法少女苏晚`
- `#33 训练赛首胜`
- `#34 宿敌归来`
- `#35 退役者的邀请`

后续联调、回放、页面验收默认优先使用这组样本，不再回到旧的 `chapter 31` 口径。

## 4. Summary Workflow

### 4.1 作用

这条链负责 `Summary First`：

- 自然语言输入
- AI 归纳摘要
- 结构化 proposal
- preview
- apply

### 4.2 主接口

- `POST /api/summary-workflow/proposals`
- `POST /api/summary-workflow/chat-turns`
- `POST /api/summary-workflow/previews`
- `POST /api/summary-workflow/apply`

### 4.3 关键参数

#### `intent`

枚举：

- `CREATE`
- `UPDATE`
- `REFINE`
- `ENRICH`

影响：

- `CREATE`
  - 允许 `targetSourceId` 为空
  - 用于新对象摘要创建
- `UPDATE`
  - 强调替换或改写已有摘要
- `REFINE`
  - 默认普通模式入口
  - 更偏整理和压缩
- `ENRICH`
  - 更偏补细节、补缺口

#### `operatorMode`

枚举：

- `DEFAULT`
- `EXPERT`
- `SYSTEM`

影响：

- `DEFAULT`
  - 面向普通作者
  - 主链是“说想法 -> AI 整理 -> 看变化 -> 确认写回”
- `EXPERT`
  - 保留更直接的摘要控制和旧表单回退
- `SYSTEM`
  - 保留给系统内部调用

#### `selectedProviderId / selectedModel`

影响：

- 可强制指定模型路由
- 主要在对话补全和实验性验证时使用

### 4.4 当前前端入口

- 人物管理
- 世界观管理
- 章节管理

这三处对象页都已经进入 `Summary / Canon / State / History` 分层，不再默认暴露字段墙。

## 5. Story Context

### 5.1 作用

这是只读上下文服务，给：

- 生成台
- 编排层
- 状态台
- 后续 MCP / State Server 适配

统一提供读取入口。

### 5.2 主接口

- `GET /api/story-context/projects/{projectId}/brief`
- `GET /api/story-context/story-units/summary`
- `GET /api/story-context/projects/{projectId}/chapters/{chapterId}/anchors`
- `GET /api/story-context/projects/{projectId}/chapters/{chapterId}/reader-known-state`
- `GET /api/story-context/projects/{projectId}/characters/{characterId}/runtime-state`
- `GET /api/story-context/projects/{projectId}/progress?limit=<n>`

### 5.3 参数影响

#### `limit`

- 只影响 `recent story progress` 返回条数
- 不改变其他上下文推导逻辑

#### `unitId / unitKey / unitType`

用于 `story-units/summary`：

- `unitType` 必须是合法 `StoryUnitType`
- 这条接口用于按协议引用读取对象摘要，不是普通对象列表接口

## 6. Story Orchestration

### 6.1 作用

这是多 session 编排与章节工作区主链：

- preview
- execute
- skeleton-generate
- skeleton-preview
- chapter-review
- skeleton scene mutation

### 6.2 主接口

- `GET /api/story-orchestration/projects/{projectId}/chapters/{chapterId}/preview`
- `POST /api/story-orchestration/projects/{projectId}/chapters/{chapterId}/execute`
- `POST /api/story-orchestration/projects/{projectId}/chapters/{chapterId}/skeleton-generate`
- `GET /api/story-orchestration/projects/{projectId}/chapters/{chapterId}/skeleton-preview`
- `GET /api/story-orchestration/projects/{projectId}/chapters/{chapterId}/chapter-review`
- `PUT /api/story-orchestration/projects/{projectId}/chapters/{chapterId}/skeleton-scenes/{sceneId}`
- `DELETE /api/story-orchestration/projects/{projectId}/chapters/{chapterId}/skeleton-scenes/{sceneId}`

### 6.3 `sceneId` 的影响

`sceneId` 是这条链里最关键的参数之一。

影响：

- 决定 preview/execute 面向哪个 scene
- 决定 scene binding 结果：
  - `SCENE_BOUND`
  - `SCENE_FALLBACK_TO_LATEST`
  - `CHAPTER_COLD_START`
  - `SCENE_QUERY_UNAVAILABLE`
- 决定是否能读取 `previousSceneHandoff`

默认值：

- `scene-1`

补充约束：

- `scene-1` 只是 preview / execute 的默认查询值，不再表示系统会自动替你生成 `scene-1`
- 章节工作区必须先有已保存 skeleton，后续 preview / execute / scene draft 才有明确目标

### 6.4 `execute` 的真实语义

`execute` 不等于“前端直接出正文”。

当前语义：

- 记录当前 scene 的运行态草稿写回结果
- 写 event / snapshot
- 不再把当前镜头标记为正式 `COMPLETED`

补充：

- `preview / execute` 现在只消费已保存的 skeleton scene，不再在缺骨架时临时推导本地候选
- 章节工作区里的“生成镜头骨架”是进入多 scene 主链之前的显式前置动作
- 章节工作区后续镜头受顺序锁约束，未接纳前一个镜头时不能越级 preview / execute / scene-draft

章节工作区里的正文草稿主链，走的是：

- `AIWriting generate-stream`

不是 `StorySessionOrchestrationController.execute`。

### 6.5 镜头删除

当前已支持删除 `written/completed` scene，但不是简单前端删列表。

后端实际处理：

- 删除 skeleton scene
- 清理关联 runtime/handoff
- 用 `deletedSceneIds` tombstone 防止规划器把历史 scene 再补回来

注意：

- 删除已执行镜头不会自动回滚已写入正文

## 7. AI Writing

### 7.1 作用

这条链负责章节工作区里的正文草稿生成与接受/拒绝。

### 7.2 主接口

- `POST /api/ai-writing/generate`
- `POST /api/ai-writing/generate-stream`
- `GET /api/ai-writing/chapter/{chapterId}`
- `GET /api/ai-writing/project/{projectId}`
- `GET /api/ai-writing/{id}`
- `POST /api/ai-writing/{id}/accept`
- `POST /api/ai-writing/{id}/reject`

### 7.3 关键参数

#### `writingType`

当前常见值：

- `draft`
- `continue`
- `polish`
- `expand`
- `rewrite`

影响：

- 决定生成任务的意图
- 会影响提示词组装和草稿行为

#### `currentContent`

- 用于 continue/polish/rewrite 时的正文基线
- 在章节工作区里通常是当前章节正文或草稿上下文

#### `userInstruction`

- 对当前一次生成的附加指导
- 不应承担长期状态输入职责

#### `maxTokens`

- 直接影响模型输出上限
- 改它会影响成本、速度和截断风险

#### `selectedProviderId / selectedModel`

- 可覆盖默认路由
- 适合实验性比对，不适合乱用

#### `entryPoint`

- 标识这次生成来自哪里
- 章节工作区、旧创作中心、其他入口可以据此区分

### 7.4 当前前端主路径

章节工作区普通路径：

1. 先显式生成当前章节的 AI 镜头骨架
2. 一次性看到整章镜头，但只允许选择已完成前缀和当前第一个未接纳镜头
3. 只为当前第一个未接纳镜头生成草稿
4. 流式接收正文与日志
5. 接受草稿
6. 用真实已接纳正文写回 runtime/handoff，并解锁下一镜头

`execute` 仍保留为专家调试动作，但不会替代章节工作区里的正式接纳完成。

补充：

- 章节工作区里的草稿、日志、模型信息现在应按 scene 维度隔离，不能再按整章共用一个流式状态
- `scene-2+` 的 prompt 必须显式带上一镜头摘要、上一镜头 handoff 和下一镜头入口预留

## 8. Story State

### 8.1 作用

这是当前 State Server 的应用层读写面。

### 8.2 主接口

#### 状态读取

- `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/events`
- `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/snapshots`
- `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/patches`
- `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/reader-reveal-state`
- `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/chapter-state`
- `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/consistency-check`

#### 迁移 / 兼容

- `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/backfill-analysis`
- `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/backfill-dry-run`
- `POST /api/story-state/projects/{projectId}/chapters/{chapterId}/backfill-execute`
- `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/compatibility-snapshot`
- `GET /api/story-state/projects/{projectId}/backfill-overview`
- `GET /api/story-state/projects/{projectId}/backfill-project-dry-run`

### 8.3 `consistency-check` 的作用

只读诊断，不写状态。

当前检查：

- scene 数
- completed scene 数
- handoff/event/snapshot/patch 数
- 是否存在 `ReaderRevealState`
- 是否存在 `ChapterIncrementalState`
- chapter review 结果及 issue

### 8.4 `backfill-execute` 的作用

当前是幂等回填执行壳，不是任意迁移写入。

它的目标是：

- 只补缺失基线
- 不覆盖新状态
- 不触碰正文主内容

### 8.5 `chapter-state` 当前覆盖

- `openLoops`
- `resolvedLoops`
- `activeLocations`
- `characterEmotions`
- `characterAttitudes`
- `characterStateTags`

## 9. Compatibility Flags

配置前缀：

- `story.compatibility.*`

当前关键开关：

- `legacyWritingCenterEnabled`
- `legacyWritingApiEnabled`
- `chapterWorkspacePrimary`
- `stateCenterPrimary`
- `generationCenterPrimary`
- `storyContextDualReadEnabled`
- `summaryWorkflowDualWriteEnabled`
- `backfillExecuteEnabled`

### 9.1 参数影响

#### `legacyWritingCenterEnabled`

- 控制旧创作中心是否继续暴露

#### `storyContextDualReadEnabled`

- 控制上下文是否保留双读兼容策略

#### `summaryWorkflowDualWriteEnabled`

- 控制摘要工作流是否保持双写兼容

#### `backfillExecuteEnabled`

- 直接控制 `backfill execute` 是否允许真正执行
- 不是纯展示字段

## 10. 前端页面与后端能力映射

### 10.1 创作台

主要读取：

- `project brief`
- `chapter skeleton preview`
- `chapter review`

### 10.2 状态台

主要读取：

- `chapter state`
- `reader reveal state`
- `reader known state`
- `migration compatibility snapshot`
- `backfill overview`
- `backfill project dry-run`
- `consistency-check`

### 10.3 生成台

主要读取：

- `story orchestration preview`
- `chapter review`

### 10.4 章节工作区

主要调用：

- `skeleton-preview`
- `preview`
- `execute`
- `chapter-review`
- `update/delete skeleton scene`
- `generate-stream`
- `accept/reject generated draft`

## 11. 修改一条链时必须同步的地方

### 11.1 改 Summary Workflow

至少同步：

- 后端 controller / service / DTO
- 前端对象页或摘要弹层
- 本文档 `Summary Workflow` 章节
- 相关 plan/progress/report

### 11.2 改 Orchestration / Workspace

至少同步：

- `StorySessionOrchestrationController`
- 工作区前端
- 本文档 `Story Orchestration` / `AI Writing`
- 页面级验收模板

### 11.3 改 State 链

至少同步：

- `StoryStateController`
- 状态台
- 本文档 `Story State`
- 回放矩阵或样本说明

### 11.4 改样本章节

至少同步：

- `docs/test-data/TESTDATA-20260424-phase10-replay-matrix-v1.md`
- `docs/guides/GUIDE-20260424-phase10-page-acceptance-template-v1.md`
- `docs/agent-context.md`

## 12. 推荐阅读顺序

后续模型接手时，优先读：

1. `docs/agent-context.md`
2. `docs/agent-handoff-rules.md`
3. 本文档
4. `docs/reports/REPORT-20260424-platform-upgrade-final-closure-v1.md`
5. `docs/test-data/TESTDATA-20260424-phase10-replay-matrix-v1.md`
6. `docs/guides/GUIDE-20260424-phase10-page-acceptance-template-v1.md`

再决定是否深入历史 phase 文档或代码。

## 贡献与署名说明

- 本文档由 Codex 基于当前代码、接口、前端入口和阶段收口结果整理撰写。
