# Story 生成可靠性与故事锚点重构 实施计划

- Req ID: REQ-20260409-generation-reliability-refactor
- Plan Version: v1
- Status: Archived
- Created At: 2026-04-09 Asia/Shanghai
- Updated At: 2026-04-10 Asia/Shanghai

## 实施范围

本计划聚焦“真实生成链路的可靠性治理”，目标不是继续横向扩展模块，而是把现有世界观 / 大纲 / 剧情 / 因果 / 人物 / 章节收敛成一条稳定的章节生产线。

本计划明确以下边界：

- 保持现有 `/api/ai-writing/*` 对外路径不变。
- 不重做现有聊天写作面板。
- 复用当前核心模块重构结果，不再起一套平行故事模型。
- 第一阶段优先治理：
  - 总导成功率与 fallback 可见性
  - 章节锚点包
  - 生成前就绪度
  - 生成追踪链

## 涉及模块

### 需要修改的后端模块

- `backend/src/main/java/com/storyweaver/service/impl/AIWritingServiceImpl.java`
- `backend/src/main/java/com/storyweaver/ai/director/application/impl/AIDirectorApplicationServiceImpl.java`
- `backend/src/main/java/com/storyweaver/ai/director/application/tool/DirectorToolExecutor.java`
- `backend/src/main/java/com/storyweaver/service/impl/ChapterServiceImpl.java`
- `backend/src/main/java/com/storyweaver/controller/ChapterController.java`
- `backend/src/main/java/com/storyweaver/controller/AIDirectorController.java`
- `backend/src/main/java/com/storyweaver/domain/entity/AIWritingRecord.java`
- `backend/src/main/java/com/storyweaver/domain/vo/AIWritingResponseVO.java`

### 建议新增的后端支撑模块

- `backend/src/main/java/com/storyweaver/story/generation/ChapterAnchorBundle.java`
- `backend/src/main/java/com/storyweaver/story/generation/ChapterAnchorResolver.java`
- `backend/src/main/java/com/storyweaver/story/generation/GenerationReadinessService.java`
- `backend/src/main/java/com/storyweaver/story/generation/GenerationReadinessVO.java`
- `backend/src/main/java/com/storyweaver/story/generation/ChapterBriefAssembler.java`
- `backend/src/main/java/com/storyweaver/story/generation/StoryConsistencyInspector.java`

### 需要修改的前端模块

- `front/src/types/index.ts`
- `front/src/views/chapter/ChapterListView.vue`
- `front/src/views/writing/WritingView.vue`
- `front/src/api/chapter.ts`
- `front/src/api/ai-director.ts`
- `front/src/components/AIDirectorDecisionCard.vue`
- `front/src/components/AIProcessLogPanel.vue`

### 建议新增的前端模块

- `front/src/components/GenerationReadinessCard.vue`
- `front/src/components/ChapterAnchorPanel.vue`

### SQL / 迁移文件

- `sql/015_generation_reliability_refactor.sql`

说明：

- 本次不新增新的长期业务主表。
- `015` 优先用于补生成链路可观测字段，不再引入更多平行业务实体。

## 数据模型设计

### 1. `ChapterAnchorBundle` 作为章节生成的统一锚点包

该对象为服务层派生对象，不作为第一阶段长期业务表。

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

组装规则：

- 优先使用章节显式绑定。
- 缺失时允许从章纲 / 卷纲派生，但必须标记为“派生值”而不是“显式值”。

### 2. `GenerationReadinessVO` 作为生成前校验结果

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

第一阶段关键校验项：

- 是否存在章节级大纲或可接受的章级替代锚点
- 是否存在 POV
- 是否存在至少一个人物锚点
- 是否存在至少一个剧情推进锚点

### 3. `ai_writing_record` 增强可观测字段

新增字段建议：

- `director_status` VARCHAR(32) NULL
- `director_error_message` VARCHAR(500) NULL
- `anchor_snapshot_json` JSON NULL
- `context_module_trace_json` JSON NULL

用途：

- 让一次生成能够回放当时的总导模式和锚点包。
- 弥补当前“有章节内容但项目下无写作记录”的调试断链问题。

## 接口设计

## 1. 章节生成就绪度接口

- Method: `GET`
- Path: `/api/projects/{projectId}/chapters/{chapterId}/generation-readiness`
- Path Params:
  - `projectId`
  - `chapterId`
- Response Body:
  - `score`
  - `status`
  - `blockingIssues`
  - `warnings`
  - `resolvedAnchors`
  - `recommendedModules`
- Validation:
  - 用户必须可访问对应项目和章节
- Error Cases:
  - 项目或章节不存在

## 2. 章节锚点统一写入口

- Method: `PUT`
- Path: `/api/projects/{projectId}/chapters/{chapterId}/anchors`
- Request Body:
  - `outlineId`
  - `mainPovCharacterId`
  - `requiredCharacterIds`
  - `storyBeatIds`
- Response Body:
  - 更新后的章节摘要字段
  - 更新后的锚点信息
  - 最新 `generationReadiness`
- Validation:
  - 章节与锚点必须属于同一项目
  - 不允许跨项目绑定人物和剧情节点
- Error Cases:
  - 锚点不存在
  - 锚点不属于当前项目

## 3. 章节 brief 预览接口

- Method: `GET`
- Path: `/api/projects/{projectId}/chapters/{chapterId}/brief`
- Response Body:
  - `chapterAnchorBundle`
  - `readiness`
  - `briefText`
- 说明:
  - 第一阶段优先作为派生只读对象，不单独持久化

## 4. 现有写作接口的内部接入

- `POST /api/ai-writing/generate`
- `POST /api/ai-writing/generate-stream`

内部新增行为：

1. 先计算 `GenerationReadiness`
2. 若为 `blocked`，返回明确错误或提示
3. 生成时写入 `anchor_snapshot_json`
4. 总导结果必须标记当前模式：
   - `tool_success`
   - `fallback`

## 5. 总导接口增强

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

### 1. 章节页

- 章节头部新增 `GenerationReadinessCard`
- 明确展示：
  - 当前绑定章纲
  - POV
  - 必出人物
  - 剧情节点
  - readiness 分数与阻塞项
- 提供“快速补齐锚点”入口，而不是让用户分散跳转多个模块后再回来生成

### 2. 写作页

- 生成开始前先展示本轮 anchor pack 摘要
- 若总导为 fallback，必须显式显示
- 若当前章节 readiness 不足，页面先提示问题，不直接进入高自由度生成

### 3. 总导展示

- `AIDirectorDecisionCard` 中把“总导成功”与“启发式兜底”区分展示
- 调试信息不再只展示 pack，还要展示 fallback 原因

## 兼容性处理

### 双模式策略

- 初期允许：
  - `warning` 模式可继续生成
  - `blocked` 模式需显式确认或直接拦截
- 建议增加配置：
  - `story.generation.require_anchor_pack`
  - `story.generation.block_on_missing_pov`
  - `story.generation.block_on_missing_outline`

### 回退策略

- 若总导真实工具调用仍不稳定，明确返回 `fallback`
- 但前端和日志必须把该状态展示出来，不能伪装成正常总导成功

## 实施步骤

### Step 0

- 目标:
  - 固定线上真实问题和改造边界
- 改动:
  - 完成线上诊断报告
  - 新建 requirement / plan / progress 文档
- 完成标准:
  - 已明确首批验收样本与改造优先级

### Step 1

- 目标:
  - 落地章节锚点解析与就绪度接口
- 改动:
  - 新增 `ChapterAnchorResolver`
  - 新增 `GenerationReadinessService`
  - 新增 `GET /generation-readiness`
- 完成标准:
  - 任一章节都可得到明确的 readiness 结果

### Step 2

- 目标:
  - 落地章节页锚点面板与统一写入口
- 改动:
  - 新增 `PUT /anchors`
  - 前端新增 readiness 卡片和 anchor 面板
- 完成标准:
  - 用户可以在同一页面补齐生成关键锚点

### Step 3

- 目标:
  - 治理总导真实兼容性与 fallback 可见性
- 改动:
  - 调整 `AIDirectorApplicationServiceImpl`
  - 调整 `DirectorToolExecutor`
  - 增强总导接口和展示
- 完成标准:
  - UI 能明确区分 `tool_success` 与 `fallback`
  - 至少定位清楚 DeepSeek 兼容问题发生在何处

### Step 4

- 目标:
  - 把 readiness 和 anchor snapshot 接入写作链路
- 改动:
  - 修改 `AIWritingServiceImpl`
  - 增强 `AIWritingRecord`
- 完成标准:
  - 每次生成都能回放其锚点和总导模式

### Step 5

- 目标:
  - 用真实项目做一致性复验
- 改动:
  - 对 `旧日王座` 做端到端验证
  - 增加 `StoryConsistencyInspector`
- 完成标准:
  - 主角命名、POV、章节推进的一致性显著改善

## 每一步的完成标准

- Step 1 完成后，系统能先回答“这章是否适合生成”
- Step 2 完成后，系统能快速补齐生成锚点
- Step 3 完成后，系统能真实区分总导成功和 fallback
- Step 4 完成后，生成记录不再不可追踪
- Step 5 完成后，真实项目样本可作为回归基线

## 验证方案

### 后端验证

- 新增服务层测试：
  - `ChapterAnchorResolver`
  - `GenerationReadinessService`
  - 总导模式识别
- 新增接口测试：
  - `/generation-readiness`
  - `/anchors`
  - 写作接口在 `blocked` / `warning` 状态下的行为
- 执行：
  - `mvn -f backend/pom.xml test`
  - 至少执行 `mvn -f backend/pom.xml -DskipTests compile`

### 前端验证

- `npm run build`
- 章节页可看到 readiness 和 anchor 面板
- 写作页可看到本轮总导模式和 anchor 摘要

### 人工验收

- 以 `旧日王座` 为样本：
  - 章节 31-34 的章纲 / POV / 人物 / story beat 绑定能被清晰看到
  - 再次生成时，不应出现主角姓名连续漂移
  - 总导若 fallback，页面必须明确提示

## 风险与回退

### 风险 1

- 风险:
  - readiness 规则过严会影响当前可用性
- 处理:
  - 第一阶段支持 warning 模式，通过配置逐步转向强阻断

### 风险 2

- 风险:
  - 总导兼容问题可能来自 Provider 特性，短期内无法彻底解决
- 处理:
  - 先把 fallback 暴露清楚，再决定是否切模型、改协议或保留显式启发式模式

### 风险 3

- 风险:
  - 当前旧项目数据锚点缺失较多，短期补齐成本高
- 处理:
  - 章节页提供最短路径补齐入口，不要求用户在多个模块之间来回跳转

### 回退方案

- 新增 readiness 规则默认可配置关闭
- 写作接口仍保留当前 fallback 能力
- 观测字段可保留，不影响旧数据读取
