USE story_weaver;

CREATE TABLE IF NOT EXISTS item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '物品ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    owner_user_id BIGINT NOT NULL COMMENT '所属用户ID',
    name VARCHAR(120) NOT NULL COMMENT '物品名称',
    description TEXT NULL COMMENT '物品描述',
    category VARCHAR(32) NOT NULL DEFAULT 'prop' COMMENT '物品分类',
    rarity VARCHAR(32) NOT NULL DEFAULT 'common' COMMENT '稀有度',
    stackable INT NOT NULL DEFAULT 0 COMMENT '是否可堆叠',
    max_stack INT NOT NULL DEFAULT 1 COMMENT '最大堆叠数',
    usable INT NOT NULL DEFAULT 0 COMMENT '是否可使用',
    equippable INT NOT NULL DEFAULT 0 COMMENT '是否可装备',
    slot_type VARCHAR(32) NOT NULL DEFAULT 'misc' COMMENT '装备部位',
    item_value INT NOT NULL DEFAULT 0 COMMENT '物品价值',
    weight INT NOT NULL DEFAULT 0 COMMENT '重量',
    attributes_json LONGTEXT NULL COMMENT '结构化属性 JSON',
    effect_json LONGTEXT NULL COMMENT '效果 JSON',
    tags VARCHAR(500) NULL COMMENT '标签',
    source_type VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT '来源类型',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '删除标记',
    INDEX idx_item_project_deleted (project_id, deleted),
    INDEX idx_item_owner_deleted (owner_user_id, deleted),
    INDEX idx_item_category_rarity (category, rarity),
    CONSTRAINT fk_item_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_owner FOREIGN KEY (owner_user_id) REFERENCES user(id) ON DELETE CASCADE
) COMMENT='项目物品定义表';

CREATE TABLE IF NOT EXISTS character_inventory_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '背包条目ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    character_id BIGINT NOT NULL COMMENT '角色ID',
    item_id BIGINT NOT NULL COMMENT '物品ID',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    equipped INT NOT NULL DEFAULT 0 COMMENT '是否装备中',
    durability INT NOT NULL DEFAULT 100 COMMENT '耐久度',
    custom_name VARCHAR(120) NULL COMMENT '自定义名称',
    notes TEXT NULL COMMENT '备注',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '删除标记',
    INDEX idx_inventory_character (character_id),
    INDEX idx_inventory_project_character (project_id, character_id),
    INDEX idx_inventory_item (item_id),
    CONSTRAINT fk_inventory_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_character FOREIGN KEY (character_id) REFERENCES `character`(id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_item FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE CASCADE
) COMMENT='角色背包条目表';

INSERT INTO system_config (config_key, config_value, description)
VALUES
    ('item_ai_provider_id', '', '物品生成默认 Provider ID'),
    ('item_ai_model', '', '物品生成默认模型名称'),
    ('prompt.item_generation', '优先生成适合长篇创作的道具、药品、装备、材料与任务物品，名称、说明、效果和标签必须便于剧情使用。', '物品生成提示词模板')
ON DUPLICATE KEY UPDATE
    description = VALUES(description);
