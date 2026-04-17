# Phase 5 Story Orchestration 线上联调报告 Round 1

- Date: 2026-04-17 Asia/Shanghai
- Scope:
  - `Phase 5.3` 最小四 session 预览链路首轮真实联调
  - 验证 `story-orchestration` preview 接口的真实返回结构与最小鉴权口径

## 本轮目标

在部署 `Phase 5` 首轮多 session 编排实现后，确认以下事项：

- `GET /api/story-orchestration/projects/{projectId}/chapters/{chapterId}/preview` 可在真实环境返回 `200`
- 返回体中包含：
  - `contextPacket`
  - `candidates`
  - `selectionDecision`
  - `writerExecutionBrief`
  - `writerSessionResult`
  - `reviewDecision`
  - `trace`
- 未带 `Authorization` 时返回 `401`
- `sceneId` 自定义参数可正确透传

## 联调样本

- 项目：`28 / 旧日王座`
- 章节：`31 / 退役者的邀请函`
- 视角人物：`15 / 林沉舟`
- 联调方式：
  - 由于当前终端环境对公网 DNS 解析不稳定，本轮直接在 `story-weaver-backend` 容器内命中 `127.0.0.1:8080`

## 验收结果

### 1. 默认 preview 主链通过

接口：

- `GET /api/story-orchestration/projects/28/chapters/31/preview`

结果：

- `HTTP 200`
- 返回结构完整，包含：
  - `contextPacket`
  - `candidates`
  - `selectionDecision`
  - `writerExecutionBrief`
  - `writerSessionResult`
  - `reviewDecision`
  - `trace`

关键返回：

- 选中候选：`opening-31`
- 选中原因：
  - `当前章节还没有 scene 执行状态，优先先做开场定向。`
- 审校结果：
  - `PASS`
- trace 条数：
  - `5`

结论：

- `director -> selector -> writer -> reviewer` 的最小只读预览闭环已在线上跑通

### 2. 未认证口径正确

接口：

- `GET /api/story-orchestration/projects/28/chapters/31/preview`

条件：

- 不带 `Authorization`

结果：

- `HTTP 401`

结论：

- 当前接口鉴权口径与 controller 级测试一致

### 3. `sceneId` 参数可透传，但当前尚未形成真实场景承接

接口：

- `GET /api/story-orchestration/projects/28/chapters/31/preview?sceneId=scene-2`

结果：

- `HTTP 200`
- 返回中的：
  - `contextPacket.sceneId = scene-2`
  - `writerExecutionBrief.sceneId = scene-2`
  - `writerSessionResult.sceneId = scene-2`
  - `reviewDecision.sceneId = scene-2`

同时观察到：

- `selectionDecision` 仍选择 `opening-31`
- `existingSceneStates` 仍为空
- 返回内容除了 `sceneId` 外，与 `scene-1` 基本一致

结论：

- 当前 `sceneId` 已完成参数级透传
- 但由于尚未接入真实 `SceneExecutionState` 查询与承接，当前仍属于“多 session 编排壳”，还不是“多 scene 连续执行”

## 阶段判断

截至本轮：

- `Phase 5.1`：完成
- `Phase 5.2`：完成
- `Phase 5.3`：最小闭环已通过首轮真实联调

当前可以给出的工程判断是：

- `Phase 5` 的“最小四 session 预览链路”已经成立
- 当前缺口不在接口通断，而在：
  - trace 与失败口径还未完全收口
  - `sceneId` 尚未绑定真实场景状态承接

## 下一步

1. 进入 `Phase 5.4`
2. 固定：
   - trace 结构
   - 失败/跳过/重试口径
3. 为 `Phase 6` 的 `SceneExecutionState` 与章节骨架接入铺路
