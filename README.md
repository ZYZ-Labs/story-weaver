# Story Weaver - AI 小说创作平台

一个基于 AI 的小说创作平台，支持章节续写、外置因果管理、自动入库 RAG 和可视化写作后台。

## 项目结构

```
story-weaver/
├── front/                    # 前端项目
│   └── vuetify-admin/       # Vuetify 管理模板（子模块）
├── backend/                 # 后端项目
│   ├── src/main/java/com/storyweaver/
│   │   ├── config/         # 配置类
│   │   ├── controller/     # 控制器
│   │   ├── domain/         # 领域模型
│   │   ├── repository/     # 数据访问层
│   │   ├── service/        # 服务层
│   │   └── utils/          # 工具类
│   └── src/main/resources/ # 资源文件
└── story-weaver-opencode-spec.md  # 项目规范文档
```

## 技术栈

### 前端
- Vue 3 + Vite
- Vuetify 3
- Vue Router
- Pinia
- Axios

### 后端
- Spring Boot 3.x
- Java 21
- MySQL 8.x
- MyBatis Plus
- Spring Security + JWT
- Redis

## 快速开始

### 环境要求
- JDK 21+
- Node.js 20+
- MySQL 8.x
- Redis

### 数据库初始化
1. 启动 MySQL 服务
2. 执行 SQL 脚本：
```bash
mysql -u root -p < backend/src/main/resources/schema.sql
```

### 后端启动
```bash
cd backend
mvn spring-boot:run
```

### 前端启动
```bash
cd front/vuetify-admin
npm install
npm run dev
```

## 默认账户
- 用户名：admin
- 密码：admin123

## API 接口

### 认证接口
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/register` - 用户注册

### 项目管理接口
- `GET /api/projects` - 获取用户项目列表
- `POST /api/projects` - 创建新项目
- `PUT /api/projects/{id}` - 更新项目
- `DELETE /api/projects/{id}` - 删除项目

## 开发计划

### Phase 1 (已完成)
- [x] 项目基础架构搭建
- [x] 登录注册功能
- [x] 项目管理 CRUD
- [x] 数据库设计
- [x] 后端基础模块

### Phase 2 (规划中)
- 前端页面开发
- 章节管理功能
- 角色管理功能
- 世界设定管理

### Phase 3 (规划中)
- AI 续写功能
- RAG 记忆库
- 因果关系管理
- 可视化编辑器

## 许可证

MIT License