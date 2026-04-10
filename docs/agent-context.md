# Agent Context

- Last Updated: 2026-04-09 Asia/Shanghai
- Current Primary Req: REQ-20260409-generation-reliability-refactor

## Active Requirements

| Priority | Req ID | Name | Status | Requirement Doc | Plan Doc | Progress Doc |
| --- | --- | --- | --- | --- | --- | --- |
| High | REQ-20260409-generation-reliability-refactor | Story 生成可靠性与故事锚点重构 | Planned | `docs/requirements/REQ-20260409-generation-reliability-refactor.md` | `docs/plans/PLAN-REQ-20260409-generation-reliability-refactor-v1.md` | `docs/progress/PROGRESS-REQ-20260409-generation-reliability-refactor.md` |
| High | REQ-20260409-core-module-refactor | Story 核心模块重构 | In Progress | `docs/requirements/REQ-20260409-core-module-refactor.md` | `docs/plans/PLAN-REQ-20260409-core-module-refactor-v1.md` | `docs/progress/PROGRESS-REQ-20260409-core-module-refactor.md` |
| High | REQ-20260408-ai-director-layer | AI 总导编排层 | In Progress | `docs/requirements/REQ-20260408-ai-director-layer.md` | `docs/plans/PLAN-REQ-20260408-ai-director-layer-v1.md` | `docs/progress/PROGRESS-REQ-20260408-ai-director-layer.md` |

## Archived Requirements

| Req ID | Name | Final Status | Requirement Doc | Plan Doc | Progress Doc |
| --- | --- | --- | --- | --- | --- |
| REQ-20260408-writing-workflow-ui-bug | AI 写作工作流修订展示缺陷 | Fixed | `docs/archive/requirements/REQ-20260408-writing-workflow-ui-bug.md` | `docs/archive/plans/PLAN-REQ-20260408-writing-workflow-ui-bug-v1.md` | `docs/archive/progress/PROGRESS-REQ-20260408-writing-workflow-ui-bug.md` |

## Current Next Action

- 先以 `docs/reports/REPORT-20260409-old-throne-live-review.md` 为准，围绕 `旧日王座` 真实样本确认下一轮重构边界。
- 新主线优先收口生成可靠性：总导兼容、章节锚点、生成前校验、chapter brief 预览与可观测性，而不是继续扩大模块数量。
- 现有核心模块重构与 AI 总导层仍保留进行中状态，后续改动应优先服务于新主线需求，而不是各自继续平行演进。

## Current Blockers

- 线上项目 `旧日王座` 的 6 条 `ai_director_decision` 全部为 `fallback`，错误一致为 `总导层返回内容不是有效 JSON`，说明真实 Provider 兼容性未闭环。
- 线上 `story.refactor.v1.*` 开关仍全部为 `false`，新结构尚未真正成为主读路径。
- 真实项目章节存在锚点缺失：多个章节无 `outline_id` / `main_pov_character_id` / `chapter_character` 绑定，已直接影响生成一致性。
- `ai_writing_record` 在项目 28 下为 `0`，当前对“生成来源 -> 章节内容 -> 总导决策”的追踪链不完整。

## Resume Reading Order

1. 先读取当前主需求对应的需求文档。
2. 再读取对应计划文档。
3. 再读取对应进度文档的“当前快照”和最近关键节点。
4. 补读 `docs/reports/REPORT-20260409-old-throne-live-review.md`，确认真实样本下暴露的问题画像。
5. 最后再进入代码和测试文件。

## Handoff Notes

- 该文件是固定入口，恢复或切换智能体时必须优先阅读。
- bug 修复已提交到 git，提交号为 `b9c797d`。
- 当前协作边界已明确：智能体负责代码开发与数据库结构 / 迁移调整；部署执行、环境联调和真实数据测试由用户负责。
- 当前已基于线上真实样本新增新主线需求 `REQ-20260409-generation-reliability-refactor`，其目标是先解决生成可靠性和故事锚点问题。
- 线上诊断报告已落盘到 `docs/reports/REPORT-20260409-old-throne-live-review.md`，首个真实验收样本为项目 `旧日王座`（`project.id = 28`）。
- 该样本已确认存在：总导 100% fallback、重构开关未开启、章节锚点缺失、人物命名漂移、`ai_writing_record` 追踪缺失。
- 当前已把 `docs/story_weaver_module_refactor_prd_v_1.md` 上升为正式需求 `REQ-20260409-core-module-refactor`，并产出 v1 实施计划。
- Step 0 已新增字段映射文档 `docs/plans/PLAN-REQ-20260409-core-module-refactor-mapping-v1.md` 和 `sql/012/013/014` 三个迁移脚本骨架。
- Step 1 已基本完成核心后端结构改造：Outline / Chapter / Plot / Causality / Character 的主要字段、兼容读写和最小数据库初始化补齐已经落地。
- Step 1 已完成前端主要页面兼容改造：`OutlineView / ChapterListView / CharacterListView / PlotView / CausalityView` 已接入新字段并通过 `npm run build`。
- Step 2 已完成第一批 AI 读取链路适配：`AIWritingServiceImpl / AIDirectorApplicationServiceImpl / DirectorToolExecutor` 已开始优先读取 `outlineId / storyBeat / POV / roleType / relatedWorldSettingIds` 等新结构。
- AI 总导编排层已完成第一批后端骨架、写作主链路接入、真实 tool calling 接入、设置页 director 配置入口，以及写作中心/章节页的总导摘要与调试日志展示；当前主要遗留是真实 Provider 联调。
