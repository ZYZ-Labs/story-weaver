# PLAN-REQ-20260425 导航懒加载 404 与章节工作区模型路由错配修复

- Req ID: `REQ-20260425-navigation-chunk404-and-model-routing-fix`
- Version: `v1`
- Updated At: 2026-04-25 Asia/Shanghai

## 实施步骤

1. 确认导航 `js 404` 的根因属于 Vite 懒加载 chunk 与发布切版后的资源失配，而不是菜单路由表本身错误。
2. 同时在客户端与 Nginx 层修复：
   - 客户端捕获 `vite:preloadError` 与动态 import 失败，执行单次自动刷新恢复。
   - Nginx 对 `index.html` 关闭缓存，降低旧入口脚本持续驻留。
3. 确认章节工作区镜头初稿入口 `phase8.chapter-workspace.scene-draft` 的模型路由当前错误落回默认模型。
4. 修复 `AIModelRoutingService` 映射规则，使章节工作区镜头初稿复用 `writing_ai_*` 配置。
5. 运行本地构建与目标单测，记录回归结论。
