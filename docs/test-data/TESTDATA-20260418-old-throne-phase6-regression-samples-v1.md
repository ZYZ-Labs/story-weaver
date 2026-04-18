# 旧日王座联调样本设计 v1

- Date: 2026-04-18 Asia/Shanghai
- Scope:
  - 服务 `Phase 6` 到 `Phase 10` 的持续联调
  - 优先覆盖 `SceneExecutionState`、`ChapterSkeleton`、`SceneHandoffSnapshot`
  - 不直接污染正式创作主线

## 目标

当前 `旧日王座` 的正式章节样本已经足够覆盖：

- `SCENE_BOUND`
- `SCENE_FALLBACK_TO_LATEST`
- 多章节 `skeleton-preview`

但仍缺少：

- `CHAPTER_COLD_START`
- 镜头级 handoff 连续执行
- 增量状态写回后的固定回归样本

所以需要在同一项目内补一批**专门用于联调的样本对象**。

## 样本使用原则

- 所有联调样本统一前缀：`[TEST]`
- 不与正式卷章混排，建议排序在项目尾部
- 每个样本都必须有明确用途说明
- 默认允许被覆盖、重置、重复联调
- 不把这些样本当作正式创作内容

## 推荐样本章节清单

### `TEST-CH-01 [TEST] 冷启动空章`

- 用途：
  - 验证 `CHAPTER_COLD_START`
  - 验证第一版 `ChapterSkeleton` 冷启动骨架
- 约束：
  - 不存在任何 `ai_writing_record`
  - 允许存在章节标题与简短摘要
  - 允许存在 `outline / pov / story beat`

### `TEST-CH-02 [TEST] 单 scene 已完成`

- 用途：
  - 验证最简单的 `SCENE_BOUND`
  - 验证单镜头后骨架补齐
- 约束：
  - 仅有 1 条已接受写作记录

### `TEST-CH-03 [TEST] scene 缺失回退`

- 用途：
  - 验证 `SCENE_FALLBACK_TO_LATEST`
  - 验证 trace 中 `retryable / details`
- 约束：
  - 仅有 `scene-1`
  - 请求 `scene-999`

### `TEST-CH-04 [TEST] 多 scene 混合状态`

- 用途：
  - 验证骨架对既有镜头的保留
  - 验证 `COMPLETED / FAILED / PLANNED` 混排
- 约束：
  - 至少包含：
    - `scene-1 = COMPLETED`
    - `scene-2 = FAILED`
    - `scene-3 = COMPLETED`

### `TEST-CH-05 [TEST] 空摘要章节`

- 用途：
  - 防回归 `chapter.summary = null`
  - 验证摘要缺失时的骨架兼容
- 约束：
  - `summary = null`

### `TEST-CH-06 [TEST] 无 POV / 无 outline 章节`

- 用途：
  - 验证 anchor 缺省兼容
  - 验证 writer brief 不因缺少 `outline / pov` 直接崩

### `TEST-CH-07 [TEST] handoff 连续样本`

- 用途：
  - 服务 `Phase 6.3`
  - 用来验证：
    - `SceneHandoffSnapshot`
    - 下一镜头承接
    - 最小写回后重新读取
- 约束：
  - 至少 2 个连续 scene
  - 允许后续重复覆盖

### `TEST-CH-08 [TEST] 章节级审校样本`

- 用途：
  - 服务 `Phase 6.4`
  - 用来验证章节级 reviewer 与章节级 trace 汇总

## 推荐对象样本清单

### 人物

- `TEST-CHAR-01 [TEST] 主角型`
  - 信息完整
- `TEST-CHAR-02 [TEST] 配角型`
  - 关系明确
- `TEST-CHAR-03 [TEST] 信息不完整型`
  - 方便验证摘要优先补全

### 世界观

- `TEST-WORLD-01 [TEST] 规则类`
- `TEST-WORLD-02 [TEST] 地点类`

### 剧情 / 因果

- `TEST-PLOT-01 [TEST] 主线`
- `TEST-PLOT-02 [TEST] 支线`
- `TEST-CAUSE-01 [TEST] 显式因果`
- `TEST-CAUSE-02 [TEST] 待补完因果`

## 样本与阶段映射

### `Phase 6`

- `TEST-CH-01`
- `TEST-CH-02`
- `TEST-CH-03`
- `TEST-CH-04`
- `TEST-CH-05`
- `TEST-CH-07`

### `Phase 7`

- `TEST-CH-07`
- `TEST-CHAR-*`
- `TEST-WORLD-*`
- `TEST-PLOT-*`
- `TEST-CAUSE-*`

### `Phase 8`

- 使用上述样本作为前端工作台演示与浏览器回归基线

### `Phase 9`

- 使用上述样本验证迁移、兼容与回填

### `Phase 10`

- 使用上述样本做固定回放测试与观测验证

## 第一批建议先落哪些

优先级最高的是：

1. `TEST-CH-01 [TEST] 冷启动空章`
2. `TEST-CH-03 [TEST] scene 缺失回退`
3. `TEST-CH-05 [TEST] 空摘要章节`
4. `TEST-CH-07 [TEST] handoff 连续样本`

原因：

- 这 4 个样本可以直接补上当前 `Phase 6` 最缺的覆盖面
- 成本最低
- 后续 `Phase 6.3 / 6.4` 也会直接复用

## 执行建议

- 先不直接手工改正式章节
- 先按这份文档落“第一批最小联调样本”
- 每推进一个阶段，只增补必要样本
- 不无限扩样本池

## 当前结论

- 当前 `旧日王座` 正式章节样本已足够支撑 `Phase 6.1 / 6.2`
- 从 `Phase 6.3` 开始，联调样本必须制度化
- 否则后续会持续被样本不足卡住

## 贡献与署名说明

- 样本建设需求与方向由用户提出和主导
- 本文档的样本拆分、阶段映射、命名规则与执行顺序由 Codex 完成
