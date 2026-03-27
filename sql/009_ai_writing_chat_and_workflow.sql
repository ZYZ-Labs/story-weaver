USE story_weaver;

CREATE TABLE IF NOT EXISTS ai_writing_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '写作会话ID',
  project_id BIGINT NOT NULL COMMENT '项目ID',
  chapter_id BIGINT NOT NULL COMMENT '章节ID',
  user_id BIGINT NOT NULL COMMENT '所属用户ID',
  active_segment_no INT NOT NULL DEFAULT 1 COMMENT '当前活跃分段编号',
  active_window_chars INT NOT NULL DEFAULT 0 COMMENT '当前活跃窗口字符数',
  compressed_summary LONGTEXT COMMENT '压缩后的历史摘要',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted INT DEFAULT 0 COMMENT '删除标记: 0-未删除 1-已删除',
  UNIQUE KEY uq_ai_writing_session (chapter_id, user_id),
  INDEX idx_ai_writing_session_project_id (project_id),
  INDEX idx_ai_writing_session_user_id (user_id),
  FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
  FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) COMMENT='AI 写作聊天会话表';

CREATE TABLE IF NOT EXISTS ai_writing_chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '写作聊天消息ID',
  session_id BIGINT NOT NULL COMMENT '写作会话ID',
  chapter_id BIGINT NOT NULL COMMENT '章节ID',
  role VARCHAR(20) NOT NULL COMMENT '消息角色: user/assistant/system',
  content LONGTEXT NOT NULL COMMENT '消息内容',
  segment_no INT NOT NULL DEFAULT 1 COMMENT '所属分段编号',
  pinned_to_background INT NOT NULL DEFAULT 0 COMMENT '是否加入背景信息',
  compressed INT NOT NULL DEFAULT 0 COMMENT '是否已被压缩摘要覆盖',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted INT DEFAULT 0 COMMENT '删除标记: 0-未删除 1-已删除',
  INDEX idx_ai_writing_chat_message_session_id (session_id),
  INDEX idx_ai_writing_chat_message_chapter_id (chapter_id),
  INDEX idx_ai_writing_chat_message_segment_no (segment_no),
  INDEX idx_ai_writing_chat_message_background (pinned_to_background),
  FOREIGN KEY (session_id) REFERENCES ai_writing_session(id) ON DELETE CASCADE,
  FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE CASCADE
) COMMENT='AI 写作聊天消息表';
