# MediCare 智慧医疗门诊管理系统 — 课程 PPT 大纲

> **课程名称**：JavaFX 桌面应用开发与分层架构实战  
> **项目案例**：MediCare 智慧医疗门诊管理系统  
> **课时**：12 课时（每课时 45 分钟）  
> **幻灯片比例**：16:9  
> **母版设计**：标题页 / 目录页 / 章节过渡页 / 内容页 统一母版  

---

## 第 1 章：课程导入与项目概览（4 页）

| 页码 | 页面标题 | 关键内容 | 可视化 |
|------|----------|----------|--------|
| 1 | 课程封面 | 课程名称、讲师、项目案例、培训周期 | — |
| 2 | 课程目标 | 学员对象、学完能力、授课方式 | — |
| 3 | 项目业务模块 | 挂号、患者、医生工作站、病历、药品库存、处方 | — |
| 4 | 技术栈全景 | Java 17 / JavaFX 21 / Maven / MySQL / HikariCP / DbUtils / Git | drawio 技术栈分层全景图 |

---

## 第 2 章：开发环境与工具链（6 页）

| 页码 | 页面标题 | 关键内容 | 可视化 |
|------|----------|----------|--------|
| 5 | 章节封面：开发环境 | 工具链概览 | — |
| 6 | JDK 17 + JavaFX 21 | 安装、模块路径、IDEA 配置 | — |
| 7 | SceneBuilder | 可视化 FXML 设计工具、插件安装 | — |
| 8 | Maven 项目结构 | pom.xml、src/main/java、resources、生命周期 | drawio Maven 项目目录结构图 |
| 9 | Maven Shade 插件 | Fat JAR 打包机制、运行命令 | — |
| 10 | 脚手架导入与运行 | mvn clean package → java -jar → 登录界面 | — |

---

## 第 3 章：Git 协作规范（6 页）

| 页码 | 页面标题 | 关键内容 | 可视化 |
|------|----------|----------|--------|
| 11 | 章节封面：Git 协作 | 为什么项目需要版本控制 | — |
| 12 | 仓库初始化 | git init --bare、git clone、远程仓库 | — |
| 13 | 分支策略 | main 保护分支、feature/模块名 分支 | drawio Git 分支模型图 |
| 14 | 提交规范 | feat / fix / refactor / docs 前缀 | — |
| 15 | 合并冲突处理 | pull → edit → add → commit 四步法 | — |
| 16 | 小组分工与依赖 | A/B/C/D 四组职责、依赖顺序 | drawio 模块依赖关系图 |

---

## 第 4 章：JavaFX 基础与 SceneBuilder（6 页）

| 页码 | 页面标题 | 关键内容 | 可视化 |
|------|----------|----------|--------|
| 17 | 章节封面：JavaFX 基础 | 桌面开发技术选型 | — |
| 18 | JavaFX 场景图 | Stage → Scene → Parent → Node 层级 | drawio JavaFX 场景图层级 |
| 19 | Application 生命周期 | init / start / stop、Launcher/Main 启动 | — |
| 20 | FXML 与 Controller | fx:id、@FXML、FXMLLoader 绑定机制 | drawio FXML 加载流程图 |
| 21 | SceneBuilder 操作 | 三区界面、拖拽组件、生成 FXML | — |
| 22 | 参考案例：登录模块 | Controller 结构、后台验证、界面切换 | — |

---

## 第 5 章：JavaFX 布局详解（6 页）

| 页码 | 页面标题 | 关键内容 | 可视化 |
|------|----------|----------|--------|
| 23 | 章节封面：布局体系 | 布局选择的重要性 | — |
| 24 | VBox / HBox | spacing、alignment、padding、hgrow | — |
| 25 | BorderPane | top/left/center/right/bottom 五区 | drawio BorderPane 五区布局图 |
| 26 | GridPane | rowIndex、columnIndex、表单布局 | — |
| 27 | SplitPane / TabPane | 分割面板、标签页、适用场景 | — |
| 28 | FXML 布局模板 | list-view / dialog-form / split-pane / tab-pane 模板 | drawio 布局选择决策树 |

---

## 第 6 章：TableView 与数据绑定（5 页）

| 页码 | 页面标题 | 关键内容 | 可视化 |
|------|----------|----------|--------|
| 29 | 章节封面：TableView | 表格控件核心能力 | — |
| 30 | TableView 泛型设计 | TableView<T> + TableColumn<T, V> | — |
| 31 | PropertyValueFactory | 属性工厂绑定原理 | drawio TableView 数据绑定流程图 |
| 32 | 自定义 CellFactory | 状态颜色、文本格式化、updateItem | — |
| 33 | Dialog 弹窗联动 | 选中行填充表单、保存删除 | — |
| 34 | 泛型类型匹配陷阱 | String vs LocalDate、ClassCastException 规避 | — |

---

## 第 7 章：多层架构思想（6 页）

| 页码 | 页面标题 | 关键内容 | 可视化 |
|------|----------|----------|--------|
| 35 | 章节封面：多层架构 | 从单层到分层的演进 | — |
| 36 | 为什么需要分层 | 单层架构反例、耦合问题 | — |
| 37 | 四层职责边界 | Controller / Service / DAO / Model | drawio 四层架构图 |
| 38 | 关键约束 | Controller 禁止直接调用 DAO | — |
| 39 | 请求数据流 | UI → Controller → Service → DAO → DB → 返回 | drawio 请求数据流图 |
| 40 | 基础数据模块走读 | 真实代码中的分层体现 | — |

---

## 第 8 章：事务控制与 Service 层（4 页）

| 页码 | 页面标题 | 关键内容 | 可视化 |
|------|----------|----------|--------|
| 41 | 章节封面：事务控制 | 业务一致性的保障 | — |
| 42 | 事务四步法 | getConnection → setAutoCommit(false) → commit/rollback → close | drawio 事务执行流程图 |
| 43 | finally 关闭连接 | 资源泄漏风险、模板代码 | — |
| 44 | 挂号事务案例 | schedule 扣减 + registration 插入 | — |
| 45 | 取消挂号与出入库练习 | 事务边界设计 | — |

---

## 第 9 章：数据访问层深度讲解（6 页）

| 页码 | 页面标题 | 关键内容 | 可视化 |
|------|----------|----------|--------|
| 46 | 章节封面：数据访问层 | 数据库访问的核心技术 | — |
| 47 | HikariCP 连接池 | 连接池 vs DriverManager、配置参数 | drawio HikariCP 连接池工作原理图 |
| 48 | DbUtils 核心 Handler | BeanListHandler / BeanHandler / ScalarHandler | — |
| 49 | BaseDAO 设计 | 泛型 DAO、CRUD 封装、事务辅助 | drawio BaseDAO 继承与调用关系图 |
| 50 | 类型映射三剑客 | 下划线→驼峰、java.time、INT UNSIGNED→Long | — |
| 51 | 手写 DepartmentDAO | 从 SQL 常量到 DAO 方法 | — |

---

## 第 10 章：后台线程与 UI 更新（4 页）

| 页码 | 页面标题 | 关键内容 | 可视化 |
|------|----------|----------|--------|
| 52 | 章节封面：后台线程 | 避免界面卡顿 | — |
| 53 | JavaFX 单线程规则 | UI 线程职责、卡顿演示 | — |
| 54 | Task 标准模板 | call() 后台执行 → Platform.runLater 更新 UI | drawio JavaFX UI 线程与后台线程交互图 |
| 55 | 进度反馈 | ProgressBar + Task 进度更新 | — |
| 56 | 对比练习 | UI 线程 sleep vs Task 模式 | — |

---

## 第 11 章：分组实战、联调与代码审查（5 页）

| 页码 | 页面标题 | 关键内容 | 可视化 |
|------|----------|----------|--------|
| 57 | 章节封面：项目实战 | 从学习到产出的关键阶段 | — |
| 58 | 分组开发流程 | DAO → Service → Controller → FXML | — |
| 59 | 模块依赖与接口约定 | 先约定接口、后用 mock 数据 | drawio 模块依赖与集成顺序图 |
| 60 | 集成联调顺序 | A 组 → B 组 → C 组 → D 组 | — |
| 61 | 代码审查 Checklist | 分层、SQL、线程、事务、类型、Git | drawio 代码审查流程图 |
| 62 | 课程总结与知识地图 | 12 课时核心技能回顾 | — |

---

## 可视化图表清单

| 编号 | 图表名称 | 所在章节 | 用途 |
|------|----------|----------|------|
| D1 | 技术栈分层全景图 | 第 1 章 | 展示完整技术栈 |
| D2 | Maven 项目目录结构图 | 第 2 章 | 解释项目结构 |
| D3 | Git 分支模型图 | 第 3 章 | 说明分支策略 |
| D4 | 模块依赖关系图 | 第 3 章 | 说明小组分工与依赖 |
| D5 | JavaFX 场景图层级 | 第 4 章 | 解释 Stage/Scene/Node 关系 |
| D6 | FXML 加载流程图 | 第 4 章 | 解释 FXML → Controller 绑定 |
| D7 | BorderPane 五区布局图 | 第 5 章 | 解释主界面布局 |
| D8 | 布局选择决策树 | 第 5 章 | 帮助选择合适布局 |
| D9 | TableView 数据绑定流程图 | 第 6 章 | 解释数据流向 |
| D10 | 四层架构图 | 第 7 章 | 解释分层边界 |
| D11 | 请求数据流图 | 第 7 章 | 展示 UI → DB 数据流 |
| D12 | 事务执行流程图 | 第 8 章 | 展示 commit/rollback |
| D13 | HikariCP 连接池工作原理图 | 第 9 章 | 解释连接池 |
| D14 | BaseDAO 继承与调用关系图 | 第 9 章 | 解释 DAO 设计 |
| D15 | JavaFX UI 线程与后台线程交互图 | 第 10 章 | 解释 Task 模式 |
| D16 | 模块依赖与集成顺序图 | 第 11 章 | 展示联调顺序 |
| D17 | 代码审查流程图 | 第 11 章 | 展示审查步骤 |
