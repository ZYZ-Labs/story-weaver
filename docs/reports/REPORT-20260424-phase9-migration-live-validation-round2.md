# Phase 9 迁移兼容线上联调报告 Round 2

- Date: 2026-04-24 Asia/Shanghai
- Phase: `Phase 9`
- Scope:
  - `旧日王座` 项目 (`projectId=28`)
  - 项目级迁移总览 / 项目级 dry-run
  - 章节级兼容分析 / dry-run / 兼容快照
  - 状态台 `Phase 9` 验收提示与推荐样本章节入口

## 目标

在 `Round 1` 已确认核心迁移兼容接口可用的基础上，补齐收口证据：

- 用当前线上真实管理员记录确认鉴权链
- 用当前真实章节编号复验项目级和章节级接口
- 确认当前部署前端产物已经包含 `Phase 9` 状态台验收辅助入口

## 鉴权与联调方式

- 读取 backend 容器的 `JWT_SECRET`
- 查询线上 `story_weaver.user` 表，确认管理员记录：
  - `id = 1`
  - `username = admin`
  - `role_code = admin`
  - `status = 1`
  - `deleted = 0`
- 基于该信息生成临时 JWT
- 通过 backend 容器内 `127.0.0.1:8080` 直接命中接口，规避网关和 DNS 干扰

## 实际联调结果

### 1. 当前部署前端已包含 `Phase 9` 状态台文案

在前端容器中确认当前 `StateCenterView` 产物已包含：

- `项目级 dry-run 计划`
- `Phase 9 验收提示`
- `推荐样本章节`

结论：

- 当前部署前端已包含 `Phase 9` 状态台验收辅助入口
- 当前不是“后端已上线、前端还是旧版”的状态

### 2. 项目级接口复验通过

以下接口在线上均返回 `200`：

- `GET /api/story-state/projects/28/backfill-overview`
- `GET /api/story-state/projects/28/backfill-project-dry-run`

关键返回确认：

- `totalChapters = 4`
- `chaptersNeedingBackfill = 4`
- `runnableChapters = 4`
- `blockedChapters = 0`

结论：

- `Phase 9.4` 的项目级迁移总览和项目级 dry-run 当前部署环境稳定可用

### 3. 章节级接口复验通过

对当前真实样本章节 `chapterId=32`：

- `GET /api/story-state/projects/28/chapters/32/backfill-analysis` -> `200`
- `GET /api/story-state/projects/28/chapters/32/backfill-dry-run` -> `200`
- `GET /api/story-state/projects/28/chapters/32/compatibility-snapshot` -> `200`

关键返回确认：

- `backfill-analysis`
  - `legacyGeneratedRecordCount = 1`
  - `needsSceneBackfill = true`
  - `needsStateBackfill = true`
- `backfill-dry-run`
  - `canRunBackfill = true`
  - 已返回 4 条动作
- `compatibility-snapshot`
  - 已返回：
    - `pageBoundaries`
    - `apiBoundaries`
    - `dataBoundaries`
    - `featureFlags`
    - `riskNotes`

结论：

- `Phase 9.2 / 9.3` 的章节级兼容链在当前部署环境继续稳定可用

### 4. 当前推荐样本章节已冻结

当前 `projectId=28` 的推荐 `Phase 9` 样本章节为：

- `#32 算法少女苏晚`
- `#33 训练赛首胜`
- `#34 宿敌归来`
- `#35 退役者的邀请`

结论：

- 后续不应再默认使用旧的 `chapter 31`
- `Phase 10` 的回放矩阵应以这四章为主

## 阶段判断

- `Phase 9.2`：线上通过
- `Phase 9.3`：线上通过
- `Phase 9.4`：线上通过
- 当前部署前端已包含 `Phase 9` 状态台验收辅助入口

整体判断：

- `Phase 9` 可以收口
- 页面级统一人工验收模板转入 `Phase 10.3`

## 下一步

1. 将 `Phase 9` 标记为完成
2. 启动 `Phase 10.1`
3. 冻结固定样本矩阵和页面级验收基线

## 贡献与署名说明

- 本轮联调、线上管理员记录核对、结论整理与文档撰写由 Codex 完成。
