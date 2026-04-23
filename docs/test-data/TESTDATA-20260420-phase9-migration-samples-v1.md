# Phase 9 迁移与回放样本清单 v1

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Phase: Phase 9.1
- Test Data ID: TESTDATA-20260420-phase9-migration-samples-v1
- Created At: 2026-04-20 Asia/Shanghai
- Author: Codex

## 目标

为 `Phase 9` 的迁移、兼容、回填和回放准备固定样本集，避免后续再以“临时找一章”方式做验证。

## 主样本项目

### 项目：旧日王座

用途：

- 新旧主链兼容回归
- 状态链回放
- 场景绑定 / 骨架 / handoff / 章节审校
- 前端工作区与旧写作中心对照

## 章节样本

### 样本 1

- 项目：旧日王座
- 章节：第 31 章
- 用途：
  - 工作区主路径
  - 编排预览
  - 生成台
  - reader known state
  - chapter-state
  - patch / snapshot / event

### 样本 2

- 项目：旧日王座
- 章节：第 32 章
- 用途：
  - 多 scene 连续执行
  - handoff 连续性
  - 章节级审校 PASS 样本

### 样本 3

- 项目：旧日王座
- 章节：第 34 章
- 用途：
  - 兼容预览
  - skeleton-preview 稳定性
  - 空摘要 / 边界输入兼容

## 专用测试样本建议

这些样本应逐步补入 `旧日王座`，统一以前缀 `[TEST]` 标识。

### `[TEST] 冷启动空章`

用途：

- `CHAPTER_COLD_START`
- 新骨架冷启动
- 空正文起手

要求：

- 无 `AIWritingRecord`
- 无 scene runtime state
- 尽量只保留最小摘要

### `[TEST] 单 scene 已完成`

用途：

- `SCENE_BOUND`
- 单镜头 handoff 起点

要求：

- 有 1 个已完成 scene
- 章节级 reviewer 可给出未完成结论

### `[TEST] scene fallback`

用途：

- `SCENE_FALLBACK_TO_LATEST`
- 不存在 sceneId 请求回退

要求：

- 已有 `scene-1`
- 请求 `scene-999`

### `[TEST] 多 scene 混合状态`

用途：

- `completed / failed / reviewing / planned` 混合状态
- chapter-review 问题汇总

要求：

- 至少包含 3 个 scene
- 至少 1 个失败 scene
- 至少 1 个缺失 handoff

### `[TEST] 空摘要章节`

用途：

- 防止 `summary = null` 类回归

要求：

- `chapter.summary = null`
- 仍能完成 preview / skeleton / review

## 对象样本建议

### 人物样本

- `[TEST-CHAR] 完整主角`
- `[TEST-CHAR] 信息不完整人物`
- `[TEST-CHAR] 强情绪标签人物`

### 世界观样本

- `[TEST-WORLD] 规则型设定`
- `[TEST-WORLD] 地点型设定`
- `[TEST-WORLD] 势力型设定`

## 回填优先顺序

1. `旧日王座 chapter 31`
2. `旧日王座 chapter 32`
3. `旧日王座 chapter 34`
4. `[TEST] 冷启动空章`
5. `[TEST] 多 scene 混合状态`

## 使用方式

### `Phase 9.2`

- 验证旧记录到新读模型的回填正确性

### `Phase 9.3`

- 验证双读 / 双写与灰度切换是否稳定

### `Phase 10`

- 作为固定回放样本集

## 贡献与署名说明

- 样本策略和样本命名规范由 Codex 基于当前联调过程整理完成。
