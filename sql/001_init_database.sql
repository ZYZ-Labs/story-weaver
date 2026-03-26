-- Story Weaver 数据库初始化脚本（唯一权威 Schema）
-- 版本: 1.1.0
-- 创建日期: 2026-03-16

CREATE DATABASE IF NOT EXISTS story_weaver
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE story_weaver;

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    email VARCHAR(100) COMMENT '邮箱',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(500) COMMENT '头像',
    role_code VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色: admin/user',
    status INT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    failed_login_attempts INT DEFAULT 0 COMMENT '登录失败次数',
    locked_until DATETIME NULL COMMENT '锁定截止时间',
    last_login_at DATETIME NULL COMMENT '最近登录时间',
    password_changed_at DATETIME NULL COMMENT '最近修改密码时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_username (username),
    INDEX idx_role_code (role_code),
    INDEX idx_status (status),
    INDEX idx_locked_until (locked_until)
) COMMENT='用户表';

-- 项目表
CREATE TABLE IF NOT EXISTS project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '项目ID',
    name VARCHAR(100) NOT NULL COMMENT '项目名称',
    description TEXT COMMENT '项目描述',
    cover_image VARCHAR(500) COMMENT '封面图片',
    user_id BIGINT NOT NULL COMMENT '创建用户ID',
    status INT DEFAULT 0 COMMENT '状态: 0-草稿, 1-进行中, 2-已完成, 3-归档',
    genre VARCHAR(50) COMMENT '类型/流派',
    tags VARCHAR(500) COMMENT '标签',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) COMMENT='项目表';

-- 章节表
CREATE TABLE IF NOT EXISTS chapter (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '章节ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    title VARCHAR(200) NOT NULL COMMENT '章节标题',
    content LONGTEXT COMMENT '章节内容',
    order_num INT DEFAULT 0 COMMENT '排序号',
    status INT DEFAULT 0 COMMENT '状态: 0-草稿, 1-待确认, 2-已发布',
    word_count INT DEFAULT 0 COMMENT '字数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_order_num (order_num),
    INDEX idx_status (status),
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
) COMMENT='章节表';

-- 章节大纲表
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
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE SET NULL
) COMMENT='章节大纲表';

-- 人物表（与 Character 实体对齐）
CREATE TABLE IF NOT EXISTS character (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '人物ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    name VARCHAR(100) NOT NULL COMMENT '人物名称',
    description TEXT COMMENT '人物描述',
    attributes JSON COMMENT '人物属性(JSON)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_name (name),
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
) COMMENT='人物表';

-- 世界设定表（与 WorldSetting 实体对齐）
CREATE TABLE IF NOT EXISTS world_setting (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '设定ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    owner_user_id BIGINT NOT NULL COMMENT '所属用户ID',
    name VARCHAR(100) NOT NULL COMMENT '设定名称',
    description TEXT COMMENT '设定描述',
    category VARCHAR(50) COMMENT '分类',
    title VARCHAR(100) COMMENT '兼容旧标题',
    content TEXT COMMENT '兼容旧内容',
    order_num INT DEFAULT 0 COMMENT '排序号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_owner_user_id (owner_user_id),
    INDEX idx_category (category),
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    FOREIGN KEY (owner_user_id) REFERENCES user(id) ON DELETE CASCADE
) COMMENT='世界设定表';

CREATE TABLE IF NOT EXISTS project_world_setting (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    world_setting_id BIGINT NOT NULL COMMENT '世界观模型ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uq_project_world_setting (project_id, world_setting_id),
    INDEX idx_pws_project_id (project_id),
    INDEX idx_pws_world_setting_id (world_setting_id),
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    FOREIGN KEY (world_setting_id) REFERENCES world_setting(id) ON DELETE CASCADE
) COMMENT='项目与世界观关联表';

-- AI写作记录表（与 AIWritingRecord 实体对齐）
CREATE TABLE IF NOT EXISTS ai_writing_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    chapter_id BIGINT NOT NULL COMMENT '章节ID',
    original_content LONGTEXT COMMENT '原始内容',
    generated_content LONGTEXT COMMENT 'AI生成内容',
    writing_type VARCHAR(50) COMMENT '写作类型',
    user_instruction TEXT COMMENT '用户指令',
    selected_provider_id BIGINT COMMENT '所选 Provider ID',
    selected_model VARCHAR(100) COMMENT '所选模型',
    prompt_snapshot LONGTEXT COMMENT 'Prompt 快照',
    status VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态: draft/accepted/rejected',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_chapter_id (chapter_id),
    INDEX idx_selected_provider_id (selected_provider_id),
    INDEX idx_status (status),
    FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE CASCADE
) COMMENT='AI写作记录表';

-- 情节表（与 Plot 实体对齐）
CREATE TABLE IF NOT EXISTS plot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '情节ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    chapter_id BIGINT COMMENT '章节ID',
    title VARCHAR(200) COMMENT '标题',
    description TEXT COMMENT '描述',
    content LONGTEXT COMMENT '内容',
    plot_type INT COMMENT '情节类型',
    sequence INT COMMENT '排序序号',
    characters TEXT COMMENT '涉及角色',
    locations TEXT COMMENT '涉及地点',
    timeline VARCHAR(255) COMMENT '时间线',
    conflicts TEXT COMMENT '冲突',
    resolutions TEXT COMMENT '解决方案',
    tags VARCHAR(500) COMMENT '标签',
    status INT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人',
    update_by BIGINT COMMENT '更新人',
    deleted INT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_chapter_id (chapter_id),
    INDEX idx_plot_type (plot_type),
    INDEX idx_sequence (sequence),
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE SET NULL
) COMMENT='情节表';

-- 因果关系表（与 Causality 实体对齐）
CREATE TABLE IF NOT EXISTS causality (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '因果关系ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    name VARCHAR(200) COMMENT '关系名称',
    description TEXT COMMENT '关系描述',
    cause_type VARCHAR(100) COMMENT '原因类型',
    effect_type VARCHAR(100) COMMENT '结果类型',
    cause_entity_id VARCHAR(100) COMMENT '原因实体ID',
    effect_entity_id VARCHAR(100) COMMENT '结果实体ID',
    cause_entity_type VARCHAR(100) COMMENT '原因实体类型',
    effect_entity_type VARCHAR(100) COMMENT '结果实体类型',
    relationship VARCHAR(255) COMMENT '关系描述',
    strength INT COMMENT '关系强度',
    conditions TEXT COMMENT '生效条件',
    tags VARCHAR(500) COMMENT '标签',
    status INT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人',
    update_by BIGINT COMMENT '更新人',
    deleted INT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_cause_entity_id (cause_entity_id),
    INDEX idx_effect_entity_id (effect_entity_id),
    INDEX idx_relationship (relationship),
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
) COMMENT='因果关系表';

-- AI 提供商表
CREATE TABLE IF NOT EXISTS ai_provider (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '提供商ID',
    name VARCHAR(100) NOT NULL COMMENT '提供商名称',
    provider_type VARCHAR(50) NOT NULL COMMENT '提供商类型',
    base_url VARCHAR(500) COMMENT '基础地址',
    api_key VARCHAR(255) COMMENT 'API Key',
    model_name VARCHAR(100) COMMENT '模型名称',
    embedding_model VARCHAR(100) COMMENT '向量模型',
    temperature DECIMAL(5,2) DEFAULT 0.70 COMMENT 'Temperature',
    top_p DECIMAL(5,2) DEFAULT 1.00 COMMENT 'Top P',
    max_tokens INT DEFAULT 2048 COMMENT '最大 Tokens',
    timeout_seconds INT DEFAULT 60 COMMENT '超时时间',
    enabled INT DEFAULT 1 COMMENT '是否启用',
    is_default INT DEFAULT 0 COMMENT '是否默认',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_provider_type (provider_type),
    INDEX idx_enabled (enabled)
) COMMENT='AI提供商表';

-- RAG 知识文档表
CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '知识文档ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    source_type VARCHAR(50) COMMENT '来源类型',
    source_ref_id VARCHAR(100) COMMENT '来源引用ID',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content_text LONGTEXT COMMENT '正文内容',
    summary TEXT COMMENT '摘要',
    status VARCHAR(50) DEFAULT 'ready' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_source_type (source_type),
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
) COMMENT='RAG知识文档表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id BIGINT COMMENT '用户ID',
    module VARCHAR(50) COMMENT '模块',
    action VARCHAR(50) COMMENT '操作',
    target_id BIGINT COMMENT '目标ID',
    description TEXT COMMENT '描述',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent TEXT COMMENT '用户代理',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_module (module),
    INDEX idx_create_time (create_time),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL
) COMMENT='操作日志表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    description VARCHAR(500) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_config_key (config_key)
) COMMENT='系统配置表';
