# Story 核心模块重构实施计划

- Req ID: REQ-20260409-core-module-refactor
- Plan Version: v1
- Status: In Progress
- Created At: 2026-04-09 Asia/Shanghai
- Updated At: 2026-04-09 Asia/Shanghai

## 实施范围

本计划用于把现有故事核心模块从“可用但偏平铺的 CRUD 结构”，升级为“可级联引用、可被 AI 稳定消费、可支撑长篇项目管理”的结构化模型。

本计划明确以下边界：

- 不重做 AI 写作接口对外路径，优先保持 `/api/ai-writing/*` 稳定。
- 不中断已经落地的 AI 总导层，只做其底层数据源适配。
- 优先扩展和兼容现有模块与接口，必要时新增只读树/图接口，但不强制首阶段整体换名。
- 数据迁移分阶段进行，旧字段和旧接口保留兼容窗口。

## 当前基线确认

结合文档、代码和一次编译验证，当前基线如下：

- AI 总导层已经接入写作主链路、配置页和写作页摘要展示，后端 `mvn -f backend/pom.xml -DskipTests compile` 与前端 `npm run build` 在 2026-04-09 均通过。
- 大纲当前核心问题不是“有没有模块”，而是 `chapter_outline` 仍默认按章节挂接，缺少树层级和世界观引用。
- 剧情模块当前仍是 `plot` 单表，主字段依然偏描述文本，缺少明确的前后链和职责分类。
- 因果模块当前仍以 `cause_entity_type / cause_entity_id / effect_entity_type / effect_entity_id` 字符串自由组合，引用稳定性不足。
- 人物模块已经有项目复用、章节必出人物、背包摘要，但快速建角和高频字段仍未从 `attributes` JSON 中抽离。
- 章节模块已经承接 AI 写作入口，但章节本身仍缺少摘要、POV、关联大纲、关联剧情链等工作台属性。

结论：

- 这不是“补几个字段”的小改动，而是一次需要兼容层、迁移脚本、AI 读取适配一起推进的系统重构。
- 该重构必须以“先加结构，再迁数据，再切读取，最后清理旧字段”的顺序执行。

## 涉及模块

### 需要修改的后端模块

- `backend/src/main/java/com/storyweaver/domain/entity/Outline.java`
- `backend/src/main/java/com/storyweaver/domain/entity/Plot.java`
- `backend/src/main/java/com/storyweaver/domain/entity/Causality.java`
- `backend/src/main/java/com/storyweaver/domain/entity/Character.java`
- `backend/src/main/java/com/storyweaver/domain/entity/Chapter.java`
- `backend/src/main/java/com/storyweaver/domain/dto/OutlineRequestDTO.java`
- `backend/src/main/java/com/storyweaver/domain/dto/CharacterRequestDTO.java`
- `backend/src/main/java/com/storyweaver/domain/dto/ChapterRequestDTO.java`
- `backend/src/main/java/com/storyweaver/controller/OutlineController.java`
- `backend/src/main/java/com/storyweaver/controller/PlotController.java`
- `backend/src/main/java/com/storyweaver/controller/CausalityController.java`
- `backend/src/main/java/com/storyweaver/controller/CharacterController.java`
- `backend/src/main/java/com/storyweaver/controller/ChapterController.java`
- `backend/src/main/java/com/storyweaver/service/impl/OutlineServiceImpl.java`
- `backend/src/main/java/com/storyweaver/service/impl/PlotCrudServiceImpl.java`
- `backend/src/main/java/com/storyweaver/service/impl/CausalityServiceImpl.java`
- `backend/src/main/java/com/storyweaver/service/impl/CharacterServiceImpl.java`
- `backend/src/main/java/com/storyweaver/service/impl/ChapterServiceImpl.java`
- `backend/src/main/java/com/storyweaver/service/impl/AIWritingServiceImpl.java`
- `backend/src/main/java/com/storyweaver/ai/director/application/DirectorModuleRegistry.java`
- `backend/src/main/java/com/storyweaver/ai/director/application/tool/DirectorToolExecutor.java`

### 建议新增的后端支撑模块

- `backend/src/main/java/com/storyweaver/story/support/StoryReferenceValidator.java`
- `backend/src/main/java/com/storyweaver/story/support/StoryStatusMapper.java`
- `backend/src/main/java/com/storyweaver/story/support/StoryGraphAssembler.java`
- `backend/src/main/java/com/storyweaver/domain/entity/OutlineWorldSettingLink.java`
- `backend/src/main/java/com/storyweaver/domain/entity/OutlinePlotLink.java`
- `backend/src/main/java/com/storyweaver/domain/entity/OutlineCausalityLink.java`
- `backend/src/main/java/com/storyweaver/domain/entity/OutlineCharacterLink.java`
- `backend/src/main/java/com/storyweaver/domain/entity/ChapterPlotLink.java`

### 需要修改的前端模块

- `front/src/types/index.ts`
- `front/src/stores/outline.ts`
- `front/src/stores/plot.ts`
- `front/src/stores/causality.ts`
- `front/src/stores/character.ts`
- `front/src/stores/chapter.ts`
- `front/src/views/outline/OutlineView.vue`
- `front/src/views/plot/PlotView.vue`
- `front/src/views/causality/CausalityView.vue`
- `front/src/views/character/CharacterListView.vue`
- `front/src/components/CharacterInventoryDialog.vue`
- `front/src/views/chapter/ChapterListView.vue`
- `front/src/views/writing/WritingView.vue`

### SQL / 迁移文件

- `sql/012_story_core_refactor_base.sql`
- `sql/013_story_core_refactor_backfill.sql`
- `sql/014_story_core_refactor_cleanup.sql`

说明：

- `012` 负责新增字段、关系表和索引，不删除旧字段。
- `013` 负责旧数据回填、状态映射、CSV/JSON 拆分和一致性校验。
- `014` 只在完成灰度切换后执行，用于下线旧读写逻辑和历史兼容列。

## 目标域模型

目标链路固定为：

```text
世界观
-> 大纲树（global / volume / chapter）
-> 剧情节点链（Story Beat）
-> 因果链图（Causality Graph）
-> 章节工作台
-> 正文 / AI 写作记录
```

核心原则：

- 大纲负责“结构骨架”。
- 剧情负责“事件节点”。
- 因果负责“逻辑依赖”。
- 人物负责“角色状态与成长”。
- 章节负责“最终落地写作工作台”。

## 数据模型设计

### 1. `chapter_outline` 升级为树状大纲骨架

新增字段：

- `outline_type` VARCHAR(20) NOT NULL
  取值：`global` / `volume` / `chapter`
- `parent_outline_id` BIGINT NULL
- `generated_chapter_id` BIGINT NULL
- `root_outline_id` BIGINT NULL
- `related_world_setting_ids_json` JSON NULL

新增关系表：

- `outline_world_setting`
  - `outline_id`
  - `world_setting_id`
- `outline_plot`
  - `outline_id`
  - `plot_id`
- `outline_causality`
  - `outline_id`
  - `causality_id`
- `outline_character_focus`
  - `outline_id`
  - `character_id`

保留兼容期字段：

- `chapter_id`
- `focus_character_ids`
- `related_plot_ids`
- `related_causality_ids`

迁移策略：

- 首阶段写入关系表，同时回写旧 CSV 字段。
- AI 和前端读取优先走关系表，缺失时回退到旧字段。

### 2. `plot` 升级为剧情节点链

首阶段不强制改表名，仍使用 `plot`，但语义升级为 Story Beat。

新增字段：

- `story_beat_type` VARCHAR(32) NOT NULL
  取值：`main` / `side` / `foreshadow` / `world_event` / `climax` / `reveal`
- `story_function` VARCHAR(32) NOT NULL
  取值：`advance_mainline` / `character_growth` / `conflict_upgrade` / `foreshadow` / `payoff`
- `event_result` TEXT NULL
- `prev_beat_id` BIGINT NULL
- `next_beat_id` BIGINT NULL
- `outline_priority` INT NULL

兼容字段处理：

- `plot_type` 保留，映射到 `story_beat_type`
- `resolutions` 保留，映射到 `event_result`
- `sequence` 保留作为排序与回退展示字段

补充关系：

- 新增 `chapter_plot` 关系表，用于章节和剧情节点的稳定引用。
- `chapter.storyBeatIds` 在 API 层暴露为数组，但落库优先走关系表，不直接存 CSV。

### 3. `causality` 升级为因果链图

保留 `causality` 表，扩展其图谱能力。

新增字段：

- `causal_type` VARCHAR(32) NOT NULL
  取值：`trigger` / `lead_to` / `block` / `reverse` / `foreshadow` / `payoff` / `escalate`
- `trigger_mode` VARCHAR(32) NULL
  取值：`instant` / `delayed` / `conditional` / `probabilistic` / `stage_based`
- `payoff_status` VARCHAR(32) NULL
  取值：`pending` / `triggered` / `fulfilled` / `expired`
- `upstream_cause_ids_json` JSON NULL
- `downstream_effect_ids_json` JSON NULL

实体引用规范：

- `cause_entity_type` / `effect_entity_type` 统一收敛为：
  - `chapter`
  - `story_beat`
  - `character`
  - `organization`
  - `location`
  - `world_rule`
  - `item`
  - `state`
- `cause_entity_id` / `effect_entity_id` 统一只保存真实 ID，不再拼接前缀。

兼容策略：

- 旧格式 `chapter-1`、`plot:2` 在迁移期解析后转成新格式。
- 删除实体时通过 `StoryReferenceValidator` 阻止悬挂引用。

### 4. `character` 与 `project_character` 升级为两阶段建角

`character` 新增一等字段：

- `identity` VARCHAR(120) NULL
- `core_goal` TEXT NULL
- `growth_arc` TEXT NULL
- `first_appearance_chapter_id` BIGINT NULL
- `active_stage` VARCHAR(32) NULL
- `is_retired` TINYINT DEFAULT 0
- `advanced_profile_json` JSON NULL

`project_character` 新增或重命名字段：

- `project_role` -> `role_type`

字段分层策略：

- 快速建角字段：
  - `roleType`
  - `name`
  - `description`
  - `identity`
  - `coreGoal`
- 高级属性折叠区：
  - `background`
  - `appearance`
  - `skills`
  - `traits`
  - `talents`
  - `weaknesses`
  - `tags`
  - `relationships`
  - `inventory`

兼容策略：

- 现有 `attributes` 继续保留一个阶段。
- 新增字段和 `advanced_profile_json` 与旧 `attributes` 双向映射。

### 5. `chapter` 升级为章节工作台

新增字段：

- `chapter_status` VARCHAR(20) NOT NULL
  取值：`draft` / `polishing` / `review` / `final` / `published` / `archived`
- `summary` TEXT NULL
- `outline_id` BIGINT NULL
- `prev_chapter_id` BIGINT NULL
- `next_chapter_id` BIGINT NULL
- `main_pov_character_id` BIGINT NULL

API 暴露字段：

- `storyBeatIds`
- `readingTimeMinutes`

兼容策略：

- 旧 `status INT` 与新 `chapter_status` 同步映射。
- `word_count` 继续保留并实时更新。
- `readingTimeMinutes` 前端派生，不强制落库。

## 接口设计

## 1. 大纲接口

- 保留：
  - `GET /api/projects/{projectId}/outlines`
  - `POST /api/projects/{projectId}/outlines`
  - `PUT /api/projects/{projectId}/outlines/{outlineId}`
- 新增：
  - `GET /api/projects/{projectId}/outlines/tree`
  - `POST /api/projects/{projectId}/outlines/{outlineId}/generate-chapter`

请求体新增字段：

- `outlineType`
- `parentOutlineId`
- `relatedWorldSettingIds`
- `generatedChapterId`

返回体新增字段：

- `children`
- `outlineType`
- `parentOutlineId`
- `generatedChapterId`
- `relatedWorldSettingIds`

## 2. 剧情节点接口

- 保留资源名：
  - `GET /api/projects/{projectId}/plotlines`
  - `POST /api/projects/{projectId}/plotlines`
  - `PUT /api/plotlines/{id}`
- 新增只读接口：
  - `GET /api/projects/{projectId}/plotlines/chain`

请求体新增字段：

- `storyBeatType`
- `storyFunction`
- `eventResult`
- `prevBeatId`
- `nextBeatId`

请求体兼容字段：

- `plotType`
- `resolutions`

## 3. 因果接口

- 保留：
  - `GET /api/projects/{projectId}/causalities`
  - `POST /api/projects/{projectId}/causalities`
  - `PUT /api/causalities/{id}`
- 新增只读接口：
  - `GET /api/projects/{projectId}/causalities/graph`

请求体新增字段：

- `causalType`
- `triggerMode`
- `payoffStatus`
- `upstreamCauseIds`
- `downstreamEffectIds`

验证要求：

- 目标实体类型必须落在枚举白名单中。
- 实体 ID 必须可解析且可校验。
- 不允许形成明显自环或重复边。

## 4. 人物接口

- 保留：
  - `POST /api/projects/{projectId}/characters`
  - `PUT /api/projects/{projectId}/characters/{characterId}`
- 新增轻量模式：
  - `POST /api/projects/{projectId}/characters/quick-create`
- 新增高级资料更新：
  - `PATCH /api/projects/{projectId}/characters/{characterId}/advanced-profile`

快速建角请求体：

- `roleType`
- `name`
- `description`
- `identity`
- `coreGoal`

高级资料请求体：

- `background`
- `appearance`
- `skills`
- `traits`
- `talents`
- `weaknesses`
- `tags`
- `relationships`
- `growthArc`
- `firstAppearanceChapterId`
- `activeStage`
- `isRetired`

## 5. 章节接口

- 保留：
  - `GET /api/projects/{projectId}/chapters`
  - `POST /api/projects/{projectId}/chapters`
  - `PUT /api/projects/{projectId}/chapters/{chapterId}`

请求体新增字段：

- `chapterStatus`
- `summary`
- `outlineId`
- `storyBeatIds`
- `prevChapterId`
- `nextChapterId`
- `mainPovCharacterId`

返回体新增字段：

- `outlineId`
- `outlineTitle`
- `storyBeatIds`
- `storyBeatTitles`
- `mainPovCharacterName`
- `readingTimeMinutes`

## 前端交互设计

### 1. 大纲页

- 从平铺卡片升级为“树视图 + 详情编辑”双栏。
- 新建大纲时必须先选类型，再决定是否挂到父级。
- 章节级大纲底部提供“生成章节”按钮和关联结果回显。

### 2. 剧情页

- 保持列表视图，但补一条“剧情链”可视区。
- 编辑表单中明确区分：
  - 事件类型
  - 事件功能
  - 上一节点
  - 下一节点
  - 事件结果

### 3. 因果页

- 保持表单与列表模式。
- 首阶段优先补“图谱摘要视图”，不强依赖复杂画布拖拽。
- 每条因果必须展示：
  - 起点实体
  - 因果类型
  - 终点实体
  - 兑现状态

### 4. 人物页

- 新建人物改为两段式：
  - Step 1 快速建角
  - Step 2 折叠高级属性
- 角色详情增加“背包”Tab，现有 `CharacterInventoryDialog` 逐步收口为详情子视图。

### 5. 章节页

- 章节基础信息区域升级为工作台头部。
- 展示：
  - 当前字数
  - 预计阅读时间
  - 章节状态
  - 关联大纲
  - POV 人物
  - 关联剧情节点
- AI 生成入口继续保留，但读取新的章节结构字段。

## 兼容性处理

### 双写策略

- 大纲关联关系：
  - 关系表写入为主
  - CSV 字段同步回写
- 人物资料：
  - 新字段写入为主
  - `attributes` 映射回写
- 章节状态：
  - `chapter_status` 写入为主
  - `status INT` 映射回写
- 剧情结果字段：
  - `event_result` 写入为主
  - `resolutions` 映射回写

### 双读策略

- 读路径优先读新字段 / 新关系表。
- 新字段缺失时回退读取旧字段。
- AI 上下文和 AI 总导层不直接读 CSV，而是统一经过适配器服务。

### 开关策略

建议增加以下系统配置：

- `story.refactor.v1.enabled`
- `story.refactor.v1.read_new_relations_first`
- `story.refactor.v1.enable_outline_tree`
- `story.refactor.v1.enable_story_graph`

## 实施步骤

### Step 0

- 目标:
  - 固定重构边界、枚举和迁移方案，避免边做边改语义。
- 改动:
  - 建立正式 requirement / plan / progress 文档
  - 列出旧字段到新字段的映射表
  - 设计双写/双读适配层
- 完成标准:
  - 所有新增字段、关系表和接口增量已经明确
  - 可以开始写 SQL 和后端适配代码

### Step 1

- 目标:
  - 先建立引用骨架，不先改页面交互
- 改动:
  - 新增大纲关系表、章节与剧情关系表
  - 新增大纲树字段、章节工作台字段、人物一等字段
  - 新增兼容状态映射和引用校验工具
- 完成标准:
  - 数据库已能承载新结构
  - 旧读写逻辑不受影响

### Step 2

- 目标:
  - 完成大纲树和章节生成链路
- 改动:
  - 改造 `OutlineServiceImpl` / `OutlineController`
  - 前端大纲页升级为树结构
  - 补 `generate-chapter` 接口
- 完成标准:
  - 可创建 global / volume / chapter 大纲
  - 可从章节级大纲生成章节并建立关联

### Step 3

- 目标:
  - 完成剧情节点链升级
- 改动:
  - 扩展 `plot` 新字段
  - 改造剧情表单和链路视图
  - 章节可稳定引用剧情节点
- 完成标准:
  - 剧情节点能表达前后关系、事件职责和事件结果

### Step 4

- 目标:
  - 完成因果图升级
- 改动:
  - 扩展 `causality` 新字段
  - 接入上下游链和兑现状态
  - 前端补图谱摘要读法
- 完成标准:
  - 因果链可被稳定检索、验证和展示

### Step 5

- 目标:
  - 完成人物两阶段建角和章节工作台
- 改动:
  - 改造 `CharacterServiceImpl` / `ChapterServiceImpl`
  - 人物页改为快速建角 + 高级资料折叠
  - 章节页补状态、摘要、POV、大纲与剧情引用
- 完成标准:
  - 人物创建成本明显下降
  - 章节页不再只是基础 CRUD，而是写作工作台

### Step 6

- 目标:
  - 切换 AI 上下文聚合和 AI 总导层到新结构
- 改动:
  - 更新 `AIWritingServiceImpl`
  - 更新 `DirectorModuleRegistry`
  - 更新 `DirectorToolExecutor`
- 完成标准:
  - AI 写作与总导读取新结构优先
  - 旧结构仍可回退

### Step 7

- 目标:
  - 完成回填、清理和旧字段退役
- 改动:
  - 执行数据回填脚本
  - 去掉不再需要的 CSV / 旧字段直读
  - 评估是否保留旧接口兼容层
- 完成标准:
  - 新读写路径稳定运行
  - 旧字段只保留必要兼容，不再作为主读写源

## 验证方案

- SQL:
  - 在空库执行 `001 -> 014` 全量迁移通过
  - 在已有样本数据上执行增量迁移通过
- 后端:
  - 补充 Outline / Plot / Causality / Character / Chapter 服务层测试
  - 验证双写与双读映射
  - 执行 `mvn -f backend/pom.xml test`
  - 至少执行 `mvn -f backend/pom.xml -DskipTests compile`
- 前端:
  - 执行 `npm run build`
  - 手工验证大纲树、剧情链、因果摘要、快速建角、章节工作台
- AI:
  - 验证 AI 写作主链路可继续生成
  - 验证 AI 总导层模块选择和上下文摘要不回退为空

## 风险与回退

- 风险:
  - 旧 CSV / JSON 数据质量参差，自动回填可能出现脏数据
  - 因果多态引用没有数据库外键，需要应用层额外兜底
  - AI 上下文读取切换期间容易出现“新旧字段混读”导致内容缺失
  - 前端页面从平铺表单升级到结构化视图后，状态管理复杂度会上升

- 回退方案:
  - 所有新读路径都挂在配置开关后面
  - 保留旧字段和旧接口至少一个完整版本周期
  - AI 主链路保留旧上下文聚合回退能力
  - 清理型 SQL 独立为最后一步，不与结构型迁移一起执行
