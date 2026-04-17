# 前端信息架构重构方案

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Arch Doc Version: v1
- Status: Drafted
- Created At: 2026-04-11 Asia/Shanghai
- Updated At: 2026-04-11 Asia/Shanghai

## 问题定性

当前前端最大的问题不是样式，而是结构：

- 仍然偏“后台对象管理”
- 仍然偏“字段录入”
- 创作动作、结构层、状态层、调试层混在一起

后续如果继续沿用“世界观页 / 人物页 / 章节页 / 写作页”的平铺结构，平台会越来越像后台管理系统，而不是创作工作台。

## 硬原则

### 1. 用户默认只看摘要

- 不直接展示原始 JSON
- 不默认展示大批字段
- 所有对象先展示摘要和变更说明

### 2. 默认入口是“当前创作动作”

用户首先看到的是：

- 当前章节
- 当前镜头
- 当前建议
- 当前待确认变化

而不是系统里有哪些对象表。

### 3. 专家模式是信息密度开关，不是权限隔离

- 普通作者默认不开
- 任何作者可主动开启
- 开启后增加结构层和调试层可见性

## 顶层导航建议

### 1. 创作台

给普通作者用的主入口。

核心内容：

- 当前项目摘要
- 当前章节摘要
- 当前镜头
- 下一步建议
- 正文编辑与生成
- 待确认变化

### 2. 故事台

看稳定内容：

- 人物
- 世界观
- 势力
- 物品
- 技能
- 地点
- 剧情
- 因果

但默认展示摘要，不展示字段墙。

### 3. 状态台

看运行时状态：

- 背包
- 技能状态
- 关系变化
- 态度
- 好感度
- 世界影响
- Reader Reveal State

### 4. 生成台

给专家模式 / 开发者用：

- 总导候选
- 选择结果
- 镜头执行
- 审校结果
- generation trace
- MCP/State Server 调用检查

### 5. 系统台

- Provider
- MCP Server
- State Server
- 设置
- 策略开关

## 章节工作区

章节页后续不应只是正文页，而应成为 `Chapter Workspace`。

建议逻辑分区：

### 左侧

- 章节骨架
- 镜头列表
- 当前镜头状态

### 中间

- 正文
- 生成结果
- 对比

### 右侧

- 人物摘要
- 背包状态
- 技能状态
- 地点与世界影响
- Reader Reveal

### 底部或抽屉

- 总导
- 选择器
- 写手
- 审校器
- trace

## 对象详情页统一模式

无论是人物、世界观、物品还是技能，都建议统一为 4 个 tab：

### 1. Summary

- 用户主视图

### 2. Canon

- 稳定设定

### 3. State

- 当前运行状态

### 4. History

- 变化记录
- 最近章节影响
- reveal 记录

默认只打开 `Summary`。

## 对象创建与修改流程

后续默认交互：

1. 用户输入自然语言摘要
2. AI 拆分结构 patch
3. 系统生成变更摘要
4. 用户确认
5. 写入 facets

因此前端默认页面不再需要承载大量输入表单。

## 模式分层

### 普通模式

- 摘要
- 当前动作
- 简要建议
- 待确认变化

### 专家模式

- 结构化摘要包
- anchor bundle
- state summary
- scene handoff
- pending patch

### 开发/调试面板

- raw trace
- MCP tool call
- state snapshot
- provider errors

## 最先需要重构的页面

1. `WritingView` -> `ChapterWorkspaceView`
2. `CharacterListView` -> `StoryWorkbench + CharacterDetailPanel`
3. `WorldSetting` 相关页面 -> `Summary First Detail`
4. 新增 `StoryStatePanel`
5. 新增 `ExecutionTracePanel`

## 贡献与署名说明

- “现有前端问题不在样式而在结构”以及“字段表单必须退居二线”的判断：用户提出。
- 顶层信息架构、模式分层、章节工作区和对象详情结构设计：Codex 完成。
- “用户只看摘要、结构下沉为协议层”的前端原则由用户与 Codex 共同讨论形成。
