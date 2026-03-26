USE story_weaver;

CREATE TABLE IF NOT EXISTS chapter_outline (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '大纲ID',
  project_id BIGINT NOT NULL COMMENT '项目ID',
  chapter_id BIGINT NULL COMMENT '关联章节ID',
  title VARCHAR(200) COMMENT '大纲标题',
  summary TEXT COMMENT '大纲摘要',
  content LONGTEXT COMMENT '详细大纲正文',
  stage_goal TEXT COMMENT '本章目标',
  key_conflict TEXT COMMENT '核心冲突',
  turning_points TEXT COMMENT '关键转折',
  expected_ending TEXT COMMENT '预期收束',
  focus_character_ids VARCHAR(500) COMMENT '聚焦人物ID列表',
  related_plot_ids VARCHAR(500) COMMENT '关联剧情ID列表',
  related_causality_ids VARCHAR(500) COMMENT '关联因果ID列表',
  status INT DEFAULT 0 COMMENT '状态: 0-规划中, 1-待写作, 2-已成稿, 3-已归档',
  order_num INT DEFAULT 0 COMMENT '排序号',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted INT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
  INDEX idx_outline_project_id (project_id),
  INDEX idx_outline_chapter_id (chapter_id),
  INDEX idx_outline_order_num (order_num),
  CONSTRAINT fk_outline_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
  CONSTRAINT fk_outline_chapter FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE SET NULL
);
