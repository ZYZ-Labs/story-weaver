# Story Weaver 前端重建 Phase 0 任务清单

> 目标：在 `front/` 目录完成可运行、可扩展的 Vue3 + Vite + Vuetify 工程基线，支撑后续业务开发（Phase 1+）。

## 0. 里程碑定义

- **M0（基础可运行）**：本地可 `npm install && npm run dev`，页面可访问。
- **M1（基础架构就绪）**：路由、状态管理、请求层、布局、环境变量可用。
- **M2（工程规范就绪）**：代码规范、目录规范、基础组件和错误处理可复用。

---

## 1. 任务分解（WBS）

## Epic A：工程初始化与依赖基线

### A-1 创建前端工程骨架
- [ ] 使用 Vite 初始化 Vue3 + TypeScript 项目。
- [ ] 安装并启用 Vuetify 3。
- [ ] 安装 Vue Router、Pinia、Axios。
- [ ] 安装基础开发依赖（ESLint、Prettier、TypeScript 类型支持）。

**验收标准**
- `front/package.json` 存在并可正常执行脚本。
- `front/src/main.ts` 能正确挂载 Vuetify、Router、Pinia。

---

### A-2 标准目录结构对齐规范
- [ ] 创建目录：
  - `src/api`
  - `src/assets`
  - `src/components`
  - `src/layouts`
  - `src/router`
  - `src/stores`
  - `src/utils`
  - `src/views/login`
  - `src/views/dashboard`
  - `src/views/project`
  - `src/views/chapter`
  - `src/views/character`
  - `src/views/plot`
  - `src/views/causality`
  - `src/views/writing`
  - `src/views/rag`
  - `src/views/provider`
  - `src/views/settings`
  - `src/views/system`
- [ ] 提供每个模块最小入口页（可先占位）。

**验收标准**
- 目录结构与 README / 规范文档一致。
- 路由可导航至所有占位页且不报错。

---

## Epic B：运行配置与环境变量

### B-1 环境变量管理
- [ ] 新建 `.env.example`（至少包含 `VITE_API_BASE_URL`）。
- [ ] 新建 `.env.development` 与 `.env.production`。
- [ ] 在 `src/utils/env.ts` 提供读取封装，避免散落使用 `import.meta.env`。

**验收标准**
- 开发环境默认指向 `http://localhost:8080/api`。
- 构建时可通过环境变量覆盖 API 地址。

---

### B-2 Vite 代理与构建配置
- [ ] 在 `vite.config.ts` 增加 `server.proxy`（可选，便于本地联调）。
- [ ] 配置别名 `@ -> src`。
- [ ] 配置基础构建输出目录及 sourcemap 策略。

**验收标准**
- 本地开发无需改代码即可切换代理/直连模式。
- `@/` 导入路径可稳定工作。

---

## Epic C：应用骨架（Layout + Router + Store）

### C-1 全局布局与导航框架
- [ ] 创建 `MainLayout.vue`：左侧菜单 + 顶栏 + 内容区。
- [ ] 创建 `AuthLayout.vue`：用于登录/注册页面。
- [ ] 提供菜单配置文件（后续可按权限动态生成）。

**验收标准**
- 未登录仅可访问认证页。
- 登录后进入主布局并可切换占位页面。

---

### C-2 路由系统
- [ ] 初始化静态路由表（登录、仪表盘、项目、章节等）。
- [ ] 增加 `beforeEach` 路由守卫（基于 token 判断）。
- [ ] 增加 404 路由。

**验收标准**
- 无 token 访问业务页会跳转登录页。
- 已登录访问 `/login` 自动跳转仪表盘。

---

### C-3 状态管理
- [ ] 创建 `stores/auth.ts`（token、用户信息、登录态判断、登出方法）。
- [ ] 创建 `stores/app.ts`（全局 loading、主题、当前项目上下文预留字段）。
- [ ] 增加持久化策略（localStorage，MVP 即可）。

**验收标准**
- 刷新后 token 不丢失。
- 登出后清除认证信息并跳转登录。

---

## Epic D：网络层与错误处理

### D-1 Axios 统一封装
- [ ] 创建 `src/api/http.ts`。
- [ ] 请求拦截：自动注入 `Authorization: Bearer <token>`。
- [ ] 响应拦截：统一处理业务错误和 HTTP 错误。

**验收标准**
- 所有 API 调用统一从 `http.ts` 发起。
- 401 时可自动清空登录态并跳转登录。

---

### D-2 API 模块分层
- [ ] 创建 API 文件：
  - `api/auth.ts`
  - `api/project.ts`
  - `api/chapter.ts`
  - `api/character.ts`
  - `api/world-setting.ts`
  - `api/ai-writing.ts`
- [ ] 以函数形式对齐 README 接口列表。

**验收标准**
- API 命名统一，参数与返回结构可追踪。
- 页面不直接写裸 Axios 请求。

---

## Epic E：通用 UI 与工程质量

### E-1 通用页面组件
- [ ] `PageContainer`（标题、描述、操作区、内容区）。
- [ ] `EmptyState`（暂无数据）。
- [ ] `ConfirmDialog`（删除确认）。

**验收标准**
- 占位页均使用统一容器组件。
- 删除类操作统一确认弹框行为。

---

### E-2 开发质量门禁
- [ ] 配置 `npm run lint`。
- [ ] 配置 `npm run type-check`。
- [ ] 配置 `npm run build`。

**验收标准**
- 三个命令均可执行（首次可允许少量 warning，需记录）。

---

## 2. 任务执行顺序（建议）

1. A-1 → A-2（工程与目录）
2. B-1 → B-2（环境与构建）
3. C-1 → C-2 → C-3（骨架能力）
4. D-1 → D-2（请求层）
5. E-1 → E-2（通用组件与质量）

---

## 3. 风险与规避

- **风险 1：Vuetify 与 Vite/TS 配置冲突**
  - 规避：优先使用官方推荐插件与最小可运行模板。
- **风险 2：后端未完全就绪导致前端联调阻塞**
  - 规避：先完成 mock/占位数据适配，API 层不阻塞页面开发。
- **风险 3：目录结构偏离规范导致后续维护成本增加**
  - 规避：以本任务清单为准进行目录验收。

---

## 4. Definition of Done（Phase 0 完成标准）

- [ ] `front/` 工程可一键安装依赖并启动。
- [ ] 路由 + 布局 + 状态管理 + 请求层全部具备。
- [ ] 与后端 API 基础通信能力具备。
- [ ] 代码质量命令（lint / type-check / build）可运行。
- [ ] 目录结构与规范文档一致，可进入 Phase 1。

---

## 5. 建议命令清单（执行检查）

```bash
cd front
npm install
npm run dev
npm run lint
npm run type-check
npm run build
```
