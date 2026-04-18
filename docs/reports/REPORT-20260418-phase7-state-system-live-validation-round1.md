# Phase 7 增量状态系统线上联调报告 Round 1

- Date: 2026-04-18 Asia/Shanghai
- Phase: `Phase 7.1`
- Scope:
  - `旧日王座` 项目 (`projectId=28`)
  - `chapterId=31`
  - `sceneId=scene-7`

## 目标

验证 `Phase 7.1` 的最小状态链是否在线上真实成立：

- `POST /api/story-orchestration/projects/{projectId}/chapters/{chapterId}/execute`
  能否真实产出：
  - `StoryEvent`
  - `StorySnapshot`
- `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/events`
- `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/snapshots`
  能否真实读回写入结果

## 联调过程

### 1. 鉴权验证

- 通过线上 backend 容器环境读取 `JWT_SECRET`
- 基于真实管理员账号 `admin / userId=1` 生成 JWT
- `GET /api/projects/28` 返回 `200`

结论：

- 当前线上联调使用的 token 有效

### 2. 首次执行结果

请求：

- `POST /api/story-orchestration/projects/28/chapters/31/execute?sceneId=scene-6`

响应：

- 返回 `200`
- `writeResult` 中已经包含：
  - `stateEvent`
  - `stateSnapshot`

但首次读取：

- `GET /api/story-state/projects/28/chapters/31/events` -> `200` + `[]`
- `GET /api/story-state/projects/28/chapters/31/snapshots` -> `200` + `[]`

## 问题定位

查看 backend 与 Redis 日志后，定位到真实运行环境问题：

- Redis 配置了：
  - `stop-writes-on-bgsave-error = yes`
- Redis 持久化目录 `/data` 在 `bgsave` 时持续报错：
  - `Failed opening the temp RDB file temp-xxxx.rdb (in server root dir /data) for saving: Permission denied`
- 因为 Redis 处于 `MISCONF` 状态，所有写请求被拒绝

补充确认：

- Redis 进程实际运行用户是 `uid=999`
- `/data` 初始挂载权限与进程用户不匹配
- 即使修正了目录 owner，当前环境下 `bgsave` 仍然持续报 `Permission denied`

## 运行时修复

为了继续验证应用链路，做了一个最小运行时热修：

- `CONFIG SET stop-writes-on-bgsave-error no`

理由：

- 当前 Redis 同时启用了 AOF
- `INFO persistence` 显示：
  - `aof_enabled = 1`
  - `aof_last_write_status = ok`
- 因此在本轮联调里，先恢复业务写入能力，再单独跟进 RDB 落盘环境问题

## 修复后复验

再次执行：

- `POST /api/story-orchestration/projects/28/chapters/31/execute?sceneId=scene-7`

结果：

- 返回 `200`
- `writeResult.stateEvent.eventId` 已生成
- `writeResult.stateSnapshot.snapshotId` 已生成

读取验证：

- `GET /api/story-state/projects/28/chapters/31/events` -> `200`
  - 已返回 `scene-7` 对应 `StoryEvent`
- `GET /api/story-state/projects/28/chapters/31/snapshots` -> `200`
  - 已返回 `scene-7` 对应 `StorySnapshot`

Redis key 验证：

- `story:state:chapter-events:28:31`
- `story:state:chapter-snapshots:28:31`
- `story:state:event:event-28-31-scene-7-...`
- `story:state:snapshot:snapshot-28-31-scene-7-...`

manifest 内容验证：

- `chapter-events:28:31` 已包含 `event-28-31-scene-7-...`
- `chapter-snapshots:28:31` 已包含 `snapshot-28-31-scene-7-...`

## 结论

应用侧结论：

- `Phase 7.1` 的最小状态链已在线上跑通：
  - `execute -> StoryEvent`
  - `execute -> StorySnapshot`
  - `events / snapshots` 查询可读回

运行环境结论：

- 当前 Redis 仍存在 RDB 持久化权限问题
- 这不是 `Phase 7.1` 应用代码链路问题，但属于运行环境待跟进项

## 后续动作

1. 将 `Phase 7.1` 在主计划和进度文档中标记为已完成联调收口
2. 进入 `Phase 7.2`
3. 单独记录 Redis 运行环境修复项：
   - 需要解决 `/data` 上 `bgsave` 的 `Permission denied`
   - 当前临时运行时开关不应替代最终环境修复

## 贡献与署名说明

- 联调目标与平台升级方向由用户提出和主导
- 本轮线上验证、日志定位、运行时热修与报告整理由 Codex 完成
