-- Story Weaver Database Schema
-- Version: 1.0.0
-- Created: 2025-03-15

-- Create database
CREATE DATABASE IF NOT EXISTS story_weaver DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE story_weaver;

-- User table
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `email` VARCHAR(100) COMMENT '邮箱',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `avatar` VARCHAR(500) COMMENT '头像URL',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记(0:正常,1:删除)',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态(0:禁用,1:正常)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- Project table
CREATE TABLE IF NOT EXISTS `project` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '项目ID',
    `name` VARCHAR(100) NOT NULL COMMENT '项目名称',
    `description` TEXT COMMENT '项目描述',
    `cover_image` VARCHAR(500) COMMENT '封面图片URL',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态(0:草稿,1:进行中,2:已完成,3:已归档)',
    `genre` VARCHAR(50) COMMENT '小说类型',
    `tags` VARCHAR(500) COMMENT '标签(逗号分隔)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记(0:正常,1:删除)',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_update_time` (`update_time`),
    CONSTRAINT `fk_project_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';

-- Chapter table (for future use)
CREATE TABLE IF NOT EXISTS `chapter` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '章节ID',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `title` VARCHAR(200) NOT NULL COMMENT '章节标题',
    `content` LONGTEXT COMMENT '章节内容',
    `order_num` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态(0:草稿,1:完成)',
    `word_count` INT DEFAULT 0 COMMENT '字数统计',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记(0:正常,1:删除)',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_order_num` (`order_num`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_chapter_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='章节表';

-- Character table (for future use)
CREATE TABLE IF NOT EXISTS `character` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `name` VARCHAR(100) NOT NULL COMMENT '角色名称',
    `description` TEXT COMMENT '角色描述',
    `attributes` JSON COMMENT '角色属性(JSON格式)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记(0:正常,1:删除)',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_name` (`name`),
    CONSTRAINT `fk_character_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- World setting table (for future use)
CREATE TABLE IF NOT EXISTS `world_setting` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '世界设定ID',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `name` VARCHAR(100) NOT NULL COMMENT '设定名称',
    `description` TEXT COMMENT '设定描述',
    `category` VARCHAR(50) COMMENT '分类',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记(0:正常,1:删除)',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_category` (`category`),
    CONSTRAINT `fk_world_setting_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='世界设定表';

-- AI writing record table
CREATE TABLE IF NOT EXISTS `ai_writing_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'AI写作记录ID',
    `chapter_id` BIGINT NOT NULL COMMENT '章节ID',
    `original_content` LONGTEXT COMMENT '原始内容',
    `generated_content` LONGTEXT COMMENT '生成内容',
    `writing_type` VARCHAR(50) COMMENT '写作类型(continue:续写,polish:润色,expand:扩写,rewrite:改写)',
    `user_instruction` TEXT COMMENT '用户指令',
    `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态(draft:草稿,accepted:已采纳,rejected:已拒绝)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记(0:正常,1:删除)',
    PRIMARY KEY (`id`),
    KEY `idx_chapter_id` (`chapter_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_ai_writing_chapter` FOREIGN KEY (`chapter_id`) REFERENCES `chapter` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI写作记录表';

-- Insert default admin user (password: admin123)
INSERT INTO `user` (`username`, `password`, `email`, `nickname`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTV5UiC', 'admin@storyweaver.com', '管理员', 1),
('testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTV5UiC', 'test@storyweaver.com', '测试用户', 1);

-- Insert sample projects
INSERT INTO `project` (`name`, `description`, `user_id`, `genre`, `tags`) VALUES
('星辰传说', '一部关于星际探险的科幻小说', 1, '科幻', '太空,探险,未来'),
('剑与魔法', '传统奇幻冒险故事', 1, '奇幻', '魔法,剑士,冒险'),
('都市异能', '现代都市中的超能力者故事', 2, '都市', '异能,现代,悬疑');