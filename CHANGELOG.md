# Changelog

## 2026-04-09

### AI Director

- 新增 AI 总导决策精确查询接口 `GET /api/ai-director/{decisionId}`，前端可按生成记录绑定的 `directorDecisionId` 读取本轮真实决策，而不是回退到章节级最新记录猜测。
- 写作中心与章节页补齐“本轮总导”只读摘要卡片，展示阶段、模式、目标字数、模块选择、硬约束、禁止事项、写作提示和工具调用调试信息。
- 流式工作流日志补齐 `director` 阶段语义；当开启 `ai.director.debug_expose_decision` 时，会输出总导概览、决策摘要、已选模块、约束和工具调用日志。
- AI 写作流式前端修正 `complete` 事件的最终记录返回，避免 SSE 在拿到最终记录后继续等待导致结果丢失。

### Workflow UX

- 补齐 AI 写作工作流的 `prepare`、`context` 阶段中文映射。
- 在修订稿通过 `replace` 事件覆盖正文时，追加“已应用修订稿”的显式日志提示。
- 统一写作中心和章节页初稿助手的工作流说明文案，确保两处展示语义一致。

### Docs & Archive

- 更新 `docs/agent-context.md`、AI 总导需求/进度文档，当前主线收敛到真实兼容 Provider 的 tool calling 联调。
- 将已完成的 `REQ-20260408-writing-workflow-ui-bug` 需求、计划、进度文档转入 `docs/archive/`。
- 归档旧版顶层计划文档，保留历史背景但不再作为恢复入口。

### Verification

- 通过 `mvn -f backend/pom.xml -DskipTests compile` 验证后端编译。
- 通过 `cd front && npm run build` 验证前端构建。
- 前端构建仍存在现有 chunk size warning，但不影响本次功能交付。

## 2026-04-03

### Plan Status

- 新增三阶段实施计划文档 `docs/plan-2026-04-02-2130.md`，并将状态更新为已完成。
- 按既定顺序完成 Phase 1 `cover` 应用、Phase 2 后端结构化、Phase 3 角色与背包系统开发。

### Phase 1 - Cover 前端应用

- 将 `front/public/cover.png` 接入登录页主视觉区域，重构左侧展示层次，保留右侧登录/注册表单流程不变。
- 补充登录页样式，使封面图、标题文案和能力卡片在桌面端与移动端下都能稳定展示。

### Phase 2 - 后端结构化处理

- 新增 `com.storyweaver.common.web` 统一响应基础能力，补充 `ApiResponse`。
- 新增 `com.storyweaver.ai.application.support.StructuredJsonSupport`，统一 AI 严格 JSON 清洗与解析逻辑，并复用到人物属性生成链路。
- 新增 `com.storyweaver.item` 模块，按 `web / application / domain / infrastructure` 分层承接物品与背包相关后端能力。
- 扩展 `AIModelRoutingService`，支持 `item` 与 `naming` 入口的模型路由配置。
- 更新数据库初始化与测试环境配置，确保物品表、背包表及相关系统配置可以自动迁移，H2 测试环境可兼容 `user` 表名。

### Phase 3 - 角色与背包系统开发

- 新增 `item` 与 `character_inventory_item` 数据表及迁移脚本 `sql/010_item_inventory_module.sql`。
- 新增项目物品 CRUD、角色背包 CRUD、AI 物品生成、背包定向生成接口。
- 在角色实体与人物列表中增加背包统计摘要，包括物品总数、已装备数量和稀有物品数量。
- 新增前端物品 API、背包 API、Pinia store 和 `CharacterInventoryDialog`，支持：
  - 项目物品库维护
  - 角色背包查看与调整
  - 装备/卸下、数量变更、移出背包
  - AI 生成物品并写入物品库或指定角色背包
- 在人物管理页接入背包入口和摘要展示，形成完整前后端闭环。

### Verification

- 通过 `cd front && npm run type-check` 验证前端类型检查。
- 通过 `cd front && npm run build` 验证前端生产构建。
- 通过 `cd backend && mvn test` 验证后端测试，结果为 `Tests run: 35, Failures: 0, Errors: 0, Skipped: 0`。
- 前端构建仍存在现有的 chunk size warning，但不影响本次功能交付。
