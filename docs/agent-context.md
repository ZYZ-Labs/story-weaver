# Agent Context

- Last Updated: 2026-04-13 Asia/Shanghai
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
- 第三份详细实施计划已创建：
  - `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-phase2-storyunit-storage-v1.md`
- 第四份详细实施计划已创建：
  - `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-phase3-summary-first-workflow-v1.md`
- 当前阶段口径已确认：
  - `Phase 1A`：包先细分、协议先行
  - `Phase 1B`：尽早进入粗粒度模块拆分
  - `Phase 2`：projection-first 的 StoryUnit 存储映射与服务协议落地
- `Phase 1A` 首批协议骨架已迁移到：
  - `story-storyunit/src/main/java/com/storyweaver/storyunit/*`
- `Phase 1B` 首轮粗粒度模块拆分已完成：
  - 根 `pom.xml`
  - `story-domain`
  - `story-storyunit`
  - `backend` 保持应用壳
- `Phase 1B` 已继续推进到 `1B.3`：
  - `story-generation` 已建立
  - generation 顶层合同层已迁移
  - `generation.impl` 暂留 `backend`
  - `story-provider` 已建立
  - `AIProviderService` 与 `ProviderDiscoveryVO` 已迁移
  - `AIProviderServiceImpl / AIModelRoutingService` 暂留 `backend`
  - `story-web` 已建立
  - `ApiResponse / ApiErrorResponse` 已迁移
  - controller / handler 暂留 `backend`
  - `story-infra` 已建立
  - `repository.* / item mapper` 已迁移
  - config / security / mapper resources 暂留 `backend`
- 当前阶段状态：
  - `Phase 2.1` 已完成
  - `Phase 2.2` 核心实现已完成
  - `Phase 2.3` 已完成
  - `Phase 2` 已完成
  - `Phase 3.1` 已完成
  - `Phase 3.2` 已启动并完成首轮可编译骨架
  - `Phase 3.2` 已补 proposal 持久化回退层与最小回归
- `Phase 3.2` 已补配置外置与异常口径细化，并已完成两轮部署联调
- `Phase 3.2` 的 Redis proposal store 已恢复真实可用
- `Phase 3.2` 的跨重启 proposal 恢复已验证
- `Phase 3.3` 已启动
- `Phase 3.3` 已完成三处前端入口接线：
  - `CharacterListView`
  - `WorldSettingView`
  - `ChapterListView`
- `Phase 3.3` 已通过：
  - `npm run type-check`
  - `npm run build`
- `Phase 3.3` 已完成首轮线上部署验收：
  - 已确认线上静态资源包含更明显的摘要入口文案
  - 但尚未完成完整浏览器交互级验收
- `Phase 3.3` 已完成第二轮本地收口：
  - 已继续提高 `摘要优先编辑` 的主入口权重
  - 已继续降低旧 `编辑` 入口的视觉优先级
- `Phase 3.3` 已完成第三轮本地收口：
  - 已补 `普通 / 专家` 模式切换
  - 已补摘要意图切换
  - 已补未变更拦截与三步式提示
- `Phase 3.3` 已完成第二轮线上验收：
  - 已确认部署产物包含 `普通模式 / 专家模式 / 精修摘要 / 改写摘要 / 补充细节`
  - 已确认 `operatorMode=EXPERT` 与 `intent=REFINE` 在线上 backend 可用
  - 已确认 proposal key 继续真实落入 Redis
- `Phase 3.3` 已继续推进：
  - 已新增 `POST /api/summary-workflow/chat-turns`
  - 已把普通模式升级为“对话采集 + 摘要草稿”
  - 已把 `Character / WorldSetting / Chapter` 的新增与编辑统一收口到摘要工作流
  - 专家模式保留直填摘要和旧表单回退
  - 已继续降低普通作者认知负担：
    - 主按钮文案改为“说想法新增”
    - 普通模式步骤收口为“说想法 -> AI 整理 -> 看变化 -> 确认写回”
    - 普通模式不再强制先选结构意图
- 当前下一步应进入：
  - 先部署并复验带“短超时 + 回退”的 `Phase 3.3` 对话采集版本
  - 继续收口 `Phase 3.3`
  - `Phase 4` 暂不启动
- 最新修正：
  - 已确认线上 `POST /api/summary-workflow/chat-turns` 在普通模式下仍会超时
  - 已确认当前阶段不能视为 `Phase 3` 完成
  - 已补本地修复：
    - `summary-workflow -> naming` 路由收口
    - `conversation-timeout-seconds`
    - `conversation-max-tokens`
    - `DefaultStorySummaryConversationService` 虚拟线程超时回退
  - 已恢复第 31 章测试摘要原值
- 当前统一构建入口应使用根工程：
  - `mvn -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -DskipTests compile`
- 当前对 `Phase 2` 的状态判断：
  - 已完成

## Current Blockers

- 新主线已完成 `Phase 1` 与 `Phase 2`，`Phase 3` 仍在进行中。
- `StoryUnit` 已完成第一批协议、代码骨架与模块迁移，但尚未完成存储映射与 service/repository 实装。
- `MCP / LSP` 边界已明确，但还没有形成服务层和接口层实现。
- 前端仍是旧信息架构，尚未切到工作台模式。
- 旧主线中的真实 provider fallback 问题仍存在，后续需要作为平台升级前的稳定性基线继续跟踪。
- `summary-workflow` 已在线上真实跑通 proposal / preview / apply，Redis proposal store 也已恢复可用。
- `summary-workflow` 的普通模式 `chat-turns` 在线上仍未通过，当前是 `Phase 3` 的主阻塞点。

## Resume Reading Order

1. 先读取 `docs/requirements/REQ-20260411-stateful-story-platform-upgrade.md`。
2. 再读取 `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-v1.md`。
3. 再读取 `docs/progress/PROGRESS-REQ-20260411-stateful-story-platform-upgrade.md`。
4. 再读取本轮详细实施计划：
   - `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-phase1-foundation-v1.md`
   - `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-phase1-modularization-v1.md`
   - `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-phase2-storyunit-storage-v1.md`
   - `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-phase3-summary-first-workflow-v1.md`
   - `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-phase4-readonly-mcp-lsp-v1.md`
5. 再读取：
   - `docs/architecture/ARCH-REQ-20260411-story-unit-and-facets-v1.md`
   - `docs/architecture/ARCH-REQ-20260412-storyunit-mapping-matrix-v1.md`
   - `docs/architecture/ARCH-REQ-20260412-story-summary-read-model-v1.md`
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
