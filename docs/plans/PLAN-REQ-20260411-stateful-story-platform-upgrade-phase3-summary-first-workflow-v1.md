# Story 平台升级 Phase 3 详细实施计划：Summary First 对象工作流

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Plan ID: PLAN-REQ-20260411-stateful-story-platform-upgrade-phase3-summary-first-workflow-v1
- Status: Completed
- Created At: 2026-04-12 Asia/Shanghai
- Updated At: 2026-04-13 Asia/Shanghai

## 本轮目标

在 `Phase 2` 已完成 `StoryUnit` 投影、`SummaryFacet` 规则回生成和统一读模型收口的基础上，把对象侧交互从“字段表单直改”升级为“摘要输入 -> 结构提案 -> 变化预览 -> 确认写回”的 `Summary First` 工作流。

本轮只追求四件事：

- 冻结对象侧 `Summary First` 交互协议
- 落第一批后端摘要提案与确认写回服务
- 为前端准备统一的摘要编辑与变化预览接口
- 先打通 `Character / WorldSetting / Chapter` 三类对象

一句话说：

- 用户默认写摘要
- 系统内部生成 patch
- 用户确认的是摘要变化，不是字段变化

## 当前阶段定位

- 这是 `Phase 3` 的详细实施计划文档
- `Phase 3.1` 已完成
- `Phase 3.2` 已完成
- `Phase 3.3` 已完成
- 后续代码开发建议按 `Phase 3.1 -> 3.2 -> 3.3` 顺序推进

## 本轮原则

- 用户默认不看字段墙
- `SummaryFacet` 是对象主视图
- 结构化 patch 是系统内部协议
- patch 默认先预览，再确认写回
- 写回仍以现有业务表为 source of truth
- 不在本阶段直接引入模型强依赖
- 不在本阶段做全对象族覆盖

## 核心设计判断

### 1. `Summary First` 不是“只有摘要，没有结构”

这里不是取消结构化，而是把结构化下沉成后台协议：

- 用户输入摘要
- 系统产出结构化 patch proposal
- 系统基于 patch 重新生成标准摘要
- 用户只确认摘要变化和关键影响

### 2. 对象修改默认是“两段式提交”

对象侧默认不再直接保存用户输入文本，而是改成：

1. `suggest/propose`
2. `preview`
3. `apply`

这比直接覆盖字段稳得多，也更适合后续 AI 参与。

### 3. 第一批只做三类对象

本阶段只做：

- `Character`
- `WorldSetting`
- `Chapter`

原因：

- 它们已经有 `StoryUnit` 投影与摘要视图
- 它们对写作主链、对象页和后续 MCP 只读最关键

### 4. UI 先改交互骨架，不追求整站换皮

本阶段先准备：

- 摘要编辑器
- 结构变化预览
- 确认写回抽屉/弹窗

不在本阶段做整站导航级重构。

## 范围内

- 定义 `Summary First` 对象交互协议
- 定义摘要输入、结构提案、变化预览、确认写回的数据结构
- 新增首批应用服务：
  - `StorySummaryProposalService`
  - `StorySummaryPreviewService`
  - `StorySummaryApplyService`
- 为 `Character / WorldSetting / Chapter` 建立对象族 handler / translator
- 提供统一 API 草案：
  - `suggest`
  - `preview`
  - `apply`
- 定义前端最小交互骨架和接口口径
- 补计划、进度、测试与验收标准

## 范围外

- 不在本阶段落地 MCP Server
- 不在本阶段落地 State Server
- 不在本阶段做四 session 编排主链接入
- 不在本阶段做所有对象页全面重构
- 不在本阶段引入 Items / Skills / Factions / Locations 的完整支持
- 不在本阶段做模型驱动的自动 patch 批量写回

## 分阶段实施拆分

### `Phase 3.1` 协议冻结与对象族适配层

目标：

- 冻结 `Summary First` 的统一协议
- 为三类对象建立摘要输入与 patch proposal 的适配边界

交付：

- `SummaryInputDraft`
- `StructuredPatchProposal`
- `SummaryChangePreview`
- `SummaryApplyCommand`
- `SummaryApplyResult`
- 三类对象族的 proposal translator 接口

退出条件：

- 同一个前端交互能驱动三类对象，不再为每类对象单独拼接口

当前状态：

- 已完成
- 已新增：
  - `SummaryInputDraft`
  - `SummaryInputIntent`
  - `SummaryOperatorMode`
  - `StructuredPatchProposal`
  - `SummaryChangePreview`
  - `SummaryApplyCommand`
  - `SummaryApplyResult`
  - `StorySummaryProposalService`
  - `StorySummaryPreviewService`
  - `StorySummaryApplyService`

### `Phase 3.2` 后端提案、预览与写回服务

目标：

- 在 `backend` 中实现第一批 `Summary First` 服务骨架

交付：

- `StorySummaryProposalService`
- `StorySummaryPreviewService`
- `StorySummaryApplyService`
- `Character / WorldSetting / Chapter` 的 handler 实现
- 结构变化摘要与摘要回生成链

退出条件：

- 三类对象都能完成：
  - 摘要输入
  - patch proposal
  - 变化预览
  - 确认写回

当前状态：

- 已启动并完成首轮骨架
- 已新增：
  - `CharacterStorySummaryTargetHandler`
  - `WorldSettingStorySummaryTargetHandler`
  - `ChapterStorySummaryTargetHandler`
  - `DefaultStorySummaryProposalWorkflowService`
  - `DefaultStorySummaryPreviewWorkflowService`
  - `DefaultStorySummaryApplyWorkflowService`
  - `SummaryProposalStore`
  - `ResilientSummaryProposalStore`
  - `SummaryWorkflowController`
- 已提供最小 API：
  - `POST /api/summary-workflow/proposals`
  - `POST /api/summary-workflow/previews`
  - `POST /api/summary-workflow/apply`
- 已支持：
  - proposal 默认尝试持久化到 Redis
  - Redis 不可用时自动回退到内存态
  - `preview / apply` 可仅基于 `proposalId`
- 已完成配置外置：
  - `STORY_SUMMARY_WORKFLOW_REDIS_PROPOSAL_STORE_ENABLED`
  - `STORY_SUMMARY_WORKFLOW_PROPOSAL_TTL_MINUTES`
  - `STORY_SUMMARY_WORKFLOW_PROPOSAL_KEY_PREFIX`
- 已完成异常口径细化：
  - proposal 不存在/过期 -> `404`
  - proposal 目标冲突 -> `409`
  - 参数缺失/输入非法 -> `400`
- 已补最小单测：
  - `DefaultStorySummaryProposalWorkflowServiceTest`
  - `DefaultStorySummaryApplyWorkflowServiceTest`
  - `ResilientSummaryProposalStoreTest`
- 已完成首轮线上联调：
  - `proposal / preview / apply` 已在部署环境真实跑通
  - `proposalId` 取回式 `preview / apply` 可用
  - `404 / 409` 异常口径已验证
  - 第 31 章摘要写回与恢复已验证
  - 见：
    - `docs/reports/REPORT-20260413-summary-workflow-live-validation-round1.md`
- 当前仍属于首轮最小实现：
  - `Character / Chapter` 写回先复用 `StructuredSummaryApplyServiceImpl`
  - `WorldSetting` 写回先复用 `WorldSettingService`
  - 摘要拆分仍是规则型最小实现，不引入模型强依赖
  - Redis proposal store 已完成线上修复
  - “跨重启 proposal 恢复”已完成第二轮联调验证
  - 见：
    - `docs/reports/REPORT-20260413-summary-workflow-live-validation-round2.md`

### `Phase 3.3` 前端最小摘要工作流入口

目标：

- 在不做全站重构的前提下，先给对象侧接入最小可用的摘要工作流入口

交付：

- 摘要编辑区
- 变化预览弹层/侧栏
- 确认写回动作
- 基础错误与冲突提示

退出条件：

- 对象页默认可用摘要流修改对象，不再强依赖字段表单

当前状态：

- 已启动
- 已完成首轮前端接线与本地构建验证
- 已完成首轮线上部署验收：
  - 已确认 `Character / WorldSetting / Chapter` 三处页面对应构建产物中存在更显式入口文案
  - 已确认当前线上版本包含：
    - `摘要优先编辑`
    - `摘要优先`
    - `打开摘要工作流`
- `Phase 3.3` 尚未完成
- 已完成的最近一轮推进：
  - 三处页面的新增与编辑已统一先进入摘要工作流
  - 普通模式已从“直填摘要”升级为“对话采集 + 摘要草稿”
  - 后端已新增：
    - `POST /api/summary-workflow/chat-turns`
  - 专家模式已调整为默认直填摘要，并保留切回旧表单入口
  - 已完成本地验证：
    - `mvn -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -DskipTests compile`
    - `mvn test -pl backend -am -Dtest=DefaultStorySummaryConversationServiceTest,DefaultStorySummaryProposalWorkflowServiceTest,DefaultStorySummaryApplyWorkflowServiceTest,ResilientSummaryProposalStoreTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2`
    - `npm run type-check`
    - `npm run build`
  - 已继续降低普通作者认知负担：
    - 普通模式的主按钮文案收口为“说想法新增”
    - 工作流步骤收口为“说想法 -> AI 整理 -> 看变化 -> 确认写回”
    - 普通模式不再强制作者先选 `REFINE / UPDATE / ENRICH`
- 最新状态修正：
  - 已确认线上 `POST /api/summary-workflow/chat-turns` 在普通模式下仍会超时
  - 已确认当前阻塞点不是前端入口，而是后端对话链路缺少短超时与快速回退
  - 已完成本地修复：
    - `summary-workflow` 优先路由到 `naming_ai_provider_id / naming_ai_model`
    - 新增 `conversation-timeout-seconds`
    - 新增 `conversation-max-tokens`
    - `DefaultStorySummaryConversationService` 加入虚拟线程超时回退
  - 已完成线上测试数据恢复：
    - 第 31 章摘要已恢复为原值
  - 见：
    - `docs/reports/REPORT-20260413-summary-workflow-live-validation-round5.md`
  - 已完成部署后复验：
    - 普通模式 `POST /api/summary-workflow/chat-turns` 已恢复 `HTTP 200`
    - `CHARACTER CREATE` 与 `CHAPTER REFINE` 两类普通模式对话已可返回摘要草稿
    - 普通模式返回结果已可继续进入 `proposal / preview`
    - 前端部署产物已确认包含：
      - `说想法新增`
      - `说想法`
      - `AI 整理`
      - `让 AI 继续整理`
    - 见：
      - `docs/reports/REPORT-20260413-summary-workflow-live-validation-round6.md`
- 已完成浏览器级真实点击验收：
  - `Character` 新增通过
  - `WorldSetting` 新增通过
  - `Chapter` 新增通过
  - `Chapter` 摘要编辑通过
  - 见：
    - `docs/reports/REPORT-20260413-summary-workflow-browser-validation-round1.md`
- 已完成最终部署复验：
  - 顶层普通模式入口减负已部署生效
  - 四条浏览器级主路径继续全部通过
  - 见：
    - `docs/reports/REPORT-20260413-summary-workflow-browser-validation-round2.md`
- 当前剩余：
  - `Phase 3` 无阻塞剩余项
  - 后续仅保留小幅 UX 微调，不再扩本阶段 scope
  - 下一阶段进入 `Phase 4`
- 已新增统一前端入口：
  - `front/src/components/SummaryWorkflowDialog.vue`
- 已新增前端 API：
  - `front/src/api/summary-workflow.ts`
- 已开始接线：
  - `CharacterListView`
  - `WorldSettingView`
  - `ChapterListView`

## 建议协议

### 1. `SummaryInputDraft`

用户输入摘要的统一承载：

- `targetType`
- `targetId`
- `projectId`
- `summaryText`
- `intent`
- `operatorMode`

### 2. `StructuredPatchProposal`

系统内部的结构化提案：

- `proposalId`
- `targetRef`
- `facet`
- `operations`
- `summary`
- `riskNotes`
- `pendingQuestions`

### 3. `SummaryChangePreview`

面向用户的预览对象：

- `beforeSummary`
- `afterSummary`
- `changeSummary`
- `affectedFacets`
- `requiresConfirmation`

### 4. `SummaryApplyCommand`

确认写回动作：

- `proposalId`
- `targetRef`
- `confirmed`
- `operatorId`

### 5. `SummaryApplyResult`

写回结果：

- `applied`
- `updatedSummary`
- `updatedUnitRef`
- `warnings`

## 首批对象族要求

### Character

第一轮至少支持：

- 人物概述
- 身份/定位提炼
- 当前状态摘要
- 项目与章节关系变化说明

### WorldSetting

第一轮至少支持：

- 设定一句话概述
- 设定长摘要
- 分类与关联对象提炼

### Chapter

第一轮至少支持：

- 章节摘要
- 当前章节状态摘要
- 与大纲、POV、剧情的关系摘要

## API 草案

建议统一走对象摘要工作流接口，而不是每类对象自建一套散接口。

首轮建议：

- `POST /api/story-units/{unitType}/{unitId}/summary/propose`
- `POST /api/story-units/{unitType}/{unitId}/summary/preview`
- `POST /api/story-units/{unitType}/{unitId}/summary/apply`

兼容约束：

- 当前旧对象 CRUD 接口先保留
- 新接口作为并行入口
- 等 `Phase 3` 稳定后再评估是否下调旧表单入口

## 迁移基线

`Phase 3.2` 不应平地起一套完全平行的写回实现，而应优先复用并拆分现有基础能力。

当前已确认可作为迁移基线的代码：

- `backend.story.generation.impl.StructuredSummaryApplyServiceImpl`
- `CharacterService`
- `WorldSettingService`
- `ChapterService`

迁移策略建议：

- 保留现有 `StructuredSummaryApplyServiceImpl` 作为过渡实现
- 把对象族差异逐步下沉到 `Character / WorldSetting / Chapter` handler
- 把“摘要输入 -> patch proposal -> preview -> apply”新协议接在外层
- 等 `Phase 3.2` 稳定后，再评估是否替换旧 controller/service 入口

## 测试与验证

### 自动化验证

- `proposal service` 至少一组规则测试
- `preview service` 至少一组摘要变化测试
- `apply service` 至少一组对象族写回测试

### 手工校验

- 用户输入摘要后，是否能看到明确的变化预览
- preview 是否稳定，不依赖前端二次拼接
- apply 后，`StoryUnit` 读模型中的 `SummaryFacet` 是否及时更新
- 三类对象是否都能按同一工作流走通

## 风险清单

### 风险 1. 变成“摘要包一层，字段逻辑还是散的”

规避：

- patch proposal 必须成为唯一的中间协议
- 不允许 controller 直接把摘要拆字段然后落表

### 风险 2. 预览信息不稳定

规避：

- `beforeSummary / afterSummary / changeSummary` 统一由服务端生成
- 前端只负责展示，不自己拼

### 风险 3. 太早把模型绑进对象编辑主链

规避：

- 首轮先做规则型 proposal / preview
- AI 参与作为后续增强，不作为第一轮硬依赖

### 风险 4. 对象族范围失控

规避：

- 本阶段只做 `Character / WorldSetting / Chapter`
- 其他对象族后续单独排期

## 本轮退出条件

`Phase 3` 的这第一份详细计划只有在下面条件满足后才算执行完成：

- `Summary First` 的交互协议已经冻结
- 三类对象族都能被同一套 proposal / preview / apply 工作流承载
- 前端最小摘要入口与后端契约已明确
- 主进度和 agent context 已切到 `Phase 3`
- 用户可据此决定是否进入编码

## 当前阶段进展

- `Phase 2` 已完成：
  - `StoryUnit` 投影服务
  - `StorySummaryService`
  - 规则型摘要回生成
  - 统一 `ProjectedStoryUnit` 读模型
- `Phase 3` 仍在进行中
- 下一步建议：
  - 继续完成 `Phase 3.3` 的真实部署验收与交互收口
  - `Phase 4` 暂不启动

## 贡献与署名说明

- “用户绝对看到的只能是摘要”“字段墙必须退居二线”的方向：用户提出。
- `Summary First` 的阶段拆分、协议设计、对象族范围和实施路线：Codex 完成。
- 当前方案由用户与 Codex 共同讨论形成。
