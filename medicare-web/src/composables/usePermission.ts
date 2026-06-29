import { computed } from 'vue'
import { useUserStore } from '../stores/user'
import type { SysUser } from '../types'

export interface KnowledgeRow {
  isSystem?: boolean
  uploadedBy?: number | null
}

export function usePermission() {
  const userStore = useUserStore()

  const user = computed(() => userStore.currentUser)
  const role = computed(() => userStore.currentUser?.role || '')
  const userId = computed(() => userStore.currentUser?.id)

  const isMainAdmin = computed(() => userId.value === 1)
  const isAdmin = computed(() => role.value === 'admin')
  const isDoctor = computed(() => role.value === 'doctor')
  const isPharmacist = computed(() => role.value === 'pharmacist')

  /** 基础数据：仅主管理员可维护 */
  const canManageBasicData = computed(() => isMainAdmin.value)

  /** 用户管理：管理员及以上可进入，但普通管理员不能管理主管理员和其他管理员 */
  const canManageUsers = computed(() => isAdmin.value)

  function canEditUser(target?: SysUser | { id?: number; role?: string }) {
    if (!target?.id) return false
    if (isMainAdmin.value) return true
    if (!isAdmin.value) return false
    // 普通管理员可以管理除主管理员外的所有用户（包括其他普通管理员），但主管理员不可被管理
    return target.id !== 1
  }

  const canDeleteUser = canEditUser

  /** 知识库管理：主管理员可操作全部；普通管理员可操作非系统文件；医生/药剂师仅可查看 */
  const canManageKnowledge = computed(() => isMainAdmin.value || isAdmin.value)

  function canManageKnowledgeRow(row?: KnowledgeRow) {
    if (!row) return false
    if (isMainAdmin.value) return true
    if (row.isSystem) return false
    if (isAdmin.value) return true
    return false
  }

  /** 知识库上传：所有已登录用户均可上传自己的文件 */
  const canUploadKnowledge = computed(() => !!userStore.currentUser)

  /** 重建全部索引：仅主管理员 */
  const canReindexKnowledge = computed(() => isMainAdmin.value)

  /** 患者管理：管理员、医生可维护；药剂师仅查看 */
  const canManagePatients = computed(() => isAdmin.value || isDoctor.value)

  /** 挂号预约：管理员可操作；医生、药剂师仅查看 */
  const canManageRegistration = computed(() => isAdmin.value)

  /** 医生工作站：管理员、医生可操作；药剂师仅查看 */
  const canUseWorkstation = computed(() => isAdmin.value || isDoctor.value)

  /** 病历管理：管理员、医生可维护；药剂师仅查看 */
  const canManageMedicalRecords = computed(() => isAdmin.value || isDoctor.value)

  /** 药品库存：管理员、药剂师可维护；医生仅查看 */
  const canManagePharmacy = computed(() => isAdmin.value || isPharmacist.value)

  /** 处方管理：管理员、药剂师可维护；医生仅查看 */
  const canManagePrescriptions = computed(() => isAdmin.value || isPharmacist.value)

  return {
    user,
    role,
    userId,
    isMainAdmin,
    isAdmin,
    isDoctor,
    isPharmacist,
    canManageBasicData,
    canManageUsers,
    canEditUser,
    canDeleteUser,
    canManageKnowledge,
    canManageKnowledgeRow,
    canUploadKnowledge,
    canReindexKnowledge,
    canManagePatients,
    canManageRegistration,
    canUseWorkstation,
    canManageMedicalRecords,
    canManagePharmacy,
    canManagePrescriptions,
  }
}
