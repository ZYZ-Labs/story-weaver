# Story 平台升级 Phase 5 详细实施计划：多 Session 编排落地

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Plan ID: PLAN-REQ-20260411-stateful-story-platform-upgrade-phase5-multi-session-orchestration-v1
- Status: In Progress
- Created At: 2026-04-17 Asia/Shanghai
- Updated At: 2026-04-17 Asia/Shanghai

## 本轮目标

在 `Phase 4` 已完成只读上下文标准化之后，开始把生成链路从“单条大 prompt 工作流”推进到“多 session 编排工作流”。

本阶段不直接进入章节骨架和镜头执行细节，那属于 `Phase 6`。
`Phase 5` 先做三件事：

- 冻结四 session 的输入输出协议
- 建立统一 `context packet` 与 `trace`
- 落最小 orchestrator 壳，确保后续 director / selector / writer / reviewer 能接入同一套编排面

一句话说：

- 先把编排壳搭稳
- 再让四个 session 逐步接进来

## 当前阶段定位

- `Phase 5` 是 `Phase 4` 的直接延续
- `Phase 4` 解决的是“怎么稳定读上下文”
- `Phase 5` 解决的是“这些上下文如何进入多 session 编排”

## 本轮原则

- 不共享长聊天历史
- 共享统一 `StorySessionContextPacket`
- 非写手环节优先输出结构化结果
- 编排 trace 必须可回放、可追踪、可诊断
- `Phase 5` 不提前承担 `Phase 6` 的镜头拆分复杂度

## 范围内

- `StorySessionContextPacket`
- `SessionExecutionTrace`
- `SessionExecutionTraceItem`
- `StorySessionContextAssembler`
- `StorySessionOrchestrator`
- 四 session 的边界和调用顺序
- 最小 orchestrator 启动壳

## 范围外

- 不在本阶段实现章节骨架拆分算法
- 不在本阶段实现逐镜头写回
- 不在本阶段做状态增量系统
- 不在本阶段接入 MCP 写回工具

## 分阶段实施拆分

### `Phase 5.1` 协议冻结与上下文包落地

目标：

- 冻结四 session 的统一输入底座
- 建立 trace 协议

交付：

- `StorySessionContextPacket`
- `SessionExecutionTrace`
- `SessionExecutionTraceItem`
- `StorySessionContextAssembler`
- `StorySessionOrchestrator`

退出条件：

- 后续 director / selector / writer / reviewer 都能消费同一份上下文包

当前进展：

- 已完成
- 已落：
  - `backend/modules/story-generation/src/main/java/com/storyweaver/story/generation/orchestration/StorySessionContextPacket.java`
  - `backend/modules/story-generation/src/main/java/com/storyweaver/story/generation/orchestration/SessionExecutionTrace.java`
  - `backend/modules/story-generation/src/main/java/com/storyweaver/story/generation/orchestration/SessionExecutionTraceItem.java`
  - `backend/modules/story-generation/src/main/java/com/storyweaver/story/generation/orchestration/StorySessionContextAssembler.java`
  - `backend/modules/story-generation/src/main/java/com/storyweaver/story/generation/orchestration/StorySessionOrchestrator.java`
  - `backend/src/main/java/com/storyweaver/story/generation/orchestration/impl/DefaultStorySessionContextAssembler.java`
  - `backend/src/main/java/com/storyweaver/story/generation/orchestration/impl/DefaultStorySessionOrchestrator.java`
  - `backend/src/test/java/com/storyweaver/story/generation/orchestration/impl/DefaultStorySessionContextAssemblerTest.java`
- 本地验证已通过：
  - `mvn -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -DskipTests compile`
  - `mvn test -pl backend -am -Dtest=DefaultStorySessionContextAssemblerTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2`

### `Phase 5.2` Director / Selector 接入编排壳

目标：

- 让总导与选择器先跑进新编排面

交付：

- `DirectorSessionService`
- `SelectorSessionService`
- orchestrator 内的 director -> selector 顺序执行
- 第一版选择结果 trace

退出条件：

- orchestrator 能稳定产出：
  - candidate list
  - selection decision
  - trace

当前进展：

- 已启动
- 已落：
  - `DirectorSessionService`
  - `SelectorSessionService`
  - `WriterExecutionBriefBuilder`
  - `StorySessionPreview`
  - `RuleBasedDirectorSessionService`
  - `RuleBasedSelectorSessionService`
  - `DefaultWriterExecutionBriefBuilder`
  - `StorySessionOrchestrationController`
- 已新增部署测试入口：
  - `GET /api/story-orchestration/projects/{projectId}/chapters/{chapterId}/preview`
- 已完成本地回归：
  - `DefaultStorySessionOrchestratorTest`
  - `StorySessionOrchestrationControllerTest`

当前判断：

- `director -> selector -> writer-brief` 最小预览链已成立
- 下一步不再是补 director / selector，而是把 writer / reviewer 一起接入同一预览链

### `Phase 5.3` Writer / Reviewer 接入编排壳

目标：

- 把写手与审校器接入相同编排壳

交付：

- `WriterSessionService`
- `ReviewerSessionService`
- writer brief 生成
- reviewer 结果回写 trace

退出条件：

- 四 session 在统一编排壳内完成最小闭环

当前进展：

- 已启动
- 已落：
  - `WriterSessionService`
  - `ReviewerSessionService`
  - `WriterSessionResult`
  - `RuleBasedWriterSessionService`
  - `RuleBasedReviewerSessionService`
  - `DefaultStorySessionOrchestrator` 已升级为：
    - `context -> director -> selector -> writer-brief -> writer-result -> reviewer-decision`
- `StorySessionPreview` 当前已返回：
  - `contextPacket`
  - `candidates`
  - `selectionDecision`
  - `writerExecutionBrief`
  - `writerSessionResult`
  - `reviewDecision`
  - `trace`
- `SessionExecutionTrace` 当前已记录 5 段最小 trace：
  - `director-candidates`
  - `selector-decision`
  - `writer-brief`
  - `writer-result`
  - `reviewer-decision`
- 已完成本地回归：
  - `DefaultStorySessionContextAssemblerTest`
  - `DefaultStorySessionOrchestratorTest`
  - `StorySessionOrchestrationControllerTest`

当前判断：

- 已完成首轮真实联调
- `director -> selector -> writer -> reviewer` 的最小预览闭环已在线上跑通
- 当前缺口不在接口通断，而在：
  - `trace` 与失败口径还未完全收口
  - `sceneId` 尚未绑定真实 `SceneExecutionState` 承接

### `Phase 5.4` 编排 trace 与重试口径收口

目标：

- 让编排层具备最小诊断和重试能力

交付：

- trace 结构固定
- 编排失败口径固定
- 最小重试边界固定

退出条件：

- 多 session 编排具备“可回放、可追踪、可重试”的最小工程性质

当前进展：

- 本地已完成第一轮收口，待部署联调
- 已新增：
  - `SceneBindingContext`
  - `SceneBindingMode`
- `StorySessionContextPacket` 当前已显式返回：
  - `sceneId`
  - `sceneBindingContext`
- `SessionExecutionTraceItem` 当前已固定新增字段：
  - `attempt`
  - `retryable`
  - `details`
- `DefaultStorySessionContextAssembler` 当前已明确区分：
  - `SCENE_BOUND`
  - `SCENE_FALLBACK_TO_LATEST`
  - `CHAPTER_COLD_START`
  - `SCENE_QUERY_UNAVAILABLE`
- `DefaultStorySessionOrchestrator` 当前已新增编排首步：
  - `context-scene-binding`
- `writer / reviewer` trace 当前已带：
  - 自动修复可否重试
  - 选择结果与风险数量
  - writer brief 的目标字数与承接信息
- 已完成本地回归：
  - `DefaultStorySessionContextAssemblerTest`
  - `DefaultStorySessionOrchestratorTest`
  - `StorySessionOrchestrationControllerTest`

当前判断：

- `Phase 5.4` 开发侧主链已到可部署联调阶段
- 已完成真实联调验证：
  - `sceneBindingContext` 已真实返回
  - `trace` 的 `attempt / retryable / details` 已真实返回
  - 当前线上返回明确为：
    - `SCENE_QUERY_UNAVAILABLE`
    - `sceneId` 仅做参数透传
  - 这说明 `Phase 5.4` 的边界表达已成立，真实 scene 承接应后移到 `Phase 6`

## 建议调用顺序

1. `StorySessionContextAssembler`
2. `DirectorSessionService`
3. `SelectorSessionService`
4. `WriterSessionService`
5. `ReviewerSessionService`
6. trace 收口

## 当前阶段判断

- `Phase 5.1` 已完成
- `Phase 5.2` 已完成
- `Phase 5.3` 已完成首轮真实联调
- `Phase 5.4` 已完成真实联调收口
- 当前重点不再是扩 `Phase 5`，而是进入 `Phase 6` 的真实 scene 承接

## 建议代码落点

- 合同层：
  - `backend/modules/story-generation/src/main/java/com/storyweaver/story/generation/orchestration/*`
- 实现层：
  - `backend/src/main/java/com/storyweaver/story/generation/orchestration/impl/*`
- 测试：
  - `backend/src/test/java/com/storyweaver/story/generation/orchestration/impl/*`

## 验证方式

- 根工程编译：
  - `mvn -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -DskipTests compile`
- 当前最小回归：
  - `DefaultStorySessionContextAssemblerTest`
  - `DefaultStorySessionOrchestratorTest`
  - `StorySessionOrchestrationControllerTest`

## 下一步

1. 启动 `Phase 6` 详细计划
2. 接入真实 `SceneExecutionState` 查询与承接
3. 开始章节骨架与场景执行读模型落地
