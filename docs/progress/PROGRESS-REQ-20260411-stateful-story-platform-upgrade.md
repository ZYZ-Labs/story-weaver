# Story 平台级架构升级 进度记录

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Status: In Progress
- Created At: 2026-04-11 Asia/Shanghai
- Updated At: 2026-04-11 Asia/Shanghai

## 当前快照

- Current Phase: Phase 1B 进行中，当前完成的是 `1B.1`，仍需继续完成 `1B.2 / 1B.3`
- Current Task: 继续完成 `Phase 1B` 的文档口径收敛与下一批粗模块边界冻结，不进入新阶段
- Last Completed: 已完成 `1B.1`，即 `story-domain` 与 `story-storyunit` 的首轮物理迁移、根工程聚合与构建链路切换
- Next Action: 在 `Phase 1B` 内继续完成：
  - `1B.2` 活动文档与构建入口统一
  - `1B.3` 下一批粗模块边界冻结
- Blockers:
  - 旧主线 `REQ-20260409-generation-reliability-refactor` 已归档，但其代码成果和回归报告仍需作为迁移基线继续参考
  - `MCP` 与 `LSP` 的边界尚未形成代码级实现，只完成讨论与文档收敛
  - 前端现有页面结构仍是旧工作流，尚未切到新信息架构
- Latest Verified:
  - 已确认用户界面默认只展示摘要是新的硬原则
  - 已确认结构化字段主要服务于 MCP/LSP、编排层和状态机
  - 已确认不采用“万能基类”，而采用 `StoryUnit + Facets` 协议壳
  - 已确认后续采用四 session：
    - 总导
    - 选择器
    - 写手
    - 审校器
  - 已确认章节执行升级为“章节骨架 + 镜头执行 + 状态交接”
  - 已确认后续开发前必须先拆详细计划文档
  - 已新增首份详细实施计划：
    - `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-phase1-foundation-v1.md`
  - 已确认模块拆分节奏调整为：
    - `Phase 1A` 先细分包边界
    - `Phase 1B` 尽早进入粗粒度模块拆分
  - `story-storyunit/src/main/java/com/storyweaver/storyunit/` 当前已承载首批包：
    - `model`
    - `facet`
    - `patch`
    - `snapshot`
    - `event`
    - `adapter`
    - `service`
    - `session`
  - 已落地首批 adapter / query service / session 协议
  - 已明确第一轮粗模块映射口径：
    - `storyunit.* -> story-storyunit`
    - `story.generation.* -> story-generation`
    - `ai.* -> story-provider`
    - `controller.* -> story-web`
    - `repository.* / 配置与持久化侧 -> story-infra`
  - 已完成首轮粗粒度模块拆分：
    - 根 `pom.xml`
    - `story-domain`
    - `story-storyunit`
    - 保留 `backend` 作为应用壳
  - 已明确 `Phase 1B` 目前只完成 `1B.1`
  - 已明确 `1B.2 / 1B.3` 仍归属当前 `Phase 1B`
  - 已完成物理迁移：
    - `domain/entity -> story-domain`
    - `item/domain -> story-domain`
    - `storyunit -> story-storyunit`
  - 已完成构建链路调整：
    - `backend/Dockerfile` 改为根工程上下文构建
    - `scripts/deploy.sh` 改为根工程上下文构建
    - `scripts/deploy.ps1` 改为根工程上下文构建
  - 已完成验证：
    - `mvn -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -DskipTests compile`
    - `git diff --check`
- Latest Unverified:
  - `StoryUnit` 的存储落地方式
  - `MCP/LSP` 的实际运行形态
  - 模块化单体的首轮拆分方案是否会影响现有主链
  - 前端信息架构在真实页面中的可用性
  - `storyunit` 与现有 domain/repository 的迁移耦合面还没有正式开始切
  - `story-generation / story-provider / story-web / story-infra` 的下一轮拆分顺序尚未冻结
  - 活动文档中的旧构建入口与旧目录口径尚未清理完毕

## 关键节点记录

### [2026-04-11 Asia/Shanghai] 确认平台升级方向并启动文档建制

- 背景:
  - 用户明确指出，后续系统会承载世界观增量、背包、技能、态度、好感、世界影响等复杂状态，不再满足于 MVP 级单链路写作系统。
- 本次完成:
  - 确认本轮升级应定性为平台级架构升级
  - 确认采用 `Summary First`
  - 确认采用 `StoryUnit + Facets`
  - 确认采用模块化单体
  - 确认采用 `MCP/LSP + 四 session 编排 + 镜头级写作`
  - 启动新版 requirement / plan / progress 文档编写
- 修改文件:
  - `docs/requirements/REQ-20260411-stateful-story-platform-upgrade.md`
  - `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-v1.md`
  - `docs/progress/PROGRESS-REQ-20260411-stateful-story-platform-upgrade.md`
- 风险/遗留:
  - 旧主线已归档，但新旧主线衔接方式还需要在后续文档中明确
- 下一步:
  - 开始按 Phase 1A 计划实施协议骨架与细分包边界

### [2026-04-11 Asia/Shanghai] 启动 Phase 1A 首批代码骨架

- 背景:
  - 用户确认采用“模块先粗拆、包先细分”的节奏，希望尽早为后续模块化拆分打底。
- 本次完成:
  - 在 `backend` 内新增 `com.storyweaver.storyunit.*` 首批包边界
  - 落地 `StoryUnit / Facets / Patch / Snapshot / Event` 协议骨架
  - 落地旧实体到新协议层的 adapter 接口与最小默认实现
  - 落地只读 query service 接口
  - 落地多 session 最小协议：
    - `DirectorCandidate`
    - `SelectionDecision`
    - `WriterExecutionBrief`
    - `ReviewDecision`
    - `SceneExecutionState`
  - 明确了 `Phase 1B` 可直接沿用的包到粗模块映射口径
  - 完成 `backend` 编译与格式校验
- 修改文件:
  - `backend/src/main/java/com/storyweaver/storyunit/model/*`
  - `backend/src/main/java/com/storyweaver/storyunit/facet/*`
  - `backend/src/main/java/com/storyweaver/storyunit/patch/*`
  - `backend/src/main/java/com/storyweaver/storyunit/snapshot/*`
  - `backend/src/main/java/com/storyweaver/storyunit/event/*`
  - `backend/src/main/java/com/storyweaver/storyunit/adapter/*`
  - `backend/src/main/java/com/storyweaver/storyunit/service/*`
  - `backend/src/main/java/com/storyweaver/storyunit/session/*`
  - `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-phase1-foundation-v1.md`
  - `docs/progress/PROGRESS-REQ-20260411-stateful-story-platform-upgrade.md`
  - `docs/agent-context.md`
- 风险/遗留:
  - 当前仍是协议层，不含 repository/service 实现
  - `MCP/LSP` 仍未进入可运行服务阶段
  - 粗粒度模块拆分尚未开始
- 下一步:
  - 收敛 Phase 1A 剩余命名与边界后，切到 `Phase 1B` 详细拆分计划

### [2026-04-11 Asia/Shanghai] 完成 Phase 1B 首轮粗粒度模块拆分

- 背景:
  - 用户要求尽早拆模块，但同时希望不要一上来拆得太细。
  - 在实际检查后确认，`domain/dto` 已经存在对 `story.generation` 的反向依赖，因此第一轮不能整包抽走整个 `domain`。
- 本次完成:
  - 新增根 `pom.xml`，将工程改为 Maven 聚合结构
  - 新增模块：
    - `story-domain`
    - `story-storyunit`
  - 保留 `backend` 为应用壳
  - 完成物理迁移：
    - `com.storyweaver.domain.entity.* -> story-domain`
    - `com.storyweaver.item.domain.* -> story-domain`
    - `com.storyweaver.storyunit.* -> story-storyunit`
  - 更新 `backend` 依赖，使其消费共享模块
  - 更新后端 Docker 构建与发布脚本，改为根工程上下文
  - 完成根工程 Reactor 编译验证
- 修改文件:
  - `pom.xml`
  - `story-domain/pom.xml`
  - `story-storyunit/pom.xml`
  - `backend/pom.xml`
  - `backend/Dockerfile`
  - `scripts/deploy.sh`
  - `scripts/deploy.ps1`
  - `story-domain/src/main/java/com/storyweaver/domain/entity/*`
  - `story-domain/src/main/java/com/storyweaver/item/domain/*`
  - `story-storyunit/src/main/java/com/storyweaver/storyunit/*`
  - `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-phase1-modularization-v1.md`
  - `docs/plans/PLAN-REQ-20260411-stateful-story-platform-upgrade-phase1-foundation-v1.md`
  - `docs/progress/PROGRESS-REQ-20260411-stateful-story-platform-upgrade.md`
  - `docs/agent-context.md`
- 风险/遗留:
  - 单独执行 `mvn -f backend/pom.xml ...` 不再是有效主入口
  - 活动文档与说明中仍有旧构建命令待清理
  - 下一轮更细模块的拆分顺序尚未冻结
- 下一步:
  - 继续留在 `Phase 1B`
  - 完成 `1B.2`：
    - 更新活动文档中的构建入口口径
    - 更新活动文档中的模块结构描述
  - 完成 `1B.3`：
    - 冻结 `story-generation / story-provider / story-web / story-infra` 的粗边界
    - 明确继续临时留在 `backend` 的包

## 贡献与署名说明

- 平台升级方向和问题提出：用户。
- 进度文档结构、术语收敛与记录整理：Codex。
- 当前结论由用户与 Codex 共同讨论形成。
