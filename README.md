# MediCare 智慧医疗门诊管理系统

> 基于 **Spring Boot 3 + Vue 3** 的全栈智慧医疗门诊管理解决方案。

**感谢沈院士 kimi199 会员支持！！！**

---

## 项目概述

MediCare 是一款面向中小型门诊的信息管理系统，采用前后端分离架构：

- **后端（medicare-server）**：Spring Boot 3 + JPA + MySQL，提供 RESTful API、Session 认证与 AI 知识库检索能力。
- **前端（medicare-web）**：Vue 3 + TypeScript + Element Plus + Vite，提供响应式单页应用。
- **基础设施**：MySQL 8 存储业务数据，Qdrant 存储文本向量，支持 AI 知识库语义检索。

系统纯净部署后**仅包含一个主管理员账号**，无任何业务数据与知识库文件；所有科室、医生、患者、药品、排班、病历、处方等数据均需通过系统界面自行录入。

---

## 技术栈

| 分层 | 技术 | 版本 |
|------|------|------|
| 后端 | Java + Spring Boot | JDK 17 / Spring Boot 3.2 |
| 后端 | Spring Data JPA | 3.2.x |
| 后端 | MySQL Connector/J | 8.3.0 |
| 后端 | Qdrant Client | 1.14.0 |
| 后端 | Apache PDFBox / POI | 文档文本抽取 |
| 前端 | Vue + TypeScript | Vue 3.4+ |
| 前端 | Element Plus / Pinia / Vue Router | 2.7+ / 2.1+ / 4.3+ |
| 前端 | Vite | 5.2+ |
| 基础设施 | MySQL | 8.0+ |
| 基础设施 | Qdrant | 1.12.0 |

---

## 项目结构

```text
MediCare/
├── medicare-server/              # 后端服务
│   ├── src/main/java/com/medicare/
│   │   ├── auth/                 # 认证拦截器、角色注解
│   │   ├── config/               # 配置类、数据迁移运行器
│   │   ├── controller/           # REST API 控制器
│   │   ├── dto/                  # 数据传输对象
│   │   ├── entity/               # JPA 实体
│   │   ├── repository/           # Spring Data 仓库
│   │   └── service/              # 业务逻辑层
│   ├── src/main/resources/application.yml
│   └── pom.xml
│
├── medicare-web/                 # 前端应用
│   ├── src/
│   │   ├── api/                  # API 请求封装
│   │   ├── components/           # 公共组件
│   │   ├── router/               # 路由配置
│   │   ├── stores/               # Pinia 状态管理
│   │   ├── types/                # TypeScript 类型
│   │   └── views/                # 页面视图
│   ├── package.json
│   └── vite.config.ts            # Vite 配置（含 /api 代理）
│
├── sql/                          # Docker 首次启动时自动执行的脚本
│   ├── medicare.sql              # 核心表结构 + 主管理员账号
│   └── migration_*.sql           # 扩展表与字段迁移
│
├── scripts/                      # 手动运维脚本（不会自动执行）
│   ├── clear_all_system_docs.sql # 清空系统文件
│   ├── rag_system_docs_cleanup.sql
│   ├── migration_knowledge_uploaded_by.sql
│   ├── migration_sys_numbering.sql
│   └── migration_vector_db.sql
│
├── docker-compose.yml            # Docker 基础设施编排
└── README.md                     # 本文件
```

---

## 环境要求

| 环境 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 后端运行 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 18+ | 前端运行 |
| npm | 9+ | 前端包管理 |
| Docker & Docker Compose | 任意较新版本 | 启动 MySQL / Qdrant |

---

## 快速开始

### 1. 克隆项目

```bash
git clone <你的仓库地址>
cd MediCare
```

### 2. 启动数据库与向量库

使用 Docker Compose 一键启动 MySQL 与 Qdrant：

```bash
docker compose up -d
```

首次启动时，MySQL 会自动执行 `sql/` 目录下的初始化脚本：

- 创建所有业务表
- 创建主管理员账号：`admin` / `12345`
- 执行后续 `migration_*.sql` 迁移脚本

| 服务 | 地址 | 说明 |
|------|------|------|
| MySQL | `localhost:3306` | 数据库名 `medicare`，root 密码 `a123456.` |
| Qdrant | `localhost:6333` | 向量数据库 HTTP 端口 |

### 3. 启动后端服务

```bash
cd medicare-server
mvn spring-boot:run -DskipTests
```

后端默认监听 `http://localhost:8080`。

### 4. 配置 AI（可选）

AI 助手、知识库 Embedding、RAG 检索等功能需要配置大模型 API 密钥。项目已提供示例文件：

```bash
cd medicare-server
cp application-secret.yml.example application-secret.yml
```

编辑 `application-secret.yml`，填入你的 OpenAI 兼容接口地址与密钥：

```yaml
ai:
  provider: openai
  api-key: sk-your-api-key-here
  model: gpt-4o-mini
  embedding-model: text-embedding-3-small
  base-url: https://api.openai.com/v1
  timeout: 20s
```

> `application-secret.yml` 已被 `.gitignore` 忽略，请勿将其提交到 Git 仓库。

### 5. 启动前端应用

```bash
cd medicare-web
npm install
npm run dev
```

前端默认监听 `http://localhost:5173`，并通过 Vite 代理将 `/api` 请求转发到后端 `http://localhost:8080`。

### 6. 登录系统

打开浏览器访问：http://localhost:5173

使用默认主管理员账号登录：

- **用户名**：`admin`
- **密码**：`12345`

登录后可通过「系统设置 > 用户管理」创建管理员、医生、药剂师等账号，再依次录入科室、医生、排班、药品、患者等基础数据。

---

## 默认账号

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| `admin` | `12345` | 主管理员 | 系统唯一预置账号，拥有全部权限 |

> 纯净部署不预置任何医生、患者、药品、科室、病历等业务数据，所有业务数据均需通过系统界面自行维护。

---

## 常用命令

```bash
# 后端打包
mvn clean package -DskipTests

# 前端生产构建
npm run build

# 停止 Docker 基础设施
docker compose down

# 完全清理数据库数据（慎用）
docker compose down -v
```

---

## API 文档

后端启动后，可访问 Swagger UI 查看接口：

```text
http://localhost:8080/swagger-ui.html
```

---

## 特别感谢

**感谢沈院士特别赞助！！！**
