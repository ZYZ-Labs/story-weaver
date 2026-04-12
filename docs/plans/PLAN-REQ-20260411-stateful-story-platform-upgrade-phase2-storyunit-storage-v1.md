# Story 平台升级 Phase 2 详细实施计划：StoryUnit 存储映射与服务协议落地

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Plan ID: PLAN-REQ-20260411-stateful-story-platform-upgrade-phase2-storyunit-storage-v1
- Status: Completed
- Created At: 2026-04-12 Asia/Shanghai
- Updated At: 2026-04-12 Asia/Shanghai

## 本轮目标

在不引入“大总表”和不打断现有业务主链的前提下，完成 `StoryUnit + Facets` 的首轮存储映射策略、服务协议和首批对象族落地方案，为后续：

- `Summary First` 对象工作流
- `MCP / LSP` 只读接入
- 多 session 编排读取统一上下文

提供稳定基础。

本轮只追求三件事：

- 明确 `StoryUnit` 的存储映射策略
- 建立首批读服务和映射装配协议
- 为人物 / 世界观 / 章节三类对象建立统一映射基线

当前阶段定位：

- 这是 `Phase 2` 的详细实施计划文档
- `Phase 2.1 / 2.2 / 2.3` 已按本计划执行完成
- 后续 `Phase 3 / Phase 4` 将继续消费本阶段产出的统一读模型与摘要协议

## 本轮原则

- 不做万能总表
- 不做 Big Bang 数据库重构
- 现有业务表暂时仍是 source of truth
- `StoryUnit` 先作为协议视图和装配结果存在
- `SummaryFacet` 优先，`Canon / State / Relation / Reveal / Execution` 渐进补齐

一句话说：

- 先做统一读写语义
- 再做真正的独立状态存储

额外约束：

- 不在这个阶段把 `StoryUnit` 做成数据库超级对象
- 不在这个阶段为所有对象族追求“一次性全接入”
- 不在这个阶段引入新的 AI 工作流耦合

## 核心设计判断

### 1. 采用 Projection First，而不是先建统一持久化总表

首轮不新建一个承载所有 facet 的超级表。  
而是采用：

- 现有关系表继续保存业务事实
- `StoryUnit` 通过 adapter / assembler / query service 从现有表投影出来

原因：

- 当前业务已经有成熟实体表，强行一次性折叠到新表风险过高
- 先做 projection，可以尽快让 MCP/LSP 和多 session 有统一读取对象
- 后续如果确实需要专用 snapshot / event / patch 存储，再增量引入

### 2. SummaryFacet 先成为默认输出

首轮不追求所有 facet 都完整。优先级应是：

1. `SummaryFacet`
2. `CanonFacet`
3. `RelationFacet`
4. `RevealFacet`
5. `ExecutionFacet`
6. `StateFacet`

原因：

- 用户层只看摘要
- MCP/LSP 第一阶段也更依赖稳定摘要和 canon，而不是所有运行时状态一次做完

### 3. 首批对象族只做三类

本阶段只做：

- `Character`
- `WorldSetting`
- `Chapter`

原因：

- 这三类对象已经直接支撑现有写作链路
- 也是后续 reader reveal、anchor bundle 和 scene execution 最常用的基础对象

## 范围内

- 明确 `StoryUnit` 的投影式存储映射策略
- 明确首批 facet 的字段来源和装配规则
- 定义：
  - `StoryUnitRegistry`
  - `StoryUnitAssembler`
  - `StoryFacetAssembler`
  - `StorySummaryService`
  - `StoryUnitProjectionService`
- 落首批 query/read service 的实现骨架
- 为 `Character / WorldSetting / Chapter` 建立映射矩阵
- 明确后续 patch / snapshot / event 的存储承载建议
- 补齐分阶段开发顺序、测试口径、验收口径和风险清单

## 范围外

- 不在本阶段落地 MCP 运行服务
- 不在本阶段落地 LSP 服务器实现
- 不在本阶段继续迁移 `generation.impl`
- 不在本阶段继续迁移 controller / security / provider 实现层
- 不在本阶段重写前端对象页
- 不在本阶段把所有对象族都接入 StoryUnit

## 分阶段实施拆分

### `Phase 2.1` 协议补齐与映射矩阵冻结

目标：

- 把 `StoryUnit` 从“只有抽象协议”推进到“有明确装配协议”
- 为三类对象族冻结字段来源矩阵

交付：

- `StoryUnitRegistry`
- `StoryUnitAssembler`
- `StoryFacetAssembler`
- 三类对象族 mapping matrix 文档

当前状态：

- 已完成

退出条件：

- 任意一个对象族都能回答：
  - 从哪些表读
  - 如何装成哪些 facet
  - 哪些 facet 暂时为空

### `Phase 2.2` 首批 Projection Service 落地

目标：

- 在 `backend` 中实现首批统一读服务骨架

交付：

- `CharacterStoryUnitProjectionService`
- `WorldSettingStoryUnitProjectionService`
- `ChapterStoryUnitProjectionService`
- 统一 `StoryUnitProjectionService`

当前状态：

- 核心实现已完成

退出条件：

- 三类对象都能被统一读取为 `StoryUnit`

### `Phase 2.3` Summary 回生成与对外读模型收口

目标：

- 让 `SummaryFacet` 成为真正稳定的默认输出
- 为 `Phase 3 / Phase 4` 准备可消费的统一读模型

交付：

- `StorySummaryService`
- `SummaryFacet` 基础回生成规则
- 面向 MCP/后续编排的查询契约

退出条件：

- 后续阶段不必再直接依赖原始实体字段来拼用户可读摘要

当前状态：

- 已完成

## 首轮存储策略

### 1. Source of Truth

首轮 source of truth 保持不变：

- 人物相关表
- 世界观相关表
- 章节相关表
- 项目与关联关系表

### 2. Projection Layer

新增统一投影层：

- `StoryUnitProjectionService`
- `StoryUnitAssembler`
- `StoryFacetAssembler`

职责：

- 从现有实体和关联关系装配 `StoryUnit`
- 输出标准 facet
- 向后续 session / MCP 暴露统一读取结果

### 3. Snapshot / Patch / Event 承载策略

本阶段先冻结协议，不强行上全量落库。

建议：

- `snapshot`
  - 先支持内存/临时对象层协议和后续可持久化接口
- `patch`
  - 先支持结构化提案对象，不强制落地专用表
- `event`
  - 先定义事件对象和挂载接口，不立即做完整事件总线

### 4. Source of Truth 与投影输出的边界

必须明确：

- 现有实体表负责“事实存储”
- `StoryUnitProjectionService` 负责“统一读取视图”
- 后续 MCP/LSP 默认优先读投影结果，而不是直接读底层表结构

这样做的结果是：

- 现有业务服务暂时不用重写
- 但系统外部消费口径已经统一

## 首批包与接口建议

建议优先落在：

- `story-storyunit`
  - 协议与抽象接口
- `backend`
  - 首批 assembler / projection service 实现

建议新增或完善的包：

- `com.storyweaver.storyunit.registry`
- `com.storyweaver.storyunit.assembler`
- `com.storyweaver.storyunit.summary`
- `com.storyweaver.storyunit.mapping`
- `com.storyweaver.storyunit.service.impl`

建议新增或完善的类：

- `StoryUnitRegistry`
- `DefaultStoryUnitRegistry`
- `StoryUnitAssembler<T>`
- `AbstractStoryUnitAssembler<T>`
- `StoryFacetAssembler<T>`
- `StoryUnitProjectionService`
- `DefaultStoryUnitProjectionService`
- `StorySummaryService`
- `RuleBasedStorySummaryService`

## 具体任务拆分

### Task 1. 冻结存储映射策略

需要落地：

- 明确 projection-first 方案
- 明确首轮不引入总表
- 明确 source of truth 仍在现有业务表
- 明确哪些 facet 来自实时装配，哪些 facet 暂为空壳

验收：

- 文档和代码命名统一，不再出现“是否要先建总表”的摇摆
- 所有后续接口都默认围绕 projection 视图组织

### Task 2. 建立 StoryUnit Registry

需要落地：

- `StoryUnitRegistry`
- `StoryUnitType -> adapter/assembler` 注册入口
- 对不同对象族的统一分发协议

验收：

- 外层读取某类对象时，不再直接手写 if/else 拼接
- registry 能明确区分：
  - 读取哪个对象族
  - 调用哪个 assembler
  - 返回哪个 `StoryUnitType`

### Task 3. 建立 Facet Assembler 协议

需要落地：

- `SummaryFacetAssembler`
- `CanonFacetAssembler`
- `RelationFacetAssembler`
- `RevealFacetAssembler`
- `ExecutionFacetAssembler`

验收：

- facet 的构造职责清晰，不把所有拼接塞进一个 service
- 每个对象族能单独替换自己的 facet assembler

### Task 4. 落地首批 Projection Service

需要落地：

- `CharacterStoryUnitProjectionService`
- `WorldSettingStoryUnitProjectionService`
- `ChapterStoryUnitProjectionService`

首轮能力：

- 根据现有实体表和关联表装配 `StoryUnit`
- 返回最小可用的 `SummaryFacet + CanonFacet + RelationFacet`

验收：

- 三类对象都能被统一读取成 `StoryUnit`
- 不需要 controller/service 层手动拼 facet

### Task 5. 建立 Summary 回生成协议

需要落地：

- `StorySummaryService`
- 从结构化 facet 回生成用户可读摘要的最小规则

首轮目标：

- 即使暂时不用模型，也能先生成稳定基础摘要
- 后续再逐步替换为 AI 辅助摘要

验收：

- 用户侧默认展示不再依赖原始字段直出
- `SummaryFacet` 在没有模型参与时也能稳定产出基础结果

### Task 6. 冻结下一阶段输入需求

需要落地：

- 为 `Phase 3` 的 Summary First 工作流准备输入契约
- 为 `Phase 4` 的 MCP 只读服务准备查询契约

验收：

- `Phase 2` 结束后，后续阶段可以直接消费统一 StoryUnit 读模型

### Task 7. 建立验证与回归口径

需要落地：

- 为三类对象族补最小单测或装配测试
- 明确本阶段的 compile / test / 手工校验方式
- 明确哪些结果写进 progress 文档

验收：

- 后续进入编码时，不会再临时决定“怎么证明投影是对的”

## 首批对象族映射要求

### Character

至少映射：

- `SummaryFacet`
  - 人物一句话摘要
  - 当前状态摘要
  - 关系摘要
- `CanonFacet`
  - 设定身份、定位、目标
- `RelationFacet`
  - 项目关联、关系挂点

建议来源：

- `character`
- `project_character`
- 相关章节/大纲关联表

首轮暂不强求：

- 背包
- 技能状态
- 好感度
- 复杂运行时态度值

### WorldSetting

至少映射：

- `SummaryFacet`
  - 设定摘要
  - 影响摘要
- `CanonFacet`
  - 类型、规则、影响范围
- `RelationFacet`
  - 与项目、章节、人物的关联

建议来源：

- `world_setting`
- `project_world_setting`
- 相关章节/大纲关联表

### Chapter

至少映射：

- `SummaryFacet`
  - 章节摘要
  - 当前生成状态摘要
- `CanonFacet`
  - 标题、序号、基础定位
- `RelationFacet`
  - 大纲、剧情、人物挂接
- `ExecutionFacet`
  - 预留当前锚点、骨架、readiness 接口位

建议来源：

- `chapter`
- `chapter_character`
- `chapter_plot`
- 相关 `outline / plot / ai_writing_record` 信息

## 对象族映射矩阵要求

进入编码前，至少要补齐这三张矩阵：

1. `Character -> Facet -> Source Table/Service`
2. `WorldSetting -> Facet -> Source Table/Service`
3. `Chapter -> Facet -> Source Table/Service`

每张矩阵至少包含：

- `facet`
- `field`
- `source`
- `assembler`
- `fallback strategy`
- `phase`

## 建议代码落点

### `story-storyunit`

承载：

- 注册接口
- assembler 抽象接口
- projection service 抽象接口
- summary service 抽象接口
- mapping 元信息接口

### `backend`

承载：

- 首批 assembler 实现
- projection service 实现
- 实体表读取与组装
- 规则型 summary service 初版实现

原因：

- 当前 repository 和业务查询实现仍在 `backend`
- 本阶段目标是先打通统一投影，不是先搬实现层模块

## 开发顺序建议

1. 先补接口和抽象类
2. 再做 `Character` 投影
3. 再做 `WorldSetting` 投影
4. 再做 `Chapter` 投影
5. 最后做 `SummaryFacet` 统一回生成

这样安排的原因：

- `Character` 最容易验证
- `WorldSetting` 次之
- `Chapter` 最复杂，放后面更稳

## 测试与验证计划

### 编译验证

- `mvn -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -DskipTests compile`

### 最小测试目标

- 三类对象族至少各有一组装配测试
- registry 至少有一组注册分发测试
- summary service 至少有一组规则回生成测试

### 手工校验点

- `SummaryFacet` 是否可读
- 空值和缺失关联是否有明确 fallback
- 输出是否稳定，不依赖前端二次拼接

## 风险清单

### 风险 1. 把 projection service 写成第二套业务 service

规避：

- 只负责读取与装配
- 不在本阶段承接复杂业务写入

### 风险 2. SummaryFacet 逻辑分散

规避：

- 集中到 `StorySummaryService`
- 不允许每个 controller 自己拼摘要

### 风险 3. Chapter 映射过早吃进太多运行时状态

规避：

- 首轮只接最小章节摘要、基础 canon、关系、execution 预留位

### 风险 4. 把 Phase 2 做成数据库改造工程

规避：

- 本阶段坚持 projection-first
- 不新增总表，不做大迁移

## 本轮退出条件

`Phase 2` 的这第一份详细计划只有在下面条件满足后才算执行完成：

- `StoryUnit` 存储映射策略已经固定为 projection-first
- 三类对象族的映射矩阵已经明确
- 统一 registry / assembler / projection service 协议已建立
- 至少三类对象可以被读取为统一 `StoryUnit`
- 主进度和 agent context 已切到 Phase 2
- 用户已能基于此计划判断是否进入编码

## 当前阶段进展

### `Phase 2.1` 已完成

- 已补齐：
  - `story-storyunit.registry`
  - `story-storyunit.assembler`
  - `story-storyunit.mapping`
- 已新增：
  - `DefaultStoryUnitRegistry`
  - `AbstractStoryUnitAssembler`
  - `StoryUnitMappingMatrix`
  - `StoryFieldMapping`
- 已新增映射矩阵文档：
  - `docs/architecture/ARCH-REQ-20260412-storyunit-mapping-matrix-v1.md`

### `Phase 2.3` 已完成

- 已新增：
  - `story-storyunit.summary.StorySummaryDraft`
  - `story-storyunit.summary.StorySummaryService`
  - `backend.storyunit.service.impl.RuleBasedStorySummaryService`
- 已完成收口：
  - `CharacterStoryUnitAssembler`
  - `WorldSettingStoryUnitAssembler`
  - `ChapterStoryUnitAssembler`
- 已完成统一查询出口：
  - `StoryUnitQueryService#getProjected`
  - `StoryUnitQueryService#listProjected`
- 已补最小规则测试：
  - `RuleBasedStorySummaryServiceTest`
- 已新增架构说明：
  - `docs/architecture/ARCH-REQ-20260412-story-summary-read-model-v1.md`

## 风险与约束

- 如果本阶段就急着落全量 state 存储，会把节奏拖死
- 如果把 summary 生成完全绑定到模型，基础稳定性会不足
- 如果不限制对象族范围，阶段会迅速失控

因此本阶段必须克制：

- 先把 `Character / WorldSetting / Chapter` 做通
- 先把读取做通
- 先把摘要回生成功能做稳

## 本轮验证结果

- 已通过：
  - `mvn -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -DskipTests compile`
  - `mvn test -pl backend -am -Dtest=RuleBasedStorySummaryServiceTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2`
  - `git diff --check`

## 贡献与署名说明

- `StoryUnit` 需要承载 MCP/LSP 基础读写，但不能变成超级大对象的方向：用户提出。
- `projection-first`、首批对象族、阶段拆解与实施路线：Codex 完成。
- 当前方案由用户与 Codex 共同讨论形成。
