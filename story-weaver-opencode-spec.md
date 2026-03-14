# Story Weaver 项目开发总规范（AI 原生开发版）

> 项目名称：`story-weaver`  
> 目标：构建一个 **AI 续写小说 + 外置因果管理 + 自动入库 RAG + 可视化写作后台** 的前后端分离系统。  
> 本文档用于驱动 AI / opencode / 开发协作者从零开始实现完整项目。  
> 本文档是 **唯一主规范**，后续实现不得偏离本文档。  

---

# 1. 项目定位

## 1.1 项目核心目标

构建一个可长期演进的小说创作平台，围绕“**小说项目（Novel Project）**”展开，具备以下核心能力：

1. 使用 AI 进行章节续写、润色、扩写、改写。
2. 将“人物 / 世界设定 / 章节 / 剧情 / 因果关系 / 记忆片段”外置化管理。
3. AI 输出的内容，在人工确认后自动进入 RAG 记忆库。
4. 外置因果支持手动维护、手动调权、手动修正。
5. 支持多 AI 供应商配置和切换。
6. 采用前后端分离架构：
   - 前端：Vue 3 + Vuetify（基于 Berry Free Vuetify Vue Admin Template）
   - 后端：Spring Boot
   - 数据库：MySQL
7. 项目目录必须在一个根目录下，形如：
   - `story-weaver/front`
   - `story-weaver/backend`
8. 支持一键启动（开发环境 + Docker 环境都支持）。

---

## 1.2 非目标（Non Goals）

以下内容 **不是当前 MVP 的核心目标**：

1. 不做复杂支付系统。
2. 不做多租户 SaaS 商业计费系统。
3. 不做复杂工作流引擎。
4. 不做训练大模型。
5. 不做全文搜索引擎集群（ES 可作为未来扩展）。
6. 不做复杂实时协同编辑（MVP 先单人/弱协作为主）。
7. 不做自动发布到小说平台。

---

# 2. 技术栈

## 2.1 前端

- Node.js 20 LTS
- Vue 3
- Vite
- Vuetify 3
- Vue Router
- Pinia
- Axios
- ECharts（统计图/因果图基础展示）
- TipTap 或 Monaco Editor（二选一，推荐 TipTap + Markdown 模式）
- 基于模板：
  - `https://github.com/codedthemes/berry-free-vuetify-vuejs-admin-template`

---

## 2.2 后端

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Security
- Spring Data JPA
- MyBatis-Plus（可选，二选一；**本项目统一建议使用 MyBatis-Plus**）
- JWT 鉴权
- Validation
- Lombok
- MapStruct
- MySQL 8.x
- Redis（用于缓存、会话辅助、RAG 检索缓存）
- MinIO（可选，存储附件 / 封面 / 导入文档）
- Quartz 或 Spring Scheduler（任务调度）
- LangChain4j 或自研 Provider Adapter（推荐自研 Adapter，避免被框架绑死）
- Milvus / pgvector / 本地向量表（三选一）
  - **MVP 建议：MySQL + embedding_cache + text_chunk 表 + 简单向量字段占位**
  - **增强版建议：pgvector 或 Milvus**
- Maven

---

## 2.3 一键运行方案

必须同时支持两种：

### 方案 A：开发模式一键运行
- Windows：
  - `start-dev.bat`
- Linux/macOS：
  - `start-dev.sh`

功能：
1. 启动前端
2. 启动后端
3. 自动检查依赖
4. 自动提示访问地址

### 方案 B：Docker Compose 一键运行
- `docker-compose.yml`

服务：
1. mysql
2. redis
3. minio（可选）
4. backend
5. front

---

# 3. 项目目录结构（强制）

```text
story-weaver/
├── README.md
├── start-dev.bat
├── start-dev.sh
├── docker-compose.yml
├── .env
├── sql/
│   ├── 001_init.sql
│   ├── 002_seed.sql
│   └── 003_alter_xxx.sql
├── docs/
│   ├── api/
│   ├── db/
│   ├── prompts/
│   └── images/
├── scripts/
│   ├── init-db.sh
│   ├── init-db.bat
│   └── package-release.sh
├── front/
│   ├── package.json
│   ├── vite.config.ts
│   ├── .env.development
│   ├── .env.production
│   ├── src/
│   │   ├── api/
│   │   ├── assets/
│   │   ├── components/
│   │   ├── layouts/
│   │   ├── router/
│   │   ├── stores/
│   │   ├── utils/
│   │   ├── views/
│   │   │   ├── login/
│   │   │   ├── dashboard/
│   │   │   ├── project/
│   │   │   ├── chapter/
│   │   │   ├── character/
│   │   │   ├── plot/
│   │   │   ├── causality/
│   │   │   ├── writing/
│   │   │   ├── rag/
│   │   │   ├── provider/
│   │   │   ├── settings/
│   │   │   └── system/
│   │   └── App.vue
│   └── ...
└── backend/
    ├── pom.xml
    ├── Dockerfile
    ├── src/main/java/com/storyweaver/
    │   ├── StoryWeaverApplication.java
    │   ├── common/
    │   ├── config/
    │   ├── security/
    │   ├── modules/
    │   │   ├── auth/
    │   │   ├── user/
    │   │   ├── project/
    │   │   ├── chapter/
    │   │   ├── character/
    │   │   ├── plot/
    │   │   ├── causality/
    │   │   ├── writing/
    │   │   ├── rag/
    │   │   ├── provider/
    │   │   ├── knowledge/
    │   │   ├── outline/
    │   │   ├── prompt/
    │   │   ├── task/
    │   │   └── system/
    │   └── infrastructure/
    │       ├── ai/
    │       ├── vector/
    │       ├── storage/
    │       ├── cache/
    │       └── scheduler/
    ├── src/main/resources/
    │   ├── application.yml
    │   ├── application-dev.yml
    │   ├── application-prod.yml
    │   ├── mapper/
    │   └── db/
    └── ...
```

---

# 4. 数据库配置（按你当前环境固定）

## 4.1 MySQL 地址

- Host: `192.168.5.249`
- Port: `3306`
- Username: `root`
- Password: `Zhouwenjian:2871`

## 4.2 数据库名

统一使用：

- Database: `story_weaver`

## 4.3 建库语句

```sql
CREATE DATABASE IF NOT EXISTS story_weaver
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;
```

---

# 5. 核心业务模型

整个系统以 **小说项目 Project** 为总单位。

## 5.1 层级关系

```text
User
 └── Project（小说项目）
      ├── Worldbook / Global Setting（全局设定）
      ├── Characters（人物）
      ├── Plotlines（剧情线）
      ├── Causality Nodes（因果节点）
      ├── Causality Edges（因果关系边）
      ├── Chapter Outlines（章节大纲）
      ├── Chapters（章节）
      ├── Writing Drafts（写作草稿）
      ├── RAG Knowledge（知识碎片）
      └── AI Provider Bindings（AI供应商配置）
```

---

## 5.2 业务模块清单

### 必做模块
1. 登录 / 注册 / 用户管理
2. 小说项目管理
3. 章节管理
4. 章节大纲管理
5. 人物管理
6. 剧情管理
7. 因果管理
8. 写作编辑器
9. AI 续写
10. AI 供应商设置
11. RAG 入库与检索
12. 项目设置
13. 操作日志
14. 系统配置

### 建议补充模块
1. 世界观设定模块
2. 关系网模块（人物关系）
3. 标签系统
4. Prompt 模板模块
5. 任务中心（异步生成）
6. 版本快照模块
7. 审核确认模块（AI 输出入库前确认）
8. 回收站

---

# 6. 权限模型

## 6.1 用户角色

MVP 先做两类：

1. `ADMIN`
2. `AUTHOR`

说明：
- ADMIN：系统管理、查看全部配置
- AUTHOR：日常创作用户

后续可扩展：
- EDITOR
- REVIEWER

---

## 6.2 登录方式

MVP 采用：
- 用户名 / 密码登录
- JWT Access Token + Refresh Token

---

# 7. 核心页面规划（前端）

前端必须基于 Berry Free Vuetify Vue Admin Template 改造，不允许脱离模板体系瞎写一套。

## 7.1 页面菜单结构

```text
Dashboard
项目管理
  ├── 项目列表
  ├── 新建项目
  └── 项目详情

人物管理
  ├── 人物列表
  ├── 人物详情
  └── 关系图谱

剧情管理
  ├── 剧情线列表
  ├── 剧情节点
  └── 冲突/伏笔管理

因果管理
  ├── 因果节点
  ├── 因果边
  ├── 因果图谱
  └── 因果值调整

章节管理
  ├── 章节列表
  ├── 章节详情
  ├── 章节大纲
  └── 草稿历史

写作中心
  ├── 写作编辑器
  ├── AI续写
  ├── AI改写
  ├── AI润色
  └── 上下文记忆面板

RAG知识库
  ├── 知识片段
  ├── 检索测试
  ├── 入库记录
  └── Embedding状态

AI供应商
  ├── 供应商列表
  ├── 模型配置
  ├── 连通性测试
  └── 默认策略

系统设置
  ├── 用户中心
  ├── 项目参数
  ├── Prompt模板
  └── 系统日志
```

---

## 7.2 核心页面说明

### 1）登录页
- 用户名
- 密码
- 登录按钮
- 记住我
- 登录错误提示

### 2）Dashboard
展示：
- 项目总数
- 章节数
- 人物数
- 因果节点数
- 最近写作记录
- 最近 AI 任务
- 最近 RAG 入库状态

### 3）项目详情页
包含 Tab：
- 基本信息
- 世界设定
- 人物
- 剧情
- 因果
- 章节
- RAG
- AI 设置

### 4）写作中心页（核心）
布局建议三栏：

左侧：
- 项目上下文
- 章节列表
- 大纲

中间：
- 编辑器
- AI 操作按钮（续写 / 改写 / 扩写 / 润色 / 生成下一段）

右侧：
- RAG召回上下文
- 人物摘要
- 因果摘要
- Prompt预览
- 最近生成历史

### 5）因果管理页（核心）
包括：
- 节点列表
- 边列表
- 图谱展示
- 手工编辑边权重
- 节点状态（激活/弱化/完成/废弃）
- 因果值修正历史

---

# 8. 核心业务规则

## 8.1 Project（小说项目）

每个小说项目必须具备：

- 名称
- 简介
- 类型（玄幻/都市/科幻/...）
- 世界观摘要
- 写作风格
- 目标篇幅
- 状态（草稿/连载中/完结/归档）
- 默认 AI 配置
- 默认 RAG 策略

---

## 8.2 Character（人物）

人物必须支持：

- 基本信息
  - 姓名
  - 别名
  - 性别
  - 年龄
  - 阵营
- 外观设定
- 性格设定
- 背景经历
- 当前状态
- 与其他人物关系
- 首次出场章节
- 重要标签
- 是否主角
- 当前剧情目标
- 当前秘密 / 隐藏信息

---

## 8.3 Plotline（剧情线）

剧情线支持：

- 主线 / 支线
- 所属项目
- 标题
- 摘要
- 当前阶段
- 优先级
- 起始章节
- 结束章节
- 关联人物
- 关联因果节点
- 状态（计划中/进行中/已完成/废弃）

---

## 8.4 Chapter（章节）

章节支持：

- 章节号
- 标题
- 大纲
- 正文
- 摘要
- 状态（草稿/生成中/待确认/已确认/发布态）
- 字数
- 关联剧情线
- 关联人物
- 关联因果节点
- RAG 入库状态
- 版本号
- 上一章 / 下一章

---

## 8.5 Causality（外置因果）

这是本项目差异化核心，必须严格建模。

### 因果节点（Causality Node）
字段建议：
- 名称
- 类型
  - 动机
  - 事件
  - 冲突
  - 目标
  - 伏笔
  - 转折
  - 结果
- 描述
- 所属项目
- 关联章节
- 关联人物
- 状态
  - active
  - inactive
  - resolved
  - deprecated
- 强度值（0-100）
- 紧迫度（0-100）
- 备注

### 因果边（Causality Edge）
字段建议：
- sourceNodeId
- targetNodeId
- relationType
  - causes
  - motivates
  - blocks
  - reveals
  - escalates
  - resolves
- weight（-100 ~ 100）
- confidence（0 ~ 100）
- manualOverride（是否人工覆盖）
- note

### 规则
1. 因果值允许手工修改。
2. AI 生成内容不能直接强制改写人工锁定的因果边。
3. manualOverride=true 的边，AI 只能建议，不得自动覆盖。
4. 因果边的调整要有历史记录。
5. 被 resolved 的节点默认降权，但不删除。

---

## 8.6 RAG（知识记忆）

### 自动入库原则
AI 输出内容 **不是一生成就立即入库**，必须经过以下流程：

1. AI 生成候选文本
2. 用户确认采用
3. 系统抽取摘要 / 实体 / 事件
4. 分块 chunk
5. 写入知识表
6. 生成 embedding
7. 入向量索引
8. 标记可检索

### 入库来源
- 已确认章节正文
- 已确认人物设定
- 已确认剧情设定
- 已确认因果解释
- 用户手工补充知识条目

### RAG 检索范围
- 当前项目内优先
- 当前章节邻近上下文优先
- 人物相关优先
- 因果相关优先
- 最新确认内容优先

---

# 9. AI 供应商模型设计

系统要支持多供应商，不允许把调用写死。

## 9.1 支持的供应商类型

- OpenAI 兼容接口
- DeepSeek 兼容接口
- Ollama
- 自定义 OpenAI-like Provider

---

## 9.2 Provider 配置字段

- providerName
- providerType
- baseUrl
- apiKey
- modelName
- embeddingModel
- temperature
- topP
- maxTokens
- timeoutSeconds
- enabled
- isDefault
- remark

---

## 9.3 AI 能力分类

- 续写
- 改写
- 润色
- 大纲生成
- 人物补全
- 剧情建议
- 因果建议
- 摘要生成
- embedding 生成

不同能力可绑定不同模型。

---

# 10. 后端模块划分（强制）

## 10.1 auth
- 登录
- 刷新 token
- 获取当前用户信息
- 登出

## 10.2 user
- 用户 CRUD
- 用户状态管理

## 10.3 project
- 小说项目 CRUD
- 项目设置
- 项目统计

## 10.4 chapter
- 章节 CRUD
- 章节排序
- 章节版本
- 章节确认发布

## 10.5 outline
- 章节大纲 CRUD
- 大纲 AI 生成

## 10.6 character
- 人物 CRUD
- 人物关系
- 人物标签

## 10.7 plot
- 剧情线 CRUD
- 剧情节点
- 剧情状态推进

## 10.8 causality
- 因果节点 CRUD
- 因果边 CRUD
- 因果值修改
- 因果图谱数据
- 调整历史

## 10.9 writing
- 写作草稿
- AI 续写/改写/润色
- Prompt 组装
- 生成记录

## 10.10 rag
- 知识片段入库
- chunk 管理
- embedding 管理
- 检索测试

## 10.11 provider
- AI 供应商管理
- 模型测试
- 模型能力映射

## 10.12 prompt
- Prompt 模板
- 系统提示词
- 项目级提示词
- 任务级提示词

## 10.13 task
- 异步任务中心
- AI任务记录
- 重试 / 失败记录

## 10.14 system
- 配置项
- 字典
- 操作日志
- 健康检查

---

# 11. 数据库表设计（MVP 完整版）

以下是建议表。不是能删减成几张玩具表的那种，MVP 就按这个层级做。

## 11.1 用户与认证

### `sys_user`
- id
- username
- password_hash
- nickname
- email
- role_code
- status
- last_login_at
- created_at
- updated_at
- deleted

### `sys_refresh_token`
- id
- user_id
- token
- expired_at
- created_at

---

## 11.2 项目

### `novel_project`
- id
- owner_id
- name
- slug
- category
- summary
- world_setting
- writing_style
- target_word_count
- status
- default_provider_id
- default_generation_model
- default_embedding_model
- rag_enabled
- created_at
- updated_at
- deleted

### `novel_project_setting`
- id
- project_id
- setting_key
- setting_value
- created_at
- updated_at

---

## 11.3 章节与大纲

### `chapter_outline`
- id
- project_id
- chapter_no
- title
- outline_text
- status
- created_at
- updated_at
- deleted

### `chapter`
- id
- project_id
- chapter_no
- title
- summary
- content_markdown
- word_count
- version_no
- status
- rag_status
- confirmed_at
- created_at
- updated_at
- deleted

### `chapter_version`
- id
- chapter_id
- version_no
- content_markdown
- change_note
- created_by
- created_at

---

## 11.4 人物

### `character_profile`
- id
- project_id
- name
- alias_name
- gender
- age_desc
- camp
- appearance
- personality
- background_story
- current_status
- goal
- secret_info
- first_appearance_chapter_no
- is_main_character
- created_at
- updated_at
- deleted

### `character_relation`
- id
- project_id
- source_character_id
- target_character_id
- relation_type
- relation_desc
- intimacy_score
- created_at
- updated_at

---

## 11.5 剧情

### `plotline`
- id
- project_id
- title
- plot_type
- summary
- priority
- status
- start_chapter_no
- end_chapter_no
- created_at
- updated_at
- deleted

### `plot_node`
- id
- plotline_id
- project_id
- title
- node_type
- description
- status
- sort_no
- created_at
- updated_at
- deleted

---

## 11.6 因果

### `causality_node`
- id
- project_id
- name
- node_type
- description
- status
- strength_value
- urgency_value
- manual_locked
- created_at
- updated_at
- deleted

### `causality_edge`
- id
- project_id
- source_node_id
- target_node_id
- relation_type
- weight_value
- confidence_value
- manual_override
- note
- created_at
- updated_at
- deleted

### `causality_adjust_log`
- id
- project_id
- node_id
- edge_id
- adjust_type
- old_value
- new_value
- reason
- operator_id
- created_at

---

## 11.7 写作与 AI 生成

### `writing_session`
- id
- project_id
- chapter_id
- session_name
- context_snapshot
- created_by
- created_at
- updated_at

### `writing_draft`
- id
- session_id
- chapter_id
- draft_type
- source_text
- prompt_text
- generated_text
- selected_provider_id
- selected_model
- status
- accepted
- created_at
- updated_at

### `ai_generation_record`
- id
- project_id
- chapter_id
- task_type
- provider_id
- model_name
- prompt_snapshot
- response_text
- token_usage_input
- token_usage_output
- latency_ms
- success_flag
- error_message
- created_by
- created_at

---

## 11.8 RAG 与知识

### `knowledge_document`
- id
- project_id
- source_type
- source_ref_id
- title
- content_text
- summary
- status
- created_at
- updated_at
- deleted

### `knowledge_chunk`
- id
- document_id
- project_id
- chunk_index
- content_text
- token_count
- embedding_status
- vector_text
- created_at
- updated_at

### `embedding_task`
- id
- project_id
- source_type
- source_ref_id
- model_name
- status
- retry_count
- error_message
- created_at
- updated_at

### `rag_retrieval_log`
- id
- project_id
- chapter_id
- query_text
- retrieved_refs
- created_at

---

## 11.9 AI 供应商

### `ai_provider`
- id
- name
- provider_type
- base_url
- api_key
- model_name
- embedding_model
- temperature
- top_p
- max_tokens
- timeout_seconds
- enabled
- is_default
- remark
- created_at
- updated_at
- deleted

### `ai_provider_capability`
- id
- provider_id
- capability_code
- model_name
- enabled
- created_at
- updated_at

---

## 11.10 Prompt

### `prompt_template`
- id
- project_id
- template_name
- template_type
- system_prompt
- user_prompt_template
- enabled
- created_at
- updated_at
- deleted

---

## 11.11 系统日志

### `operation_log`
- id
- operator_id
- module_code
- action_code
- biz_id
- request_payload
- response_payload
- created_at

---

# 12. 核心 API 设计（必须按 REST 风格）

以下只列核心接口，不代表全部接口数量。

## 12.1 认证

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `GET /api/auth/me`
- `POST /api/auth/logout`

## 12.2 项目

- `GET /api/projects`
- `POST /api/projects`
- `GET /api/projects/{id}`
- `PUT /api/projects/{id}`
- `DELETE /api/projects/{id}`

## 12.3 章节

- `GET /api/projects/{projectId}/chapters`
- `POST /api/projects/{projectId}/chapters`
- `GET /api/chapters/{id}`
- `PUT /api/chapters/{id}`
- `POST /api/chapters/{id}/confirm`
- `GET /api/chapters/{id}/versions`

## 12.4 大纲

- `GET /api/projects/{projectId}/outlines`
- `POST /api/projects/{projectId}/outlines`
- `POST /api/outlines/{id}/generate-ai`

## 12.5 人物

- `GET /api/projects/{projectId}/characters`
- `POST /api/projects/{projectId}/characters`
- `GET /api/characters/{id}`
- `PUT /api/characters/{id}`
- `GET /api/projects/{projectId}/character-relations`
- `POST /api/projects/{projectId}/character-relations`

## 12.6 剧情

- `GET /api/projects/{projectId}/plotlines`
- `POST /api/projects/{projectId}/plotlines`
- `GET /api/plotlines/{id}`
- `PUT /api/plotlines/{id}`

## 12.7 因果

- `GET /api/projects/{projectId}/causality/nodes`
- `POST /api/projects/{projectId}/causality/nodes`
- `PUT /api/causality/nodes/{id}`
- `GET /api/projects/{projectId}/causality/edges`
- `POST /api/projects/{projectId}/causality/edges`
- `PUT /api/causality/edges/{id}`
- `POST /api/causality/edges/{id}/adjust-weight`
- `GET /api/projects/{projectId}/causality/graph`
- `GET /api/projects/{projectId}/causality/adjust-logs`

## 12.8 写作

- `POST /api/writing/sessions`
- `GET /api/writing/sessions/{id}`
- `POST /api/writing/generate/continue`
- `POST /api/writing/generate/rewrite`
- `POST /api/writing/generate/polish`
- `POST /api/writing/generate/expand`
- `POST /api/writing/drafts/{id}/accept`
- `POST /api/writing/drafts/{id}/reject`

## 12.9 RAG

- `GET /api/projects/{projectId}/knowledge/documents`
- `GET /api/projects/{projectId}/knowledge/chunks`
- `POST /api/projects/{projectId}/rag/reindex`
- `POST /api/projects/{projectId}/rag/query`
- `GET /api/projects/{projectId}/rag/logs`

## 12.10 AI 供应商

- `GET /api/providers`
- `POST /api/providers`
- `PUT /api/providers/{id}`
- `POST /api/providers/{id}/test`
- `GET /api/providers/{id}/capabilities`
- `PUT /api/providers/{id}/capabilities`

---

# 13. AI 续写链路（必须这样做）

## 13.1 输入来源

AI 续写输入由以下部分拼装：

1. 当前章节已写正文
2. 当前章节大纲
3. 上一章节摘要
4. 当前项目世界设定
5. 相关人物摘要
6. 相关剧情线摘要
7. 相关因果节点/边摘要
8. RAG 检索召回片段
9. 当前任务 Prompt 模板
10. 用户本次操作附加指令

---

## 13.2 生成流程

```text
用户点击 AI续写
→ 后端构造 WritingContext
→ 执行 RAG 检索
→ 组装 Prompt
→ 调用 Provider Adapter
→ 保存生成记录
→ 返回草稿
→ 用户确认采用
→ 写入 chapter / version
→ 触发知识抽取
→ 写入 RAG
```

---

## 13.3 关键规则

1. AI 输出先作为草稿，不得直接覆盖正文。
2. 用户确认后才更新正式章节。
3. 正式章节更新后必须留版本历史。
4. 知识入库必须走异步任务，但要可追踪状态。
5. 因果建议要与人工锁定值隔离。

---

# 14. RAG 设计细则

## 14.1 知识来源类型

`source_type` 枚举建议：
- chapter
- character
- plot
- causality
- manual_note
- world_setting

---

## 14.2 Chunk 策略

建议：
- 按语义段落分块
- 每块 300~800 中文字
- 保留 source_ref_id
- 保留 chunk_index
- 保留摘要字段
- 保留关键词字段（可选）

---

## 14.3 检索策略

推荐组合：
1. 项目内过滤
2. source_type 加权
3. 人物命中加权
4. 因果命中加权
5. 最近确认内容加权

---

# 15. Prompt 体系

必须支持分层 Prompt：

1. 系统级 Prompt
2. 项目级 Prompt
3. 任务级 Prompt
4. 用户追加 Prompt

示例任务类型：
- CONTINUE_WRITING
- REWRITE_TEXT
- POLISH_TEXT
- EXPAND_OUTLINE
- GENERATE_OUTLINE
- SUGGEST_CAUSALITY
- SUMMARIZE_CHAPTER

---

# 16. 一键运行要求（强制）

## 16.1 根目录 README 要包含

1. 环境要求
2. MySQL 配置说明
3. 前后端启动方式
4. Docker 启动方式
5. 默认登录账户
6. 模板来源说明
7. 常见问题

---

## 16.2 `start-dev.bat` 需要做的事

1. 检查 Node / npm
2. 检查 Java / Maven
3. 提示数据库已配置
4. 启动后端
5. 启动前端

---

## 16.3 `start-dev.sh` 需要做的事

与 bat 一致。

---

## 16.4 Docker Compose 要求

服务名建议：
- `story-weaver-mysql`
- `story-weaver-redis`
- `story-weaver-backend`
- `story-weaver-front`

注意：
- 如果外部已经有 MySQL，则 docker 版本应允许通过 `.env` 切换是否使用内置 MySQL。
- 当前默认数据库使用：
  - `192.168.5.249:3306`

---

# 17. 前端实现要求（不能乱）

## 17.1 页面风格
- 沿用 Berry 模板布局
- 左侧菜单 + 顶部栏
- 内容页卡片化
- 表格 + 抽屉 + 弹窗结合
- 表单统一组件风格

## 17.2 状态管理
Pinia 分 store：
- authStore
- projectStore
- chapterStore
- characterStore
- plotStore
- causalityStore
- providerStore
- writingStore
- ragStore

## 17.3 API 封装
- 统一 axios 实例
- 统一 token 注入
- 统一错误处理
- 统一响应结构解析

---

# 18. 后端实现要求（不能瞎封装）

## 18.1 分层建议

```text
controller
service
service.impl
manager（可选）
mapper
entity
dto
vo
convert
```

## 18.2 统一响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

## 18.3 异常处理
- 业务异常
- 参数异常
- 鉴权异常
- 系统异常
统一全局处理。

## 18.4 安全
- JWT
- 接口鉴权
- 密码 BCrypt
- Provider api_key 返回前脱敏

---

# 19. 初始化数据要求

必须提供初始化 SQL，至少包括：

1. 管理员账户
2. 示例项目
3. 示例人物
4. 示例剧情
5. 示例因果节点
6. 示例因果边
7. 示例 Prompt 模板

默认管理员账户建议：
- username: `admin`
- password: `Admin@123456`

---

# 20. 开发顺序（强制路线）

## Phase 1：基础骨架
1. 根目录结构
2. front 初始化 + Berry 模板接入
3. backend 初始化 + 安全框架接入
4. 登录接口
5. 项目管理 CRUD

## Phase 2：创作核心
1. 章节管理
2. 人物管理
3. 剧情管理
4. 大纲管理
5. 写作中心页面

## Phase 3：差异化能力
1. 因果节点管理
2. 因果边管理
3. 权重手工编辑
4. 因果历史记录
5. 图谱展示

## Phase 4：AI能力
1. Provider 配置
2. 续写
3. 改写
4. 润色
5. Prompt 模板

## Phase 5：RAG能力
1. 知识文档
2. chunk
3. embedding
4. query
5. 自动入库

## Phase 6：工程化
1. Docker 化
2. 一键脚本
3. README
4. 默认数据
5. 日志与健康检查

---

# 21. AI / opencode 开发约束（非常关键）

## AI MUST
1. 输出完整代码，不要伪代码。
2. 优先修改已有结构，不要乱建平级目录。
3. 任何新增模块必须符合本文档目录结构。
4. 前端页面必须基于 Berry 模板风格延展。
5. 后端接口命名必须统一 `/api/**`。
6. 数据库表名统一小写下划线。
7. 所有 AI 输出必须先落草稿，再人工确认。
8. 因果手工锁定值不得被 AI 自动覆盖。
9. RAG 入库必须可追踪状态。

## AI MUST NOT
1. 不得把前后端拆成两个独立仓库。
2. 不得把数据库换成 MongoDB。
3. 不得随意换前端模板。
4. 不得跳过登录系统。
5. 不得把 RAG 写成纯内存玩具实现。
6. 不得把因果功能简化成一个备注字段。
7. 不得把版本历史省略掉。
8. 不得把 AI Provider 写死成单一厂商。

## AI SHOULD
1. 优先保证主链路可跑通。
2. 先做 CRUD，再接 AI，再接 RAG。
3. 所有重要地方加注释，但不要废话注释。
4. 提供默认样例数据。
5. 提供脚本和 README，做到拉下来就能跑。

---

# 22. 建议的配置文件内容

## 22.1 后端开发环境配置要点

- server.port=8080
- datasource 指向 `192.168.5.249:3306/story_weaver`
- redis 默认 `127.0.0.1:6379`
- jwt.secret 提供本地默认值
- file storage 可先走本地或 MinIO

---

## 22.2 前端开发环境配置要点

- VITE_API_BASE_URL=http://localhost:8080/api

---

# 23. 数据库连接配置（直接按此实现）

后端 `application-dev.yml` 必须体现以下配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://192.168.5.249:3306/story_weaver?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: "Zhouwenjian:2871"
```

---

# 24. MVP 验收标准

满足以下条件，视为 MVP 达成：

1. 可以登录后台。
2. 可以创建小说项目。
3. 可以管理人物、剧情、章节、大纲。
4. 可以进入写作中心编辑正文。
5. 可以配置至少一个 AI Provider。
6. 可以执行一次 AI 续写并生成草稿。
7. 可以确认草稿并写入正式章节。
8. 可以自动把确认后的章节写入 RAG。
9. 可以管理因果节点与边。
10. 可以手动修改因果权重并查看历史。
11. 前后端在一个项目根目录下。
12. 支持一键启动。

---

# 25. 后续增强方向（非 MVP）

1. 因果图谱交互拖拽编辑
2. 角色关系图自动生成
3. 多项目跨世界设定复用
4. 章节对比视图
5. Prompt 调试台
6. 自动摘要与设定冲突检测
7. RAG 检索效果评估
8. 多模型路由策略
9. 本地模型 / 云模型混合调用
10. 发布到小说平台辅助工具

---

# 26. 交付物清单（AI 最终应该产出的东西）

1. 完整根目录工程
2. front 可运行
3. backend 可运行
4. SQL 初始化脚本
5. README
6. Docker Compose
7. start-dev.bat / start-dev.sh
8. 默认管理员
9. 至少一套示例数据
10. 基础页面全部可访问
11. 登录后完整菜单可见
12. 主链路可演示：项目 -> 章节 -> AI 续写 -> 确认 -> RAG 入库

---

# 27. 最终实施结论

本项目不是普通“AI 调接口写小说”小工具，而是：

- 以 **小说项目** 为核心单位
- 以 **外置设定** 为约束
- 以 **因果管理** 为差异化中枢
- 以 **RAG 记忆回流** 为持续增强能力
- 以 **前后端分离后台系统** 为落地形态

最终要实现的是一个可长期迭代的创作平台，而不是一次性脚本。

---

# 28. 对 opencode 的直接执行指令（可放在文档末尾）

你现在要基于本文档，从零开始创建一个名为 `story-weaver` 的完整项目，要求：

1. 根目录为单仓结构，包含 `front` 与 `backend`。
2. 前端基于 Berry Free Vuetify Vue Admin Template 改造。
3. 后端使用 Spring Boot + MySQL。
4. 数据库连接使用本文档中的固定配置。
5. 必须支持登录、项目、章节、人物、剧情、因果、写作、RAG、AI Provider。
6. 必须提供初始化 SQL、README、Docker Compose、一键启动脚本。
7. 必须优先保证主链路跑通，不允许只生成演示假代码。
8. 每一阶段都输出完整可运行文件，不要输出伪代码，不要省略关键文件。
