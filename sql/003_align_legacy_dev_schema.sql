-- 用于把旧版开发库对齐到当前代码期望的字段结构
-- 适用场景：
-- 1. character 表仍使用 profile / avatar / status 旧结构，缺少 attributes
-- 2. world_setting 表仍使用 title / content 旧结构，缺少 name / description

USE story_weaver;

SET @schema_name = DATABASE();

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `character` ADD COLUMN `attributes` JSON NULL AFTER `description`',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'character'
    AND column_name = 'attributes'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `character`
SET attributes = CASE
  WHEN profile IS NULL OR TRIM(profile) = '' THEN JSON_OBJECT()
  WHEN JSON_VALID(profile) THEN CAST(profile AS JSON)
  ELSE JSON_OBJECT('资料', profile)
END
WHERE attributes IS NULL;

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
  name = COALESCE(NULLIF(name, ''), title),
  description = COALESCE(description, content)
WHERE name IS NULL
   OR name = ''
   OR description IS NULL;
