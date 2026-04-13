# Summary Workflow 线上验收报告 Round 5

- Date: 2026-04-13 Asia/Shanghai
- Scope:
  - `Phase 3.3` 普通模式 `chat-turns` 真实可用性复验
  - 线上测试数据清理
  - 本地修复方案落盘

## 本轮目标

确认两件事：

- 普通模式的 `POST /api/summary-workflow/chat-turns` 是否已在线上真实可用
- 若仍不可用，明确是否继续停留在 `Phase 3`

## 验收结果

### 1. 普通模式 `chat-turns` 线上仍超时

已通过 `story-weaver-backend` 容器内本机接口真实调用：

- `POST /api/summary-workflow/chat-turns`

测试条件：

- `targetType=CHARACTER`
- `projectId=28`
- `intent=CREATE`
- `operatorMode=DEFAULT`
- 输入为“新增一个油滑、会算计、与林沉舟旧识的经纪人”

实际结果：

- 容器内 `wget --timeout=25` 最终返回：
  - `wget: download timed out`
- 说明：
  - 普通模式当前仍会卡在模型调用阶段
  - 当前线上版本不满足“对话采集可用”验收标准

结论：

- `Phase 3.3` 不能视为完成
- `Phase 4` 不能启动

### 2. 线上测试数据已恢复

本轮已将 `旧日王座` 第 31 章摘要恢复为原值：

- 原值：
  - `两年沉寂后，主角收到旧战队邀请，命运再次启动。`

已验证：

- `GET /api/projects/28/chapters/31`
- 返回摘要已恢复为原值

### 3. 本地修复已完成，待部署复验

本地已完成三项修复：

- `summary-workflow` 路由改为优先走 `naming_ai_provider_id / naming_ai_model`
- 摘要对话新增短超时配置：
  - `STORY_SUMMARY_WORKFLOW_CONVERSATION_TIMEOUT_SECONDS`
  - `STORY_SUMMARY_WORKFLOW_CONVERSATION_MAX_TOKENS`
- `DefaultStorySummaryConversationService` 新增：
  - 虚拟线程隔离调用
  - 超时后快速回退到本地摘要草稿
  - fallback 日志

本地验证已通过：

- `mvn -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -DskipTests compile`
- `mvn test -pl backend -am -Dtest=DefaultStorySummaryConversationServiceTest,DefaultStorySummaryProposalWorkflowServiceTest,DefaultStorySummaryApplyWorkflowServiceTest,ResilientSummaryProposalStoreTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2`
- `git diff --check`

## 阶段判断

截至本轮：

- `Phase 3.2`：完成
- `Phase 3.3`：进行中
- `Phase 3`：未完成

下一步必须是：

- 部署带“短超时 + 回退”的新 backend
- 重新复验 `POST /api/summary-workflow/chat-turns`
- 只有普通模式真实可用后，才允许进入下一阶段

## 备注

- 本轮重新修正了上一轮“`Phase 3.3` 已完成”的判断
- 当前最大问题不是前端按钮，而是普通模式后端对话链路仍会挂住
