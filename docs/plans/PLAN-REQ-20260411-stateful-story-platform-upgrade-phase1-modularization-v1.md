# Story 平台升级 Phase 1B 详细实施计划：粗粒度模块拆分

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Plan ID: PLAN-REQ-20260411-stateful-story-platform-upgrade-phase1-modularization-v1
- Status: Completed
- Created At: 2026-04-11 Asia/Shanghai
- Updated At: 2026-04-12 Asia/Shanghai

## 本轮目标

在不打断现有 Spring Boot 应用壳的前提下，完成 `Phase 1B` 的粗粒度模块拆分与边界收敛，为后续继续拆 `generation / provider / web / infra` 打底。

本轮只追求三件事：

- 建立根 Maven 聚合工程
- 抽出低耦合共享模块
- 保持现有 `backend` 仍可作为应用壳继续运行

## 本轮拆分策略

`Phase 1B` 不追求把所有边界一次切完，而是分三段完成：

- `1B.1`
  - 先抽低耦合共享模块，让工程先站住
- `1B.2`
  - 收敛构建入口、活动文档和部署口径，消除“代码已拆、文档还按旧工程描述”的偏差
- `1B.3`
  - 在不继续大规模搬运的前提下，冻结下一批粗模块边界，明确：
    - `story-generation`
    - `story-provider`
    - `story-web`
    - `story-infra`
  - 仍归属 `Phase 1B`，不单独另开新阶段

`1B.1` 当前已完成的拆分策略是：

1. `story-domain`
   - 承载 `com.storyweaver.domain.entity.*`
   - 承载 `com.storyweaver.item.domain.*`
2. `story-storyunit`
   - 承载 `com.storyweaver.storyunit.*`

`backend` 当前继续保留：

- Spring Boot 应用入口
- controller / service / repository / config / security / utils
- `com.storyweaver.domain.dto.*`
- `com.storyweaver.domain.vo.*`
- 其余尚未稳定的业务实现

这样做的原因：

- `dto/vo` 中已经存在对 `story.generation` 的反向依赖
- 如果第一轮把整个 `domain` 一口气搬走，会立刻打破依赖方向
- 先抽 `entity + item domain + storyunit`，收益明显，风险最低

## 阶段分段

### `1B.1` 已完成

- 根 Maven 聚合工程已建立
- `story-domain` 已抽出
- `story-storyunit` 已抽出
- `backend` 继续作为应用壳保留
- 后端 Docker 构建和发布脚本已切到根工程上下文

### `1B.2` 已完成

- 活动文档中的根工程构建入口已统一
- 旧单模块构建入口已降级为历史/风险说明，不再作为执行口径
- 模块结构与部署入口描述已统一回填到活动文档

### `1B.3` 已完成

- `story-generation / story-provider / story-web / story-infra` 的粗边界已经冻结
- 最小模块壳已经建立
- 当前暂留 `backend` 的包已经写清
- 下一轮优先迁移顺序已经冻结

当前进展：

- `story-generation` 的合同层已经开始抽出：
  - `com.storyweaver.story.generation.*`
    - 顶层接口
    - VO / Result
    - Suggestion / Pack
  - generation 相关 request DTO：
    - `StructuredCreationApplyRequestDTO`
    - `StructuredSummaryApplyRequestDTO`
    - `ChapterAnchorUpdateRequestDTO`
- `com.storyweaver.story.generation.impl.*` 仍暂留 `backend`
- `story-provider` 的合同层已经开始抽出：
  - `AIProviderService`
  - `ProviderDiscoveryVO`
- `AIProviderServiceImpl` 与 `AIModelRoutingService` 仍暂留 `backend`
- `story-web` 已建立，并承载最小 HTTP 公共响应壳：
  - `ApiResponse`
  - `ApiErrorResponse`
- `story-infra` 已建立，并承载首批持久化接口：
  - `com.storyweaver.repository.*`
  - `com.storyweaver.item.infrastructure.persistence.mapper.*`
- controller / config / security / service impl 仍暂留 `backend`

## 本轮产出

### 工程结构

- 新增根 `pom.xml`，作为聚合父工程
- 新增模块：
  - `story-domain`
  - `story-storyunit`
  - `story-generation`
  - `story-provider`
  - `story-web`
  - `story-infra`
- 保留应用模块：
  - `backend`

### 已完成迁移

- `backend/src/main/java/com/storyweaver/domain/entity/*`
  - 已迁移到 `story-domain/src/main/java/com/storyweaver/domain/entity/*`
- `backend/src/main/java/com/storyweaver/item/domain/*`
  - 已迁移到 `story-domain/src/main/java/com/storyweaver/item/domain/*`
- `backend/src/main/java/com/storyweaver/storyunit/*`
  - 已迁移到 `story-storyunit/src/main/java/com/storyweaver/storyunit/*`
- `backend/src/main/java/com/storyweaver/story/generation/*.java`
  - 顶层合同层已迁移到 `story-generation/src/main/java/com/storyweaver/story/generation/*`
- `backend/src/main/java/com/storyweaver/domain/dto/StructuredCreationApplyRequestDTO.java`
  - 已迁移到 `story-generation/src/main/java/com/storyweaver/domain/dto/StructuredCreationApplyRequestDTO.java`
- `backend/src/main/java/com/storyweaver/domain/dto/StructuredSummaryApplyRequestDTO.java`
  - 已迁移到 `story-generation/src/main/java/com/storyweaver/domain/dto/StructuredSummaryApplyRequestDTO.java`
- `backend/src/main/java/com/storyweaver/domain/dto/ChapterAnchorUpdateRequestDTO.java`
  - 已迁移到 `story-generation/src/main/java/com/storyweaver/domain/dto/ChapterAnchorUpdateRequestDTO.java`
- `backend/src/main/java/com/storyweaver/service/AIProviderService.java`
  - 已迁移到 `story-provider/src/main/java/com/storyweaver/service/AIProviderService.java`
- `backend/src/main/java/com/storyweaver/domain/vo/ProviderDiscoveryVO.java`
  - 已迁移到 `story-provider/src/main/java/com/storyweaver/domain/vo/ProviderDiscoveryVO.java`
- `backend/src/main/java/com/storyweaver/common/web/ApiResponse.java`
  - 已迁移到 `story-web/src/main/java/com/storyweaver/common/web/ApiResponse.java`
- `backend/src/main/java/com/storyweaver/exception/ApiErrorResponse.java`
  - 已迁移到 `story-web/src/main/java/com/storyweaver/exception/ApiErrorResponse.java`
- `backend/src/main/java/com/storyweaver/repository/*`
  - 已迁移到 `story-infra/src/main/java/com/storyweaver/repository/*`
- `backend/src/main/java/com/storyweaver/item/infrastructure/persistence/mapper/*`
  - 已迁移到 `story-infra/src/main/java/com/storyweaver/item/infrastructure/persistence/mapper/*`

### 当前暂留 `backend` 的 generation 包

- `com.storyweaver.story.generation.impl.*`

保留原因：

- 实现层仍直接依赖 `repository / service / transaction`
- 当前先抽合同层，可以避免在本轮引入循环依赖

### 当前暂留 `backend` 的 provider 包

- `com.storyweaver.service.impl.AIProviderServiceImpl`
- `com.storyweaver.service.AIModelRoutingService`

保留原因：

- `AIProviderServiceImpl` 仍直接依赖 repository、事务和 HTTP 调用实现
- `AIModelRoutingService` 仍直接依赖 `SystemConfigService`

### 已完成构建链路调整

- 根工程可执行：
  - `mvn -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -DskipTests compile`
- `backend` Docker 构建已切到根工程上下文
- 发布脚本已切到根工程上下文构建后端镜像

### 当前暂留 `backend` 的 web 包

- `com.storyweaver.controller.*`
- `com.storyweaver.exception.GlobalExceptionHandler`
- `com.storyweaver.exception.RestAuthenticationEntryPoint`
- `com.storyweaver.exception.RestAccessDeniedHandler`
- `com.storyweaver.controller.AuthHeaderSupport`

保留原因：

- 当前仍是 Spring MVC 入口与安全响应装配层
- 这一层在本轮只冻结边界，不急着拆出控制器实现

### 当前暂留 `backend` 的 infra 相关包

- `com.storyweaver.config.*`
- `com.storyweaver.security.*`
- `src/main/resources/mapper/*`

保留原因：

- 仍与 Spring Boot 应用壳、Mapper 扫描和资源装配强绑定
- 本轮先抽 mapper 接口，不在这一轮移动配置与 XML 资源

## 本轮未做

- 未继续拆 `story-generation`
- `story-generation` 只完成了合同层抽离，未完成实现层拆分
- `story-provider` 只完成了合同层抽离，未完成实现层拆分
- `story-web` 只完成了公共响应壳抽离，未拆 controller/handler 实现
- `story-infra` 只完成了 mapper 接口抽离，未拆 config/security/resource 实现

这些内容被明确留给后续阶段，不再属于 `Phase 1B` 收尾范围。

## 风险与结论

### 已确认风险

- 旧的单模块构建入口：
  - `mvn -f backend/pom.xml -DskipTests compile`
  - 不再是有效主入口
- 原因不是代码错误，而是 `backend` 已经依赖兄弟模块，必须通过根工程聚合构建

### 当前结论

- `1B.1` 已成功站住
- 现有部署链路只要走根工程上下文即可继续工作
- `Phase 1B` 已完成
- 当前完成度为 `100%`，但仅限当前定义的粗粒度模块拆分范围
- `Phase 1B` 的结束不包含实现层继续外搬
- 后续继续拆更细模块时，不必再处理 `entity / storyunit` 的物理迁移

## 当前模块归属表

- `story-domain`
  - `com.storyweaver.domain.entity.*`
  - `com.storyweaver.item.domain.*`
- `story-storyunit`
  - `com.storyweaver.storyunit.*`
- `story-generation`
  - `com.storyweaver.story.generation.*` 顶层合同层
  - generation 相关 request DTO
- `story-provider`
  - `AIProviderService`
  - `ProviderDiscoveryVO`
- `story-web`
  - `ApiResponse`
  - `ApiErrorResponse`
- `story-infra`
  - `com.storyweaver.repository.*`
  - `com.storyweaver.item.infrastructure.persistence.mapper.*`
- `backend`
  - Spring Boot 应用入口
  - controller / service / impl / config / security / utils
  - 剩余 DTO / VO
  - `generation.impl`
  - `AIProviderServiceImpl / AIModelRoutingService`
  - mapper XML 资源

## 下一轮优先迁移顺序

1. `StoryUnit + Facets` 的存储映射与服务层协议落地
2. `SystemConfigService / 路由策略` 抽象，为后续 `provider` 实现层迁移解耦
3. `generation.impl` 从 `service / repository / transaction` 反向依赖中解耦
4. `story-web` 的 controller 分层与工作台新接口组织
5. `story-infra` 的配置、扫描与 mapper XML 资源收敛

## 验证方式

- `mvn -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -DskipTests compile`
- `git diff --check`

当前结果：

- 根工程 Reactor 编译已通过
- `story-domain` 编译通过
- `story-storyunit` 编译通过
- `story-generation` 编译通过
- `story-provider` 编译通过
- `story-web` 编译通过
- `story-infra` 编译通过
- `story-weaver-backend` 编译通过
- `git diff --check` 已通过

## 本轮收尾建议

如果只以当前 `Phase 1B` 的定义作为验收口径，本轮收尾已严格限制在：

1. 文档口径统一
2. 暂留边界写死
3. 下一轮迁移顺序写死

不建议在 `Phase 1B` 收尾时继续外搬：

- `generation.impl`
- `AIProviderServiceImpl / AIModelRoutingService`
- controller 实现
- config / security / mapper XML 资源

## Phase 1B 退出条件

`Phase 1B` 只有在下面这些条件全部满足后才算结束：

- 根工程聚合构建口径在活动文档中已经统一
- 当前已抽出的模块边界不再含糊
- 下一批粗模块的边界和迁移顺序已经冻结
- `backend` 中哪些包是“临时保留”已经写清
- 后续进入更细模块拆分时，不需要重新解释当前模块结构

当前状态：

- 以上退出条件已满足

## 贡献与署名说明

- 模块尽早拆分的判断与方向要求：用户提出。
- 第一轮粗粒度模块拆分方案、Maven 聚合结构、构建链路调整与文档整理：Codex 完成。
- 当前方案由用户与 Codex 共同讨论形成。
