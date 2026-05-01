# 导航懒加载 404 与章节工作区模型路由错配修复进度

- Req ID: `REQ-20260425-navigation-chunk404-and-model-routing-fix`
- Updated At: 2026-04-25 Asia/Shanghai
- Status: In Verification

## 当前状态

- 菜单点击 `js 404` 的根因已确认并修复。
- 章节工作区镜头初稿模型路由错配已确认并修复。
- 本地构建与目标单测已通过，待用户环境回归。

## 最近关键结论

- 证据 1：
  - 菜单路由表与懒加载页面定义本身无明显错配。
  - 当前构建产物使用 Vite hash chunk，创作台首屏会先加载工作台 chunk，其它菜单在点击时才加载对应 chunk。
- 结论 1：
  - 用户所见“只有创作台正常，其他菜单点开就是 js 404”符合“浏览器仍持有旧入口脚本，点击其它菜单时去拉旧 chunk hash”这一类切版失配症状。
- 证据 2：
  - `nginx/nginx.conf` 之前对 `js/css` 使用强缓存，但没有对 `index.html` 显式关闭缓存。
- 结论 2：
  - 需要同时做入口页面缓存治理和前端单次自动恢复，不能只改其中一侧。
- 证据 3：
  - `front/src/views/chapter/ChapterWorkspaceView.vue` 使用 `entryPoint = phase8.chapter-workspace.scene-draft`。
  - `backend/src/main/java/com/storyweaver/service/AIModelRoutingService.java` 之前只把 `writing-center / writing_center / writing` 映射到 `writing_ai_*`，导致章节工作区镜头初稿落回 `default_ai_*`。
- 结论 3：
  - 章节工作区与旧写作中心模型不一致，不是前端表单问题，而是后端入口映射缺失。
- 证据 4：
  - `front/src/views/writing/WritingView.vue` 仍支持手动选择 `selectedProviderId / selectedModel`，并把该偏好持久化到浏览器本地。
- 结论 4：
  - 本轮修复的是“默认模型路由错配”。
  - 如果旧写作中心被用户手动切到其它模型，它仍然可以与章节工作区不同；这属于旧页保留的显式覆盖能力，不是这次缺陷。

## 已完成动作

- 前端：
  - `front/src/main.ts`
  - 捕获 `vite:preloadError` 与路由动态 import 错误，针对 chunk 失配执行单次自动刷新恢复。
- 部署：
  - `nginx/nginx.conf`
  - 对 `index.html` 显式关闭缓存，降低旧入口脚本长期驻留。
- 后端：
  - `backend/src/main/java/com/storyweaver/service/AIModelRoutingService.java`
  - 将章节工作区镜头初稿入口映射到 `writing_ai_provider_id / writing_ai_model`。
- 测试：
  - 新增 `backend/src/test/java/com/storyweaver/service/AIModelRoutingServiceTest.java`
  - `mvn -pl backend -am -Dmaven.repo.local=/usr/local/project/github/story-weaver/.cache/m2 -Dtest=AIModelRoutingServiceTest,AIProviderServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - `npm run build`

## 下一步动作

- 让用户环境回归：
  - 验证切版后首次点击非创作台菜单是否仍出现 `js 404`
  - 验证章节工作区当前镜头生成使用的是否已与旧写作中心一致

## 阻塞项

- 当前无代码阻塞
- 仍需用户环境确认发布后的真实行为
