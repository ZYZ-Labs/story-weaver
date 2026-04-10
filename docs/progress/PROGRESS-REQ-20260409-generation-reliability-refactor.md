# Story 生成可靠性、减法交互与结构化沉淀重构 进度记录

- Req ID: REQ-20260409-generation-reliability-refactor
- Status: In Progress
- Created At: 2026-04-09 Asia/Shanghai
- Updated At: 2026-04-10 Asia/Shanghai

## 当前快照

- Current Phase: 已完成主线收口，并进一步确认 `chat / generate` 的职责边界
- Current Task: 准备进入 Step 1，先固定最小作者模型、摘要建议包协议、章节锚点模型、进度预测结果模型，以及聊天采集到最终生成的交接规则
- Last Completed: 已根据 2026-04-10 的进一步讨论，把“按钮触发式进度预测”纳入专家 / 开发者模式范围，并确认正文主链不是持续会话式生成
- Next Action: 先落 `SummarySuggestionPack / ChapterAnchorBundle / GenerationReadinessVO / StoryProgressSuggestionVO` 的协议和服务骨架
- Next Action: 把“`chat` 采集澄清、`summary` 待确认、`generate` 最终成稿”的边界正式落实到接口与服务设计
- Blockers:
  - 真实 Provider 下总导仍返回无效 JSON，兼容问题尚未定位到具体协议细节
  - 需要在实现前确定摘要建议是复用现有聊天能力，还是独立做轻量整理助手
- Latest Verified:
  - 线上项目 `旧日王座` 的项目、世界观、大纲、人物、章节、剧情、因果数据已实际核对
  - 线上 `story.refactor.v1.*` 开关当前全部为 `false`
  - 线上项目 `旧日王座` 下共有 6 条 `ai_director_decision`，全部为 `fallback`
  - 6 条总导记录错误一致为 `总导层返回内容不是有效 JSON`
  - 线上章节存在明显锚点缺失：多个章节 `outlineId / mainPovCharacterId / requiredCharacterIds` 为空
  - 线上样本已出现主角命名漂移问题
  - 已归档 `REQ-20260408-ai-director-layer` 和 `REQ-20260409-core-module-refactor` 相关文档
  - 已归档本需求旧版计划 `PLAN-REQ-20260409-generation-reliability-refactor-v1`
  - 已形成新的 v2 计划，明确“作者前台做减法、后台做结构化沉淀”的方向
  - 已将“进度预测 / 节奏建议”纳入 v2 计划，作为专家 / 开发者模式下的按钮触发式建议能力
  - 已代码级确认：`/api/ai-writing/*` 主链是多阶段 `generate` 流水线，不是持续会话式正文生成
  - 已代码级确认：现有聊天子系统主要通过 `buildParticipationContext` 方式为生成链路提供背景摘要，而非直接复用实时会话
  - 已确认专家模式可以作为交互层级开关开放给普通作者主动启用
- Latest Unverified:
- 尚未对 DeepSeek 兼容协议做代码级修正验证
- 尚未实现摘要建议 / 确认写回 / readiness / anchor pack / observability 增强
- 尚未把工作流日志语义和真实执行模型对齐到前端展示

## 关键节点记录

### [2026-04-09 Asia/Shanghai] 完成线上样本诊断与下一轮重构立项

- 背景:
  - 用户要求直接查看已部署站点、真实数据库和 `旧日王座` 项目数据，基于真实样本而不是本地猜测给出系统判断，并沉淀下一轮重构文档。
- 本次完成:
  - 读取 `backend.env`，确认数据库与运行环境连接信息
  - 访问线上站点并通过 JWT 方式调用真实 API
  - 交叉核对项目、章节、世界观、大纲、剧情、因果、人物、总导记录和配置开关
  - 新建线上诊断报告
  - 新建本需求的 requirement / plan / progress 文档
  - 更新 `docs/agent-context.md`
- 修改文件:
  - `docs/reports/REPORT-20260409-old-throne-live-review.md`
  - `docs/requirements/REQ-20260409-generation-reliability-refactor.md`
  - `docs/plans/PLAN-REQ-20260409-generation-reliability-refactor-v1.md`
  - `docs/progress/PROGRESS-REQ-20260409-generation-reliability-refactor.md`
  - `docs/agent-context.md`
- 验证:
  - 已实际读取线上 API 和 MySQL 数据
  - 已确认 `旧日王座` 真实样本中的总导 fallback、结构开关关闭、章节锚点缺失和角色命名漂移
- 风险/遗留:
  - 当前仍是分析与文档阶段，尚未进入代码实现
  - 真实 Provider 的协议兼容问题需要后续代码和日志进一步定位
- 下一步:
  - 先落章节锚点解析、生成就绪度接口和章节页的 readiness 展示

### [2026-04-10 Asia/Shanghai] 归档旧主线并把计划升级为 v2

- 背景:
  - 用户确认后续只保留当前需求为唯一主线，并明确指出普通作家面对大量底层字段会困惑，系统需要做减法，更多通过对话整理后再结构化保存。
- 本次完成:
  - 归档 AI 总导层和 Story 核心模块重构相关 requirement / plan / progress 文档
  - 归档本需求旧版计划 `v1`
  - 重写 requirement，加入“减法交互、对话式采集、摘要确认后结构化沉淀”方向
  - 新建 `PLAN-REQ-20260409-generation-reliability-refactor-v2`
  - 更新 `docs/agent-context.md`
- 修改文件:
  - `docs/archive/requirements/REQ-20260408-ai-director-layer.md`
  - `docs/archive/plans/PLAN-REQ-20260408-ai-director-layer-v1.md`
  - `docs/archive/progress/PROGRESS-REQ-20260408-ai-director-layer.md`
  - `docs/archive/requirements/REQ-20260409-core-module-refactor.md`
  - `docs/archive/plans/PLAN-REQ-20260409-core-module-refactor-v1.md`
  - `docs/archive/plans/PLAN-REQ-20260409-core-module-refactor-mapping-v1.md`
  - `docs/archive/progress/PROGRESS-REQ-20260409-core-module-refactor.md`
  - `docs/archive/plans/PLAN-REQ-20260409-generation-reliability-refactor-v1.md`
  - `docs/requirements/REQ-20260409-generation-reliability-refactor.md`
  - `docs/plans/PLAN-REQ-20260409-generation-reliability-refactor-v2.md`
  - `docs/progress/PROGRESS-REQ-20260409-generation-reliability-refactor.md`
  - `docs/agent-context.md`
- 验证:
  - 已确认当前 docs 主线只剩本需求
  - 已完成新旧文档路径切换和计划升级
- 风险/遗留:
  - 现在仍是方案修订阶段，尚未开始代码实现
  - 对话式摘要流如何与现有聊天能力复用，仍需 Step 1 收敛
- 下一步:
  - 定义最小作者模型和摘要建议包协议，再进入实现阶段

### [2026-04-10 Asia/Shanghai] 将进度预测纳入新主线计划

- 背景:
  - 用户提出：在大纲、剧情、章节与因果已存在的前提下，系统其实更接近半开放式创作；因此在创建新大纲、剧情和章节时，应该能根据已有内容预测“当前更适合什么进度”。
- 本次完成:
  - 将“进度预测 / 节奏建议”纳入 requirement / plan
  - 明确该能力仅服务专家 / 开发者模式
  - 明确该能力是按钮触发式建议，不自动执行
- 修改文件:
  - `docs/requirements/REQ-20260409-generation-reliability-refactor.md`
  - `docs/plans/PLAN-REQ-20260409-generation-reliability-refactor-v2.md`
  - `docs/progress/PROGRESS-REQ-20260409-generation-reliability-refactor.md`
- 验证:
  - 已完成文档级边界收敛，未引入与“做减法”方向冲突的复杂自动化设定
- 风险/遗留:
  - 若预测逻辑做得过重，可能反向增加系统复杂度
- 下一步:
  - 在 Step 1 先定义 `StoryProgressSuggestionVO`，再决定规则优先还是模型优先

### [2026-04-10 Asia/Shanghai] 确认 `chat / generate` 边界与专家模式定位

- 背景:
  - 用户要求进一步确认：当前工作流日志看起来像单 session chat，但从结果导向看更像单次提交式生成，这会直接影响总导、上下文补救和后续重构方向判断。
- 本次完成:
  - 代码级检查 `AIWritingController / AIWritingServiceImpl / AIWritingChatServiceImpl / AIProviderServiceImpl`
  - 确认 `/api/ai-writing/generate` 与 `/generate-stream` 主链为：
    - `prepare -> plan -> write -> check -> revise`
    - 其内部是多次无状态 `generateText / streamText` 调用
  - 确认现有 `AIWritingChatServiceImpl` 是独立聊天子系统，主要用于多轮消息和背景整理
  - 确认聊天能力当前只通过 `buildParticipationContext` 摘要化参与正文生成
  - 将“`chat` 负责采集澄清，`generate` 负责最终成稿”写入 requirement / plan
  - 将“专家模式是普通作者也可主动开启的可选层级”写入 requirement / plan
- 修改文件:
  - `docs/requirements/REQ-20260409-generation-reliability-refactor.md`
  - `docs/plans/PLAN-REQ-20260409-generation-reliability-refactor-v2.md`
  - `docs/progress/PROGRESS-REQ-20260409-generation-reliability-refactor.md`
- 验证:
  - 已在代码中确认写作主链和聊天子系统并不是同一条执行语义
  - 已确认当前日志语义存在误导风险，需要在后续 UI 与记录模型中显式校正
- 风险/遗留:
  - 如果后续仍把聊天记录直接当正文生成输入，系统会继续保留“看似可补救，实则不可追踪”的不稳定性
- 下一步:
  - 在 Step 1 固定交接协议，在 Step 7 修正写作页和日志展示语义
