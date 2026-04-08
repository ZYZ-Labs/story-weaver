# AI 总导编排层 实施计划

- Req ID: REQ-20260408-ai-director-layer
- Plan Version: v1
- Status: In Progress
- Created At: 2026-04-08 Asia/Shanghai
- Updated At: 2026-04-08 Asia/Shanghai

## 实施范围

本计划的目标是在现有 AI 写作链路前增加一个独立的“总导层 / 决策层”，用于判断当前章节阶段、决定本轮写作该使用哪些上下文模块、生成结构化 `decision pack`，并将该结果交给现有写作层执行正文生成。

本计划明确以下边界：

- 保留现有 `/api/ai-writing/generate` 和 `/api/ai-writing/generate-stream` 对外接口。
- 保留现有 `AIWritingChatService` 的 chat 式背景协作入口，不改成决策层代理聊天。
- 决策层独立配置 Provider / Model，与写作层配置解耦。
- 决策层优先使用 function call / tool calling 获取上下文。
- 决策层失败时退回当前 `AIWritingServiceImpl` 的固定规则聚合逻辑。

## 涉及模块

### 新增后端模块

- `backend/src/main/java/com/storyweaver/ai/director/application/AIDirectorApplicationService.java`
- `backend/src/main/java/com/storyweaver/ai/director/application/impl/AIDirectorApplicationServiceImpl.java`
- `backend/src/main/java/com/storyweaver/ai/director/application/DirectorModuleRegistry.java`
- `backend/src/main/java/com/storyweaver/ai/director/application/DirectorDecisionPackAssembler.java`
- `backend/src/main/java/com/storyweaver/ai/director/application/tool/DirectorToolDefinition.java`
- `backend/src/main/java/com/storyweaver/ai/director/application/tool/DirectorToolExecutor.java`
- `backend/src/main/java/com/storyweaver/ai/director/application/tool/impl/*.java`
- `backend/src/main/java/com/storyweaver/domain/entity/AIDirectorDecision.java`
- `backend/src/main/java/com/storyweaver/repository/AIDirectorDecisionMapper.java`
- `backend/src/main/java/com/storyweaver/domain/dto/AIDirectorDecisionRequestDTO.java`
- `backend/src/main/java/com/storyweaver/domain/vo/AIDirectorDecisionVO.java`
- `backend/src/main/java/com/storyweaver/controller/AIDirectorController.java`

### 需要修改的后端模块

- `backend/src/main/java/com/storyweaver/service/AIProviderService.java`
- `backend/src/main/java/com/storyweaver/service/impl/AIProviderServiceImpl.java`
- `backend/src/main/java/com/storyweaver/service/AIModelRoutingService.java`
- `backend/src/main/java/com/storyweaver/service/impl/AIWritingServiceImpl.java`
- `backend/src/main/java/com/storyweaver/service/impl/SystemConfigServiceImpl.java`
- `backend/src/main/java/com/storyweaver/domain/entity/AIWritingRecord.java`
- `backend/src/main/java/com/storyweaver/domain/vo/AIWritingResponseVO.java`

### 新增或修改的前端模块

- `front/src/types/index.ts`
- `front/src/stores/settings.ts`
- `front/src/views/settings/SettingsView.vue`
- `front/src/api/ai-director.ts`
- `front/src/views/writing/WritingView.vue`

说明：

- `AIWritingChatPanel` 保持交互方式不变。
- v1 前端只增加“决策配置”和可选的只读决策摘要，不改 chat 面板职责。

### SQL / 迁移文件

- `sql/011_ai_director_layer.sql`

## 数据模型

### 1. 新增表 `ai_director_decision`

用途：

- 存储每次决策层运行的结构化结果。
- 让写作生成记录能够追溯当时采用的决策。
- 为后续调试、比对和质量评估提供依据。

建议字段：

- `id` BIGINT PK
- `project_id` BIGINT NOT NULL
- `chapter_id` BIGINT NOT NULL
- `user_id` BIGINT NOT NULL
- `source_type` VARCHAR(32) NOT NULL
  取值：`writing` / `chat` / `manual`
- `entry_point` VARCHAR(64) NOT NULL
  取值示例：`writing-center` / `draft`
- `stage` VARCHAR(32) NOT NULL
  取值：`opening` / `setup` / `advancement` / `turning` / `convergence` / `polish`
- `writing_mode` VARCHAR(32) NOT NULL
  取值：`draft` / `continue` / `expand` / `rewrite` / `polish`
- `target_word_count` INT NULL
- `selected_modules_json` JSON NOT NULL
- `module_weights_json` JSON NOT NULL
- `required_facts_json` JSON NOT NULL
- `prohibited_moves_json` JSON NOT NULL
- `decision_pack_json` JSON NOT NULL
- `tool_trace_json` JSON NOT NULL
- `selected_provider_id` BIGINT NOT NULL
- `selected_model` VARCHAR(128) NOT NULL
- `status` VARCHAR(32) NOT NULL
  取值：`generated` / `applied` / `fallback` / `failed`
- `error_message` VARCHAR(500) NULL
- `create_time` DATETIME
- `update_time` DATETIME
- `deleted` INT DEFAULT 0

索引：

- `idx_ai_director_decision_chapter_id`
- `idx_ai_director_decision_project_id`
- `idx_ai_director_decision_user_id`
- `idx_ai_director_decision_status`

### 2. 修改表 `ai_writing_record`

新增字段：

- `director_decision_id` BIGINT NULL

用途：

- 将正文生成结果与具体 `decision pack` 建立追踪关系。

### 3. v1 不新增的表

以下能力在 v1 先不落库，用代码注册或即时计算解决：

- 模块注册表
- 决策层长期 session
- 决策层独立聊天记录

原因：

- 本次目标是先把“编排职责”独立出来，而不是先做第二套聊天系统。
- 模块规则放代码里更利于弱化 prompt 和做 schema 校验。

## 系统配置设计

### 新增配置项

- `director_ai_provider_id`
- `director_ai_model`
- `ai.director.enabled`
- `ai.director.max_tool_calls`
- `ai.director.max_selected_modules`
- `ai.director.debug_expose_decision`

### 配置行为

- `director_ai_provider_id` / `director_ai_model` 作为决策层默认模型配置。
- 若请求中显式指定决策层模型，则优先使用请求值。
- `ai.director.enabled=false` 时，写作接口直接走现有固定规则链路。
- `ai.director.max_tool_calls` 单独限制决策层，不复用写作工作流的 `ai.workflow.max_tool_calls`。

## 决策层阶段模型

### 有限状态枚举

v1 固定为以下 6 个状态：

- `opening`
  章节开场，优先搭建场景、人物状态和冲突起点
- `setup`
  铺垫阶段，优先交代目标、关系和伏笔
- `advancement`
  冲突推进，优先使用剧情、因果和人物约束
- `turning`
  关键转折，优先使用大纲转折、因果触发和硬性约束
- `convergence`
  收束阶段，优先使用收束方向、结果代价和后续钩子
- `polish`
  润色阶段，减少大范围剧情决策，强调语言质量和一致性

### 阶段判断输入

- 当前章节是否为空
- 当前正文字数
- 写作入口传入的 `writingType`
- 当前大纲中的 `stageGoal` / `keyConflict` / `turningPoints` / `expectedEnding`
- 用户补充要求
- 背景聊天提炼出的剧情推进 / 硬性约束

### 阶段与写作模式的关系

- 阶段描述“剧情处于哪一段”
- `writingMode` 描述“本轮模型执行什么动作”
- `writingMode` 仍沿用现有值：`draft` / `continue` / `expand` / `rewrite` / `polish`

## 模块注册表设计

v1 使用代码注册，不使用数据库配置。

### 模块定义字段

每个模块需在 `DirectorModuleRegistry` 中声明：

- `moduleName`
- `toolName`
- `defaultWeight`
- `maxItems`
- `availableFields`
- `supportedStages`
- `requiredWhen`
- `optionalWhen`

### v1 首批模块

- `chapter_snapshot`
- `outline`
- `plot`
- `causality`
- `world_setting`
- `required_characters`
- `character_inventory`
- `knowledge`
- `chat_background`

### 模块选择规则

- `chapter_snapshot` 必选
- `outline` 在存在章节大纲时高优先
- `plot` / `causality` 在 `advancement` / `turning` 阶段高优先
- `world_setting` 在 `opening` / `setup` 阶段优先
- `required_characters` 和 `chat_background` 在存在对应数据时高优先
- `character_inventory` 仅在章节存在必出人物且有背包数据时启用
- `knowledge` 作为补充模块，不默认抢占核心模块权重

## 工具调用设计

### 工具调用总原则

- 工具只对决策层开放。
- 写作层 v1 不直接调用业务工具，只消费 `decision pack`。
- 工具返回结构化 JSON，不返回长段自然语言。

### v1 工具白名单

#### 1. `getChapterSnapshot`

- 输入：
  - `chapterId`
  - `includeCurrentContent`
- 输出：
  - `chapterId`
  - `projectId`
  - `title`
  - `orderNum`
  - `wordCount`
  - `requiredCharacterIds`
  - `requiredCharacterNames`
  - `currentContentSummary`

#### 2. `getOutlineContext`

- 输入：
  - `projectId`
  - `chapterId`
- 输出：
  - `outlineId`
  - `title`
  - `summary`
  - `stageGoal`
  - `keyConflict`
  - `turningPoints`
  - `expectedEnding`
  - `focusCharacterNames`
  - `relatedPlotIds`
  - `relatedCausalityIds`

#### 3. `getPlotCandidates`

- 输入：
  - `projectId`
  - `chapterId`
  - `relatedPlotIds`
  - `limit`
- 输出：
  - `plots[]`
    - `id`
    - `title`
    - `description`
    - `conflicts`
    - `resolutions`

#### 4. `getCausalityCandidates`

- 输入：
  - `projectId`
  - `relatedCausalityIds`
  - `limit`
- 输出：
  - `causalities[]`
    - `id`
    - `name`
    - `relationship`
    - `description`
    - `conditions`
    - `strength`

#### 5. `getWorldSettingFacts`

- 输入：
  - `projectId`
  - `limit`
- 输出：
  - `worldSettings[]`
    - `id`
    - `name`
    - `category`
    - `description`

#### 6. `getRequiredCharacterState`

- 输入：
  - `projectId`
  - `characterIds`
- 输出：
  - `characters[]`
    - `id`
    - `name`
    - `description`
    - `attributes`
    - `projectRole`

#### 7. `getRequiredCharacterInventory`

- 输入：
  - `projectId`
  - `characterIds`
  - `limitPerCharacter`
- 输出：
  - `inventories[]`
    - `characterId`
    - `characterName`
    - `items[]`

#### 8. `searchKnowledgeDocuments`

- 输入：
  - `projectId`
  - `query`
  - `limit`
- 输出：
  - `documents[]`
    - `id`
    - `title`
    - `summary`
    - `sourceType`

#### 9. `getChatBackgroundSummary`

- 输入：
  - `chapterId`
  - `userId`
- 输出：
  - `worldFacts[]`
  - `characterConstraints[]`
  - `plotGuidance[]`
  - `writingPreferences[]`
  - `hardConstraints[]`

### 工具实现映射

- `getChatBackgroundSummary` 复用 `AIWritingChatService.buildParticipationContext(...)`
- `getPlotCandidates` 复用 `PlotService`
- `getCausalityCandidates` 复用 `CausalityService`
- `getWorldSettingFacts` 复用 `WorldSettingService`
- `searchKnowledgeDocuments` 复用 `KnowledgeDocumentService`
- 人物和背包相关工具复用现有 `CharacterService`、`CharacterInventoryItemMapper`、`ItemMapper`

## 决策输出结构

### `decision pack` JSON Schema

```json
{
  "version": "v1",
  "chapterId": 123,
  "projectId": 45,
  "entryPoint": "writing-center",
  "stage": "advancement",
  "writingMode": "continue",
  "targetWordCount": 1200,
  "decisionSummary": "本轮优先推进核心冲突，并强约束人物口吻与因果触发条件。",
  "selectedModules": [
    {
      "module": "outline",
      "weight": 1.0,
      "required": true,
      "topK": 1,
      "fields": ["stageGoal", "keyConflict", "turningPoints"]
    },
    {
      "module": "plot",
      "weight": 0.92,
      "required": true,
      "topK": 3,
      "fields": ["title", "description", "conflicts", "resolutions"]
    }
  ],
  "requiredFacts": [
    "本章必出人物必须全部出现"
  ],
  "prohibitedMoves": [
    "不要跳过当前大纲中的关键转折"
  ],
  "writerHints": [
    "保持主角克制口吻",
    "先推进冲突，再补情绪描写"
  ]
}
```

### 字段约束

- `stage`、`writingMode`、`selectedModules[*].module` 必填。
- `weight` 范围固定为 `0.0` 到 `1.0`。
- `topK` 必须为正整数。
- `fields` 只能从模块注册表的 `availableFields` 中选取。
- `requiredFacts` / `prohibitedMoves` / `writerHints` 均限制条数，避免重新膨胀成大 prompt。

## 接口设计

### 1. `POST /api/ai-director/decide`

- Method: `POST`
- Path: `/api/ai-director/decide`
- Path Params: 无
- Query Params: 无
- Request Body:
  - `chapterId` `Long` 必填
  - `currentContent` `String` 选填
  - `userInstruction` `String` 选填
  - `entryPoint` `String` 选填
  - `sourceType` `String` 选填，默认 `writing`
  - `forceRefresh` `Boolean` 选填，默认 `false`
- Response Body:
  - `decisionId`
  - `stage`
  - `writingMode`
  - `targetWordCount`
  - `decisionSummary`
  - `selectedModules`
  - `requiredFacts`
  - `prohibitedMoves`
  - `writerHints`
  - `selectedProviderId`
  - `selectedModel`
  - `status`
- Validation:
  - 章节必须存在且用户可访问
  - `entryPoint` 为空时默认 `writing-center`
  - 若决策层关闭，返回当前固定规则推导出的 fallback pack，并标记 `status=fallback`
- Error Cases:
  - 章节不存在
  - 决策层模型不可用
  - 决策输出不符合 schema

### 2. `GET /api/ai-director/chapter/{chapterId}/latest`

- Method: `GET`
- Path: `/api/ai-director/chapter/{chapterId}/latest`
- Path Params:
  - `chapterId`
- Response Body:
  - 最新一次决策记录的 `AIDirectorDecisionVO`
- Validation:
  - 用户必须有章节访问权限
- Error Cases:
  - 章节不存在
  - 无历史决策时返回 `404`

### 3. 现有写作接口的内部接入

- `POST /api/ai-writing/generate`
- `POST /api/ai-writing/generate-stream`

对外请求体保持不变。

内部新增行为：

1. `prepareGeneration(...)` 先调用决策层生成 `decision pack`
2. 写作层只聚合被选中的模块和字段
3. `AIWritingRecord` 写入 `director_decision_id`
4. 决策失败时走旧逻辑

## 前端交互与状态

### 系统设置页

- 增加“决策层默认模型服务”和“决策层默认模型”配置项
- 增加“启用决策层”“决策层工具调用上限”“决策层最大模块数”配置项
- 不新增“决策层提示词大文本编辑器”，避免重新把逻辑堆回 prompt

### 写作中心

- 保持现有生成面板和背景聊天面板布局不变
- v1 可选增加“本轮决策摘要”只读卡片
- 生成开始后，日志面板先显示“决策中”，再进入写作流程

### 章节页初稿助手

- 对外按钮和交互保持不变
- 内部同样走决策层 -> 写作层的流程

## 兼容性要求

- 当 `ai.director.enabled=false` 时，现有写作功能不受影响
- 现有前端请求参数不需要新增必填项
- 决策层异常时，写作链路必须自动退回原 `buildContextBundle(...)` 规则
- `AIWritingChatPanel` 的请求、存储和展示逻辑不做破坏性改动

## Provider 与工具协议设计

### `AIProviderService` 扩展方向

新增非流式工具调用接口：

- `runToolDecision(...)`

返回结果至少包含：

- `finalText`
- `toolCalls[]`
- `rawResponse`

说明：

- v1 决策层先实现非流式工具调用，不要求决策过程流式展示。
- 写作层正文生成仍保留现有流式能力。

### 兼容性策略

- `openai-compatible` 走 `/v1/chat/completions` 的 `tools` / `tool_calls`
- `ollama` 仅在使用兼容 `/v1` 入口时支持工具调用
- 不支持工具调用的 Provider，决策层直接退回 fallback pack，不做伪造工具调用

## 实施步骤

### Step 1

- 目标: 建立决策层最小数据结构和存储能力
- 改动:
  - 新增 `sql/011_ai_director_layer.sql`
  - 新增 `AIDirectorDecision` 实体、Mapper、DTO、VO
  - 修改 `AIWritingRecord` 增加 `directorDecisionId`
- 完成标准:
  - 可以存取 `ai_director_decision`
  - 写作记录可关联决策记录

### Step 2

- 目标: 建立决策层工具协议和模块注册表
- 改动:
  - 扩展 `AIProviderService`
  - 增加工具定义、工具执行器、模块注册表
  - 固化阶段枚举和 schema 校验
- 完成标准:
  - 决策层能执行至少一次真实 tool call 或 fallback
  - 非法 `decision pack` 会被校验拦截

### Step 3

- 目标: 实现决策服务和对外决策接口
- 改动:
  - 新增 `AIDirectorApplicationService`
  - 新增 `/api/ai-director/decide`
  - 新增 `/api/ai-director/chapter/{chapterId}/latest`
- 完成标准:
  - 可返回稳定的 `AIDirectorDecisionVO`
  - 结果入库并可追踪 provider/model

### Step 4

- 目标: 将决策层接入现有 AI 写作链路
- 改动:
  - 修改 `AIWritingServiceImpl.prepareGeneration(...)`
  - 增加根据 `decision pack` 组装上下文的逻辑
  - 加入决策失败回退逻辑
- 完成标准:
  - 现有写作接口在开启决策层后可正常生成正文
  - 关闭决策层时仍走旧逻辑

### Step 5

- 目标: 增加系统设置和只读决策可见性
- 改动:
  - `SettingsView.vue` 增加决策层配置项
  - 可选增加写作中心决策摘要展示
- 完成标准:
  - 管理员可配置决策层模型和开关
  - 前端可看到最近一次决策摘要或至少能读取最新决策接口

## 每一步的完成标准

- Step 1 完成后，数据库和实体层可追踪决策记录
- Step 2 完成后，决策层不再依赖“大段自然语言 prompt 选模块”
- Step 3 完成后，决策结果可被独立调试和复用
- Step 4 完成后，写作中心和章节页共用同一决策能力
- Step 5 完成后，管理员能配置、用户可观察决策结果

## 验证方案

### 后端验证

- 新增单元测试：
  - 阶段判断测试
  - `decision pack` schema 校验测试
  - 模块注册表字段合法性测试
- 新增集成测试：
  - `/api/ai-director/decide` 正常返回
  - 决策层关闭时走 fallback
  - `AIWritingServiceImpl` 接入决策层后仍可成功生成并落库

### 前端验证

- 系统设置页能保存并回显决策层配置
- 写作中心不改 chat 交互也能正常生成
- 可读取最新决策摘要或最新决策结果

### 人工验收

- 同一章节在不同阶段输入下，决策层会切换模块权重和写作模式
- 当背景聊天存在硬约束时，决策层输出中能看到相关约束
- 决策层不可用时，用户仍能完成生成

## 风险与回退

### 风险 1

- 风险: Provider 的工具调用兼容性不稳定
- 处理: v1 仅支持兼容 `/v1/chat/completions` 的 Provider；不支持则 fallback

### 风险 2

- 风险: `decision pack` 重新变成隐形 prompt，字段膨胀
- 处理: 限制字段长度、条数和枚举集合；不允许把长段自然语言塞进 pack

### 风险 3

- 风险: 决策层引入后生成耗时明显增加
- 处理: 决策层独立限制工具调用次数和模块数；必要时缓存最近一次章节决策

### 风险 4

- 风险: 决策层与 chat 层职责重新混淆
- 处理: 明确 v1 中 chat 只提供背景事实提炼，不直接替代决策 API

### 回退方案

- 关闭 `ai.director.enabled`
- 保留新增表但停止读写
- 写作接口退回当前 `buildContextBundle(...) + runWorkflow(...)` 逻辑
