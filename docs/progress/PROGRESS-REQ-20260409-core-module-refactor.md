# Story 核心模块重构 进度记录

- Req ID: REQ-20260409-core-module-refactor
- Status: In Progress
- Created At: 2026-04-09 Asia/Shanghai
- Updated At: 2026-04-09 Asia/Shanghai

## 当前快照

- Current Phase: Step 2 AI 读取链路第一批适配已落地，待进入联调与行为验证阶段
- Current Task: 继续核对 AI 写作上下文 / 总导工具对新结构的命中情况，并补端到端验证
- Last Completed: 已完成 AI 写作 prompt 与总导工具的第一批新结构适配，使 `outlineId / storyBeat / POV / roleType / 世界观关联` 开始进入 AI 读取链路
- Next Action: 结合真实章节数据验证 AI 写作与总导决策的上下文命中，并收口剩余兼容细节
- Blockers:
  - 暂无硬阻塞；主要风险在于旧 CSV / JSON 数据迁移质量
- Latest Verified:
  - 已核对 `Outline / Plot / Causality / Character / Chapter` 当前实体、接口和前端页面结构
  - 已核对 AI 总导层当前实现已经进入写作主链路
  - 已产出 `PLAN-REQ-20260409-core-module-refactor-mapping-v1`
  - 已新增 `sql/012_story_core_refactor_base.sql`
  - 已新增 `sql/013_story_core_refactor_backfill.sql`
  - 已新增 `sql/014_story_core_refactor_cleanup.sql`
  - 已新增大纲 / 章节关联关系表对应的后端实体与 Mapper
  - 已扩展 `Outline` / `Chapter` 与对应 DTO 的重构字段
  - 已改造 `OutlineServiceImpl` / `ChapterServiceImpl`，支持新结构读写并兼容旧字段
  - 已扩展 `Plot` / `Causality` / `Character` 与对应 DTO
  - 已改造 `PlotService` / `PlotCrudServiceImpl` / `CausalityServiceImpl` / `CharacterServiceImpl`
  - 已改造 `DatabaseMigrationInitializer`，启动时可自动补齐本轮新增字段、关系表和配置项
  - 已执行 `mvn -f backend/pom.xml -DskipTests compile`，后端编译通过
  - 已执行 `npm run build`，前端构建通过
  - 已完成 `OutlineView / ChapterListView / CharacterListView / PlotView / CausalityView` 的新字段接入
  - 已改造 `AIWritingServiceImpl / DirectorToolExecutor / AIDirectorApplicationServiceImpl` 的第一批新结构读取逻辑
  - 已再次执行 `mvn -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -f backend/pom.xml -DskipTests compile`，后端编译通过
- Latest Unverified:
  - 尚未实际执行 `012/013/014` 脚本
  - 尚未做浏览器侧手工回归验证，当前仅完成构建验证
  - AI 写作上下文与 AI 总导层虽已开始读取新结构，但尚未做真实章节场景的行为验证

## 关键节点记录

### [2026-04-09 Asia/Shanghai] 完成模块重构正式规划

- 背景:
  - 用户要求先阅读 agent 文档，确认前序需求完成度，再重点审阅 `docs/story_weaver_module_refactor_prd_v_1.md`，输出完整重构计划。
- 本次完成:
  - 阅读 `docs/agent-context.md`、`docs/agent-handoff-rules.md`
  - 阅读 AI 总导层 requirement / plan / progress 文档
  - 阅读 `docs/story_weaver_module_refactor_prd_v_1.md`
  - 抽查后端实体、服务、控制器和前端视图，确认当前故事模块结构问题
  - 新建正式 requirement / plan / progress 文档
- 修改文件:
  - `docs/requirements/REQ-20260409-core-module-refactor.md`
  - `docs/plans/PLAN-REQ-20260409-core-module-refactor-v1.md`
  - `docs/progress/PROGRESS-REQ-20260409-core-module-refactor.md`
  - `docs/agent-context.md`
- 验证:
  - 执行 `mvn -f backend/pom.xml -DskipTests compile` 成功
  - 执行 `npm run build` 成功
- 风险/遗留:
  - 当前重构仍处于规划阶段，尚未进入 SQL 和服务实现
  - `docs/story_weaver_module_refactor_prd_v_1.md` 仍是上游 PRD 来源，不等于正式实施计划
- 下一步:
  - 根据计划的 Step 0 输出字段映射表与第一批 SQL 迁移脚本

### [2026-04-09 Asia/Shanghai] 完成 Step 0 映射与 SQL 骨架

- 背景:
  - 规划文档完成后，需要把“如何迁移”从口头方案落成正式工件，避免后续边改边变更语义。
- 本次完成:
  - 新增字段映射文档，固定旧字段到新字段的转换规则和双读 / 双写优先级
  - 新增 `012_story_core_refactor_base.sql`，定义结构扩展、关系表和开关配置
  - 新增 `013_story_core_refactor_backfill.sql`，定义默认回填和安全迁移规则
  - 新增 `014_story_core_refactor_cleanup.sql`，固定后续清理边界
- 修改文件:
  - `docs/plans/PLAN-REQ-20260409-core-module-refactor-mapping-v1.md`
  - `sql/012_story_core_refactor_base.sql`
  - `sql/013_story_core_refactor_backfill.sql`
  - `sql/014_story_core_refactor_cleanup.sql`
  - `docs/progress/PROGRESS-REQ-20260409-core-module-refactor.md`
  - `docs/agent-context.md`
- 验证:
  - 已人工复核字段映射与现有实体字段的对应关系
  - 本次仅新增文档和 SQL 骨架，未执行数据库脚本
- 风险/遗留:
  - `013` 中 CSV 拆分和窗口函数回填依赖 MySQL 8+ 能力，正式执行前需结合目标环境确认
  - 因果实体 ID 的历史脏数据仍可能需要服务层二次清洗
- 下一步:
  - 从后端实体和服务层开始落实 `012` 的第一批结构字段

### [2026-04-09 Asia/Shanghai] 完成 Step 1 第一批后端结构改造

- 背景:
  - Step 0 已固定字段映射和 SQL 骨架，下一步需要先把最核心的大纲 / 章节结构落到代码里，形成可编译的后端基础。
- 本次完成:
  - 新增 `outline_world_setting`、`outline_plot`、`outline_causality`、`outline_character_focus`、`chapter_plot` 对应实体与 Mapper
  - 扩展 `Outline` / `Chapter` 实体字段和 `OutlineRequestDTO` / `ChapterRequestDTO`
  - 重写 `OutlineServiceImpl`，支持大纲类型、父子关系、生成章节关联、世界观关联和关系表优先读取
  - 重写 `ChapterServiceImpl`，支持章节状态、摘要、大纲绑定、剧情节点绑定、POV 字段与工作台元数据回填
- 修改文件:
  - `backend/src/main/java/com/storyweaver/domain/entity/Outline.java`
  - `backend/src/main/java/com/storyweaver/domain/entity/Chapter.java`
  - `backend/src/main/java/com/storyweaver/domain/entity/OutlineWorldSettingLink.java`
  - `backend/src/main/java/com/storyweaver/domain/entity/OutlinePlotLink.java`
  - `backend/src/main/java/com/storyweaver/domain/entity/OutlineCausalityLink.java`
  - `backend/src/main/java/com/storyweaver/domain/entity/OutlineCharacterFocusLink.java`
  - `backend/src/main/java/com/storyweaver/domain/entity/ChapterPlotLink.java`
  - `backend/src/main/java/com/storyweaver/repository/OutlineWorldSettingMapper.java`
  - `backend/src/main/java/com/storyweaver/repository/OutlinePlotMapper.java`
  - `backend/src/main/java/com/storyweaver/repository/OutlineCausalityMapper.java`
  - `backend/src/main/java/com/storyweaver/repository/OutlineCharacterFocusMapper.java`
  - `backend/src/main/java/com/storyweaver/repository/ChapterPlotMapper.java`
  - `backend/src/main/java/com/storyweaver/domain/dto/OutlineRequestDTO.java`
  - `backend/src/main/java/com/storyweaver/domain/dto/ChapterRequestDTO.java`
  - `backend/src/main/java/com/storyweaver/service/impl/OutlineServiceImpl.java`
  - `backend/src/main/java/com/storyweaver/service/impl/ChapterServiceImpl.java`
  - `docs/progress/PROGRESS-REQ-20260409-core-module-refactor.md`
  - `docs/agent-context.md`
- 验证:
  - 执行 `mvn -f backend/pom.xml -DskipTests compile` 成功
- 风险/遗留:
  - 运行期仍依赖数据库已具备 `012` 中的新字段和关系表
  - Plot / Causality / Character 相关的新结构尚未同步进服务层
- 下一步:
  - 继续补齐 Plot / Causality / Character 的兼容结构改造

### [2026-04-09 Asia/Shanghai] 完成 Step 1 第二批后端结构改造

- 背景:
  - Outline / Chapter 第一批改造完成后，仍需要把 Plot / Causality / Character 侧的核心结构和兼容写法补齐，避免模型层长期处于半升级状态。
- 本次完成:
  - 扩展 `Plot`，补齐 `storyBeatType / storyFunction / eventResult / prevBeatId / nextBeatId / outlinePriority`
  - 扩展 `Causality`，补齐 `causalType / triggerMode / payoffStatus / upstreamCauseIdsJson / downstreamEffectIdsJson`
  - 扩展 `Character`、`ProjectCharacterLink` 与 `CharacterRequestDTO`，补齐快速建角字段和 `roleType`
  - 改造 `PlotService` / `PlotCrudServiceImpl`，兼容新旧剧情字段的双向归一化
  - 改造 `CausalityServiceImpl`，兼容旧关系值、旧实体类型与旧前缀 ID
  - 改造 `CharacterServiceImpl` / `CharacterController`，支持 `identity / coreGoal / growthArc / activeStage / advancedProfileJson / roleType`
  - 改造 `DatabaseMigrationInitializer`，启动时最小补齐 Step 1 所需字段、关系表和配置项
- 修改文件:
  - `backend/src/main/java/com/storyweaver/config/DatabaseMigrationInitializer.java`
  - `backend/src/main/java/com/storyweaver/controller/CharacterController.java`
  - `backend/src/main/java/com/storyweaver/domain/dto/CharacterRequestDTO.java`
  - `backend/src/main/java/com/storyweaver/domain/entity/Plot.java`
  - `backend/src/main/java/com/storyweaver/domain/entity/Causality.java`
  - `backend/src/main/java/com/storyweaver/domain/entity/Character.java`
  - `backend/src/main/java/com/storyweaver/domain/entity/ProjectCharacterLink.java`
  - `backend/src/main/java/com/storyweaver/service/PlotService.java`
  - `backend/src/main/java/com/storyweaver/service/impl/PlotCrudServiceImpl.java`
  - `backend/src/main/java/com/storyweaver/service/impl/CausalityServiceImpl.java`
  - `backend/src/main/java/com/storyweaver/service/impl/CharacterServiceImpl.java`
  - `docs/progress/PROGRESS-REQ-20260409-core-module-refactor.md`
  - `docs/agent-context.md`
- 验证:
  - 再次执行 `mvn -f backend/pom.xml -DskipTests compile` 成功
- 风险/遗留:
  - 前端仍未消费这些新字段
  - AI 上下文与总导工具尚未切到优先读取新结构
- 下一步:
  - 继续推进前端 `types / stores / views` 和 AI 读取链路适配

### [2026-04-09 Asia/Shanghai] 完成 Step 1 前端核心页面兼容改造

- 背景:
  - Step 1 后端结构已基本补齐，但前端仍停留在旧模型，导致大纲树、章节工作台、剧情节拍、因果新字段和角色一级字段无法真正编辑与展示。
- 本次完成:
  - 扩展 `front/src/types/index.ts`，补齐 `Outline / Chapter / Character / Plot / Causality` 的重构字段
  - 改造 `OutlineView`，接入 `global / volume / chapter` 大纲类型、父级关系、绑定章节和世界观关联
  - 改造 `ChapterListView`，接入章节摘要、章节状态、大纲绑定、剧情节拍、POV、前后章节等工作台字段
  - 改造 `CharacterListView`，把 `identity / coreGoal / growthArc / activeStage / firstAppearanceChapterId / isRetired / advancedProfileJson` 相关能力接到页面
  - 改造 `PlotView`，接入 `storyBeatType / storyFunction / eventResult / prevBeatId / nextBeatId / outlinePriority`
  - 改造 `CausalityView`，接入 `causalType / triggerMode / payoffStatus / upstream / downstream`，并兼容 `story_beat / state` 新实体类型
- 修改文件:
  - `front/src/types/index.ts`
  - `front/src/api/character.ts`
  - `front/src/stores/character.ts`
  - `front/src/views/outline/OutlineView.vue`
  - `front/src/views/chapter/ChapterListView.vue`
  - `front/src/views/character/CharacterListView.vue`
  - `front/src/views/plot/PlotView.vue`
  - `front/src/views/causality/CausalityView.vue`
  - `docs/progress/PROGRESS-REQ-20260409-core-module-refactor.md`
  - `docs/agent-context.md`
- 验证:
  - 执行 `npm run build` 成功
  - 执行 `git diff --check` 成功
- 风险/遗留:
  - 当前仅完成构建级验证，尚未在真实浏览器和真实 API 数据上逐页回归
  - AI 写作上下文与 AI 总导读取链路还未切到优先消费新结构
- 下一步:
  - 继续推进 AI 上下文 / 总导读取链路适配，并补端到端联调

### [2026-04-09 Asia/Shanghai] 完成 Step 2 第一批 AI 读取链路适配

- 背景:
  - 前端主要页面已消费新结构，但 AI 写作上下文和总导工具仍主要按旧章节绑定和旧 prompt 字段工作，需要先把新结构真正送进模型。
- 本次完成:
  - 改造 `AIWritingServiceImpl.buildContextBundle(...)`，优先按 `chapter.outlineId` 解析当前大纲，并把 `storyBeatIds / relatedWorldSettingIds / chapter.summary / outline.stageGoal` 纳入上下文筛选
  - 改造 `AIWritingServiceImpl.buildUserPrompt(...)`，在 prompt 中补入章节摘要、章节状态、绑定大纲、主 POV、剧情节拍，以及 Plot / Causality 的新字段摘要
  - 改造 `AIDirectorApplicationServiceImpl.resolveOutline(...)`，优先使用章节显式绑定的大纲
  - 改造 `DirectorToolExecutor`，让 `chapter_snapshot / outline / plot / causality / world_setting / required_characters` 工具返回新结构字段
- 修改文件:
  - `backend/src/main/java/com/storyweaver/service/impl/AIWritingServiceImpl.java`
  - `backend/src/main/java/com/storyweaver/ai/director/application/impl/AIDirectorApplicationServiceImpl.java`
  - `backend/src/main/java/com/storyweaver/ai/director/application/tool/DirectorToolExecutor.java`
  - `docs/progress/PROGRESS-REQ-20260409-core-module-refactor.md`
  - `docs/agent-context.md`
- 验证:
  - 执行 `mvn -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -f backend/pom.xml -DskipTests compile` 成功
  - 执行 `git diff --check` 成功
- 风险/遗留:
  - 当前只验证到编译级，尚未对真实章节做总导决策 / prompt 命中检查
  - outline 树的多层父链信息尚未单独形成完整 AI 上下文片段
- 下一步:
  - 继续做真实数据联调，检查 AI 决策与 prompt 是否稳定命中新结构
