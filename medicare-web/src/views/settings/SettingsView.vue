<template>
  <div class="settings-view">
    <PageHeader title="系统设置" subtitle="用户管理与个人密码修改" />

    <el-card shadow="hover" class="data-card">
      <el-tabs v-model="activeTab" type="border-card">
        <!-- 用户管理 Tab（仅 admin 可见） -->
        <el-tab-pane v-if="userStore.hasRole('admin')" label="用户管理" name="users">
          <DataToolbar show-add add-label="新增用户" @add="openUserDialog()">
            <template #filters>
              <el-input v-model="userKeyword" placeholder="搜索用户名/姓名" clearable style="width:200px" @input="filterUsers" />
            </template>
          </DataToolbar>
          <el-table v-loading="userLoading" :data="filteredUserList" border stripe>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="username" label="用户名" width="120" />
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column prop="role" label="角色" width="100">
              <template #default="{ row }">
                <StatusTag :type="roleTagType(row.role)" :label="roleText(row.role)" />
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <StatusTag :type="row.status === 1 ? 'success' : 'danger'" :label="row.status === 1 ? '启用' : '禁用'" />
              </template>
            </el-table-column>
            <el-table-column prop="doctorId" label="关联医生ID" width="110">
              <template #default="{ row }">{{ row.doctorId || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right" align="center">
              <template #default="{ row }">
                <div class="action-buttons">
                  <el-button size="small" type="primary" @click="openUserDialog(row)">编辑</el-button>
                  <el-button size="small" type="danger" @click="handleDeleteUser(row)" :disabled="row.username === 'admin'">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <EmptyState v-if="!userLoading && filteredUserList.length === 0" icon="Setting" title="暂无用户数据" description="点击右上角“新增用户”按钮添加" />

          <!-- 用户编辑弹窗 -->
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
                <el-select v-model="userForm.role" placeholder="请选择角色" @change="onRoleChange">
                  <el-option label="管理员" value="admin" />
                  <el-option label="医生" value="doctor" />
                  <el-option label="药剂师" value="pharmacist" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="userForm.role === 'doctor'" label="关联医生" prop="doctorId">
                <el-select v-model="userForm.doctorId" placeholder="请选择关联医生" clearable>
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
        </el-tab-pane>

        <!-- 修改密码 Tab -->
        <el-tab-pane label="修改密码" name="password">
          <div class="password-form-wrapper">
            <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px" style="max-width: 420px">
              <el-divider content-position="left">修改密码</el-divider>
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
                <el-button type="primary" :loading="pwdLoading" @click="handleChangePassword">修改密码</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '../../stores/user'
import { listUsers, createUser, updateUser, deleteUser, updatePassword } from '../../api/user'
import { listDoctors } from '../../api/doctor'
import type { SysUser, Doctor } from '../../types'
import PageHeader from '../../components/PageHeader.vue'
import DataToolbar from '../../components/DataToolbar.vue'
import EmptyState from '../../components/EmptyState.vue'
import StatusTag from '../../components/StatusTag.vue'

const userStore = useUserStore()
const activeTab = ref(userStore.hasRole('admin') ? 'users' : 'password')

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
  role: 'admin' as string,
  status: 1,
  doctorId: null as number | null,
})

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

function filterUsers() {}

function roleText(role: string) {
  const map: Record<string, string> = { admin: '管理员', doctor: '医生', pharmacist: '药剂师' }
  return map[role] || role
}

function roleTagType(role: string): any {
  const map: Record<string, any> = { admin: 'danger', doctor: 'primary', pharmacist: 'success' }
  return map[role] || 'info'
}

function onRoleChange(val: string) {
  if (val !== 'doctor') userForm.doctorId = null
}

function openUserDialog(row?: SysUser) {
  isEdit.value = !!row
  if (row) {
    Object.assign(userForm, { id: row.id, username: row.username, password: '', realName: row.realName, role: row.role, status: row.status, doctorId: row.doctorId || null })
  } else {
    Object.assign(userForm, { id: 0, username: '', password: '', realName: '', role: 'admin', status: 1, doctorId: null })
  }
  userDialogVisible.value = true
}

async function handleSaveUser() {
  await userFormRef.value?.validate()
  userSaveLoading.value = true
  try {
    if (isEdit.value) {
      await updateUser(userForm.id, { ...userForm } as SysUser)
      ElMessage.success('用户更新成功')
    } else {
      await createUser({ ...userForm } as SysUser)
      ElMessage.success('用户创建成功')
    }
    userDialogVisible.value = false
    loadUsers()
  } catch {}
  userSaveLoading.value = false
}

async function handleDeleteUser(row: SysUser) {
  if (row.username === 'admin') return
  await ElMessageBox.confirm(`确定要删除用户 "${row.realName}" 吗？删除后不可恢复`, '删除确认', { type: 'warning' })
  try { await deleteUser(row.id); ElMessage.success('删除成功'); loadUsers() } catch {}
}

async function loadUsers() {
  userLoading.value = true
  try { const res = await listUsers(); userList.value = res.data } catch {}
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
  await pwdFormRef.value?.validate()
  pwdLoading.value = true
  try {
    const userId = userStore.currentUser?.id
    if (!userId) return
    await updatePassword(userId, { oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    userStore.clearUser()
    window.location.href = '/login'
  } catch {}
  pwdLoading.value = false
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
  animation: fadeIn 0.4s ease-out;
}
.data-card {
  border-radius: var(--radius-lg);
  overflow: hidden;
}
.password-form-wrapper {
  padding: 20px 0;
}
.action-buttons {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 4px;
}
</style>
