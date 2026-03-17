# Story Weaver - AI 小说创作平台

一个基于 AI 的小说创作平台，支持章节续写、因果管理、自动入库 RAG 和可视化写作后台。

## 项目结构

```text
story-weaver/
├── front/                    # Vue3 + Vite + Vuetify 前端
│   ├── src/
│   │   ├── router/          # 路由配置
│   │   ├── stores/          # Pinia 状态管理
│   │   └── views/           # MVP 页面
│   └── .env.example         # 前端环境变量示例
├── backend/                  # Spring Boot 后端
└── sql/                      # 数据库初始化脚本
```

## 快速开始

### 前置要求

- JDK 21+
- Node.js 20+
- MySQL 8.x
- Redis

### 后端启动

```bash
cd backend
mvn spring-boot:run
```

### 前端启动

```bash
cd front
npm install
npm run dev
```

### 前端环境变量

复制示例文件并按需修改：

```bash
cd front
cp .env.example .env
```

默认配置：

```env
VITE_API_BASE_URL=http://localhost:8080/api
```


## 后端 API 列表

以下接口统一使用 `/api` 前缀：

- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/projects`
- `POST /api/projects`
- `PUT /api/projects/{id}`
- `DELETE /api/projects/{id}`
- `GET /api/projects/{projectId}/chapters`
- `POST /api/projects/{projectId}/chapters`
- `PUT /api/projects/{projectId}/chapters/{chapterId}`
- `DELETE /api/projects/{projectId}/chapters/{chapterId}`
- `GET /api/projects/{projectId}/chapters/{chapterId}`
- `GET /api/projects/{projectId}/characters`
- `POST /api/projects/{projectId}/characters`
- `PUT /api/projects/{projectId}/characters/{characterId}`
- `DELETE /api/projects/{projectId}/characters/{characterId}`
- `GET /api/projects/{projectId}/characters/{characterId}`
- `POST /api/ai-writing/generate`
- `GET /api/ai-writing/chapter/{chapterId}`
- `GET /api/ai-writing/{id}`
- `POST /api/ai-writing/{id}/accept`
- `POST /api/ai-writing/{id}/reject`
- `GET /api/world-settings/project/{projectId}`
- `GET /api/world-settings/{id}`
- `POST /api/world-settings`
- `PUT /api/world-settings/{id}`
- `DELETE /api/world-settings/{id}`

