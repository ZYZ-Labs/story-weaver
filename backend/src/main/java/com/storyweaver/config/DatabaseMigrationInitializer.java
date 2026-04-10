package com.storyweaver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

@Component
public class DatabaseMigrationInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationInitializer.class);

    private static final String CREATE_AI_WRITING_SESSION_SQL = """
            CREATE TABLE IF NOT EXISTS ai_writing_session (
              id BIGINT PRIMARY KEY AUTO_INCREMENT,
              project_id BIGINT NOT NULL,
              chapter_id BIGINT NOT NULL,
              user_id BIGINT NOT NULL,
              active_segment_no INT NOT NULL DEFAULT 1,
              active_window_chars INT NOT NULL DEFAULT 0,
              compressed_summary LONGTEXT,
              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              deleted INT DEFAULT 0,
              UNIQUE KEY uq_ai_writing_session (chapter_id, user_id),
              INDEX idx_ai_writing_session_project_id (project_id),
              INDEX idx_ai_writing_session_user_id (user_id),
              CONSTRAINT fk_ai_writing_session_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
              CONSTRAINT fk_ai_writing_session_chapter FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE CASCADE,
              CONSTRAINT fk_ai_writing_session_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
            )
            """;

    private static final String CREATE_AI_WRITING_CHAT_MESSAGE_SQL = """
            CREATE TABLE IF NOT EXISTS ai_writing_chat_message (
              id BIGINT PRIMARY KEY AUTO_INCREMENT,
              session_id BIGINT NOT NULL,
              chapter_id BIGINT NOT NULL,
              role VARCHAR(20) NOT NULL,
              content LONGTEXT NOT NULL,
              segment_no INT NOT NULL DEFAULT 1,
              pinned_to_background INT NOT NULL DEFAULT 0,
              compressed INT NOT NULL DEFAULT 0,
              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              deleted INT DEFAULT 0,
              INDEX idx_ai_writing_chat_message_session_id (session_id),
              INDEX idx_ai_writing_chat_message_chapter_id (chapter_id),
              INDEX idx_ai_writing_chat_message_segment_no (segment_no),
              INDEX idx_ai_writing_chat_message_background (pinned_to_background),
              CONSTRAINT fk_ai_writing_chat_message_session FOREIGN KEY (session_id) REFERENCES ai_writing_session(id) ON DELETE CASCADE,
              CONSTRAINT fk_ai_writing_chat_message_chapter FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE CASCADE
            )
            """;

    private static final String CREATE_ITEM_SQL = """
            CREATE TABLE IF NOT EXISTS item (
              id BIGINT PRIMARY KEY AUTO_INCREMENT,
              project_id BIGINT NOT NULL,
              owner_user_id BIGINT NOT NULL,
              name VARCHAR(120) NOT NULL,
              description TEXT NULL,
              category VARCHAR(32) NOT NULL DEFAULT 'prop',
              rarity VARCHAR(32) NOT NULL DEFAULT 'common',
              stackable INT NOT NULL DEFAULT 0,
              max_stack INT NOT NULL DEFAULT 1,
              usable INT NOT NULL DEFAULT 0,
              equippable INT NOT NULL DEFAULT 0,
              slot_type VARCHAR(32) NOT NULL DEFAULT 'misc',
              item_value INT NOT NULL DEFAULT 0,
              weight INT NOT NULL DEFAULT 0,
              attributes_json LONGTEXT NULL,
              effect_json LONGTEXT NULL,
              tags VARCHAR(500) NULL,
              source_type VARCHAR(32) NOT NULL DEFAULT 'manual',
              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              deleted INT DEFAULT 0,
              INDEX idx_item_project_deleted (project_id, deleted),
              INDEX idx_item_owner_deleted (owner_user_id, deleted),
              INDEX idx_item_category_rarity (category, rarity),
              CONSTRAINT fk_item_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
              CONSTRAINT fk_item_owner FOREIGN KEY (owner_user_id) REFERENCES user(id) ON DELETE CASCADE
            )
            """;

    private static final String CREATE_CHARACTER_INVENTORY_SQL = """
            CREATE TABLE IF NOT EXISTS character_inventory_item (
              id BIGINT PRIMARY KEY AUTO_INCREMENT,
              project_id BIGINT NOT NULL,
              character_id BIGINT NOT NULL,
              item_id BIGINT NOT NULL,
              quantity INT NOT NULL DEFAULT 1,
              equipped INT NOT NULL DEFAULT 0,
              durability INT NOT NULL DEFAULT 100,
              custom_name VARCHAR(120) NULL,
              notes TEXT NULL,
              sort_order INT NOT NULL DEFAULT 0,
              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              deleted INT DEFAULT 0,
              INDEX idx_inventory_character (character_id),
              INDEX idx_inventory_project_character (project_id, character_id),
              INDEX idx_inventory_item (item_id),
              CONSTRAINT fk_inventory_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
              CONSTRAINT fk_inventory_character FOREIGN KEY (character_id) REFERENCES `character`(id) ON DELETE CASCADE,
              CONSTRAINT fk_inventory_item FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE CASCADE
            )
            """;

    private static final String CREATE_AI_DIRECTOR_DECISION_SQL = """
            CREATE TABLE IF NOT EXISTS ai_director_decision (
              id BIGINT PRIMARY KEY AUTO_INCREMENT,
              project_id BIGINT NOT NULL,
              chapter_id BIGINT NOT NULL,
              user_id BIGINT NOT NULL,
              source_type VARCHAR(32) NOT NULL DEFAULT 'writing',
              entry_point VARCHAR(64) NOT NULL DEFAULT 'writing-center',
              stage VARCHAR(32) NOT NULL,
              writing_mode VARCHAR(32) NOT NULL,
              target_word_count INT NULL,
              selected_modules_json LONGTEXT NOT NULL,
              module_weights_json LONGTEXT NOT NULL,
              required_facts_json LONGTEXT NOT NULL,
              prohibited_moves_json LONGTEXT NOT NULL,
              decision_pack_json LONGTEXT NOT NULL,
              tool_trace_json LONGTEXT NOT NULL,
              selected_provider_id BIGINT NOT NULL,
              selected_model VARCHAR(128) NOT NULL,
              status VARCHAR(32) NOT NULL DEFAULT 'generated',
              error_message VARCHAR(500) NULL,
              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              deleted INT DEFAULT 0,
              INDEX idx_ai_director_decision_chapter_id (chapter_id),
              INDEX idx_ai_director_decision_project_id (project_id),
              INDEX idx_ai_director_decision_user_id (user_id),
              INDEX idx_ai_director_decision_status (status),
              CONSTRAINT fk_ai_director_decision_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
              CONSTRAINT fk_ai_director_decision_chapter FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE CASCADE,
              CONSTRAINT fk_ai_director_decision_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
            )
            """;

    private static final String ALTER_AI_WRITING_RECORD_ADD_DIRECTOR_DECISION_ID_SQL = """
            ALTER TABLE ai_writing_record
            ADD COLUMN director_decision_id BIGINT NULL,
            ADD INDEX idx_ai_writing_record_director_decision_id (director_decision_id)
            """;

    private static final String ALTER_AI_WRITING_RECORD_ADD_GENERATION_TRACE_JSON_SQL = """
            ALTER TABLE ai_writing_record
            ADD COLUMN generation_trace_json JSON NULL
            """;

    private static final String UPSERT_ITEM_PROVIDER_CONFIG_SQL = """
            INSERT INTO system_config (config_key, config_value, description)
            VALUES ('item_ai_provider_id', '', '物品生成默认 Provider ID')
            ON DUPLICATE KEY UPDATE description = VALUES(description)
            """;

    private static final String UPSERT_ITEM_MODEL_CONFIG_SQL = """
            INSERT INTO system_config (config_key, config_value, description)
            VALUES ('item_ai_model', '', '物品生成默认模型名称')
            ON DUPLICATE KEY UPDATE description = VALUES(description)
            """;

    private static final String UPSERT_ITEM_PROMPT_CONFIG_SQL = """
            INSERT INTO system_config (config_key, config_value, description)
            VALUES ('prompt.item_generation', '优先生成适合长篇创作的道具、药品、装备、材料与任务物品，名称、说明、效果和标签必须便于剧情使用。', '物品生成提示词模板')
            ON DUPLICATE KEY UPDATE description = VALUES(description)
            """;

    private static final String UPSERT_DIRECTOR_PROVIDER_CONFIG_SQL = """
            INSERT INTO system_config (config_key, config_value, description)
            VALUES ('director_ai_provider_id', '1', 'AI 总导决策层默认模型服务')
            ON DUPLICATE KEY UPDATE description = VALUES(description)
            """;

    private static final String UPSERT_DIRECTOR_MODEL_CONFIG_SQL = """
            INSERT INTO system_config (config_key, config_value, description)
            VALUES ('director_ai_model', 'qwen2.5:7b', 'AI 总导决策层默认模型')
            ON DUPLICATE KEY UPDATE description = VALUES(description)
            """;

    private static final String UPSERT_DIRECTOR_ENABLED_CONFIG_SQL = """
            INSERT INTO system_config (config_key, config_value, description)
            VALUES ('ai.director.enabled', 'true', '是否启用 AI 总导决策层')
            ON DUPLICATE KEY UPDATE description = VALUES(description)
            """;

    private static final String UPSERT_DIRECTOR_TOOL_CALLS_CONFIG_SQL = """
            INSERT INTO system_config (config_key, config_value, description)
            VALUES ('ai.director.max_tool_calls', '4', '总导决策层最大工具调用次数')
            ON DUPLICATE KEY UPDATE description = VALUES(description)
            """;

    private static final String UPSERT_DIRECTOR_SELECTED_MODULES_CONFIG_SQL = """
            INSERT INTO system_config (config_key, config_value, description)
            VALUES ('ai.director.max_selected_modules', '6', '总导决策层最大选中模块数')
            ON DUPLICATE KEY UPDATE description = VALUES(description)
            """;

    private static final String UPSERT_DIRECTOR_DEBUG_CONFIG_SQL = """
            INSERT INTO system_config (config_key, config_value, description)
            VALUES ('ai.director.debug_expose_decision', 'false', '是否暴露总导决策调试信息')
            ON DUPLICATE KEY UPDATE description = VALUES(description)
            """;

    private static final String CREATE_OUTLINE_WORLD_SETTING_SQL = """
            CREATE TABLE IF NOT EXISTS outline_world_setting (
              id BIGINT PRIMARY KEY AUTO_INCREMENT,
              outline_id BIGINT NOT NULL,
              world_setting_id BIGINT NOT NULL,
              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              UNIQUE KEY uq_outline_world_setting (outline_id, world_setting_id),
              INDEX idx_ows_outline_id (outline_id),
              INDEX idx_ows_world_setting_id (world_setting_id),
              CONSTRAINT fk_ows_outline FOREIGN KEY (outline_id) REFERENCES chapter_outline(id) ON DELETE CASCADE,
              CONSTRAINT fk_ows_world_setting FOREIGN KEY (world_setting_id) REFERENCES world_setting(id) ON DELETE CASCADE
            )
            """;

    private static final String CREATE_OUTLINE_PLOT_SQL = """
            CREATE TABLE IF NOT EXISTS outline_plot (
              id BIGINT PRIMARY KEY AUTO_INCREMENT,
              outline_id BIGINT NOT NULL,
              plot_id BIGINT NOT NULL,
              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              UNIQUE KEY uq_outline_plot (outline_id, plot_id),
              INDEX idx_op_outline_id (outline_id),
              INDEX idx_op_plot_id (plot_id),
              CONSTRAINT fk_op_outline FOREIGN KEY (outline_id) REFERENCES chapter_outline(id) ON DELETE CASCADE,
              CONSTRAINT fk_op_plot FOREIGN KEY (plot_id) REFERENCES plot(id) ON DELETE CASCADE
            )
            """;

    private static final String CREATE_OUTLINE_CAUSALITY_SQL = """
            CREATE TABLE IF NOT EXISTS outline_causality (
              id BIGINT PRIMARY KEY AUTO_INCREMENT,
              outline_id BIGINT NOT NULL,
              causality_id BIGINT NOT NULL,
              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              UNIQUE KEY uq_outline_causality (outline_id, causality_id),
              INDEX idx_oc_outline_id (outline_id),
              INDEX idx_oc_causality_id (causality_id),
              CONSTRAINT fk_oc_outline FOREIGN KEY (outline_id) REFERENCES chapter_outline(id) ON DELETE CASCADE,
              CONSTRAINT fk_oc_causality FOREIGN KEY (causality_id) REFERENCES causality(id) ON DELETE CASCADE
            )
            """;

    private static final String CREATE_OUTLINE_CHARACTER_FOCUS_SQL = """
            CREATE TABLE IF NOT EXISTS outline_character_focus (
              id BIGINT PRIMARY KEY AUTO_INCREMENT,
              outline_id BIGINT NOT NULL,
              character_id BIGINT NOT NULL,
              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              UNIQUE KEY uq_outline_character_focus (outline_id, character_id),
              INDEX idx_ocf_outline_id (outline_id),
              INDEX idx_ocf_character_id (character_id),
              CONSTRAINT fk_ocf_outline FOREIGN KEY (outline_id) REFERENCES chapter_outline(id) ON DELETE CASCADE,
              CONSTRAINT fk_ocf_character FOREIGN KEY (character_id) REFERENCES `character`(id) ON DELETE CASCADE
            )
            """;

    private static final String CREATE_CHAPTER_PLOT_SQL = """
            CREATE TABLE IF NOT EXISTS chapter_plot (
              id BIGINT PRIMARY KEY AUTO_INCREMENT,
              chapter_id BIGINT NOT NULL,
              plot_id BIGINT NOT NULL,
              relation_type VARCHAR(32) NOT NULL DEFAULT 'primary',
              sort_order INT NOT NULL DEFAULT 0,
              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              UNIQUE KEY uq_chapter_plot (chapter_id, plot_id),
              INDEX idx_cp_chapter_id (chapter_id),
              INDEX idx_cp_plot_id (plot_id),
              CONSTRAINT fk_cp_chapter FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE CASCADE,
              CONSTRAINT fk_cp_plot FOREIGN KEY (plot_id) REFERENCES plot(id) ON DELETE CASCADE
            )
            """;

    private static final String UPSERT_STORY_REFACTOR_ENABLED_CONFIG_SQL = """
            INSERT INTO system_config (config_key, config_value, description)
            VALUES ('story.refactor.v1.enabled', 'false', '是否启用 Story 核心模块重构能力')
            ON DUPLICATE KEY UPDATE description = VALUES(description)
            """;

    private static final String UPSERT_STORY_REFACTOR_READ_NEW_RELATIONS_CONFIG_SQL = """
            INSERT INTO system_config (config_key, config_value, description)
            VALUES ('story.refactor.v1.read_new_relations_first', 'false', '是否优先读取新关系表')
            ON DUPLICATE KEY UPDATE description = VALUES(description)
            """;

    private static final String UPSERT_STORY_REFACTOR_OUTLINE_TREE_CONFIG_SQL = """
            INSERT INTO system_config (config_key, config_value, description)
            VALUES ('story.refactor.v1.enable_outline_tree', 'false', '是否启用大纲树能力')
            ON DUPLICATE KEY UPDATE description = VALUES(description)
            """;

    private static final String UPSERT_STORY_REFACTOR_STORY_GRAPH_CONFIG_SQL = """
            INSERT INTO system_config (config_key, config_value, description)
            VALUES ('story.refactor.v1.enable_story_graph', 'false', '是否启用剧情链与因果图新结构')
            ON DUPLICATE KEY UPDATE description = VALUES(description)
            """;

    private final DataSource dataSource;

    public DatabaseMigrationInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            String normalizedProduct = productName == null ? "" : productName.toLowerCase(Locale.ROOT);
            if (!normalizedProduct.contains("mysql") && !normalizedProduct.contains("mariadb")) {
                return;
            }

            boolean sessionTableExists = tableExists(connection, "ai_writing_session");
            boolean messageTableExists = tableExists(connection, "ai_writing_chat_message");
            boolean itemTableExists = tableExists(connection, "item");
            boolean inventoryTableExists = tableExists(connection, "character_inventory_item");
            boolean directorDecisionTableExists = tableExists(connection, "ai_director_decision");
            boolean aiWritingRecordTableExists = tableExists(connection, "ai_writing_record");
            boolean directorDecisionColumnExists = aiWritingRecordTableExists
                    && columnExists(connection, "ai_writing_record", "director_decision_id");
            boolean generationTraceColumnExists = aiWritingRecordTableExists
                    && columnExists(connection, "ai_writing_record", "generation_trace_json");

            try (Statement statement = connection.createStatement()) {
                if (!sessionTableExists) {
                    log.warn("检测到 ai_writing_session 缺失，开始自动补齐。");
                    statement.execute(CREATE_AI_WRITING_SESSION_SQL);
                }
                if (!messageTableExists) {
                    log.warn("检测到 ai_writing_chat_message 缺失，开始自动补齐。");
                    statement.execute(CREATE_AI_WRITING_CHAT_MESSAGE_SQL);
                }
                if (!itemTableExists) {
                    log.warn("检测到 item 缺失，开始自动补齐。");
                    statement.execute(CREATE_ITEM_SQL);
                }
                if (!inventoryTableExists) {
                    log.warn("检测到 character_inventory_item 缺失，开始自动补齐。");
                    statement.execute(CREATE_CHARACTER_INVENTORY_SQL);
                }
                if (!directorDecisionTableExists) {
                    log.warn("检测到 ai_director_decision 缺失，开始自动补齐。");
                    statement.execute(CREATE_AI_DIRECTOR_DECISION_SQL);
                }
                if (aiWritingRecordTableExists && !directorDecisionColumnExists) {
                    log.warn("检测到 ai_writing_record.director_decision_id 缺失，开始自动补齐。");
                    statement.execute(ALTER_AI_WRITING_RECORD_ADD_DIRECTOR_DECISION_ID_SQL);
                }
                if (aiWritingRecordTableExists && !generationTraceColumnExists) {
                    log.warn("检测到 ai_writing_record.generation_trace_json 缺失，开始自动补齐。");
                    statement.execute(ALTER_AI_WRITING_RECORD_ADD_GENERATION_TRACE_JSON_SQL);
                }

                statement.execute(UPSERT_ITEM_PROVIDER_CONFIG_SQL);
                statement.execute(UPSERT_ITEM_MODEL_CONFIG_SQL);
                statement.execute(UPSERT_ITEM_PROMPT_CONFIG_SQL);
                statement.execute(UPSERT_DIRECTOR_PROVIDER_CONFIG_SQL);
                statement.execute(UPSERT_DIRECTOR_MODEL_CONFIG_SQL);
                statement.execute(UPSERT_DIRECTOR_ENABLED_CONFIG_SQL);
                statement.execute(UPSERT_DIRECTOR_TOOL_CALLS_CONFIG_SQL);
                statement.execute(UPSERT_DIRECTOR_SELECTED_MODULES_CONFIG_SQL);
                statement.execute(UPSERT_DIRECTOR_DEBUG_CONFIG_SQL);
                ensureStoryRefactorBase(connection, statement);
            }

            log.info("数据库增量初始化检查完成。");
        } catch (SQLException exception) {
            throw new IllegalStateException("初始化增量数据表失败，请检查数据库权限和表结构", exception);
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();
        return exists(metaData, catalog, tableName)
                || exists(metaData, catalog, tableName.toLowerCase(Locale.ROOT))
                || exists(metaData, catalog, tableName.toUpperCase(Locale.ROOT));
    }

    private boolean exists(DatabaseMetaData metaData, String catalog, String tableName) throws SQLException {
        try (ResultSet resultSet = metaData.getTables(catalog, null, tableName, new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();
        try (ResultSet resultSet = metaData.getColumns(catalog, null, tableName, columnName)) {
            if (resultSet.next()) {
                return true;
            }
        }
        try (ResultSet resultSet = metaData.getColumns(catalog, null, tableName.toLowerCase(Locale.ROOT), columnName.toLowerCase(Locale.ROOT))) {
            if (resultSet.next()) {
                return true;
            }
        }
        try (ResultSet resultSet = metaData.getColumns(catalog, null, tableName.toUpperCase(Locale.ROOT), columnName.toUpperCase(Locale.ROOT))) {
            return resultSet.next();
        }
    }

    private void ensureStoryRefactorBase(Connection connection, Statement statement) throws SQLException {
        ensureColumnExists(connection, statement, "chapter_outline", "outline_type",
                "ALTER TABLE chapter_outline ADD COLUMN outline_type VARCHAR(20) NOT NULL DEFAULT 'chapter'",
                "检测到 chapter_outline.outline_type 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "chapter_outline", "parent_outline_id",
                "ALTER TABLE chapter_outline ADD COLUMN parent_outline_id BIGINT NULL",
                "检测到 chapter_outline.parent_outline_id 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "chapter_outline", "root_outline_id",
                "ALTER TABLE chapter_outline ADD COLUMN root_outline_id BIGINT NULL",
                "检测到 chapter_outline.root_outline_id 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "chapter_outline", "generated_chapter_id",
                "ALTER TABLE chapter_outline ADD COLUMN generated_chapter_id BIGINT NULL",
                "检测到 chapter_outline.generated_chapter_id 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "chapter_outline", "related_world_setting_ids_json",
                "ALTER TABLE chapter_outline ADD COLUMN related_world_setting_ids_json JSON NULL",
                "检测到 chapter_outline.related_world_setting_ids_json 缺失，开始自动补齐。");

        ensureColumnExists(connection, statement, "plot", "story_beat_type",
                "ALTER TABLE plot ADD COLUMN story_beat_type VARCHAR(32) NOT NULL DEFAULT 'main'",
                "检测到 plot.story_beat_type 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "plot", "story_function",
                "ALTER TABLE plot ADD COLUMN story_function VARCHAR(32) NOT NULL DEFAULT 'advance_mainline'",
                "检测到 plot.story_function 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "plot", "event_result",
                "ALTER TABLE plot ADD COLUMN event_result TEXT NULL",
                "检测到 plot.event_result 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "plot", "prev_beat_id",
                "ALTER TABLE plot ADD COLUMN prev_beat_id BIGINT NULL",
                "检测到 plot.prev_beat_id 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "plot", "next_beat_id",
                "ALTER TABLE plot ADD COLUMN next_beat_id BIGINT NULL",
                "检测到 plot.next_beat_id 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "plot", "outline_priority",
                "ALTER TABLE plot ADD COLUMN outline_priority INT NULL",
                "检测到 plot.outline_priority 缺失，开始自动补齐。");

        ensureColumnExists(connection, statement, "causality", "causal_type",
                "ALTER TABLE causality ADD COLUMN causal_type VARCHAR(32) NOT NULL DEFAULT 'trigger'",
                "检测到 causality.causal_type 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "causality", "trigger_mode",
                "ALTER TABLE causality ADD COLUMN trigger_mode VARCHAR(32) NULL",
                "检测到 causality.trigger_mode 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "causality", "payoff_status",
                "ALTER TABLE causality ADD COLUMN payoff_status VARCHAR(32) NULL",
                "检测到 causality.payoff_status 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "causality", "upstream_cause_ids_json",
                "ALTER TABLE causality ADD COLUMN upstream_cause_ids_json JSON NULL",
                "检测到 causality.upstream_cause_ids_json 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "causality", "downstream_effect_ids_json",
                "ALTER TABLE causality ADD COLUMN downstream_effect_ids_json JSON NULL",
                "检测到 causality.downstream_effect_ids_json 缺失，开始自动补齐。");

        ensureColumnExists(connection, statement, "character", "identity",
                "ALTER TABLE `character` ADD COLUMN identity VARCHAR(120) NULL",
                "检测到 character.identity 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "character", "core_goal",
                "ALTER TABLE `character` ADD COLUMN core_goal TEXT NULL",
                "检测到 character.core_goal 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "character", "growth_arc",
                "ALTER TABLE `character` ADD COLUMN growth_arc TEXT NULL",
                "检测到 character.growth_arc 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "character", "first_appearance_chapter_id",
                "ALTER TABLE `character` ADD COLUMN first_appearance_chapter_id BIGINT NULL",
                "检测到 character.first_appearance_chapter_id 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "character", "active_stage",
                "ALTER TABLE `character` ADD COLUMN active_stage VARCHAR(32) NULL",
                "检测到 character.active_stage 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "character", "is_retired",
                "ALTER TABLE `character` ADD COLUMN is_retired TINYINT NOT NULL DEFAULT 0",
                "检测到 character.is_retired 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "character", "advanced_profile_json",
                "ALTER TABLE `character` ADD COLUMN advanced_profile_json JSON NULL",
                "检测到 character.advanced_profile_json 缺失，开始自动补齐。");

        ensureColumnExists(connection, statement, "project_character", "role_type",
                "ALTER TABLE project_character ADD COLUMN role_type VARCHAR(50) NULL",
                "检测到 project_character.role_type 缺失，开始自动补齐。");

        ensureColumnExists(connection, statement, "chapter", "chapter_status",
                "ALTER TABLE chapter ADD COLUMN chapter_status VARCHAR(20) NOT NULL DEFAULT 'draft'",
                "检测到 chapter.chapter_status 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "chapter", "summary",
                "ALTER TABLE chapter ADD COLUMN summary TEXT NULL",
                "检测到 chapter.summary 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "chapter", "outline_id",
                "ALTER TABLE chapter ADD COLUMN outline_id BIGINT NULL",
                "检测到 chapter.outline_id 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "chapter", "prev_chapter_id",
                "ALTER TABLE chapter ADD COLUMN prev_chapter_id BIGINT NULL",
                "检测到 chapter.prev_chapter_id 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "chapter", "next_chapter_id",
                "ALTER TABLE chapter ADD COLUMN next_chapter_id BIGINT NULL",
                "检测到 chapter.next_chapter_id 缺失，开始自动补齐。");
        ensureColumnExists(connection, statement, "chapter", "main_pov_character_id",
                "ALTER TABLE chapter ADD COLUMN main_pov_character_id BIGINT NULL",
                "检测到 chapter.main_pov_character_id 缺失，开始自动补齐。");

        ensureTableExists(connection, statement, "outline_world_setting", CREATE_OUTLINE_WORLD_SETTING_SQL,
                "检测到 outline_world_setting 缺失，开始自动补齐。");
        ensureTableExists(connection, statement, "outline_plot", CREATE_OUTLINE_PLOT_SQL,
                "检测到 outline_plot 缺失，开始自动补齐。");
        ensureTableExists(connection, statement, "outline_causality", CREATE_OUTLINE_CAUSALITY_SQL,
                "检测到 outline_causality 缺失，开始自动补齐。");
        ensureTableExists(connection, statement, "outline_character_focus", CREATE_OUTLINE_CHARACTER_FOCUS_SQL,
                "检测到 outline_character_focus 缺失，开始自动补齐。");
        ensureTableExists(connection, statement, "chapter_plot", CREATE_CHAPTER_PLOT_SQL,
                "检测到 chapter_plot 缺失，开始自动补齐。");

        statement.execute(UPSERT_STORY_REFACTOR_ENABLED_CONFIG_SQL);
        statement.execute(UPSERT_STORY_REFACTOR_READ_NEW_RELATIONS_CONFIG_SQL);
        statement.execute(UPSERT_STORY_REFACTOR_OUTLINE_TREE_CONFIG_SQL);
        statement.execute(UPSERT_STORY_REFACTOR_STORY_GRAPH_CONFIG_SQL);
    }

    private void ensureTableExists(
            Connection connection,
            Statement statement,
            String tableName,
            String createSql,
            String logMessage) throws SQLException {
        if (!tableExists(connection, tableName)) {
            log.warn(logMessage);
            statement.execute(createSql);
        }
    }

    private void ensureColumnExists(
            Connection connection,
            Statement statement,
            String tableName,
            String columnName,
            String alterSql,
            String logMessage) throws SQLException {
        if (!tableExists(connection, tableName)) {
            return;
        }
        if (!columnExists(connection, tableName, columnName)) {
            log.warn(logMessage);
            statement.execute(alterSql);
        }
    }
}
