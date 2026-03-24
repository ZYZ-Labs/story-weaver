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
- MySQL 8.x（**必需**，固定地址 `192.168.5.249:3306`）
- Redis（**必需**，固定地址 `192.168.5.249:6379`）
- 数据库账号：`root`
- 数据库密码：`your-local-password`
- Redis 密码：`your-local-password`

### 一键启动

Windows:

```bat
start-dev.bat
```

Linux/macOS:

```bash
chmod +x start-dev.sh
./start-dev.sh
```

默认演示账号：

- `admin / Admin@123456`
- `author / Admin@123456`
- `testuser / Admin@123456`

启动脚本会自动做这些事：

- 检查 `JDK 21+`、`Maven`、`Node.js`、`npm`
- 若检测到 `nvm`，尝试切换到根目录 `.nvmrc` 中的 Node 版本
- 检查 `192.168.5.249` 上的 MySQL / Redis 端口与凭据
- 安装前端依赖并启动前后端

### 手动启动

```bash
cd backend
mvn spring-boot:run
```

```bash
cd front
npm install
npm run dev
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
- `GET /api/projects/{projectId}/plotlines`
- `POST /api/projects/{projectId}/plotlines`
- `GET /api/plotlines/{id}`
- `PUT /api/plotlines/{id}`
- `DELETE /api/plotlines/{id}`
- `GET /api/projects/{projectId}/causalities`
- `POST /api/projects/{projectId}/causalities`
- `GET /api/causalities/{id}`
- `PUT /api/causalities/{id}`
- `DELETE /api/causalities/{id}`
- `GET /api/providers`
- `POST /api/providers`
- `GET /api/providers/{id}`
- `PUT /api/providers/{id}`
- `DELETE /api/providers/{id}`
- `POST /api/providers/{id}/test`
- `GET /api/projects/{projectId}/knowledge/documents`
- `POST /api/projects/{projectId}/knowledge/documents`
- `GET /api/knowledge/documents/{id}`
- `PUT /api/knowledge/documents/{id}`
- `DELETE /api/knowledge/documents/{id}`
- `POST /api/projects/{projectId}/rag/query`
- `POST /api/projects/{projectId}/rag/reindex`
