// ========== 通用响应 ==========
export interface Result<T> {
  code: number
  message: string
  data: T
}

// ========== 用户与认证 ==========
export interface SysUser {
  id: number
  code?: string
  username: string
  password?: string
  realName: string
  role: 'admin' | 'doctor' | 'pharmacist'
  status: number
  doctorId?: number | null
  createTime?: string
  updateTime?: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  user: SysUser
  token?: string
}

// ========== 科室 ==========
export interface Department {
  id?: number
  code?: string
  name: string
  location?: string
  phone?: string
  createTime?: string
  updateTime?: string
}

// ========== 医生 ==========
export interface Doctor {
  id?: number
  code?: string
  name: string
  departmentId: number
  title?: string
  status: number
  departmentName?: string
  createTime?: string
  updateTime?: string
}

// ========== 患者 ==========
export interface Patient {
  id?: number
  code?: string
  idCard: string
  name: string
  gender: number
  birthDate?: string | null
  phone?: string
  address?: string
  allergyInfo?: string
  createTime?: string
  updateTime?: string
}

// ========== 排班 ==========
export interface Schedule {
  id?: number
  code?: string
  doctorId: number
  workDate: string
  timeSlot: string
  totalSlots: number
  remainSlots: number
  doctorName?: string
  departmentName?: string
  createTime?: string
  updateTime?: string
}

// ========== 挂号 ==========
export interface Registration {
  id?: number
  code?: string
  patientId: number
  scheduleId: number
  doctorId?: number
  regTime?: string
  status: number
  seqNo?: number
  fee?: number
  patientName?: string
  doctorName?: string
  departmentName?: string
  timeSlot?: string
  createTime?: string
  updateTime?: string
}

// ========== 病历 ==========
export interface MedicalRecord {
  id?: number
  code?: string
  registrationId: number
  patientId: number
  doctorId: number
  chiefComplaint?: string
  presentIllness?: string
  pastHistory?: string
  physicalExam?: string
  diagnosis?: string
  advice?: string
  patientName?: string
  doctorName?: string
  createTime?: string
  updateTime?: string
}

// ========== 药品 ==========
export interface Medicine {
  id?: number
  code?: string
  name: string
  spec?: string
  unit?: string
  stock: number
  safetyStock: number
  expiryDate?: string
  batchNo?: string
  pinyinCode?: string
  price?: number
  manufacturer?: string
  status: number
  createTime?: string
  updateTime?: string
}

// ========== 处方 ==========
export interface Prescription {
  id?: number
  code?: string
  recordId: number
  patientId: number
  doctorId: number
  totalAmount?: number
  status: number
  patientName?: string
  doctorName?: string
  items?: PrescriptionItem[]
  createTime?: string
  updateTime?: string
}

export interface PrescriptionItem {
  id?: number
  code?: string
  prescriptionId?: number
  medicineId: number
  quantity: number
  dosage?: string
  usageDesc?: string
  unitPrice?: number
  amount?: number
  medicineName?: string
  medicineSpec?: string
  medicineUnit?: string
  medicineCode?: string
}

// ========== 库存日志 ==========
export interface InventoryLog {
  id?: number
  medicineId: number
  type: number
  quantity: number
  batchNo?: string
  expiryDate?: string
  operator?: string
  remark?: string
  logTime?: string
  medicineName?: string
}

// ========== 仪表盘 ==========
export interface DashboardStats {
  todayRegCount: number
  waitingCount: number
  stockAlertCount: number
}

// ========== 请求参数 ==========
export interface StockRequest {
  quantity: number
  batchNo?: string
  expiryDate?: string
  operator?: string
  remark?: string
}

// ========== AI 助手 ==========
export interface AiChatContext {
  route?: string
  routeName?: string
  [key: string]: unknown
}

export interface AiChatRequest {
  message: string
  sessionId?: string
  context?: AiChatContext
  fileSourcePath?: string
}

export interface AiReference {
  type?: string
  id?: string
  title?: string
  sourcePath?: string
  content?: string
}

export interface AiAction {
  label?: string
  type?: string
  target?: string
}

export interface AiChatResponse {
  answer: string
  sessionId?: string
  provider: string
  model: string
  references: AiReference[]
  actions: AiAction[]
}

export interface AiChatSession {
  id: number
  sessionKey: string
  title: string
  createTime?: string
  updateTime?: string
}

export interface AiChatMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  references?: AiReference[]
  createTime?: string
}

export interface RagQueryRequest {
  question: string
  topK?: number
}

export interface RagReindexResponse {
  documentCount: number
  chunkCount: number
  message: string
}

export interface KnowledgeUploadResponse {
  filename: string
  sourcePath: string
  sourceType: string
  chunkCount: number
  message: string
}

export interface KnowledgeSystemUploadBatchResponse {
  totalCount: number
  successCount: number
  failCount: number
  totalChunkCount: number
  message: string
  files: KnowledgeUploadResponse[]
}

export interface KnowledgeDocumentResponse {
  id: number
  filename: string
  sourcePath: string
  sourceType: string
  chunkCount: number
  status: number
  isSystem?: boolean
  createTime?: string
  updateTime?: string
}
