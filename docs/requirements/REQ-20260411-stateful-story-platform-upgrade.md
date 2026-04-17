# Story 平台级架构升级：Summary First、StoryUnit、MCP/State Server 与多 Session 编排

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Status: In Progress
- Created At: 2026-04-11 Asia/Shanghai
- Updated At: 2026-04-11 Asia/Shanghai

## 背景

在 `REQ-20260409-generation-reliability-refactor` 及其三轮真实线上回归之后，当前系统的问题已经从“生成链路不稳定”进一步升级为“平台形态与产品形态错位”：

- 后端核心能力仍以 `prompt orchestration` 为主，状态承接能力不足。
- 总导、写手、审校、上下文拼接过度耦合，导致 prompt 膨胀、调试困难、真实 Provider fallback 率高。
- 世界观、人物、物品、技能、因果、剧情、章节锚点等信息主要靠应用层临时拼接，缺少一个统一的、可被 MCP/State Server 读取的稳定基础单元。
- 前端仍保留较强的“后台表单 + 字段录入”结构，和未来要承载的多状态、多镜头、多 session 创作工作流不匹配。
- 用户真正需要的是“摘要驱动的创作工作台”，而不是“对象字段管理后台”。

基于 2026-04-11 的进一步讨论，这次升级不再被视为“继续优化一条写作链路”，而是一次平台级架构升级。目标是把当前系统从：

- `多轮 prompt 修补`

升级为：

- `Summary First`
- `StoryUnit + Facets`
- `MCP/State Server 驱动的状态读写`
- `总导 / 选择器 / 写手 / 审校器` 多 session 编排
- `章节骨架 + 镜头执行 + 状态交接`

## 核心判断

### 1. 结构化依然必要，但不能继续直接暴露给用户

- 结构化不是为了让作者填字段，而是为了让 MCP/State Server、编排层、状态机和回放系统有稳定输入。
- 用户界面默认只应展示摘要，而不是原始 JSON 字段。
- 系统内部必须保留结构化 facet，但这些 facet 应该下沉到后台协议层。

### 2. 当前问题不只是模型问题，而是架构问题

- 继续在现有总导 prompt 上叠规则，会持续进入“规则越多，链路越脆”的循环。
- 正确方向是把“决定写什么”和“把它写出来”拆开，并把状态交接外置。

### 3. 后续必须升级为模块化单体

- 仍可维持单部署、单 JAR。
- 但内部必须拆成多模块，不再继续把 Provider、Setting、Generation、MCP/State Server、Story State 全塞在同一层里。

### 4. 每次开发前必须先拆详细计划文档

- 这是为了支持 agent 切换、长周期开发和可回放。
- 后续所有 Epic 级和子任务级实施，都必须先落文档，再进入编码。

## 目标

- 建立一套统一的 `StoryUnit` 基础单元与 facet 体系，作为 MCP/State Server 的基础读写单位。
- 明确区分并承载：
  - `Summary`
  - `Canon`
  - `State`
  - `Relation`
  - `Reveal`
  - `Execution`
- 建立本地 `MCP Server` 与 `State Server`，把上下文读取、运行时状态、快照与增量变化从 prompt 中剥离出来。
- 把章节写作链路升级为：
  - 章节总导
  - 选择器
  - 写手
  - 审校器
  四 session 协作模型。
- 把“按章一把生成”升级为“章节骨架 -> 镜头候选 -> 镜头执行 -> 镜头交接 -> 全章审校”。
- 重做前端信息架构，使普通作者只看到摘要和当前创作动作，专家模式再暴露结构层和调试层。
- 建立长期可维护的文档体系、命名规则、实施计划制度和 agent 交接入口。

## 范围内

- 模块化单体拆分方案。
- `StoryUnit + Facets + Patch + Snapshot + Event` 元模型。
- 本地 `MCP Server` 与 `State Server` 的职责边界、协议和第一批工具清单。
- 多 session 编排架构。
- 镜头级写作工作流。
- 运行时状态与增量状态体系：
  - 背包
  - 技能状态
  - 态度
  - 关系
  - 好感度
  - 世界影响
  - Reader Reveal State
- 前端信息架构重构：
  - 创作工作台
  - 故事台
  - 状态台
  - 生成台
  - 系统台
- 文档治理流程。

## 范围外

- 继续在旧总导 prompt 上做无限制微调。
- 再扩一批无统一状态协议支撑的新业务模块。
- Big Bang 式一次性重写全部旧代码和旧页面。
- 一开始就做完全自动化、无限自主的 Agent 系统。
- 把用户暴露面重新做回字段录入后台。

## 平台级原则

### 1. Summary First

- 用户默认只看摘要。
- 所有结构化字段都服务于系统，不直接作为前台主交互。
- 用户可确认“摘要变化”，而不是逐字段确认。

### 2. Structured Behind

- 结构化不能消失，只能下沉。
- JSON 字段、facet、patch、snapshot、event 都应成为 MCP/State Server 和编排层的内部协议。

### 3. StoryUnit Is a Protocol Envelope, Not a God Object

- `StoryUnit` 不是万能基类，也不是大杂烩总表。
- 它是统一读写协议外壳，用于承载多个 facet 的引用与快照语义。

### 4. State Must Be Externalized

- session 之间的承接不能依赖聊天历史。
- 状态必须写入 `State Server`，通过快照和增量进行交接。

### 5. Small Prompts, Strong Protocols

- 后续不追求一个超长 prompt 解决所有问题。
- 每个 session 的 prompt 只承担单一职责。
- 复杂度应从 prompt 文案转移到协议、状态和编排层。

### 6. Progressive Planning

- 这是长周期工程。
- 平台总计划之外，每次编码前都必须拆到本轮实施计划。
- 计划、进度和 agent-context 必须同步更新。

## 高层验收标准

- 新架构主线文档、核心架构文档、前端信息架构文档和治理文档建立完成，并可作为后续开发基线。
- 后端内部形成明确模块拆分方案，且能够在单 JAR 约束下演进为模块化单体。
- `StoryUnit + Facets` 成为后续对象读写、生成编排和 MCP/State Server 的统一协议基础。
- 前端主交互不再默认暴露对象字段表单，而是以摘要、工作台和状态面板为核心。
- 多 session 写作链路具备明确的职责切分、输入输出协议与状态交接定义。
- 文档治理流程可支持数周级、上千文件级工程推进与智能体切换。

## 风险与代价

- 工程量显著增加，已不再是 MVP 级改造。
- 若先做过细的状态体系而不做优先级裁剪，容易在前期拖慢主链推进。
- 若不强制执行文档治理，后续多阶段演进会迅速失控。
- 若 MCP/State Server 被直接交给模型自由探索，而不是由应用编排层先接管，稳定性反而可能下降。

## 关键决策记录

- [2026-04-11 Asia/Shanghai] 确认当前系统问题已从“生成可靠性不足”升级为“平台形态不适合继续扩展”，需要平台级架构升级。
- [2026-04-11 Asia/Shanghai] 确认用户前台默认只展示摘要，结构化字段转入系统内部协议层。
- [2026-04-11 Asia/Shanghai] 确认采用 `StoryUnit + Facets` 作为 MCP/State Server 的统一基础读写单位。
- [2026-04-11 Asia/Shanghai] 确认后续写作主链升级为四 session：
  - 总导
  - 选择器
  - 写手
  - 审校器
- [2026-04-11 Asia/Shanghai] 确认章节生成从“全章一把写”升级为“章节骨架 + 镜头执行 + 状态交接”。
- [2026-04-11 Asia/Shanghai] 确认后续每次开发前都要先拆详细计划文档，并将其作为 agent 切换时的优先阅读材料。

## 贡献与署名说明

- 原始问题、产品方向、平台升级诉求：用户提出。
- 架构术语收敛、原则整理、范围边界、文档结构与正文撰写：Codex 完成。
- 核心路线判断与方案取舍：用户与 Codex 共同讨论形成。
