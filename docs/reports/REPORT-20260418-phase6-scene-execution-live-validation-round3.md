# Phase 6 镜头执行与 handoff 写回联调报告（第三轮）

- Report ID: REPORT-20260418-phase6-scene-execution-live-validation-round3
- Related Req: REQ-20260411-stateful-story-platform-upgrade
- Related Plan:
  - `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-phase6-scene-skeleton-and-execution-v1.md`
- Created At: 2026-04-18 Asia/Shanghai
- Author: Codex

## 背景

`Phase 6.1 / 6.2` 已完成场景绑定与章节骨架的真实联调收口。本轮目标是验证 `Phase 6.3` 的最小镜头执行闭环是否已经在线上成立，也就是：

- `POST /api/story-orchestration/projects/{projectId}/chapters/{chapterId}/execute`
- 当前镜头执行结果是否真实写入 runtime state
- 下一镜头是否能够读取 `previousSceneHandoff`
- 执行后再次请求同一 `sceneId` 时，是否从回退语义升级为真实绑定语义

## 联调环境

- Target Service: `story-weaver-backend`
- Access Path: 容器内 `http://127.0.0.1:8080`
- Auth: 现网 `testuser` Bearer Token
- Validation Time: 2026-04-18 Asia/Shanghai

## 联调请求

1. `GET /api/story-orchestration/projects/28/chapters/31/skeleton-preview`
2. `GET /api/story-orchestration/projects/28/chapters/31/preview?sceneId=scene-4`
3. `POST /api/story-orchestration/projects/28/chapters/31/execute?sceneId=scene-4`
4. `GET /api/story-orchestration/projects/28/chapters/31/preview?sceneId=scene-5`
5. `GET /api/story-orchestration/projects/28/chapters/31/preview?sceneId=scene-4`

## 联调结果

### 1. 执行前的 `scene-4` 仍处于回退语义

- 请求：
  - `GET /api/story-orchestration/projects/28/chapters/31/preview?sceneId=scene-4`
- 结果：
  - `HTTP 200`
  - `contextPacket.sceneBindingContext.mode = SCENE_FALLBACK_TO_LATEST`
  - `resolvedSceneId = scene-1`
  - 当前没有 `previousSceneHandoff`

结论：

- 在执行前，`scene-4` 仍属于“计划中的下一镜头”
- 这符合当前 `scene-4` 尚未写入 runtime state 的预期

### 2. `execute` 已真实写入当前镜头执行结果

- 请求：
  - `POST /api/story-orchestration/projects/28/chapters/31/execute?sceneId=scene-4`
- 结果：
  - `HTTP 200`
  - 返回体包含：
    - `preview`
    - `writeResult`
    - `trace`
  - `writeResult.sceneExecutionState.sceneId = scene-4`
  - `writeResult.sceneExecutionState.status = COMPLETED`
  - `writeResult.handoffSnapshot.fromSceneId = scene-4`
  - `writeResult.handoffSnapshot.toSceneId = scene-5`
  - `trace` 中存在：
    - `stepKey = scene-writeback`
    - `status = COMPLETED`

结论：

- `Phase 6.3` 的最小执行入口已经在线上真实写入当前镜头运行态
- 当前镜头结束后，已经生成可供下一镜头消费的 `handoffSnapshot`

### 3. 下一镜头已可消费显式 handoff

- 请求：
  - `GET /api/story-orchestration/projects/28/chapters/31/preview?sceneId=scene-5`
- 结果：
  - `HTTP 200`
  - `contextPacket.sceneBindingContext.mode = SCENE_FALLBACK_TO_LATEST`
  - `resolvedSceneId = scene-4`
  - `contextPacket.previousSceneHandoff.fromSceneId = scene-4`
  - `contextPacket.previousSceneHandoff.toSceneId = scene-5`
  - `existingSceneStates` 已包含 `scene-4`
  - `writerExecutionBrief.continuityNotes` 已体现上一镜头 handoff 的连续性提示

结论：

- 下一镜头不再只能靠隐式 prompt 承接
- `previousSceneHandoff` 已进入真实上下文包，并影响 `writerExecutionBrief`

### 4. 执行后的 `scene-4` 已升级为真实绑定

- 请求：
  - `GET /api/story-orchestration/projects/28/chapters/31/preview?sceneId=scene-4`
- 结果：
  - `HTTP 200`
  - `contextPacket.sceneBindingContext.mode = SCENE_BOUND`
  - `resolvedSceneId = scene-4`
  - `existingSceneStates` 已包含 runtime 写回的 `scene-4`

结论：

- 当前镜头在执行写回后，已经不再走回退语义
- `SceneRuntimeStateStore` 与 `SceneExecutionStateQueryService` 的联动在线上已成立

## 结论

- `Phase 6.3` 的主链已完成真实联调验证：
  - `execute` 已真实写入当前 scene runtime state
  - 下一镜头已能读取显式 `previousSceneHandoff`
  - 当前镜头执行后会从 `SCENE_FALLBACK_TO_LATEST` 升级为 `SCENE_BOUND`
- 当前 `Phase 6` 的阶段状态应调整为：
  - `Phase 6.1` 已完成
  - `Phase 6.2` 已完成
  - `Phase 6.3` 已完成
  - 下一步进入 `Phase 6.4`

## 下一步建议

1. 正式进入 `Phase 6.4`
2. 开始做章节级 trace 汇总与 reviewer 收口
3. 继续补 `旧日王座` 联调样本，专门覆盖：
   - `CHAPTER_COLD_START`
   - 多镜头连续执行
   - 章节级 reviewer 结论

## 贡献与署名说明

- 需求方向与验证目标由用户提出和主导
- 本报告的联调执行、结果整理、阶段判断与文档撰写由 Codex 完成
