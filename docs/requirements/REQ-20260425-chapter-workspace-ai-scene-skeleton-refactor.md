# REQ-20260425 章节工作区多 Scene 真 AI 骨架重构

- Req ID: `REQ-20260425-chapter-workspace-ai-scene-skeleton-refactor`
- Created At: 2026-04-25 Asia/Shanghai
- Status: In Progress

## 背景

- 当前章节工作区的多 scene 并不是先由 AI 真实拆解章节，再逐镜头生成正文。
- 原链路里，章节骨架与 director candidate 仍会在本地用 rule-based 规则补出 `scene-1 / scene-2 / scene-3`，导致不同镜头容易写成同一段剧情的平行改写。
- 前端流式草稿状态又按 `chapterId` 单键存储，不同 scene 的日志与草稿会互相覆盖，进一步放大“多 scene 只是表面存在”的问题。
- 章节工作区切到严格顺序 scene 之后，`scene-2+` 的流式生成更容易暴露 SSE 异步派发链路问题；如果认证没有在 async/error dispatch 上续上，前端会被误报成“章节生成连接中断”。
- 即使顺序锁和真实接纳链已经落地，当前 continuity state 仍只是在 writer brief 里以“上一镜头摘要 / 交接 / 承接说明”几句自然语言透传，没有把时间锚点、联系人称呼、停点边界这些硬事实结构化落盘并重新注入下一镜头。
- 当前章节工作区一旦接纳镜头正文，就没有对应的撤回主链；用户只能手改正文或删骨架，无法把已接纳镜头、runtime/handoff 和章节状态一起回退。
- accepted scene 回退虽然已落地，但当前实现仍把 runtime store 里的 scene state 当成撤回前置条件；一旦早期 scene runtime 因 TTL 或清理缺失，就会把“可根据 accept 快照恢复”的情况误判成不可撤回。
- 章节骨架生成仍走同步 `POST /skeleton-generate`，前端又复用了全局 15 秒 axios timeout；只要模型规划稍慢，页面就会先报 `timeout of 15000ms exceeded`，用户既看不到进度，也无法区分是前端超时还是后端真正失败。

## 目标

- 把章节骨架改成显式 AI 生成，不再默认用本地规则自动补 scene。
- 让 preview / execute / scene draft 都依赖已保存的章节骨架，而不是临时伪造 candidate。
- 让章节工作区前端先生成骨架，再按 scene 生成正文，并把草稿与日志隔离到 scene 维度。
- 把章节工作区多 scene 改成严格顺序状态机：一次性看到整章镜头，但只能处理第一个未接纳镜头。
- 让“接受当前镜头草稿”成为唯一能解锁下一镜头的动作，不能再靠中间 scene 直跳或手动推进绕过。
- 让已接纳镜头支持安全撤回：既能逐级撤回最新已接纳镜头，也能一次性撤回本章全部已接纳镜头，并同步恢复正文、scene runtime/handoff、读者已知与章节状态。
- 让 `scene-2+` 的承上输入来自上一镜头已接纳的真实正文摘要与 handoff，而不是本地假摘要。
- 让上一镜头已接纳正文回写结构化 continuity state，至少沉淀真实摘要、真实交接、关键承接事实、时间锚点和已确认称呼。
- 让当前镜头 prompt 显式带“下一镜头入口预留”，只允许把局面送到入口前，不允许提前抢写下一镜头正文。
- 让章节工作区 scene draft 在生成后执行连续性硬校验，至少拦截人名/称呼漂移、时间线冲突和越过下一镜头停点这类可明确判断的问题。
- 让章节工作区和写作聊天这类 SSE 接口在 async/error dispatch 上继续携带认证，不再把异步派发鉴权丢失误报为连接中断。
- 让章节骨架生成也进入流式日志链路，避免前端同步超时把长耗时规划误报成失败。
- 让 accepted scene 撤回以 accept 快照为第一真源，scene runtime/handoff 只作为可选辅助状态，而不是撤回前置条件。

## 非目标

- 不重做整套 Story Orchestration 架构。
- 不改旧写作中心的主交互。
- 不在本轮把“刷新后继续同一条流式生成”作为主目标。

## 验收标准

- `skeleton-preview / preview / execute` 不再从本地 rule-based 逻辑自动补出新 scene；没有骨架时应明确返回空或提示先生成。
- `POST /api/story-orchestration/projects/{projectId}/chapters/{chapterId}/skeleton-generate` 能生成并保存 AI 骨架，生成结果中的新 scene 来源为 `ai-skeleton`。
- `POST /api/story-orchestration/projects/{projectId}/chapters/{chapterId}/skeleton-generate-stream` 必须返回可持续消费的阶段日志与最终骨架结果；章节工作区前端不再因为固定 15 秒同步超时而先报失败。
- 章节工作区在未生成骨架时不能继续按 scene 生成正文，并给出显式入口按钮。
- 切换 scene 后，当前 scene 的草稿、日志、模型信息不再与同章节其他 scene 混用。
- 后续 scene 必须可见但不可越级选择；未接纳 `scene-1` 时，`scene-2+` 不允许直接 preview / generate / accept。
- 当前镜头只有在“接受草稿并写回正文”后才会标记 `COMPLETED` 并解锁下一镜头；普通 `execute` 不得替代接纳。
- 章节工作区必须提供“撤回最新已接纳镜头”和“撤回全部已接纳镜头”两个入口；撤回后，章节正文必须恢复到对应 accept 时快照，而不是简单删掉当前 scene 文本。
- 撤回最新已接纳镜头时，上一镜头通向当前镜头的 handoff 必须保留；只能删除被撤回镜头自己的 runtime state 和 outgoing handoff，不能把 continuity 一起删断。
- 对仍保留 accept 快照、但早期 scene runtime 已缺失的章节，撤回仍应成功；系统只能在正文快照漂移、骨架前缀断裂或已接纳记录缺失这类真正无法保证一致性的情况下拒绝撤回。
- 撤回后，reader reveal / chapter state / unlocked scene 必须与剩余 accepted scene 前缀保持一致，不能出现“正文回退了，但状态仍停在后面”的脏数据。
- 接纳 `scene-n` 后写入 runtime/handoff 的摘要与 handoff line 必须来自 `scene-n` 的真实已接纳正文。
- 接纳 `scene-n` 后，runtime/handoff 里必须附带结构化 continuity state，而不只是字符串摘要。
- `scene-n+1` 生成时必须显式拿到上一镜头摘要、上一镜头 handoff、关键承接事实、时间锚点和下一镜头入口预留。
- 当 `scene-n+1` 正文把上一镜头确认的人名/称呼改写成其他人、把“明天开服”写成“现在已开服”却没有时间跳转、或提前写进下一镜头目标时，系统必须先拦截并触发修订/失败，不能直接落成可接纳草稿。
- 已通过认证的流式写作请求在 async/error dispatch 上不得再因为认证丢失而被 Security 直接掐断；出现异常时必须继续落为明确业务错误或正常 `complete`，不能只剩“连接中断”。
- 目标测试、后端 compile 与前端构建通过。
