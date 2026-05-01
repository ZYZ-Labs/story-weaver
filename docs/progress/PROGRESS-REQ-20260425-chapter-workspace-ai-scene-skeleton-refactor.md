# 章节工作区多 Scene 真 AI 骨架重构进度

- Req ID: `REQ-20260425-chapter-workspace-ai-scene-skeleton-refactor`
- Updated At: 2026-04-26 Asia/Shanghai
- Status: In Progress

## 当前状态

- 根因已确认，不再把“scene 文案重复”误判为单次生成问题。
- 后端已切到显式 AI 骨架生成 + 严格顺序守卫，章节工作区前端已改成“先骨架、后顺序接纳推进”的主链。
- 当前实现里，只有“接受当前镜头草稿”才会把 scene 标记为 `COMPLETED` 并解锁下一镜头；普通 `execute` 不再能绕过。
- 新增根因已确认：`scene-2+` 出现“章节生成连接中断”并不是 scene 顺序逻辑本身坏了，而是 SSE async dispatch 上的认证丢失导致流被 Security 掐断。
- 新增根因已确认：章节工作区 scene continuity 虽然已经接到了本地状态服务，但只以“上一镜头摘要 / 交接 / 承接说明”弱提示形式进入 prompt，没有成为结构化硬约束，所以 scene 仍会出现人名漂移、时间线冲突和越过停点。
- 后端现在已把 accepted scene 的真实正文回写成结构化 continuity state，并在 scene draft 生成前后同时使用；前端也已改成显式展示和下发 continuity facts。
- 新增根因已确认：章节工作区缺少 accepted scene 回退主链，用户无法把已接纳镜头、正文、runtime/handoff 和状态链一起撤回；如果只删 scene 或手改正文，会把顺序状态机打散。
- accepted scene 回退主链已落地：accept 时补落 accept 快照，章节工作区支持逐级撤回最新已接纳镜头和一键撤回全部，并同步回退 reader reveal / chapter state。
- 新增根因已确认：accepted scene 撤回虽然已有快照，但服务端仍把 scene runtime 是否还在 store 里误当成硬前置条件；runtime 过期时，本可回退的章节会被误报成“scene 运行态缺失”。
- 新增根因已确认：章节骨架生成仍走同步接口，章节工作区前端又共用了 `http.ts` 的 15 秒 axios timeout，所以长耗时规划会先被浏览器侧超时掐断，而不是以流式日志继续等待模型完成。
- 新增根因已确认：`scene-2+` 的硬性完整性校验里，当前镜头的 continuity 名称约束此前混入了 `nextSceneGoal` 提取出的下一镜头人物名；与此同时，修订链对“称呼漂移 / 提前抢写下一镜头”缺少专门的定向修订，所以同类失败会在重试时反复出现。
- 新增根因已确认：scene 数量继续增加时，continuity 提取器此前仍在用过宽的汉字段扫描兜底抽名字，容易把“大厅边缘”“第二天中午”这类非人物片段混入 `expectedNames / counterpartNames / timeAnchors`；一旦旧状态被带进后续镜头，scene 越多，误判“称呼漂移”的概率就越高。
- 新增根因已确认：即便把误提取范围缩小，如果 continuity 仍主要依赖代码内硬编码词表来识别“时间推进 / 会话对象 / 下一镜头越界”，它依旧会对当前项目语料过拟合；换项目或换叙事风格后，系统又会回到“词表不认识就误判/漏判”的脆弱状态。
- 新增结论已确认：即便把 continuity 提取器继续修细，当前章节工作区主链仍然是“正文先行、状态后补”；这说明问题已从缺陷修补转为架构升级，后续 node/checkpoint/runtime 改造已拆到独立需求 `REQ-20260427-state-driven-narrative-runtime-upgrade`。
- 前端 type-check、后端 compile、`JwtAuthenticationFilterTest`、`SceneContinuitySupportTest` 与新增 `ChapterWorkspaceAcceptedSceneRollbackServiceTest` 已通过；新的 rollback 单测已改成无 Mockito 版本，避免再被当前 JDK 的 inline attach 限制卡住。

## 最近关键结论

- 证据 1：
  - `RuleBasedChapterSkeletonPlanner` 之前会在没有已保存骨架时，直接从本地候选推导 `scene-1 / scene-2 / scene-3`。
  - `RuleBasedDirectorSessionService` 之前也会围绕这些本地候选继续出 candidate。
- 结论 1：
  - 多 scene 不是“AI 先规划、AI 再逐 scene 写”，而是“本地规则先伪造 scene，再拿 scene 名义去写”，这是本轮首要根因。
- 证据 2：
  - 章节工作区 AI 草稿流状态之前只按 `chapterId` 存储。
  - 同一章节切换 scene 后，草稿内容、日志、模型信息没有天然隔离。
- 结论 2：
  - 即使骨架改正确，如果前端状态仍按章节混存，用户仍会看到 scene 之间互相覆盖，不能算真正修好。
- 证据 3：
  - 章节工作区“接受草稿”之前会再调用一次 `executeStorySession(sceneId)`，由 `RuleBasedWriterSessionService` 生成模板化假摘要写入 runtime/handoff。
  - `DefaultSceneExecutionStateQueryService` 还会把每条 AI 写作记录按顺序推成 `scene-1 / scene-2 / scene-3`。
- 结论 3：
  - 之前的“承上”并不是基于真实已接纳正文，而是基于假执行摘要和记录顺序幻觉；要根治，必须把接纳写回改成真实正文驱动，并切断 `scene-draft record -> fake scene state` 这条旁路。
- 证据 4：
  - 真实容器日志在流式写作链路上出现 `Unable to handle the Spring Security Exception because the response is already committed`。
  - 根异常是 `AccessDeniedException: Access Denied`，触发栈落在 Tomcat `AsyncContextImpl` 的 async dispatch 上，而不是模型 provider 调用层。
- 结论 4：
  - `scene-2+` 更容易触发“连接中断”，是因为流更长，更容易进入/暴露 async dispatch；根因是 `JwtAuthenticationFilter` 默认没有覆盖 `ASYNC / ERROR` dispatch，导致已认证 SSE 请求在异步派发阶段丢失认证，被 Security 二次拦断。
- 证据 5：
  - `StorySessionContextPacket`、`SceneHandoffSnapshot.stateDelta` 和 `SceneExecutionState.stateDelta` 里其实已经能拿到 scene runtime/handoff 信息。
  - 但 `DefaultWriterExecutionBriefBuilder` 之前只把这些状态压成 `continuityNotes`、`previousSceneSummary` 和 `handoffLine` 三种自然语言提示。
  - 用户提供的 `scene-1 / scene-2` 样本里，出现了“明天十点开服”与“现在已开服”并存、`老陈` 漂成 `老猫`、以及 `scene-1` 提前写进 `scene-2` 的登录/大厅动作。
- 结论 5：
  - 不是 MCP/State Server 完全没接，而是“读到了状态，但没有把状态变成结构化硬约束和生成后硬校验”；因此模型一旦偏移，单次修订仍可能不够。
- 证据 6：
  - 章节工作区 accept 之前只保存了生成时的 `originalContent`，真正接纳时追加到 chapter 的却是“当下数据库里的最新正文”。
  - 如果用户在“生成草稿”和“接纳草稿”之间手改过正文，仅靠 `originalContent` 回退会把正文回到错误版本。
- 结论 6：
  - accepted scene 回退不能依赖生成时快照，必须在 accept 时补落 `contentBeforeAccept / contentAfterAccept` 真快照；撤回最新镜头时还必须只删该镜头的 outgoing handoff，不能把上一镜头通向当前镜头的 continuity 一起删掉。
- 证据 7：
  - `ChapterWorkspaceAcceptedSceneRollbackService` 之前在收集 accepted scene 前缀时，逐条强依赖 `sceneRuntimeStateStore.getSceneState(...)`，缺一个就直接抛“镜头 scene-x 的运行态缺失，无法安全撤回”。
  - 但真正用于正文回退的是 accept 时落下的 `contentBeforeAccept / contentAfterAccept`，不是 runtime store。
- 结论 7：
  - rollback 的第一真源应是 accepted record + accept 快照；runtime/handoff 缺失最多只影响“恢复得更细”而不该阻止正文和前缀状态回退。只有正文漂移、已接纳记录断裂、骨架前缀不连续这类情况才应拒绝。
- 证据 8：
  - `front/src/api/http.ts` 把全局 axios timeout 固定成 `15000ms`。
  - `generateChapterSkeleton(...)` 之前走的是同步 `http.post('/skeleton-generate')`，章节工作区没有任何骨架生成流式消费或日志面板。
- 结论 8：
  - 骨架生成的超时不是模型规划链本身唯一失败，而是前端把长耗时规划错接到了短超时同步请求链；要根治，必须把骨架生成改成独立 SSE + 日志，而不是单纯调大 timeout。
- 证据 9：
  - `SceneContinuitySupport.buildAcceptedContinuityState(...)` 和 `withForwardTargets(...)` 之前会把 `nextSceneGoal` 里的名字继续并入 `expectedNames / counterpartNames`。
  - `AIWritingServiceImpl` 在初稿和常规修订之后，只会自动修结尾；如果剩余硬问题是“称呼漂移 / 提前抢写下一镜头”，会直接失败，不会再做针对性修订。
- 结论 9：
  - 对 `scene-2` 这类承上启下镜头，下一镜头的人名和动作边界本该只用于“启下提示”，不该反向污染当前镜头的称呼一致性判断；同时硬性校验失败前还需要一轮面向 continuity 的定向修订，否则同一错误会稳定复现。
- 证据 10：
  - `SceneContinuitySupport.extractNames(...)` 之前会在上下文模式之外继续用宽泛的 `[\p{IsHan}]{2,4}` 兜底抽取中文片段。
  - 在 `scene-3` 这类“大厅边缘看见陆川”“第二天中午登录新纪元”的句式里，这种兜底会把地点/时间短语与旧人物名一起写进 continuity state，随后在后续 scene 被硬检查当成必须沿用的人物称呼。
- 结论 10：
  - `scene` 越多误杀概率越高，不是模型随机性线性累积，而是 continuity state 会越积越脏；根治必须改成“最近已接纳镜头优先 + 基于上下文的名字提取”，不能继续让旧 scene 的泛化短语污染后续硬检查。
- 证据 11：
  - `SceneContinuitySupport` 之前除了名字提取，还内置了大量与当前小说语料高度耦合的关键字和状态词，例如围绕开服、消息、会面、游戏大厅等场景词来判断时间推进、会话对象和下一镜头越界。
  - 这类规则即便对当前《旧日王座》样本有效，也无法保证换题材、换叙事动作、换称呼体系后继续稳定。
- 结论 11：
  - continuity 的核心判断不能再主要依赖代码硬编码语料词表；必须把“上一镜头真实连续性状态”在 accepted 时抽成结构化 JSON 持久化，再让后续生成和 hard check 优先消费这份 state。代码规则只保留通用兜底能力，不能继续充当主判断器。

## 已完成动作

- 后端：
  - 新增 `ChapterSkeletonGenerationService` 与 `AIChapterSkeletonGenerationService`
  - `RuleBasedChapterSkeletonPlanner` 改为仅返回已保存骨架并合并 runtime scene 状态
  - `RuleBasedDirectorSessionService` 改为只从已保存骨架读取当前 scene，并补下一镜头目标禁止项
  - `StorySessionOrchestrationController` 新增 `skeleton-generate`
  - 新增章节镜头顺序守卫，`preview / execute / scene-draft / accept` 都受“第一个未接纳镜头”约束
  - `DefaultSceneExecutionWriteService` 区分“运行态草稿写回”和“已接纳正文写回”，只有后者会落 `COMPLETED + 下一镜头 handoff`
  - `DefaultSceneExecutionWriteService` 现在在 accepted scene 写回时额外落 `continuity` 结构，包含真实摘要、真实交接、关键承接事实、时间锚点和已确认称呼
  - `DefaultSceneExecutionStateQueryService` 忽略章节工作区 `scene-draft` 记录，避免再把草稿尝试误识别成新镜头
  - `SceneContinuityState` / `SceneContinuitySupport` 落地，`DefaultWriterExecutionBriefBuilder` 现在会把 runtime/handoff continuity 重新组装进 `writerExecutionBrief`
  - `AIWritingServiceImpl` 对章节工作区 scene draft 新增后端“镜头顺序硬约束”段落，并在生成后执行 continuity 硬校验，优先拦截时间冲突、称呼漂移和越过下一镜头目标
  - `JwtAuthenticationFilter` 改为覆盖 `ASYNC / ERROR` dispatch，避免 SSE 在异步派发阶段丢失认证
  - `AIWritingController` / `AIWritingServiceImpl` 新增 chapter 级 accepted scene rollback 接口；accept 现在会把 `contentBeforeAccept / contentAfterAccept` 真实写回到 `generationTrace.acceptance`
  - 新增 `ChapterWorkspaceAcceptedSceneRollbackService`，以 accepted scene 前缀为主链回退正文、scene runtime、outgoing handoff、reader reveal 和 chapter state
  - `ChapterWorkspaceAcceptedSceneRollbackService` 现在允许前缀已接纳 scene 的早期 runtime 缺失；reader reveal 回退优先用 runtime delta，缺失时回退到骨架 `readerReveal`，并继续以 accept 快照为正文真源
  - `SceneRuntimeStateStore` / `ResilientSceneRuntimeStateStore` 新增 `deleteHandoffsFromScene`，避免撤回最新镜头时把上一镜头通向当前镜头的 handoff 误删
  - `ChapterSkeletonGenerationService` / `AIChapterSkeletonGenerationService` 新增骨架生成流式链；`StorySessionOrchestrationController` 暴露 `skeleton-generate-stream` SSE 入口，并在模型规划期间持续发送阶段日志与等待心跳
  - `SceneContinuitySupport` 不再把 `nextSceneGoal` 提取出的名字并入当前镜头 continuity 的 `expectedNames / counterpartNames`
  - `SceneContinuitySupport` 现在改为“最近已接纳镜头优先”继承 `counterpartNames / timeAnchors / carryForwardFacts`，并把名字提取从宽泛汉字段扫描收紧为基于会面、消息、登录、开口等上下文模式的抽取，避免地点/时间短语继续污染后续 scene 的 continuity state
  - 新增 `AIContinuityStateService`：accepted scene 写回时，先让模型把当前镜头抽成结构化 continuity JSON（`carryForwardFacts / timeAnchors / expectedNames / counterpartNames / requiresExplicitTimeTransition`）并持久化到 runtime/handoff；scene draft hard check 也优先用模型对这份结构化 state 做窄任务审查
  - `SceneContinuitySupport` 现已降级为通用兜底层：只保留与项目无关的名字/短语提取、状态合并和最小越界检查，不再内置围绕《旧日王座》语料的场景词表
  - `AIWritingServiceImpl` 新增“硬性完整性定向修订”收口：常规修订后若仍残留称呼漂移、时间冲突或提前抢写下一镜头，会再按明确问题清单执行一轮 continuity 专项修订，再决定是否最终失败
- 前端：
  - `ChapterWorkspaceView.vue` 新增显式“生成 / 重新生成镜头骨架”入口
  - 未生成骨架时，当前镜头执行卡改为明确空状态
  - scene 草稿流状态改成按 scene scope 隔离
  - 后续镜头改成可见但未解锁时不可选，当前只允许为第一个未接纳镜头生成 / 接纳草稿
  - 当前镜头执行卡与 scene draft prompt 现在都会显式展示/使用上一镜头真实摘要、真实交接、必须继承事实、时间锚点和下一镜头入口预留
  - 章节工作区当前镜头执行卡新增“撤回上一个已接纳镜头 / 一次性撤回全部”入口，并在回退后自动刷新章节正文、scene 锁定和状态面板
  - `writing` store 与其他写作记录状态展示已补 `rolled_back`，避免旧页面把已撤回记录误判成仍可处理的草稿
  - 章节工作区现在改用 `skeleton-generate-stream` 生成骨架，并把骨架规划过程接入日志面板；前端不再复用全局 15 秒 axios 同步超时链来等待骨架结果
- 验证：
  - `mvn -pl backend -am -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -DskipTests compile`
  - `mvn -pl backend -am -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -Dtest=JwtAuthenticationFilterTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - `mvn -pl backend -am -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -Dtest=SceneContinuitySupportTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - `mvn -pl backend -am -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -Dtest=ChapterWorkspaceAcceptedSceneRollbackServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - `npm run type-check`

## 下一步动作

- 等用户在真实页面回归：
  - 未生成骨架时，章节工作区是否不再假装已有多个 scene
  - 生成骨架后，后续镜头是否确实保持未解锁不可选，且必须接纳前一镜头才能继续
  - `scene-2+` 的上一镜头摘要 / handoff / 必须继承事实是否来自真实已接纳正文，而不是模板化假摘要
  - `scene-2+` 是否还能复现“老陈漂成老猫”“明天开服却直接写成已开服”“scene-1 提前把 scene-2 写掉”这三类问题
  - 切换已完成镜头与当前镜头时，草稿与日志是否仍会互相覆盖
  - `scene-2+` 流式生成是否不再出现“章节生成连接中断”，而是正常完成或返回明确业务错误
  - 撤回最新已接纳镜头后，正文、当前解锁镜头、reader reveal 和 chapter state 是否一起回到上一镜头
  - 一次性撤回全部已接纳镜头后，章节是否回到镜头写作开始前版本，且 `scene-1` 重新成为当前可处理镜头
  - 前缀 scene runtime 缺失时，一键撤回是否不再误报“scene 运行态缺失”，而是正常回退或在正文真实漂移时给出正确冲突提示
  - 重新生成镜头骨架时，页面是否开始显示骨架生成日志，并在长耗时规划期间不再先报 `timeout of 15000ms exceeded`
  - `scene-2+` 遇到“称呼漂移 / 抢写 scene-3”时，是否能通过定向修订自动收回，而不是连续多次稳定打回
  - `scene-3 / scene-4` 这类更长链路里，hard check 是否还会因为旧人物名、地点短语或时间短语污染 continuity state 而稳定误杀
  - 更换到非《旧日王座》题材或命名体系的项目后，continuity 抽取和 hard check 是否仍能工作，而不会因为代码里认识/不认识某批词而明显漂移
  - scene mode 兼容基线保持可用的同时，后续 node/checkpoint/runtime 主链改造是否能逐步接管章节工作区，而不再由 scene 正文承担事实真相层
- 记录用户环境回归结论

## 阻塞项

- 当前无新增代码阻塞
- 仍需用户实际页面回归
