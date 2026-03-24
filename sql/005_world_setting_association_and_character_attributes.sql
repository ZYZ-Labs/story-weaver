USE story_weaver;

ALTER TABLE world_setting
    ADD COLUMN IF NOT EXISTS owner_user_id BIGINT NULL AFTER project_id,
    ADD COLUMN IF NOT EXISTS title VARCHAR(100) NULL AFTER description,
    ADD COLUMN IF NOT EXISTS content TEXT NULL AFTER title,
    ADD COLUMN IF NOT EXISTS order_num INT DEFAULT 0 AFTER category;

UPDATE world_setting ws
JOIN project p ON p.id = ws.project_id
SET ws.owner_user_id = p.user_id
WHERE ws.owner_user_id IS NULL;

ALTER TABLE world_setting
    MODIFY COLUMN owner_user_id BIGINT NOT NULL;

ALTER TABLE world_setting
    ADD INDEX IF NOT EXISTS idx_owner_user_id (owner_user_id);

CREATE TABLE IF NOT EXISTS project_world_setting (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    world_setting_id BIGINT NOT NULL COMMENT '世界观模型ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uq_project_world_setting (project_id, world_setting_id),
    INDEX idx_pws_project_id (project_id),
    INDEX idx_pws_world_setting_id (world_setting_id),
    CONSTRAINT fk_pws_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    CONSTRAINT fk_pws_world_setting FOREIGN KEY (world_setting_id) REFERENCES world_setting(id) ON DELETE CASCADE
);

INSERT IGNORE INTO project_world_setting (project_id, world_setting_id)
SELECT project_id, id
FROM world_setting
WHERE deleted = 0;

INSERT INTO system_config (config_key, config_value, description)
VALUES ('prompt.character_attributes', '根据角色描述补齐年龄、身份、阵营、目标、技能、特性、天赋、弱点、装备和关系，内容要可直接用于小说设定表。', '人物属性生成提示词模板')
ON DUPLICATE KEY UPDATE
config_value = VALUES(config_value),
description = VALUES(description);
