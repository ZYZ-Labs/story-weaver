# Story Summary 回生成与统一读模型说明

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Arch ID: ARCH-REQ-20260412-story-summary-read-model-v1
- Status: Active
- Created At: 2026-04-12 Asia/Shanghai
- Updated At: 2026-04-12 Asia/Shanghai

## 目标

把 `SummaryFacet` 从“各个 assembler 内部临时拼字符串”收口为统一规则服务，并把 `StoryUnit` 的对外读取口径收口为稳定的投影视图。

这一层主要服务：

- 用户侧默认摘要展示
- `Phase 3` 的 `Summary First` 工作流
- `Phase 4` 的 MCP 只读查询
- 后续多 session 编排的轻量上下文读取

## 本轮落地

已新增：

- `StorySummaryDraft`
- `StorySummaryService`
- `RuleBasedStorySummaryService`

已收口：

- `CharacterStoryUnitAssembler`
- `WorldSettingStoryUnitAssembler`
- `ChapterStoryUnitAssembler`

已统一查询出口：

- `StoryUnitQueryService#getProjected`
- `StoryUnitQueryService#listProjected`

## 规则说明

### 1. 摘要输入不直接等于摘要输出

assembler 不再直接生成最终 `DefaultSummaryFacet`，而是先组装 `StorySummaryDraft`：

- `displayTitle`
- `oneLineCandidates`
- `longSummaryCandidates`
- `stateFacts`
- `relationFacts`
- `changeFacts`
- `pendingQuestions`

### 2. 回生成规则

`RuleBasedStorySummaryService` 的首轮规则是：

- `displayTitle`
  - 取 `displayTitle -> oneLine -> longSummary` 的第一个非空值
- `oneLineSummary`
  - 取 `oneLineCandidates` 的第一个非空值，缺失时回退到 `displayTitle`
- `longSummary`
  - 取 `longSummaryCandidates` 的第一个非空值，缺失时回退到 `oneLineSummary`
- `stateSummary / relationSummary / changeSummary`
  - 按事实列表去重后用 `；` 拼接
- `pendingQuestions`
  - 去重、去空值后保留为列表

### 3. 当前刻意不做的事

- 不引入模型生成摘要
- 不在本阶段做多语言摘要
- 不在本阶段做富格式摘要
- 不在本阶段做 `state/reveal/execution` 的复杂推断

## 查询口径

`StoryUnitQueryService` 现在以 `ProjectedStoryUnit` 为统一读模型：

- `getProjected(ref)`
- `listProjected(projectId, unitType)`

而不是继续让上层分别去取：

- `StoryUnit`
- `SummaryFacet`
- 其他 facet

这样做的原因是：

- MCP 和后续编排层要读的是“一个稳定对象视图”
- 不应该继续依赖上层自己拼装摘要和 facet

## 下一步

- `Phase 3` 在对象交互层默认展示 `SummaryFacet`
- 后续 `summary -> structured patch -> summary` 双向流，将继续复用 `StorySummaryDraft / StorySummaryService`
- `Phase 4` 的 MCP 只读能力，优先消费 `ProjectedStoryUnit`

## 贡献与署名说明

- `Summary First`、用户默认只看摘要、结构字段主要服务于 MCP/LSP 的方向：用户提出。
- `StorySummaryDraft`、规则型摘要回生成、统一读模型收口实现：Codex 完成。
- 本文档由 Codex 撰写整理，方案基于双方共同讨论形成。
