USE story_weaver;

-- Story 核心模块重构 Phase 3: 仅在完成完整切流后执行。
-- 默认不执行 destructive DDL；先留清单和顺序。

SELECT 'Execute cleanup only after story.refactor.v1.read_new_relations_first=true has been stable in production.' AS warning_message;

-- 建议执行顺序:
-- 1. 停止旧字段写入
-- 2. 观察至少一个完整版本周期
-- 3. 确认 AI 写作 / AI 总导 / 前端页面全部只读新结构
-- 4. 再执行下列清理动作

-- 候选清理项:
-- ALTER TABLE chapter_outline DROP COLUMN focus_character_ids;
-- ALTER TABLE chapter_outline DROP COLUMN related_plot_ids;
-- ALTER TABLE chapter_outline DROP COLUMN related_causality_ids;
-- ALTER TABLE plot DROP COLUMN resolutions;
-- ALTER TABLE project_character DROP COLUMN project_role;
-- ALTER TABLE chapter DROP COLUMN status;

-- 若最终决定重命名资源:
-- 1. 先完成后端 DTO / VO 与前端 store 适配
-- 2. 再考虑是否把 `plot` 正式迁移为 `story_beat`

-- 风险提示:
-- 1. 该阶段不可与回填脚本同批次执行
-- 2. 执行前必须完成数据库备份
-- 3. 执行前必须确认旧版本前端和旧版本服务已经全部下线
