# Summary Workflow 线上验收报告 Round 4

- Date: 2026-04-13 Asia/Shanghai
- Scope:
  - `Phase 3.3` 部署验收
  - 摘要工作流前端最新交互确认
  - `operatorMode / intent` 后端协同确认

## 本轮目标

确认以下内容已经进入线上：

- 三处对象页的“摘要优先”主入口
- 摘要弹层中的：
  - `普通模式 / 专家模式`
  - `精修摘要 / 改写摘要 / 补充细节`
  - 摘要未变更拦截
- `summary-workflow` 继续稳定写入 Redis proposal store

## 验收结果

### 1. 前端构建产物确认

已在 `story-weaver-front` 部署产物中确认以下字符串存在：

- `普通模式`
- `专家模式`
- `精修摘要`
- `改写摘要`
- `补充细节`
- `摘要优先编辑`
- `打开摘要工作流`

说明：

- `SummaryWorkflowDialog` 最新交互代码已进入线上静态资源
- `Character / WorldSetting / Chapter` 三处入口也已进入线上版本

### 2. 后端协同确认

已使用线上 backend 实际调用：

- `POST /api/summary-workflow/proposals`

请求携带：

- `operatorMode=EXPERT`
- `intent=REFINE`

返回结果：

- `HTTP 200`
- 成功生成 proposal 与 preview
- 说明前后端在 `operatorMode / intent` 上已经打通

### 3. Redis proposal store 确认

已确认 proposal：

- `d5142466-5248-4ff2-b583-300f874feeb7`

对应 Redis key 已存在：

- `story:summary-proposal:d5142466-5248-4ff2-b583-300f874feeb7`

同时未发现新的日志：

- `Redis unavailable when saving summary proposal`

结论：

- proposal 仍然真实落在 Redis 中
- 当前 backend 没有回退到内存态

## 阶段判断

本轮验收后，当前判断为：

- `Phase 3.2`：完成
- `Phase 3.3`：完成
- `Phase 3`：完成

下一阶段应切换到：

- `Phase 4. Read-Only MCP 与 LSP 基础设施`

## 备注

- 本轮未做浏览器自动化点击脚本，但已完成：
  - 部署产物确认
  - 后端接口真实调用
  - Redis proposal key 验证
- 作为当前阶段收口依据，这一轮证据已足够
