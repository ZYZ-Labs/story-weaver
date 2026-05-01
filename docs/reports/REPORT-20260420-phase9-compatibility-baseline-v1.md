# Phase 9 迁移兼容基线报告 v1

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Phase: Phase 9
- Report ID: REPORT-20260420-phase9-compatibility-baseline-v1
- Created At: 2026-04-20 Asia/Shanghai
- Author: Codex

## 结论

`Phase 9` 当前不应直接进入“迁掉旧链”的动作，而应先冻结兼容基线。

现阶段最重要的不是清理旧入口，而是明确：

- 哪些页面已经由新主链接管
- 哪些页面仍必须保留作迁移期备用入口
- 哪些接口已经成为新协议主入口
- 哪些旧数据仍然只是兼容读模型来源

## 当前兼容矩阵

### 页面层

#### 已成为新主入口

- `创作台 /workbench`
- `状态台 /state-center`
- `生成台 /generation-center`
- `章节工作区 /chapter-workspace`

#### 迁移期仍需保留

- `旧写作中心 /writing`
- `章节管理 /chapters`
- `人物管理 /characters`
- `世界观管理 /world-settings`

判断：

- 新工作流已具备主入口能力
- 但对象页和旧写作中心仍承担迁移期补位角色

### API 层

#### 新协议主链

- `summary-workflow`
- `story-context`
- `story-orchestration`
- `story-state`

#### 迁移期仍需保留

- 旧 `writing` 相关接口
- 旧对象管理更新接口

判断：

- 新页面和新工作流主要依赖新协议
- 但旧页面、旧写作链和部分编辑动作仍引用旧接口

### 数据层

#### 已纳入新状态链

- `SceneExecutionState`
- `SceneHandoffSnapshot`
- `StoryEvent`
- `StorySnapshot`
- `StoryPatch`
- `ReaderRevealState`
- `ChapterIncrementalState`

#### 当前仍主要依赖兼容读模型

- `AIWritingRecord -> SceneExecutionState`
- `AIWritingRecord -> StorySnapshot / StoryEvent`
- 旧章节摘要和旧正文状态

判断：

- 新状态链已经成型
- 但旧数据尚未做系统化回填，仍主要以兼容读模型承接

## 迁移优先级建议

### 优先级 1

- 冻结旧页面保留期
- 冻结旧接口保留期
- 冻结联调/回放样本集

### 优先级 2

- 建立 `AIWritingRecord` 回填基线
- 建立 `Chapter` 级旧摘要/正文状态回填基线

### 优先级 3

- 建立双读 / 双写边界和 feature flag

## 当前不建议做的事

- 直接删除旧写作中心
- 直接删除旧写作接口
- 一次性迁空旧表
- 在没有回填报告前切掉兼容读模型

## 当前线上样本修正

`旧日王座 / projectId=28` 的当前线上章节编号已经变化，后续 `Phase 9` 联调不应再默认使用历史上的 `chapter 31`。

当前线上建议主样本：

- `#32 算法少女苏晚`
- `#33 训练赛首胜`
- `#34 宿敌归来`
- `#35 退役者的邀请`

说明：

- 继续请求历史 `chapter 31` 时，章节级兼容接口会返回 `data=null`
- 这代表样本编号已变，不代表接口实现失效

## 下一步

1. 进入 `Phase 9.1`
2. 把兼容矩阵补到对象级 / 接口级 / 数据级明细
3. 冻结样本集
4. 再开始 `Phase 9.2` 回填实现

## 贡献与署名说明

- “先冻结兼容基线，再做迁移”的方向来自用户与 Codex 的持续讨论。
- 本报告的兼容矩阵整理、迁移优先级和风险边界由 Codex 完成。
