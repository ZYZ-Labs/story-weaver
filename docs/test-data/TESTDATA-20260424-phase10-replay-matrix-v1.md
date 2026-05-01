# Phase 10 固定回放矩阵 v1

- Date: 2026-04-24 Asia/Shanghai
- Phase: `Phase 10.1`
- Scope:
  - 固定章节样本
  - 固定页面验收入口
  - 固定接口回放入口

## 目标

把当前平台主链的样本冻结下来，后续回放、验收和问题复现都优先基于这份矩阵。

## 固定项目

- 项目：`旧日王座`
- `projectId = 28`

## 固定章节样本

### `chapterId = 32` `算法少女苏晚`

用途：

- `Phase 9` 迁移兼容主样本
- 章节级：
  - `backfill-analysis`
  - `backfill-dry-run`
  - `compatibility-snapshot`
- 状态台：
  - `Phase 9 验收提示`
  - 推荐样本章节跳转

### `chapterId = 33` `训练赛首胜`

用途：

- 多镜头执行
- handoff 连续性
- chapter review 正向样本

### `chapterId = 34` `宿敌归来`

用途：

- 骨架预览
- 编排预览
- 兼容回放辅助样本

### `chapterId = 35` `退役者的邀请`

用途：

- 项目级迁移总览辅助样本
- 对象页 / 摘要工作流辅助样本

## 固定页面验收入口

### `创作台`

关注：

- 当前项目简报
- 最近进度
- 章节骨架预览
- 进入章节工作区主路径

### `状态台`

关注：

- 章节状态
- 读者揭晓与 POV 状态
- `Phase 9 验收提示`
- `迁移兼容分析`
- `灰度边界与开关`
- `项目级迁移总览`
- `项目级 dry-run 计划`

### `生成台`

关注：

- 多 session 编排预览
- 候选列表
- writer brief
- trace
- 章节审校摘要

### `章节工作区`

关注：

- 骨架镜头列表
- 当前镜头草稿生成
- 草稿接受/拒绝
- runtime / handoff 推进
- trace 与 chapter review

### `人物 / 世界观 / 章节管理`

关注：

- `Summary / Canon / State / History`
- 不回退成字段墙

## 固定接口回放入口

### `story-context`

- `GET /api/story-context/projects/28/brief`
- `GET /api/story-context/projects/28/chapters/{chapterId}/reader-known-state`

### `story-orchestration`

- `GET /api/story-orchestration/projects/28/chapters/{chapterId}/preview`
- `GET /api/story-orchestration/projects/28/chapters/{chapterId}/skeleton-preview`
- `GET /api/story-orchestration/projects/28/chapters/{chapterId}/chapter-review`

### `story-state`

- `GET /api/story-state/projects/28/chapters/{chapterId}/events`
- `GET /api/story-state/projects/28/chapters/{chapterId}/snapshots`
- `GET /api/story-state/projects/28/chapters/{chapterId}/patches`
- `GET /api/story-state/projects/28/chapters/{chapterId}/reader-reveal-state`
- `GET /api/story-state/projects/28/chapters/{chapterId}/chapter-state`
- `GET /api/story-state/projects/28/chapters/{chapterId}/backfill-analysis`
- `GET /api/story-state/projects/28/chapters/{chapterId}/backfill-dry-run`
- `GET /api/story-state/projects/28/chapters/{chapterId}/compatibility-snapshot`
- `GET /api/story-state/projects/28/backfill-overview`
- `GET /api/story-state/projects/28/backfill-project-dry-run`

## 回放原则

- 默认先跑固定章节样本，再临时抽查其他章节
- 默认先跑只读接口，再跑执行链
- 页面级问题优先记在对应页面，不混进接口问题

## 贡献与署名说明

- 本矩阵由 Codex 基于当前已完成阶段、线上样本与联调结果整理撰写。
