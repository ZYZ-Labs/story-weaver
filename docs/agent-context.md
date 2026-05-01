# Agent Context

- Last Updated: 2026-04-27 Asia/Shanghai
- Current Primary Req: `REQ-20260427-state-driven-narrative-runtime-upgrade`

## Current State

- `REQ-20260411-stateful-story-platform-upgrade` 已完成正式收口
- 当前主需求已切换到架构升级：`REQ-20260427-state-driven-narrative-runtime-upgrade`
- 当前方向不再只是继续修补多 scene continuity，而是把章节工作区从“正文先行”改成“checkpoint / intent / resolve / render”状态驱动主链
- 章节工作区已接入 node runtime 预览面板：
  - 当前能看到 node skeleton、current node、checkpoint 和 open loop
  - 当前已支持 chapter-level `scene / node` mode 切换
  - `node resolve` 默认仍关闭，但即使后续打开全局开关，也只有切到 `node mode` 的章节才允许推进
  - `scene draft / accept / rollback` 已全部绑定 `scene mode`
- 当前正在处理独立缺陷需求：`REQ-20260425-chapter-workspace-ai-scene-skeleton-refactor`
- 当前主链聚焦章节工作区多 scene：
  - 章节骨架必须由 AI 显式生成，不再由本地规则自动补 scene
  - 章节工作区草稿与日志必须按 scene 隔离，不再按整章混存
  - 章节工作区 scene 必须严格顺序解锁，只允许处理第一个未接纳镜头
  - 接受当前镜头草稿是唯一的推进动作，runtime/handoff 必须来自真实已接纳正文
  - 章节工作区必须支持 accepted scene 安全撤回：既能逐级撤回最新已接纳镜头，也能一次性撤回全部，并同步恢复正文、scene runtime/handoff、reader reveal 与 chapter state
  - `scene-n+1` 不能再只依赖“上一镜头摘要/交接”弱提示，accepted scene 必须写回结构化 continuity state，并在生成前后同时使用
  - 章节工作区和写作聊天的 SSE 流必须在 `ASYNC / ERROR` dispatch 上保持认证，不能再被 Security 异步掐断
- `REQ-20260425-navigation-chunk404-and-model-routing-fix` 与 `REQ-20260425-ai-writing-timeout-root-cause-fix` 已完成代码修复，当前仅剩用户环境回归确认
- 后续工作仍应新建独立需求，不再继续向平台升级主需求追加 phase

## Active Requirements

| Req ID | Name | Status | Requirement Doc | Master Plan | Progress |
| --- | --- | --- | --- | --- | --- |
| REQ-20260427-state-driven-narrative-runtime-upgrade | 状态驱动叙事 Runtime 架构升级 | In Progress | `docs/requirements/REQ-20260427-state-driven-narrative-runtime-upgrade.md` | `docs/plans/PLAN-REQ-20260427-state-driven-narrative-runtime-upgrade-v1.md` | `docs/progress/PROGRESS-REQ-20260427-state-driven-narrative-runtime-upgrade.md` |
| REQ-20260425-chapter-workspace-ai-scene-skeleton-refactor | 章节工作区多 Scene 真 AI 骨架重构 | In Progress | `docs/requirements/REQ-20260425-chapter-workspace-ai-scene-skeleton-refactor.md` | `docs/plans/PLAN-REQ-20260425-chapter-workspace-ai-scene-skeleton-refactor-v1.md` | `docs/progress/PROGRESS-REQ-20260425-chapter-workspace-ai-scene-skeleton-refactor.md` |
| REQ-20260425-navigation-chunk404-and-model-routing-fix | 导航懒加载 404 与章节工作区模型路由错配修复 | In Progress | `docs/requirements/REQ-20260425-navigation-chunk404-and-model-routing-fix.md` | `docs/plans/PLAN-REQ-20260425-navigation-chunk404-and-model-routing-fix-v1.md` | `docs/progress/PROGRESS-REQ-20260425-navigation-chunk404-and-model-routing-fix.md` |
| REQ-20260425-ai-writing-timeout-root-cause-fix | 章节工作区 AI Writing 卡住问题根因修复 | In Progress | `docs/requirements/REQ-20260425-ai-writing-timeout-root-cause-fix.md` | `docs/plans/PLAN-REQ-20260425-ai-writing-timeout-root-cause-fix-v1.md` | `docs/progress/PROGRESS-REQ-20260425-ai-writing-timeout-root-cause-fix.md` |

## Latest Completed Requirement

| Req ID | Name | Status | Requirement Doc | Master Plan | Progress | Final Closure |
| --- | --- | --- | --- | --- | --- | --- |
| REQ-20260411-stateful-story-platform-upgrade | Story 平台级架构升级：Summary First、StoryUnit、MCP/State Server 与多 Session 编排 | Completed | `docs/requirements/REQ-20260411-stateful-story-platform-upgrade.md` | `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-v1.md` | `docs/progress/PROGRESS-REQ-20260411-stateful-story-platform-upgrade.md` | `docs/reports/REPORT-20260424-platform-upgrade-final-closure-v1.md` |

## First Read

后续模型接手时，固定先读：

1. `docs/agent-context.md`
2. `docs/agent-handoff-rules.md`
3. `docs/guides/GUIDE-20260424-system-capabilities-and-chain-reference-v1.md`
4. `docs/reports/REPORT-20260424-platform-upgrade-final-closure-v1.md`
5. `docs/test-data/TESTDATA-20260424-phase10-replay-matrix-v1.md`
6. `docs/guides/GUIDE-20260424-phase10-page-acceptance-template-v1.md`

## Core Reference Docs

### System

- `docs/guides/GUIDE-20260424-system-capabilities-and-chain-reference-v1.md`
- `docs/guides/GUIDE-20260424-phase10-page-acceptance-template-v1.md`
- `docs/test-data/TESTDATA-20260424-phase10-replay-matrix-v1.md`

### Completed Upgrade

- `docs/requirements/REQ-20260411-stateful-story-platform-upgrade.md`
- `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-v1.md`
- `docs/progress/PROGRESS-REQ-20260411-stateful-story-platform-upgrade.md`
- `docs/reports/REPORT-20260424-platform-upgrade-final-closure-v1.md`

### Optional Historical Deep Dive

- `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-phase*.md`
- `docs/reports/REPORT-20260424-phase10-consistency-baseline-v1.md`
- `docs/reports/REPORT-20260424-phase9-migration-live-validation-round2.md`
- `docs/architecture/*.md`
- `docs/archive/**`

## Next Action Template

如果后续继续开发，先做：

1. 新建独立需求文档
2. 在本文件补一条新的 Active Requirement
3. 明确主入口文档
4. 再开始编码

## Notes

- 固定联调项目仍优先使用 `旧日王座 / projectId=28`
- 固定章节样本以 `TESTDATA-20260424-phase10-replay-matrix-v1.md` 为准
- 新架构主入口文档新增：
  - `docs/architecture/ARCH-REQ-20260427-state-driven-narrative-runtime-v1.md`
- 当前优先回归新缺陷：
  - 章节工作区是否先由 AI 生成真实镜头骨架
  - 章节工作区后续镜头是否未解锁不可越级选择，且只能在接纳前一镜头后继续
  - accepted scene 撤回后，章节正文、reader reveal、chapter state 和当前解锁镜头是否一起恢复，不再残留脏状态
  - 已接纳镜头的前缀 runtime 如果部分过期，撤回是否仍能依据 accept 快照安全恢复，而不是误报“scene 运行态缺失”
  - `scene-2+` 的承上摘要 / handoff / continuity facts 是否来自真实已接纳正文
  - `scene-2+` 是否还会出现人物称呼漂移、时间线冲突或提前抢写下一镜头
  - 章节工作区切换 scene 后，草稿和日志是否仍会相互覆盖
  - `scene-2+` 或背景聊天这类 SSE 请求是否还会出现因 async dispatch 鉴权丢失导致的“连接中断”
  - 章节骨架生成是否已改为流式日志链，不再被前端 15 秒同步超时直接掐断
- 当前新架构下一步：
  - narrator render 正式接入 node runtime
  - checkpoint 回档与正文撤回边界收敛
  - node / scene mode migration 边界收敛
- 当前验证约定：
  - 开发阶段不再额外为 Mockito / JVM attach 测试处理本机环境
  - 代码侧以编译、非 Mockito 最小回归、前端类型检查作为开发期验证
  - 最终部署和浏览器联调由用户环境回归
- `REQ-20260425-navigation-chunk404-and-model-routing-fix` 与 `REQ-20260425-ai-writing-timeout-root-cause-fix` 仍保留为独立缺陷记录，避免与本轮章节工作区多 scene 重构混淆
- 早期 `旧日王座` 回归报告、旧 PRD、旧上下文流说明已移入 `docs/archive/legacy/*`
