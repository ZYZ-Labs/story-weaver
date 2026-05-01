# REQ-20260425 导航懒加载 404 与章节工作区模型路由错配修复

- Req ID: `REQ-20260425-navigation-chunk404-and-model-routing-fix`
- Created At: 2026-04-25 Asia/Shanghai
- Status: In Progress

## 背景

- 当前前端在创作台之外的菜单切换时，用户会遇到 `js 404`。
- 当前章节工作区镜头初稿使用的模型，与旧写作中心使用的模型配置不一致。

## 目标

- 修复菜单切换时的懒加载脚本 404，避免部署切版后进入“首屏能打开、其他菜单全部炸 chunk”的状态。
- 修复章节工作区镜头初稿的模型路由，使其与旧写作中心统一使用 `writing_ai_provider_id / writing_ai_model`。

## 非目标

- 不重做整套前端发布架构。
- 不改章节工作区镜头初稿的业务提示词内容。
- 不回退或覆盖前一条 AI timeout / SSE 错误传递缺陷的修复。

## 验收标准

- 切版后若浏览器拿到旧入口脚本，前端能够在首次 chunk 失配时自动刷新恢复，不再长期停留在菜单点击 `js 404`。
- Nginx 对 `index.html` 不做强缓存，降低旧入口脚本引用旧 chunk hash 的概率。
- 章节工作区 `phase8.chapter-workspace.scene-draft` 入口解析到 `writing_ai_*` 配置，而不是落回 `default_ai_*`。
- 本地构建与目标单测通过。
