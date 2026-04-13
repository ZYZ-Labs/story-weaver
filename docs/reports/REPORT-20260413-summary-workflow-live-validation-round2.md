# Summary Workflow 线上联调报告（Round 2）

- Date: 2026-04-13 Asia/Shanghai
- Scope:
  - Redis proposal store 修复验证
  - `proposalId` 跨重启恢复验证
- Project:
  - `projectId=28`
  - `旧日王座`
- Validation Target:
  - `chapterId=31`
  - `退役者的邀请函`

## 结论

- Redis proposal store 的线上问题已定位并修复。
- 根因不是代码链路本身，而是部署环境文件中的 Redis 密码错误。
- 修复后，`summary-workflow` 已达到：
  - proposal 落 Redis
  - 后端重启后仍可用同一个 `proposalId` 继续 `preview / apply`
  - apply 后 proposal key 被正常删除

## 根因定位

当前线上 stack 实际使用：

- compose:
  - `/usr/local/project/docker/portainer/data/compose/18/docker-compose.yml`
- env_file:
  - `/usr/local/project/docker/env/story-weaver/backend.env`

问题点：

- 该 env 文件中的 `SPRING_DATA_REDIS_PASSWORD` 配置为旧值：
  - `JCsy422YaqGCqzZr/5K/RJoX`
- 实际 Redis `192.168.5.249:6379` 正确密码为：
  - `Zhouwenjian:2871`

直接验证结果：

- 使用旧密码：
  - `WRONGPASS`
- 使用新密码：
  - `PONG`

## 修复动作

1. 备份原部署环境文件：
   - `/usr/local/project/docker/env/story-weaver/backend.env.bak.codexfix-20260413-redis`
2. 修正部署环境文件：
   - `/usr/local/project/docker/env/story-weaver/backend.env`
3. 仅重建 `story-weaver-backend`：
   - 使用现网 compose project `story-weaver` 原地重建

## 修复后验证

### 1. Redis proposal key 成功写入

- 创建 proposal 后，已在 Redis 中查到：
  - `story:summary-proposal:cfca66bc-ab7e-4748-b53f-10ead0279715`

### 2. 后端重启后 preview 成功

- 重建 `story-weaver-backend`
- 使用同一个 `proposalId`
- `/api/summary-workflow/previews` 返回 `HTTP 200`

### 3. 后端重启后 apply 成功

- 使用同一个 `proposalId`
- `/api/summary-workflow/apply` 返回 `HTTP 200`
- 章节摘要保持原值，无额外数据污染

### 4. proposal key 删除成功

- apply 后查询 Redis：
  - `EXISTS story:summary-proposal:cfca66bc-ab7e-4748-b53f-10ead0279715 -> 0`
  - `TTL -> -2`

### 5. 后端日志确认

- 修复后创建/预览/写回链路中，不再出现：
  - `Redis unavailable when saving summary proposal ...`
  - `Redis unavailable when loading summary proposal ...`
  - `Redis unavailable when deleting summary proposal ...`

## 当前判断

- `Phase 3.2` 后端链路可视为完成
- Redis proposal store 已从“内存回退可用”升级为“真实 Redis 可用”
- 下一步可以把重点切到 `Phase 3.3` 的前端摘要入口

## 贡献与署名说明

- 平台方向、Summary First 路线与联调目标：
  - 由用户提出与主导
- 本轮问题定位、部署环境修复、线上回归验证与文档撰写：
  - 由 Codex 完成
