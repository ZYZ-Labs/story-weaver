# PLAN-REQ-20260425 章节工作区多 Scene 真 AI 骨架重构

- Req ID: `REQ-20260425-chapter-workspace-ai-scene-skeleton-refactor`
- Version: `v1`
- Updated At: 2026-04-26 Asia/Shanghai

## 实施步骤

1. 确认根因属于章节骨架与 director candidate 仍由本地 rule-based 逻辑伪造，而不是单纯某个 scene prompt 出错。
2. 把章节骨架规划改成“仅消费已保存骨架”，新增显式 AI 骨架生成入口，禁止 planner / director 再隐式补 scene。
3. 把章节工作区改成严格顺序状态机：
   - 后端校验后续 scene 不可越级 preview / generate / accept
   - 前端镜头列表一次性展示，但只允许选择已完成前缀和当前第一个未接纳镜头
   - “接受草稿”成为唯一能解锁下一镜头的动作
4. 把 runtime/handoff 的完成写回改成基于“真实已接纳正文”：
   - 普通 `execute` 只保留运行态草稿，不再标记 `COMPLETED`
   - `accept` 时根据真实正文生成 outcomeSummary / handoffLine，并写回下一镜头 handoff
   - 过滤章节工作区 `scene-draft` 记录，避免再被 legacy scene 查询误识别为新镜头
5. 把真实已接纳正文继续沉淀成结构化 continuity state：
   - runtime/handoff 除了字符串摘要，还要落真实承接事实、时间锚点、已确认称呼和下一镜头入口
   - preview/writer brief 必须把这份 continuity state 重新注入 scene draft 主链，不能只保留自然语言 note
   - AI Writing 生成后要做最小硬校验，先拦截称呼漂移、时间冲突和提前抢写下一镜头
6. 调整章节工作区前端：
   - 先生成 / 重新生成镜头骨架
   - 未生成骨架时禁止 scene 正文主链
   - 当前 scene 执行卡显示顺序锁、上一镜头摘要、关键 continuity facts 与下一镜头入口预留
7. 把 AI Writing 流式状态改成 scene 维度隔离，避免同章节不同 scene 的草稿、日志、模型信息互相覆盖。
8. 修复章节工作区 `scene-2+` 暴露出的 SSE 异步派发认证丢失问题：
   - JWT 鉴权过滤器必须覆盖 `ASYNC / ERROR` dispatch
   - 已认证的流式请求不能在异步派发阶段被 Spring Security 二次拦断
9. 补上 accepted scene 回退主链：
   - accept 时必须落 accept 快照，撤回不能再依赖生成时 `originalContent`
   - 后端提供“撤回最新已接纳镜头 / 撤回全部已接纳镜头”接口，并同步重建正文、runtime/handoff、reader reveal 与 chapter state
   - accept 快照是撤回真源；如果前缀已接纳 scene 的 runtime 因 TTL 或清理缺失，仍要能安全回退，不能把 runtime 缺失直接当成致命错误
   - 前端章节工作区提供两个撤回入口，且只允许按 accepted scene 前缀回退，不能中间跳撤
10. 把章节骨架生成切到独立 SSE + 日志链：
   - 后端提供 `skeleton-generate-stream`，在模型调用期间持续发送阶段日志和等待心跳
   - 前端章节工作区用流式接口消费骨架生成，不再复用全局 15 秒 axios timeout 的同步调用
11. 补齐 planner/controller/runtime/query/security/continuity/rollback 目标测试，更新主入口文档与页面验收要点。
12. 把“scene 文本先行”升级为“node/checkpoint 状态驱动”的架构改造拆到独立需求 `REQ-20260427-state-driven-narrative-runtime-upgrade`，当前计划仅继续承担 scene mode 的兼容基线，不再继续吞并主架构升级范围。
