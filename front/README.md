# Story Weaver Frontend

前端基于 Vue 3 + Vite + Vuetify，包含 MVP 页面骨架与基础路由。

## 启动方式

```bash
cd front
npm install
npm run dev
```

## 目录结构

```text
front/
├── src/
│   ├── main.ts
│   ├── App.vue
│   ├── plugins/
│   │   └── vuetify.ts
│   ├── router/
│   │   └── index.ts
│   ├── services/
│   │   └── api.ts
│   ├── stores/
│   │   └── app.ts
│   └── views/
│       ├── LoginView.vue
│       ├── ProjectListView.vue
│       ├── ChapterManagementView.vue
│       ├── CharacterManagementView.vue
│       ├── WorldSettingView.vue
│       └── AIWritingView.vue
├── .env.example
└── package.json
```

## MVP 页面

- 登录 `/login`
- 项目列表 `/projects`
- 章节管理 `/chapters`
- 角色管理 `/characters`
- 世界设定 `/world`
- AI 写作入口 `/ai-writing`

## 环境变量

复制 `.env.example` 为 `.env`：

```bash
cp .env.example .env
```

- `VITE_API_BASE_URL`：后端 API 地址（由 `src/services/api.ts` 统一读取）
