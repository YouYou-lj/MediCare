# MediCare 下一步开发计划

> 更新日期：2026-06-25
> 当前目标：AI 助手“强制检索 + 引用展示 + 来源预览”闭环已完成，下一步可继续向业务 Agent 扩展。

---

## 本次已修复问题（高优先级）

### 1. 左侧“检索”栏未显示检索到的文件（已修复）
**现象**：AI 助手回答时，左侧“检索”标签页中没有列出对应的检索来源文件，仅在消息正文里可能出现 `[引用N]` 标记。

**修复结果**：
- `AiChatPanel.vue` 新增 `appendMessage()` / `updateMessage()`，流式内容、`references`、`meta` 均通过响应式 `messages` 数组替换消息对象，确保 Vue 能稳定触发视图更新。
- `currentReferences` 改为从消息列表尾部查找最后一条 assistant 消息，避免不必要的数组拷贝，并稳定读取最新引用。
- 发送完成后的会话列表刷新改为 `loadSessions({ shouldAutoSelect: false })`，不再自动选中会话并重载历史消息，避免刚收到的 `references` 被无引用的历史消息覆盖。
- `references` 或 `done.references` 到达时会更新当前 assistant 消息并自动切换到“检索”标签页。
- 修复流式回复在 2 秒打字动画启动前就完成时可能不显示正文的缓冲区问题。

### 2. 引用来源预览弹窗体验不佳（已修复）
**现象**：预览弹窗当前以整段文本方式展示，内容超出时是整个弹窗/页面滚动，而不是在固定大小的窗口内部滚动。

**修复结果**：
- `el-dialog` 宽度改为响应式 `min(700px, calc(100vw - 32px))`，避免小屏溢出。
- `.ref-preview-dialog` 改为固定高度 flex 布局，整体高度控制在视口内。
- 标题栏和底部按钮固定，中间 `.ref-preview-content` 独立滚动。
- 弹窗样式改为 `:global(.ref-preview-dialog ...)`，解决 Element Plus Dialog 默认 teleport 到 `body` 后 scoped 样式不生效的问题。

---

## 已完成（本次迭代）

### 1. AI 助手强制前置检索
- 后端 `AiAssistantService.chat` / `streamChat` 在调用大模型前，先调用 `RagService.retrieveReferences()` 检索知识库。
- 检索结果作为上下文写入 system prompt，并要求模型在引用内容后标注 `[引用N]`。
- 非流式与流式接口均把 `references` 返回给前端。

### 2. 流式对话支持 `references` 事件
- SSE 新增 `event: references`，在回答生成前推送检索来源。
- `event: done` 元数据中也携带 `references`，确保前端最终状态一致。
- 新增 `event: references_error`，检索失败时前端可收到明确提示。

### 3. 前端引用展示与交互
- `AiChatPanel.vue` 中：
  - AI 回复内的 `[引用N]` 被渲染为可点击上标，且仅对有效引用编号生成点击标签。
  - 每条 assistant 消息下方展示“来源：[1] [2] …”链接。
  - 左侧“检索”标签页 / 折叠角标实时显示当前回答的引用数量。
  - 收到引用后自动切换到“检索”标签页。
  - 点击来源弹出只读预览弹窗，调用 `/api/knowledge/documents/{id}/preview` 查看原文（已放开为登录用户可访问）。
- 新会话切换后左侧检索记录自动清空/更新。

### 4. 引用清理与防编造
- 系统提示词严格区分“有检索片段”和“无检索片段”：
  - 无检索片段时禁止输出任何 `[引用N]`。
  - 有检索片段时必须在事实后标注有效 `[引用N]`。
- 后端非流式回答与保存历史记录前会清理无效引用标记。

### 5. 前后端启动验证
- 后端 `mvn compile` 通过，`spring-boot:run` 已在 `8080` 启动。
- 前端 `vue-tsc --noEmit` 通过，开发服务 `5173` 已启动。
- 2026-06-25 前端 `npm run build` 通过，确认 `AiChatPanel.vue` 的引用响应式修复与预览弹窗样式无类型/构建错误。

### 6. AI 检索与生成多线程加速
- 新增 AI 专用线程池配置，生成任务使用 `ai-generation-*` 线程池，检索评分使用 `ai-retrieval-*` 线程池。
- 流式对话仍保持原有 SSE 事件顺序：先推送 `references`，再持续推送 `chunk`，最后推送 `done`。
- RAG 向量检索在读取候选 chunk 后，对 embedding 解析、cosine 相似度与关键词加权进行分批并行评分。
- 线程池大小可通过 `AI_GENERATION_CORE_SIZE`、`AI_GENERATION_MAX_SIZE`、`AI_RETRIEVAL_CORE_SIZE`、`AI_RETRIEVAL_MAX_SIZE` 等环境变量调节。
- 2026-06-25 后端 `mvn compile` 通过，并已重启 `8080` 后端完成登录接口冒烟验证。

---

## 下一步建议

### 近期（可立即继续）

1. **验证 RAG 检索效果**
   - 在系统已有知识库文档（`DOC/`、`plan.md`、`step.md` 等）的前提下，向 AI 助手提问文档相关问题。
   - 确认回答下方出现 `[1][2]` 引用标签，左侧“检索” tab 同步显示来源。
   - 点击左侧检索来源，确认预览弹窗保持固定高度，正文在内容区域内独立滚动。

### 中期

2. **接入业务数据只读工具（第 10 步）**
   - 实现 `PatientTool`、`MedicalRecordTool`、`MedicineTool`、`PrescriptionTool`、`RegistrationTool`。
   - AI 助手根据问题自动判断：文档问题走 RAG，业务问题走 Tool，普通问题走普通对话。

3. **病历辅助与用药提醒（第 11、12 步）**
   - 医生工作站病历区域增加 AI 辅助侧栏。
   - 处方开立区域增加“AI 用药检查”按钮。

4. **个性化推荐与行为分析（第 13、14 步）**
   - 首页按角色展示推荐卡片。
   - 记录用户操作路径，生成行为分析汇总。

---

## 相关文件

```text
medicare-server/src/main/java/com/medicare/service/RagService.java
medicare-server/src/main/java/com/medicare/service/AiAssistantService.java
medicare-server/src/main/java/com/medicare/config/AiTaskExecutorConfig.java
medicare-server/src/main/java/com/medicare/config/AiProperties.java
medicare-server/src/main/java/com/medicare/controller/KnowledgeController.java
medicare-web/src/api/ai.ts
medicare-web/src/api/knowledge.ts
medicare-web/src/components/AiChatPanel.vue
```
