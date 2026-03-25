USE story_weaver;

SET @owner_user_id_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'character'
    AND COLUMN_NAME = 'owner_user_id'
);

SET @add_owner_user_id_sql := IF(
  @owner_user_id_exists = 0,
  'ALTER TABLE `character` ADD COLUMN `owner_user_id` BIGINT NULL AFTER `project_id`',
  'SELECT 1'
);
PREPARE add_owner_user_id_stmt FROM @add_owner_user_id_sql;
EXECUTE add_owner_user_id_stmt;
DEALLOCATE PREPARE add_owner_user_id_stmt;

UPDATE `character` c
JOIN project p ON p.id = c.project_id
SET c.owner_user_id = p.user_id
WHERE c.owner_user_id IS NULL;

SET @owner_user_id_index_exists := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'character'
    AND INDEX_NAME = 'idx_owner_user_id'
);

SET @add_owner_user_id_index_sql := IF(
  @owner_user_id_index_exists = 0,
  'ALTER TABLE `character` ADD INDEX `idx_owner_user_id` (`owner_user_id`)',
  'SELECT 1'
);
PREPARE add_owner_user_id_index_stmt FROM @add_owner_user_id_index_sql;
EXECUTE add_owner_user_id_index_stmt;
DEALLOCATE PREPARE add_owner_user_id_index_stmt;

CREATE TABLE IF NOT EXISTS project_character (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
  project_id BIGINT NOT NULL COMMENT '项目ID',
  character_id BIGINT NOT NULL COMMENT '人物ID',
  project_role VARCHAR(50) DEFAULT '配角' COMMENT '当前项目中的角色定位',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uq_project_character (project_id, character_id),
  INDEX idx_pc_project_id (project_id),
  INDEX idx_pc_character_id (character_id),
  CONSTRAINT fk_pc_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
  CONSTRAINT fk_pc_character FOREIGN KEY (character_id) REFERENCES `character`(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS chapter_character (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
  chapter_id BIGINT NOT NULL COMMENT '章节ID',
  character_id BIGINT NOT NULL COMMENT '人物ID',
  required_flag TINYINT DEFAULT 1 COMMENT '是否为本章必出人物',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uq_chapter_character (chapter_id, character_id),
  INDEX idx_cc_chapter_id (chapter_id),
  INDEX idx_cc_character_id (character_id),
  CONSTRAINT fk_cc_chapter FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE CASCADE,
  CONSTRAINT fk_cc_character FOREIGN KEY (character_id) REFERENCES `character`(id) ON DELETE CASCADE
);

INSERT IGNORE INTO project_character (project_id, character_id, project_role)
SELECT project_id, id, '配角'
FROM `character`
WHERE deleted = 0
  AND project_id IS NOT NULL;
