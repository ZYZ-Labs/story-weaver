USE story_weaver;

-- Story 核心模块重构 Phase 2: 回填新字段，保留旧字段不删。
-- 约束:
-- 1. 只做确定性迁移
-- 2. 所有语义不稳定字段保持为空，交给后续 UI / 服务层补齐
-- 3. 脚本应可重复执行

UPDATE chapter_outline
SET outline_type = CASE
  WHEN outline_type IS NOT NULL AND outline_type <> '' THEN outline_type
  WHEN chapter_id IS NOT NULL THEN 'chapter'
  ELSE 'global'
END
WHERE deleted = 0;

UPDATE chapter_outline
SET generated_chapter_id = chapter_id
WHERE deleted = 0
  AND generated_chapter_id IS NULL
  AND chapter_id IS NOT NULL
  AND outline_type = 'chapter';

UPDATE chapter_outline
SET root_outline_id = id
WHERE deleted = 0
  AND root_outline_id IS NULL;

INSERT IGNORE INTO outline_character_focus (outline_id, character_id)
SELECT o.id, CAST(TRIM(j.value) AS UNSIGNED)
FROM chapter_outline o
JOIN JSON_TABLE(
  CONCAT('["', REPLACE(REPLACE(IFNULL(o.focus_character_ids, ''), '，', ','), ',', '","'), '"]'),
  '$[*]' COLUMNS(value VARCHAR(64) PATH '$')
) AS j
WHERE o.deleted = 0
  AND IFNULL(o.focus_character_ids, '') <> ''
  AND TRIM(j.value) <> '';

INSERT IGNORE INTO outline_plot (outline_id, plot_id)
SELECT o.id, CAST(TRIM(j.value) AS UNSIGNED)
FROM chapter_outline o
JOIN JSON_TABLE(
  CONCAT('["', REPLACE(REPLACE(IFNULL(o.related_plot_ids, ''), '，', ','), ',', '","'), '"]'),
  '$[*]' COLUMNS(value VARCHAR(64) PATH '$')
) AS j
WHERE o.deleted = 0
  AND IFNULL(o.related_plot_ids, '') <> ''
  AND TRIM(j.value) <> '';

INSERT IGNORE INTO outline_causality (outline_id, causality_id)
SELECT o.id, CAST(TRIM(j.value) AS UNSIGNED)
FROM chapter_outline o
JOIN JSON_TABLE(
  CONCAT('["', REPLACE(REPLACE(IFNULL(o.related_causality_ids, ''), '，', ','), ',', '","'), '"]'),
  '$[*]' COLUMNS(value VARCHAR(64) PATH '$')
) AS j
WHERE o.deleted = 0
  AND IFNULL(o.related_causality_ids, '') <> ''
  AND TRIM(j.value) <> '';

UPDATE plot
SET story_beat_type = CASE
  WHEN plot_type = 1 THEN 'main'
  WHEN plot_type = 2 THEN 'side'
  WHEN plot_type = 3 THEN 'climax'
  WHEN plot_type = 4 THEN 'foreshadow'
  WHEN plot_type = 5 THEN 'reveal'
  ELSE COALESCE(NULLIF(story_beat_type, ''), 'main')
END,
story_function = CASE
  WHEN plot_type = 1 THEN 'advance_mainline'
  WHEN plot_type = 2 THEN 'character_growth'
  WHEN plot_type = 3 THEN 'conflict_upgrade'
  WHEN plot_type = 4 THEN 'foreshadow'
  WHEN plot_type = 5 THEN 'payoff'
  ELSE COALESCE(NULLIF(story_function, ''), 'advance_mainline')
END,
event_result = CASE
  WHEN event_result IS NULL OR event_result = '' THEN resolutions
  ELSE event_result
END,
outline_priority = CASE
  WHEN outline_priority IS NULL THEN sequence
  ELSE outline_priority
END
WHERE deleted = 0;

INSERT IGNORE INTO chapter_plot (chapter_id, plot_id, relation_type, sort_order)
SELECT chapter_id, id, 'primary', COALESCE(sequence, 0)
FROM plot
WHERE deleted = 0
  AND chapter_id IS NOT NULL;

UPDATE causality
SET causal_type = CASE
  WHEN causal_type IS NOT NULL AND causal_type <> '' THEN causal_type
  WHEN relationship IN ('causes', 'trigger') THEN 'trigger'
  WHEN relationship IN ('escalates', 'lead_to') THEN 'lead_to'
  WHEN relationship IN ('blocks', 'block') THEN 'block'
  WHEN relationship IN ('reveals', 'foreshadow') THEN 'foreshadow'
  WHEN relationship IN ('resolves', 'payoff') THEN 'payoff'
  WHEN relationship IN ('motivates', 'reverse') THEN 'reverse'
  ELSE 'trigger'
END,
trigger_mode = CASE
  WHEN trigger_mode IS NOT NULL AND trigger_mode <> '' THEN trigger_mode
  WHEN conditions IS NOT NULL AND conditions <> '' THEN 'conditional'
  ELSE 'instant'
END,
payoff_status = CASE
  WHEN payoff_status IS NOT NULL AND payoff_status <> '' THEN payoff_status
  ELSE 'pending'
END,
cause_entity_type = CASE
  WHEN cause_entity_type = 'plot' THEN 'story_beat'
  WHEN cause_entity_type IN ('knowledge', 'writing', 'manual') THEN 'state'
  ELSE cause_entity_type
END,
effect_entity_type = CASE
  WHEN effect_entity_type = 'plot' THEN 'story_beat'
  WHEN effect_entity_type IN ('knowledge', 'writing', 'manual') THEN 'state'
  ELSE effect_entity_type
END
WHERE deleted = 0;

-- 历史 `chapter-1` / `plot:2` 风格的实体 ID 在不同环境里质量不一。
-- 这里先只做安全的前缀剥离；如果值本身不是数字，保留原值供服务层二次校验。
UPDATE causality
SET cause_entity_id = CASE
  WHEN cause_entity_id LIKE 'chapter-%' THEN SUBSTRING_INDEX(cause_entity_id, '-', -1)
  WHEN cause_entity_id LIKE 'plot-%' THEN SUBSTRING_INDEX(cause_entity_id, '-', -1)
  WHEN cause_entity_id LIKE 'plot:%' THEN SUBSTRING_INDEX(cause_entity_id, ':', -1)
  WHEN cause_entity_id LIKE 'character-%' THEN SUBSTRING_INDEX(cause_entity_id, '-', -1)
  ELSE cause_entity_id
END,
effect_entity_id = CASE
  WHEN effect_entity_id LIKE 'chapter-%' THEN SUBSTRING_INDEX(effect_entity_id, '-', -1)
  WHEN effect_entity_id LIKE 'plot-%' THEN SUBSTRING_INDEX(effect_entity_id, '-', -1)
  WHEN effect_entity_id LIKE 'plot:%' THEN SUBSTRING_INDEX(effect_entity_id, ':', -1)
  WHEN effect_entity_id LIKE 'character-%' THEN SUBSTRING_INDEX(effect_entity_id, '-', -1)
  ELSE effect_entity_id
END
WHERE deleted = 0;

UPDATE `character`
SET identity = CASE
  WHEN identity IS NOT NULL AND identity <> '' THEN identity
  WHEN JSON_EXTRACT(attributes, '$.\"身份\"') IS NOT NULL THEN JSON_UNQUOTE(JSON_EXTRACT(attributes, '$.\"身份\"'))
  ELSE identity
END,
core_goal = CASE
  WHEN core_goal IS NOT NULL AND core_goal <> '' THEN core_goal
  WHEN JSON_EXTRACT(attributes, '$.\"目标\"') IS NOT NULL THEN JSON_UNQUOTE(JSON_EXTRACT(attributes, '$.\"目标\"'))
  ELSE core_goal
END,
advanced_profile_json = CASE
  WHEN advanced_profile_json IS NOT NULL THEN advanced_profile_json
  WHEN attributes IS NOT NULL THEN attributes
  ELSE advanced_profile_json
END,
is_retired = COALESCE(is_retired, 0)
WHERE deleted = 0;

UPDATE project_character
SET role_type = CASE
  WHEN role_type IS NOT NULL AND role_type <> '' THEN role_type
  WHEN project_role IS NOT NULL AND project_role <> '' THEN project_role
  ELSE '配角'
END;

UPDATE chapter
SET chapter_status = CASE
  WHEN chapter_status IS NOT NULL AND chapter_status <> '' THEN chapter_status
  WHEN status = 0 THEN 'draft'
  WHEN status = 1 THEN 'review'
  WHEN status = 2 THEN 'published'
  ELSE 'draft'
END
WHERE deleted = 0;

UPDATE chapter c
JOIN chapter_outline o ON o.chapter_id = c.id AND o.deleted = 0
SET c.outline_id = o.id
WHERE c.deleted = 0
  AND c.outline_id IS NULL;

UPDATE chapter c
JOIN (
  SELECT id,
         LAG(id) OVER (PARTITION BY project_id ORDER BY order_num, id) AS prev_id,
         LEAD(id) OVER (PARTITION BY project_id ORDER BY order_num, id) AS next_id
  FROM chapter
  WHERE deleted = 0
) seq ON seq.id = c.id
SET c.prev_chapter_id = COALESCE(c.prev_chapter_id, seq.prev_id),
    c.next_chapter_id = COALESCE(c.next_chapter_id, seq.next_id)
WHERE c.deleted = 0;

-- 以下字段保持空值，后续由 UI 或服务层补齐:
-- chapter.summary
-- chapter.main_pov_character_id
-- character.growth_arc
-- character.first_appearance_chapter_id
-- chapter_outline.parent_outline_id
-- causality.upstream_cause_ids_json / downstream_effect_ids_json
