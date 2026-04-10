USE story_weaver;

-- Story 核心模块重构 Phase 1: 先补结构，不删旧字段。
-- 该脚本的目标是给后端和前端改造提供稳定的数据承载层。

INSERT INTO system_config (config_key, config_value, description)
VALUES ('story.refactor.v1.enabled', 'false', '是否启用 Story 核心模块重构能力')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO system_config (config_key, config_value, description)
VALUES ('story.refactor.v1.read_new_relations_first', 'false', '是否优先读取新关系表')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO system_config (config_key, config_value, description)
VALUES ('story.refactor.v1.enable_outline_tree', 'false', '是否启用大纲树能力')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO system_config (config_key, config_value, description)
VALUES ('story.refactor.v1.enable_story_graph', 'false', '是否启用剧情链与因果图新结构')
ON DUPLICATE KEY UPDATE description = VALUES(description);

ALTER TABLE chapter_outline
  ADD COLUMN IF NOT EXISTS outline_type VARCHAR(20) NOT NULL DEFAULT 'chapter' COMMENT '大纲类型: global/volume/chapter' AFTER project_id,
  ADD COLUMN IF NOT EXISTS parent_outline_id BIGINT NULL COMMENT '父级大纲ID' AFTER outline_type,
  ADD COLUMN IF NOT EXISTS root_outline_id BIGINT NULL COMMENT '根大纲ID' AFTER parent_outline_id,
  ADD COLUMN IF NOT EXISTS generated_chapter_id BIGINT NULL COMMENT '由本大纲生成的章节ID' AFTER chapter_id,
  ADD COLUMN IF NOT EXISTS related_world_setting_ids_json JSON NULL COMMENT '关联世界观ID列表JSON' AFTER related_causality_ids;

ALTER TABLE plot
  ADD COLUMN IF NOT EXISTS story_beat_type VARCHAR(32) NOT NULL DEFAULT 'main' COMMENT '剧情节点类型' AFTER plot_type,
  ADD COLUMN IF NOT EXISTS story_function VARCHAR(32) NOT NULL DEFAULT 'advance_mainline' COMMENT '剧情节点功能' AFTER story_beat_type,
  ADD COLUMN IF NOT EXISTS event_result TEXT NULL COMMENT '事件结果' AFTER resolutions,
  ADD COLUMN IF NOT EXISTS prev_beat_id BIGINT NULL COMMENT '上一剧情节点ID' AFTER event_result,
  ADD COLUMN IF NOT EXISTS next_beat_id BIGINT NULL COMMENT '下一剧情节点ID' AFTER prev_beat_id,
  ADD COLUMN IF NOT EXISTS outline_priority INT NULL COMMENT '大纲优先级' AFTER next_beat_id;

ALTER TABLE causality
  ADD COLUMN IF NOT EXISTS causal_type VARCHAR(32) NOT NULL DEFAULT 'trigger' COMMENT '因果关系类型' AFTER relationship,
  ADD COLUMN IF NOT EXISTS trigger_mode VARCHAR(32) NULL COMMENT '触发模式' AFTER causal_type,
  ADD COLUMN IF NOT EXISTS payoff_status VARCHAR(32) NULL COMMENT '兑现状态' AFTER trigger_mode,
  ADD COLUMN IF NOT EXISTS upstream_cause_ids_json JSON NULL COMMENT '上游因果ID列表JSON' AFTER payoff_status,
  ADD COLUMN IF NOT EXISTS downstream_effect_ids_json JSON NULL COMMENT '下游因果ID列表JSON' AFTER upstream_cause_ids_json;

ALTER TABLE `character`
  ADD COLUMN IF NOT EXISTS identity VARCHAR(120) NULL COMMENT '角色身份' AFTER description,
  ADD COLUMN IF NOT EXISTS core_goal TEXT NULL COMMENT '核心目标' AFTER identity,
  ADD COLUMN IF NOT EXISTS growth_arc TEXT NULL COMMENT '成长弧线' AFTER core_goal,
  ADD COLUMN IF NOT EXISTS first_appearance_chapter_id BIGINT NULL COMMENT '首次出场章节ID' AFTER growth_arc,
  ADD COLUMN IF NOT EXISTS active_stage VARCHAR(32) NULL COMMENT '当前活跃阶段' AFTER first_appearance_chapter_id,
  ADD COLUMN IF NOT EXISTS is_retired TINYINT NOT NULL DEFAULT 0 COMMENT '是否退场' AFTER active_stage,
  ADD COLUMN IF NOT EXISTS advanced_profile_json JSON NULL COMMENT '高级角色资料JSON' AFTER attributes;

ALTER TABLE project_character
  ADD COLUMN IF NOT EXISTS role_type VARCHAR(50) NULL COMMENT '当前项目中的角色类型' AFTER project_role;

ALTER TABLE chapter
  ADD COLUMN IF NOT EXISTS chapter_status VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '章节状态' AFTER status,
  ADD COLUMN IF NOT EXISTS summary TEXT NULL COMMENT '章节摘要' AFTER title,
  ADD COLUMN IF NOT EXISTS outline_id BIGINT NULL COMMENT '关联大纲ID' AFTER summary,
  ADD COLUMN IF NOT EXISTS prev_chapter_id BIGINT NULL COMMENT '上一章节ID' AFTER outline_id,
  ADD COLUMN IF NOT EXISTS next_chapter_id BIGINT NULL COMMENT '下一章节ID' AFTER prev_chapter_id,
  ADD COLUMN IF NOT EXISTS main_pov_character_id BIGINT NULL COMMENT '主POV人物ID' AFTER next_chapter_id;

CREATE TABLE IF NOT EXISTS outline_world_setting (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
  outline_id BIGINT NOT NULL COMMENT '大纲ID',
  world_setting_id BIGINT NOT NULL COMMENT '世界观ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uq_outline_world_setting (outline_id, world_setting_id),
  INDEX idx_ows_outline_id (outline_id),
  INDEX idx_ows_world_setting_id (world_setting_id),
  CONSTRAINT fk_ows_outline FOREIGN KEY (outline_id) REFERENCES chapter_outline(id) ON DELETE CASCADE,
  CONSTRAINT fk_ows_world_setting FOREIGN KEY (world_setting_id) REFERENCES world_setting(id) ON DELETE CASCADE
) COMMENT='大纲与世界观关联表';

CREATE TABLE IF NOT EXISTS outline_plot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
  outline_id BIGINT NOT NULL COMMENT '大纲ID',
  plot_id BIGINT NOT NULL COMMENT '剧情节点ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uq_outline_plot (outline_id, plot_id),
  INDEX idx_op_outline_id (outline_id),
  INDEX idx_op_plot_id (plot_id),
  CONSTRAINT fk_op_outline FOREIGN KEY (outline_id) REFERENCES chapter_outline(id) ON DELETE CASCADE,
  CONSTRAINT fk_op_plot FOREIGN KEY (plot_id) REFERENCES plot(id) ON DELETE CASCADE
) COMMENT='大纲与剧情节点关联表';

CREATE TABLE IF NOT EXISTS outline_causality (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
  outline_id BIGINT NOT NULL COMMENT '大纲ID',
  causality_id BIGINT NOT NULL COMMENT '因果ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uq_outline_causality (outline_id, causality_id),
  INDEX idx_oc_outline_id (outline_id),
  INDEX idx_oc_causality_id (causality_id),
  CONSTRAINT fk_oc_outline FOREIGN KEY (outline_id) REFERENCES chapter_outline(id) ON DELETE CASCADE,
  CONSTRAINT fk_oc_causality FOREIGN KEY (causality_id) REFERENCES causality(id) ON DELETE CASCADE
) COMMENT='大纲与因果关联表';

CREATE TABLE IF NOT EXISTS outline_character_focus (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
  outline_id BIGINT NOT NULL COMMENT '大纲ID',
  character_id BIGINT NOT NULL COMMENT '人物ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uq_outline_character_focus (outline_id, character_id),
  INDEX idx_ocf_outline_id (outline_id),
  INDEX idx_ocf_character_id (character_id),
  CONSTRAINT fk_ocf_outline FOREIGN KEY (outline_id) REFERENCES chapter_outline(id) ON DELETE CASCADE,
  CONSTRAINT fk_ocf_character FOREIGN KEY (character_id) REFERENCES `character`(id) ON DELETE CASCADE
) COMMENT='大纲聚焦人物关联表';

CREATE TABLE IF NOT EXISTS chapter_plot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
  chapter_id BIGINT NOT NULL COMMENT '章节ID',
  plot_id BIGINT NOT NULL COMMENT '剧情节点ID',
  relation_type VARCHAR(32) NOT NULL DEFAULT 'primary' COMMENT '关联类型',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uq_chapter_plot (chapter_id, plot_id),
  INDEX idx_cp_chapter_id (chapter_id),
  INDEX idx_cp_plot_id (plot_id),
  CONSTRAINT fk_cp_chapter FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE CASCADE,
  CONSTRAINT fk_cp_plot FOREIGN KEY (plot_id) REFERENCES plot(id) ON DELETE CASCADE
) COMMENT='章节与剧情节点关联表';

-- 索引和外键补强可在正式执行前根据数据库版本按需追加。
