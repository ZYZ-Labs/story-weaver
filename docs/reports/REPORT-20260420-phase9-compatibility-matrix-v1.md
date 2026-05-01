# Phase 9 兼容矩阵明细报告 v1

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Phase: Phase 9.1
- Report ID: REPORT-20260420-phase9-compatibility-matrix-v1
- Created At: 2026-04-20 Asia/Shanghai
- Author: Codex

## 结论

当前系统已经形成“新主链可用、旧主链仍在”的过渡结构。

`Phase 9.1` 的首要任务不是删除旧链，而是把兼容面冻结清楚。

这份矩阵把兼容面拆成四层：

- 页面层
- API 层
- 数据层
- 样本层

## 页面层兼容矩阵

| 页面 | 路由 | 当前角色 | 依赖主链 | 迁移状态 | 处理建议 |
| --- | --- | --- | --- | --- | --- |
| 创作台 | `/workbench` | 新主入口 | `story-context`, `story-orchestration`, `story-state` | 主链 | 保持主入口 |
| 状态台 | `/state-center` | 新主入口 | `story-state`, `story-context` | 主链 | 保持主入口 |
| 生成台 | `/generation-center` | 新主入口 | `story-orchestration` | 主链 | 保持主入口 |
| 章节工作区 | `/chapter-workspace` | 新主入口 | `story-orchestration`, `story-state`, `story-context`, `generateStream` | 主链 | 保持主入口 |
| 旧写作中心 | `/writing` | 迁移备用入口 | 旧写作链 + 新状态链局部复用 | 兼容入口 | 暂保留，后续降级 |
| 章节管理 | `/chapters` | 结构维护页 | 新旧混合 | 迁移中 | 保留二级入口 |
| 人物管理 | `/characters` | 结构维护页 | `summary-workflow` + 旧对象更新链 | 迁移中 | 保留二级入口 |
| 世界观管理 | `/world-settings` | 结构维护页 | `summary-workflow` + 旧对象更新链 | 迁移中 | 保留二级入口 |

判断：

- `创作台 / 状态台 / 生成台 / 章节工作区` 已足够作为新的作者主路径。
- `旧写作中心` 和三类对象页仍需保留，避免迁移期动作断链。

## API 层兼容矩阵

| API 组 | 当前角色 | 主要消费者 | 迁移状态 | 处理建议 |
| --- | --- | --- | --- | --- |
| `summary-workflow` | 新对象工作流主链 | 对象页、摘要工作流弹层 | 主链 | 继续保持稳定 |
| `story-context` | 新只读上下文主链 | 创作台、状态台、生成台、后端编排 | 主链 | 继续扩展，不破坏现协议 |
| `story-orchestration` | 新编排主链 | 生成台、章节工作区 | 主链 | 继续扩展，不破坏现协议 |
| `story-state` | 新状态主链 | 状态台、章节工作区、生成台、迁移分析 | 主链 | 继续扩展，不破坏现协议 |
| 旧 `writing` 接口 | 正文生成兼容链 | 旧写作中心、章节工作区草稿生成 | 兼容入口 | 暂保留 |
| 旧对象 CRUD 接口 | 对象更新兼容链 | 章节管理、人物管理、世界观管理 | 兼容入口 | 暂保留，逐步降级 |

判断：

- 新主链接口已经清晰。
- 旧接口仍然有明确页面依赖，不能提前删。

## 数据层兼容矩阵

| 数据对象 | 新体系归属 | 当前状态 | 主要来源 | 备注 |
| --- | --- | --- | --- | --- |
| `SceneExecutionState` | `State Server` | 已稳定 | 新执行写回 + 旧 `AIWritingRecord` 兼容读模型 | 已形成主链 |
| `SceneHandoffSnapshot` | `State Server` | 已稳定 | 新执行写回 | 已形成主链 |
| `StoryEvent` | `State Server` | 已稳定 | 新执行写回 | 已形成主链 |
| `StorySnapshot` | `State Server` | 已稳定 | 新执行写回 | 已形成主链 |
| `StoryPatch` | `State Server` | 已稳定 | 新执行写回 | 已形成主链 |
| `ReaderRevealState` | `State Server` | 已稳定 | 新执行写回 | 已形成主链 |
| `ChapterIncrementalState` | `State Server` | 已稳定 | 新执行写回 | 已形成主链 |
| `ChapterSkeleton` | `Orchestration` | 已稳定 | 规则规划 + mutation 覆写 | 已形成主链 |
| `AIWritingRecord` | 旧体系核心 | 兼容来源 | 旧正文生成链 | `Phase 9` 重点回填对象 |
| `Chapter.content/summary` | 旧体系核心 | 新旧混合 | 旧编辑/新摘要工作流/草稿写回 | `Phase 9` 重点兼容对象 |

判断：

- 状态链已经成型，但旧数据仍未系统回填。
- `AIWritingRecord` 仍然是迁移期最重要的兼容源。

## 样本层兼容矩阵

| 样本 | 用途 | 当前阶段 | 备注 |
| --- | --- | --- | --- |
| `旧日王座 chapter 32` | 迁移兼容、状态台、章节级 dry-run 主样本 | Phase 6-10 | 当前主样本 |
| `旧日王座 chapter 33` | 多镜头连续执行、章节审校样本 | Phase 6-10 | 当前主样本 |
| `旧日王座 chapter 34` | 兼容预览与骨架稳定性样本 | Phase 6-10 | 当前辅样本 |
| `旧日王座 chapter 35` | 迁移总览与对象/摘要工作流样本 | Phase 6-10 | 当前辅样本 |
| `[TEST] 冷启动空章` | `CHAPTER_COLD_START` / 回填空样本 | Phase 6-10 | 仍需补全 |
| `[TEST] scene fallback 样本` | `SCENE_FALLBACK_TO_LATEST` | Phase 6-10 | 建议保留 |
| `[TEST] 多 scene 混合状态样本` | handoff / review / migration | Phase 6-10 | 建议保留 |

## 当前迁移阻塞点

1. 旧写作中心仍是可见入口  
说明：它不能立刻移除，但必须逐步降级成迁移备用页。

2. 旧对象页仍依赖部分旧对象更新链  
说明：前端结构已改，但后端写入尚未完全切到新状态链。

3. `AIWritingRecord` 尚未系统回填为新事件/快照基线  
说明：这将直接影响 `Phase 9.2`。

## `Phase 9.2` 当前新增分析入口

- `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/backfill-analysis`
- `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/backfill-dry-run`
- `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/compatibility-snapshot`
- `GET /api/story-state/projects/{projectId}/backfill-overview`
- `GET /api/story-state/projects/{projectId}/backfill-project-dry-run`

作用：

- 只读分析旧章节是否已经具备：
  - `SceneExecutionState`
  - `StoryEvent`
  - `StorySnapshot`
  - `StoryPatch`
  - `ReaderRevealState`
  - `ChapterIncrementalState`
- 为后续真正的幂等回填提供基线，而不是先盲写数据
- 状态台已同步展示这两类结果，避免兼容面只能靠后端接口人工观察
- 状态台已同步展示页面/API/数据三层灰度边界和 feature flag，不再只能靠计划文档判断当前链路口径
- 状态台已同步展示项目级迁移总览，不再需要逐章点进状态台才能判断整个项目的回填压力
- 状态台已同步展示项目级 dry-run 计划，不再只能看“哪里有缺口”，还能看“下一步该迁什么”

## 下一步

1. 冻结样本清单
2. 明确回填对象的优先级
3. 开始 `AIWritingRecord` 回填服务设计

## 贡献与署名说明

- 本兼容矩阵由 Codex 基于当前代码结构、计划文档和已验证主链整理完成。
