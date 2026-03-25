# Story Weaver Deployment Guide

本文档描述当前项目的镜像发布与服务端部署方式，目标是：

- 本地构建前后端镜像
- 推送到选定的私有镜像仓库
- 在服务端通过 `docker compose` 拉取镜像并启动

## 一、当前发布方案

项目当前区分两类编排文件：

- `docker-compose.yml`  
  用于本地容器化开发

- `docker-compose.server.yml`  
  用于服务端部署镜像

对应的发布脚本：

- Windows: `scripts/deploy.bat` / `scripts/deploy.ps1`
- Linux / macOS: `scripts/deploy.sh`

当前脚本支持三类镜像仓库：

- Docker Hub
- 阿里云 ACR
- 自定义 Docker Registry

当前仓库已经预设为你的阿里云 ACR：

- `REGISTRY_HOST=crpi-2iicgf8z27uyvaq1.cn-hangzhou.personal.cr.aliyuncs.com`
- `LOGIN_SERVER=crpi-2iicgf8z27uyvaq1.cn-hangzhou.personal.cr.aliyuncs.com`
- `REGISTRY_NAMESPACE=silvericekey`
- `LOGIN_USERNAME=your-registry-login-username`
- `BACKEND_IMAGE_NAME=story-weaver-backend`
- `FRONTEND_IMAGE_NAME=story-weaver-front`

## 二、发布前准备

本地机器需要：

- Docker 可用
- 可以登录目标镜像仓库
- 已在目标仓库中创建前后端私有仓库或命名空间

建议的仓库名：

- `story-weaver-backend`
- `story-weaver-front`

## 三、本地发布到镜像仓库

### Windows

首次运行：

```powershell
.\scripts\deploy.bat
```

指定标签：

```powershell
.\scripts\deploy.bat -Tag v1.0.0
```

重新配置：

```powershell
.\scripts\deploy.bat -Reconfigure
```

跳过 `docker login`：

```powershell
.\scripts\deploy.bat -SkipLogin
```

### Linux / macOS

首次运行：

```bash
chmod +x scripts/deploy.sh
./scripts/deploy.sh
```

指定标签：

```bash
./scripts/deploy.sh --tag=v1.0.0
```

重新配置：

```bash
./scripts/deploy.sh --reconfigure
```

跳过 `docker login`：

```bash
./scripts/deploy.sh --skip-login
```

## 四、交互式配置内容

首次执行时，脚本会交互式询问并保存这些配置：

- 镜像仓库类型
- 镜像仓库地址
- 登录服务地址
- 登录用户名
- 仓库命名空间
- 后端仓库名
- 前端仓库名
- 镜像标签

配置文件位置：

```text
.deploy/registry.env
```

示例文件：

```text
.deploy/registry.env.example
```

如果你直接沿用当前这套阿里云仓库，通常只需要改镜像标签，不需要再手填 host、namespace 和用户名。

旧版本如果已经存在：

```text
.deploy/dockerhub.env
```

脚本会自动兼容并在下次保存时迁移到新配置文件。

另外，脚本在发布完成后还会额外打印一份 Dockge 可用的 `compose.yaml` 模板。

说明：

- 其中 `volumes` 里的 `xxx/...` 是本地路径占位符
- 你需要把 `xxx` 替换成服务器上的真实目录
- 替换完成后可以直接粘贴到 Dockge

### 交互式选择示例

脚本会提示选择：

```text
1. Docker Hub
2. Alibaba Cloud ACR
3. Custom registry
```

其中：

- Docker Hub 会生成类似 `namespace/repository:tag` 的镜像地址
- 阿里云 ACR 会生成类似 `crpi-2iicgf8z27uyvaq1.cn-hangzhou.personal.cr.aliyuncs.com/namespace/repository:tag` 的镜像地址
- 自定义 Registry 会根据你输入的 host 和 namespace 生成镜像地址

### 阿里云 ACR 说明

如果你选择阿里云 ACR，脚本会要求输入：

- ACR registry host  
  默认：`crpi-2iicgf8z27uyvaq1.cn-hangzhou.personal.cr.aliyuncs.com`

- ACR login server  
  默认与 registry host 相同

- ACR namespace  
  默认：`silvericekey`

- ACR login username  
  默认：`your-registry-login-username`

脚本不会保存密码，`docker login` 时由 Docker 自己进行交互输入。

你也可以手动先登录一次：

```bash
docker login --username=your-registry-login-username crpi-2iicgf8z27uyvaq1.cn-hangzhou.personal.cr.aliyuncs.com
```

## 五、服务端部署

### 1. 准备文件

把下面两个文件带到服务端：

- `docker-compose.server.yml`
- `.env.server.example`

然后复制环境文件：

```bash
cp .env.server.example .env
```

### 2. 修改 `.env`

至少需要确认这些配置：

```env
BACKEND_IMAGE=your-registry/story-weaver-backend:latest
FRONTEND_IMAGE=your-registry/story-weaver-front:latest

BACKEND_PORT=8080
FRONTEND_PORT=80
TZ=Asia/Shanghai

SPRING_DATASOURCE_URL=jdbc:mysql://192.168.5.249:3306/story_weaver?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your-local-password

SPRING_DATA_REDIS_HOST=192.168.5.249
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=your-local-password

JWT_SECRET=change-this-to-a-long-random-jwt-secret-at-least-32-characters
APP_CORS_ALLOWED_ORIGIN_PATTERNS=http://localhost:*,http://127.0.0.1:*,http://192.168.*:*,http://10.*:*
```

建议你至少修改：

- `BACKEND_IMAGE`
- `FRONTEND_IMAGE`
- `JWT_SECRET`
- `APP_CORS_ALLOWED_ORIGIN_PATTERNS`

### 3. 启动服务

```bash
docker login
docker compose --env-file .env -f docker-compose.server.yml up -d
```

查看状态：

```bash
docker compose --env-file .env -f docker-compose.server.yml ps
```

查看日志：

```bash
docker compose --env-file .env -f docker-compose.server.yml logs -f
```

停止服务：

```bash
docker compose --env-file .env -f docker-compose.server.yml down
```

拉取新镜像并更新：

```bash
docker compose --env-file .env -f docker-compose.server.yml pull
docker compose --env-file .env -f docker-compose.server.yml up -d
```

## 六、网络与反向代理说明

当前前端生产镜像内置了 Nginx，行为如下：

- 对外提供前端页面
- 将 `/api` 转发到 `backend:8080`
- 对流式正文生成关闭代理缓冲

这意味着：

- 前端应始终使用相对路径 `/api`
- 不需要在浏览器里直接请求 `http://backend:8080`
- 只要访问前端域名或 IP，API 也会随之生效

## 七、CORS 说明

后端现在支持通过 `APP_CORS_ALLOWED_ORIGIN_PATTERNS` 配置允许来源。

常见例子：

```env
APP_CORS_ALLOWED_ORIGIN_PATTERNS=http://localhost:*,http://127.0.0.1:*,http://192.168.*:*,https://your-domain.com
```

如果你前端和后端都走同一个 Nginx 域名，通常不会再遇到浏览器跨域问题。

## 八、数据库初始化与迁移

全新库：

```bash
mysql -u root -p < sql/001_init_database.sql
mysql -u root -p < sql/002_seed_data.sql
```

旧开发库升级：

```bash
mysql -u root -p < sql/003_align_legacy_dev_schema.sql
mysql -u root -p < sql/004_world_setting_and_naming_config.sql
mysql -u root -p < sql/005_world_setting_association_and_character_attributes.sql
```

默认账号：

- `admin / Admin@123456`
- `author / Admin@123456`
- `testuser / Admin@123456`

## 九、常见问题

### 1. 发布脚本提示找不到 docker

说明当前机器没有安装 Docker，或者 Docker 没有加入 PATH。  
先确认 `docker version` 能执行，再运行发布脚本。

### 2. 局域网访问前端时出现跨域或请求失败

优先检查：

- 前端是否使用了相对 `/api`
- 服务端 `.env` 中的 `APP_CORS_ALLOWED_ORIGIN_PATTERNS` 是否包含你的来源
- 是否通过前端 Nginx 暴露页面，而不是浏览器直接跨域请求后端

### 3. 正文流式生成最后不结束

当前版本已经处理了这类问题，但如果线上仍出现，优先确认：

- 已部署的是最新前端镜像
- 前端请求的是 `/api/ai-writing/generate-stream`
- 使用的是当前仓库内置的生产 Nginx 配置

### 4. 镜像仓库拉取失败

检查：

- 服务端是否执行过 `docker login`
- 镜像地址和标签是否正确
- 目标仓库是否已创建且当前账号有权限
- 如果是阿里云 ACR，确认登录地址、命名空间和账号信息都正确
