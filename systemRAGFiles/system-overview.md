# MediCare 系统概述与整体架构

> **所属系统**: MediCare 智慧医疗门诊管理系统
> **文档类型**: RAG 检索文档 — 系统级概述
> **版本**: v1.0.0

---

## 1. 系统简介

MediCare 是一款面向中小型医疗机构的智慧医疗门诊管理系统，采用 **前后端分离** 架构，覆盖患者管理、挂号预约、医生工作站、病历管理、药品库存、处方管理等核心业务。系统内置 AI 智能助手与 RAG 知识库，支持管理员上传文档并基于向量检索进行智能问答。

---

## 2. 技术栈总览

| 层级 | 技术选型 | 版本 |
|------|----------|------|
| 后端框架 | Spring Boot | 3.2.5 |
| Java 版本 | OpenJDK | 17 |
| 持久层 | Spring Data JPA + Hibernate | 6.4.4 |
| 数据库 | MySQL | 8.0+ |
| 连接池 | HikariCP | 5.0.1 |
| 安全 | Spring Security Crypto (BCrypt) | 6.2.4 |
| API 文档 | SpringDoc OpenAPI (Swagger UI) | 2.5.0 |
| 前端框架 | Vue | 3.x |
| 前端语言 | TypeScript | 5.x |
| UI 组件库 | Element Plus | 2.x |
| 构建工具 | Vite | 5.x |
| 图表库 | ECharts | 5.x |
| 状态管理 | Pinia | 2.x |
| 路由 | Vue Router | 4.x |
| HTTP 客户端 | Axios | 1.x |
| AI 服务 | 阿里云百炼 (Bailian) / DashScope | 兼容 OpenAI API |
| 文档解析 | Apache PDFBox + Apache POI | 2.0.x / 5.2.x |

---

## 3. 项目目录结构

```
MediCare/
├── medicare-server/                  # 后端完整版（Spring Boot）
│   ├── src/main/java/com/medicare/
│   │   ├── MediCareApplication.java   # 启动类
│   │   ├── auth/                      # 认证拦截器 + 角色注解 + AOP 切面
│   │   ├── config/                    # 配置类（WebMvc、AI、数据源迁移）
│   │   ├── controller/                # REST API 控制器（12个模块）
│   │   ├── dto/                       # 请求/响应数据传输对象
│   │   ├── entity/                    # JPA 实体（15张表）
│   │   ├── exception/                 # 全局异常处理
│   │   ├── repository/                # Spring Data JPA 仓库接口
│   │   └── service/                   # 业务逻辑层（含 AI、RAG）
│   ├── src/main/resources/
│   │   ├── application.yml            # 主配置文件
│   │   ├── application-dev.yml.example # 开发环境配置模板
│   │   ├── ai/medicare-agent-skill.md # AI 助手 Skill 提示词
│   │   └── db/migration_v2.sql        # V2 数据库迁移脚本
│   └── pom.xml                        # Maven 构建配置
│
├── medicare-server-archetype/        # 后端原型/模板版（练习用）
│   └── src/...                        # 仅保留方法签名，需学生补全实现
│
├── medicare-web/                     # 前端项目（Vue 3 + Vite）
│   ├── src/
│   │   ├── api/                       # 各模块 API 封装（TypeScript）
│   │   ├── components/                # 公共组件（AI 助手浮窗、统计卡片等）
│   │   ├── router/index.ts            # 路由配置（11个页面 + 登录）
│   │   ├── stores/user.ts             # 用户状态（Pinia）
│   │   ├── views/                     # 页面视图
│   │   │   ├── basic-data/            # 基础数据
│   │   │   ├── doctor/WorkstationView.vue # 医生工作站
│   │   │   ├── knowledge-manage/      # 知识库管理
│   │   │   ├── knowledge-upload/      # 知识库上传
│   │   │   ├── layout/                # 主布局 + 仪表盘
│   │   │   ├── login/LoginView.vue    # 登录页
│   │   │   ├── medical-record/RecordList.vue # 病历列表
│   │   │   ├── patient/PatientList.vue # 患者列表
│   │   │   ├── pharmacy/MedicineList.vue # 药品库存
│   │   │   ├── prescription/PrescriptionView.vue # 处方管理
│   │   │   ├── registration/RegistrationView.vue # 挂号预约
│   │   │   └── settings/SettingsView.vue # 系统设置
│   │   ├── App.vue
│   │   └── main.ts
│   ├── index.html
│   └── package.json
│
├── DOC/                                # 项目文档
│   ├── 部署手册.md
│   ├── 教案.md
│   ├── SQL参考.md
│   ├── SRS.html / api.html
│   └── tasks/                          # 学生任务书
│       ├── task-A-患者管理.md
│       ├── task-B-挂号预约.md
│       ├── task-C-医生工作站与病历.md
│       ├── task-D-药品库存与处方.md
│       └── task-teacher-基础数据演示.md
│
├── docs/docker-database.md             # Docker 数据库说明
├── docker-compose.yml                  # Docker Compose 配置
└── .github/                            # GitHub 自动化升级脚本
```

---

## 4. 核心功能模块

| 模块 | 页面路径 | 角色权限 | 后端 Controller |
|------|----------|----------|----------------|
| 登录 | `/login` | 无 | `AuthController` |
| 首页仪表盘 | `/dashboard` | admin/doctor/pharmacist | `DashboardController` |
| 患者管理 | `/patients` | admin/doctor | `PatientController` |
| 基础数据 | `/basic-data` | admin/doctor | `DepartmentController`, `DoctorController`, `UserController`, `ScheduleController` |
| 挂号预约 | `/registration` | admin | `RegistrationController` |
| 医生工作站 | `/workstation` | admin/doctor | `DoctorController` (候诊队列) |
| 病历管理 | `/medical-records` | admin/doctor | `MedicalRecordController` |
| 药品库存 | `/pharmacy` | admin/doctor/pharmacist | `MedicineController` |
| 处方管理 | `/prescriptions` | admin/doctor/pharmacist | `PrescriptionController` |
| 系统设置 | `/settings` | admin/doctor/pharmacist | `UserController` |
| 知识库上传 | `/knowledge-upload` | admin | `KnowledgeController` |
| 知识库管理 | `/knowledge-manage` | admin | `KnowledgeController` |
| AI 助手浮窗 | 全局悬浮 | 所有登录用户 | `AiController` |

---

## 5. 系统角色

| 角色标识 | 角色名称 | 可访问模块 |
|----------|----------|------------|
| `admin` | 系统管理员 | 全部模块 |
| `doctor` | 医生 | 仪表盘、患者管理、基础数据、医生工作站、病历、药品库存、处方、设置 |
| `pharmacist` | 药剂师 | 仪表盘、药品库存、处方、设置 |

---

## 6. 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | 12345 | admin |

---

## 7. 服务端口

| 服务 | 开发端口 | 生产端口 |
|------|----------|----------|
| medicare-web | 5173 | 80 (Nginx) |
| medicare-server | 8080 | 8080 |
| MySQL | 3306 | 3306 |

---

## 8. 关键设计约束

- 认证方式：基于 **HttpSession** 的 Session-Cookie 认证
- 权限控制：`@RequireRole` 注解 + `RoleCheckAspect` AOP 切面
- 数据库迁移：通过 `DataMigrationRunner` 在启动时自动将旧密码迁移为 BCrypt 格式
- AI 集成：通过 `application-secret.yml` 配置 `AI_API_KEY`，默认使用阿里云百炼 `deepseek-v4-flash` 模型
- RAG 知识库：默认索引 `DOC/`、`docs/`、`medicare-server/src/main/java/com/medicare/controller|dto|entity`、`medicare-web/src/api|types|router` 等目录下的文本文件
- 文档解析：支持 PDF、Word (.doc/.docx)、PowerPoint (.ppt)、Markdown、HTML、纯文本、Java、TypeScript、Vue、YAML、JSON
