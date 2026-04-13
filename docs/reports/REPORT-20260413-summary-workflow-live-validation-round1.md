# Summary Workflow 线上联调报告（Round 1）

- Date: 2026-04-13 Asia/Shanghai
- Scope:
  - `POST /api/summary-workflow/proposals`
  - `POST /api/summary-workflow/previews`
  - `POST /api/summary-workflow/apply`
- Project:
  - `projectId=28`
  - `旧日王座`
- Validation Target:
  - `chapterId=31`
  - `退役者的邀请函`

## 结论

- `summary-workflow` 三段式主链已在线上部署版本真实跑通。
- `proposalId` 取回式 `preview / apply` 已真实可用。
- `404 / 409` 的异常口径与本地实现一致。
- 运行时存在一个明确问题：
  - Redis proposal store 在线上不可用，当前实际走的是内存回退。

## 实测结果

### 1. proposal 创建

- 输入:
  - `targetType=CHAPTER`
  - `targetSourceId=31`
  - `projectId=28`
  - 新摘要文本为第一章起稿导向摘要
- 结果:
  - `HTTP 200`
  - 返回 `proposal.proposalId`
  - 同时返回 `preview`

### 2. preview by proposalId

- 输入:
  - 仅传 `proposalId`
- 结果:
  - `HTTP 200`
  - 成功返回 `beforeSummary / afterSummary / affectedFacets / warnings`

### 3. apply by proposalId

- 输入:
  - 仅传 `proposalId`
  - `confirmed=true`
- 结果:
  - `HTTP 200`
  - 第 31 章摘要被成功写回
  - `warnings` 中包含：
    - `首轮只写回章节摘要，不修改正文`
    - `readiness 已重新评估`

### 4. 写回后回查

- 回查 `/api/projects/28/chapters`
- 第 31 章摘要已变更为新值
- 随后已用同一链路写回原摘要，当前线上数据已恢复

### 5. 404 异常口径

- 使用不存在的 `proposalId` 调用 `/api/summary-workflow/previews`
- 结果:
  - `HTTP 404`

### 6. 409 异常口径

- 创建合法 proposal 后，使用错误的 `targetRef` 调用 `/api/summary-workflow/apply`
- 结果:
  - `HTTP 409`

## 发现的问题

### Redis proposal store 未真实生效

- 后端日志多次出现：
  - `Redis unavailable when saving summary proposal ...`
  - `Redis unavailable when loading summary proposal ...`
  - `Redis unavailable when deleting summary proposal ...`
- 这说明当前线上虽然接口可用，但 proposal 实际只存于进程内存。

直接影响：

- 当前进程存活期间，`proposalId -> preview/apply` 可正常工作。
- 一旦后端重启，尚未消费的 proposal 将丢失。
- 本轮未能完成“重启后 proposal 仍可恢复”的目标验证。

## 当前判断

- 功能状态:
  - 可用，但只达到“内存回退可用”级别
- 稳定性状态:
  - 单实例短链路可用
  - 不满足“跨重启可靠恢复”的预期

## 建议下一步

1. 先排查线上 Redis 连通与鉴权是否正确。
2. 在 `ResilientSummaryProposalStore` 中补一条更具体的运行日志，至少打印异常类型，便于区分：
   - 网络不可达
   - 认证失败
   - 序列化问题
3. Redis 恢复后再做一次 round 2 联调：
   - 创建 proposal
   - 仅用 `proposalId` preview
   - 重启后端
   - 再仅用 `proposalId` apply

## 贡献与署名说明

- 需求方向与平台思路：
  - 由用户提出与主导
- 本报告的诊断、测试执行、结论整理与文档撰写：
  - 由 Codex 完成
