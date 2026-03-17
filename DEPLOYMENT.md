# Story Weaver 部署指南

## 环境要求

### 必需软件
1. **Java 21+** - 后端运行环境
2. **Node.js 20+** - 前端运行环境
3. **MySQL 8.0+** - 数据库（必需，默认端口 `3306`）
4. **Maven 3.8+** - Java 项目管理
5. **Git** - 版本控制

### 可选软件
1. **Redis** - 缓存服务（可选，默认端口 `6379`，当前版本未强依赖）
2. **Docker** - 容器化部署（未来版本）

## 快速开始

### Windows 用户
1. 运行 `init-db.bat` 初始化数据库
2. 运行 `start-dev.bat` 启动前后端服务
3. 访问 http://localhost:5173

### Linux/macOS 用户
1. 执行数据库初始化：
   ```bash
   mysql -u root -p < backend/src/main/resources/schema.sql
   ```
2. 启动后端：
   ```bash
   cd backend
   mvn spring-boot:run
   ```
3. 启动前端：
   ```bash
   cd front/vuetify-admin
   npm install
   npm run dev
   ```

## 详细步骤

### 1. 环境检查
运行测试脚本检查环境：
```bash
# Windows
.\test-start.ps1

# 或使用 PowerShell
powershell -ExecutionPolicy Bypass -File test-start.ps1
```

### 2. 数据库初始化
```bash
# 创建数据库和表结构
mysql -u root -p < backend/src/main/resources/schema.sql

# 默认账户
# - admin / admin123
# - testuser / admin123
```

### 3. 后端启动
先复制配置模板并按本地环境修改账号密码与 JWT 密钥：
```bash
cd backend/src/main/resources
cp application-example.yml application.yml
```

```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

后端服务将在 http://localhost:8080 启动

### 4. 前端启动
```bash
cd front/vuetify-admin
npm install        # 首次运行需要安装依赖
npm run dev
```

前端服务将在 http://localhost:5173 启动

### 5. 访问应用
1. 打开浏览器访问 http://localhost:5173
2. 使用默认账户登录：
   - 用户名：admin
   - 密码：admin123

## 配置文件

### 后端配置
- 示例文件：`backend/src/main/resources/application-example.yml`
- 本地文件：`backend/src/main/resources/application.yml`（由示例文件复制并自行填写）

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/story_weaver?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: your_mysql_username
    password: your_mysql_password
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      host: localhost
      port: 6379

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto

jwt:
  secret: change-this-to-a-32-plus-char-jwt-secret
  expiration: 86400000  # 24小时
```

### 前端配置 (front/vuetify-admin/.env)
```env
VITE_API_URL=http://localhost:8080/api
```

## 故障排除

### 常见问题

#### 1. 后端启动失败
**问题**: `mvn spring-boot:run` 失败
**解决**:
- 检查 Java 版本：`java -version`
- 检查 Maven 安装：`mvn -v`
- 清理并重新编译：`mvn clean compile`

#### 2. 数据库连接失败
**问题**: `Cannot create connection to database server`
**解决**:
- 检查 MySQL 服务是否启动
- 检查数据库配置是否正确
- 确认数据库用户权限

#### 3. 前端启动失败
**问题**: `npm run dev` 失败
**解决**:
- 检查 Node.js 版本：`node -v`
- 清理并重新安装依赖：`rm -rf node_modules && npm install`
- 检查端口占用：`netstat -ano | findstr :5173`

#### 4. 登录失败
**问题**: 无法使用 admin/admin123 登录
**解决**:
- 确认数据库已正确初始化
- 检查后端日志中的错误信息
- 验证 JWT 配置

### 日志查看

#### 后端日志
```bash
cd backend
tail -f target/logs/application.log  # Linux/macOS
# 或查看控制台输出
```

#### 前端日志
在浏览器开发者工具中查看 Console 和 Network 标签

## 生产部署

### Docker 部署（未来版本）
```bash
# 构建镜像
docker build -t story-weaver-backend -f backend/Dockerfile .
docker build -t story-weaver-frontend -f front/Dockerfile .

# 运行容器
docker-compose up -d
```

### 传统部署
1. 后端打包：
   ```bash
   cd backend
   mvn clean package -DskipTests
   java -jar target/story-weaver-backend-1.0.0.jar
   ```

2. 前端构建：
   ```bash
   cd front/vuetify-admin
   npm run build
   # 将 dist 目录部署到 Web 服务器
   ```

## 开发指南

### 项目结构
```
story-weaver/
├── front/                    # 前端项目
│   └── vuetify-admin/       # Vuetify 管理模板
├── backend/                 # 后端项目
│   ├── src/main/java/com/storyweaver/
│   │   ├── config/         # 配置类
│   │   ├── controller/     # 控制器
│   │   ├── domain/         # 领域模型
│   │   ├── repository/     # 数据访问层
│   │   ├── service/        # 服务层
│   │   └── utils/          # 工具类
│   └── src/main/resources/ # 资源文件
└── docs/                   # 文档
```

### API 文档
启动后端后访问：http://localhost:8080/api/swagger-ui.html

### 代码规范
- 后端：遵循 Spring Boot 最佳实践
- 前端：遵循 Vue 3 组合式 API 规范
- 数据库：使用 MySQL 8.0+ 特性

## 联系方式

如有问题，请参考：
1. 项目文档：README.md
2. API 文档：Swagger UI
3. 问题反馈：GitHub Issues

---

**注意**: 生产环境请务必修改默认密码和 JWT 密钥！