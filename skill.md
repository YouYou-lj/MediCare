# MediCare 项目 Skill 使用规范

> 适用范围：MediCare 后续所有开发、重构、测试、接口设计、数据库设计、UI 优化和代码审查。  
> 核心要求：后续开发必须结合 `plan.md` 和 `step.md`，并按任务类型使用对应 skill，不允许跳过相关 skill 直接开发。

---

## 1. 总体原则

1. 所有开发任务先阅读 `plan.md`，确认当前功能属于哪个模块和优化方向。
2. 所有执行任务再阅读 `step.md`，确认开发顺序、前置依赖和完成标准。
3. 开发前必须选择对应 skill；一个任务涉及多个领域时，按顺序组合使用多个 skill。
4. 每次开发都要保持功能连贯性：先保证现有系统可运行，再做 UI、接口、数据库或 AI 扩展。
5. 每个阶段完成后都要做基本验证，不能只写代码不验证。

---

## 2. 已安装 Skill

当前项目后续开发应使用以下 skill：

| Skill | 用途 |
|---|---|
| `planning-with-files-zh` | 阅读项目文件、拆分任务、维护开发计划、跟踪阶段进度 |
| `frontend-design` | UI 美化、布局设计、视觉层次、交互体验、动效设计 |
| `vue-frontend` | Vue 3、Vite、Element Plus、Pinia、Router、Axios 前端开发 |
| `springboot-backend` | Spring Boot、Controller、Service、Repository、权限、事务后端开发 |
| `api-contract` | REST API 设计、请求响应格式、接口契约、前后端联调约定 |
| `database-design` | MySQL 表设计、字段、索引、迁移脚本、向量库和 AI 数据表设计 |
| `code-review` | 代码审查、质量检查、风险识别、测试缺口检查 |
| `medical-agent` | AI Agent 工具调用开发规范：Tool 接口、意图识别、权限控制、只读约束 |
| `record-assist` | 病历辅助功能开发规范：AI 病历建议生成、插入交互、安全边界 |
| `drug-risk-check` | 用药风险检查功能开发规范：过敏检查、库存检查、剂量检查规则 |
| `recommendation` | 个性化推荐功能开发规范：规则引擎、角色差异化、推荐卡片 UI |
| `behavior-analysis` | 用户行为分析功能开发规范：埋点事件、上报策略、分析图表 |

---

## 3. Skill 使用顺序

### 3.1 任何开发任务

必须先使用：

1. `planning-with-files-zh`

使用目的：

1. 读取 `plan.md` 和 `step.md`。
2. 判断当前开发属于第几步。
3. 明确本次改动范围。
4. 避免破坏已有业务流程。

适用任务：

1. 新功能开发。
2. UI 改造。
3. 后端接口开发。
4. 数据库调整。
5. AI 功能开发。
6. 测试、部署、文档更新。

---

### 3.2 UI 和交互设计任务

必须使用：

1. `planning-with-files-zh`
2. `frontend-design`
3. `vue-frontend`

适用范围：

1. 登录页美化。
2. 主布局改造。
3. 首页仪表盘升级。
4. 患者、挂号、病历、药品、处方页面 UI 统一。
5. 动画、过渡、图表、状态标签、空状态、弹窗体验优化。

开发要求：

1. 先用 `frontend-design` 明确视觉风格、布局层次和交互体验。
2. 再用 `vue-frontend` 实现 Vue 组件、页面逻辑和 Element Plus 组件。
3. 必须保持当前角色权限、路由和原有业务操作不变。
4. 页面改完后至少验证登录、菜单、当前页面核心操作。

对应 `step.md`：

1. 第 2 步：统一前端主题和公共组件。
2. 第 3 步：改造主布局和登录页。
3. 第 4 步：升级首页仪表盘。
4. 第 5 步：统一基础业务页面 UI。

---

### 3.3 Vue 前端功能任务

必须使用：

1. `planning-with-files-zh`
2. `vue-frontend`
3. `api-contract`

适用范围：

1. 新增前端页面。
2. 新增 API 请求文件。
3. 改造 Pinia 状态。
4. 修改路由和权限菜单。
5. 对接后端接口。
6. AI 助手前端面板、知识库页面、行为分析页面。

开发要求：

1. 先用 `api-contract` 明确接口路径、请求参数、响应结构。
2. 再用 `vue-frontend` 编写 API 封装、类型定义、页面组件。
3. 所有接口统一使用 `medicare-web/src/api/` 下的 Axios 实例。
4. 类型定义优先补充到 `medicare-web/src/types/index.ts`。
5. 前端错误提示要清晰，不能让页面崩溃。

对应文件：

```text
medicare-web/src/api/
medicare-web/src/types/index.ts
medicare-web/src/router/index.ts
medicare-web/src/stores/user.ts
medicare-web/src/views/
medicare-web/src/components/
```

---

### 3.4 Spring Boot 后端任务

必须使用：

1. `planning-with-files-zh`
2. `springboot-backend`
3. `api-contract`

适用范围：

1. 新增 Controller。
2. 新增 Service。
3. 新增 Repository。
4. 修改认证、权限、拦截器和 AOP。
5. 增加 AI 后端接口。
6. 增加 Dashboard 统计接口。
7. 增加行为分析接口。

开发要求：

1. 先用 `api-contract` 定义接口契约。
2. 再用 `springboot-backend` 实现 Controller、Service、Repository。
3. 响应格式必须统一使用 `Result<T>`。
4. 权限必须复用 `@RequireRole` 和现有登录 Session。
5. 涉及多表写入、库存扣减、处方状态变更时必须使用事务。
6. 不允许让 AI 接口绕过现有权限规则。

对应文件：

```text
medicare-server/src/main/java/com/medicare/controller/
medicare-server/src/main/java/com/medicare/service/
medicare-server/src/main/java/com/medicare/repository/
medicare-server/src/main/java/com/medicare/dto/
medicare-server/src/main/java/com/medicare/auth/
```

---

### 3.5 API 契约和前后端联调任务

必须使用：

1. `planning-with-files-zh`
2. `api-contract`
3. `vue-frontend`
4. `springboot-backend`

适用范围：

1. 新增 REST API。
2. 修改请求参数。
3. 修改返回结构。
4. 前后端接口联调。
5. Swagger/OpenAPI 文档。
6. AI 相关接口设计。

开发要求：

1. 先写清楚接口路径、方法、权限、请求体、响应体。
2. 再分别实现后端和前端封装。
3. 接口返回必须是：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

4. 修改接口时必须同步更新前端类型和调用方。
5. 联调完成后要跑核心流程或对应测试脚本。

---

### 3.6 数据库和向量库任务

必须使用：

1. `planning-with-files-zh`
2. `database-design`
3. `springboot-backend`
4. `api-contract`

适用范围：

1. 新增 MySQL 表。
2. 修改字段、索引、约束。
3. 新增 AI 对话表。
4. 新增知识库表。
5. 新增推荐表。
6. 新增行为日志表。
7. 设计 Chroma、Qdrant、Milvus 等向量库集合。

开发要求：

1. 先用 `database-design` 设计表结构和索引。
2. 再用 `springboot-backend` 实现 Entity、Repository、Service。
3. 数据库脚本必须放在 `sql/` 或后端迁移目录。
4. 不能破坏现有业务表和历史数据。
5. 涉及敏感数据时要减少冗余存储。

对应 `step.md`：

1. 第 8 步：保存 AI 对话历史。
2. 第 9 步：建设 RAG 文档知识库。
3. 第 13 步：实现个性化推荐。
4. 第 14 步：实现用户行为分析。

---

### 3.7 AI、RAG、Agent 功能任务

必须使用：

1. `planning-with-files-zh`
2. `api-contract`
3. `database-design`
4. `springboot-backend`
5. `vue-frontend`
6. `code-review`

适用范围：

1. AI 助手。
2. AI 对话历史。
3. RAG 文档检索。
4. 知识库问答。
5. 业务只读 Agent。
6. 病历辅助。
7. 用药和库存风险提醒。
8. 个性化推荐。
9. 用户行为分析。

开发要求：

1. 先确认当前属于 `step.md` 第 7 步到第 14 步的哪一部分。
2. 先设计接口和数据表，再写后端服务，最后写前端入口。
3. AI 第一阶段只读业务数据，不允许直接新增、修改、删除业务数据。
4. AI 输出必须有安全提示：仅供辅助参考，不能替代医生判断。
5. AI 服务异常时不能影响主业务。
6. 所有 AI 工具调用都必须按当前登录角色做权限校验。

---

### 3.8 测试、回归和代码审查任务

必须使用：

1. `planning-with-files-zh`
2. `code-review`

根据任务类型追加：

1. 前端测试追加 `vue-frontend`。
2. 后端测试追加 `springboot-backend`。
3. 接口测试追加 `api-contract`。
4. 数据库测试追加 `database-design`。

适用范围：

1. 功能开发完成后的自检。
2. Pull Request 或提交前检查。
3. 修复 bug 后的回归。
4. AI、RAG、Agent 功能上线前检查。

审查重点：

1. 是否符合 `plan.md` 的目标。
2. 是否符合 `step.md` 的开发顺序。
3. 是否破坏已有核心业务流程。
4. 是否遗漏权限、事务、异常处理。
5. 是否遗漏前端错误提示和 loading 状态。
6. 是否遗漏测试或联调验证。

---

## 4. 按开发阶段绑定 Skill

| 阶段 | 对应 `step.md` | 必用 Skill |
|---|---|---|
| 项目基线确认 | 第 0 步 | `planning-with-files-zh`、`code-review` |
| 工程依赖整理 | 第 1 步 | `planning-with-files-zh`、`springboot-backend`、`vue-frontend` |
| 前端主题和组件 | 第 2 步 | `planning-with-files-zh`、`frontend-design`、`vue-frontend` |
| 主布局和登录页 | 第 3 步 | `planning-with-files-zh`、`frontend-design`、`vue-frontend` |
| 首页仪表盘 | 第 4 步 | `planning-with-files-zh`、`frontend-design`、`vue-frontend`、`springboot-backend`、`api-contract` |
| 业务页面 UI | 第 5 步 | `planning-with-files-zh`、`frontend-design`、`vue-frontend` |
| 接口文档和测试 | 第 6 步 | `planning-with-files-zh`、`api-contract`、`springboot-backend`、`code-review` |
| AI 基础对话 | 第 7 步 | `planning-with-files-zh`、`api-contract`、`springboot-backend`、`vue-frontend` |
| AI 对话历史 | 第 8 步 | `planning-with-files-zh`、`database-design`、`api-contract`、`springboot-backend`、`vue-frontend` |
| RAG 知识库 | 第 9 步 | `planning-with-files-zh`、`database-design`、`api-contract`、`springboot-backend` |
| 业务只读 Agent | 第 10 步 | `planning-with-files-zh`、`medical-agent`、`api-contract`、`springboot-backend`、`code-review` |
| 病历辅助 | 第 11 步 | `planning-with-files-zh`、`record-assist`、`api-contract`、`springboot-backend`、`vue-frontend`、`code-review` |
| 用药风险提醒 | 第 12 步 | `planning-with-files-zh`、`drug-risk-check`、`database-design`、`api-contract`、`springboot-backend`、`vue-frontend`、`code-review` |
| 个性化推荐 | 第 13 步 | `planning-with-files-zh`、`recommendation`、`database-design`、`api-contract`、`springboot-backend`、`vue-frontend` |
| 用户行为分析 | 第 14 步 | `planning-with-files-zh`、`behavior-analysis`、`database-design`、`api-contract`、`springboot-backend`、`vue-frontend` |
| 全流程联调 | 第 15 步 | `planning-with-files-zh`、`code-review`、`api-contract` |
| 部署演示 | 第 16 步 | `planning-with-files-zh`、`code-review` |

---

## 5. 开发前检查清单

每次开发前必须确认：

1. 本次任务属于 `step.md` 的哪一步。
2. 本次任务涉及哪些文件。
3. 本次任务必须使用哪些 skill。
4. 是否需要先设计 API。
5. 是否需要先设计数据库。
6. 是否会影响登录、权限、库存、处方、病历等核心流程。

---

## 6. 开发后验证清单

每次开发后必须确认：

1. 前端能否正常启动或构建。
2. 后端能否正常启动。
3. 本次修改页面是否可访问。
4. 本次修改接口是否返回统一 `Result<T>`。
5. 当前角色权限是否正确。
6. 核心业务流程是否未被破坏。
7. 如涉及 AI，AI 服务失败时主业务是否仍可用。
8. 如涉及数据库，脚本是否可重复执行或有明确迁移顺序。

---

## 7. 推送前要求

推送前必须使用：

1. `code-review`

并检查：

1. `.vscode/` 不推送。
2. `DOC/` 不推送。
3. `node_modules/`、`dist/`、`target/`、`logs/` 不推送。
4. 遵守 `push-requirements.md`。
5. `git status --short` 中只包含本次开发相关文件。

---

## 8. 强制约定

1. 不允许跳过 `planning-with-files-zh` 直接修改代码。
2. 不允许 UI 任务跳过 `frontend-design`。
3. 不允许 Vue 任务跳过 `vue-frontend`。
4. 不允许后端任务跳过 `springboot-backend`。
5. 不允许接口任务跳过 `api-contract`。
6. 不允许数据库任务跳过 `database-design`。
7. 不允许提交前跳过 `code-review`。
8. 涉及多个领域时必须组合使用多个 skill。

该文件作为 MediCare 后续开发的项目级技能使用规范，后续所有任务都应先对照本文件，再进入实际开发。
