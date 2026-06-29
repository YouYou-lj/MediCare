# 数据库实体与数据模型

> **所属系统**: MediCare 智慧医疗门诊管理系统
> **文档类型**: RAG 检索文档 — 数据模型
> **数据库**: MySQL 8.0+ (utf8mb4_unicode_ci)

---

## 1. 实体关系总览

系统共包含 **15 个 JPA 实体**，对应 15 张数据库表：

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   department    │     │     doctor      │     │   sys_user      │
│   (科室)        │◄────│   (医生)        │     │   (系统用户)     │
└─────────────────┘     └────────┬────────┘     └─────────────────┘
                                 │
                                 ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│    patient      │     │    schedule     │◄────│  registration   │
│   (患者)        │     │   (排班号源)     │     │   (挂号)        │
└────────┬────────┘     └─────────────────┘     └────────┬────────┘
         │                                               │
         │         ┌─────────────────┐                   │
         └────────►│  medical_record │◄──────────────────┘
                   │   (病历)        │
                   └────────┬────────┘
                            │
                            ▼
                   ┌─────────────────┐     ┌─────────────────┐
                   │  prescription   │────►│ prescription_item│
                   │   (处方)        │     │   (处方明细)     │
                   └────────┬────────┘     └─────────────────┘
                            │
                            ▼
                   ┌─────────────────┐
                   │    medicine     │
                   │   (药品)        │◄────┐
                   └─────────────────┘     │
                                            │
                   ┌─────────────────┐     │
                   │  inventory_log  │─────┘
                   │ (库存变动日志)  │
                   └─────────────────┘

┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ ai_chat_session │────►│ ai_chat_message │     │knowledge_document│
│  (AI 会话)      │     │  (AI 消息)      │     │  (知识库文档)    │
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                                         │
                                                         ▼
                                                ┌─────────────────┐
                                                │ knowledge_chunk │
                                                │  (知识库分块)   │
                                                └─────────────────┘
```

---

## 2. 核心业务实体详解

### 2.1 Patient (患者)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 患者ID |
| id_card | VARCHAR(18) | NOT NULL, UNIQUE | 身份证号（唯一） |
| name | VARCHAR(50) | NOT NULL | 姓名 |
| gender | TINYINT | NOT NULL | 性别：0=女，1=男，2=其他 |
| birth_date | DATE | — | 出生日期 |
| phone | VARCHAR(20) | — | 手机号 |
| address | VARCHAR(200) | — | 住址 |
| allergy_info | VARCHAR(500) | — | 过敏信息 |
| create_time | DATETIME | DEFAULT | 建档时间 |
| update_time | DATETIME | ON UPDATE | 更新时间 |

**业务规则**: 身份证号全局唯一，新增/编辑时校验唯一性（编辑排除自身）。

---

### 2.2 Doctor (医生)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 医生ID |
| name | VARCHAR(50) | NOT NULL | 姓名 |
| department_id | BIGINT | NOT NULL | 所属科室ID |
| title | VARCHAR(30) | — | 职称 |
| status | TINYINT | NOT NULL, DEFAULT 1 | 状态：1=在职，0=离职 |
| create_time | DATETIME | DEFAULT | 创建时间 |
| update_time | DATETIME | ON UPDATE | 更新时间 |

**关联**: `department_id → department.id`

---

### 2.3 Department (科室)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 科室ID |
| name | VARCHAR(50) | NOT NULL | 科室名称 |
| location | VARCHAR(100) | — | 科室位置 |
| phone | VARCHAR(20) | — | 科室电话 |
| create_time | DATETIME | DEFAULT | 创建时间 |
| update_time | DATETIME | ON UPDATE | 更新时间 |

---

### 2.4 Schedule (排班号源)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 排班ID |
| doctor_id | BIGINT | NOT NULL | 医生ID |
| work_date | DATE | NOT NULL | 工作日期 |
| time_slot | VARCHAR(20) | NOT NULL | 时段（如"上午"、"下午"） |
| total_slots | INT | NOT NULL, DEFAULT 0 | 总号源数 |
| remain_slots | INT | NOT NULL, DEFAULT 0 | 剩余号源数 |
| create_time | DATETIME | DEFAULT | 创建时间 |
| update_time | DATETIME | ON UPDATE | 更新时间 |

**关联**: `doctor_id → doctor.id`

---

### 2.5 Registration (挂号)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 挂号ID |
| patient_id | BIGINT | NOT NULL, FK | 患者ID |
| schedule_id | BIGINT | NOT NULL, FK | 排班ID |
| doctor_id | BIGINT | — | 医生ID（V2 迁移新增） |
| reg_time | DATETIME | DEFAULT | 挂号时间 |
| status | TINYINT | NOT NULL, DEFAULT 0 | 状态：0=候诊，1=就诊中，2=已完成，3=已取消 |
| seq_no | INT | — | 当日序号 |
| fee | DECIMAL(10,2) | DEFAULT 0.00 | 挂号费 |
| create_time | DATETIME | DEFAULT | 创建时间 |
| update_time | DATETIME | ON UPDATE | 更新时间 |

**状态文本映射**:
- 0 → "候诊"
- 1 → "就诊中"
- 2 → "已完成"
- 3 → "已取消"

**关联**: `patient_id → patient.id`, `schedule_id → schedule.id`, `doctor_id → doctor.id`

---

### 2.6 MedicalRecord (病历)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 病历ID |
| registration_id | BIGINT | NOT NULL, FK | 挂号ID |
| patient_id | BIGINT | NOT NULL, FK | 患者ID |
| doctor_id | BIGINT | NOT NULL, FK | 医生ID |
| chief_complaint | VARCHAR(500) | — | 主诉 |
| present_illness | TEXT | — | 现病史 |
| past_history | VARCHAR(1000) | — | 既往史 |
| physical_exam | VARCHAR(1000) | — | 体格检查 |
| diagnosis | VARCHAR(500) | — | 诊断 |
| advice | VARCHAR(1000) | — | 医嘱 |
| create_time | DATETIME | DEFAULT | 创建时间 |
| update_time | DATETIME | ON UPDATE | 更新时间 |

**关联**: `registration_id → registration.id`, `patient_id → patient.id`, `doctor_id → doctor.id`

---

### 2.7 Prescription (处方)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 处方ID |
| record_id | BIGINT | NOT NULL, FK | 病历ID |
| patient_id | BIGINT | NOT NULL, FK | 患者ID |
| doctor_id | BIGINT | NOT NULL, FK | 医生ID |
| total_amount | DECIMAL(10,2) | DEFAULT 0.00 | 总金额 |
| status | TINYINT | NOT NULL, DEFAULT 0 | 状态：0=待缴费，1=已缴费，2=已取药，3=已作废 |
| create_time | DATETIME | DEFAULT | 创建时间 |
| update_time | DATETIME | ON UPDATE | 更新时间 |

**状态常量**:
- `STATUS_PENDING = 0` (待缴费)
- `STATUS_PAID = 1` (已缴费)
- `STATUS_DISPENSED = 2` (已取药)
- `STATUS_CANCELLED = 3` (已作废)

**关联**: `record_id → medical_record.id`

---

### 2.8 PrescriptionItem (处方明细)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 明细ID |
| prescription_id | BIGINT | NOT NULL, FK | 处方ID |
| medicine_id | BIGINT | NOT NULL, FK | 药品ID |
| quantity | INT | NOT NULL, DEFAULT 1 | 数量 |
| dosage | VARCHAR(200) | — | 剂量 |
| usage_desc | VARCHAR(200) | — | 用法说明 |
| unit_price | DECIMAL(10,2) | NOT NULL, DEFAULT 0.00 | 单价 |
| amount | DECIMAL(10,2) | NOT NULL, DEFAULT 0.00 | 金额（= quantity × unit_price） |
| create_time | DATETIME | DEFAULT | 创建时间 |

**关联**: `prescription_id → prescription.id`, `medicine_id → medicine.id`

---

### 2.9 Medicine (药品)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 药品ID |
| name | VARCHAR(100) | NOT NULL | 药品名称 |
| spec | VARCHAR(100) | — | 规格 |
| unit | VARCHAR(20) | — | 单位 |
| stock | INT | NOT NULL, DEFAULT 0 | 当前库存 |
| safety_stock | INT | NOT NULL, DEFAULT 10 | 安全库存 |
| expiry_date | DATE | — | 有效期 |
| batch_no | VARCHAR(50) | — | 批号 |
| pinyin_code | VARCHAR(50) | — | 拼音简码 |
| price | DECIMAL(10,2) | DEFAULT 0.00 | 零售价 |
| manufacturer | VARCHAR(200) | — | 生产厂家 |
| status | TINYINT | NOT NULL, DEFAULT 1 | 状态：1=正常，0=停用 |
| create_time | DATETIME | DEFAULT | 创建时间 |
| update_time | DATETIME | ON UPDATE | 更新时间 |

**唯一约束**: `UNIQUE KEY uk_medicine_name_spec (name, spec)`

**库存预警**: `stock <= safety_stock` 时标红显示。

---

### 2.10 InventoryLog (库存变动日志)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 日志ID |
| medicine_id | BIGINT | NOT NULL, FK | 药品ID |
| type | TINYINT | NOT NULL | 类型：1=入库，2=出库，3=盘盈，4=盘亏 |
| quantity | INT | NOT NULL, DEFAULT 0 | 变动数量 |
| batch_no | VARCHAR(50) | — | 批号 |
| expiry_date | DATE | — | 有效期 |
| operator | VARCHAR(50) | — | 操作人 |
| remark | VARCHAR(500) | — | 备注 |
| log_time | DATETIME | DEFAULT | 记录时间 |

**类型常量**:
- `TYPE_STOCK_IN = 1` (入库)
- `TYPE_STOCK_OUT = 2` (出库)
- `TYPE_SURPLUS = 3` (盘盈)
- `TYPE_LOSS = 4` (盘亏)

---

### 2.11 SysUser (系统用户)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 用户ID |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 用户名 |
| password | VARCHAR(100) | NOT NULL | 密码（BCrypt 加密） |
| real_name | VARCHAR(50) | — | 真实姓名 |
| role | VARCHAR(20) | DEFAULT 'doctor' | 角色：admin/doctor/pharmacist |
| status | TINYINT | NOT NULL, DEFAULT 1 | 状态：1=正常，0=禁用 |
| doctor_id | BIGINT | — | 关联医生ID |
| create_time | DATETIME | DEFAULT | 创建时间 |
| update_time | DATETIME | ON UPDATE | 更新时间 |

**角色常量**:
- `ROLE_ADMIN = "admin"`
- `ROLE_DOCTOR = "doctor"`
- `ROLE_PHARMACIST = "pharmacist"`

---

### 2.12 AI & 知识库实体

#### AiChatSession (AI 会话)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 会话ID |
| session_key | VARCHAR(64) | NOT NULL, UNIQUE | 会话唯一标识 |
| user_id | BIGINT | NOT NULL | 用户ID |
| title | VARCHAR(200) | — | 会话标题 |
| create_time | DATETIME | DEFAULT | 创建时间 |
| update_time | DATETIME | ON UPDATE | 更新时间 |

#### AiChatMessage (AI 消息)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 消息ID |
| session_id | BIGINT | NOT NULL | 会话ID |
| role | VARCHAR(20) | NOT NULL | 角色：user / assistant |
| content | TEXT | NOT NULL | 消息内容 |
| references_json | LONGTEXT | — | 引用来源 JSON |
| create_time | DATETIME | DEFAULT | 创建时间 |

#### KnowledgeDocument (知识库文档)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 文档ID |
| title | VARCHAR(500) | NOT NULL | 文档标题 |
| source_type | VARCHAR(50) | NOT NULL | 来源类型：markdown/html/text |
| source_path | VARCHAR(500) | NOT NULL, UNIQUE | 来源路径 |
| content_hash | VARCHAR(64) | NOT NULL | 内容哈希（SHA-256） |
| chunk_count | INT | NOT NULL, DEFAULT 0 | 分块数量 |
| status | INT | NOT NULL, DEFAULT 1 | 状态：1=正常 |
| create_time | DATETIME | DEFAULT | 创建时间 |
| update_time | DATETIME | ON UPDATE | 更新时间 |

#### KnowledgeChunk (知识库分块)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 分块ID |
| document_id | BIGINT | NOT NULL | 文档ID |
| chunk_index | INT | NOT NULL | 分块序号 |
| title | VARCHAR(500) | NOT NULL | 分块标题 |
| content | LONGTEXT | NOT NULL | 分块内容 |
| keywords | VARCHAR(1000) | — | 关键词 |
| embedding | LONGTEXT | — | 向量嵌入（JSON 数组） |
| embedding_model | VARCHAR(100) | — | 嵌入模型名称 |
| source_path | VARCHAR(500) | NOT NULL | 来源路径 |
| create_time | DATETIME | DEFAULT | 创建时间 |

---

## 3. 数据库索引设计

### 3.1 外键索引

| 表 | 字段 | 索引名 |
|----|------|--------|
| registration | patient_id | fk_reg_patient |
| registration | schedule_id | fk_reg_schedule |
| registration | doctor_id | idx_reg_doctor (V2 新增) |
| medical_record | registration_id | fk_mr_registration |
| medical_record | patient_id | fk_mr_patient |
| medical_record | doctor_id | fk_mr_doctor |
| prescription | record_id | — |
| prescription | patient_id | — |
| prescription_item | prescription_id | — |
| prescription_item | medicine_id | — |
| inventory_log | medicine_id | — |
| schedule | doctor_id | — |
| doctor | department_id | — |
| sys_user | doctor_id | — |

### 3.2 唯一索引

| 表 | 字段 | 索引名 |
|----|------|--------|
| patient | id_card | uk_patient_id_card |
| medicine | (name, spec) | uk_medicine_name_spec |
| sys_user | username | — |
| knowledge_document | source_path | — |
| ai_chat_session | session_key | — |

---

## 4. V2 数据库迁移

`migration_v2.sql` 执行的迁移操作：

1. `ALTER TABLE registration ADD COLUMN doctor_id BIGINT UNSIGNED` — 添加医生ID字段
2. `UPDATE registration ... SET doctor_id = s.doctor_id` — 回填已有记录的 doctor_id
3. `ALTER TABLE registration ADD KEY idx_reg_doctor (doctor_id)` — 添加索引
4. `ALTER TABLE registration ADD CONSTRAINT fk_reg_doctor ...` — 添加外键约束
5. 密码迁移由 `DataMigrationRunner` 在启动时自动完成（将明文密码转为 BCrypt）
