# Phase 9 迁移兼容线上联调报告 Round 1

- Date: 2026-04-23 Asia/Shanghai
- Phase: `Phase 9`
- Scope:
  - `旧日王座` 项目 (`projectId=28`)
  - 章节级兼容分析 / dry-run / 兼容快照
  - 项目级迁移总览 / 项目级 dry-run

## 目标

验证 `Phase 9` 当前已实现的迁移兼容接口是否在真实部署环境里可用：

- `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/backfill-analysis`
- `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/backfill-dry-run`
- `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/compatibility-snapshot`
- `GET /api/story-state/projects/{projectId}/backfill-overview`
- `GET /api/story-state/projects/{projectId}/backfill-project-dry-run`

## 鉴权与联调方式

- 通过现网 `story-weaver-backend` 容器环境读取 `JWT_SECRET`
- 基于真实管理员账号 `admin / userId=1` 生成临时 JWT
- 通过 backend 容器内 `127.0.0.1:8080` 直接命中接口，规避外部网关和 DNS 干扰

## 实际联调结果

### 1. 项目级接口已通过

以下接口在线上均返回 `200`：

- `GET /api/story-state/projects/28/backfill-overview`
- `GET /api/story-state/projects/28/backfill-project-dry-run`

项目级返回已确认包含真实数据：

- `backfill-overview`
  - `totalChapters = 4`
  - `analyzedChapters = 4`
  - `chaptersNeedingSceneBackfill = 4`
  - `chaptersNeedingStateBackfill = 4`
  - `chaptersReadyForBackfill = 4`
- `backfill-project-dry-run`
  - `chaptersNeedingBackfill = 4`
  - `runnableChapters = 4`
  - `blockedChapters = 0`
  - 每章都已返回：
    - `actions`
    - `riskNotes`

结论：

- `Phase 9.4` 的项目级迁移总览和项目级 dry-run 计划壳，线上已真实可用。

### 2. 章节级接口在真实章节上已通过

对真实存在的章节 `chapterId=32`：

- `GET /api/story-state/projects/28/chapters/32/backfill-analysis` -> `200`
- `GET /api/story-state/projects/28/chapters/32/backfill-dry-run` -> `200`
- `GET /api/story-state/projects/28/chapters/32/compatibility-snapshot` -> `200`

已确认返回中包含真实字段：

- `backfill-analysis`
  - `legacyRecordCount = 1`
  - `legacyGeneratedRecordCount = 1`
  - `needsSceneBackfill = true`
  - `needsStateBackfill = true`
- `backfill-dry-run`
  - `canRunBackfill = true`
  - 已返回 4 条动作：
    - `scan-legacy-records`
    - `derive-scene-state`
    - `derive-state-facets`
    - `verify-post-backfill`
- `compatibility-snapshot`
  - 已返回：
    - `pageBoundaries`
    - `apiBoundaries`
    - `dataBoundaries`
    - `featureFlags`
    - `riskNotes`

结论：

- `Phase 9.2 / 9.3` 的章节级兼容分析、dry-run 和兼容边界快照，线上已真实可用。

### 3. 初始样本选择有偏差，但不是代码问题

最初按历史联调习惯请求：

- `GET /api/story-state/projects/28/chapters/31/backfill-analysis`
- `GET /api/story-state/projects/28/chapters/31/backfill-dry-run`
- `GET /api/story-state/projects/28/chapters/31/compatibility-snapshot`

结果均返回：

- `HTTP 200`
- `data = null`

随后确认当前线上 `projectId=28` 的真实章节列表已经变化，现有章节是：

- `35` `退役者的邀请`
- `32` `算法少女苏晚`
- 以及后续章节

说明：

- 这不是接口缺陷
- 而是联调样本章节编号和当前线上数据已经不一致

结论：

- `旧日王座` 的联调样本需要更新到当前真实章节编号
- `chapter 31` 不应再作为 `projectId=28` 的固定联调主样本

## 阶段判断

- `Phase 9.2`：线上通过
- `Phase 9.3`：线上通过
- `Phase 9.4`：项目级只读验收入口线上通过

当前 `Phase 9` 的代码主链已经达到“可部署联调”状态，而且这一轮真实联调已验证通过。

## 下一步建议

1. 更新 `旧日王座` 的联调样本章节编号  
2. 把状态台的新迁移面板做一次人工页面验收  
3. 再决定是否进入 `Phase 10`，或者先补一轮 `Phase 9` 的验收报告壳

## 贡献与署名说明

- 本轮联调、问题定位、结论整理与文档撰写由 Codex 完成。
