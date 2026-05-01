# PLAN-REQ-20260427 状态驱动叙事 Runtime 架构升级

- Req ID: `REQ-20260427-state-driven-narrative-runtime-upgrade`
- Version: `v1`
- Updated At: 2026-04-27 Asia/Shanghai

## 实施步骤

1. 明确职责重划：
   - `scene` 降级为渲染批次
   - `node` 成为推进单元
   - `Causality` 降级为 authoring 元数据
   - checkpoint 成为撤回和分支的第一真源
2. 新建独立架构文档，写清：
   - 现状问题
   - 目标 runtime 模型
   - 与现有 Story State 的映射
   - 渐进迁移策略
3. 在 `storyunit` 落最小 runtime 对象和 store：
   - `StoryActionIntent`
   - `StoryResolvedTurn`
   - `StoryNodeCheckpoint`
   - `StoryOpenLoop`
4. 扩展 `ResilientStoryStateStore`：
   - Redis key / manifest
   - 内存 fallback
   - 最小读写测试
5. 为章节工作区设计 node mode 主链协议：
   - 节点骨架生成
   - 推荐动作 / 自定义动作提交
   - resolver 结算
   - narrator 渲染
   - checkpoint 回档
6. 设计兼容边界：
   - scene mode 继续可用
   - 新章节可切 node mode
   - 当前 `REQ-20260425` 的多 scene 修补链保留为兼容基线，不继续承担架构升级主线
7. 第二轮落 controller / service / 前端章节工作区最小接入：
   - 新增 `node-preview / node-actions/resolve`
   - 章节工作区展示 node runtime 预览
   - 先用兼容开关把 `node resolve` 默认关掉，避免和 scene mode 混写
8. 第三轮落 chapter-level mode 切换：
   - 章节级 `scene / node` mode 持久化
   - 后端统一保护 scene / node 两条真相链，避免混写
   - 章节工作区按 mode 切换主交互，而不是只灰按钮
9. 第四轮再继续：
   - narrator render 接入 node runtime
   - checkpoint 回档与正文渲染重试拆分
   - 评估 mode migration 是否需要专门的数据迁移入口
