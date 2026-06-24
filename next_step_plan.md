# MediCare 下一步开发计划

> 更新日期：2026-06-24
> 当前目标：完成 AI 助手“强制检索 + 引用展示 + 来源预览”闭环，并继续向业务 Agent 扩展。

---

## 当前待修复问题（高优先级）

### 1. 左侧“检索”栏未显示检索到的文件
**现象**：AI 助手回答时，左侧“检索”标签页中没有列出对应的检索来源文件，仅在消息正文里可能出现 `[引用N]` 标记。

**可能原因**：
- 前端 `AiChatPanel.vue` 中 `currentReferences` 计算属性依赖最后一条 assistant 消息的 `references`，但消息对象在创建时未初始化 `references` 字段，Vue 响应式可能未正确追踪。
- SSE `references` 事件到达时，`assistantMessage.references = refs` 赋值可能未触发视图更新。
- 自动切换 `activeSideTab = 'references'` 的逻辑可能未生效或被后续状态覆盖。
- 后端 `RagService.retrieve()` 返回的 chunks 实际为空（向量相似度过滤后无结果），导致 `references` 为空列表。

**建议排查步骤**：
1. 在浏览器 DevTools 中查看 SSE 响应，确认是否收到 `event: references` 及其 data 内容。
2. 在 `AiChatPanel.vue` 的 `onReferences` 回调中打印 `refs`，确认赋值成功。
3. 检查 `currentReferences` computed 是否正确响应 `messages` 变化。
4. 后端日志中查看 `RagService.retrieve()` 实际返回的 chunk 数量与相似度分数。

### 2. 引用来源预览弹窗体验不佳
**现象**：预览弹窗当前以整段文本方式展示，内容超出时是整个弹窗/页面滚动，而不是在固定大小的窗口内部滚动。

**期望**：
- 弹窗大小固定，整体高度小于当前浏览器视口高度（例如视口的 70%~80%）。
- 标题栏和底部按钮固定，仅中间内容区域可上下滚动。
- 内容区域不使用整段文本的连续滑动，而是有固定可视区域 + 滚动条。

**相关文件**：
- `medicare-web/src/components/AiChatPanel.vue` 中的 `el-dialog` 与 `.ref-preview-dialog` 样式。

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

---

## 下一步建议

### 近期（可立即继续）

1. **修复左侧检索栏不显示问题**
   - 优先验证 SSE `references` 事件是否到达前端。
   - 若事件到达但 UI 未更新，检查 Vue 响应式与 `currentReferences` 计算属性。
   - 若事件未到达或为空，检查后端的向量检索逻辑与相似度阈值。

2. **优化预览弹窗交互**
   - 将弹窗改为固定高度，内部内容区独立滚动。
   - 可考虑使用 `el-drawer` 替代 `el-dialog`，或在 `el-dialog` 中通过 flex 布局固定头部/底部。

3. **验证 RAG 检索效果**
   - 在系统已有知识库文档（`DOC/`、`plan.md`、`step.md` 等）的前提下，向 AI 助手提问文档相关问题。
   - 确认回答下方出现 `[1][2]` 引用标签，左侧“检索” tab 同步显示来源。

### 中期

4. **接入业务数据只读工具（第 10 步）**
   - 实现 `PatientTool`、`MedicalRecordTool`、`MedicineTool`、`PrescriptionTool`、`RegistrationTool`。
   - AI 助手根据问题自动判断：文档问题走 RAG，业务问题走 Tool，普通问题走普通对话。

5. **病历辅助与用药提醒（第 11、12 步）**
   - 医生工作站病历区域增加 AI 辅助侧栏。
   - 处方开立区域增加“AI 用药检查”按钮。

6. **个性化推荐与行为分析（第 13、14 步）**
   - 首页按角色展示推荐卡片。
   - 记录用户操作路径，生成行为分析汇总。

---

## 相关文件

```text
medicare-server/src/main/java/com/medicare/service/RagService.java
medicare-server/src/main/java/com/medicare/service/AiAssistantService.java
medicare-server/src/main/java/com/medicare/controller/KnowledgeController.java
medicare-web/src/api/ai.ts
medicare-web/src/api/knowledge.ts
medicare-web/src/components/AiChatPanel.vue
```
