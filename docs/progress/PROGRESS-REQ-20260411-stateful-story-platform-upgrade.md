# Story 平台级架构升级 进度记录

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Status: In Progress
- Created At: 2026-04-11 Asia/Shanghai
- Updated At: 2026-04-11 Asia/Shanghai

## 当前快照

- Current Phase: 文档建制与术语冻结
- Current Task: 完成平台级主需求、总计划、元模型、前端架构、MCP/LSP 协议和文档治理规则
- Last Completed: 已确认本次升级不是单功能重构，而是平台级架构升级
- Next Action: 补齐其余核心架构文档，并更新 `agent-context.md`
- Blockers:
  - 旧主线 `REQ-20260409-generation-reliability-refactor` 已归档，但其代码成果和回归报告仍需作为迁移基线继续参考
  - `MCP` 与 `LSP` 的边界尚未形成代码级实现，只完成讨论与文档收敛
  - 前端现有页面结构仍是旧工作流，尚未切到新信息架构
- Latest Verified:
  - 已确认用户界面默认只展示摘要是新的硬原则
  - 已确认结构化字段主要服务于 MCP/LSP、编排层和状态机
  - 已确认不采用“万能基类”，而采用 `StoryUnit + Facets` 协议壳
  - 已确认后续采用四 session：
    - 总导
    - 选择器
    - 写手
    - 审校器
  - 已确认章节执行升级为“章节骨架 + 镜头执行 + 状态交接”
  - 已确认后续开发前必须先拆详细计划文档
- Latest Unverified:
  - `StoryUnit` 的存储落地方式
  - `MCP/LSP` 的实际运行形态
  - 模块化单体的首轮拆分方案是否会影响现有主链
  - 前端信息架构在真实页面中的可用性

## 关键节点记录

### [2026-04-11 Asia/Shanghai] 确认平台升级方向并启动文档建制

- 背景:
  - 用户明确指出，后续系统会承载世界观增量、背包、技能、态度、好感、世界影响等复杂状态，不再满足于 MVP 级单链路写作系统。
- 本次完成:
  - 确认本轮升级应定性为平台级架构升级
  - 确认采用 `Summary First`
  - 确认采用 `StoryUnit + Facets`
  - 确认采用模块化单体
  - 确认采用 `MCP/LSP + 四 session 编排 + 镜头级写作`
  - 启动新版 requirement / plan / progress 文档编写
- 修改文件:
  - `docs/requirements/REQ-20260411-stateful-story-platform-upgrade.md`
  - `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-v1.md`
  - `docs/progress/PROGRESS-REQ-20260411-stateful-story-platform-upgrade.md`
- 风险/遗留:
  - 旧主线已归档，但新旧主线衔接方式还需要在后续文档中明确
- 下一步:
  - 完成核心架构文档并更新 `agent-context.md`

## 贡献与署名说明

- 平台升级方向和问题提出：用户。
- 进度文档结构、术语收敛与记录整理：Codex。
- 当前结论由用户与 Codex 共同讨论形成。
