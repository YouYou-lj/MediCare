# 部署与运维手册

> **所属系统**: MediCare 智慧医疗门诊管理系统
> **文档类型**: RAG 检索文档 — 部署运维
> **相关文件**: `DOC/部署手册.md`, `docker-compose.yml`, `docs/docker-database.md`

---

## 1. 环境要求

### 1.1 开发环境

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 后端运行环境（OpenJDK 或 Oracle JDK） |
| Node.js | 18+ | 前端运行环境 |
| MySQL | 8.0+ | 数据库服务（字符集 utf8mb4） |
| Redis | 7.0+ | 缓存 / 分布式锁 / Session 共享 / 限流 |
| Maven | 3.8+ | 后端构建工具（可选，也可用 IDE） |
| npm | 9+ | 前端包管理器 |

### 1.2 生产环境

| 组件 | 建议配置 |
|------|----------|
| 操作系统 | Linux (CentOS 7+/Ubuntu 20.04+) |
| JDK | 17 LTS |
| MySQL | 8.0+ |
| Redis | 7.0+（建议独立部署或云 Redis） |
| 前端服务器 | Nginx 1.20+ |
| 内存 | 后端 >= 2GB（含 Redis 连接），前端静态资源无特殊要求 |

---

## 2. 项目说明

| 项目 | 技术栈 | 默认端口 | 描述 |
|------|--------|----------|------|
| **medicare-server** | Spring Boot 3 + JPA + MySQL | 8080 | 后端完整版，功能代码完整 |
| **medicare-server-archetype** | Spring Boot 3 + JPA | 8080 | 后端练习版，需补全代码 |
| **medicare-web** | Vue 3 + TypeScript + Element Plus | 5173（开发）/ 80（生产） | 前端单页应用 |

---

## 3. 数据库部署

### 3.1 创建数据库

```bash
# 登录 MySQL
mysql -u root -p

# 创建数据库（字符集必须为 utf8mb4）
CREATE DATABASE IF NOT EXISTS medicare
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

### 3.2 导入初始数据

```bash
# 从项目根目录执行
mysql -u root -p medicare < sql/medicare.sql
```

### 3.3 Docker 数据库与 Redis（推荐开发环境）

项目提供 `docker-compose.yml` 快速启动 MySQL + Redis + Qdrant + Adminer：

```bash
docker-compose up -d
```

Docker 配置：
- MySQL:
  - 镜像: `mysql:8.0`
  - 端口映射: `3306:3306`
  - 数据库: `medicare`
  - 用户名: `root`
  - 密码: 查看 `docker-compose.yml` 中的 `MYSQL_ROOT_PASSWORD`
- Redis:
  - 镜像: `redis:7-alpine`
  - 端口映射: `6379:6379`
  - 持久化: AOF
  - 最大内存策略: `allkeys-lru`
- Qdrant: 向量数据库，用于 RAG 知识库
- Adminer: MySQL Web 管理界面，端口 `8081`

启动后无需额外配置 Redis，`application.yml` 默认连接 `localhost:6379`。

---

## 4. 后端部署 (medicare-server)

### 4.1 开发环境运行

#### 步骤 1: 配置数据库连接

编辑 `medicare-server/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/medicare?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
    username: root          # 根据本机环境修改
    password: a123456.      # 根据本机环境修改
```

#### 步骤 2: 配置 AI API Key（可选）

创建 `medicare-server/application-secret.yml`：

```yaml
ai:
  api-key: "sk-your-api-key-here"
```

#### 步骤 3: 启动应用

方式一：IDE 运行
- 打开 `medicare-server` 项目
- 运行 `MediCareApplication.java`

方式二：命令行
```bash
cd medicare-server
mvn spring-boot:run
```

方式三：打包运行
```bash
cd medicare-server
mvn clean package
java -jar target/medicare-server-1.0.0-SNAPSHOT.jar
```

### 4.2 生产环境配置建议

| 配置项 | 建议值 | 说明 |
|--------|--------|------|
| `server.port` | 8080 | 保持默认 |
| `datasource.username` | medicare | 使用专用数据库用户 |
| `datasource.password` | 强密码 | 大小写字母+数字+特殊字符 |
| `spring.data.redis.host` | 独立 Redis 地址 | 避免与后端同机 |
| `spring.data.redis.password` | 强密码 | Redis 启用密码认证 |
| `spring.session.timeout` | 30m | 根据安全要求调整 |
| `logging.level.com.medicare` | info | 生产环境降低日志级别 |
| `logging.level.org.hibernate.SQL` | warn | 关闭 SQL 日志 |
| `jpa.show-sql` | false | 关闭 SQL 打印 |
| `springdoc.api-docs.enabled` | false | 关闭 API 文档（生产环境） |
| `springdoc.swagger-ui.enabled` | false | 关闭 Swagger UI |

### 4.3 高并发部署建议

- **Session 共享**：已启用 `spring-session-data-redis`，多实例部署时登录态自动共享。
- **负载均衡**：可在 Nginx 中配置 upstream 多个后端实例（如 `8080`、`8081`），前端只访问 Nginx。
- **限流与缓存**：Redis 限流和缓存已接入，无需额外组件。
- **数据库**：号源/库存扣减已使用数据库乐观锁 + Redis 分布式锁双重保护，可作为最终一致性兜底。

---

## 5. 前端部署 (medicare-web)

### 5.1 开发环境运行

```bash
cd medicare-web

# 安装依赖（首次）
npm install

# 开发模式运行（自动代理到 localhost:8080）
npm run dev

# 访问地址: http://localhost:5173
```

### 5.2 Vite 代理配置

开发环境通过 Vite 代理转发 API 请求：

```typescript
// vite.config.ts
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
}
```

### 5.3 生产构建

```bash
cd medicare-web
npm run build
```

构建输出到 `medicare-web/dist/` 目录，包含：
- `index.html`
- `assets/`（JS、CSS、字体等静态资源）

### 5.4 Nginx 部署配置

```nginx
server {
    listen 80;
    server_name medicare.example.com;
    root /opt/medicare/web/dist;
    index index.html;

    # 前端路由（SPA 模式）
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理到后端
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header Cookie $http_cookie;
    }

    # 静态资源缓存
    location /assets {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

---

## 6. 默认账号与登录

### 6.1 初始账号

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| admin | 12345 | admin | 系统管理员 |

### 6.2 登录验证

1. 访问前端地址（如 `http://localhost:5173`）
2. 使用默认账号登录
3. 登录成功后跳转到 Dashboard 首页

---

## 7. 端口清单

| 端口 | 服务 | 说明 |
|------|------|------|
| 5173 | medicare-web | 前端开发服务器 |
| 8080 | medicare-server | 后端 API 服务 |
| 3306 | MySQL | 数据库服务 |
| 6379 | Redis | 缓存 / 分布式锁 / Session / 限流 |
| 6333/6334 | Qdrant | 向量数据库（RAG） |
| 8081 | Adminer | MySQL Web 管理（可选） |

---

## 8. 常见问题排查

### 8.1 数据库连接失败

**现象**: 后端启动报错 `Connection refused` 或 `Access denied`

**排查步骤**:
1. 检查 `application.yml` 中数据库连接信息是否正确
2. 确认 MySQL 服务已启动：`systemctl status mysql` 或 `brew services list`
3. 确认数据库 `medicare` 已创建
4. 确认用户名密码正确
5. 检查防火墙是否放行 3306 端口

### 8.2 前端无法访问

**现象**: 前端启动失败或页面空白

**排查步骤**:
1. 检查 `npm run dev` 启动后是否有控制台报错
2. 执行 `rm -rf node_modules && npm install` 重新安装依赖
3. 检查 Node.js 版本是否 >= 18
4. 检查浏览器控制台是否有 JS 错误

### 8.3 跨域请求被拒绝

**现象**: 前端请求后端返回 `Invalid CORS request`

**排查步骤**:
1. 检查 `WebMvcConfig` 中 `allowedOrigins` 是否包含前端地址
2. 开发环境确保前端通过 Vite 代理访问（不是直接访问 8080）
3. 生产环境检查 Nginx 代理配置

### 8.4 Session 认证问题

**现象**: 登录成功后刷新页面需要重新登录

**排查步骤**:
1. 检查浏览器是否阻止了 Cookie（Session 需要 Cookie 支持）
2. 检查浏览器控制台 Network 中请求是否携带了 Cookie
3. 检查后端是否正确配置了 Session
4. 确认 `withCredentials: true` 已设置（Axios 配置）

### 8.5 AI 助手无法使用

**现象**: AI 对话报错或提示 API Key 未配置

**排查步骤**:
1. 检查 `application-secret.yml` 中 `ai.api-key` 是否配置
2. 确认 API Key 有效且未过期
3. 检查网络是否能访问阿里云百炼服务
4. 查看后端日志中的具体错误信息

### 8.6 知识库无法检索

**现象**: RAG 问答提示"知识库尚未建立向量索引"

**排查步骤**:
1. 管理员执行"重建知识库"操作
2. 检查后端日志中是否有 embedding API 调用错误
3. 确认 `ai.embedding-model` 配置正确
4. 确认 `ai.api-key` 有调用 embedding 接口的权限

---

## 9. 运维建议

### 9.1 日志管理

后端日志配置在 `application.yml`：

```yaml
logging:
  level:
    com.medicare: debug        # 业务日志
    org.hibernate.SQL: debug   # SQL 日志（生产建议关闭）
```

日志文件位置：
- 开发环境：控制台输出
- 生产环境：可配置 `logging.file.name` 输出到文件

### 9.2 数据库备份

建议定期备份数据库：

```bash
# 备份
mysqldump -u root -p medicare > backup_$(date +%Y%m%d).sql

# 恢复
mysql -u root -p medicare < backup_20260101.sql
```

### 9.3 部署目录结构

```
/opt/medicare/
├── server/                    # 后端部署目录
│   ├── medicare-server-1.0.0-SNAPSHOT.jar
│   └── application-secret.yml
├── web/                       # 前端静态资源
│   ├── index.html
│   └── assets/
├── backup/                    # 数据库备份目录
└── scripts/                   # 运维脚本
    └── backup.sh
```

### 9.4 系统升级

1. 备份数据库
2. 停止旧版本服务
3. 部署新版本 jar 和前端 dist
4. 如有迁移脚本，先执行 SQL 迁移
5. 启动服务并验证

---

## 10. 技术栈版本清单

| 依赖/组件 | 版本 | 用途 |
|-----------|------|------|
| Spring Boot | 3.2.5 | 后端框架 |
| Java | 17 | 编程语言 |
| Hibernate | 6.4.4 | ORM 框架 |
| MySQL Connector | 8.3.0 | 数据库驱动 |
| HikariCP | 5.0.1 | 连接池 |
| Spring Security Crypto | 6.2.4 | 密码加密 |
| SpringDoc OpenAPI | 2.5.0 | API 文档 |
| PDFBox | 2.0.31 | PDF 解析 |
| Apache POI | 5.2.5 | Office 文档解析 |
| Lombok | 1.18.42 | 代码简化 |
| Vue | 3.x | 前端框架 |
| TypeScript | 5.x | 类型系统 |
| Vite | 5.x | 构建工具 |
| Element Plus | 2.x | UI 组件库 |
| ECharts | 5.x | 图表库 |
| Pinia | 2.x | 状态管理 |
| Axios | 1.x | HTTP 客户端 |
| Day.js | 1.x | 日期处理 |
