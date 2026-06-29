# 后端技术架构（medicare-server）

> **所属系统**: MediCare 智慧医疗门诊管理系统
> **文档类型**: RAG 检索文档 — 后端架构
> **技术栈**: Spring Boot 3.2.5 + Java 17 + JPA + MySQL 8.0

---

## 1. 后端项目结构

```
medicare-server/
├── src/main/java/com/medicare/
│   ├── MediCareApplication.java          # 启动类
│   ├── annotation/                        # 自定义注解
│   │   ├── RateLimit.java                 # 接口限流注解
│   │   └── RepeatSubmitLock.java          # 防重复提交注解
│   ├── aspect/                            # AOP 切面
│   │   ├── RateLimitAspect.java           # 限流切面
│   │   └── RepeatSubmitAspect.java        # 防重复提交切面
│   ├── auth/
│   │   ├── AuthInterceptor.java          # Session 认证拦截器
│   │   ├── RequireRole.java              # 角色权限注解
│   │   └── RoleCheckAspect.java          # @RequireRole AOP 切面
│   ├── common/                            # 通用工具
│   │   ├── CacheKey.java                 # 缓存 Key 常量
│   │   ├── RateLimiter.java              # Redis 滑动窗口限流器
│   │   └── RedisLock.java                # Redis 分布式锁
│   ├── config/
│   │   ├── AiProperties.java             # AI 配置属性类
│   │   ├── AiTaskExecutorConfig.java     # AI 任务线程池配置
│   │   ├── DataMigrationRunner.java      # 启动时数据迁移（密码 BCrypt）
│   │   ├── RedisConfig.java              # Redis 缓存/序列化配置
│   │   └── WebMvcConfig.java             # WebMvc 配置（CORS、拦截器）
│   ├── controller/                        # REST API 控制器（12个）
│   │   ├── AiController.java             # AI 助手 + RAG 接口
│   │   ├── AuthController.java           # 登录/登出/当前用户
│   │   ├── DashboardController.java      # 仪表盘统计数据
│   │   ├── DepartmentController.java     # 科室管理
│   │   ├── DoctorController.java         # 医生管理 + 候诊队列
│   │   ├── KnowledgeController.java      # 知识库文档管理
│   │   ├── MedicalRecordController.java  # 病历 CRUD
│   │   ├── MedicineController.java       # 药品库存管理
│   │   ├── PatientController.java        # 患者管理
│   │   ├── PrescriptionController.java   # 处方管理
│   │   ├── RegistrationController.java   # 挂号预约管理
│   │   ├── ScheduleController.java       # 排班号源管理
│   │   └── UserController.java           # 用户管理 + 修改密码
│   ├── dto/                               # 数据传输对象
│   ├── entity/                            # JPA 实体（15个）
│   ├── exception/
│   │   ├── BusinessException.java         # 业务异常
│   │   └── GlobalExceptionHandler.java    # 全局异常处理器
│   ├── repository/                        # JPA Repository 接口（15个）
│   └── service/                           # 业务逻辑层
│       ├── AiAssistantService.java        # AI 对话核心服务
│       ├── AiChatHistoryService.java      # 会话历史管理
│       ├── DashboardService.java          # 仪表盘统计
│       ├── DepartmentService.java         # 科室业务
│       ├── DoctorService.java             # 医生业务
│       ├── DocumentTextExtractionService.java # 文档文本提取
│       ├── LiveBusinessContextService.java    # 实时业务数据快照
│       ├── MedicalRecordService.java      # 病历业务
│       ├── MedicineService.java           # 药品库存业务
│       ├── PatientService.java            # 患者业务
│       ├── PrescriptionService.java       # 处方业务
│       ├── RagService.java                # RAG 知识库服务
│       ├── RegistrationService.java       # 挂号业务
│       ├── ScheduleService.java           # 排班业务
│       └── SysUserService.java            # 用户认证业务
```

---

## 2. Maven 依赖 (pom.xml)

| 依赖 | 版本 | 用途 |
|------|------|------|
| `spring-boot-starter-web` | 3.2.5 | REST API + 内嵌 Tomcat |
| `spring-boot-starter-data-jpa` | 3.2.5 | Spring Data JPA + Hibernate |
| `spring-boot-starter-validation` | 3.2.5 | Hibernate Validator |
| `spring-boot-starter-aop` | 3.2.5 | Spring AOP + AspectJ（权限、限流、幂等切面） |
| `spring-boot-starter-data-redis` | 3.2.5 | Redis 缓存 + 分布式锁 + 限流 |
| `spring-session-data-redis` | 3.2.5 | 多实例 Session 共享 |
| `spring-security-crypto` | 6.2.4 | BCrypt 密码加密 |
| `springdoc-openapi-starter-webmvc-ui` | 2.5.0 | Swagger UI 文档 |
| `mysql-connector-j` | 8.3.0 | MySQL 驱动 |
| `lombok` | 1.18.42 | 减少样板代码 |
| `pdfbox` | 2.0.31 | PDF 文档解析 |
| `poi-ooxml` | 5.2.5 | Word/Excel 解析 |
| `poi-scratchpad` | 5.2.5 | PowerPoint 解析 |

---

## 3. 配置文件 (application.yml)

### 3.1 服务器、数据库与 Redis

```yaml
server:
  port: 8080
  tomcat:
    threads:
      max: 200
      min-spare: 20
    accept-count: 100
    connection-timeout: 20000

spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:medicare}?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:a123456.}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      idle-timeout: 300000
      connection-timeout: 10000
      max-lifetime: 1800000

  jpa:
    hibernate:
      ddl-auto: none
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}
      timeout: 3s
      lettuce:
        pool:
          max-active: 32
          max-idle: 16
          min-idle: 4
          max-wait: 2s

  session:
    store-type: redis
    timeout: 30m
    redis:
      namespace: medicare:session

  cache:
    type: redis
```

### 3.2 高并发相关组件

| 组件 | 文件/配置 | 作用 |
|------|-----------|------|
| Redis 缓存 | `RedisConfig` + `@Cacheable` | 科室、医生、排班、药品、用户、仪表盘统计缓存 |
| 分布式锁 | `RedisLock` | 号源扣减、药品库存扣减的互斥保护 |
| 接口限流 | `@RateLimit` + `RateLimitAspect` | 登录、AI 对话、挂号、开处方、出入库等接口限流 |
| 防重复提交 | `@RepeatSubmitLock` + `RepeatSubmitAspect` | 写接口幂等控制 |
| Session 共享 | `spring-session-data-redis` | 多实例部署时登录态共享 |
| 连接池/线程池 | `HikariCP` / `Tomcat` | 数据库连接池与内嵌 Tomcat 调优 |

### 3.3 AI 配置

```yaml
ai:
  provider: ${AI_PROVIDER:bailian}
  api-key: ${AI_API_KEY:}
  model: ${AI_MODEL:deepseek-v4-flash}
  embedding-model: ${AI_EMBEDDING_MODEL:text-embedding-v4}
  base-url: ${AI_BASE_URL:https://dashscope.aliyuncs.com/compatible-mode/v1}
  timeout: ${AI_TIMEOUT:20s}
  executor:
    generation-core-size: 4
    generation-max-size: 12
    retrieval-core-size: 4
    retrieval-max-size: 12
    queue-capacity: 100
    retrieval-min-parallel-chunks: 32
```

### 3.3 OpenAPI / Swagger

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    tags-sorter: alpha
    operations-sorter: alpha
  packages-to-scan: com.medicare.controller
```

Swagger 访问地址: `http://localhost:8080/swagger-ui.html`

---

## 4. 认证与拦截机制

### 4.1 认证流程

1. 前端提交 `LoginRequest` (username, password) 到 `POST /api/auth/login`
2. `SysUserService.login()` 校验密码（兼容明文与 BCrypt）
3. 成功后写入 `HttpSession`，键为 `AuthInterceptor.CURRENT_USER_KEY`
4. 返回 `SysUser`（密码置空）
5. 后续请求通过 `AuthInterceptor` 从 Session 获取用户
6. 未登录返回 HTTP 401

### 4.2 权限控制

- **注解**: `@RequireRole({"admin", "doctor"})` 标注在 Controller 方法上
- **切面**: `RoleCheckAspect` 拦截带 `@RequireRole` 的方法，检查当前用户角色是否在允许列表中
- **角色**: `admin`（管理员）、`doctor`（医生）、`pharmacist`（药剂师）

### 4.3 CORS 配置

`WebMvcConfig` 配置 CORS，允许前端开发服务器 (`localhost:5173`) 跨域访问，支持 `GET/POST/PUT/DELETE/OPTIONS`，并暴露 `Authorization` 响应头。

---

## 5. 全局响应格式

所有 API 统一返回 `Result<T>` 对象：

```java
public class Result<T> {
    private int code;      // 200=成功, 401=未登录, 403=无权限, 500=服务器错误, 502=AI服务错误, 504=AI超时
    private String message;
    private T data;
}
```

---

## 6. 异常处理

| 异常类型 | 处理方式 |
|----------|----------|
| `BusinessException` | 返回 JSON，code 与 message 由业务定义 |
| `RestClientResponseException` | AI 服务返回非 2xx 时，提取错误信息返回 502 |
| `ResourceAccessException` | AI 网络超时，返回 504 |
| `AsyncRequestNotUsableException` | 流式连接被客户端关闭，静默处理 |
| 其他未捕获异常 | `GlobalExceptionHandler` 捕获，返回 500 并记录日志 |

---

## 7. 启动数据迁移

`DataMigrationRunner`（实现 `CommandLineRunner`）在应用启动时执行：
- 扫描所有 `sys_user` 中仍为明文格式的密码
- 使用 `BCryptPasswordEncoder` 进行加密后更新
- 确保旧系统平滑迁移到 Web 版
