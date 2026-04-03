CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100),
    password VARCHAR(255),
    email VARCHAR(255),
    nickname VARCHAR(100),
    avatar VARCHAR(255),
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL,
    deleted INT DEFAULT 0,
    status INT DEFAULT 0,
    role_code VARCHAR(50),
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP NULL,
    last_login_at TIMESTAMP NULL,
    password_changed_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    cover_image VARCHAR(255),
    user_id BIGINT,
    status INT DEFAULT 0,
    genre VARCHAR(100),
    tags VARCHAR(500),
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS chapter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    title VARCHAR(255),
    content TEXT,
    order_num INT DEFAULT 0,
    status INT DEFAULT 0,
    word_count INT DEFAULT 0,
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS `character` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    owner_user_id BIGINT,
    name VARCHAR(255),
    description TEXT,
    attributes TEXT,
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS project_character (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    character_id BIGINT,
    project_role VARCHAR(100),
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS chapter_character (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chapter_id BIGINT,
    character_id BIGINT,
    required_flag INT DEFAULT 0,
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS world_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    owner_user_id BIGINT,
    name VARCHAR(255),
    description TEXT,
    title VARCHAR(255),
    content TEXT,
    category VARCHAR(100),
    order_num INT DEFAULT 0,
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS project_world_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    world_setting_id BIGINT,
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS ai_provider (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    provider_type VARCHAR(100),
    base_url VARCHAR(500),
    api_key VARCHAR(500),
    model_name VARCHAR(255),
    embedding_model VARCHAR(255),
    temperature DOUBLE,
    top_p DOUBLE,
    max_tokens INT,
    timeout_seconds INT,
    enabled INT DEFAULT 1,
    is_default INT DEFAULT 0,
    remark TEXT,
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ai_writing_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chapter_id BIGINT,
    original_content TEXT,
    generated_content TEXT,
    writing_type VARCHAR(50),
    user_instruction TEXT,
    selected_provider_id BIGINT,
    selected_model VARCHAR(255),
    prompt_snapshot TEXT,
    status VARCHAR(50),
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ai_writing_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    chapter_id BIGINT,
    user_id BIGINT,
    active_segment_no INT DEFAULT 1,
    active_window_chars INT DEFAULT 0,
    compressed_summary TEXT,
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ai_writing_chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT,
    chapter_id BIGINT,
    role VARCHAR(50),
    content TEXT,
    segment_no INT DEFAULT 1,
    pinned_to_background INT DEFAULT 0,
    compressed INT DEFAULT 0,
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS plot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    chapter_id BIGINT,
    title VARCHAR(255),
    description TEXT,
    content TEXT,
    plot_type INT,
    sequence INT,
    characters TEXT,
    locations TEXT,
    timeline TEXT,
    conflicts TEXT,
    resolutions TEXT,
    tags TEXT,
    status INT DEFAULT 0,
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL,
    create_by BIGINT,
    update_by BIGINT,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS chapter_outline (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    chapter_id BIGINT,
    title VARCHAR(255),
    summary TEXT,
    content TEXT,
    stage_goal TEXT,
    key_conflict TEXT,
    turning_points TEXT,
    expected_ending TEXT,
    focus_character_ids TEXT,
    related_plot_ids TEXT,
    related_causality_ids TEXT,
    status INT DEFAULT 0,
    order_num INT DEFAULT 0,
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS causality (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    name VARCHAR(255),
    description TEXT,
    cause_type VARCHAR(100),
    effect_type VARCHAR(100),
    cause_entity_id VARCHAR(255),
    effect_entity_id VARCHAR(255),
    cause_entity_type VARCHAR(100),
    effect_entity_type VARCHAR(100),
    relationship VARCHAR(100),
    strength INT,
    conditions TEXT,
    tags TEXT,
    status INT DEFAULT 0,
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL,
    create_by BIGINT,
    update_by BIGINT,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    source_type VARCHAR(100),
    source_ref_id VARCHAR(255),
    title VARCHAR(255),
    content_text TEXT,
    summary TEXT,
    status VARCHAR(50),
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(255),
    config_value TEXT,
    description VARCHAR(255),
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    owner_user_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    category VARCHAR(32) DEFAULT 'prop',
    rarity VARCHAR(32) DEFAULT 'common',
    stackable INT DEFAULT 0,
    max_stack INT DEFAULT 1,
    usable INT DEFAULT 0,
    equippable INT DEFAULT 0,
    slot_type VARCHAR(32) DEFAULT 'misc',
    item_value INT DEFAULT 0,
    weight INT DEFAULT 0,
    attributes_json TEXT,
    effect_json TEXT,
    tags VARCHAR(500),
    source_type VARCHAR(32) DEFAULT 'manual',
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS character_inventory_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    character_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity INT DEFAULT 1,
    equipped INT DEFAULT 0,
    durability INT DEFAULT 100,
    custom_name VARCHAR(120),
    notes TEXT,
    sort_order INT DEFAULT 0,
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL,
    deleted INT DEFAULT 0
);
