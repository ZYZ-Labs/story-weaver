# Phase 7 增量状态系统线上联调报告 Round 2

- Date: 2026-04-18 Asia/Shanghai
- Phase: `Phase 7.2`
- Scope:
  - `旧日王座` 项目 (`projectId=28`)
  - `chapterId=31`
  - `sceneId=scene-8`

## 目标

验证 `Phase 7.2` 的最小状态链是否在线上真实成立：

- `POST /api/story-orchestration/projects/{projectId}/chapters/{chapterId}/execute`
  能否真实产出：
  - `StoryPatch`
  - `ReaderRevealState`
  - `Chapter State Snapshot`
- `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/patches`
- `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/reader-reveal-state`
  能否真实读回写入结果
- `GET /api/story-context/projects/{projectId}/chapters/{chapterId}/reader-known-state`
  是否开始优先消费状态存储

## 联调过程

### 1. 鉴权验证

- 通过当前容器环境 `JWT_SECRET` 生成管理员 JWT
- `GET /api/projects/28` 返回 `200`

结论：

- 当前联调 token 有效

### 2. 执行写回

请求：

- `POST /api/story-orchestration/projects/28/chapters/31/execute?sceneId=scene-8`

响应：

- 返回 `200`
- `writeResult` 中已包含：
  - `stateEvent`
  - `stateSnapshot`
  - `statePatch`
  - `readerRevealState`
  - `chapterStateSnapshot`

### 3. 首次读取现象

首次把 `execute` 与 `events / snapshots / patches / reader-reveal-state` 并发打到线上时：

- `events / snapshots` 读到旧值
- `patches` 返回 `[]`
- `reader-reveal-state` 返回 `null`

结论：

- 这不是应用 bug，而是本轮验证中的请求竞态
- 写入已在后端执行中完成，但并发读取先命中了旧 manifest

### 4. Redis 验证

直接检查 Redis 后确认：

- 已存在 key：
  - `story:state:chapter-events:28:31`
  - `story:state:chapter-snapshots:28:31`
  - `story:state:chapter-patches:28:31`
  - `story:state:chapter-reveal:28:31`
- manifest 已包含新增 scene-8：
  - `event-28-31-scene-8-...`
  - `snapshot-28-31-scene-8-...`
  - `patch-28-31-scene-8-...`

### 5. 顺序复验

在 `execute` 完成后再次顺序读取：

- `GET /api/story-state/projects/28/chapters/31/events` -> `200`
  - 已返回：
    - `scene-7`
    - `scene-8`
- `GET /api/story-state/projects/28/chapters/31/snapshots` -> `200`
  - 已返回：
    - `scene-7` 的 scene snapshot
    - `scene-8` 的 scene snapshot
    - `scene-8` 的 chapter state snapshot
- `GET /api/story-state/projects/28/chapters/31/patches` -> `200`
  - 已返回 `patch-28-31-scene-8-...`
- `GET /api/story-state/projects/28/chapters/31/reader-reveal-state` -> `200`
  - 已返回 chapter reveal state
- `GET /api/story-context/projects/28/chapters/31/reader-known-state` -> `200`
  - `knownFacts` 已包含：
    - `两年沉寂后，主角收到旧战队邀请，命运再次启动。`

## 结论

应用侧结论：

- `Phase 7.2` 的最小状态链已在线上跑通：
  - `execute -> StoryPatch`
  - `execute -> ReaderRevealState`
  - `execute -> Chapter State Snapshot`
  - `patches / reader-reveal-state` 查询可读回
  - `story-context reader-known-state` 已开始优先消费状态存储

运行环境结论：

- Redis 的 `/data` RDB 权限问题仍是环境告警
- 但当前不再阻断 `Phase 7.2` 应用链路

## 后续动作

1. 将 `Phase 7.2` 在主计划和进度文档中标记为已完成联调收口
2. 进入 `Phase 7.3`
3. 扩展高优先级状态族：
   - Open Loops
   - Reader Reveal State 增量细化
   - 人物态度 / 情绪标签
   - 地点状态

## 贡献与署名说明

- 联调目标与平台升级方向由用户提出和主导
- 本轮线上验证、竞态识别、Redis 核验与报告整理由 Codex 完成
