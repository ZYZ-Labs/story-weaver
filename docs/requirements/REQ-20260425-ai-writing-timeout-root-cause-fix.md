# 章节工作区 AI Writing 卡住问题根因修复

- Req ID: `REQ-20260425-ai-writing-timeout-root-cause-fix`
- Created At: 2026-04-25 Asia/Shanghai
- Status: In Verification

## 1. 背景

- `章节工作区 -> 当前镜头生成初稿` 在 `plan` 阶段会长期停留在“写作计划仍在生成中，请稍候”
- 页面刷新后流式连接消失，会让用户误以为进度被吞掉
- 上一轮通过“辅助阶段超时后跳过 + 刷新恢复”止血，但没有解决真正导致卡住的底层原因

## 2. 问题定义

- 需要先撤回临时止血代码，再定位会让 `AI Writing` 多阶段链路持续卡住的共性根因
- 修复目标不是只让 `plan` 阶段暂时不报错，而是避免 `plan / write / check / revise / repair / tool calling` 再以同类超时解析错误复发

## 3. 根因假设

- `AIProviderServiceImpl` 当前把 provider 级超时与全局 `ai.request.timeout_seconds` 用 `Math.max(...)` 合并
- 现有种子数据与前端默认值里，provider 超时为 `60s`，全局超时为 `3600s`
- 这会直接让 provider 级超时失效，导致所有共用 `generateText / streamText / tool calling` 的请求都可能被放大到小时级等待
- 即使底层请求已正确失败，当前流式收尾仍可能把业务错误覆盖成浏览器侧 `network error`
- 覆盖点同时存在于：
  - 后端 SSE error 事件发送后立即收尾的脆弱窗口
  - 前端 `reader.read()` 抛错时优先抛原始网络异常，而不是先消费缓冲区里的尾部 SSE 错误事件

## 4. 需求范围

- 回退上一轮为止血加入的：
  - 辅助阶段超时后跳过
  - SSE 断线后继续跑完
  - 前端刷新恢复与本地持久化
- 在 provider 层修正统一请求超时解析
- 修正流式错误事件的后端收尾与前端消费逻辑，避免业务错误被覆盖成 `network error`
- 增加单测覆盖新的超时优先级
- 更新交接文档，明确根因、修复边界与剩余验证点

## 5. 非目标

- 不做章节工作区流式恢复架构升级
- 不在本次需求里重做整个 SSE 生命周期协议
- 不引入新的阶段性降级或跳过逻辑

## 6. 验收标准

- provider 级超时不再被全局超时放大覆盖
- 同类修复覆盖 `generateText / streamText / tool calling` 共用请求路径
- 上一轮临时止血逻辑已撤回
- 后端编译通过，前端类型检查通过，新增单测可运行
