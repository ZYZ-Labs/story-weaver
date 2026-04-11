# 文档治理与开发计划工作流

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Governance Doc Version: v1
- Status: Drafted
- Created At: 2026-04-11 Asia/Shanghai
- Updated At: 2026-04-11 Asia/Shanghai

## 目标

该文档用于规范未来数周级平台开发中的：

- 文档类型
- 命名规则
- 计划拆分规则
- agent 切换时的阅读顺序

## 文档类型

### 1. Requirement

用于定义主需求边界、目标、范围和验收标准。

命名：

- `REQ-YYYYMMDD-<slug>.md`

### 2. Plan

用于定义实施路径。

分两层：

- 主计划
- 本轮详细实施计划

命名：

- 主计划：`PLAN-REQ-<req-id>-vN.md`
- 本轮实施：`PLAN-REQ-<req-id>-<topic>-vN.md`

### 3. Progress

用于记录当前快照和关键节点。

命名：

- `PROGRESS-REQ-<req-id>.md`

### 4. Architecture

用于定义稳定架构边界与协议。

命名：

- `ARCH-REQ-<req-id>-<topic>-vN.md`

### 5. Governance

用于定义流程和制度。

命名：

- `GOV-REQ-<req-id>-<topic>-vN.md`

### 6. Report

用于记录真实测试、回归、线上排查。

命名：

- `REPORT-YYYYMMDD-<topic>.md`

## 开发前置规则

后续每次进入编码前，必须先有：

1. 主 Requirement
2. 主 Plan
3. Progress
4. 至少一份本轮详细实施计划

如果缺少第 4 项，不进入编码。

## 本轮详细实施计划最少内容

- 本轮目标
- 涉及模块
- 读写边界
- 风险
- 验证方式
- 预期交付物

## Agent 切换阅读顺序

恢复工作时必须按如下顺序阅读：

1. `docs/agent-context.md`
2. 当前 Primary Req 对应的 Requirement
3. 当前 Primary Req 对应的 Master Plan
4. 当前 Primary Req 的 Progress
5. 当前正在执行的本轮详细实施计划
6. 相关 Report
7. 再进入代码

## 更新规则

### 编码开始前

- 更新或新增本轮详细计划

### 编码过程中

- 关键架构变更必须同步更新 Architecture 文档

### 编码结束后

- 更新 Progress
- 更新 `agent-context.md`
- 若有真实回归，再新增 Report

## 多主线规则

如果存在两个活跃需求：

- 必须在 `agent-context.md` 中标注优先级
- 必须说明主线与次主线关系

## 署名与来源规则

后续关键文档必须包含：

- 用户提出的原始方向
- Codex 整理与撰写的部分
- 双方共同讨论形成的判断

这是为了在长期协作中保留清晰的贡献来源。

## 贡献与署名说明

- “每次开发前必须拆详细计划文档并写入 agent 入口”的要求：用户提出并确认。
- 文档类型、命名规则、阅读顺序和治理条款整理：Codex 完成。
- 本治理流程由用户与 Codex 共同讨论形成。

