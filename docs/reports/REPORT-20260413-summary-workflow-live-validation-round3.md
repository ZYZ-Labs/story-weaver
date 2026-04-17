# Summary Workflow 线上验收报告 Round 3

- Date: 2026-04-13 Asia/Shanghai
- Scope:
  - `summary-workflow` 线上复验
  - Redis proposal store 回归确认
  - `Phase 3.3` 前端入口部署可见性确认

## 背景

在第二轮联调中，`summary-workflow` 已验证：

- `proposal / preview / apply` 可用
- proposal key 可落 Redis
- 后端重启后 `proposalId` 可继续 `preview / apply`

但本轮用户重新部署后，线上 backend 容器再次出现：

- `Redis unavailable when saving summary proposal`

说明部署态与此前修复态不一致，需要重新核对现网 env 与容器运行参数。

## 本轮结论

### 1. 根因确认

线上实际运行容器 `story-weaver-backend` 的环境变量仍为旧 Redis 密码：

- `SPRING_DATA_REDIS_PASSWORD=JCsy422YaqGCqzZr/5K/RJoX`

现网 compose 仍引用：

- `/usr/local/project/docker/portainer/data/compose/18/docker-compose.yml`
- `/usr/local/project/docker/env/story-weaver/backend.env`

而该 `backend.env` 在本轮复验前确实还是旧值。

### 2. 已完成修复

已完成以下动作：

- 备份现网 env：
  - `/usr/local/project/docker/env/story-weaver/backend.env.bak.codexfix-20260413-redis`
- 修正：
  - `/usr/local/project/docker/env/story-weaver/backend.env`
- 将 `SPRING_DATA_REDIS_PASSWORD` 更新为正确值：
  - `Zhouwenjian:2871`
- 使用现网 compose project 原地重建：
  - `story-weaver-backend`

### 3. 修复后验证结果

修复后已验证：

- 容器环境变量已更新为正确密码
- `POST /api/summary-workflow/proposals` 再次返回 `200`
- proposal id:
  - `25e040ff-5fed-4ea9-aa8e-d8f0df30fa9a`
- Redis 中已真实存在 key：
  - `story:summary-proposal:25e040ff-5fed-4ea9-aa8e-d8f0df30fa9a`
- 最新 backend 日志中未再出现新的：
  - `Redis unavailable when saving summary proposal`

结论：

- 当前 `summary-workflow` proposal store 已恢复为真实 Redis 可用态
- 本轮线上 backend 不再依赖内存回退保存 proposal

## 前端部署验收

本轮同时确认前端构建产物已经包含更明显的摘要入口文案。

已在部署产物中确认可见字符串：

- `摘要优先编辑`
- `摘要优先`
- `打开摘要工作流`
- `章节现在支持“摘要优先编辑”`

涉及页面：

- `CharacterListView`
- `WorldSettingView`
- `ChapterListView`

这说明：

- `Phase 3.3` 的前端入口代码已进入线上静态资源
- 用户此前“看不出来”的问题，当前版本已经至少在构建产物层面被修正

## 当前判断

- `Phase 3.2`：完成，并已再次复验
- `Phase 3.3`：进行中，已完成部署级入口验收

当前最合理的下一步不是继续修 Redis，而是：

1. 继续完成 `Phase 3.3` 的页面级排版与交互收口
2. 评估是否进入 `Phase 4` 的只读 `MCP / State Server` 基础设施

## 备注

- 本轮修复的是现网部署参数，不是业务代码缺陷
- 若后续再次出现 Redis proposal store 回退，优先检查：
  - 当前 compose project
  - 当前 `backend.env`
  - backend 容器内实际生效环境变量
