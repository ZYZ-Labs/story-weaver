# Story 平台升级 Phase 1B 详细实施计划：粗粒度模块拆分

- Req ID: REQ-20260411-stateful-story-platform-upgrade
- Plan ID: PLAN-REQ-20260411-stateful-story-platform-upgrade-phase1-modularization-v1
- Status: In Progress
- Created At: 2026-04-11 Asia/Shanghai
- Updated At: 2026-04-11 Asia/Shanghai

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

### `1B.2` 未完成但仍属于当前 Phase 1B

- 清理活动文档中的旧构建入口：
  - `mvn -f backend/pom.xml ...`
- 清理活动文档中的旧目录口径：
  - `backend/src/main/java/com/storyweaver/storyunit/*`
- 补齐根工程构建、模块结构和部署入口的统一说明
- 补齐哪些脚本、哪些构建链必须从仓库根目录执行

### `1B.3` 未完成但仍属于当前 Phase 1B

- 冻结下一批粗模块的代码边界，不急着立即搬代码：
  - `story-generation`
  - `story-provider`
  - `story-web`
  - `story-infra`
- 明确哪些包先留在 `backend`
- 明确哪些包下一轮可整体迁移
- 明确哪些反向依赖必须先消除，才能继续拆

## 本轮产出

### 工程结构

- 新增根 `pom.xml`，作为聚合父工程
- 新增模块：
  - `story-domain`
  - `story-storyunit`
- 保留应用模块：
  - `backend`

### 已完成迁移

- `backend/src/main/java/com/storyweaver/domain/entity/*`
  - 已迁移到 `story-domain/src/main/java/com/storyweaver/domain/entity/*`
- `backend/src/main/java/com/storyweaver/item/domain/*`
  - 已迁移到 `story-domain/src/main/java/com/storyweaver/item/domain/*`
- `backend/src/main/java/com/storyweaver/storyunit/*`
  - 已迁移到 `story-storyunit/src/main/java/com/storyweaver/storyunit/*`

### 已完成构建链路调整

- 根工程可执行：
  - `mvn -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -DskipTests compile`
- `backend` Docker 构建已切到根工程上下文
- 发布脚本已切到根工程上下文构建后端镜像

## 本轮未做

- `1B.2` 的活动文档口径清理尚未完成
- `1B.3` 的下一批粗模块边界冻结尚未完成
- 未继续拆 `story-generation`
- 未继续拆 `story-provider`
- 未继续拆 `story-web`
- 未继续拆 `story-infra`

## 风险与结论

### 已确认风险

- 旧的单模块构建入口：
  - `mvn -f backend/pom.xml -DskipTests compile`
  - 不再是有效主入口
- 原因不是代码错误，而是 `backend` 已经依赖兄弟模块，必须通过根工程聚合构建

### 当前结论

- `1B.1` 已成功站住
- 现有部署链路只要走根工程上下文即可继续工作
- `Phase 1B` 还没有完成
- 后续继续拆更细模块时，不必再处理 `entity / storyunit` 的物理迁移

## 验证方式

- `mvn -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -DskipTests compile`
- `git diff --check`

当前结果：

- 根工程 Reactor 编译已通过
- `story-domain` 编译通过
- `story-storyunit` 编译通过
- `story-weaver-backend` 编译通过
- `git diff --check` 已通过

## Phase 1B 剩余工作

在当前口径下，剩余工作仍全部归属 `Phase 1B`：

1. 完成 `1B.2`
   - 清理活动文档中的旧构建入口
   - 统一根工程构建口径
   - 统一模块结构描述
2. 完成 `1B.3`
   - 冻结 `story-generation / story-provider / story-web / story-infra` 的粗边界
   - 标注继续留在 `backend` 的临时包
   - 标注下一轮优先迁移包

## Phase 1B 退出条件

`Phase 1B` 只有在下面这些条件全部满足后才算结束：

- 根工程聚合构建口径在活动文档中已经统一
- 当前已抽出的模块边界不再含糊
- 下一批粗模块的边界和迁移顺序已经冻结
- `backend` 中哪些包是“临时保留”已经写清
- 后续进入更细模块拆分时，不需要重新解释当前模块结构

## 贡献与署名说明

- 模块尽早拆分的判断与方向要求：用户提出。
- 第一轮粗粒度模块拆分方案、Maven 聚合结构、构建链路调整与文档整理：Codex 完成。
- 当前方案由用户与 Codex 共同讨论形成。
