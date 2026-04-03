# Changelog

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
