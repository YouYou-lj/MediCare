<template>
  <div class="patient-list">
    <PageHeader title="患者管理" subtitle="患者建档、信息查询与编辑" />

    <el-card shadow="hover" class="patient-list__card">
      <DataToolbar
        v-model:searchModelValue="keyword"
        search-placeholder="搜索姓名/身份证/手机号"
        show-refresh
        show-add
        add-label="新增患者"
        @search="handleSearch"
        @refresh="loadData"
        @add="openDialog()"
      />

      <div class="patient-list__table-wrap">
        <el-table
          v-loading="loading"
          :data="tableData"
          stripe
          border
          height="100%"
          :default-sort="{ prop: 'code', order: 'ascending' }"
        >
          <template #empty>
            <EmptyState
              icon="User"
              title="暂无患者数据"
              description="点击右上角“新增患者”按钮开始建档"
            />
          </template>
          <el-table-column type="index" label="序号" width="60" align="center" :resizable="false" />
          <el-table-column prop="code" label="ID" width="120" align="center" :resizable="false" />
          <el-table-column prop="name" label="姓名" min-width="100" align="center" :resizable="false" />
          <el-table-column prop="idCard" label="身份证号" min-width="180" align="center" :resizable="false" />
          <el-table-column prop="gender" label="性别" min-width="70" align="center" :resizable="false">
            <template #default="{ row }">{{ ['女','男','其他'][row.gender] || '未知' }}</template>
          </el-table-column>
          <el-table-column prop="birthDate" label="出生日期" min-width="120" align="center" :resizable="false" />
          <el-table-column prop="phone" label="手机号" min-width="130" align="center" :resizable="false" />
          <el-table-column prop="allergyInfo" label="过敏史" min-width="200" show-overflow-tooltip :resizable="false" />
          <el-table-column label="操作" width="160" align="center" :resizable="false">
            <template #default="{ row }">
              <div class="patient-list__actions">
                <el-button size="small" type="primary" @click="openDialog(row)">编辑</el-button>
                <el-popconfirm v-if="canDeletePatient" title="确定删除该患者? 删除后不可恢复" @confirm="handleDelete(row.id)">
                  <template #reference><el-button size="small" type="danger">删除</el-button></template>
                </el-popconfirm>
                <el-button v-else size="small" type="danger" disabled>删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑患者' : '新增患者'" width="550px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-form-item label="姓名" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="身份证号" prop="idCard"><el-input v-model="form.idCard" maxlength="18" /></el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender"><el-radio :value="0">女</el-radio><el-radio :value="1">男</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="出生日期" prop="birthDate"><el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-divider content-position="left">联系与过敏</el-divider>
        <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="住址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="过敏史"><el-input v-model="form.allergyInfo" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { listPatients, searchPatients, createPatient, updatePatient, deletePatient, deletePatientWithRelated } from '../../api/patient'
import type { Patient } from '../../types'
import { useUserStore } from '../../stores/user'
import PageHeader from '../../components/PageHeader.vue'
import DataToolbar from '../../components/DataToolbar.vue'
import EmptyState from '../../components/EmptyState.vue'

const userStore = useUserStore()
const tableData = ref<Patient[]>([])
const keyword = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const loading = ref(false)
const saveLoading = ref(false)
const canDeletePatient = computed(() => userStore.hasRole('admin'))

const defaultForm = (): Patient => ({ idCard: '', name: '', gender: 1, birthDate: null, phone: '', address: '', allergyInfo: '' })
const form = reactive<Patient>(defaultForm())

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }, { pattern: /^\d{17}[\dXx]$/, message: '身份证号格式不正确', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await listPatients()
    tableData.value = normalizePatientList(res.data).sort((a, b) => (a.id ?? 0) - (b.id ?? 0))
  } catch {}
  loading.value = false
}

async function handleSearch() {
  if (!keyword.value) { loadData(); return }
  loading.value = true
  try {
    const res = await searchPatients(keyword.value)
    tableData.value = normalizePatientList(res.data).sort((a, b) => (a.id ?? 0) - (b.id ?? 0))
  } catch {}
  loading.value = false
}

function openDialog(row?: Patient) {
  isEdit.value = !!row
  Object.assign(form, row ? { ...row } : defaultForm())
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saveLoading.value = true
  try {
    const payload = buildPatientPayload()
    if (isEdit.value && form.id) {
      await updatePatient(form.id, payload)
    } else {
      await createPatient(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch {}
  saveLoading.value = false
}

async function handleDelete(id: number) {
  try {
    await deletePatient(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    const message = error instanceof Error ? error.message : ''
    if (!message.includes('已有挂号/病历/处方记录')) {
      return
    }
    try {
      await ElMessageBox.confirm(
        '该患者已有挂号、病历或处方记录。继续操作将同时清理这些关联业务数据，再删除患者档案。此操作不可恢复，是否继续？',
        '清理关联数据并删除',
        {
          confirmButtonText: '清理并删除',
          cancelButtonText: '取消',
          type: 'warning',
        }
      )
      await deletePatientWithRelated(id)
      ElMessage.success('关联业务数据已清理，患者已删除')
      loadData()
    } catch {}
  }
}

function buildPatientPayload(): Patient {
  return {
    id: form.id,
    idCard: form.idCard.trim(),
    name: form.name.trim(),
    gender: form.gender,
    birthDate: form.birthDate || null,
    phone: form.phone?.trim() || '',
    address: form.address?.trim() || '',
    allergyInfo: form.allergyInfo?.trim() || '',
  }
}

function normalizePatientList(data: Patient[] | { list?: Patient[] }) {
  return Array.isArray(data) ? data : data?.list || []
}

onMounted(loadData)
</script>

<style scoped>
.patient-list {
  height: max(560px, calc(100vh - var(--header-height) - var(--content-padding) * 2));
  display: flex;
  flex-direction: column;
  min-width: 0;
  animation: fadeIn 0.4s ease-out;
}

.patient-list__card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-card);
  overflow: hidden;
}

.patient-list__card :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: var(--space-lg);
}

.patient-list__table-wrap {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.patient-list__table-wrap :deep(.el-table) {
  width: 100%;
}

.patient-list__actions {
  display: flex;
  flex-wrap: nowrap;
  justify-content: center;
  align-items: center;
  gap: 0;
  white-space: nowrap;
}

@media (max-width: 1180px) {
  .patient-list {
    height: auto;
    min-height: max(560px, calc(100vh - var(--header-height) - var(--content-padding) * 2));
  }

  .patient-list__card {
    min-height: 620px;
  }

  .patient-list__table-wrap {
    flex: none;
    height: clamp(360px, 48vh, 560px);
    min-height: 0;
  }
}

@media (max-width: 768px) {
  .patient-list__card :deep(.el-card__body) {
    padding: var(--space-md);
  }

  .patient-list__table-wrap {
    height: 320px;
  }
}
</style>
