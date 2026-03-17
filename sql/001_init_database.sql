-- Story Weaver 数据库初始化脚本
-- 版本: 1.0.0
-- 创建日期: 2026-03-16

-- 创建数据库
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
    status INT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_username (username),
    INDEX idx_status (status)
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

-- 人物表
CREATE TABLE IF NOT EXISTS character (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '人物ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    name VARCHAR(100) NOT NULL COMMENT '人物名称',
    description TEXT COMMENT '人物描述',
    profile TEXT COMMENT '详细设定',
    avatar VARCHAR(500) COMMENT '头像',
    status INT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_name (name),
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
) COMMENT='人物表';

-- 世界设定表
CREATE TABLE IF NOT EXISTS world_setting (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '设定ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    title VARCHAR(200) NOT NULL COMMENT '设定标题',
    content LONGTEXT COMMENT '设定内容',
    category VARCHAR(50) COMMENT '分类',
    order_num INT DEFAULT 0 COMMENT '排序号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_category (category),
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
) COMMENT='世界设定表';

-- AI写作记录表
CREATE TABLE IF NOT EXISTS ai_writing_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    chapter_id BIGINT COMMENT '章节ID',
    prompt TEXT NOT NULL COMMENT '提示词',
    response TEXT NOT NULL COMMENT 'AI响应',
    model VARCHAR(100) COMMENT '模型名称',
    token_usage INT DEFAULT 0 COMMENT 'token使用量',
    status INT DEFAULT 0 COMMENT '状态: 0-生成中, 1-成功, 2-失败',
    error_message TEXT COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_project_id (project_id),
    INDEX idx_chapter_id (chapter_id),
    INDEX idx_status (status),
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE SET NULL
) COMMENT='AI写作记录表';

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

echo "数据库表结构创建完成";