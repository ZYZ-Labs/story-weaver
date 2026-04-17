# Phase 4 Story Context 线上联调报告 Round 1

- Date: 2026-04-17 Asia/Shanghai
- Scope:
  - `Phase 4.3` 首批只读 `story-context` 接口真实部署联调
  - `旧日王座` 项目真实数据校验

## 本轮目标

确认以下首批只读上下文接口在真实部署环境可用，并返回与项目实际数据一致的结构化结果：

- `GET /api/story-context/projects/{projectId}/brief`
- `GET /api/story-context/story-units/summary`
- `GET /api/story-context/projects/{projectId}/chapters/{chapterId}/anchors`
- `GET /api/story-context/projects/{projectId}/chapters/{chapterId}/reader-known-state`
- `GET /api/story-context/projects/{projectId}/characters/{characterId}/runtime-state`
- `GET /api/story-context/projects/{projectId}/progress`

## 联调样本

- 项目：`28 / 旧日王座`
- 章节：`31 / 退役者的邀请函`
- 主 POV 人物：`15 / 林沉舟`
- StoryUnit：
  - `unitId=31`
  - `unitKey=chapter:31`
  - `unitType=CHAPTER`

## 验收结果

### 1. 项目摘要接口通过

接口：

- `GET /api/story-context/projects/28/brief`

结果：

- `HTTP 200`
- 返回了项目标题、`logline` 和摘要
- 内容与 `旧日王座` 当前项目资料一致

结论：

- 通过

### 2. StoryUnit 摘要接口通过

接口：

- `GET /api/story-context/story-units/summary?unitId=31&unitKey=chapter:31&unitType=CHAPTER`

结果：

- `HTTP 200`
- 返回：
  - `unitRef`
  - `unitType=CHAPTER`
  - `title=退役者的邀请函`
  - `summary=两年沉寂后，主角收到旧战队邀请，命运再次启动。`

结论：

- 通过

### 3. 章节锚点接口通过

接口：

- `GET /api/story-context/projects/28/chapters/31/anchors`

结果：

- `HTTP 200`
- 返回：
  - `outlineTitle=第一卷：新纪元开服`
  - `mainPovCharacterName=林沉舟`
  - `storyBeats` 与章节真实配置一致

结论：

- 通过

### 4. 读者已知状态接口通过

接口：

- `GET /api/story-context/projects/28/chapters/31/reader-known-state`

结果：

- `HTTP 200`
- 返回空 `knownFacts`
- `unrevealedFacts` 已包含：
  - 当前章节摘要待揭晓
  - 当前剧情节点待推进
  - 当前 POV 人物提示

结论：

- 通过

### 5. 最近进度接口通过

接口：

- `GET /api/story-context/projects/28/progress?limit=5`

结果：

- `HTTP 200`
- 返回最近生成记录与项目上下文
- 数据与 `旧日王座` 历史生成记录一致

结论：

- 通过

### 6. 人物运行时状态接口失败

接口：

- `GET /api/story-context/projects/28/characters/15/runtime-state`

结果：

- `HTTP 500`

容器日志定位：

- `DefaultCharacterRuntimeStateQueryService`
- `List.of(...)` 中包含空值，触发 `NullPointerException`

关键日志：

- `java.lang.NullPointerException`
- `at java.util.List.of(...)`
- `at DefaultCharacterRuntimeStateQueryService.getCharacterRuntimeState(DefaultCharacterRuntimeStateQueryService.java:58)`

根因：

- 真实数据下，`projectRole / roleType / activeStage / isRetired` 中存在空值组合
- 当前实现使用 `List.of(...)` 组装 `stateTags`
- `List.of` 不允许元素为 `null`

本地修复状态：

- 已改为 `Arrays.asList(...)` 后再走 `ContextViewSupport.sanitizeDistinct(...)`
- 已补回归测试：
  - `DefaultCharacterRuntimeStateQueryServiceTest`
- 本地编译与定向测试已通过

结论：

- 发现真实线上缺陷
- 已在本地完成修复
- 待部署后进行 Round 2 复验

## 阶段判断

截至本轮：

- `Phase 4.1`：完成
- `Phase 4.2`：完成
- `Phase 4.3`：真实联调已启动

当前真实状态不是“接口设计有问题”，而是：

- 首批 6 个只读接口里，`5/6` 已通过线上联调
- `runtime-state` 存在一个真实空值缺陷，已本地修复，待部署复验

## 下一步

1. 部署本地 `runtime-state` 修复版本
2. 复验 `GET /api/story-context/projects/28/characters/15/runtime-state`
3. 若复验通过，则可将 `Phase 4.3` 标记为完成，并进入 `Phase 4.4 / Phase 5` 准备
