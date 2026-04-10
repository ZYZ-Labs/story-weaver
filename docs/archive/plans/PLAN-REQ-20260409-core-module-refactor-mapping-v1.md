# Story 核心模块重构字段映射

- Req ID: REQ-20260409-core-module-refactor
- Mapping Version: v1
- Status: Archived
- Created At: 2026-04-09 Asia/Shanghai
- Updated At: 2026-04-10 Asia/Shanghai

## 目标

该文档用于固定 Step 0 的三个关键结果：

- 旧字段到新字段的映射规则
- 双读 / 双写期间的主读写优先级
- SQL 回填阶段需要遵守的默认值策略

## 1. 大纲 `chapter_outline`

| 旧字段 | 新字段 | 策略 |
| --- | --- | --- |
| `chapter_id` | `generated_chapter_id` | 迁移期默认回填；章节级大纲可直接继承 |
| 无 | `outline_type` | `chapter_id` 非空时默认为 `chapter`，否则默认 `global` |
| 无 | `parent_outline_id` | 首阶段默认空，后续由树状编辑补齐 |
| 无 | `root_outline_id` | 首阶段默认为自身 `id`，父子结构形成后再递归修正 |
| `focus_character_ids` | `outline_character_focus` | 旧 CSV 拆到关系表；迁移期继续回写 CSV |
| `related_plot_ids` | `outline_plot` | 旧 CSV 拆到关系表；迁移期继续回写 CSV |
| `related_causality_ids` | `outline_causality` | 旧 CSV 拆到关系表；迁移期继续回写 CSV |
| 无 | `outline_world_setting` | 首阶段只建关系表，不做自动回填 |
| 无 | `related_world_setting_ids_json` | 作为过渡字段，仅用于批量导入和兼容缓存 |

主读写规则：

- 写：关系表优先，旧 CSV 同步回写。
- 读：关系表优先，旧 CSV 作为回退。

## 2. 剧情 `plot`

| 旧字段 | 新字段 | 策略 |
| --- | --- | --- |
| `plot_type` | `story_beat_type` | 用固定映射回填 |
| `plot_type` | `story_function` | 用固定映射回填，不追求 100% 语义精准 |
| `resolutions` | `event_result` | 首阶段直接复制 |
| 无 | `prev_beat_id` | 首阶段默认空；可结合 `sequence` 和人工修正补齐 |
| 无 | `next_beat_id` | 首阶段默认空；可结合 `sequence` 和人工修正补齐 |
| `chapter_id` | `chapter_plot` | 旧章节绑定关系迁移到关系表 |
| `sequence` | `outline_priority` | 首阶段复制数值，后续允许独立调整 |

`plot_type` 映射建议：

| `plot_type` | `story_beat_type` | `story_function` |
| --- | --- | --- |
| `1` | `main` | `advance_mainline` |
| `2` | `side` | `character_growth` |
| `3` | `climax` | `conflict_upgrade` |
| `4` | `foreshadow` | `foreshadow` |
| `5` | `reveal` | `payoff` |
| 其他 / 空 | `main` | `advance_mainline` |

主读写规则：

- 写：新字段 + `chapter_plot` 优先，旧字段保留回写。
- 读：新字段优先，旧字段仅作兼容展示。

## 3. 因果 `causality`

| 旧字段 | 新字段 | 策略 |
| --- | --- | --- |
| `relationship` | `causal_type` | 依据关键词和旧值做粗映射 |
| `conditions` | `trigger_mode` | 有条件文本时默认 `conditional`，否则 `instant` |
| 无 | `payoff_status` | 首阶段默认 `pending` |
| 无 | `upstream_cause_ids_json` | 首阶段不自动推断，默认空 |
| 无 | `downstream_effect_ids_json` | 首阶段不自动推断，默认空 |
| `cause_entity_type` | `cause_entity_type` | 值域统一到新枚举 |
| `effect_entity_type` | `effect_entity_type` | 值域统一到新枚举 |
| `cause_entity_id` | `cause_entity_id` | 去掉历史前缀，保留真实 ID |
| `effect_entity_id` | `effect_entity_id` | 去掉历史前缀，保留真实 ID |

旧实体类型映射建议：

| 旧值 | 新值 |
| --- | --- |
| `plot` | `story_beat` |
| `knowledge` | `state` |
| `writing` | `state` |
| `manual` | `state` |
| `chapter` / `character` | 原值保留 |

主读写规则：

- 写：统一写新枚举和标准化后的实体 ID。
- 读：优先读新值；若仍是历史前缀格式，先做运行时标准化。

## 4. 人物 `character` / `project_character`

| 旧字段 | 新字段 | 策略 |
| --- | --- | --- |
| `project_character.project_role` | `project_character.role_type` | 首阶段复制；兼容期双写 |
| `attributes.身份` | `identity` | 从 JSON 中提取 |
| `attributes.目标` | `core_goal` | 从 JSON 中提取 |
| 无 | `growth_arc` | 默认空 |
| 无 | `first_appearance_chapter_id` | 默认空，后续由章节绑定推断 |
| 无 | `active_stage` | 默认空 |
| 无 | `is_retired` | 默认 `0` |
| `attributes` | `advanced_profile_json` | 迁移期整体复制 |

双写规则：

- 写新字段时，同时回写 `attributes`。
- 编辑 `attributes` 时，服务层同步解析并刷新 `identity / core_goal / advanced_profile_json`。

## 5. 章节 `chapter`

| 旧字段 | 新字段 | 策略 |
| --- | --- | --- |
| `status` | `chapter_status` | 使用状态映射回填 |
| 无 | `summary` | 首阶段默认空 |
| 无 | `outline_id` | 从章节级大纲反查回填 |
| 无 | `prev_chapter_id` | 按 `order_num` 推断 |
| 无 | `next_chapter_id` | 按 `order_num` 推断 |
| 无 | `main_pov_character_id` | 默认空 |
| 无 | `storyBeatIds` | 通过 `chapter_plot` 关系表对外暴露 |

`status` 映射建议：

| 旧值 | 新值 |
| --- | --- |
| `0` | `draft` |
| `1` | `review` |
| `2` | `published` |
| 其他 / 空 | `draft` |

主读写规则：

- 写：`chapter_status`、`outline_id` 和 `chapter_plot` 优先。
- 读：若新字段为空，则回退到旧 `status`、章节级大纲绑定和 `plot.chapter_id`。

## 6. API 兼容清单

### 保持路径不变

- `/api/projects/{projectId}/outlines`
- `/api/projects/{projectId}/plotlines`
- `/api/projects/{projectId}/causalities`
- `/api/projects/{projectId}/characters`
- `/api/projects/{projectId}/chapters`

### 新增但不替换的接口

- `/api/projects/{projectId}/outlines/tree`
- `/api/projects/{projectId}/plotlines/chain`
- `/api/projects/{projectId}/causalities/graph`
- `/api/projects/{projectId}/characters/quick-create`
- `/api/projects/{projectId}/characters/{characterId}/advanced-profile`

### DTO / VO 兼容要求

- 旧请求字段继续接受。
- 新请求字段加入后，服务层统一映射到新结构。
- 响应体优先返回新字段，同时保留旧字段一段时间供旧前端消费。

## 7. 功能开关约定

建议在 `system_config` 中预留以下键：

- `story.refactor.v1.enabled`
- `story.refactor.v1.read_new_relations_first`
- `story.refactor.v1.enable_outline_tree`
- `story.refactor.v1.enable_story_graph`

默认策略：

- 全部默认为 `false`
- 先完成 SQL 和服务层双写，再逐步打开读新结构

## 8. 回填原则

- 只做确定性回填，不做高风险语义推断。
- 无法从旧数据稳定推出的新字段，统一留空，由 UI 和后续服务补齐。
- 所有回填脚本必须可重复执行，使用 `INSERT IGNORE` 或条件更新。
- 任何 destructive cleanup 必须留到 `014_story_core_refactor_cleanup.sql`。
