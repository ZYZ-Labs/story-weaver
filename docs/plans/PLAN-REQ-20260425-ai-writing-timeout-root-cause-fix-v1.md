# AI Writing 卡住问题根因修复计划

- Req ID: `REQ-20260425-ai-writing-timeout-root-cause-fix`
- Plan ID: `PLAN-REQ-20260425-ai-writing-timeout-root-cause-fix-v1`
- Created At: 2026-04-25 Asia/Shanghai

## 1. 目标

- 回退上一轮临时止血措施
- 找到 `章节工作区 -> 当前镜头生成初稿` 卡住的真正共性根因
- 在 provider 共用层完成彻底修复，并补文档与校验

## 2. 执行步骤

### Step 1 回退临时措施

- 回退 `AIWritingController` 的断线继续执行
- 回退 `AIWritingServiceImpl` 的辅助阶段超时跳过
- 回退前端的本地持久化与刷新恢复逻辑

### Step 2 根因修复

- 核对 `AIProviderServiceImpl` 中 provider 超时与全局超时的合并逻辑
- 修复 `generateText / streamText / tool calling` 共用请求超时解析
- 补独立单测，锁住新优先级

### Step 3 落盘与验证

- 更新 `agent-context`
- 新建 requirement / progress 文档
- 执行后端编译、前端类型检查与目标单测

## 3. 当前状态

- `Step 1` 已完成
- `Step 2` 已完成
- `Step 3` 已完成
- 当前剩余的是用户侧章节工作区回归确认，不再存在代码层未落盘事项
