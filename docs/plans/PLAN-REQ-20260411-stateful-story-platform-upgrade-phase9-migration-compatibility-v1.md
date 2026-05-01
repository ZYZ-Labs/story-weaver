# Story 平台升级 Phase 9 详细实施计划：迁移、兼容与回填

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Plan ID: PLAN-REQ-20260411-stateful-story-platform-upgrade-phase9-migration-compatibility-v1
- Status: Completed
- Created At: 2026-04-20 Asia/Shanghai
- Updated At: 2026-04-24 Asia/Shanghai

## 本轮目标

在 `Phase 8` 前端信息架构完成代码侧收口之后，开始冻结新旧主链并存阶段的迁移、兼容与回填策略。

`Phase 9` 的目标不是再新增一套功能，而是确保：

- 旧数据可以继续被新架构消费
- 旧入口在迁移期不会突然断掉
- 新状态链与旧写作链可以并存
- 后续统一验收和灰度切换有可执行基线

一句话说：

- `Phase 8` 解决“新界面和新主动作”
- `Phase 9` 解决“新旧主链怎么一起活着，并逐步切过去”

## 本轮原则

- 先做兼容清单，不先做大迁移
- 先做只读回填，再做写入双写
- 不在本阶段引入破坏式数据重写
- 不默认一次性迁空旧表或旧入口
- 所有迁移动作都要求：
  - 可回放
  - 可重跑
  - 可诊断
  - 可回退

## 范围内

- 新旧主链兼容清单
- 旧记录到新读模型的回填策略
- `AIWritingRecord -> Scene / Snapshot / Event` 的迁移基线
- 旧对象页、旧写作页、旧 API 的迁移期保留策略
- 双读/双写开关与灰度顺序
- 迁移样本与验收口径

## 范围外

- 不在本阶段做最终数据清库
- 不在本阶段删除旧写作中心
- 不在本阶段关闭旧接口
- 不在本阶段做最终产品化 MCP 发布

## 分阶段实施拆分

### `Phase 9.1` 兼容清单冻结与迁移样本定稿

目标：

- 列出所有仍在使用的旧入口、旧接口、旧数据源
- 列出哪些已经被新主链覆盖，哪些仍需保留
- 冻结迁移样本集和回放样本集

交付物：

- 兼容矩阵
- 旧入口保留清单
- 旧数据回填清单
- 联调样本清单

退出条件：

- 不再模糊地说“后面再迁”，而是明确哪些对象、哪些接口、哪些页面仍受旧链影响

### `Phase 9.2` 旧记录回填与兼容读模型落地

目标：

- 先提供只读回填分析入口，明确每章旧记录与新状态覆盖缺口
- 建立回填服务，把旧记录映射到新读模型
- 优先覆盖：
  - `AIWritingRecord -> SceneExecutionState`
  - `AIWritingRecord -> StorySnapshot`
  - `AIWritingRecord -> StoryEvent`
  - 旧章节摘要/正文状态 -> Chapter read model

交付物：

- 回填分析服务与分析接口
- dry-run 规划服务与规划接口
- 前端兼容分析面板
- 回填服务
- 回填报告
- 幂等重跑策略

退出条件：

- 新界面与新编排链不再只能依赖“新产生的数据”

### `Phase 9.3` 双读 / 双写与灰度边界

目标：

- 明确哪些链路先双读，哪些链路先双写
- 明确旧写作中心、章节工作区、状态台、生成台之间的灰度关系
- 明确 feature flag / 配置开关

交付物：

- 双读/双写边界清单
- 灰度顺序
- 回退策略

退出条件：

- 新旧链路在迁移期不会互相踩坏，也不会因为一个入口切换导致另一个入口不可用

当前实现进度：

- 已新增兼容快照接口：
  - `GET /api/story-state/projects/{projectId}/chapters/{chapterId}/compatibility-snapshot`
- 已新增 `story.compatibility.*` 配置项，用于冻结：
  - 页面主入口
  - `story-context` 双读
  - `summary-workflow` 双写
  - 兼容回填执行开关
- 已在 `状态台` 同步增加“灰度边界与开关”面板，前后端口径已对齐

### `Phase 9.4` 回填验收与迁移收口

目标：

- 用固定项目和固定章节完成迁移正确性验证
- 形成迁移验收报告
- 为 `Phase 10` 的测试、观测、回放打下稳定基线

退出条件：

- 迁移结果可验证
- 问题可重现
- 回填可重跑

当前实现进度：

- 已新增项目级迁移总览接口：
  - `GET /api/story-state/projects/{projectId}/backfill-overview`
- 已在 `状态台` 先行展示项目级总览：
  - 总章节
  - 已分析章节
  - scene/state 回填需求
  - 可执行回填章节
- 当前仍保持克制：
  - 先做项目级只读验收入口
  - 暂不直接引入批量写入
- 已新增项目级 dry-run 计划接口：
  - `GET /api/story-state/projects/{projectId}/backfill-project-dry-run`
- 已在 `状态台` 增加项目级 dry-run 面板：
  - 需回填章节
  - 可执行章节
  - 阻塞章节
  - 项目级建议动作
  - 前 6 个章节的 dry-run 清单
- 当前仍坚持：
  - 只补只读验收和项目级计划壳
  - 暂不引入破坏式批量写入
- 已完成首轮线上联调：
  - `backfill-overview`
  - `backfill-project-dry-run`
  - `chapter backfill-analysis`
  - `chapter backfill-dry-run`
  - `compatibility-snapshot`
  均已在线上返回真实数据
- 已完成第二轮线上联调：
  - 已确认当前部署前端产物包含：
    - `项目级 dry-run 计划`
    - `Phase 9 验收提示`
    - `推荐样本章节`
  - 已确认项目级和章节级接口继续稳定返回 `200`
- 其中样本章节当前已修正为：
  - `#32 算法少女苏晚`
  - `#33 训练赛首胜`
  - `#34 宿敌归来`
  - `#35 退役者的邀请`
- 页面级统一人工验收模板已转入 `Phase 10.3`

## 当前阶段判断

- `Phase 8` 当前状态：
  - 代码侧已接近完成
  - 仍待一次统一部署与最终人工验收
- `Phase 9` 已完成
- 后续页面级统一人工验收模板不再阻塞 `Phase 9` 关闭
- 下一步进入 `Phase 10.1`

## 首批兼容重点

### 1. 页面兼容

- `创作台`
- `状态台`
- `生成台`
- `章节工作区`
- `旧写作中心`
- `章节管理`
- `人物管理`
- `世界观管理`

要求：

- 明确哪些页面已经成为主入口
- 明确哪些页面只是迁移备用入口

### 2. API 兼容

- `summary-workflow`
- `story-context`
- `story-orchestration`
- `story-state`
- 旧 `writing` 相关接口

要求：

- 明确哪些接口已经是新主链
- 明确哪些接口仍然必须保留给旧入口

### 3. 数据兼容

- `AIWritingRecord`
- `Chapter`
- `Character`
- `WorldSetting`
- `Outline / Plot / Causality`

要求：

- 明确哪些对象已经进入 `StoryUnit + Facets`
- 明确哪些仍然只是“兼容读模型”

## 首批迁移样本建议

- `旧日王座`
  - `第 32 章`
  - `第 33 章`
  - `第 34 章`
  - `第 35 章`
- 专门联调样本章节：
  - 冷启动空章
  - 单 scene 已完成
  - scene fallback
  - 多 scene 混合状态
  - 空摘要章节

## 建议代码落点

- `backend/src/main/java/com/storyweaver/migration/*`
- `backend/src/main/java/com/storyweaver/compat/*`
- `backend/modules/story-storyunit/src/main/java/com/storyweaver/storyunit/migration/*`
- `backend/modules/story-generation/src/main/java/com/storyweaver/story/generation/migration/*`

## 验证方式

- 回填前后对比
- 固定样本章节验证
- 新旧入口对照验证
- 迁移脚本幂等重跑验证
- 双读/双写日志审计

## 风险

- 如果跳过兼容矩阵，后面统一测试时会重复踩“哪个入口还是旧链”的坑
- 如果直接做大迁移，不做回填报告，错误会很难定位
- 如果不定义灰度边界，章节工作区和旧写作中心会出现状态错位

## 下一步

1. 先冻结 `Phase 9.1` 兼容清单
2. 再冻结迁移样本与回填对象边界
3. 然后才开始 `Phase 9.2` 的回填实现

## 贡献与署名说明

- “新旧主链并存期必须制度化管理，不要边测边猜”的方向由用户与 Codex 共同讨论形成。
- 本文档的阶段拆分、迁移顺序、兼容边界与回填策略由 Codex 整理与撰写。
