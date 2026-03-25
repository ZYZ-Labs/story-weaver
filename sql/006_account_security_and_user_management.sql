USE story_weaver;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'role_code'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE user ADD COLUMN role_code VARCHAR(20) NOT NULL DEFAULT ''user'' COMMENT ''role code'' AFTER avatar',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'failed_login_attempts'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE user ADD COLUMN failed_login_attempts INT DEFAULT 0 COMMENT ''failed login attempts'' AFTER status',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'locked_until'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE user ADD COLUMN locked_until DATETIME NULL COMMENT ''account locked until'' AFTER failed_login_attempts',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'last_login_at'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE user ADD COLUMN last_login_at DATETIME NULL COMMENT ''last login time'' AFTER locked_until',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'password_changed_at'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE user ADD COLUMN password_changed_at DATETIME NULL COMMENT ''password changed time'' AFTER last_login_at',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND INDEX_NAME = 'idx_role_code'
);
SET @ddl = IF(
    @index_exists = 0,
    'ALTER TABLE user ADD INDEX idx_role_code (role_code)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND INDEX_NAME = 'idx_locked_until'
);
SET @ddl = IF(
    @index_exists = 0,
    'ALTER TABLE user ADD INDEX idx_locked_until (locked_until)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE user
SET role_code = 'admin'
WHERE username = 'admin';

UPDATE user
SET role_code = 'user'
WHERE username <> 'admin'
  AND (role_code IS NULL OR role_code NOT IN ('admin', 'user'));

UPDATE user
SET failed_login_attempts = 0
WHERE failed_login_attempts IS NULL;

UPDATE user
SET password_changed_at = COALESCE(password_changed_at, update_time, create_time, NOW())
WHERE password_changed_at IS NULL;

INSERT INTO system_config (config_key, config_value, description)
VALUES ('auth.max_failed_attempts', '5', 'login max failed attempts'),
       ('auth.lock_minutes', '30', 'login lock duration minutes')
ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    description = VALUES(description);

INSERT INTO system_config (config_key, config_value, description)
VALUES ('registration_enabled', 'false', 'public registration enabled')
ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    description = VALUES(description);
