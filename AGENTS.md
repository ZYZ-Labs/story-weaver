# 智能体交接与文档落盘规则

## 1. 目标

这份规则只做一件事：

- 让后续模型接手时，不需要重新翻长聊天、长进度和大量代码去猜系统现状。

交接规则必须服务真实开发，而不是写成漂亮但没人执行的制度。

## 2. 固定阅读顺序

新模型接手、长时间中断恢复、或者切换到新需求前，固定按这个顺序读：

1. [agent-context.md](/usr/local/project/github/story-weaver/docs/agent-context.md)
2. [agent-handoff-rules.md](/usr/local/project/github/story-weaver/docs/agent-handoff-rules.md)
3. [GUIDE-20260424-system-capabilities-and-chain-reference-v1.md](/usr/local/project/github/story-weaver/docs/guides/GUIDE-20260424-system-capabilities-and-chain-reference-v1.md)
4. [REPORT-20260424-platform-upgrade-final-closure-v1.md](/usr/local/project/github/story-weaver/docs/reports/REPORT-20260424-platform-upgrade-final-closure-v1.md)
5. [TESTDATA-20260424-phase10-replay-matrix-v1.md](/usr/local/project/github/story-weaver/docs/test-data/TESTDATA-20260424-phase10-replay-matrix-v1.md)
6. [GUIDE-20260424-phase10-page-acceptance-template-v1.md](/usr/local/project/github/story-weaver/docs/guides/GUIDE-20260424-phase10-page-acceptance-template-v1.md)

只有确认当前主链、样本和页面验收入口之后，才去读：

- 相关 controller / service / view
- 对应历史阶段文档
- archived 报告

## 3. 不同任务的额外阅读入口

### 3.1 改摘要工作流

先读：

- 系统能力总览中的 `Summary Workflow`
- [GUIDE-20260413-summary-first-user-manual-v1.md](/usr/local/project/github/story-weaver/docs/guides/GUIDE-20260413-summary-first-user-manual-v1.md)

### 3.2 改章节工作区 / 编排

先读：

- 系统能力总览中的 `Story Orchestration` 与 `AI Writing`
- 页面验收模板

### 3.3 改状态台 / 状态链

先读：

- 系统能力总览中的 `Story State`
- 回放矩阵

### 3.4 改迁移 / 兼容

先读：

- 系统能力总览中的 `Compatibility Flags` 和 `Story State`
- 回放矩阵
- 如有必要，再读 `Phase 9` 历史计划和报告

## 4. 哪些文档必须及时更新

### 4.1 必须立即更新 `docs/progress/*`

以下事件一发生就要更新进度文档：

- 阶段状态变化
- 新接口落地
- 样本章节编号变化
- 线上联调结论变化
- 环境阻塞变化
- 缺陷根因判断变化
- 从临时止血转为根因修复，或确认必须做架构升级
- 收口判断变化

### 4.2 必须立即更新 `docs/plans/*`

以下事件一发生就要更新计划文档：

- 开始新阶段
- 调整实施顺序
- 改变范围边界
- 把页面级验收、回放、迁移等从一个阶段移到另一个阶段

### 4.3 必须立即更新 `docs/agent-context.md`

以下事件一发生就要更新：

- 当前主需求变化
- 当前主需求完成
- 后续阅读顺序变化
- 固定样本入口变化

### 4.4 必须新增报告文档

以下事件不要只写进进度里，必须单独留报告：

- 一轮真实部署联调
- 一轮浏览器人工验收
- 一次阶段收口
- 一次专项评估

### 4.5 必须更新测试样本与模板

以下事件发生时，不要只改代码：

- 固定样本章节变化
- 页面验收顺序变化
- 新主页面或新主链入口出现

必须同步更新：

- 回放矩阵
- 页面验收模板

## 5. 什么内容不该继续堆进进度文档

不要再把进度文档当系统说明书。

下面这些内容应该放别处：

- 当前系统能力：放系统能力总览
- 如何手工验收：放页面验收模板
- 样本章节和回放矩阵：放测试数据文档
- 历史阶段细节：放 phase 文档或 archive

进度文档只保留：

- 当前状态
- 最近关键结论
- 下一步动作
- 阻塞项

## 6. 归档规则

### 6.1 可以归档的内容

- 已完成需求的阶段性 round 报告
- 早期参考 PRD
- 已被新样本替代的旧测试样本
- 已被新入口替代的旧说明文档

### 6.2 不应立即归档的内容

- 当前系统能力总览
- 当前主样本矩阵
- 当前页面验收模板
- 当前主需求的最终收口报告
- 仍被主入口引用的文档

### 6.3 归档后的要求

- 如果文档被物理移动，所有仍然存在的引用必须同步改路径
- 不允许留下明显死链接

## 7. 编码前后的最低动作

### 7.1 开始写代码前

至少确认：

- 当前要改的是哪条链
- 对应的主入口文档是哪一份
- 这次改动会不会影响前端 / 样本 / 验收模板

### 7.2 处理缺陷时必须先追根因

如果当前任务是修 bug，而不是做明确的架构升级，默认遵守下面几条：

- 不允许只用超时、跳过、兜底、重试、降级、前端提示、刷新恢复，就把问题判定为已解决
- 除非用户明确接受阶段性止血，或者已经确认必须做架构升级，否则必须继续定位到根因，再提交最终修复
- 如果先做止血措施，必须同步写清楚：为什么先止血、当前根因假设、验证证据、剩余风险、下一步根因修复入口
- 修复验收标准不是“这个报错暂时不出现”，而是“导致这一类问题的根因被消除，邻近阶段和同链路不会继续以同样方式复发”
- 对多阶段链路，必须横向检查同类型调用。一个阶段暴露出的卡死、超时、中断、状态丢失，默认要排查同链路的相邻阶段，而不是只补当前报错点
- 如果最终只能先止血，交接和汇报里必须明确标注“临时措施”，不能把临时绕过写成“问题已解决”
- 交接或汇报时，必须明确回答四件事：根因是什么，证据是什么，修复边界是什么，还剩什么未证实风险

### 7.3 停止本轮前

至少完成：

- 代码或文档落盘
- 进度文档更新
- 如有阶段切换或主入口变化，更新 `agent-context`

## 8. 当前固定主入口

后续模型默认先看这几份，不再先翻历史 phase 文档：

- [agent-context.md](/usr/local/project/github/story-weaver/docs/agent-context.md)
- [GUIDE-20260424-system-capabilities-and-chain-reference-v1.md](/usr/local/project/github/story-weaver/docs/guides/GUIDE-20260424-system-capabilities-and-chain-reference-v1.md)
- [REPORT-20260424-platform-upgrade-final-closure-v1.md](/usr/local/project/github/story-weaver/docs/reports/REPORT-20260424-platform-upgrade-final-closure-v1.md)
- [TESTDATA-20260424-phase10-replay-matrix-v1.md](/usr/local/project/github/story-weaver/docs/test-data/TESTDATA-20260424-phase10-replay-matrix-v1.md)
- [GUIDE-20260424-phase10-page-acceptance-template-v1.md](/usr/local/project/github/story-weaver/docs/guides/GUIDE-20260424-phase10-page-acceptance-template-v1.md)

## 9. 贡献与署名说明

- 本规则由 Codex 根据本次平台级架构升级的真实推进过程、阶段切换方式、联调习惯和文档维护成本重写整理。
