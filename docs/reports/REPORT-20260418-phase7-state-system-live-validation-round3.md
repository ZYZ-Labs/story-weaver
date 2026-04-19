# Phase 7 状态系统线上联调报告 Round 3

- Report ID: REPORT-20260418-phase7-state-system-live-validation-round3
- Related Req: REQ-20260411-stateful-story-platform-upgrade
- Related Plan:
  - `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-phase7-incremental-state-v1.md`
- Created At: 2026-04-18 Asia/Shanghai
- Author: Codex

## 联调范围

围绕 `Phase 7.3` 的章节级状态聚合链路进行真实联调，验证：

- `POST /api/story-orchestration/projects/{projectId}/chapters/{chapterId}/execute`
- `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/chapter-state`
- `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/patches`
- `GET /api/story-context/projects/{projectId}/chapters/{chapterId}/reader-known-state`

验证项目与章节：

- project `28` `旧日王座`
- chapter `31`
- execute scene `scene-9`

## 联调结果

### 1. execute 返回 `200`

调用：

- `POST /api/story-orchestration/projects/28/chapters/31/execute?sceneId=scene-9`

结果：

- 返回 `200`
- `writeResult` 已真实包含：
  - `chapterStatePatch`
  - `chapterIncrementalState`
  - `chapterStateSnapshot`

关键返回：

- `chapterStatePatch.patchId = patch-chapter-state-28-31-scene-9-...`
- `chapterStatePatch.facetType = STATE`
- `chapterIncrementalState.openLoops = ["scene:scene-10:pending"]`
- `chapterIncrementalState.resolvedLoops = ["scene:scene-9:pending"]`
- `chapterIncrementalState.characterEmotions.林沉舟 = 低谷回归期`
- `chapterIncrementalState.characterAttitudes.林沉舟 = 重返职业赛场并证明自己`

### 2. 章节状态查询返回 `200`

调用：

- `GET /api/story-state/projects/28/chapters/31/chapter-state`

结果：

- 返回 `200`
- 已真实读回：
  - `openLoops`
  - `resolvedLoops`
  - `activeLocations`
  - `characterEmotions`
  - `characterAttitudes`
  - `characterStateTags`

### 3. patch 清单返回 `200`

调用：

- `GET /api/story-state/projects/28/chapters/31/patches`

结果：

- 返回 `200`
- 已可读回：
  - `patch-28-31-scene-9-...` `facetType=REVEAL`
  - `patch-chapter-state-28-31-scene-9-...` `facetType=STATE`

### 4. Redis 状态已同步落盘

已确认 Redis 存在：

- `story:state:chapter-state:28:31`
- `story:state:chapter-patches:28:31`

其中：

- `chapter-state` key 已保存 `ChapterIncrementalState`
- `chapter-patches` manifest 已包含 `STATE` patch id

### 5. reader-known-state 未回退

调用：

- `GET /api/story-context/projects/28/chapters/31/reader-known-state`

结果：

- 返回 `200`
- 当前仍稳定优先读取 reveal 状态存储，不受 `Phase 7.3` 新增章节状态聚合影响

## 发现与判断

### 初次并发读取出现空值/旧值

现象：

- 第一次把 `execute` 与 `chapter-state / patches` 并发打出时：
  - `chapter-state` 返回 `null`
  - `patches` 只读到旧 manifest

复验：

- 顺序重拉后：
  - `chapter-state` 返回完整状态
  - `patches` 返回 `REVEAL + STATE` 两类 patch
  - Redis key 与 manifest 内容一致

判断：

- 这是请求竞态，不是当前代码缺陷
- 当前链路写入顺序成立，真实状态已成功持久化

## 阶段结论

- `Phase 7.3` 已完成真实联调收口
- 当前已经具备：
  - `execute -> chapterStatePatch -> chapter-state query`
  - `execute -> STATE patch -> patches query`
  - `execute -> Redis chapter-state persistence`
- 下一步可进入 `Phase 7.4` 收口与阶段总结

## 贡献与署名说明

- 需求方向与测试目标由用户提出和主导
- 实际联调执行、结果分析与报告撰写由 Codex 完成
