# Story 生成可靠性与故事锚点重构 进度记录

- Req ID: REQ-20260409-generation-reliability-refactor
- Status: Planned
- Created At: 2026-04-09 Asia/Shanghai
- Updated At: 2026-04-09 Asia/Shanghai

## 当前快照

- Current Phase: 已完成线上真实样本诊断与下一轮重构文档化
- Current Task: 等待进入 Step 1，先落章节锚点解析与生成就绪度
- Last Completed: 已完成 `旧日王座` 线上项目、数据库与 API 的交叉排查，并形成正式报告与 v1 计划
- Next Action: 从 `ChapterAnchorResolver / GenerationReadinessService` 开始实现第一批支撑能力
- Blockers:
  - 真实 Provider 下总导返回内容不是有效 JSON，兼容问题尚未定位到具体协议细节
  - 真实样本项目当前章节锚点缺失较多，进入实现前需明确第一阶段采用 warning 还是 hard block
- Latest Verified:
  - 线上项目 `旧日王座` 的项目、世界观、大纲、人物、章节、剧情、因果数据已实际核对
  - 线上 `story.refactor.v1.*` 开关当前全部为 `false`
  - 线上项目 `旧日王座` 下共有 6 条 `ai_director_decision`，全部为 `fallback`
  - 6 条总导记录错误一致为 `总导层返回内容不是有效 JSON`
  - 线上章节存在明显锚点缺失：多个章节 `outlineId / mainPovCharacterId / requiredCharacterIds` 为空
  - 线上样本已出现主角命名漂移问题
  - 已新增线上诊断报告和本需求的 requirement / plan / progress 文档
- Latest Unverified:
  - 尚未对 DeepSeek 兼容协议做代码级修正验证
  - 尚未实现 readiness / anchor pack / observability 增强

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
