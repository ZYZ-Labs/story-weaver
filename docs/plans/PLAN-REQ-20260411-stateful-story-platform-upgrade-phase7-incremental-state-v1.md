# Story 平台升级 Phase 7 详细实施计划：增量状态系统

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Plan ID: PLAN-REQ-20260411-stateful-story-platform-upgrade-phase7-incremental-state-v1
- Status: In Progress
- Created At: 2026-04-18 Asia/Shanghai
- Updated At: 2026-04-18 Asia/Shanghai

## 本轮目标

在 `Phase 6` 已完成镜头级执行、handoff 写回与章节级审校之后，开始把“scene runtime state”推进到更通用的增量状态系统。

`Phase 7` 要解决的不是再多加几个执行接口，而是：

- 镜头执行结果如何沉淀成 `StoryEvent`
- 当前状态如何沉淀成 `StorySnapshot`
- 后续状态变化如何通过 `event + patch` 承接
- `State Server` 在内部层面如何开始具备真正的读写闭环

一句话说：

- `Phase 6` 解决“镜头怎么执行”
- `Phase 7` 解决“执行结果如何变成可追踪状态”

## 本轮原则

- 先从 `scene execution` 产出事件与快照，不先一口气覆盖所有业务对象
- 先做最小状态读写闭环，再逐步扩展复杂状态族
- 事件与快照优先服务内部 `State Server`，不是先做复杂前端面板
- 保持 `Redis 优先 + 内存回退`
- 保持现有章节与镜头联调链路不回退

## 范围内

- `StoryEventStore`
- `StorySnapshotStore`
- `StoryStateProperties`
- scene execution -> event + snapshot 写回
- 章节级 state 查询接口
- `Phase 7.1` 的最小状态读写联调

## 范围外

- 不在本阶段直接实现完整背包系统
- 不在本阶段实现完整技能冷却系统
- 不在本阶段实现最终的关系/好感工作台
- 不在本阶段开放第三方 MCP 写接口

## 分阶段实施拆分

### `Phase 7.1` Scene 执行结果的事件化与快照化

目标：

- 让每次 `execute` 不只写 runtime state 与 handoff
- 同时产出：
  - `StoryEvent`
  - `StorySnapshot`

交付：

- `StoryEventStore`
- `StorySnapshotStore`
- `ResilientStoryStateStore`
- `StoryStateController`
- `SceneExecutionWriteResult` 扩展为包含 event + snapshot

退出条件：

- `POST /api/story-orchestration/.../execute` 后可观察到：
  - 事件写回
  - 快照写回
  - 查询接口可读回

### `Phase 7.2` Patch 边界与最小状态写回协议

目标：

- 在已有 `StoryPatch` 基础上，把状态变更从“章节摘要修改”推进到“执行态修改”

交付：

- state facet 相关 patch 约束
- state patch 的最小应用边界
- 与 `StoryEvent` 的关联规则

退出条件：

- 至少一类状态变化可由 `patch -> apply -> snapshot` 串起来

### `Phase 7.3` 高优先级状态族接入

优先级最高：

- Open Loops
- Reader Reveal State
- 人物态度 / 情绪标签
- 地点状态

退出条件：

- 这些状态至少有一类完成真实写回与真实读取

### `Phase 7.4` 联调收口

目标：

- 用真实章节验证：
  - execute
  - event
  - snapshot
  - patch
  - state query

退出条件：

- 至少一个真实章节的状态链可完整回读

## 当前阶段判断

- `Phase 6` 已完成
- `Phase 7.1` 已完成真实联调收口：
  - `execute` 已可在线上真实产出：
    - `StoryEvent`
    - `StorySnapshot`
  - `/api/story-state/.../events`
  - `/api/story-state/.../snapshots`
    已可在线上读回
- 当前第一优先级已切换到 `Phase 7.2`
- 当前阶段备注：
  - Redis 运行环境仍存在 `bgsave` 的 `/data` 权限问题
  - 已将 `stop-writes-on-bgsave-error no` 写入当前挂载的 `redis.conf`
  - Redis 重启后不再因为 `bgsave` 失败阻断业务写入
  - `/data` 的 RDB 权限问题仍存在，但已从业务 blocker 降级为环境告警
  - `Phase 7.2` 当前已到可部署联调阶段：
    - `StoryPatchStore`
    - `ReaderRevealStateStore`
    - `patch -> apply -> snapshot`
    - `/api/story-state/.../patches`
    - `/api/story-state/.../reader-reveal-state`

## 建议代码落点

- 合同层：
  - `backend/modules/story-storyunit/src/main/java/com/storyweaver/storyunit/event/*`
  - `backend/modules/story-storyunit/src/main/java/com/storyweaver/storyunit/snapshot/*`
  - `backend/modules/story-storyunit/src/main/java/com/storyweaver/storyunit/service/*`
- 实现层：
  - `backend/src/main/java/com/storyweaver/storyunit/service/impl/*`
  - `backend/src/main/java/com/storyweaver/story/generation/orchestration/impl/*`
  - `backend/src/main/java/com/storyweaver/controller/*`
- 测试：
  - `backend/src/test/java/com/storyweaver/storyunit/service/impl/*`
  - `backend/src/test/java/com/storyweaver/story/generation/orchestration/impl/*`
  - `backend/src/test/java/com/storyweaver/controller/*`

## 验证方式

- 根工程编译：
  - `mvn -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -DskipTests compile`
- 最小回归：
  - `DefaultSceneExecutionWriteServiceTest`
  - `StoryStateControllerTest`
  - `ResilientStoryStateStoreTest`
- 真实联调：
  - `旧日王座` 项目章节执行后读取：
    - `/api/story-state/projects/{projectId}/chapters/{chapterId}/events`
    - `/api/story-state/projects/{projectId}/chapters/{chapterId}/snapshots`

## 风险

- 如果把所有状态族一次性纳入，复杂度会迅速失控
- 如果 event/snapshot 命名和作用域不稳定，后续 State Server 会很难回放
- 如果只写不读，状态系统会再次退化成隐式存储

## 下一步

1. 开始 `Phase 7.2`
2. 冻结 state patch 的最小边界
3. 让至少一类状态变化形成：
   - `patch -> apply -> snapshot`
4. 单独跟进 Redis 运行环境的持久化权限修复

## 贡献与署名说明

- 需求方向与产品判断由用户提出和主导
- 本文档结构化拆分、工程化范围界定与实施顺序整理由 Codex 完成
