<template>
  <div class="patient-list">
    <PageHeader title="患者管理" subtitle="患者建档、信息查询与编辑" />

    <el-card shadow="hover" class="data-card">
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

      <el-table v-loading="loading" :data="tableData" stripe border style="width:100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="idCard" label="身份证号" width="180" />
        <el-table-column prop="gender" label="性别" width="70">
          <template #default="{ row }">{{ ['女','男','其他'][row.gender] || '未知' }}</template>
        </el-table-column>
        <el-table-column prop="birthDate" label="出生日期" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="allergyInfo" label="过敏史" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" type="primary" @click="openDialog(row)">编辑</el-button>
              <el-popconfirm title="确定删除该患者? 删除后不可恢复" @confirm="handleDelete(row.id)">
                <template #reference><el-button size="small" type="danger">删除</el-button></template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState
        v-if="!loading && tableData.length === 0"
        icon="User"
        title="暂无患者数据"
        description="点击右上角“新增患者”按钮开始建档"
      />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { listPatients, searchPatients, createPatient, updatePatient, deletePatient } from '../../api/patient'
import type { Patient } from '../../types'
import PageHeader from '../../components/PageHeader.vue'
import DataToolbar from '../../components/DataToolbar.vue'
import EmptyState from '../../components/EmptyState.vue'

const tableData = ref<Patient[]>([])
const keyword = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const loading = ref(false)
const saveLoading = ref(false)

const defaultForm = (): Patient => ({ idCard: '', name: '', gender: 1, birthDate: '', phone: '', address: '', allergyInfo: '' })
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
    tableData.value = res.data
  } catch {}
  loading.value = false
}

async function handleSearch() {
  if (!keyword.value) { loadData(); return }
  loading.value = true
  try {
    const res = await searchPatients(keyword.value)
    tableData.value = res.data
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
    if (isEdit.value && form.id) {
      await updatePatient(form.id, form)
    } else {
      await createPatient(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch {}
  saveLoading.value = false
}

async function handleDelete(id: number) {
  try { await deletePatient(id); ElMessage.success('删除成功'); loadData() } catch {}
}

onMounted(loadData)
</script>

<style scoped>
.patient-list {
  animation: fadeIn 0.4s ease-out;
}
.data-card {
  border-radius: var(--radius-lg);
  overflow: hidden;
}
.action-buttons {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 4px;
}
</style>
