# Phase 6 场景绑定与章节骨架联调报告（第二轮）

- Report ID: REPORT-20260418-phase6-scene-skeleton-live-validation-round2
- Related Req: REQ-20260411-stateful-story-platform-upgrade
- Related Plan:
  - `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-phase6-scene-skeleton-and-execution-v1.md`
- Created At: 2026-04-18 Asia/Shanghai
- Author: Codex

## 背景

第一轮联调中，`chapter 32 / 34` 的 `preview` 与 `chapter 32` 的 `skeleton-preview` 因 `ChapterStoryUnitAssembler` 使用 `List.of(null, ...)` 触发 `NullPointerException`，导致返回 `500`。

本轮目标是验证修复版部署后：

- `chapter 32 / 34` 不再返回 `500`
- `sceneBindingContext` 继续维持真实绑定语义
- `skeleton-preview` 在多章节下稳定返回
- 尝试寻找真实 `CHAPTER_COLD_START` 样本

## 联调环境

- Target Service: `story-weaver-backend`
- Access Path: 容器内 `http://127.0.0.1:8080`
- Auth: 现网管理员 Bearer Token
- Validation Time: 2026-04-18 Asia/Shanghai

## 联调结果

### 1. `chapter 31` 不存在 sceneId 的回退链路通过

- 请求：
  - `GET /api/story-orchestration/projects/28/chapters/31/preview?sceneId=scene-999`
- 结果：
  - `HTTP 200`
  - `contextPacket.sceneBindingContext.mode = SCENE_FALLBACK_TO_LATEST`
  - `resolvedSceneId = scene-1`
  - `trace.items[0].status = SKIPPED`
  - `trace.items[0].retryable = true`

结论：

- `SCENE_FALLBACK_TO_LATEST` 已在线上真实生效
- 当前 trace 已足够表达“请求 scene 未命中，已回退到最近 scene”这类诊断信息

### 2. `chapter 31` 章节骨架继续稳定返回

- 请求：
  - `GET /api/story-orchestration/projects/28/chapters/31/skeleton-preview`
- 结果：
  - `HTTP 200`
  - `sceneCount = 4`
  - 已保留 3 个兼容型既有 scene
  - 追加 1 个 `PLANNED` scene

### 3. `chapter 32` preview 与 skeleton-preview 全部恢复

- 请求：
  - `GET /api/story-orchestration/projects/28/chapters/32/preview?sceneId=scene-1`
  - `GET /api/story-orchestration/projects/28/chapters/32/skeleton-preview`
- 结果：
  - 均为 `HTTP 200`
  - `preview` 中 `sceneBindingContext.mode = SCENE_BOUND`
  - `skeleton-preview.sceneCount = 3`

结论：

- `chapter.summary = null` 的兼容修复已经在线上生效
- `ChapterStoryUnitAssembler` 不再因为空摘要触发 `500`

### 4. `chapter 34` preview 与 skeleton-preview 全部恢复

- 请求：
  - `GET /api/story-orchestration/projects/28/chapters/34/preview?sceneId=scene-1`
  - `GET /api/story-orchestration/projects/28/chapters/34/skeleton-preview`
- 结果：
  - 均为 `HTTP 200`
  - `preview` 中 `sceneBindingContext.mode = SCENE_BOUND`
  - `skeleton-preview.sceneCount = 3`

## 关于 `CHAPTER_COLD_START`

本轮继续尝试在 `旧日王座` 项目中寻找真实 `CHAPTER_COLD_START` 样本，但当前联调范围内的章节 `31-34` 均已有历史写作记录，因此：

- 当前项目样本已验证：
  - `SCENE_BOUND`
  - `SCENE_FALLBACK_TO_LATEST`
- 当前项目样本尚未验证：
  - `CHAPTER_COLD_START`

这不是代码失败，而是当前样本不足。

## 结论

- `Phase 6.1` 当前已完成真实联调收口：
  - `SCENE_BOUND` 已验证
  - `SCENE_FALLBACK_TO_LATEST` 已验证
- `Phase 6.2` 当前已完成真实联调收口：
  - `chapter 31 / 32 / 34` 的 `skeleton-preview` 全部返回 `200`
  - `ChapterSkeleton` 在多章节下均能稳定产出 `3-5` 个镜头骨架
- 当前剩余未验证项只剩：
  - 真实 `CHAPTER_COLD_START` 样本

## 下一步建议

1. 正式进入 `Phase 6.3`
2. 开始接 `SceneHandoffSnapshot` 与镜头级最小写回
3. 后续如有新的空章/无历史记录章节，再补 `CHAPTER_COLD_START` 真实联调

## 贡献与署名说明

- 需求方向与验证目标由用户提出和主导
- 本报告的排查、联调执行、结果整理与阶段判断由 Codex 完成
