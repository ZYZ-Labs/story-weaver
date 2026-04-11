# Story 生成可靠性、减法交互与结构化沉淀重构 实施计划

- Req ID: REQ-20260409-generation-reliability-refactor
- Plan Version: v2
- Status: In Progress
- Created At: 2026-04-10 Asia/Shanghai
- Updated At: 2026-04-10 Asia/Shanghai

## 实施范围

本计划聚焦“真实生成链路的可靠性治理 + 作者前台做减法”。目标不是继续横向扩展模块，而是把现有世界观 / 大纲 / 剧情 / 因果 / 人物 / 章节收敛成一条更像写作产品、而不是后台建模工具的生产线。

本计划明确以下边界：

- 保持现有 `/api/ai-writing/*` 对外路径不变。
- 不重做整个站点，也不再起一套平行故事模型。
- 优先复用现有核心模块重构成果作为内部结构层。
- 普通作者默认不直接面对大批底层字段，优先通过对话 / 引导式输入表达意图。
- 专家模式默认隐藏，但允许任何作者按需主动开启。
- 系统负责把自然语言整理成结构化摘要，并在确认后写回实体字段与可复用摘要数据。

## 产品原则

### 1. 前台做减法，后台保结构

- 作者前台只保留最少必要输入。
- 深层字段下沉为：
  - 系统派生
  - 摘要确认写回
  - 专家模式编辑

### 2. 对话优先于表单，确认优先于自动落库

- 对话 / 引导式输入是默认入口。
- AI 输出的是“建议摘要包”，不是直接改库。
- 所有结构化落盘都要经过用户确认。

### 3. 先解决一致性，再追求智能度

- 先保证人物、POV、章节目标不漂。
- 再追求更复杂的总导、知识检索和 Agent 能力。

### 4. 半开放式创作要有“当前进度判断”

- 当项目已经存在大纲、剧情、章节、因果时，系统不应再把每次新建对象都当成完全从零开始。
- 系统应能基于已有结构推断“当前更像主线推进、支线展开、伏笔铺设、转折、收束还是兑现”。
- 该能力只提供建议，不自动替作者决定。

### 5. `chat` 负责采集澄清，`generate` 负责最终成稿

- 现有正文生成主链应继续保持结果导向的 `generate` 形态。
- 聊天能力主要承担多轮澄清、补问、偏好收集和背景压缩。
- 聊天结果不能直接等同于可生成正文的稳定上下文，必须先经过摘要建议和确认。
- 写作页日志即使展示 `plan / write / check / revise`，也要明确这是生成流水线阶段，而不是持续会话式正文生成。

### 6. 系统知道不等于读者知道

- 世界观、大纲、剧情、因果、历史设定首先是系统侧约束，不是默认读者前情。
- 正文生成必须遵守“揭晓边界”：
  - 已揭晓事实可以承接
  - 未揭晓事实只能约束模型，不能让叙事直接跳过说明
- 空章起稿、首章和新章节开头必须优先完成读者定向，而不是像从中段切入。

### 7. AI 创作新增对象，必须结构化挂起

- AI 可以在生成或摘要过程中提出：
  - 新人物
  - 新因果
  - 新剧情挂点
- 但这些新增不能只留在正文里。
- 新增内容必须通过明确的 function call / 创建接口落为“待确认新增对象”，再由作者继续补全设置。
- 未确认的新增对象不能直接视为稳定 canon。

## 涉及模块

### 需要修改的后端模块

- `backend/src/main/java/com/storyweaver/service/impl/AIWritingServiceImpl.java`
- `backend/src/main/java/com/storyweaver/service/impl/AIWritingChatServiceImpl.java`
- `backend/src/main/java/com/storyweaver/ai/director/application/impl/AIDirectorApplicationServiceImpl.java`
- `backend/src/main/java/com/storyweaver/ai/director/application/tool/DirectorToolExecutor.java`
- `backend/src/main/java/com/storyweaver/service/impl/ChapterServiceImpl.java`
- `backend/src/main/java/com/storyweaver/service/impl/CharacterServiceImpl.java`
- `backend/src/main/java/com/storyweaver/service/impl/OutlineServiceImpl.java`
- `backend/src/main/java/com/storyweaver/controller/ChapterController.java`
- `backend/src/main/java/com/storyweaver/controller/CharacterController.java`
- `backend/src/main/java/com/storyweaver/controller/AIDirectorController.java`
- `backend/src/main/java/com/storyweaver/controller/AIWritingChatController.java`
- `backend/src/main/java/com/storyweaver/domain/entity/AIWritingRecord.java`
- `backend/src/main/java/com/storyweaver/domain/vo/AIWritingResponseVO.java`

### 建议新增的后端支撑模块

- `backend/src/main/java/com/storyweaver/story/generation/ChapterAnchorBundle.java`
- `backend/src/main/java/com/storyweaver/story/generation/ChapterAnchorResolver.java`
- `backend/src/main/java/com/storyweaver/story/generation/GenerationReadinessService.java`
- `backend/src/main/java/com/storyweaver/story/generation/GenerationReadinessVO.java`
- `backend/src/main/java/com/storyweaver/story/generation/ConversationSummaryService.java`
- `backend/src/main/java/com/storyweaver/story/generation/SummarySuggestionPack.java`
- `backend/src/main/java/com/storyweaver/story/generation/StructuredSummaryApplyService.java`
- `backend/src/main/java/com/storyweaver/story/generation/StoryProgressPredictor.java`
- `backend/src/main/java/com/storyweaver/story/generation/StoryProgressSuggestionVO.java`
- `backend/src/main/java/com/storyweaver/story/generation/StoryConsistencyInspector.java`

### 需要修改的前端模块

- `front/src/types/index.ts`
- `front/src/views/chapter/ChapterListView.vue`
- `front/src/views/character/CharacterListView.vue`
- `front/src/views/writing/WritingView.vue`
- `front/src/api/chapter.ts`
- `front/src/api/character.ts`
- `front/src/api/ai-director.ts`
- `front/src/components/AIDirectorDecisionCard.vue`
- `front/src/components/AIProcessLogPanel.vue`

### 建议新增的前端模块

- `front/src/components/GenerationReadinessCard.vue`
- `front/src/components/ChapterAnchorPanel.vue`
- `front/src/components/ConversationSummaryReviewDialog.vue`
- `front/src/components/StoryBriefPromptPanel.vue`
- `front/src/components/StoryProgressSuggestionCard.vue`

### SQL / 迁移文件

- `sql/015_generation_reliability_refactor.sql`

说明：

- 本次不新增新的大业务表。
- 优先利用现有实体字段和 `knowledge_document` 做摘要沉淀。
- `015` 重点补生成链路可观测字段与摘要来源分类，不再扩一批新业务实体。

## 最小作者模型

普通作者默认只面对这几类对象：

### 1. 项目摘要 `Project Brief`

作者只需要说明：

- 这是什么故事
- 主角是谁
- 核心冲突是什么
- 故事大概要走到哪里

系统再把这些整理到：

- 项目描述
- 项目标签 / 类型
- 全书总纲摘要
- 项目级 canon 摘要

### 2. 人物卡 `Character Card`

默认只要求：

- 这个人是谁
- 他想要什么
- 他和主角是什么关系
- 当前阶段是什么状态

其余高级字段下沉为：

- AI 摘要建议
- 专家模式补录

### 3. 章节 brief `Chapter Brief`

默认只要求：

- 这一章谁视角
- 这一章要发生什么
- 本章必须出现谁
- 这一章读者会第一次知道什么
- 这一章写到哪里停
- 这一章不能违背什么

系统再把其转换为：

- `outlineId`
- `mainPovCharacterId`
- `requiredCharacterIds`
- `storyBeatIds`
- `chapter.summary`
- `chapter anchor bundle`

### 4. 进度预测 `Story Progress Suggestion`

默认只服务专家 / 开发者模式，但普通作者也可主动开启专家模式后使用。

系统根据当前已有：

- 已有章节
- 已有剧情节点
- 已有因果
- 当前卷纲 / 章纲

预测当前即将创建的对象更像：

- `mainline_advance`
- `side_branch`
- `foreshadow`
- `turning`
- `payoff`
- `convergence`

说明：

- 这是“建议标签 + 建议理由”，不是自动填表或自动创建。
- 用户不点击按钮，不触发预测。

## 数据模型设计

### 1. `SummarySuggestionPack` 作为对话输入后的建议摘要包

该对象为服务层 DTO，不直接持久化。

字段建议：

- `scope`
  - `project`
  - `character`
  - `chapter`
- `rawInputSummary`
- `structuredFields`
- `canonSummaryText`
- `readerRevealGoals[]`
- `missingQuestions[]`
- `confidenceHints[]`
- `proposedCreates[]`

用途：

- 把作者的自然语言输入转成“待确认的结构化摘要包”。
- 避免作者直接编辑大量意义不明的底层字段。
- 显式记录“本轮需要揭晓给读者的内容”和“AI 提议新增但尚未落库的结构对象”。

### 2. `ChapterAnchorBundle` 作为章节生成的统一锚点包

该对象为服务层派生对象。

字段建议：

- `chapterId`
- `projectId`
- `chapterOutlineId`
- `volumeOutlineId`
- `mainPovCharacterId`
- `mainPovCharacterName`
- `requiredCharacterIds`
- `requiredCharacterNames`
- `storyBeatIds`
- `storyBeatTitles`
- `relatedWorldSettingIds`
- `relatedWorldSettingNames`
- `chapterSummary`
- `chapterStatus`
- `anchorSources`
  - `explicit`
  - `derived_from_outline`
  - `derived_from_summary`

组装规则：

- 优先使用章节显式绑定。
- 缺失时允许从章纲 / 摘要包派生，但必须标记来源。

### 3. `GenerationReadinessVO` 作为生成前校验结果

返回结构至少包括：

- `score` `0-100`
- `status`
  - `ready`
  - `warning`
  - `blocked`
- `blockingIssues[]`
- `warnings[]`
- `resolvedAnchors`
- `recommendedModules[]`

关键校验项：

- 是否存在章节级约束摘要或章纲
- 是否存在 POV
- 是否存在至少一个人物锚点
- 是否存在至少一个剧情推进锚点

### 4. `StoryProgressSuggestionVO` 作为进度预测结果

返回结构建议：

- `scope`
  - `outline`
  - `plot`
  - `chapter`
- `predictedProgressType`
- `confidence`
- `reasons[]`
- `basedOn`
  - `existingChapters`
  - `existingPlots`
  - `existingCausalities`
  - `currentOutline`
- `recommendedFields`

用途：

- 让开发者快速判断当前更适合创建什么类型的大纲、剧情或章节。
- 让半开放式创作具备“节奏感知”，而不是每次都靠人工脑补当前进度。

### 5. 摘要沉淀策略

优先复用现有实体与 `knowledge_document`：

- 项目级稳定摘要：
  - `knowledge_document.source_type = canon_project`
- 人物级稳定摘要：
  - `knowledge_document.source_type = canon_character`
- 章节级 brief / stable summary：
  - `knowledge_document.source_type = canon_chapter`
- 生成缓存 / 草稿摘要：
  - `knowledge_document.source_type = draft_cache`

说明：

- 稳定摘要和生成缓存必须分开，避免漂移内容反向污染 canon。

### 6. 交接模型 `Chat -> Summary -> Generate`

目标：

- 避免把聊天记录直接当成正文生成输入。
- 保证最终生成只消费已确认的稳定约束和必要背景。

交接规则：

- `chat` 链路负责：
  - 追问缺失信息
  - 收集作者偏好
  - 整理背景上下文
- `summary` 链路负责：
  - 产出 `SummarySuggestionPack`
  - 标出缺失项和不确定项
  - 区分“系统已知事实”和“本章准备揭晓给读者的事实”
  - 提出新人物 / 新因果的待确认新增建议
  - 等待用户确认
- `generate` 链路负责：
  - 消费 `ChapterAnchorBundle`
  - 消费确认后的章节 brief / canon 摘要
  - 执行 `plan / write / check / revise` 流水线

说明：

- 生成阶段可以读取聊天沉淀出的参与上下文，但不直接依赖实时会话补救缺失 prompt。
- 这样可以保留聊天的纠偏能力，同时避免正文生成链路因 prompt 漏项而不可追踪。

### 7. `ReaderRevealConstraint` 作为读者揭晓边界约束

该对象可为服务层 DTO，也可先作为 `SummarySuggestionPack / ChapterAnchorBundle` 的内嵌结构。

字段建议：

- `chapterId`
- `openingMode`
  - `cold_open`
  - `chapter_continue`
- `readerKnownFacts[]`
- `revealTargets[]`
- `forbiddenAssumptions[]`

用途：

- 明确哪些事实已经在正文被揭晓，可直接承接。
- 明确哪些事实虽然系统知道，但本章仍需先通过场景、动作、对话逐步揭示。
- 避免空章、第一章或新章节开头出现“从中段直接开始”的观感。

### 8. `StructuredCreationSuggestion` 作为 AI 新增结构候选

该对象为服务层 DTO，不直接等同于最终 canon。

字段建议：

- `entityType`
  - `character`
  - `causality`
  - `plot`
- `operation`
  - `create`
- `candidateFields`
- `sourceExcerpt`
- `sourceChapterId`
- `requiresConfirmation`

用途：

- 让 AI 在生成或摘要阶段产生的新人物 / 新因果有显式承接链路。
- 通过指定 function call / 创建接口先落为待确认对象，再由作者继续设置。
### 9. `ai_writing_record` 增强可观测字段

新增字段建议：

- `director_status` VARCHAR(32) NULL
- `director_error_message` VARCHAR(500) NULL
- `anchor_snapshot_json` JSON NULL
- `context_module_trace_json` JSON NULL
- `summary_source_trace_json` JSON NULL

用途：

- 回放当时的总导模式、锚点包和摘要来源。
- 弥补当前“有章节内容但项目下无写作记录”的调试断链问题。

## 接口设计

## 1. 项目摘要建议接口

- Method: `POST`
- Path: `/api/projects/{projectId}/story-brief/suggest`
- Request Body:
  - `inputText`
  - `contextMode`
    - `free_text`
    - `guided_answers`
- Response Body:
  - `SummarySuggestionPack`
- 说明:
  - 用于把自然语言项目设定整理成待确认摘要包

## 2. 人物摘要建议接口

- Method: `POST`
- Path: `/api/projects/{projectId}/characters/suggest`
- Request Body:
  - `inputText`
  - `characterId` 可选
- Response Body:
  - `SummarySuggestionPack`

## 3. 章节 brief 建议接口

- Method: `POST`
- Path: `/api/projects/{projectId}/chapters/{chapterId}/brief/suggest`
- Request Body:
  - `inputText`
  - `currentContext` 可选
- Response Body:
  - `SummarySuggestionPack`

## 4. 进度预测接口

- Method: `POST`
- Path:
  - `/api/projects/{projectId}/outlines/progress-suggest`
  - `/api/projects/{projectId}/plotlines/progress-suggest`
  - `/api/projects/{projectId}/chapters/progress-suggest`
- Request Body:
  - `contextText` 可选
  - `targetOutlineId` / `targetChapterId` 可选
- Response Body:
  - `StoryProgressSuggestionVO`
- 说明:
  - 仅在按钮触发时执行
  - 仅给建议，不直接落库

## 5. 摘要确认写回接口

- Method: `POST`
- Path: `/api/structured-summaries/apply`
- Request Body:
  - `scope`
  - `targetId`
  - `structuredFields`
  - `canonSummaryText`
- Response Body:
  - 更新后的目标对象
  - 最新 `readiness`（若是 chapter）

## 6. 章节生成就绪度接口

- Method: `GET`
- Path: `/api/projects/{projectId}/chapters/{chapterId}/generation-readiness`
- Response Body:
  - `score`
  - `status`
  - `blockingIssues`
  - `warnings`
  - `resolvedAnchors`
  - `recommendedModules`

## 7. 章节锚点统一写入口

- Method: `PUT`
- Path: `/api/projects/{projectId}/chapters/{chapterId}/anchors`
- Request Body:
  - `outlineId`
  - `mainPovCharacterId`
  - `requiredCharacterIds`
  - `storyBeatIds`
- Response Body:
  - 更新后的章节锚点信息
  - 最新 `generationReadiness`

## 8. 现有写作接口的内部接入

- `POST /api/ai-writing/generate`
- `POST /api/ai-writing/generate-stream`

内部新增行为：

1. 先计算 `GenerationReadiness`
2. 若为 `blocked`，返回明确错误或提示
3. 生成时写入 `anchor_snapshot_json`
4. 生成时记录 `summary_source_trace_json`
5. 总导结果必须标记当前模式：
   - `tool_success`
   - `fallback`
6. 明确记录当前执行形态：
   - `generation_pipeline`
7. 若存在聊天参与上下文，只记录其摘要来源，不把 UI 误导为“正文基于持续会话生成”

## 9. 总导接口增强

- 保留：
  - `GET /api/ai-director/chapter/{chapterId}/latest`
  - `GET /api/ai-director/{decisionId}`

返回体补充：

- `mode`
  - `tool_success`
  - `fallback`
- `failureReason`
- `selectedAnchorSummary`

## 前端交互设计

### 1. 项目与人物输入改为“自然语言优先”

- 普通模式默认不展示大量底层字段。
- 提供“说一段 / 写一段 -> 系统整理 -> 你确认”的交互。
- 只有进入专家模式时，才暴露深层结构字段。
- 专家模式是可选开关，普通作者也可按需开启。

### 2. 章节页

- 章节头部新增 `GenerationReadinessCard`
- 新增 `ChapterAnchorPanel`
- 明确展示：
  - 当前绑定章纲或章节 brief
  - POV
  - 必出人物
  - 剧情节点
  - readiness 分数与阻塞项
- 提供“快速补齐锚点”和“根据自然语言整理本章摘要”入口
- 在专家模式中增加“预测当前章节进度”按钮，展示 `StoryProgressSuggestionCard`

### 3. 大纲页与剧情页

- 在专家模式中提供“预测当前更适合创建什么类型的大纲 / 剧情”按钮。
- 返回结果只展示：
  - 建议类型
  - 建议理由
  - 依据哪些已有章节 / 剧情 / 因果
- 不自动修改表单。

### 4. 写作页

- 生成开始前先展示本轮 anchor pack 摘要
- 若总导为 fallback，必须显式显示
- 若当前章节 readiness 不足，页面先提示问题，不直接进入高自由度生成
- 提供“把这段想法整理成章节约束”的入口
- 明确展示“本次为生成流水线执行”，避免把阶段日志误解为持续聊天会话

### 5. 摘要确认

- 所有结构化建议都进入确认卡或确认弹窗
- 允许：
  - 整包应用
  - 局部应用
  - 回退修改原文再重试

## 兼容性处理

### 双模式策略

- 普通模式：
  - 对话 / 摘要优先
  - 字段最小化
- 专家模式：
  - 允许直接编辑结构化字段
  - 可查看进度预测、锚点来源和更多调试信息

说明：

- 专家模式不是权限隔离，而是交互层级开关；普通作者也可主动开启。

### 回退策略

- 若总导真实工具调用仍不稳定，明确返回 `fallback`
- 若摘要建议效果不好，作者仍可直接编辑少量关键锚点
- 摘要确认前不写库，避免 AI 错误理解直接污染项目数据

## 实施步骤

### Step 0

- 目标:
  - 固定线上问题和减法方向
- 改动:
  - 完成线上诊断报告
  - 归档旧主线文档
  - 输出 v2 计划
- 完成标准:
  - 当前主线只剩一个 requirement，边界清晰

### Step 1

- 目标:
  - 固定最小作者模型、摘要包协议与 `chat / generate` 边界
- 改动:
  - 定义 `SummarySuggestionPack`
  - 定义 `ChapterAnchorBundle`
  - 定义 `GenerationReadinessVO`
  - 定义 `StoryProgressSuggestionVO`
  - 固定聊天采集、摘要确认、最终生成三段交接规则
- 完成标准:
  - 系统已明确“作者前台最小输入”与“后台结构摘要”的分工
  - 系统已明确“聊天澄清”和“正文生成”不是同一条执行语义

### Step 2

- 目标:
  - 落地项目 / 人物 / 章节的摘要建议与确认流
- 改动:
  - 新增 `story-brief/character/chapter brief` 建议接口
  - 新增统一摘要确认写回接口
  - 前端新增摘要确认弹窗
  - 让聊天上下文可进入摘要建议流，但不直接进入正文生成主调用
- 完成标准:
  - 作者可以通过自然语言输入获得待确认的结构化摘要包

### Step 3

- 目标:
  - 落地专家模式下的进度预测能力
- 改动:
  - 新增 `StoryProgressPredictor`
  - 新增 `progress-suggest` 系列接口
  - 前端新增 `StoryProgressSuggestionCard`
- 完成标准:
  - 大纲 / 剧情 / 章节在按钮触发后都可得到当前进度类型建议

### Step 4

- 目标:
  - 落地章节锚点解析与生成就绪度
- 改动:
  - 新增 `ChapterAnchorResolver`
  - 新增 `GenerationReadinessService`
  - 新增 `GET /generation-readiness`
- 完成标准:
  - 任一章节都可得到明确的 readiness 结果

### Step 5

- 目标:
  - 落地章节页锚点面板与最小锚点写入口
- 改动:
  - 新增 `PUT /anchors`
  - 前端新增 readiness 卡片和 anchor 面板
- 完成标准:
  - 用户可以在同一页面补齐生成关键锚点

### Step 6

- 目标:
  - 治理总导真实兼容性与 fallback 可见性
- 改动:
  - 调整 `AIDirectorApplicationServiceImpl`
  - 调整 `DirectorToolExecutor`
  - 增强总导接口和展示
- 完成标准:
  - UI 能明确区分 `tool_success` 与 `fallback`
  - 至少定位清楚 DeepSeek 兼容问题发生在何处

### Step 7

- 目标:
  - 把 readiness、anchor snapshot 和 summary trace 接入写作链路
- 改动:
  - 修改 `AIWritingServiceImpl`
  - 增强 `AIWritingRecord`
  - 调整写作页和流式日志语义展示
- 完成标准:
  - 每次生成都能回放其锚点、摘要来源和总导模式
  - UI 不再把多阶段生成流水线误解为持续聊天正文生成

### Step 8

- 目标:
  - 用真实项目做一致性复验
- 改动:
  - 对 `旧日王座` 做端到端验证
  - 增加 `StoryConsistencyInspector`
- 完成标准:
  - 主角命名、POV、章节推进的一致性显著改善

### Step 9

- 当前状态:
  - 已完成第二轮线上复验，确认新字段已生效，但结尾完整性仍需继续修
- 目标:
  - 落地读者揭晓边界与空章起稿保护
- 改动:
  - 为摘要包和章节锚点补充 `readerRevealGoals / openingMode`
  - 为写作 prompt 与总导决策加入“系统已知 != 读者已知”约束
  - 为空章起稿、第一章和首轮续写增加开场定向检查
  - 为生成结果增加“半截入场 / 半句截断 / 未完成收束”检测
- 完成标准:
  - 空章起稿不再默认读者已知前情
  - 首轮续写不再像从章节中段直接开始

### Step 10

- 当前状态:
  - 已完成第二轮线上复验，确认建议链路已生效；待验证真实创建闭环
- 目标:
  - 落地 AI 新增人物 / 因果的结构化创建链路
- 改动:
  - 为摘要建议或生成后处理新增 `StructuredCreationSuggestion`
  - 提供显式 function call / 创建接口，用于新增人物 / 因果候选
  - 前端增加待确认新增对象面板，允许作者继续补完设置
- 完成标准:
  - AI 创作出的新人物 / 新因果不再只存在于正文里
  - 新增结构可以被作者确认、修改并沉淀为可复用数据

## 每一步的完成标准

- Step 1 完成后，系统知道“作者到底只该填什么”
- Step 2 完成后，系统可以把自然语言稳定变成待确认摘要
- Step 3 完成后，系统能给出“当前应该是什么进度 / 节奏”的建议
- Step 4 完成后，系统能先回答“这章是否适合生成”
- Step 5 完成后，系统能快速补齐生成锚点
- Step 6 完成后，系统能真实区分总导成功和 fallback
- Step 7 完成后，生成记录不再不可追踪
- Step 7 完成后，写作日志语义与真实执行模型一致
- Step 8 完成后，真实项目样本可作为回归基线
- Step 9 完成后，系统能显式控制“本章哪些内容可被读者默认知道”
- Step 10 完成后，AI 生成的新人物 / 新因果可以进入结构化确认流

## 验证方案

### 后端验证

- 新增服务层测试：
  - `ConversationSummaryService`
  - `StructuredSummaryApplyService`
  - `StoryProgressPredictor`
  - `ChapterAnchorResolver`
  - `GenerationReadinessService`
  - 总导模式识别
- 新增接口测试：
  - 摘要建议接口
  - 进度预测接口
  - 摘要确认写回接口
  - `/generation-readiness`
  - `/anchors`
  - 写作接口在 `blocked / warning` 状态下的行为
- 执行：
  - `mvn -f backend/pom.xml test`
  - 至少执行 `mvn -f backend/pom.xml -DskipTests compile`

### 前端验证

- `npm run build`
- 普通模式下默认字段数量明显下降
- 专家模式下可看到按钮触发式的进度预测建议
- 普通作者手动开启专家模式后，也能看到对应高级能力
- 章节页可看到 readiness 和 anchor 面板
- 写作页可看到本轮总导模式和 anchor 摘要
- 写作页明确标注本次为生成流水线，而不是持续聊天会话
- 摘要确认弹窗能正确展示和应用建议
- 空章起稿和首轮续写不会出现明显“从中段闯入”的开场
- 若 AI 提议新增人物 / 因果，页面能展示待确认新增对象并支持作者补完

### 人工验收

- 以 `旧日王座` 为样本：
  - 章节 31-34 的章纲 / POV / 人物 / story beat 绑定能被清晰看到
  - 新建大纲 / 剧情 / 章节时，专家模式下能获得符合当前项目进度的建议类型
  - 再次生成时，不应出现主角姓名连续漂移
  - 总导若 fallback，页面必须明确提示
  - 作者不需要理解一堆字段，也能完成一章的准备工作
  - 第一章或空章起稿时，读者不会感到“默认已经看过上文”
  - 若正文中出现 AI 创作的新人物 / 新因果，系统能产出对应待确认结构对象

## 风险与回退

### 风险 1

- 风险:
  - 对话总结可能产生“理解错但说得很像对”的建议包
- 处理:
  - 所有摘要先确认再写回
  - 支持局部应用和回退
  - 不让未确认聊天内容直接进入最终正文生成主链

### 风险 2

- 风险:
  - readiness 规则过严会影响当前可用性
- 处理:
  - 第一阶段支持 warning 模式，通过配置逐步转向强阻断

### 风险 3

- 风险:
  - 总导兼容问题可能来自 Provider 特性，短期内无法彻底解决
- 处理:
  - 先把 fallback 暴露清楚，再决定是否切模型、改协议或保留显式启发式模式

### 风险 4

- 风险:
  - 旧项目历史数据缺锚点较多，补齐成本高
- 处理:
  - 章节页提供最短路径补齐入口
  - 允许先从摘要建议流补齐，而不是要求用户手动补很多字段

### 风险 5

- 风险:
  - 进度预测若做得过重，会重新把系统拉回“复杂但不好懂”
- 处理:
  - 只在专家 / 开发者模式中暴露
  - 只做按钮触发式建议，不做自动执行

### 风险 6

- 风险:
  - 过度强调“读者定向”可能把正文写成生硬说明文
- 处理:
  - 约束的是“必须揭晓”，不是“必须解释”
  - 优先通过场景、动作、对话完成定向，而不是大段世界观讲解

### 风险 7

- 风险:
  - AI 新增对象若直接写库，会重新污染 canon
- 处理:
  - 一律先进入待确认创建流
  - 保留来源片段、来源章节和创建时间，支持作者拒绝或编辑

### 回退方案

- 普通模式的新摘要流可逐步开关发布
- 专家模式保留直接编辑关键结构字段的能力
- 写作接口保留当前 fallback 能力
- 观测字段和 canon 分类可保留，不影响旧数据读取
