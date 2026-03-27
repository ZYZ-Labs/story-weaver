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
            if (sessionTableExists && messageTableExists) {
                return;
            }

            log.warn("检测到 AI 写作聊天相关数据表缺失，开始自动补齐。sessionExists={}, messageExists={}",
                    sessionTableExists, messageTableExists);

            try (Statement statement = connection.createStatement()) {
                if (!sessionTableExists) {
                    statement.execute(CREATE_AI_WRITING_SESSION_SQL);
                }
                if (!messageTableExists) {
                    statement.execute(CREATE_AI_WRITING_CHAT_MESSAGE_SQL);
                }
            }

            log.info("AI 写作聊天相关数据表已补齐完成。");
        } catch (SQLException exception) {
            throw new IllegalStateException("初始化 AI 写作聊天数据表失败，请检查数据库权限和表结构", exception);
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
}
