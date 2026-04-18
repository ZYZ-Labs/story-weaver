# Story 平台升级 Phase 6 详细实施计划：章节骨架与镜头执行

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Plan ID: PLAN-REQ-20260411-stateful-story-platform-upgrade-phase6-scene-skeleton-and-execution-v1
- Status: In Progress
- Created At: 2026-04-17 Asia/Shanghai
- Updated At: 2026-04-18 Asia/Shanghai

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

当前进展：

- 已启动
- 已落最小合同：
  - `backend/modules/story-generation/src/main/java/com/storyweaver/story/generation/orchestration/ChapterSkeleton.java`
  - `backend/modules/story-generation/src/main/java/com/storyweaver/story/generation/orchestration/SceneSkeletonItem.java`
  - `backend/modules/story-generation/src/main/java/com/storyweaver/story/generation/orchestration/ChapterSkeletonPlanner.java`
- 已落规则规划器：
  - `backend/src/main/java/com/storyweaver/story/generation/orchestration/impl/RuleBasedChapterSkeletonPlanner.java`
- 当前实现策略：
  - 先复用 `StorySessionContextAssembler`
  - 先消费 `director candidates + selection decision`
  - 先保留既有 `SceneExecutionState`
  - 再为未执行镜头补 `PLANNED` 骨架
- 已开放最小预览接口：
  - `GET /api/story-orchestration/projects/{projectId}/chapters/{chapterId}/skeleton-preview`
- 当前已补本地回归：
  - `RuleBasedChapterSkeletonPlannerTest`
  - `StorySessionOrchestrationControllerTest`

当前判断：

- `Phase 6.2` 已到可部署联调阶段
- 这版不是最终骨架引擎，而是第一版只读 `ChapterSkeleton` 预览
- 当前价值是先把章节骨架正式接进真实部署链路

线上联调结果补充：

- 第一轮联调已验证：
  - `chapter 31 + scene-1` -> `SCENE_BOUND`
  - `chapter 31 + scene-2` -> `SCENE_BOUND`
  - `chapter 31 + scene-999` -> `SCENE_FALLBACK_TO_LATEST`
  - `chapter 31 skeleton-preview` -> `200`
- 第一轮暴露缺陷：
  - `chapter 32 / 34 preview` 与 `chapter 32 skeleton-preview` 因 `ChapterStoryUnitAssembler` 处理 `chapter.summary = null` 触发 `500`
- 当前已完成本地修复：
  - `ChapterStoryUnitAssembler` 已改为 `compactStrings(...)`
  - 已新增 `ChapterStoryUnitAssemblerTest`
- 第二轮联调已验证：
  - `chapter 32 + scene-1` -> `SCENE_BOUND`
  - `chapter 32 skeleton-preview` -> `200`
  - `chapter 34 + scene-1` -> `SCENE_BOUND`
  - `chapter 34 skeleton-preview` -> `200`
- 当前结论：
  - `Phase 6.1 / 6.2` 主链已收口
  - 当前只剩 `CHAPTER_COLD_START` 缺少真实样本，不构成继续进入 `Phase 6.3` 的阻塞

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

当前进展：

- 已启动
- 已新增最小协议：
  - `SceneHandoffSnapshot`
  - `SceneRuntimeStateStore`
  - `SceneExecutionWriteService`
  - `SceneExecutionWriteResult`
  - `StorySessionExecution`
  - `SceneExecutionRequest`
- 已新增 backend 实现：
  - `ResilientSceneRuntimeStateStore`
  - `DefaultSceneExecutionWriteService`
  - `DefaultStorySessionOrchestrator.execute(...)`
- 已完成 `StorySessionContextPacket` 扩展：
  - `previousSceneHandoff`
- 已完成 `DefaultStorySessionContextAssembler` 承接补齐：
  - 可读取目标 `sceneId` 对应的上一镜头 handoff
- 已完成 `DefaultWriterExecutionBriefBuilder` 承接补齐：
  - 优先消费 `previousSceneHandoff`
- 已新增最小执行入口：
  - `POST /api/story-orchestration/projects/{projectId}/chapters/{chapterId}/execute`
- 已新增联调样本设计文档：
  - `docs/test-data/TESTDATA-20260418-old-throne-phase6-regression-samples-v1.md`
- 已完成本地回归：
  - `DefaultSceneExecutionStateQueryServiceTest`
  - `DefaultStorySessionContextAssemblerTest`
  - `DefaultSceneExecutionWriteServiceTest`
  - `RuleBasedChapterSkeletonPlannerTest`
  - `StorySessionOrchestrationControllerTest`
  - `DefaultStorySessionOrchestratorTest`

当前判断：

- `Phase 6.3` 已到可部署联调阶段
- 当前实现是“最小执行写回”，不是完整多镜头连续执行
- 这一步的价值是先让：
  - 当前镜头执行结果真实写入 runtime state
  - 下一镜头能读取显式 handoff
  - 后续 `Phase 6.4` 有章节级联调基础

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
- `Phase 6.1` 已完成真实联调收口
- `Phase 6.2` 已完成真实联调收口
- 当前第一优先级已切换到 `Phase 6.3` 的镜头级 handoff 写回

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

1. 部署联调 `POST /api/story-orchestration/projects/{projectId}/chapters/{chapterId}/execute`
2. 验证 `scene runtime state` 与 `handoff snapshot` 的真实写回
3. 在后续新样本中继续补齐 `CHAPTER_COLD_START` 真实联调
4. 为 `Phase 6.4` 准备章节级联调样本

## 贡献与署名说明

- 需求方向与产品判断由用户提出和主导
- 本文档结构化拆分、工程化范围界定与实施顺序整理由 Codex 完成
