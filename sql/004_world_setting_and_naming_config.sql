USE story_weaver;

SET @schema_name = DATABASE();

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'CREATE TABLE `world_setting` (
      `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
      `project_id` BIGINT NOT NULL,
      `name` VARCHAR(100) NOT NULL,
      `description` TEXT NULL,
      `category` VARCHAR(50) NULL,
      `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
      `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      `deleted` INT DEFAULT 0,
      INDEX `idx_world_setting_project_id` (`project_id`),
      INDEX `idx_world_setting_category` (`category`),
      CONSTRAINT `fk_world_setting_project` FOREIGN KEY (`project_id`) REFERENCES `project`(`id`) ON DELETE CASCADE
    )',
    'SELECT 1'
  )
  FROM information_schema.tables
  WHERE table_schema = @schema_name
    AND table_name = 'world_setting'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `world_setting` ADD COLUMN `name` VARCHAR(100) NULL AFTER `project_id`',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'world_setting'
    AND column_name = 'name'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `world_setting` ADD COLUMN `description` TEXT NULL AFTER `name`',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'world_setting'
    AND column_name = 'description'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `world_setting`
SET
  `name` = COALESCE(NULLIF(`name`, ''), `title`),
  `description` = COALESCE(`description`, `content`)
WHERE `name` IS NULL
   OR `name` = ''
   OR `description` IS NULL;

INSERT INTO `system_config` (`config_key`, `config_value`, `description`)
VALUES
  ('naming_ai_provider_id', '1', '命名模型服务'),
  ('naming_ai_model', 'qwen2.5:3b', '命名模型'),
  ('prompt.naming.chapter', '生成适合长篇小说连载的中文章节标题，要求短、稳、易记，能体现当前章节的核心冲突或气氛。', '章节命名提示词模板'),
  ('prompt.naming.character', '生成适合当前题材的人物名称，要求有辨识度、易读、贴合角色气质，并尽量避免过度生僻。', '人物命名提示词模板')
ON DUPLICATE KEY UPDATE
  `config_value` = VALUES(`config_value`),
  `description` = VALUES(`description`);
