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
- Step 8 的 inspector 已落地：项目级一致性报告接口已可用，下一步是在部署后对 `旧日王座` 跑真实回归。
- 所有后续改动都优先服务于“做减法、对话式采集、摘要确认后结构化沉淀、提高生成可靠性”，而不是继续平行扩模块。
- 当前已确认正文生成主链是多阶段 `generate` 流水线，而不是持续会话式 chat；后续实现应坚持“`chat` 采集澄清，`generate` 最终成稿”的边界。

## Current Blockers

- `旧日王座` 的历史总导记录显示真实 Provider 曾稳定触发 `总导层返回内容不是有效 JSON`；代码级兼容已补，但尚未部署回归。
- 线上 `story.refactor.v1.*` 开关仍全部为 `false`，历史结构改造尚未真正成为主读路径。
- 真实项目章节存在锚点缺失：多个章节无 `outline_id` / `main_pov_character_id` / `chapter_character` 绑定，已直接影响生成一致性。
- `com.storyweaver.story.generation` 已存在基础协议与服务骨架，后续新增接口应优先复用这层，而不是把逻辑直接散落回 controller 或 `AIWritingServiceImpl`。
- 新的后端接口已存在但前端尚未接入，包括摘要建议、确认写回、进度预测、章节 readiness 与 anchors。
- `StoryConsistencyInspector` 已实现，但 `旧日王座` 还没有跑新的端到端回归。
- inspector 目前只有后端接口 `/api/projects/{projectId}/story-consistency`，还没有独立前端入口。

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
- Step 6 已完成代码级兼容治理：总导成功统一为 `tool_success`，fallback 保留真实 tool trace，并在前端暴露 failure reason。
- Step 7 已完成代码级接入：写作记录新增 `generation_trace_json`，写作 prompt 显式带入 readiness 与 anchor snapshot，前端日志统一改为“生成流水线”语义。
- Step 8 已完成检查器后端落地：可按项目查看人物命名漂移风险、POV 缺失、story beat 缺失、总导 fallback 和 generation trace 完整度。
