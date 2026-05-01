# AI Writing 卡住问题根因修复进度

- Req ID: `REQ-20260425-ai-writing-timeout-root-cause-fix`
- Updated At: 2026-04-25 Asia/Shanghai
- Status: In Verification

## 当前状态

- 已回退上一轮“超时跳过 / 刷新恢复 / 断线继续执行”止血代码
- 已确认根因位于 `AIProviderServiceImpl` 的统一请求超时解析
- 已在 provider 层修复超时优先级，并补了独立单测

## 最近关键结论

- 证据 1：
  - `sql/002_seed_data.sql` 的全局 `ai.request.timeout_seconds = 3600`
  - provider 默认与前端表单默认 `timeoutSeconds = 60`
- 证据 2：
  - `AIProviderServiceImpl` 之前用 `Math.max(resolveTimeoutSeconds(provider), resolveConfiguredAiTimeoutSeconds())`
  - 这会把 provider 的 `60s` 直接放大为全局 `3600s`
- 结论：
  - `plan / write / check / revise / repair / tool calling` 这类共用 provider 请求路径都存在同类风险
  - 之前只在 `plan/check/revise/repair` 层做跳过，是典型止血，不是根因修复
- 证据 3：
  - 模型调用失败时，后端会进入 SSE `error` 分支，但前端 `streamGenerateWriting` / `streamWritingChatMessage` 在 `reader.read()` 抛错时会优先抛原始网络异常
  - 这会把本该呈现为业务错误的模型超时、连通性失败，放大成浏览器侧 `network error`
- 新结论：
  - 当前问题不是单一 provider 超时配置错误，还包含一层“流式错误被覆盖”的传输面问题
  - 必须同时修正 provider 请求超时解析和 SSE 错误传递，才能让章节工作区稳定落成明确错误而不是假性的 network error

## 已完成动作

- 回退：
  - `backend/src/main/java/com/storyweaver/controller/AIWritingController.java`
  - `backend/src/main/java/com/storyweaver/service/impl/AIWritingServiceImpl.java`
  - `backend/src/main/java/com/storyweaver/service/impl/SystemConfigServiceImpl.java`
  - `front/src/stores/writing.ts`
  - 以及对应测试与展示层止血改动
- 修复：
  - `backend/src/main/java/com/storyweaver/service/impl/AIProviderServiceImpl.java`
  - provider 请求统一改为按更严格的超时侧生效，不再取更大值放大等待时间
  - provider 传输失败改为区分超时、连通性失败和普通 I/O 错误，避免只返回笼统的“无法读取返回结果”
  - `backend/src/main/java/com/storyweaver/controller/AIWritingController.java`
  - `backend/src/main/java/com/storyweaver/controller/AIWritingChatController.java`
  - controller 在异常分支改为稳态发送 `error` 事件并安静收尾，降低业务错误被连接关闭覆盖的概率
  - `front/src/api/ai-writing.ts`
  - `front/src/api/ai-writing-chat.ts`
  - 前端在流读取异常时先尝试消费缓冲区尾部 SSE，再把原生 network error 归一成明确提示
- 测试：
  - 新增 `backend/src/test/java/com/storyweaver/service/impl/AIProviderServiceImplTest.java`
  - `mvn -pl backend -am -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -DskipTests compile`
  - `mvn -pl backend -am -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -Dtest=AIProviderServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - `npm run type-check`

## 下一步动作

- 用固定样本项目 `旧日王座 / projectId=28` 回归章节工作区当前镜头生成
- 观察是否还会出现小时级心跳卡住，必要时再补充线上回归记录

## 阻塞项

- 当前无代码阻塞
- 如需进一步证明线上行为，还需要用户环境做章节工作区回归
