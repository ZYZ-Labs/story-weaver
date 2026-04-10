# 旧日王座线上回归报告（第二轮）

日期：2026-04-10

目标项目：`旧日王座`（projectId=`28`）

目标地址：`https://home.silvericekey.fun:41202/`

## 本轮结论

本轮线上回归确认：这次代码已经真正进入线上主链，但效果是“部分生效”，不是“问题已解决”。

更准确地说：

- Step 9 已在线生效：
  - 新生成记录已经写入 `generationTrace.readerReveal`
  - 写作链路会把“不要默认读者知道未揭晓前情”带入本轮 trace
- Step 10 的建议链路已在线生效：
  - 摘要建议接口已经能返回 `proposedCreates`
  - 新人物 / 新因果候选可以被识别为待确认新增对象
- 但核心稳定性问题仍未过关：
  - 总导在真实 DeepSeek 上依旧 `fallback`
  - 新生成记录仍可能在结尾停在半句，收束检查没有真正兜住
  - `旧日王座` 第 32-34 章仍然因为锚点缺失处于 `blocked`

## 线上证据

### 1. 新链路已经进入线上主链

我在第 31 章重新发起了一次新的 `continue` 生成：

- `chapterId=31`
- 新记录：`ai_writing_record.id=35`

该记录已经具备新的 trace 结构：

- `generationTrace.readerReveal.openingMode=chapter_continue`
- `generationTrace.readerReveal.revealTargets` 已写入
- `generationTrace.readerReveal.forbiddenAssumptions` 已写入
- `generationTrace.director.mode=fallback`

这说明：

- 新版后端确实已经部署
- Step 9 不是只停留在本地代码里，线上请求已消费到新字段

### 2. 总导兼容性仍未修好，只是更可见了

新记录 `id=35` 的总导结果为：

- `director.mode=fallback`
- `decisionSummary=总导已执行真实模型决策，但最终返回格式不可解析，当前回退到启发式 decision pack。`

而重新调用项目一致性接口后，当前项目结果变为：

- `score=0`
- `status=critical`
- `directorFallbackCount=4`
- `directorFallbackRate=100.0`

说明：

- Step 6 的“fallback 显式化”已经生效
- 但真实 Provider 兼容问题本身没有被消除
- 现在只是从“假装总导在工作”变成了“明确告诉你总导还在回退”

### 3. 新生成记录仍然会半句截断

第 31 章新记录 `id=35` 的生成内容开头是自然承接的，没有再出现明显“从空气里硬切入”的问题，这说明开场约束有帮助。

但它的结尾仍然停在：

- `他又调出银行账户。游戏仓的`

也就是明显的半句截断。

这说明：

- Step 9 对“开场边界”有效
- 但“半句截断 / 未完成收束”的检查还没有真正拦住线上结果
- 当前 `check -> revise` 链路对这类失败样本仍然存在漏检

### 4. Step 10 的建议链路在线有效

我没有直接落库污染项目数据，而是用非落库方式验证了建议接口。

章节摘要建议接口：

- `POST /api/projects/28/chapters/31/brief/suggest`

当输入包含新因果时，响应已返回：

- `proposedCreates[0].entityType=causality`

当输入包含重复出现的新人物名 `沈归尘` 时，响应已返回：

- `proposedCreates[0].entityType=character`
- `candidateFields.name=沈归尘`

说明：

- Step 10 的“建议识别”已经在线
- 作者后续的“确认创建 / 补完设置”前置条件已经具备

### 5. 项目整体仍然被旧数据卡住

当前各章 readiness 如下：

- `chapterId=31`
  - `score=90`
  - `status=warning`
  - 仅缺少必出人物约束
- `chapterId=32`
  - `score=60`
  - `status=blocked`
  - 缺少章纲或章节 brief
- `chapterId=33`
  - `score=25`
  - `status=blocked`
  - 缺少章节 brief、POV、人物锚点
- `chapterId=34`
  - `score=25`
  - `status=blocked`
  - 缺少章节 brief、POV、人物锚点

同时项目一致性接口仍显示：

- `traceCoverageRate=25.0`
- `traceMissingCount=3`
- `povIssueCount=2`
- `namingRiskCount=1`

说明：

- 第 32-34 章还没有用新链路重新生成
- 旧数据本身仍然处于高风险状态
- 所以这轮部署并不会自动“治好历史章节”

## 根因判断

本轮我给出的根因排序如下：

1. Step 9 已经进线上，但只解决了“开场边界”，还没有解决“结尾完整性”。
2. 总导真实 Provider 的 JSON 兼容问题仍然存在，所以最新章节依旧 100% 依赖 fallback。
3. 第 32-34 章的锚点缺失没有补齐，导致新架构即使上线，也没有足够输入让这些章节变稳。
4. 当前系统虽然能识别待确认新增对象，但作者侧“先补锚点再生成”的工作流还没有被强制执行。

## 建议动作

### P0

- 先修“半句截断”：
  - 把结尾完整性检查从软判断提升为硬阻断
  - 重点拦截：
    - 结尾停在助词 / 结构词
    - 末句无谓语完成
    - 明显未完成动作收束

- 先修“blocked 仍可生成”：
  - `GenerationReadinessVO.status=blocked` 时，不再放任写作主链继续生成
  - 至少要求用户确认“强制继续”，或者直接拒绝执行

### P1

- 优先补齐 `旧日王座` 第 32-34 章：
  - 章节 brief
  - `mainPovCharacterId`
  - `requiredCharacterIds`

- 然后再用新链路重生这三章草稿，否则第二轮回归没有意义

### P2

- 继续处理总导真实兼容：
  - DeepSeek 返回格式仍不稳定
  - 需要进一步把返回包装、非 JSON 前后缀、Markdown 围栏等异常做更强兼容

## 直接判断

如果现在继续直接生成 `旧日王座` 后三章，结果仍然不会稳。

不是因为这次改动没生效，而是因为：

- 新链路只在“新生成记录”上生效
- 历史坏数据还在
- readiness 的 `blocked` 还没真正阻止错误生成
- 总导 fallback 还没治好

所以这轮回归的结论不是“失败”，而是：

- 架构方向对了
- 线上接入成功了
- 但要真正稳定，还必须补一轮 `P0 + P1`
