# Summary Workflow 线上验收报告 Round 6

- Date: 2026-04-13 Asia/Shanghai
- Scope:
  - `Phase 3.3` 普通模式 chat 式摘要采集复验
  - 普通模式草稿到 `proposal / preview` 的主链复验
  - 部署产物文案确认

## 本轮目标

确认部署新版本后，普通模式已经不再只是“有按钮”，而是能真实跑通：

- `chat-turns`
- `proposal / preview`
- 普通模式新文案

## 验收结果

### 1. 普通模式 `chat-turns` 已恢复可用

已通过 `story-weaver-backend` 容器内本机接口真实调用：

- `POST /api/summary-workflow/chat-turns`

对象一：

- `targetType=CHARACTER`
- `intent=CREATE`
- `operatorMode=DEFAULT`

输入：

- “我想要一个很油滑的经纪人，表面圆滑，其实特别会算计，和林沉舟以前有合作。”

结果：

- `HTTP 200`
- 返回：
  - `assistantMessage`
  - `draftSummary`
  - `pendingQuestions`
  - `readyForPreview=true`
  - `selectedProviderId=6`
  - `selectedModel=qwen2.5:3b`

对象二：

- `targetType=CHAPTER`
- `targetSourceId=31`
- `intent=REFINE`
- `operatorMode=DEFAULT`

输入：

- “我想让这章的摘要更明确一点，先让读者看到林沉舟的现实状态，再把邀请函作为触发点抛出来，最后停在他决定去赴约。”

结果：

- `HTTP 200`
- 正常返回摘要草稿与追问

结论：

- 普通模式后端对话链已恢复，不再出现上一轮的超时阻塞

### 2. 普通模式草稿可继续进入 `proposal / preview`

已使用普通模式返回的章节摘要草稿，继续调用：

- `POST /api/summary-workflow/proposals`

结果：

- `HTTP 200`
- 正常返回：
  - `proposal`
  - `preview`
- 说明普通模式已经不是孤立接口，而是可继续接入摘要工作流主链

注意：

- 本轮只验证到 `proposal / preview`
- 未执行 `apply`
- 因此没有新增线上写回脏数据

### 3. 前端部署产物确认

已在 `story-weaver-front` 容器内构建产物中确认以下字符串存在：

- `说想法新增`
- `说想法`
- `AI 整理`
- `让 AI 继续整理`

结论：

- 普通模式最新减负文案已经进入线上静态资源

## 阶段判断

截至本轮：

- `Phase 3.2`：完成
- `Phase 3.3`：核心主链已在线上恢复可用
- `Phase 3`：进入收口阶段

当前剩余更偏产品收口，而不是主链不可用：

- 普通模式追问节奏继续优化
- 浏览器真实点击体验继续验收
- 是否正式结束 `Phase 3`，取决于后续一轮交互体验确认

## 备注

- 本轮没有再修改线上业务数据
- 第 31 章摘要仍保持原值：
  - `两年沉寂后，主角收到旧战队邀请，命运再次启动。`
