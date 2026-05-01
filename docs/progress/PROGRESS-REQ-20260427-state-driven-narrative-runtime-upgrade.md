# 状态驱动叙事 Runtime 架构升级进度

- Req ID: `REQ-20260427-state-driven-narrative-runtime-upgrade`
- Updated At: 2026-04-27 Asia/Shanghai
- Status: In Progress

## 当前状态

- 已确认当前章节工作区的根问题不再只是 continuity 漂移或多 scene 约束不足，而是“正文先行、状态后补”的主链方向不对。
- 已明确新的架构方向：
  - 事实真相层：`intent -> resolve -> event/state -> checkpoint`
  - 文本层：`narrative render`
  - 规划层：`causality / node skeleton / authoring`
- 已新增第一批 runtime 基础模型与 store 接口：
  - `StoryActionIntent`
  - `StoryResolvedTurn`
  - `StoryNodeCheckpoint`
  - `StoryOpenLoop`
  - `StoryActionIntentStore`
  - `StoryResolvedTurnStore`
  - `StoryNodeCheckpointStore`
  - `StoryOpenLoopStore`
- `ResilientStoryStateStore` 已扩展为支持这四类 runtime 对象的 Redis + 内存 fallback 持久化，作为 node mode 的状态底座第一批落点。
- `story-generation` 侧已新增 node mode 契约对象：
  - `ChapterNodeSkeleton`
  - `StoryNodeSkeletonItem`
  - `NodeActionOption`
  - `NodeActionRequest`
  - `NodeResolutionResult`
- `story-orchestration` 已新增 node mode 最小入口：
  - `node-preview`
  - `node-actions/resolve`
- 章节工作区已接入 node runtime 预览面板，并补上 chapter-level mode 真源：
  - 章节详情现在会返回 `narrativeRuntimeMode`
  - `scene mode / node mode` 按章节单独存储
  - `node resolve` 仍默认关闭，但即使后续打开全局开关，也必须先把章节切到 `node mode` 才允许推进
  - `scene draft / accept / rollback` 已在后端绑定 `scene mode`，不会再与 `node mode` 混写。

## 最近关键结论

- 证据 1：
  - 现有 `Causality` 仅是项目级 CRUD，核心字段仍围绕 `causeEntity / effectEntity / relationship / conditions / payoffStatus`。
  - 现有 `CausalityServiceImpl` 也只做实体标准化和权限校验，没有运行态裁决能力。
- 结论 1：
  - `Causality` 不适合作为 runtime 因果主链，后续应降级为 authoring layer 的依赖/伏笔元数据。

- 证据 2：
  - 当前章节工作区虽然已有 `StoryEvent / StorySnapshot / StoryPatch / ReaderRevealState / ChapterIncrementalState`，但事实推进依旧依赖 scene 正文接纳。
  - continuity hard check 需要不断从文本中反推时间、人名、承接事实，随着镜头变长会不断累积不稳定性。
- 结论 2：
  - 系统需要单独的 runtime 真相层对象，承接“动作意图、结算结果、checkpoint、悬念线”，不能继续只靠正文反推。

- 证据 3：
  - `DefaultChapterNodeRuntimeService.resolve()` 当前会真实写入 `StoryActionIntent / StoryResolvedTurn / StoryNodeCheckpoint / StoryOpenLoop / ReaderRevealState / ChapterIncrementalState`。
  - 章节工作区的正文生成与镜头接纳主链仍然是 scene mode。
- 结论 3：
  - 在没有 chapter-level mode 切换前，不能直接把 node resolve 和 scene mode 同时对同一章节开放写入；必须先用兼容开关把 node runtime 控制在“预览开放、推进默认关闭”。

- 证据 4：
  - `AIWritingServiceImpl.prepareGeneration()`、`acceptGeneratedContent()`、scene rollback，以及 `StorySessionOrchestrationController.preview/execute` 仍是 scene 链的真实入口。
  - 仅在 `StorySessionOrchestrationController` 上加 node 开关，不足以阻止同一章节继续通过 `AIWritingController` 写入 scene 真相链。
- 结论 4：
  - chapter-level mode 必须成为章节工作区的统一裁判，后端要同时拦住：
    - `scene draft / accept / rollback`
    - `story-orchestration preview / execute`
    - `node resolve`
  - 不能只做前端按钮禁用。

## 已完成动作

- 文档：
  - 新建 `REQ-20260427-state-driven-narrative-runtime-upgrade`
  - 新建 `PLAN-REQ-20260427-state-driven-narrative-runtime-upgrade-v1`
  - 新建 `ARCH-REQ-20260427-state-driven-narrative-runtime-v1`
- 代码：
  - `storyunit/runtime` 下新增 `StoryActionIntent / StoryResolvedTurn / StoryNodeCheckpoint / StoryOpenLoop / StoryLoopStatus`
  - `storyunit/service` 下新增对应 store 接口
  - `StoryStateProperties` 新增 intent/turn/checkpoint/open-loop 的 key prefix 与 manifest prefix
  - `ResilientStoryStateStore` 新增这四类 runtime 对象的 Redis 和内存 fallback 持久化
  - `ResilientStoryStateStoreTest` 新增这四类 runtime 对象的最小回退读写验证
  - `story-generation/orchestration` 下新增 node mode 协议对象，固定后续章节工作区 controller / service 的输入输出形状
  - `DefaultChapterNodeRuntimeService` 已落最小 node resolver，并通过 `scene skeleton -> node skeleton` 适配现有骨架
  - `StorySessionOrchestrationController` 已新增 `node-preview / node-actions/resolve`
  - `StoryCompatibilityProperties` 已新增：
    - `chapterWorkspaceNodePreviewEnabled`
    - `chapterWorkspaceNodeResolveEnabled`
  - `DefaultMigrationCompatibilitySnapshotService` 已把 node preview / resolve 开关暴露到 `featureFlags`
  - 已新增 `ChapterNarrativeRuntimeMode / ChapterNarrativeRuntimeModeService`
  - 章节级运行模式通过 `system_config` 持久化，默认 `scene`，按章节返回到 `Chapter.narrativeRuntimeMode`
  - `ChapterController` 已新增 `PUT /api/projects/{projectId}/chapters/{chapterId}/runtime-mode`
  - `StorySessionOrchestrationController` 已新增章节级 mode 保护：
    - `preview / execute` 只允许 `scene mode`
    - `node-actions/resolve` 只允许 `node mode`
  - `AIWritingServiceImpl` 和 `ChapterWorkspaceAcceptedSceneRollbackService` 已新增章节级 mode 保护：
    - `phase8.chapter-workspace.scene-draft` 的生成、接纳、回滚都只允许 `scene mode`
  - 章节工作区已新增运行模式切换 UI：
    - 支持 `scene mode / node mode`
    - `node mode` 只在全局 resolve 开关打开时可选
    - 切到 `node mode` 后会停用 scene 草稿主链，node runtime 面板升级为当前章节主链
  - 章节工作区已接入 node runtime 只读面板：
    - 显示当前 node 链、推荐动作、latest checkpoint、active loops
    - 默认根据兼容开关禁用 `node resolve`

## 下一步动作

- 把 narrator render 接到 node runtime：
  - 先结算 node
  - 再渲染正文
  - 重试只重渲染，不改事实
- 设计 node mode 的 checkpoint 回档：
  - 明确 node rollback 与现有 accepted scene rollback 的边界
  - 不要让两套回退链共享一个模糊入口
- 评估 checkpoint 回档如何与已存在的 accepted scene rollback 合并或并存。

## 阻塞项

- 当前无新增代码阻塞
- 当前 chapter-level mode 已落地，但仍有一个明确边界：
  - 已存在 accepted scene 前缀的章节不能直接切到 `node mode`
  - 已存在 node checkpoint / turn 的章节不能直接切回 `scene mode`
  - 这是为了避免两套真相链对同一章节交叉覆写，后续如果需要迁移，必须走专门的 mode migration，而不是直接切开关
- controller 层的 Mockito 测试仍受本机 JVM attach 环境限制，当前只能通过：
  - `mvn -pl backend -am -DskipTests compile`
  - 非 Mockito 的最小服务级测试
  来确认代码未回退
- 当前验证策略已与用户确认：
  - 不再为本机 Mockito / JVM attach 测试单独耗时兜环境
  - 开发阶段以编译通过、非 Mockito 的最小链路验证、前端类型检查为准
  - 最终浏览器与部署联调由用户侧环境完成
- 章节工作区目前已经具备 chapter-level mode 切换，但 node mode 仍缺 narrator render，所以现在只是“状态推进主链已切换”，还不是完整的 node mode 写作闭环
