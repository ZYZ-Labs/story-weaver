# Agent Context

- Last Updated: 2026-04-11 Asia/Shanghai
- Current Primary Req: REQ-20260411-stateful-story-platform-upgrade

## Active Requirements

| Priority | Req ID | Name | Status | Requirement Doc | Plan Doc | Progress Doc |
| --- | --- | --- | --- | --- | --- | --- |
| High | REQ-20260411-stateful-story-platform-upgrade | Story 平台级架构升级：Summary First、StoryUnit、MCP/LSP 与多 Session 编排 | In Progress | `docs/requirements/REQ-20260411-stateful-story-platform-upgrade.md` | `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-v1.md` | `docs/progress/PROGRESS-REQ-20260411-stateful-story-platform-upgrade.md` |

## Archived Requirements

| Req ID | Name | Final Status | Requirement Doc | Plan Doc | Progress Doc |
| --- | --- | --- | --- | --- | --- |
| REQ-20260409-generation-reliability-refactor | Story 生成可靠性、减法交互与结构化沉淀重构 | Archived | `docs/archive/requirements/REQ-20260409-generation-reliability-refactor.md` | `docs/archive/plans/PLAN-REQ-20260409-generation-reliability-refactor-v2.md` | `docs/archive/progress/PROGRESS-REQ-20260409-generation-reliability-refactor.md` |
| REQ-20260409-core-module-refactor | Story 核心模块重构 | Archived | `docs/archive/requirements/REQ-20260409-core-module-refactor.md` | `docs/archive/plans/PLAN-REQ-20260409-core-module-refactor-v1.md` | `docs/archive/progress/PROGRESS-REQ-20260409-core-module-refactor.md` |
| REQ-20260408-ai-director-layer | AI 总导编排层 | Archived | `docs/archive/requirements/REQ-20260408-ai-director-layer.md` | `docs/archive/plans/PLAN-REQ-20260408-ai-director-layer-v1.md` | `docs/archive/progress/PROGRESS-REQ-20260408-ai-director-layer.md` |
| REQ-20260408-writing-workflow-ui-bug | AI 写作工作流修订展示缺陷 | Fixed | `docs/archive/requirements/REQ-20260408-writing-workflow-ui-bug.md` | `docs/archive/plans/PLAN-REQ-20260408-writing-workflow-ui-bug-v1.md` | `docs/archive/progress/PROGRESS-REQ-20260408-writing-workflow-ui-bug.md` |

## Current Next Action

- 新主线已切换到 `REQ-20260411-stateful-story-platform-upgrade`，后续优先完成平台级架构文档与实施拆分。
- 当前第一优先级不再是继续修旧总导 prompt，而是建立：
  - `StoryUnit + Facets`
  - 模块化单体边界
  - `MCP / LSP`
  - 多 session 写作管线
  - 前端信息架构
  - 文档治理制度
- `REQ-20260409-generation-reliability-refactor` 已归档，但其 report 仍保留为当前代码稳定性与线上样本基线。
- 第一份详细实施计划已创建：
  - `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-phase1-foundation-v1.md`
- 第二份详细实施计划已创建：
  - `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-phase1-modularization-v1.md`
- 当前阶段口径已确认：
  - `Phase 1A`：包先细分、协议先行
  - `Phase 1B`：尽早进入粗粒度模块拆分
- `Phase 1A` 首批协议骨架已迁移到：
  - `story-storyunit/src/main/java/com/storyweaver/storyunit/*`
- `Phase 1B` 首轮粗粒度模块拆分已完成：
  - 根 `pom.xml`
  - `story-domain`
  - `story-storyunit`
  - `backend` 保持应用壳
- 当前统一构建入口应使用根工程：
  - `mvn -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -DskipTests compile`
- 当前下一步应继续完成：
  - 继续留在 `Phase 1B`
  - 完成 `1B.2`
    - 清理活动文档中的旧构建入口描述
    - 清理活动文档中的旧目录口径
  - 完成 `1B.3`
    - 冻结 `story-generation / story-provider / story-web / story-infra` 的粗边界
    - 明确继续临时留在 `backend` 的包

## Current Blockers

- 新主线已完成 `Phase 1B` 的 `1B.1`，但 `1B.2 / 1B.3` 仍未完成，当前仍处于 `Phase 1B`。
- `StoryUnit` 已完成第一批协议、代码骨架与模块迁移，但尚未完成存储映射与 service/repository 实装。
- `MCP / LSP` 边界已明确，但还没有形成服务层和接口层实现。
- 前端仍是旧信息架构，尚未切到工作台模式。
- 旧主线中的真实 provider fallback 问题仍存在，后续需要作为平台升级前的稳定性基线继续跟踪。

## Resume Reading Order

1. 先读取 `docs/requirements/REQ-20260411-stateful-story-platform-upgrade.md`。
2. 再读取 `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-v1.md`。
3. 再读取 `docs/progress/PROGRESS-REQ-20260411-stateful-story-platform-upgrade.md`。
4. 再读取本轮详细实施计划：
   - `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-phase1-foundation-v1.md`
   - `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-phase1-modularization-v1.md`
5. 再读取：
   - `docs/architecture/ARCH-REQ-20260411-story-unit-and-facets-v1.md`
   - `docs/architecture/ARCH-REQ-20260411-module-boundaries-and-mcp-lsp-v1.md`
   - `docs/architecture/ARCH-REQ-20260411-writing-pipeline-and-scene-execution-v1.md`
   - `docs/architecture/ARCH-REQ-20260411-frontend-information-architecture-v1.md`
   - `docs/governance/GOV-REQ-20260411-documentation-and-planning-workflow-v1.md`
6. 如果需要回到旧代码链路，再补读 `docs/archive/requirements/REQ-20260409-generation-reliability-refactor.md` 及其 report。
7. 最后再进入代码和测试文件。

## Handoff Notes

- 该文件是固定入口，恢复或切换智能体时必须优先阅读。
- 当前协作边界已升级为平台工程协作：
  - 智能体负责架构文档、实施计划、代码开发与迁移建议
  - 用户负责方向确认、部署执行和真实环境联调
- 新主线不是旧主线的简单延长，而是平台级升级。
- 旧主线 `REQ-20260409-generation-reliability-refactor` 已归档，但仍应被视为：
  - 当前代码基线
  - 真实样本基线
  - 新平台升级前的稳定性参考线
- 新主线的硬原则已固定：
  - `Summary First`
  - `StoryUnit + Facets`
  - `MCP / LSP`
  - 四 session 编排
  - 章节骨架 + 镜头执行
  - 每次开发前必须拆详细计划
