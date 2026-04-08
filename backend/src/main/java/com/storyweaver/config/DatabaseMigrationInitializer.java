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

                statement.execute(UPSERT_ITEM_PROVIDER_CONFIG_SQL);
                statement.execute(UPSERT_ITEM_MODEL_CONFIG_SQL);
                statement.execute(UPSERT_ITEM_PROMPT_CONFIG_SQL);
                statement.execute(UPSERT_DIRECTOR_PROVIDER_CONFIG_SQL);
                statement.execute(UPSERT_DIRECTOR_MODEL_CONFIG_SQL);
                statement.execute(UPSERT_DIRECTOR_ENABLED_CONFIG_SQL);
                statement.execute(UPSERT_DIRECTOR_TOOL_CALLS_CONFIG_SQL);
                statement.execute(UPSERT_DIRECTOR_SELECTED_MODULES_CONFIG_SQL);
                statement.execute(UPSERT_DIRECTOR_DEBUG_CONFIG_SQL);
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
}
