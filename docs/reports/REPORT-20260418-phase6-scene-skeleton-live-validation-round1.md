# Phase 6 Scene / Skeleton 线上联调 Round 1

- Report ID: REPORT-20260418-phase6-scene-skeleton-live-validation-round1
- Date: 2026-04-18 Asia/Shanghai
- Scope:
  - `Phase 6.1` scene 真实绑定语义
  - `Phase 6.2` 章节骨架只读预览

## 验收环境

- 服务容器：`story-weaver-backend`
- 联调方式：容器内直连 `127.0.0.1:8080`
- 项目样本：`旧日王座` `projectId=28`

## 已通过

- `GET /api/story-orchestration/projects/28/chapters/31/preview?sceneId=scene-1`
  - `200`
  - `sceneBindingContext.mode=SCENE_BOUND`
- `GET /api/story-orchestration/projects/28/chapters/31/preview?sceneId=scene-2`
  - `200`
  - `sceneBindingContext.mode=SCENE_BOUND`
- `GET /api/story-orchestration/projects/28/chapters/31/preview?sceneId=scene-999`
  - `200`
  - `sceneBindingContext.mode=SCENE_FALLBACK_TO_LATEST`
- `GET /api/story-orchestration/projects/28/chapters/31/skeleton-preview`
  - `200`
  - 返回 `ChapterSkeleton`
  - 当前产出 `4` 个镜头：
    - `scene-1` 到 `scene-3` 继承兼容型 `SceneExecutionState`
    - `scene-4` 作为新的 `PLANNED` 镜头

## 新发现问题

- `GET /api/story-orchestration/projects/28/chapters/32/preview?sceneId=scene-1`
  - `500`
- `GET /api/story-orchestration/projects/28/chapters/34/preview?sceneId=scene-1`
  - `500`
- `GET /api/story-orchestration/projects/28/chapters/32/skeleton-preview`
  - `500`

## 根因

- 异常不在 `ChapterSkeletonPlanner`
- 根因位于：
  - `backend/src/main/java/com/storyweaver/storyunit/assembler/ChapterStoryUnitAssembler.java`
- 真实数据里部分章节 `chapter.summary = null`
- 旧逻辑在 `ChapterSummaryFacetAssembler` 中使用：
  - `List.of(source.chapter().getSummary(), displayTitle)`
  - `List.of(source.chapter().getSummary(), abbreviate(...))`
- `List.of(null, ...)` 在 Java 21 下直接触发 `NullPointerException`

## 本地修复

- 已改为：
  - `compactStrings(source.chapter().getSummary(), displayTitle)`
  - `compactStrings(source.chapter().getSummary(), abbreviate(...))`
- 新增回归：
  - `backend/src/test/java/com/storyweaver/storyunit/assembler/ChapterStoryUnitAssemblerTest.java`

## 当前判断

- `Phase 6.1` 主链已通过
- `Phase 6.2` 主链已在 `chapter 31` 通过
- 当前阻塞点不是设计错误，而是章节摘要投影层的空值兼容缺陷
- 修复已完成，但需要重新部署后做 Round 2 复验

## 下一步

1. 部署包含 `ChapterStoryUnitAssembler` 空值修复的新版本
2. 复验：
   - `chapter 32 / 34 preview`
   - `chapter 32 skeleton-preview`
3. 继续寻找真实 `CHAPTER_COLD_START` 样本
