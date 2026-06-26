<template>
  <div class="settings-view">
    <PageHeader title="系统设置" subtitle="用户管理与个人账户" />

    <div class="settings-view__workspace">
      <el-tabs v-model="activeTab" class="settings-view__tabs" type="border-card">
        <!-- 用户管理：仅管理员可见 -->
        <el-tab-pane v-if="userStore.hasRole('admin')" label="用户管理" name="users">
          <div class="settings-view__tab-content">
            <el-card shadow="hover" class="settings-view__panel settings-view__panel--users">
              <template #header>
                <div class="settings-view__panel-header">
                  <div class="settings-view__panel-title">
                    <el-icon><User /></el-icon>
                    <span>用户管理</span>
                  </div>
                  <span class="settings-view__panel-meta">共 {{ userList.length }} 个账号</span>
                </div>
              </template>

              <div class="settings-view__stats">
                <div class="settings-view__stat">
                  <span class="settings-view__stat-label">启用账号</span>
                  <strong>{{ enabledUserCount }}</strong>
                </div>
                <div class="settings-view__stat">
                  <span class="settings-view__stat-label">医生账号</span>
                  <strong>{{ doctorUserCount }}</strong>
                </div>
                <div class="settings-view__stat">
                  <span class="settings-view__stat-label">药师账号</span>
                  <strong>{{ pharmacistUserCount }}</strong>
                </div>
              </div>

              <DataToolbar show-refresh show-add add-label="新增用户" @refresh="loadUsers" @add="openUserDialog()">
                <template #filters>
                  <el-input v-model="userKeyword" placeholder="搜索用户名/姓名" clearable class="settings-view__filter-input" @input="filterUsers" />
                </template>
              </DataToolbar>

              <div class="settings-view__table-wrap">
                <el-table v-loading="userLoading" :data="filteredUserList" border stripe height="100%" :default-sort="{ prop: 'code', order: 'ascending' }" @row-click="openPreviewDialog">
                  <template #empty>
                    <EmptyState icon="Setting" title="暂无用户数据" description="点击右上角“新增用户”按钮添加" />
                  </template>
                  <el-table-column type="index" label="序号" width="60" align="center" fixed="left" :resizable="false" />
                  <el-table-column prop="code" label="ID" width="120" align="center" fixed="left" :resizable="false" />
                  <el-table-column prop="username" label="用户名" min-width="130" align="center" fixed="left" :resizable="false" />
                  <el-table-column prop="realName" label="姓名" min-width="140" align="center" :resizable="false" />
                  <el-table-column prop="role" label="角色" min-width="140" align="center" :resizable="false">
                    <template #default="{ row }">
                      <div class="settings-view__role-cell">
                        <StatusTag :type="roleTagType(row.role)" :label="roleDisplay(row)" />
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="status" label="状态" min-width="100" align="center" class-name="settings-view__tag-cell" :resizable="false">
                    <template #default="{ row }">
                      <StatusTag :type="row.status === 1 ? 'success' : 'danger'" :label="row.status === 1 ? '启用' : '禁用'" />
                    </template>
                  </el-table-column>
                  <el-table-column prop="doctorId" label="关联医生ID" min-width="120" align="center" :resizable="false">
                    <template #default="{ row }">{{ row.doctorId || '-' }}</template>
                  </el-table-column>
                  <el-table-column label="操作" width="160" fixed="right" align="center" :resizable="false">
                    <template #default="{ row }">
                      <div class="settings-view__actions">
                        <el-button size="small" type="primary" @click.stop="openUserDialog(row)" :disabled="!canEditUser(row)">编辑</el-button>
                        <el-button size="small" type="danger" @click.stop="handleDeleteUser(row)" :disabled="!canDeleteUser(row)">删除</el-button>
                      </div>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-card>
          </div>
        </el-tab-pane>

        <!-- 个人账户：所有角色可见 -->
        <el-tab-pane label="个人账户" name="profile">
          <div class="settings-view__tab-content settings-view__tab-content--profile">
            <el-card shadow="hover" class="settings-view__panel settings-view__profile">
              <template #header>
                <div class="settings-view__panel-header">
                  <div class="settings-view__panel-title">
                    <el-icon><Setting /></el-icon>
                    <span>当前账号</span>
                  </div>
                  <StatusTag :type="userStore.currentUser?.status === 1 ? 'success' : 'info'" :label="userStore.currentUser?.status === 1 ? '启用' : '未知'" />
                </div>
              </template>

              <div class="settings-view__profile-grid">
                <div class="settings-view__profile-item">
                  <span>用户名</span>
                  <strong>{{ userStore.currentUser?.username || '-' }}</strong>
                </div>
                <div class="settings-view__profile-item">
                  <span>姓名</span>
                  <strong>{{ userStore.currentUser?.realName || '-' }}</strong>
                </div>
                <div class="settings-view__profile-item">
                  <span>角色</span>
                  <StatusTag :type="roleTagType(userStore.currentUser?.role || '')" :label="currentRoleDisplay" />
                </div>
                <div class="settings-view__profile-item">
                  <span>关联医生</span>
                  <strong>{{ userStore.currentUser?.doctorId || '-' }}</strong>
                </div>
              </div>
            </el-card>

            <el-card shadow="hover" class="settings-view__panel settings-view__password">
              <template #header>
                <div class="settings-view__panel-header">
                  <div class="settings-view__panel-title">
                    <el-icon><Lock /></el-icon>
                    <span>修改密码</span>
                  </div>
                  <span class="settings-view__panel-meta">账号安全</span>
                </div>
              </template>

              <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px" class="settings-view__password-form">
                <el-form-item label="当前密码" prop="oldPassword">
                  <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码（至少6位）" />
                </el-form-item>
                <el-form-item label="确认新密码" prop="confirmPassword">
                  <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :icon="Key" :loading="pwdLoading" @click="handleChangePassword">修改密码</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="userDialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="480px" destroy-on-close>
      <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="isEdit" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="userForm.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="userForm.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" placeholder="请选择角色" class="settings-view__form-control" @change="onRoleChange">
            <el-option v-if="isCurrentUserMainAdmin" label="管理员" value="admin" />
            <el-option label="医生" value="doctor" />
            <el-option label="药剂师" value="pharmacist" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="userForm.role === 'doctor'" label="关联医生" prop="doctorId">
          <el-select v-model="userForm.doctorId" placeholder="请选择关联医生" clearable class="settings-view__form-control">
            <el-option v-for="d in doctorOptions" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="userSaveLoading" @click="handleSaveUser">保存</el-button>
      </template>
    </el-dialog>

    <!-- 用户预览弹窗 -->
    <el-dialog
      v-model="previewVisible"
      title="用户详情"
      width="520px"
      align-center
      class="settings-view__preview-dialog"
      destroy-on-close
    >
      <div v-if="previewUser" class="settings-view__preview-body">
        <div class="settings-view__preview-header">
          <div class="settings-view__preview-avatar">
            <el-icon :size="40"><User /></el-icon>
          </div>
          <div class="settings-view__preview-info">
            <strong>{{ previewUser.realName || '-' }}</strong>
            <span>{{ previewUser.username }}</span>
          </div>
          <StatusTag :type="roleTagType(previewUser.role)" :label="roleDisplay(previewUser)" />
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户ID">{{ previewUser.code || previewUser.id }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ previewUser.username }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ previewUser.realName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ roleDisplay(previewUser) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :type="previewUser.status === 1 ? 'success' : 'danger'" :label="previewUser.status === 1 ? '启用' : '禁用'" />
          </el-descriptions-item>
          <el-descriptions-item label="关联医生ID">{{ previewUser.doctorId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(previewUser.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatTime(previewUser.updateTime) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Key, Lock, Setting, User } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { useUserStore } from '../../stores/user'
import { listUsers, createUser, updateUser, deleteUser, updatePassword } from '../../api/user'
import { listDoctors } from '../../api/doctor'
import type { SysUser, Doctor } from '../../types'
import PageHeader from '../../components/PageHeader.vue'
import DataToolbar from '../../components/DataToolbar.vue'
import EmptyState from '../../components/EmptyState.vue'
import StatusTag from '../../components/StatusTag.vue'

const userStore = useUserStore()

// ========== 标签页 ==========
const activeTab = ref(userStore.hasRole('admin') ? 'users' : 'profile')

function switchToProfile() {
  activeTab.value = 'profile'
}

// ========== 用户管理 ==========
const userList = ref<SysUser[]>([])
const userKeyword = ref('')
const userLoading = ref(false)
const userDialogVisible = ref(false)
const isEdit = ref(false)
const userFormRef = ref<FormInstance>()
const userSaveLoading = ref(false)
const doctorOptions = ref<Doctor[]>([])

const userForm = reactive({
  id: 0,
  username: '',
  password: '',
  realName: '',
  role: 'doctor' as string,
  status: 1,
  doctorId: null as number | null,
})

const previewVisible = ref(false)
const previewUser = ref<SysUser | null>(null)

function openPreviewDialog(row: SysUser) {
  previewUser.value = row
  previewVisible.value = true
}

function formatTime(value?: string) {
  if (!value) return '-'
  const time = dayjs(value)
  return time.isValid() ? time.format('YYYY-MM-DD HH:mm:ss') : value
}

const userRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

const filteredUserList = computed(() => {
  if (!userKeyword.value) return userList.value
  const k = userKeyword.value.toLowerCase()
  return userList.value.filter(u => u.username.toLowerCase().includes(k) || u.realName.toLowerCase().includes(k))
})
const enabledUserCount = computed(() => userList.value.filter(user => user.status === 1).length)
const doctorUserCount = computed(() => userList.value.filter(user => user.role === 'doctor').length)
const pharmacistUserCount = computed(() => userList.value.filter(user => user.role === 'pharmacist').length)
const isCurrentUserMainAdmin = computed(() => userStore.currentUser?.id === 1)
const currentRoleDisplay = computed(() => userStore.currentUser ? roleDisplay(userStore.currentUser) : '-')

function isMainAdminUser(user?: SysUser) {
  return user?.id === 1
}

function isCurrentUser(user?: SysUser) {
  return user?.id === userStore.currentUser?.id
}

function roleDisplay(user?: SysUser) {
  if (!user) return '-'
  if (isMainAdminUser(user)) return '主管理员'
  const map: Record<string, string> = { admin: '管理员', doctor: '医生', pharmacist: '药剂师' }
  return map[user.role] || user.role
}

function roleTagType(role: string): any {
  const map: Record<string, any> = { admin: 'danger', doctor: 'primary', pharmacist: 'success' }
  return map[role] || 'info'
}

function canEditUser(row: SysUser) {
  if (isCurrentUserMainAdmin.value) return true
  return !isMainAdminUser(row) && row.role !== 'admin'
}

function canDeleteUser(row: SysUser) {
  if (isCurrentUserMainAdmin.value) return true
  return !isMainAdminUser(row) && row.role !== 'admin'
}

function filterUsers() {}

function onRoleChange(val: string) {
  if (val !== 'doctor') userForm.doctorId = null
}

function openUserDialog(row?: SysUser) {
  isEdit.value = !!row
  if (row) {
    Object.assign(userForm, { id: row.id, username: row.username, password: '', realName: row.realName, role: row.role, status: row.status, doctorId: row.doctorId || null })
  } else {
    Object.assign(userForm, { id: 0, username: '', password: '', realName: '', role: 'doctor', status: 1, doctorId: null })
  }
  userDialogVisible.value = true
}

async function handleSaveUser() {
  const valid = await userFormRef.value?.validate().catch(() => false)
  if (!valid) return
  userSaveLoading.value = true
  try {
    if (isEdit.value) {
      await updateUser(userForm.id, { ...userForm } as SysUser)
      ElMessage.success('用户更新成功')
      if (userForm.id === userStore.currentUser?.id) {
        await userStore.syncFromServer(true)
      }
    } else {
      await createUser({ ...userForm } as SysUser)
      ElMessage.success('用户创建成功')
    }
    userDialogVisible.value = false
    loadUsers()
  } catch {} finally {
    userSaveLoading.value = false
  }
}

async function handleDeleteUser(row: SysUser) {
  if (row.username === 'admin') return
  try {
    await ElMessageBox.confirm(`确定要删除用户 "${row.realName}" 吗？删除后不可恢复`, '删除确认', { type: 'warning' })
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    loadUsers()
  } catch {}
}

async function loadUsers() {
  userLoading.value = true
  try { const res = await listUsers(); userList.value = res.data.sort((a, b) => (a.id ?? 0) - (b.id ?? 0)) } catch {}
  userLoading.value = false
}

async function loadDoctors() {
  try { const res = await listDoctors(); doctorOptions.value = res.data } catch {}
}

// ========== 修改密码 ==========
const pwdFormRef = ref<FormInstance>()
const pwdLoading = ref(false)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次密码输入不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

async function handleChangePassword() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  pwdLoading.value = true
  try {
    const userId = userStore.currentUser?.id
    if (!userId) return
    await updatePassword(userId, { oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    userStore.clearUser()
    window.location.href = '/login'
  } catch {} finally {
    pwdLoading.value = false
  }
}

onMounted(() => {
  if (userStore.hasRole('admin')) {
    loadUsers()
    loadDoctors()
  }
})
</script>

<style scoped>
.settings-view {
  height: max(560px, calc(100vh - var(--header-height) - var(--content-padding) * 2));
  display: flex;
  flex-direction: column;
  min-width: 0;
  animation: fadeIn 0.4s ease-out;
}

.settings-view__workspace {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.settings-view__tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.settings-view__tabs :deep(.el-tabs__header) {
  flex-shrink: 0;
  margin-bottom: 0;
}

.settings-view__tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.settings-view__tabs :deep(.el-tab-pane) {
  height: 100%;
  min-height: 0;
}

.settings-view__tab-content {
  height: 100%;
  min-height: 0;
  padding: var(--space-lg);
  box-sizing: border-box;
}

.settings-view__tab-content--profile {
  display: grid;
  grid-template-columns: minmax(340px, 0.5fr) minmax(340px, 0.5fr);
  gap: var(--space-xl);
  align-content: start;
}

.settings-view__panel {
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-card);
  overflow: hidden;
}

.settings-view__panel :deep(.el-card__header) {
  flex-shrink: 0;
}

.settings-view__panel :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: var(--space-lg);
}

.settings-view__panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
}

.settings-view__panel-title {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  color: var(--text-primary);
  font-weight: 600;
}

.settings-view__panel-meta {
  color: var(--text-muted);
  font-size: var(--font-size-sm);
  white-space: nowrap;
}

.settings-view__stats {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}

.settings-view__stat {
  min-width: 0;
  padding: var(--space-md);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-table);
  background: var(--bg-toolbar);
}

.settings-view__stat-label {
  display: block;
  margin-bottom: var(--space-xs);
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}

.settings-view__stat strong {
  color: var(--text-primary);
  font-size: var(--font-size-2xl);
  line-height: 1.2;
}

.settings-view__table-wrap {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.settings-view__table-wrap :deep(.el-table) {
  width: 100%;
}

.settings-view__actions {
  display: flex;
  flex-wrap: nowrap;
  justify-content: center;
  align-items: center;
  gap: 0;
  white-space: nowrap;
}

.settings-view__filter-input {
  width: 220px;
  flex: 0 0 220px;
}

.settings-view__profile {
  height: auto;
  flex: 0 0 auto;
}

.settings-view__password {
  height: auto;
  flex: 0 0 auto;
}

.settings-view__profile-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-md);
}

.settings-view__profile-item {
  min-width: 0;
  padding: var(--space-md);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-table);
  background: var(--bg-toolbar);
}

.settings-view__profile-item span {
  display: block;
  margin-bottom: var(--space-xs);
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}

.settings-view__profile-item strong {
  color: var(--text-primary);
  font-size: var(--font-size-md);
  line-height: 1.4;
  word-break: break-word;
}

.settings-view__password-form {
  max-width: 460px;
}

.settings-view :deep(.settings-view__form-control) {
  width: 100%;
}

.settings-view :deep(.settings-view__tag-cell .cell) {
  overflow: visible;
  text-overflow: clip;
  white-space: nowrap;
}

.settings-view__role-cell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);
}

/* 用户预览弹窗：固定高度并垂直居中 */
.settings-view__preview-dialog :deep(.el-dialog__body) {
  height: 360px;
  overflow-y: auto;
  padding: var(--space-lg);
}

.settings-view__preview-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.settings-view__preview-header {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-card);
  background: var(--bg-toolbar);
}

.settings-view__preview-avatar {
  width: 56px;
  height: 56px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-circle);
  color: var(--color-primary);
  background: var(--color-primary-light);
  flex-shrink: 0;
}

.settings-view__preview-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.settings-view__preview-info strong {
  color: var(--text-primary);
  font-size: var(--font-size-lg);
  line-height: 1.3;
}

.settings-view__preview-info span {
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

@media (max-width: 1180px) {
  .settings-view {
    height: auto;
    min-height: max(560px, calc(100vh - var(--header-height) - var(--content-padding) * 2));
  }

  .settings-view__tab-content--profile {
    grid-template-columns: 1fr;
  }

  .settings-view__panel--users {
    min-height: 620px;
  }

  .settings-view__table-wrap {
    flex: none;
    height: clamp(360px, 46vh, 560px);
    min-height: 0;
  }

  .settings-view__password {
    flex: none;
  }
}

@media (max-width: 768px) {
  .settings-view__stats,
  .settings-view__profile-grid {
    grid-template-columns: 1fr;
  }

  .settings-view__table-wrap {
    height: 320px;
  }

  .settings-view__filter-input {
    width: 100%;
    flex-basis: auto;
  }

  .settings-view__password-form {
    max-width: none;
  }
}
</style>
