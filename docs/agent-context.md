# Agent Context

- Last Updated: 2026-04-10 Asia/Shanghai
- Current Primary Req: REQ-20260409-generation-reliability-refactor

## Active Requirements

| Priority | Req ID | Name | Status | Requirement Doc | Plan Doc | Progress Doc |
| --- | --- | --- | --- | --- | --- | --- |
| High | REQ-20260409-generation-reliability-refactor | Story 生成可靠性、减法交互与结构化沉淀重构 | In Progress | `docs/requirements/REQ-20260409-generation-reliability-refactor.md` | `docs/plans/PLAN-REQ-20260409-generation-reliability-refactor-v2.md` | `docs/progress/PROGRESS-REQ-20260409-generation-reliability-refactor.md` |

## Archived Requirements

| Req ID | Name | Final Status | Requirement Doc | Plan Doc | Progress Doc |
| --- | --- | --- | --- | --- | --- |
| REQ-20260409-core-module-refactor | Story 核心模块重构 | Archived | `docs/archive/requirements/REQ-20260409-core-module-refactor.md` | `docs/archive/plans/PLAN-REQ-20260409-core-module-refactor-v1.md` | `docs/archive/progress/PROGRESS-REQ-20260409-core-module-refactor.md` |
| REQ-20260408-ai-director-layer | AI 总导编排层 | Archived | `docs/archive/requirements/REQ-20260408-ai-director-layer.md` | `docs/archive/plans/PLAN-REQ-20260408-ai-director-layer-v1.md` | `docs/archive/progress/PROGRESS-REQ-20260408-ai-director-layer.md` |
| REQ-20260408-writing-workflow-ui-bug | AI 写作工作流修订展示缺陷 | Fixed | `docs/archive/requirements/REQ-20260408-writing-workflow-ui-bug.md` | `docs/archive/plans/PLAN-REQ-20260408-writing-workflow-ui-bug-v1.md` | `docs/archive/progress/PROGRESS-REQ-20260408-writing-workflow-ui-bug.md` |

## Current Next Action

- 以 `docs/reports/REPORT-20260409-old-throne-live-review.md` 为问题基线，以 `PLAN-REQ-20260409-generation-reliability-refactor-v2` 为唯一主线继续推进。
- 下一步先固定最小作者模型、摘要建议包协议与章节锚点模型，避免一边实现一边重新讨论前台到底该暴露什么。
- 所有后续改动都优先服务于“做减法、对话式采集、摘要确认后结构化沉淀、提高生成可靠性”，而不是继续平行扩模块。
- 当前已确认正文生成主链是多阶段 `generate` 流水线，而不是持续会话式 chat；后续实现应坚持“`chat` 采集澄清，`generate` 最终成稿”的边界。

## Current Blockers

- 线上项目 `旧日王座` 的 6 条 `ai_director_decision` 全部为 `fallback`，错误一致为 `总导层返回内容不是有效 JSON`，说明真实 Provider 兼容性未闭环。
- 线上 `story.refactor.v1.*` 开关仍全部为 `false`，历史结构改造尚未真正成为主读路径。
- 真实项目章节存在锚点缺失：多个章节无 `outline_id` / `main_pov_character_id` / `chapter_character` 绑定，已直接影响生成一致性。
- 当前需要先确定摘要建议流是复用现有聊天能力，还是独立做轻量整理助手，否则后续接口边界容易反复变动。
- 当前仍需决定摘要建议入口是复用现有聊天 UI，还是独立做轻量整理助手；但架构边界已经明确，二者都不能直接替代最终正文生成主链。

## Resume Reading Order

1. 先读取当前主需求对应的需求文档。
2. 再读取对应的 `PLAN-REQ-20260409-generation-reliability-refactor-v2.md`。
3. 再读取对应进度文档的“当前快照”和最近关键节点。
4. 补读 `docs/reports/REPORT-20260409-old-throne-live-review.md`，确认真实样本下暴露的问题画像。
5. 最后再进入代码和测试文件。

## Handoff Notes

- 该文件是固定入口，恢复或切换智能体时必须优先阅读。
- 当前协作边界已明确：智能体负责代码开发、文档、数据库结构 / 迁移调整建议；部署执行、环境联调和真实数据测试由用户负责。
- 旧主线 `REQ-20260408-ai-director-layer` 和 `REQ-20260409-core-module-refactor` 已归档，其已落地成果继续保留为当前主线的底层基础。
- 当前主线已从“继续增加结构和模块”切换为“普通作者前台做减法，后台通过摘要确认流沉淀结构化数据”。
- 专家模式被视为交互层级开关，不是权限人群隔离；普通作者也可主动开启。
- 当前首个真实验收样本仍为项目 `旧日王座`（`project.id = 28`）。
- 该样本已确认存在：总导 100% fallback、章节锚点缺失、人物命名漂移、结构开关未切到主读路径等问题。
- 本需求的旧版计划 `PLAN-REQ-20260409-generation-reliability-refactor-v1.md` 已归档，当前有效计划为 `v2`。
