# 用户认证与权限控制

> **所属系统**: MediCare 智慧医疗门诊管理系统
> **文档类型**: RAG 检索文档 — 认证与权限
> **相关模块**: auth, controller, service, aspect

---

## 1. 认证架构概述

系统采用 **Session-Cookie** 认证机制，基于 HttpSession 实现无状态后端的有状态登录管理。前端通过 `withCredentials: true` 的 Axios 请求自动携带 Cookie，后端通过 `AuthInterceptor` 从 Session 中提取当前用户。

---

## 2. 核心组件

### 2.1 AuthInterceptor（认证拦截器）

- **路径**: `com.medicare.auth.AuthInterceptor`
- **实现**: `HandlerInterceptor`
- **职责**:
  1. 拦截所有请求，从 `HttpSession` 获取 `CURRENT_USER_KEY` 对应的用户对象
  2. 将用户对象存入 `Request` 属性，供后续 Controller 使用
  3. 未登录时返回 HTTP 401，前端据此跳转登录页

```java
public static final String CURRENT_USER_KEY = "CURRENT_USER";

public static SysUser getCurrentUser(HttpServletRequest request) {
    return (SysUser) request.getAttribute(CURRENT_USER_KEY);
}
```

### 2.2 RequireRole（角色注解）

- **路径**: `com.medicare.auth.RequireRole`
- **类型**: `@Retention(RUNTIME) @Target(METHOD)`
- **用途**: 标注在 Controller 方法上，声明该方法允许访问的角色列表

```java
@RequireRole({"admin", "doctor"})
@GetMapping
public Result<List<Patient>> list() { ... }

@RequireRole("admin")
@DeleteMapping("/{id}")
public Result<Void> delete(@PathVariable Long id) { ... }
```

### 2.3 RoleCheckAspect（权限切面）

- **路径**: `com.medicare.auth.RoleCheckAspect`
- **实现**: `@Aspect` + `@Around`
- **拦截规则**: 所有标记了 `@RequireRole` 的方法
- **执行逻辑**:
  1. 从 `HttpServletRequest` 获取当前用户
  2. 检查用户角色是否在 `@RequireRole` 允许的列表中
  3. 无权限则抛出 `BusinessException(403, "权限不足")`
  4. 有权限则继续执行原方法

---

## 3. 登录流程

### 3.1 登录接口

```
POST /api/auth/login
Content-Type: application/json

Request Body:
{
  "username": "admin",
  "password": "12345"
}

Response (Success):
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "admin",
    "realName": "管理员",
    "role": "admin",
    "doctorId": null
  }
}
```

### 3.2 登录处理逻辑（AuthController）

```java
@PostMapping("/login")
public Result<SysUser> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    SysUser user = sysUserService.login(request.getUsername(), request.getPassword());
    user.setPassword(null);  // 密码清空，不返回给前端
    httpRequest.getSession(true).setAttribute(AuthInterceptor.CURRENT_USER_KEY, user);
    return Result.ok(user);
}
```

### 3.3 密码校验逻辑（SysUserService）

1. 根据用户名查询 `sys_user` 表
2. 若用户不存在或已禁用，抛出异常
3. 密码校验：兼容 **明文**（迁移阶段）与 **BCrypt** 两种格式
4. 若密码是明文格式且匹配，自动调用 `BCryptPasswordEncoder` 加密并更新数据库
5. 校验成功返回用户对象

```java
public SysUser login(String username, String password) {
    SysUser user = sysUserRepository.findByUsername(username)
        .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));
    if (user.getStatus() != 1) {
        throw new BusinessException(401, "账号已禁用");
    }
    // 兼容明文与 BCrypt
    if (passwordEncoder.matches(password, user.getPassword())) {
        return user;
    } else if (password.equals(user.getPassword())) {
        // 明文匹配，自动升级为 BCrypt
        user.setPassword(passwordEncoder.encode(password));
        sysUserRepository.save(user);
        return user;
    }
    throw new BusinessException(401, "用户名或密码错误");
}
```

---

## 4. 登出流程

```
POST /api/auth/logout

处理逻辑:
1. 获取当前 Session（如果不存在则跳过）
2. 调用 session.invalidate() 销毁 Session
3. 返回 Result.ok()
```

---

## 5. 获取当前用户信息

```
GET /api/auth/current

Response:
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "admin",
    "realName": "管理员",
    "role": "admin"
  }
}
```

**用途**: 前端刷新页面后，通过此接口恢复登录状态。

---

## 6. 角色与权限矩阵

### 6.1 角色定义

| 角色标识 | 角色名称 | 说明 |
|----------|----------|------|
| `admin` | 管理员 | 拥有全部权限，包括系统设置、知识库管理、用户管理等 |
| `doctor` | 医生 | 可操作患者、病历、处方、医生工作站等医疗业务 |
| `pharmacist` | 药剂师 | 可操作药品库存、处方配药等药房业务 |

### 6.2 模块权限矩阵

| 模块 | 路径 | admin | doctor | pharmacist |
|------|------|:-----:|:------:|:----------:|
| 登录 | `/login` | ✅ | ✅ | ✅ |
| 首页仪表盘 | `/dashboard` | ✅ | ✅ | ✅ |
| 患者管理 | `/patients` | ✅ | ✅ | ❌ |
| 基础数据 | `/basic-data` | ✅ | ✅ | ❌ |
| 挂号预约 | `/registration` | ✅ | ❌ | ❌ |
| 医生工作站 | `/workstation` | ✅ | ✅ | ❌ |
| 病历管理 | `/medical-records` | ✅ | ✅ | ❌ |
| 药品库存 | `/pharmacy` | ✅ | ✅ | ✅ |
| 处方管理 | `/prescriptions` | ✅ | ✅ | ✅ |
| 系统设置 | `/settings` | ✅ | ✅ | ✅ |
| 知识库上传 | `/knowledge-upload` | ✅ | ❌ | ❌ |
| 知识库管理 | `/knowledge-manage` | ✅ | ❌ | ❌ |
| AI 助手 | 全局悬浮 | ✅ | ✅ | ✅ |

### 6.3 API 权限控制示例

```java
@RestController
@RequestMapping("/api/patients")
public class PatientController {

    @GetMapping
    @RequireRole({"admin", "doctor"})     // 患者列表：admin + doctor
    public Result<List<Patient>> list() { ... }

    @PostMapping
    @RequireRole({"admin", "doctor"})   // 新增患者：admin + doctor
    public Result<Patient> create(...) { ... }

    @DeleteMapping("/{id}")
    @RequireRole("admin")                 // 删除患者：仅 admin
    public Result<Void> delete(...) { ... }
}
```

---

## 7. 前端权限控制

### 7.1 路由守卫 (router/index.ts)

```typescript
router.beforeEach(async (to, _from, next) => {
  document.title = `${to.meta.title || 'MediCare'} - 智慧医疗`

  if (to.meta.requiresAuth !== false) {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) {
      userStore.loadFromStorage()
    }
    await userStore.syncFromServer()
    if (!userStore.isLoggedIn) {
      next('/login')
      return
    }
    const roles = to.meta.roles as string[] | undefined
    if (roles && !roles.includes(userStore.currentUser!.role)) {
      next('/dashboard')   // 角色不匹配 → 跳转首页
      return
    }
  }
  next()
})
```

### 7.2 菜单动态过滤 (MainLayout.vue)

侧边栏菜单根据当前用户角色动态过滤：

```typescript
const allMenuItems = [
  { path: '/dashboard', title: '首页', roles: ['admin', 'doctor', 'pharmacist'] },
  { path: '/patients', title: '患者管理', roles: ['admin', 'doctor'] },
  { path: '/registration', title: '挂号预约', roles: ['admin'] },
  // ...
]

const menuItems = computed(() => {
  const role = userStore.currentUser?.role || ''
  return allMenuItems.filter((item) => item.roles.includes(role))
})
```

---

## 8. 修改密码

```
PUT /api/users/password
Content-Type: application/json

Request Body:
{
  "oldPassword": "旧密码",
  "newPassword": "新密码"
}

校验规则:
- 旧密码必须正确
- 新密码不能为空
- 新密码不能与旧密码相同
```

---

## 9. 安全设计要点

| 安全项 | 实现方式 |
|--------|----------|
| 密码存储 | BCrypt 哈希（强度默认） |
| 密码传输 | HTTPS 建议（开发环境 HTTP + CORS） |
| 会话管理 | HttpSession，默认 Cookie 机制 |
| 跨域保护 | CORS 配置只允许特定 origin |
| 敏感字段过滤 | 返回前端时 `password` 字段置空 |
| 权限校验 | 后端注解 + AOP 切面（不可绕过） |
| 前端菜单过滤 | 仅做 UI 控制，最终权限由后端校验 |
| SQL 注入防护 | Spring Data JPA 参数化查询 |

---

## 10. 常见问题

### Q1: 登录后刷新页面需要重新登录？

**原因**: 浏览器阻止了第三方 Cookie，或后端 Session 配置异常。
**排查**:
1. 检查浏览器是否允许 Cookie
2. 检查 `AuthInterceptor` 是否正确配置
3. 检查 `application.yml` 中 `server.servlet.session` 配置

### Q2: 跨域请求被拒绝？

**原因**: 前端地址不在后端 CORS 允许列表中。
**解决**: 检查 `WebMvcConfig` 中的 `allowedOrigins` 是否包含前端开发服务器地址（如 `http://localhost:5173`）。

### Q3: 权限不足但角色正确？

**原因**: `@RequireRole` 注解与 `RoleCheckAspect` 未正确生效。
**排查**:
1. 确认 `RoleCheckAspect` 类上有 `@Aspect` 和 `@Component`
2. 确认 `WebMvcConfig` 中正确配置了 `AuthInterceptor`
3. 确认 Spring AOP 依赖已引入 (`spring-boot-starter-aop`)
