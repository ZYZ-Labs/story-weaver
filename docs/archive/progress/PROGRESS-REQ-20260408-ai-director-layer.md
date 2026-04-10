# AI 总导编排层 进度记录

- Req ID: REQ-20260408-ai-director-layer
- Status: Archived
- Created At: 2026-04-08 Asia/Shanghai
- Updated At: 2026-04-10 Asia/Shanghai

## 当前快照

- Current Phase: 已归档，相关实现保留但不再作为当前主线推进
- Current Task: 无
- Last Completed: 已完成总导层主链路接入、配置接入和可视化展示
- Next Action: 如需继续治理真实 Provider 兼容性，统一并入 `REQ-20260409-generation-reliability-refactor`
- Blockers:
  - 决策层工具调用当前仅支持兼容 `/v1/chat/completions` 的 Provider，真实联调结果仍未知
- Latest Verified:
  - 已实现 `AIProviderService.generateTextWithTools(...)`，支持基于兼容 chat completions 的工具循环
  - 已实现总导工具执行器，覆盖章节快照、大纲、剧情、因果、世界观、人物、背包、知识、背景聊天摘要
  - 已实现总导层模型决策解析与失败回退启发式 fallback
  - 已补前端系统设置中的 director Provider / Model / 开关 / 限制项配置
  - 已补 `GET /api/ai-director/{decisionId}`，前端可按生成记录精确读取对应总导决策
  - 已补写作中心与章节页的总导摘要卡片，展示阶段、模式、模块、约束和工具调用调试信息
  - 已补流式工作流日志中的 `director` 阶段映射，并在开启调试开关时输出总导概览、模块与工具调用日志
  - 已执行 `mvn -f backend/pom.xml -DskipTests compile`，后端编译通过
  - 已执行 `npm run build`，前端构建通过
- Latest Unverified:
  - 尚未验证真实 Provider 返回的 tool call schema 是否与当前兼容实现完全一致

### [2026-04-10 Asia/Shanghai] 归档 AI 总导层主线文档
- 背景:
  - 用户要求将其余 requirement / plan / progress 文档归档，后续以“生成可靠性与故事锚点重构”为唯一主线继续推进。
- 本次完成:
  - 归档 AI 总导层 requirement / plan / progress 文档
  - 保留已实现能力，但不再以“继续扩展总导层”为主线推进
- 修改文件:
  - `docs/archive/requirements/REQ-20260408-ai-director-layer.md`
  - `docs/archive/plans/PLAN-REQ-20260408-ai-director-layer-v1.md`
  - `docs/archive/progress/PROGRESS-REQ-20260408-ai-director-layer.md`
- 验证:
  - 已确认线上真实样本的主要问题已从“是否有总导”转为“生成可靠性与故事锚点不足”
- 风险/遗留:
  - 真实 Provider 的 tool-calling 兼容问题仍存在，但后续按新主线统一治理
- 下一步:
  - 由 `REQ-20260409-generation-reliability-refactor` 接管相关遗留问题

## 关键节点记录

### [2026-04-08 Asia/Shanghai] 完成需求与问题定位文档
- 背景:
  - 项目现有模块较多，但写作链路仍主要依赖固定规则拼接 prompt，缺少独立编排层。
  - 同时发现 AI 写作工作流中的“检查/修订”前端展示语义不完整，需要先文档化。
- 本次完成:
  - 新建 `REQ-20260408-ai-director-layer`
  - 新建 `REQ-20260408-writing-workflow-ui-bug`
  - 更新 `docs/agent-context.md`
- 修改文件:
  - `docs/requirements/REQ-20260408-ai-director-layer.md`
  - `docs/requirements/REQ-20260408-writing-workflow-ui-bug.md`
  - `docs/agent-context.md`
- 验证:
  - 已对照现有代码确认写作工作流、聊天提炼和流事件行为
- 风险/遗留:
  - 当时尚未形成正式实施计划，部分接口和数据结构仍未收敛
- 下一步:
  - 输出正式计划文档，固定阶段枚举、工具清单和 `decision pack`

### [2026-04-08 Asia/Shanghai] 完成 v1 实施计划
- 背景:
  - 用户要求先把文档补完整，再开始实际代码工作。
- 本次完成:
  - 输出 `PLAN-REQ-20260408-ai-director-layer-v1`
  - 将需求状态从 `Discussing` 更新为 `Planned`
  - 明确决策层插入现有写作链路但不替换 chat 式编写
- 修改文件:
  - `docs/requirements/REQ-20260408-ai-director-layer.md`
  - `docs/plans/PLAN-REQ-20260408-ai-director-layer-v1.md`
  - `docs/progress/PROGRESS-REQ-20260408-ai-director-layer.md`
- 验证:
  - 已核对 `AIWritingServiceImpl`、`AIWritingChatServiceImpl`、`AIProviderServiceImpl`、系统设置页和模型服务页当前结构
  - 已确保计划中的接口、配置项和文件路径与现有仓库结构兼容
- 风险/遗留:
  - v1 工具调用兼容性仍依赖具体 provider 对 `/v1/chat/completions` 工具协议的支持
  - “最新决策摘要”前端是否首版交付，仍可在实现时裁剪
- 下一步:
  - 从 SQL 迁移和后端实体层开始实现最小骨架

### [2026-04-08 Asia/Shanghai] 完成总导层第一批后端骨架
- 背景:
  - bug 修复已经提交，需要切回总导层主线并开始实际功能实现。
- 本次完成:
  - 新增 `sql/011_ai_director_layer.sql`
  - 新增 `AIDirectorDecision` 实体、Mapper、DTO、VO
  - 新增 `AIDirectorApplicationService`、`AIDirectorController`
  - 新增 `DirectorModuleRegistry` 和 `DirectorDecisionPackAssembler`
  - 扩展系统配置、模型路由和数据库增量初始化，加入 director 相关配置与表结构
  - 为 `AIWritingRecord` / `AIWritingResponseVO` 增加 `directorDecisionId` 字段
- 修改文件:
  - `sql/011_ai_director_layer.sql`
  - `backend/src/main/java/com/storyweaver/domain/entity/AIDirectorDecision.java`
  - `backend/src/main/java/com/storyweaver/repository/AIDirectorDecisionMapper.java`
  - `backend/src/main/java/com/storyweaver/domain/dto/AIDirectorDecisionRequestDTO.java`
  - `backend/src/main/java/com/storyweaver/domain/vo/AIDirectorDecisionVO.java`
  - `backend/src/main/java/com/storyweaver/ai/director/application/AIDirectorApplicationService.java`
  - `backend/src/main/java/com/storyweaver/ai/director/application/DirectorModuleRegistry.java`
  - `backend/src/main/java/com/storyweaver/ai/director/application/DirectorDecisionPackAssembler.java`
  - `backend/src/main/java/com/storyweaver/ai/director/application/impl/AIDirectorApplicationServiceImpl.java`
  - `backend/src/main/java/com/storyweaver/controller/AIDirectorController.java`
  - `backend/src/main/java/com/storyweaver/service/AIModelRoutingService.java`
  - `backend/src/main/java/com/storyweaver/service/impl/SystemConfigServiceImpl.java`
  - `backend/src/main/java/com/storyweaver/config/DatabaseMigrationInitializer.java`
  - `backend/src/main/java/com/storyweaver/domain/entity/AIWritingRecord.java`
  - `backend/src/main/java/com/storyweaver/domain/vo/AIWritingResponseVO.java`
- 验证:
  - 执行 `mvn -f backend/pom.xml -DskipTests compile` 成功
- 风险/遗留:
  - 当前 `decision pack` 仍使用后端启发式规则生成，真实 tool calling 还未接入
  - 写作接口尚未真正消费总导决策结果
- 下一步:
  - 把总导层接入 `AIWritingServiceImpl.prepareGeneration(...)`

### [2026-04-08 Asia/Shanghai] 完成写作主链路接入
- 背景:
  - 第一批骨架完成后，需要验证总导决策不是孤立接口，而是真正进入写作生成链路。
- 本次完成:
  - `AIWritingServiceImpl` 在生成前会先调用总导决策服务
  - `buildUserPrompt(...)` 开始消费总导决策中的阶段、摘要、硬约束、禁止事项和写作提示
  - 上下文段落改为按已选模块决定是否拼入 prompt
  - 生成记录开始写入 `directorDecisionId`
- 修改文件:
  - `backend/src/main/java/com/storyweaver/service/impl/AIWritingServiceImpl.java`
- 验证:
  - 再次执行 `mvn -f backend/pom.xml -DskipTests compile` 成功
- 风险/遗留:
  - 当前模块选择仍基于后端启发式规则，不是真实 tool calling 结果
  - 目前还没有把 director 配置暴露到前端设置页
- 下一步:
  - 继续实现 provider 级 tool calling 协议和前端配置接入

### [2026-04-08 Asia/Shanghai] 完成真实 tool calling 与前端 director 配置入口
- 背景:
  - 写作主链路已接入总导服务，但此前总导决策仍是启发式规则拼装，尚未真正使用工具调用。
- 本次完成:
  - 为 `AIProviderService` / `AIProviderServiceImpl` 补齐非流式 tool calling 协议
  - 新增 `DirectorToolExecutor` 和 `DirectorToolDefinition`，落地 9 个总导工具白名单
  - `AIDirectorApplicationServiceImpl` 改为优先调用工具并解析模型返回的结构化决策；失败时退回启发式 fallback
  - `DirectorDecisionPackAssembler` 改为同时支持模型决策与 fallback 的统一装配
  - 前端系统设置页新增 director Provider / Model / 开关 / 最大工具调用次数 / 最大选中模块数 / 调试开关
  - 前端新增 `ai-director` API 和对应类型定义
- 修改文件:
  - `backend/src/main/java/com/storyweaver/service/AIProviderService.java`
  - `backend/src/main/java/com/storyweaver/service/impl/AIProviderServiceImpl.java`
  - `backend/src/main/java/com/storyweaver/ai/director/application/DirectorDecisionPackAssembler.java`
  - `backend/src/main/java/com/storyweaver/ai/director/application/impl/AIDirectorApplicationServiceImpl.java`
  - `backend/src/main/java/com/storyweaver/ai/director/application/tool/DirectorToolDefinition.java`
  - `backend/src/main/java/com/storyweaver/ai/director/application/tool/DirectorToolExecutor.java`
  - `front/src/views/settings/SettingsView.vue`
  - `front/src/types/index.ts`
  - `front/src/api/ai-director.ts`
- 验证:
  - 执行 `mvn -f backend/pom.xml -DskipTests compile` 成功
  - 执行 `npm run build` 成功
- 风险/遗留:
  - 当前只支持兼容 `/v1/chat/completions` 工具协议的 Provider
  - `getChatBackgroundSummary` 仍复用现有聊天提炼链路，真实成本和效果还需要联调验证
  - 写作页尚未直接展示最新总导决策摘要
- 下一步:
  - 用真实兼容 Provider 做一次端到端决策验证
  - 评估是否补写作页只读决策摘要

### [2026-04-09 Asia/Shanghai] 完成总导决策摘要与调试日志展示收尾
- 背景:
  - 前一阶段已经完成总导层真实 tool calling 和设置页配置，但写作页仍缺少对总导决策的只读展示，调试信息也只能从数据库或接口侧查看。
- 本次完成:
  - 新增 `GET /api/ai-director/{decisionId}`，支持按生成记录中的 `directorDecisionId` 精确查询决策详情
  - 为 `AIDirectorDecisionVO` 暴露 `toolTrace`，前端可直接展示工具调用参数与结果摘要
  - 新增前端总导摘要卡片，并接入写作中心与章节页两个写作入口
  - 在流式日志面板补齐 `director` 阶段语义，生成过程中可看到总导概览、约束、模块和工具调用日志
- 修改文件:
  - `backend/src/main/java/com/storyweaver/ai/director/application/AIDirectorApplicationService.java`
  - `backend/src/main/java/com/storyweaver/ai/director/application/impl/AIDirectorApplicationServiceImpl.java`
  - `backend/src/main/java/com/storyweaver/controller/AIDirectorController.java`
  - `backend/src/main/java/com/storyweaver/repository/AIDirectorDecisionMapper.java`
  - `backend/src/main/java/com/storyweaver/domain/vo/AIDirectorDecisionVO.java`
  - `backend/src/main/java/com/storyweaver/service/impl/AIWritingServiceImpl.java`
  - `front/src/components/AIDirectorDecisionCard.vue`
  - `front/src/components/AIProcessLogPanel.vue`
  - `front/src/views/writing/WritingView.vue`
  - `front/src/views/chapter/ChapterListView.vue`
  - `front/src/api/ai-director.ts`
  - `front/src/api/ai-writing.ts`
  - `front/src/types/index.ts`
- 验证:
  - 执行 `mvn -f backend/pom.xml -DskipTests compile` 成功
  - 执行 `npm run build` 成功
- 风险/遗留:
  - 前端展示已可用，但真实 Provider 是否稳定返回当前兼容的 tool call 结构仍需联调确认
  - 调试日志默认受 `ai.director.debug_expose_decision` 控制，未开启时主要依赖只读摘要卡片查看结果
- 下一步:
  - 用真实兼容 Provider 做一次端到端决策验证
