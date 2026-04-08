# AI 写作工作流修订展示缺陷 实施计划

- Req ID: REQ-20260408-writing-workflow-ui-bug
- Plan Version: v1
- Status: Completed
- Created At: 2026-04-08 Asia/Shanghai
- Updated At: 2026-04-08 Asia/Shanghai

## 实施范围

本次修复只处理前端对 AI 写作工作流的阶段展示和修订结果反馈，不改动写作模型策略，不调整 chat 面板交互，也不改变对外写作接口。

修复目标：

- 让日志面板正确显示 `prepare`、`context`、`plan`、`write`、`check`、`revise`
- 当修订稿通过 `replace` 事件覆盖正文时，用户能明确感知
- 保证写作中心和章节页初稿助手共用一套修复结果

## 涉及模块

- `front/src/stores/writing.ts`
- `front/src/components/AIProcessLogPanel.vue`
- `front/src/types/index.ts`

## 问题拆解

### 问题 1

- 现象: 日志面板未对 `prepare` 和 `context` 做中文阶段映射
- 影响: 用户看到原始英文阶段名，流程可读性差
- 修复策略: 在 `AIProcessLogPanel` 中补齐阶段映射和文案

### 问题 2

- 现象: `replace` 事件只会静默替换正文，不会进入日志列表
- 影响: 用户不容易知道当前预览内容已经变成修订稿
- 修复策略: 在共享流状态层收到 `replace` 时追加显式日志

### 问题 3

- 现象: 空日志时的占位文案只提到规划、写作、自检和修订
- 影响: 与真实工作流阶段不一致
- 修复策略: 调整占位文案，覆盖准备上下文与背景整理阶段

## 实施步骤

### Step 1

- 目标: 修复日志面板阶段映射
- 改动:
  - 更新 `AIProcessLogPanel.vue` 的阶段映射
  - 调整空态说明文案
- 完成标准:
  - `prepare` 和 `context` 以明确中文名称展示

### Step 2

- 目标: 为 `replace` 事件补充显式修订反馈
- 改动:
  - 在 `writing.ts` 中处理 `replace` 事件时追加日志项
  - 为该日志项绑定 `revise` 阶段语义
- 完成标准:
  - 修订稿覆盖正文时，日志中出现“已应用修订稿”一类提示

### Step 3

- 目标: 做可执行验证并补进度文档
- 改动:
  - 运行前端构建或类型检查
  - 更新 bug 进度文档和 `agent-context`
- 完成标准:
  - 至少完成一次前端构建验证

## 验证方案

- 构建验证：`npm run build`
- 人工验证：
  - 流式生成开始时能看到准备和背景整理阶段
  - 修订触发后，日志面板可见修订已应用提示
  - 写作中心和章节页均共享相同日志行为

## 风险与回退

- 风险: 仅前端补日志后仍不足以表达复杂修订语义
- 处理: 保留后续在后端增加显式事件的空间，但本次先不扩大范围

- 回退方案:
  - 仅涉及前端展示层和共享 store，可直接回退相关文件改动
