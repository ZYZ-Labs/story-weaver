# Story 平台升级 Phase 6 详细实施计划：章节骨架与镜头执行

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Plan ID: PLAN-REQ-20260411-stateful-story-platform-upgrade-phase6-scene-skeleton-and-execution-v1
- Status: Planned
- Created At: 2026-04-17 Asia/Shanghai
- Updated At: 2026-04-17 Asia/Shanghai

## 本轮目标

在 `Phase 5` 已完成多 session 最小编排壳之后，开始把“整章一把写”的工作流推进到“章节骨架 + 镜头执行 + 显式交接”。

`Phase 6` 不再解决 `director / selector / writer / reviewer` 有没有，而是解决：

- 章节先被拆成什么骨架
- 每个镜头如何显式承接上一个镜头
- `sceneId` 如何真正绑定到真实 `SceneExecutionState`
- 多镜头执行后的章节级审校怎么做

一句话说：

- `Phase 5` 解决“编排壳”
- `Phase 6` 解决“镜头怎么连起来”

## 当前阶段定位

- `Phase 6` 是 `Phase 5` 的直接延续
- `Phase 5` 已完成四 session 编排壳与 trace 诊断口径
- 当前真实联调已经证明：
  - `sceneBindingContext` 协议成立
  - 但线上仍处于 `SCENE_QUERY_UNAVAILABLE`
- 所以 `Phase 6` 第一优先级不是继续扩 prompt，而是接入真实 scene 状态读模型

## 本轮原则

- 单章先拆 `3-5` 个镜头，不做无限细拆
- 镜头之间必须有显式 handoff state
- 先做读模型和执行预览，再考虑真实写回增强
- 骨架优先于正文扩写
- `reader reveal` 继续作为硬边界参与镜头执行
- 不在本阶段引入复杂增量状态系统，那属于 `Phase 7`

## 范围内

- `ChapterSkeleton`
- `SceneSkeletonItem`
- `SceneExecutionState`
- `SceneHandoffSnapshot`
- `SceneExecutionStateQueryService`
- `SceneExecutionStateWriteService`
- 章节总导产出镜头骨架
- 镜头级 `director / selector / writer / reviewer` 执行顺序
- 镜头结束后的最小状态写回
- 章节级 trace 串联与章节级审校

## 范围外

- 不在本阶段实现背包、技能、态度、好感等增量状态系统
- 不在本阶段重构前端为完整镜头工作台
- 不在本阶段引入 MCP 写回工具
- 不在本阶段做最终迁移与灰度切换

## 分阶段实施拆分

### `Phase 6.1` Scene 状态读模型与真实绑定

目标：

- 让 `sceneId` 不再停留在协议层
- 让 `StorySessionContextAssembler` 能从真实数据绑定 scene 上下文

交付：

- `SceneExecutionState` 统一读模型
- `SceneExecutionStateQueryService` 真实实现
- `listChapterScenes / getSceneState / findLatestChapterScene`
- `sceneBindingContext` 从 `SCENE_QUERY_UNAVAILABLE` 升级为真实绑定语义

退出条件：

- `SCENE_BOUND / SCENE_FALLBACK_TO_LATEST / CHAPTER_COLD_START` 能在真实数据下出现

当前进展：

- 已启动
- 已落兼容型 `SceneExecutionStateQueryService`：
  - `backend/src/main/java/com/storyweaver/storyunit/service/impl/DefaultSceneExecutionStateQueryService.java`
- 当前实现策略：
  - 先基于现有 `AIWritingRecord` 历史记录衍生 scene 读模型
  - 每条有正文产出的写作记录映射为一个兼容型 scene
  - `scene-1 / scene-2 / scene-3` 按章节内写作记录时间顺序生成
- 当前已支持：
  - `getSceneState`
  - `listChapterScenes`
  - `findLatestChapterScene`
- 当前已补 mapper：
  - `AIWritingRecordMapper.findByChapterId`
- 当前状态映射：
  - `accepted / completed / success -> COMPLETED`
  - `rejected / failed / error -> FAILED`
  - 其余按兼容规则映射到 `WRITING / REVIEWING / BLOCKED / WRITTEN / PLANNED`
- 当前已补本地回归：
  - `DefaultSceneExecutionStateQueryServiceTest`
  - `DefaultStorySessionContextAssemblerTest`
  - `DefaultStorySessionOrchestratorTest`
  - `StorySessionOrchestrationControllerTest`

当前判断：

- `Phase 6.1` 已到可部署联调阶段
- 这版不是最终镜头状态系统，而是 `AIWritingRecord -> SceneExecutionState` 的兼容读模型
- 其价值是先让：
  - `SCENE_BOUND`
  - `SCENE_FALLBACK_TO_LATEST`
  - `CHAPTER_COLD_START`
  在真实环境里开始出现

### `Phase 6.2` ChapterSkeleton 与镜头骨架生成

目标：

- 让章节总导先产出一条镜头骨架

交付：

- `ChapterSkeleton`
- `SceneSkeletonItem`
- `ChapterSkeletonPlanner`
- 第一版章节骨架预览接口

退出条件：

- 单章可稳定产出 `3-5` 个镜头骨架
- 每个镜头至少包含：
  - `sceneId`
  - `goal`
  - `readerReveal`
  - `mustUseAnchors`
  - `stopCondition`
  - `targetWords`

### `Phase 6.3` 镜头级执行与 handoff 写回

目标：

- 让四 session 按镜头执行，而不是只做整章 preview

交付：

- 镜头级 orchestrator 执行入口
- `SceneHandoffSnapshot`
- 每个镜头结束后的最小写回
- 下一个镜头可消费上一个镜头的 handoff

退出条件：

- `scene-1 -> scene-2 -> scene-3` 的承接不再依赖隐式 prompt
- `writerExecutionBrief` 能消费真实 handoff 信息

### `Phase 6.4` 章节级审校与联调收口

目标：

- 在多镜头串联后补章节级收口

交付：

- 章节级 reviewer
- 章节级 trace 汇总
- 章节级浏览器/API 联调报告

退出条件：

- 至少一个真实章节能完成：
  - 骨架生成
  - 镜头执行
  - handoff 写回
  - 章节级审校

## 建议调用顺序

1. `ChapterSkeletonPlanner`
2. `SceneExecutionStateQueryService`
3. 镜头级 `StorySessionOrchestrator`
4. `SceneExecutionStateWriteService`
5. 章节级 `Reviewer`
6. 章节级 trace 汇总

## 当前阶段判断

- `Phase 5` 已完成
- `Phase 6` 还未开始编码
- 当前第一优先级不是再修 `Phase 5`，而是把真实 scene 状态接进来

## 建议代码落点

- 合同层：
  - `backend/modules/story-generation/src/main/java/com/storyweaver/story/generation/orchestration/*`
  - `backend/modules/story-storyunit/src/main/java/com/storyweaver/storyunit/context/*`
- 实现层：
  - `backend/src/main/java/com/storyweaver/story/generation/orchestration/impl/*`
  - `backend/src/main/java/com/storyweaver/storyunit/context/impl/*`
- 测试：
  - `backend/src/test/java/com/storyweaver/story/generation/orchestration/impl/*`
  - `backend/src/test/java/com/storyweaver/storyunit/context/impl/*`

## 验证方式

- 根工程编译：
  - `mvn -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -DskipTests compile`
- 最小回归：
  - `SceneExecutionStateQueryService` 相关测试
  - `ChapterSkeletonPlanner` 相关测试
  - 镜头级 orchestrator 测试
- 真实联调：
  - `旧日王座` 项目章节级 API/浏览器联调

## 风险

- 如果真实 scene 状态读模型字段过少，镜头承接仍会偏粗
- 如果章节骨架拆得过细，token 与复杂度会快速膨胀
- 如果先做写回再做读模型，容易把错误状态写死

## 下一步

1. 先做 `Phase 6.1`
2. 接入真实 `SceneExecutionStateQueryService`
3. 再做 `Phase 6.2` 的章节骨架生成

## 贡献与署名说明

- 需求方向与产品判断由用户提出和主导
- 本文档结构化拆分、工程化范围界定与实施顺序整理由 Codex 完成
