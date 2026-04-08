USE story_weaver;

CREATE TABLE IF NOT EXISTS ai_director_decision (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'AI 总导决策ID',
  project_id BIGINT NOT NULL COMMENT '项目ID',
  chapter_id BIGINT NOT NULL COMMENT '章节ID',
  user_id BIGINT NOT NULL COMMENT '所属用户ID',
  source_type VARCHAR(32) NOT NULL DEFAULT 'writing' COMMENT '决策来源类型',
  entry_point VARCHAR(64) NOT NULL DEFAULT 'writing-center' COMMENT '入口点',
  stage VARCHAR(32) NOT NULL COMMENT '章节阶段',
  writing_mode VARCHAR(32) NOT NULL COMMENT '本轮写作模式',
  target_word_count INT NULL COMMENT '目标字数',
  selected_modules_json LONGTEXT NOT NULL COMMENT '选中模块JSON',
  module_weights_json LONGTEXT NOT NULL COMMENT '模块权重JSON',
  required_facts_json LONGTEXT NOT NULL COMMENT '硬约束事实JSON',
  prohibited_moves_json LONGTEXT NOT NULL COMMENT '禁止事项JSON',
  decision_pack_json LONGTEXT NOT NULL COMMENT '完整决策包JSON',
  tool_trace_json LONGTEXT NOT NULL COMMENT '工具调用轨迹JSON',
  selected_provider_id BIGINT NOT NULL COMMENT '所选Provider ID',
  selected_model VARCHAR(128) NOT NULL COMMENT '所选模型',
  status VARCHAR(32) NOT NULL DEFAULT 'generated' COMMENT '状态',
  error_message VARCHAR(500) NULL COMMENT '错误信息',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted INT DEFAULT 0 COMMENT '删除标记',
  INDEX idx_ai_director_decision_chapter_id (chapter_id),
  INDEX idx_ai_director_decision_project_id (project_id),
  INDEX idx_ai_director_decision_user_id (user_id),
  INDEX idx_ai_director_decision_status (status),
  CONSTRAINT fk_ai_director_decision_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
  CONSTRAINT fk_ai_director_decision_chapter FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE CASCADE,
  CONSTRAINT fk_ai_director_decision_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) COMMENT='AI 总导决策记录表';

ALTER TABLE ai_writing_record
  ADD COLUMN IF NOT EXISTS director_decision_id BIGINT NULL COMMENT '关联的AI总导决策ID' AFTER prompt_snapshot;

CREATE INDEX idx_ai_writing_record_director_decision_id
  ON ai_writing_record (director_decision_id);

INSERT INTO system_config (config_key, config_value, description)
VALUES ('director_ai_provider_id', '1', '总导决策层默认模型服务')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO system_config (config_key, config_value, description)
VALUES ('director_ai_model', 'qwen2.5:7b', '总导决策层默认模型')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO system_config (config_key, config_value, description)
VALUES ('ai.director.enabled', 'true', '是否启用AI总导决策层')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO system_config (config_key, config_value, description)
VALUES ('ai.director.max_tool_calls', '4', '总导决策层最大工具调用次数')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO system_config (config_key, config_value, description)
VALUES ('ai.director.max_selected_modules', '6', '总导决策层最大选中模块数')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO system_config (config_key, config_value, description)
VALUES ('ai.director.debug_expose_decision', 'false', '是否暴露总导决策调试信息')
ON DUPLICATE KEY UPDATE description = VALUES(description);
