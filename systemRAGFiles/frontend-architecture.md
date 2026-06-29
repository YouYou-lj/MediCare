# 前端技术架构（medicare-web）

> **所属系统**: MediCare 智慧医疗门诊管理系统
> **文档类型**: RAG 检索文档 — 前端架构
> **技术栈**: Vue 3 + TypeScript + Vite + Element Plus + ECharts + Pinia

---

## 1. 前端项目结构

```
medicare-web/
├── src/
│   ├── api/                          # API 封装模块
│   │   ├── index.ts                   # Axios 全局实例（拦截器、错误处理）
│   │   ├── ai.ts                      # AI 助手接口
│   │   ├── auth.ts                    # 认证接口
│   │   ├── department.ts             # 科室接口
│   │   ├── doctor.ts                  # 医生接口
│   │   ├── knowledge.ts              # 知识库接口
│   │   ├── medical-record.ts         # 病历接口
│   │   ├── medicine.ts               # 药品接口
│   │   ├── patient.ts                # 患者接口
│   │   ├── prescription.ts           # 处方接口
│   │   ├── registration.ts           # 挂号接口
│   │   ├── schedule.ts               # 排班接口
│   │   └── user.ts                   # 用户/仪表盘接口
│   ├── components/                    # 公共组件
│   │   ├── AiAssistantFloat.vue      # AI 助手悬浮按钮 + 聊天面板
│   │   ├── AiChatPanel.vue           # AI 聊天面板（流式输出）
│   │   ├── AnimatedNumber.vue        # 数字动画组件
│   │   ├── DataToolbar.vue           # 数据工具栏（搜索/刷新/新增）
│   │   ├── EmptyState.vue            # 空状态占位组件
│   │   ├── PageHeader.vue            # 页面标题头
│   │   ├── StatCard.vue              # 统计卡片
│   │   └── StatusTag.vue             # 状态标签组件
│   ├── router/index.ts                # Vue Router 路由配置
│   ├── stores/user.ts                  # Pinia 用户状态管理
│   ├── styles/                         # 样式文件
│   │   ├── animations.css             # 动画定义
│   │   └── theme.css                  # 主题变量（CSS 自定义属性）
│   ├── types/index.ts                  # 全局 TypeScript 类型定义
│   ├── views/                          # 页面视图
│   │   ├── basic-data/BasicDataView.vue          # 基础数据（科室/医生/排班/用户）
│   │   ├── doctor/WorkstationView.vue            # 医生工作站（候诊 + 病历书写）
│   │   ├── knowledge-manage/KnowledgeManageView.vue  # 知识库管理
│   │   ├── knowledge-upload/KnowledgeUploadView.vue  # 知识库上传
│   │   ├── layout/DashboardView.vue              # 首页仪表盘
│   │   ├── layout/MainLayout.vue                 # 主布局（侧边栏 + 顶栏 + 内容区）
│   │   ├── login/LoginView.vue                   # 登录页
│   │   ├── medical-record/RecordList.vue         # 病历列表
│   │   ├── patient/PatientList.vue               # 患者列表
│   │   ├── pharmacy/MedicineList.vue             # 药品库存
│   │   ├── prescription/PrescriptionView.vue     # 处方管理
│   │   ├── registration/RegistrationView.vue     # 挂号预约
│   │   └── settings/SettingsView.vue             # 系统设置（改密码）
│   ├── App.vue                         # 根组件
│   ├── env.d.ts                        # 环境类型声明
│   └── main.ts                         # 入口文件
├── index.html                          # HTML 模板
├── vite.config.ts                      # Vite 构建配置
├── tsconfig.json                       # TypeScript 配置
└── package.json                        # 依赖配置
```

---

## 2. 路由配置 (router/index.ts)

### 2.1 路由表

| 路径 | 名称 | 组件 | 页面标题 | 图标 | 角色权限 |
|------|------|------|----------|------|----------|
| `/login` | Login | LoginView.vue | 登录 | — | 无 |
| `/dashboard` | Dashboard | DashboardView.vue | 首页 | HomeFilled | admin/doctor/pharmacist |
| `/patients` | PatientList | PatientList.vue | 患者管理 | User | admin/doctor |
| `/basic-data` | BasicData | BasicDataView.vue | 基础数据 | Folder | admin/doctor |
| `/registration` | Registration | RegistrationView.vue | 挂号预约 | Calendar | admin |
| `/workstation` | Workstation | WorkstationView.vue | 医生工作站 | Monitor | admin/doctor |
| `/medical-records` | MedicalRecordList | RecordList.vue | 病历管理 | Document | admin/doctor |
| `/pharmacy` | Pharmacy | MedicineList.vue | 药品库存 | FirstAidKit | admin/doctor/pharmacist |
| `/prescriptions` | PrescriptionList | PrescriptionView.vue | 处方管理 | Notebook | admin/doctor/pharmacist |
| `/settings` | Settings | SettingsView.vue | 系统设置 | Setting | admin/doctor/pharmacist |
| `/knowledge-upload` | KnowledgeUpload | KnowledgeUploadView.vue | 知识库上传 | UploadFilled | admin |
| `/knowledge-manage` | KnowledgeManage | KnowledgeManageView.vue | 知识库管理 | Management | admin |

### 2.2 路由守卫

- 未登录用户访问非登录页 → 自动跳转 `/login`
- 已登录用户访问 `/login` → 自动跳转 `/dashboard`
- 角色不匹配路由 → 自动跳转 `/dashboard`
- 页面标题动态设置：`document.title = "\${title} - 智慧医疗"`

---

## 3. HTTP 请求封装 (api/index.ts)

### 3.1 Axios 配置

```typescript
const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true,   // 携带 Cookie（Session 认证必需）
})
```

### 3.2 响应拦截器行为

- `code === 200` → 返回 `response.data`
- `code === 401` → 清除用户状态，跳转登录页
- `status === 403` → 提示"权限不足"
- `status === 500` → 提示"后端服务异常，请确认已启动最新后端并加载 application-secret.yml"
- `ERR_NETWORK` → 提示"后端服务未启动或网络连接失败"
- `Invalid CORS request` → 提示"跨域请求被后端拒绝"

---

## 4. 状态管理 (stores/user.ts)

Pinia Store 管理用户状态：
- `currentUser: SysUser | null` — 当前登录用户信息
- `isLoggedIn: boolean` — 登录状态
- `loadFromStorage()` — 从 localStorage 恢复会话
- `syncFromServer()` — 从后端 `/api/auth/current` 同步登录状态
- `clearUser()` — 清除状态并跳转登录

---

## 5. 主题与样式

### 5.1 CSS 自定义属性 (theme.css)

系统使用 CSS 变量定义全局主题色：

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `--color-primary` | `#0F9F8F` | 主色（医疗绿） |
| `--color-danger` | `#EF4444` | 危险/红色 |
| `--color-warning` | `#F59E0B` | 警告/橙色 |
| `--color-success` | `#10B981` | 成功/绿色 |
| `--bg-page` | `#f8fafc` | 页面背景 |
| `--bg-card` | `#ffffff` | 卡片背景 |
| `--bg-sidebar` | `#ffffff` | 侧边栏背景 |
| `--text-primary` | `#1e293b` | 主文字 |
| `--text-secondary` | `#64748b` | 次要文字 |
| `--header-height` | `60px` | 顶栏高度 |
| `--content-padding` | `24px` | 内容区内边距 |

### 5.2 动画

- 路由切换：`fade-slide`（淡入 + 上下位移，0.25s）
- 统计卡片数字：`AnimatedNumber` 递增动画
- 页面加载：`fadeIn` 0.4s ease-out

---

## 6. 全局组件

### 6.1 AiAssistantFloat（AI 助手浮窗）

- 位置：页面右下角固定悬浮
- 功能：点击展开聊天面板，支持基础对话和流式对话
- 特性：Markdown 渲染、引用折叠、会话历史、新会话、复制回答
- 消息角色：用户（右侧蓝色气泡）、AI（左侧白色气泡 + 头像）
- 安全提示：AI 回复底部固定附加安全提示

### 6.2 PageHeader（页面标题）

```vue
<PageHeader title="患者管理" subtitle="管理门诊患者档案信息" />
```

### 6.3 StatCard（统计卡片）

用于 Dashboard 展示各类统计数据，支持图标、数值、颜色、警告变体。

### 6.4 DataToolbar（数据工具栏）

通用表格顶部工具栏，包含搜索框、刷新按钮、新增按钮，支持插槽扩展。

### 6.5 StatusTag（状态标签）

根据状态值自动映射为带颜色的 Element Plus 标签，常用于表格状态列。

---

## 7. Vite 开发配置

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

开发环境 API 请求自动代理到后端 `localhost:8080`。

---

## 8. 构建与部署

```bash
cd medicare-web
npm install
npm run dev        # 开发模式（http://localhost:5173）
npm run build      # 生产构建（输出到 dist/）
```

生产部署：将 `dist/` 目录通过 Nginx 托管，配置反向代理到后端。
