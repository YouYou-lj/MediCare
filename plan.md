# MediCare 智慧医疗门诊管理系统后续优化实施方案

> 版本：v3.0  
> 更新日期：2026-06-22  
> 适用范围：当前 `MediCare` 项目代码、`DOC/` 文档、`Java企业级项目综合实训实施方案_前后端分离版 .docx`  
> 目标：在现有前后端分离项目基础上完成 UI 现代化升级，并增加基于 LangChain / RAG / Agent 的智能医疗辅助能力。

---

## 1. 项目现状

### 1.1 当前代码结构

```text
MediCare/
├── medicare-web/                 # Vue 3 + Vite + Element Plus 前端
│   ├── src/api/                  # Axios API 封装
│   ├── src/router/index.ts       # 路由与角色权限
│   ├── src/stores/user.ts        # 登录用户状态
│   ├── src/types/index.ts        # 前端类型定义
│   └── src/views/                # 登录、首页、患者、挂号、医生工作站等页面
├── medicare-server/              # Spring Boot 3 + JPA 后端
│   ├── src/main/java/com/medicare/
│   │   ├── auth/                 # Session 登录、角色注解、AOP 权限校验
│   │   ├── controller/           # REST API 控制器
│   │   ├── service/              # 业务服务
│   │   ├── repository/           # Spring Data JPA Repository
│   │   ├── entity/               # JPA 实体
│   │   ├── dto/                  # DTO / VO / Result
│   │   └── exception/            # 全局异常处理
│   └── src/main/resources/
│       ├── application.yml       # MySQL 连接、JPA 配置
│       └── db/migration_v2.sql
├── sql/medicare.sql              # 完整数据库脚本与示例数据
├── DOC/                          # SRS、API、部署手册、教学任务书
├── scripts/test_functional.py    # 功能测试脚本
└── plan.md                       # 本实施方案
```

### 1.2 已实现业务模块

| 模块 | 前端页面 | 后端接口 | 当前能力 |
|---|---|---|---|
| 登录认证 | `views/login/LoginView.vue` | `/api/auth` | Session + Cookie 登录、退出、当前用户 |
| 首页仪表盘 | `views/layout/DashboardView.vue` | `/api/dashboard/stats` | 今日挂号、候诊、库存预警统计 |
| 患者管理 | `views/patient/PatientList.vue` | `/api/patients` | 患者 CRUD、关键词搜索 |
| 基础数据 | `views/basic-data/BasicDataView.vue` | `/api/departments`、`/api/doctors`、`/api/schedules` | 科室、医生、排班维护 |
| 挂号预约 | `views/registration/RegistrationView.vue` | `/api/registrations` | 现场挂号、叫号、完成、取消 |
| 医生工作站 | `views/doctor/WorkstationView.vue` | 挂号、病历、处方相关接口 | 候诊列表、病历书写、处方开立 |
| 病历管理 | `views/medical-record/RecordList.vue` | `/api/medical-records` | 病历查询、新增、修改 |
| 药品库存 | `views/pharmacy/MedicineList.vue` | `/api/medicines` | 药品 CRUD、入库、出库、低库存预警 |
| 处方管理 | `views/prescription/PrescriptionView.vue` | `/api/prescriptions` | 处方创建、取药、作废、库存日志 |
| 系统设置 | `views/settings/SettingsView.vue` | `/api/users` | 用户管理、修改密码 |

### 1.3 主要优化方向

1. 当前界面功能完整，但视觉风格偏基础后台模板，需要统一主题、布局、交互动效和数据展示层次。
2. 当前系统已具备患者、病历、处方、库存等业务数据，适合增加 AI 助手、RAG 文档问答、病历辅助、用药提醒、推荐和行为分析。
3. 当前后端依赖主要通过 `libs/` 手动维护，后续如引入 LangChain4j、向量库客户端、OpenAPI、测试依赖，建议优先补齐 `pom.xml` 或 Gradle 构建。
4. 文档要求强调企业级实训、RESTful API、JPA、事务、权限、前后端联调，后续方案要保留这些工程实践要求。

---

## 2. 技术栈与核心依赖

### 2.1 现有技术栈

| 层级 | 当前技术 |
|---|---|
| 前端 | Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router、Axios |
| 后端 | Java 17、Spring Boot 3.2、Spring Web、Spring Data JPA、Spring AOP、Hibernate、HikariCP |
| 数据库 | MySQL 8.0 |
| 认证权限 | Session + Cookie、`AuthInterceptor`、`@RequireRole`、`RoleCheckAspect` |
| 文档与测试 | `DOC/api.html`、`DOC/SRS.html`、`scripts/test_functional.py` |

### 2.2 建议新增依赖

前端新增：

| 依赖 | 用途 |
|---|---|
| `echarts` | 首页趋势图、科室分布、库存走势、行为分析图表 |
| `@vueuse/core` | 主题切换、窗口尺寸、滚动状态、动画触发等工具 |
| `dayjs` | 日期格式化、统计时间区间处理 |
| `markdown-it` | AI 回复 Markdown 渲染 |
| `highlight.js` | AI 回复中的代码或结构化内容高亮 |

后端新增，推荐先迁移到 Maven 后引入：

| 依赖 | 用途 |
|---|---|
| `langchain4j` | Java 侧构建 AI Service、ChatModel、Embedding、RAG 流程 |
| `langchain4j-community` 或对应模型供应商模块 | 接入 OpenAI、DashScope、Ollama 等大模型 |
| `springdoc-openapi-starter-webmvc-ui` | 自动生成 Swagger/OpenAPI 文档 |
| `spring-boot-starter-test` | 单元测试、接口测试 |
| `testcontainers-mysql` | MySQL 集成测试 |
| 向量库客户端 | Milvus、Qdrant、Chroma 或 pgvector 客户端 |

可选 Python AI 微服务：

| 依赖 | 用途 |
|---|---|
| FastAPI | 独立 AI 服务接口 |
| LangChain | Agent、RAG、工具调用 |
| sentence-transformers 或模型供应商 embedding | 文档向量化 |
| Chroma / FAISS | 本地向量库，适合教学和轻量部署 |

说明：如果希望项目保持纯 Java，优先使用 LangChain4j；如果希望快速完成 AI 原型，可新增 `medicare-ai/` Python FastAPI 服务，由 Spring Boot 代理调用。

---

## 3. UI 美化升级方案

### 3.1 设计目标

1. 主题符合“智慧医疗门诊管理系统”：干净、可信、低噪音、高可读性。
2. 从普通后台模板升级为现代医疗工作台：信息密度适中，重点数据突出，操作路径清晰。
3. 支持管理员、医生、药师三类角色的差异化首页和快捷入口。
4. 增加适量动画、过渡、动态数字和图表，提升演示效果，但不影响业务操作效率。

### 3.2 视觉风格

| 项目 | 方案 |
|---|---|
| 主色 | 医疗蓝绿色：`#0F9F8F`、`#1677FF` |
| 辅助色 | 成功绿、预警橙、危险红、信息蓝 |
| 背景 | `#F5F8FB` 浅灰蓝，内容区使用白色与极浅蓝分区 |
| 字体 | 系统字体，标题加粗，表格与表单保持 14px 左右 |
| 圆角 | 卡片 8px，按钮 6px，表格区域 6px |
| 阴影 | 轻阴影，不使用过重投影 |
| 图标 | 继续使用 `@element-plus/icons-vue`，统一菜单、按钮、空状态图标 |

### 3.3 页面级改造

#### 3.3.1 登录页

当前问题：渐变背景偏通用，登录卡片信息少。

优化内容：

1. 背景改为医疗主题视觉：淡蓝网格、门诊业务流线、半透明数据面板。
2. 登录卡片增加系统名称、角色说明、演示账号快捷填充。
3. 增加登录按钮 loading、错误抖动、输入框聚焦高亮。
4. 页面底部增加版本号与实训项目说明。

涉及文件：

```text
medicare-web/src/views/login/LoginView.vue
```

#### 3.3.2 主布局

当前问题：左侧菜单使用默认深色后台样式，页面层次一般。

优化内容：

1. 侧边栏改为医疗蓝绿色主题，增加当前模块高亮条。
2. 顶栏增加面包屑、今日日期、角色标签、消息/AI 助手入口。
3. 内容区增加统一页面容器、标题区、操作区样式。
4. 路由切换增加淡入过渡。
5. 折叠菜单增加平滑动画，移动端支持抽屉菜单。

涉及文件：

```text
medicare-web/src/views/layout/MainLayout.vue
medicare-web/src/router/index.ts
```

#### 3.3.3 首页仪表盘

当前问题：只有 3 个统计卡片和空状态，展示价值不足。

优化内容：

1. 增加角色化工作台：
   - 管理员：首页展示今日挂号、候诊、处方、库存预警、用户数。
   - 医生：首页展示今日待诊、已完成、最近病历、快捷开处方。
   - 药师：首页展示待取药处方、低库存、近效期药品、出入库趋势。
2. 使用 ECharts 展示：
   - 近 7 日挂号趋势。
   - 科室挂号分布。
   - 库存预警药品排行。
3. 增加动态数字组件，统计值从 0 过渡到真实值。
4. 增加“今日待办”列表和“快捷操作”区。

涉及文件：

```text
medicare-web/src/views/layout/DashboardView.vue
medicare-server/src/main/java/com/medicare/controller/DashboardController.java
medicare-server/src/main/java/com/medicare/service/DashboardService.java
```

#### 3.3.4 列表与表单页面

统一优化患者、基础数据、挂号、病历、药品、处方、用户页面：

1. 列表页结构统一为：页面标题区 → 查询过滤区 → 表格区 → 分页/操作区。
2. 搜索栏使用紧凑布局，按钮带图标。
3. 表格增加斑马纹、状态标签、库存预警标签、金额格式化。
4. 弹窗表单统一宽度、字段分组、校验提示、提交 loading。
5. 空数据统一使用医疗主题空状态。
6. 危险操作使用二次确认并展示业务影响，例如“作废处方会回滚库存”。

建议新增公共组件：

```text
medicare-web/src/components/PageHeader.vue
medicare-web/src/components/StatCard.vue
medicare-web/src/components/DataToolbar.vue
medicare-web/src/components/StatusTag.vue
medicare-web/src/components/AnimatedNumber.vue
medicare-web/src/components/EmptyState.vue
medicare-web/src/components/AiAssistantFloat.vue
medicare-web/src/styles/theme.css
medicare-web/src/styles/animations.css
```

### 3.4 动画与动态模块

| 场景 | 动画方案 | 实现方式 |
|---|---|---|
| 路由切换 | 淡入 + 轻微上移 | Vue `<Transition>` + CSS |
| 统计卡片 | 数字滚动 | `requestAnimationFrame` 或 `AnimatedNumber` |
| 表格加载 | 骨架屏 | Element Plus Skeleton |
| 表单弹窗 | 缩放淡入 | Element Plus Dialog 自定义 class |
| AI 助手 | 悬浮按钮呼吸光效、消息面板滑入 | CSS animation |
| 首页图表 | 首次加载渐入 | ECharts animation |

注意：动画只用于反馈和层次表达，不做大面积装饰，不影响医生、药师高频操作。

---

## 4. AI Agent 智能体功能方案

### 4.1 总体目标

在现有门诊管理业务上新增“AI 智能辅助层”，让系统具备以下能力：

1. AI 助手：用户可在任意页面提问，获得业务操作说明、患者摘要、库存提醒等回答。
2. RAG 文档检索：基于 `DOC/` 文档、实训方案 docx、接口文档、部署手册进行知识库问答。
3. 病历辅助：根据患者历史病历、主诉、诊断草稿生成结构化病历建议。
4. 用药提醒：结合患者过敏史、处方明细、药品库存信息提示风险。
5. 个性化推荐：为医生推荐常用诊疗模板，为药师推荐优先处理的低库存或待取药事项。
6. 用户行为分析：记录用户操作路径，生成模块访问、功能使用、异常操作统计。

### 4.2 推荐架构

优先方案：Spring Boot + LangChain4j。

```text
Vue 前端 AI 面板
    ↓ /api/ai/*
Spring Boot AiController
    ↓
AiAssistantService
    ├── RagService              # 文档检索问答
    ├── MedicalAgentService     # 病历、处方、药品工具调用
    ├── RecommendationService   # 推荐
    ├── BehaviorAnalysisService # 行为分析
    └── LangChain4j ChatModel / EmbeddingModel
            ↓
        大模型 API + 向量库
            ↓
        MySQL 业务数据 + DOC 文档知识库
```

可选方案：新增 `medicare-ai/` Python FastAPI + LangChain 服务。

```text
Vue → Spring Boot /api/ai/* → Python FastAPI /agent/*
                         ↘ MySQL 业务数据
Python LangChain → LLM / Embedding / Chroma
```

选择建议：

1. 教学和 Java 企业项目完整性优先：使用 LangChain4j。
2. 快速验证 Agent/RAG 效果优先：使用 Python FastAPI + LangChain，后续再迁移 Java。

### 4.3 AI 模块划分

后端建议新增目录：

```text
medicare-server/src/main/java/com/medicare/ai/
├── controller/
│   └── AiController.java
├── dto/
│   ├── AiChatRequest.java
│   ├── AiChatResponse.java
│   ├── RagQueryRequest.java
│   ├── RecordAssistRequest.java
│   ├── DrugCheckRequest.java
│   └── RecommendationVO.java
├── service/
│   ├── AiAssistantService.java
│   ├── RagService.java
│   ├── MedicalAgentService.java
│   ├── RecordAssistService.java
│   ├── DrugRiskService.java
│   ├── RecommendationService.java
│   └── BehaviorAnalysisService.java
├── tool/
│   ├── PatientTool.java
│   ├── MedicalRecordTool.java
│   ├── MedicineTool.java
│   ├── PrescriptionTool.java
│   └── RegistrationTool.java
└── config/
    ├── AiModelConfig.java
    └── VectorStoreConfig.java
```

前端建议新增：

```text
medicare-web/src/api/ai.ts
medicare-web/src/components/AiAssistantFloat.vue
medicare-web/src/components/AiChatPanel.vue
medicare-web/src/views/ai/KnowledgeBaseView.vue
medicare-web/src/views/ai/BehaviorAnalysisView.vue
```

### 4.4 AI 助手

功能说明：

1. 全局悬浮按钮打开对话面板。
2. 支持自然语言提问，例如：
   - “今天还有多少候诊患者？”
   - “帮我总结这个患者最近 3 次病历。”
   - “这个处方有没有库存或过敏风险？”
   - “如何部署本系统？”
3. 根据当前页面和选中业务对象自动带入上下文，例如患者 ID、病历 ID、处方 ID。

数据来源：

| 数据 | 来源 |
|---|---|
| 当前用户 | `userStore.currentUser`、后端 Session |
| 当前页面 | Vue Router path、query |
| 患者数据 | `PatientRepository` |
| 病历数据 | `MedicalRecordRepository` |
| 药品和处方 | `MedicineRepository`、`PrescriptionRepository` |
| 文档知识 | `DOC/`、docx、API 文档、部署手册 |

接口设计：

```http
POST /api/ai/chat
Content-Type: application/json

{
  "message": "帮我总结患者 1 的病历",
  "sessionId": "front-generated-session-id",
  "context": {
    "route": "/medical-records",
    "patientId": 1,
    "recordId": 3
  }
}
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "answer": "患者近期主要问题为...",
    "references": [
      { "type": "medical_record", "id": 3, "title": "2026-06-17 病历" }
    ],
    "actions": [
      { "label": "查看病历", "type": "route", "target": "/medical-records?patientId=1" }
    ]
  }
}
```

调用流程：

```text
用户提问
→ 前端带上 route、patientId、recordId 等上下文
→ AiController 校验登录和角色
→ AiAssistantService 判断意图
→ 需要业务数据时调用 Tool/Repository
→ 需要文档知识时调用 RagService
→ 组装 Prompt
→ 调用大模型
→ 返回答案、引用、可执行动作
```

### 4.5 RAG 文档检索与知识库问答

知识库内容：

```text
DOC/SRS.html
DOC/api.html
DOC/部署手册.md
DOC/SQL参考.md
DOC/tasks/*.md
Java企业级项目综合实训实施方案_前后端分离版 .docx
plan.md
```

入库流程：

```text
读取文档
→ 清洗 HTML / Markdown / docx
→ 按标题和段落切分 chunk
→ 生成 embedding
→ 写入向量库
→ 保存文档元数据到 MySQL
```

建议 chunk 规则：

| 项目 | 建议 |
|---|---|
| chunk 大小 | 500-900 中文字符 |
| overlap | 80-120 中文字符 |
| metadata | `docName`、`sectionTitle`、`sourcePath`、`updatedAt` |
| 检索数量 | topK = 4 或 6 |
| 重排 | 第一阶段可不做，第二阶段增加 rerank |

向量库方案：

| 方案 | 适用场景 | 建议 |
|---|---|---|
| Chroma | 本地教学、快速启动 | 第一阶段推荐 |
| Qdrant | Docker 部署、性能较好 | 第二阶段推荐 |
| Milvus | 大规模知识库 | 后续扩展 |
| MySQL + JSON 向量 | 不推荐 | 检索效率和生态较弱 |

接口设计：

```http
POST /api/ai/rag/query
{
  "question": "系统有哪些角色权限？",
  "topK": 5
}
```

```http
POST /api/ai/rag/reindex
```

`/api/ai/rag/reindex` 仅允许管理员调用。

### 4.6 病历辅助

功能场景：

1. 医生工作站填写主诉后，AI 生成现病史、体格检查、初步诊断建议。
2. 保存病历前，AI 检查结构是否完整。
3. 根据历史病历生成“患者摘要”。

数据来源：

| 数据 | 表 |
|---|---|
| 患者基础信息、过敏史 | `patient` |
| 当前挂号 | `registration` |
| 历史病历 | `medical_record` |
| 当前医生与科室 | `doctor`、`department` |

接口设计：

```http
POST /api/ai/record/assist
{
  "patientId": 1,
  "registrationId": 6,
  "chiefComplaint": "头痛三天",
  "draft": {
    "presentIllness": "",
    "physicalExam": "",
    "diagnosis": ""
  }
}
```

返回：

```json
{
  "suggestions": {
    "presentIllness": "建议补充疼痛部位、性质、持续时间...",
    "physicalExam": "建议记录血压、神经系统查体...",
    "diagnosis": "可考虑：紧张性头痛、高血压相关头痛..."
  },
  "warnings": [
    "AI 建议仅供医生参考，最终诊断以医生判断为准"
  ]
}
```

前端交互：

1. 在医生工作站病历表单右侧增加“AI 辅助”侧栏。
2. 每个建议字段提供“插入到表单”按钮。
3. 保存病历时不自动覆盖医生输入。

### 4.7 用药提醒

功能场景：

1. 开处方时检查患者过敏史与药品名称、类别、备注的风险。
2. 检查库存是否足够、是否低于安全库存、是否临近有效期。
3. 对重复用药、超常用剂量做提示。

数据来源：

| 数据 | 表 |
|---|---|
| 患者过敏史 | `patient.allergy_info` |
| 药品名称、规格、库存、有效期 | `medicine` |
| 处方明细 | `prescription_item` |
| 历史处方 | `prescription` |

接口设计：

```http
POST /api/ai/drug/check
{
  "patientId": 1,
  "items": [
    {
      "medicineId": 1,
      "quantity": 2,
      "usageText": "每日三次，每次一粒"
    }
  ]
}
```

返回：

```json
{
  "riskLevel": "warning",
  "warnings": [
    "患者记录青霉素过敏，请确认阿莫西林是否适用",
    "该药品开方后库存仍充足"
  ],
  "suggestions": [
    "必要时选择非青霉素类替代药物"
  ]
}
```

前端交互：

1. 处方明细变更后可手动点击“AI 用药检查”。
2. 高风险提示使用红色 Alert，普通提醒使用黄色 Alert。
3. 保存处方前如存在高风险，需要二次确认。

### 4.8 个性化推荐

推荐对象：

| 角色 | 推荐内容 |
|---|---|
| 管理员 | 今日高峰科室、异常取消挂号、库存预警处理优先级 |
| 医生 | 今日优先接诊列表、常用病历模板、患者历史摘要 |
| 药师 | 待取药处方、低库存药品、近效期药品 |

第一阶段可使用规则推荐，第二阶段再接入 AI 生成解释：

```text
规则推荐
→ 根据角色、今日数据、历史操作统计生成推荐列表
→ AI 只负责把推荐理由表达得更自然
```

接口设计：

```http
GET /api/ai/recommendations
```

返回：

```json
{
  "items": [
    {
      "type": "stock",
      "title": "阿莫西林胶囊库存接近安全阈值",
      "reason": "当前库存低于近 7 日平均消耗量的 3 天用量",
      "priority": "high",
      "target": "/pharmacy"
    }
  ]
}
```

### 4.9 用户行为分析

目标：

1. 为系统优化提供依据。
2. 为推荐模块提供用户偏好。
3. 支撑答辩演示中的“数据驱动优化”亮点。

采集内容：

| 事件 | 示例 |
|---|---|
| 页面访问 | `page_view:/patients` |
| 搜索 | `search_patient`、`search_medicine` |
| 业务操作 | `create_registration`、`create_prescription` |
| AI 使用 | `ai_chat`、`rag_query`、`drug_check` |
| 异常 | 接口 4xx / 5xx、权限拒绝 |

数据库新增表：

```sql
CREATE TABLE user_behavior_log (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NULL,
  username VARCHAR(50) NULL,
  role VARCHAR(30) NULL,
  event_type VARCHAR(50) NOT NULL,
  event_name VARCHAR(100) NOT NULL,
  target_type VARCHAR(50) NULL,
  target_id BIGINT NULL,
  route VARCHAR(200) NULL,
  request_id VARCHAR(64) NULL,
  detail_json JSON NULL,
  ip VARCHAR(64) NULL,
  user_agent VARCHAR(500) NULL,
  create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_behavior_user_time (user_id, create_time),
  KEY idx_behavior_event_time (event_name, create_time)
);
```

前端埋点：

1. 路由切换自动上报页面访问。
2. 查询、创建、删除、AI 提问等关键按钮手动上报。
3. 上报失败不影响主业务。

接口设计：

```http
POST /api/behavior/events
GET /api/behavior/summary?days=7
```

---

## 5. 数据库与向量库设计

### 5.1 现有核心业务表

当前 `sql/medicare.sql` 已包含：

```text
department
doctor
schedule
patient
registration
medical_record
medicine
prescription
prescription_item
inventory_log
sys_user
```

这些表继续作为主业务数据源，不进行破坏性重构。

### 5.2 AI 新增 MySQL 表

#### 5.2.1 AI 对话表

```sql
CREATE TABLE ai_chat_session (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  session_key VARCHAR(64) NOT NULL UNIQUE,
  user_id BIGINT UNSIGNED NULL,
  title VARCHAR(200) NULL,
  create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
);

CREATE TABLE ai_chat_message (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT UNSIGNED NOT NULL,
  role VARCHAR(20) NOT NULL,
  content TEXT NOT NULL,
  reference_json JSON NULL,
  create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_ai_msg_session_time (session_id, create_time)
);
```

#### 5.2.2 知识库文档表

```sql
CREATE TABLE knowledge_document (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  doc_name VARCHAR(200) NOT NULL,
  source_path VARCHAR(500) NOT NULL,
  content_hash VARCHAR(64) NOT NULL,
  status TINYINT DEFAULT 1,
  chunk_count INT DEFAULT 0,
  create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_kb_source (source_path)
);

CREATE TABLE knowledge_chunk (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT UNSIGNED NOT NULL,
  chunk_no INT NOT NULL,
  title VARCHAR(300) NULL,
  content TEXT NOT NULL,
  vector_id VARCHAR(128) NULL,
  metadata_json JSON NULL,
  create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_kb_doc_chunk (document_id, chunk_no)
);
```

#### 5.2.3 AI 推荐与行为表

```sql
CREATE TABLE ai_recommendation (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NULL,
  role VARCHAR(30) NULL,
  type VARCHAR(50) NOT NULL,
  title VARCHAR(200) NOT NULL,
  reason VARCHAR(500) NULL,
  priority VARCHAR(20) DEFAULT 'normal',
  target_route VARCHAR(200) NULL,
  status TINYINT DEFAULT 0,
  create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_ai_rec_user_status (user_id, status, create_time)
);
```

行为日志表见 4.9。

### 5.3 向量库集合设计

集合名：

```text
medicare_knowledge
```

向量记录字段：

| 字段 | 含义 |
|---|---|
| `vector_id` | 与 `knowledge_chunk.vector_id` 对应 |
| `embedding` | 文本向量 |
| `content` | chunk 文本 |
| `doc_name` | 文档名 |
| `source_path` | 来源路径 |
| `section_title` | 标题 |
| `chunk_no` | 分块序号 |
| `updated_at` | 更新时间 |

---

## 6. 接口设计思路

### 6.1 统一响应

继续使用现有 `Result<T>`：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

AI 接口也必须遵守该格式，避免前端增加额外适配成本。

### 6.2 AI 接口清单

| 方法 | 路径 | 权限 | 功能 |
|---|---|---|---|
| POST | `/api/ai/chat` | 登录用户 | 全局 AI 助手对话 |
| GET | `/api/ai/chat/sessions` | 登录用户 | 获取当前用户会话列表 |
| GET | `/api/ai/chat/sessions/{id}/messages` | 登录用户 | 获取会话消息 |
| POST | `/api/ai/rag/query` | 登录用户 | 知识库问答 |
| POST | `/api/ai/rag/reindex` | admin | 重建知识库索引 |
| POST | `/api/ai/record/assist` | admin、doctor | 病历辅助 |
| POST | `/api/ai/drug/check` | admin、doctor、pharmacist | 用药与库存风险检查 |
| GET | `/api/ai/recommendations` | 登录用户 | 获取推荐 |
| PUT | `/api/ai/recommendations/{id}/done` | 登录用户 | 标记推荐已处理 |
| POST | `/api/behavior/events` | 登录用户 | 上报行为事件 |
| GET | `/api/behavior/summary` | admin | 行为分析汇总 |

### 6.3 前后端交互原则

1. AI 接口由 `medicare-web/src/api/ai.ts` 统一封装。
2. AI 面板不直接访问业务 API，只向 `/api/ai/*` 提交问题和上下文。
3. 后端负责权限校验和数据裁剪，避免前端传入任意 ID 后越权查询。
4. AI 返回内容必须包含免责声明：辅助建议仅供参考，医疗诊断和处方以专业医生判断为准。
5. 高风险结果只提示，不自动修改业务数据。

---

## 7. 开发步骤与任务拆分

### 阶段 0：工程基线整理

目标：保证后续 UI 和 AI 开发有稳定基础。

任务：

1. 补充后端构建文件，优先新增 `medicare-server/pom.xml`，把 `libs/` 依赖迁移为 Maven 依赖。
2. 确认 JDK 17、Node.js 18+、MySQL 8.0 运行环境。
3. 运行 `sql/medicare.sql` 初始化数据库。
4. 启动后端 `MediCareApplication`，启动前端 `npm run dev`。
5. 执行 `scripts/test_functional.py`，记录当前可通过的核心业务流。
6. 备份当前 UI 截图，作为优化前后对比材料。

交付物：

```text
medicare-server/pom.xml
docs/baseline-screenshots/
docs/test-baseline.md
```

### 阶段 1：UI 主题与公共组件

目标：先统一视觉系统，再改页面。

任务：

1. 新增 `src/styles/theme.css`，定义颜色、间距、圆角、阴影、状态色。
2. 新增 `src/styles/animations.css`，定义路由、卡片、弹窗动画。
3. 在 `main.ts` 引入全局样式。
4. 新增公共组件：
   - `PageHeader.vue`
   - `StatCard.vue`
   - `DataToolbar.vue`
   - `StatusTag.vue`
   - `AnimatedNumber.vue`
   - `EmptyState.vue`
5. 改造 `MainLayout.vue`，统一侧边栏、顶栏、内容区和路由过渡。

验收标准：

1. 所有页面仍可正常访问。
2. 菜单权限保持原逻辑。
3. 桌面端、窄屏下布局不溢出。
4. 页面切换有平滑过渡。

### 阶段 2：首页和核心页面升级

目标：让系统第一屏具备演示效果和业务价值。

任务：

1. 安装 `echarts`、`dayjs`。
2. 改造 `DashboardView.vue`：
   - 统计卡片使用 `StatCard` + `AnimatedNumber`。
   - 增加趋势图、科室分布图、待办列表。
   - 根据角色展示不同快捷入口。
3. 扩展 `DashboardService`：
   - 增加近 7 日挂号趋势。
   - 增加科室挂号分布。
   - 增加低库存 TopN。
4. 改造患者、挂号、病历、药品、处方页面：
   - 统一标题区、搜索区、表格状态标签。
   - 弹窗表单增加 loading 与分组。
   - 危险操作增加明确确认文案。

验收标准：

1. 首页统计与数据库一致。
2. 图表在无数据时显示空状态。
3. 页面按钮、表格、弹窗风格一致。
4. 业务流程不回退：患者登记 → 挂号 → 叫号 → 病历 → 处方 → 取药。

### 阶段 3：AI 基础设施

目标：打通后端 AI 调用和前端 AI 面板。

任务：

1. 后端新增 AI 配置：
   - `ai.provider`
   - `ai.api-key`
   - `ai.model`
   - `ai.embedding-model`
   - `ai.vector-store`
2. 新增 `AiController`、`AiAssistantService`、AI DTO。
3. 接入 LangChain4j ChatModel，先实现普通问答。
4. 前端新增：
   - `api/ai.ts`
   - `AiAssistantFloat.vue`
   - `AiChatPanel.vue`
5. 在 `MainLayout.vue` 挂载 AI 悬浮按钮。
6. 增加 AI 对话历史表并保存消息。

验收标准：

1. 登录后可打开 AI 面板。
2. 能发送问题并得到回答。
3. 刷新页面后可查看历史会话。
4. 未登录访问 AI 接口会被拦截。

### 阶段 4：RAG 知识库

目标：让 AI 能回答项目文档、接口、部署、实训要求相关问题。

任务：

1. 新增知识库表：`knowledge_document`、`knowledge_chunk`。
2. 实现文档读取：
   - Markdown：直接读取。
   - HTML：去除标签后按标题分块。
   - docx：读取段落文本。
3. 实现 chunk 切分、embedding、向量库写入。
4. 新增 `/api/ai/rag/reindex` 管理员接口。
5. 新增 `/api/ai/rag/query` 查询接口。
6. AI 回复展示引用来源，例如文档名和章节。

验收标准：

1. 能回答“系统有哪些角色？”、“部署步骤是什么？”、“患者管理接口有哪些？”。
2. 回答中展示引用来源。
3. 文档更新后可重建索引。

### 阶段 5：医疗业务 Agent

目标：让 AI 能调用系统业务工具，而不是只做普通聊天。

任务：

1. 新增 Tool 类：
   - `PatientTool`
   - `MedicalRecordTool`
   - `MedicineTool`
   - `PrescriptionTool`
   - `RegistrationTool`
2. 工具方法只读优先，第一阶段不允许 AI 直接新增、修改、删除业务数据。
3. 实现意图识别：
   - 文档问答 → RAG。
   - 患者摘要 → 患者 + 病历 Tool。
   - 用药检查 → 患者 + 药品 + 处方 Tool。
   - 操作导航 → 返回前端 route action。
4. 控制工具权限：
   - 医生可查询患者、病历、处方建议。
   - 药师可查询药品、库存、待取药处方。
   - 管理员可查询统计和全部业务数据。

验收标准：

1. “总结患者 1 的历史病历”能调用病历数据。
2. “这个处方有没有库存风险”能结合药品库存。
3. 不同角色不能越权查询。
4. AI 不会直接修改业务数据。

### 阶段 6：病历辅助与用药提醒

目标：把 AI 能力嵌入医生工作站和处方流程。

任务：

1. 医生工作站增加 AI 病历辅助侧栏。
2. 实现 `/api/ai/record/assist`。
3. 处方页面增加“AI 用药检查”按钮。
4. 实现 `/api/ai/drug/check`。
5. 根据风险级别显示不同提示。
6. 高风险保存前二次确认。

验收标准：

1. 输入主诉后能生成病历字段建议。
2. AI 建议可一键插入，但不自动覆盖医生输入。
3. 患者过敏史与药品风险能被提示。
4. 库存不足或低库存能被提示。

### 阶段 7：推荐与行为分析

目标：形成可展示的数据智能闭环。

任务：

1. 新增行为日志表和接口。
2. 前端路由访问、搜索、业务操作、AI 使用上报行为事件。
3. 新增行为分析页面：
   - 近 7 日页面访问量。
   - 高频操作。
   - AI 使用次数。
   - 异常接口统计。
4. 实现 `/api/ai/recommendations`。
5. 首页展示推荐卡片。

验收标准：

1. 用户访问和操作能生成日志。
2. 管理员能查看行为分析图表。
3. 不同角色看到不同推荐。

---

## 8. 测试方案

### 8.1 后端测试

测试重点：

1. 患者身份证唯一性、搜索、删除权限。
2. 挂号号源扣减、取消回滚、状态流转。
3. 病历与挂号、患者、医生关联正确。
4. 处方开立事务：保存处方、扣库存、写库存日志。
5. 处方作废事务：状态变更、库存回滚。
6. 角色权限：admin、doctor、pharmacist 的接口访问差异。
7. AI 接口：未登录拦截、角色权限、异常降级。
8. RAG：文档索引、检索结果、引用来源。

建议测试文件：

```text
medicare-server/src/test/java/com/medicare/service/
medicare-server/src/test/java/com/medicare/controller/
```

### 8.2 前端测试

测试重点：

1. 登录、退出、刷新后用户状态恢复。
2. 菜单按角色显示。
3. 核心页面 CRUD 操作。
4. 表单校验和错误提示。
5. 首页图表无数据、有数据两种状态。
6. AI 面板发送、loading、错误、历史消息展示。
7. 移动端或窄屏布局不溢出。

### 8.3 功能联调脚本

继续扩展：

```text
scripts/test_functional.py
```

建议覆盖流程：

```text
登录 admin
→ 创建患者
→ 查询可用排班
→ 挂号
→ 叫号
→ 创建病历
→ 创建处方
→ 取药
→ 查询库存日志
→ 调用 AI 病历辅助
→ 调用 AI 用药检查
→ 调用 RAG 问答
```

### 8.4 验收标准

1. 前端 `npm run build` 通过。
2. 后端启动无异常。
3. 核心业务脚本通过。
4. AI 服务不可用时，主业务仍可正常使用，前端显示友好提示。
5. 所有新增接口遵守 `Result<T>` 响应格式。
6. 文档、接口、页面与本计划一致。

---

## 9. 部署方案

### 9.1 本地开发部署

```text
MySQL 8.0
→ 执行 sql/medicare.sql
→ 启动 Spring Boot 后端，端口 8080
→ 启动 Vue 前端，端口 5173
→ 可选启动 Chroma/Qdrant 向量库
```

前端：

```bash
cd medicare-web
npm install
npm run dev
```

后端：

```bash
# 推荐 Maven 化后
cd medicare-server
mvn spring-boot:run
```

### 9.2 生产部署

建议：

1. 前端 `npm run build` 后由 Nginx 托管。
2. 后端打包为 jar，通过 systemd 或 Docker 启动。
3. MySQL 单独部署，开启定期备份。
4. 向量库使用 Docker 部署 Chroma 或 Qdrant。
5. AI API Key 通过环境变量注入，不写入 Git。

示例环境变量：

```text
AI_PROVIDER=openai
AI_API_KEY=xxxx
AI_MODEL=gpt-4.1-mini
AI_EMBEDDING_MODEL=text-embedding-3-small
VECTOR_STORE_URL=http://localhost:6333
```

### 9.3 降级策略

1. AI 服务失败：返回“AI 服务暂不可用”，不影响业务 CRUD。
2. 向量库失败：AI 助手退化为普通问答，提示无法使用文档检索。
3. 大模型超时：设置 20-30 秒超时，前端允许取消请求。
4. API Key 缺失：后端启动时给出明确日志，AI 接口返回配置错误。

---

## 10. 风险与注意事项

1. 医疗安全：AI 只做辅助建议，不能替代医生诊断；界面必须展示免责声明。
2. 隐私数据：患者身份证、手机号、病历内容属于敏感信息，AI Prompt 中只传必要字段。
3. 权限控制：AI 工具调用必须复用当前登录用户角色，不能绕过 `@RequireRole` 的业务边界。
4. 成本控制：RAG 优先检索本地文档，减少长 Prompt；普通统计推荐优先用规则实现。
5. 工程复杂度：先完成 UI 和基础 AI 对话，再做 Agent 工具调用，避免一次性改动过大。
6. 构建管理：引入 AI 依赖前应先补齐 Maven，继续手动维护 jar 会增加风险。

---

## 11. 推荐开发顺序

1. 工程基线整理：补齐构建、确认测试脚本、记录现状。
2. UI 主题和主布局：先统一全局体验。
3. 首页仪表盘：先做最容易展示效果的页面。
4. 核心页面视觉统一：患者、挂号、病历、药品、处方。
5. AI 普通对话：打通前后端与模型调用。
6. RAG 知识库：接入项目文档问答。
7. 医疗业务 Agent：接入患者、病历、药品、处方工具。
8. 病历辅助和用药提醒：嵌入医生工作站和处方流程。
9. 推荐和行为分析：形成智能化闭环。
10. 测试、部署、文档更新：保证可交付。

---

## 12. 最终交付物

| 类型 | 交付内容 |
|---|---|
| UI | 统一主题、主布局、登录页、首页图表、核心页面美化、动画过渡 |
| AI | AI 助手、RAG 文档问答、病历辅助、用药提醒、推荐、行为分析 |
| 后端 | AI 控制器、服务、DTO、工具类、数据库迁移脚本、权限控制 |
| 前端 | AI 面板、知识库页面、行为分析页面、图表组件、公共 UI 组件 |
| 数据 | AI 对话表、知识库表、推荐表、行为日志表、向量库集合 |
| 测试 | 后端单元测试、接口测试、前端构建测试、功能联调脚本 |
| 文档 | 更新后的 plan、接口说明、部署说明、演示脚本 |

本方案以后续真实开发为导向：先稳定现有业务，再逐步升级界面和智能能力；每个阶段都应保持系统可运行、可演示、可回退。
