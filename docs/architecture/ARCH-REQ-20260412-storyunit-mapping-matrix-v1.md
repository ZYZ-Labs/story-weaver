# StoryUnit 首批对象族映射矩阵

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Arch Doc Version: v1
- Status: Drafted
- Created At: 2026-04-12 Asia/Shanghai
- Updated At: 2026-04-12 Asia/Shanghai

## 说明

本文件用于冻结 `Phase 2.1` 的首批对象族映射矩阵。

目标不是定义数据库新结构，而是明确：

- 每个对象族从哪些表或服务读取
- 每个 facet 的字段如何装配
- 哪些字段当前只保留预留位
- 缺失数据时的 fallback 策略

当前只覆盖：

- `Character`
- `WorldSetting`
- `Chapter`

## Character

### Source Keys

- `character`
- `project_character`
- `chapter_character`

### Matrix

| Facet | Field | Source | Assembler | Fallback | Phase |
| --- | --- | --- | --- | --- | --- |
| `Summary` | `displayTitle` | `character.name` | `CharacterSummaryFacetAssembler` | `EMPTY_VALUE` | `2.1` |
| `Summary` | `oneLineSummary` | `character.identity + character.description` | `CharacterSummaryFacetAssembler` | `DERIVE_FROM_SUMMARY` | `2.1` |
| `Summary` | `longSummary` | `character.description + character.advanced_profile_json` | `CharacterSummaryFacetAssembler` | `EMPTY_VALUE` | `2.1` |
| `Summary` | `stateSummary` | `character.active_stage + character.is_retired` | `CharacterSummaryFacetAssembler` | `EMPTY_VALUE` | `2.1` |
| `Summary` | `relationSummary` | `project_character.role_type + chapter_character.chapter_id` | `CharacterSummaryFacetAssembler` | `EMPTY_COLLECTION` | `2.1` |
| `Canon` | `name` | `character.name` | `CharacterCanonFacetAssembler` | `EMPTY_VALUE` | `2.1` |
| `Canon` | `identity` | `character.identity` | `CharacterCanonFacetAssembler` | `EMPTY_VALUE` | `2.1` |
| `Canon` | `coreGoal` | `character.core_goal` | `CharacterCanonFacetAssembler` | `EMPTY_VALUE` | `2.1` |
| `Canon` | `growthArc` | `character.growth_arc` | `CharacterCanonFacetAssembler` | `EMPTY_VALUE` | `2.1` |
| `Canon` | `attributes` | `character.attributes` | `CharacterCanonFacetAssembler` | `EMPTY_VALUE` | `2.1` |
| `Relation` | `projectRefs` | `project_character.project_id` | `CharacterRelationFacetAssembler` | `EMPTY_COLLECTION` | `2.1` |
| `Relation` | `chapterRefs` | `chapter_character.chapter_id` | `CharacterRelationFacetAssembler` | `EMPTY_COLLECTION` | `2.1` |
| `Relation` | `firstAppearanceChapterRef` | `character.first_appearance_chapter_id` | `CharacterRelationFacetAssembler` | `OMIT_FIELD` | `2.1` |
| `Reveal` | `readerKnown` | `reserved` | `CharacterRevealFacetAssembler` | `RESERVE_ONLY` | `future` |
| `Execution` | `pendingChanges` | `reserved` | `CharacterExecutionFacetAssembler` | `RESERVE_ONLY` | `future` |

### 当前结论

- `Character` 在 `Phase 2.1` 先做 `Summary / Canon / Relation`
- `Reveal / Execution / State` 只保留接口位
- 背包、技能状态、好感度不在这一轮接入

## WorldSetting

### Source Keys

- `world_setting`
- `project_world_setting`
- `outline_world_setting`

### Matrix

| Facet | Field | Source | Assembler | Fallback | Phase |
| --- | --- | --- | --- | --- | --- |
| `Summary` | `displayTitle` | `world_setting.title + world_setting.name` | `WorldSettingSummaryFacetAssembler` | `EMPTY_VALUE` | `2.1` |
| `Summary` | `oneLineSummary` | `world_setting.description` | `WorldSettingSummaryFacetAssembler` | `DERIVE_FROM_SUMMARY` | `2.1` |
| `Summary` | `longSummary` | `world_setting.content + world_setting.description` | `WorldSettingSummaryFacetAssembler` | `EMPTY_VALUE` | `2.1` |
| `Summary` | `relationSummary` | `project_world_setting.project_id + outline_world_setting.outline_id` | `WorldSettingSummaryFacetAssembler` | `EMPTY_COLLECTION` | `2.1` |
| `Canon` | `title` | `world_setting.title` | `WorldSettingCanonFacetAssembler` | `EMPTY_VALUE` | `2.1` |
| `Canon` | `name` | `world_setting.name` | `WorldSettingCanonFacetAssembler` | `EMPTY_VALUE` | `2.1` |
| `Canon` | `category` | `world_setting.category` | `WorldSettingCanonFacetAssembler` | `EMPTY_VALUE` | `2.1` |
| `Canon` | `content` | `world_setting.content` | `WorldSettingCanonFacetAssembler` | `EMPTY_VALUE` | `2.1` |
| `Relation` | `projectRefs` | `project_world_setting.project_id` | `WorldSettingRelationFacetAssembler` | `EMPTY_COLLECTION` | `2.1` |
| `Relation` | `outlineRefs` | `outline_world_setting.outline_id` | `WorldSettingRelationFacetAssembler` | `EMPTY_COLLECTION` | `2.1` |
| `Reveal` | `readerKnown` | `reserved` | `WorldSettingRevealFacetAssembler` | `RESERVE_ONLY` | `future` |
| `Execution` | `pendingChanges` | `reserved` | `WorldSettingExecutionFacetAssembler` | `RESERVE_ONLY` | `future` |

### 当前结论

- `WorldSetting` 先做可读摘要和基础 canon
- 与项目、大纲的关联在这一轮直接进 `RelationFacet`
- `StateFacet` 暂不单独建立

## Chapter

### Source Keys

- `chapter`
- `chapter_character`
- `chapter_plot`
- `outline`
- `plot`
- `ai_writing_record`

### Matrix

| Facet | Field | Source | Assembler | Fallback | Phase |
| --- | --- | --- | --- | --- | --- |
| `Summary` | `displayTitle` | `chapter.title` | `ChapterSummaryFacetAssembler` | `EMPTY_VALUE` | `2.1` |
| `Summary` | `oneLineSummary` | `chapter.summary` | `ChapterSummaryFacetAssembler` | `DERIVE_FROM_SUMMARY` | `2.1` |
| `Summary` | `longSummary` | `chapter.summary + chapter.content` | `ChapterSummaryFacetAssembler` | `EMPTY_VALUE` | `2.1` |
| `Summary` | `stateSummary` | `chapter.chapter_status + chapter.word_count` | `ChapterSummaryFacetAssembler` | `EMPTY_VALUE` | `2.1` |
| `Summary` | `relationSummary` | `chapter_character.character_id + chapter_plot.plot_id + outline.title` | `ChapterSummaryFacetAssembler` | `EMPTY_COLLECTION` | `2.1` |
| `Canon` | `title` | `chapter.title` | `ChapterCanonFacetAssembler` | `EMPTY_VALUE` | `2.1` |
| `Canon` | `orderNum` | `chapter.order_num` | `ChapterCanonFacetAssembler` | `EMPTY_VALUE` | `2.1` |
| `Canon` | `summary` | `chapter.summary` | `ChapterCanonFacetAssembler` | `EMPTY_VALUE` | `2.1` |
| `Relation` | `outlineRef` | `chapter.outline_id + outline.id` | `ChapterRelationFacetAssembler` | `OMIT_FIELD` | `2.1` |
| `Relation` | `mainPovCharacterRef` | `chapter.main_pov_character_id + character.id` | `ChapterRelationFacetAssembler` | `OMIT_FIELD` | `2.1` |
| `Relation` | `characterRefs` | `chapter_character.character_id` | `ChapterRelationFacetAssembler` | `EMPTY_COLLECTION` | `2.1` |
| `Relation` | `plotRefs` | `chapter_plot.plot_id` | `ChapterRelationFacetAssembler` | `EMPTY_COLLECTION` | `2.1` |
| `Execution` | `anchorBundleRef` | `chapter.id + ai_writing_record.generation_trace_json` | `ChapterExecutionFacetAssembler` | `RESERVE_ONLY` | `2.1` |
| `Execution` | `readinessRef` | `chapter.id + ai_writing_record.generation_trace_json` | `ChapterExecutionFacetAssembler` | `RESERVE_ONLY` | `2.1` |
| `Reveal` | `readerKnown` | `reserved` | `ChapterRevealFacetAssembler` | `RESERVE_ONLY` | `future` |

### 当前结论

- `Chapter` 是首批对象里最复杂的一类
- `Phase 2.1` 只冻结 `Summary / Canon / Relation / Execution` 预留位
- 不在这一轮直接把全文生成 trace 变成完整状态模型

## Phase 2.1 输出要求

进入 `Phase 2.2` 之前，代码和文档至少要满足：

- `StoryUnitRegistry` 已存在
- `StoryUnitAssembler / StoryFacetAssembler` 已存在
- 三类对象族的 mapping matrix 已冻结
- `Phase 2.2` 不再需要重新决定“从哪读、怎么装”

## 贡献与署名说明

- 首批对象族只选 `Character / WorldSetting / Chapter`，以及 projection-first 的边界判断：用户与 Codex 共同讨论形成。
- 映射矩阵结构、字段来源整理、assembler 责任切分与文档撰写：Codex 完成。
