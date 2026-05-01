# Phase 10 一致性检查基线报告

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Report ID: REPORT-20260424-phase10-consistency-baseline-v1
- Created At: 2026-04-24 Asia/Shanghai
- Scope: `Phase 10.2` 状态一致性检查本地基线

## 目标

在进入 `Phase 10.3` 页面级烟测之前，先补一条只读的一致性检查链，避免后续只看 `HTTP 200` 却无法判断当前章节状态是否合理。

## 本轮新增

- 后端新增章节级一致性检查合同与实现：
  - `StoryConsistencyCheck`
  - `StoryConsistencyIssue`
  - `StoryConsistencyCheckService`
  - `DefaultStoryConsistencyCheckService`
- 后端新增接口：
  - `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/consistency-check`
- 前端 `状态台` 新增“一致性检查”面板：
  - scene 数量
  - completed scene 数量
  - handoff/event/snapshot/patch 数量
  - `ReaderRevealState` / `ChapterIncrementalState` 是否存在
  - chapter review 是否存在及结果
  - 问题清单

## 当前检查规则

- `scene runtime state` 为空时提示 `no_scene_state`
- 有 scene 但没有 event 时提示 `missing_events`
- 有 scene 但没有 snapshot 时提示 `missing_snapshots`
- 有 completed scene 但没有 patch 时提示 `missing_patches`
- 缺少 `ReaderRevealState` 时提示 `missing_reader_reveal_state`
- 缺少 `ChapterIncrementalState` 时提示 `missing_chapter_state`
- 章节级 review 未收口或存在 review issue 时，统一折叠进一致性问题列表

## 本地验证

- 后端：
  - `DefaultStoryConsistencyCheckServiceTest`
  - `StoryStateControllerTest`
- 前端：
  - `npm run type-check`
  - `npm run build`

## 结论

- `Phase 10.2` 已具备本地可用的一致性检查主链
- 这条链当前仍是只读诊断，不参与写回
- 下一步更合理的是继续推进 `Phase 10.3` 的页面级验收模板和烟测基线，而不是继续扩一致性规则范围

## 贡献与署名说明

- 本文档由 Codex 基于当前 `Phase 10.2` 本地实现与定向回归结果整理撰写。
