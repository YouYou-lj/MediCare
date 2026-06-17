<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>患者管理</span>
          <div>
            <el-input v-model="keyword" placeholder="搜索姓名/身份证/手机号" style="width:240px;margin-right:12px" clearable @clear="loadData" @keyup.enter="handleSearch">
              <template #append><el-button @click="handleSearch"><el-icon><Search /></el-icon></el-button></template>
            </el-input>
            <el-button type="primary" @click="openDialog()"><el-icon><Plus /></el-icon> 新增</el-button>
          </div>
        </div>
      </template>
      <el-table :data="tableData" stripe border style="width:100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="idCard" label="身份证号" width="180" />
        <el-table-column prop="gender" label="性别" width="70">
          <template #default="{ row }">{{ ['女','男','其他'][row.gender] || '未知' }}</template>
        </el-table-column>
        <el-table-column prop="birthDate" label="出生日期" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="allergyInfo" label="过敏史" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除该患者?" @confirm="handleDelete(row.id)">
              <template #reference><el-button text type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑患者' : '新增患者'" width="550px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="姓名" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="身份证号" prop="idCard"><el-input v-model="form.idCard" maxlength="18" /></el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender"><el-radio :value="0">女</el-radio><el-radio :value="1">男</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="出生日期" prop="birthDate"><el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="住址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="过敏史"><el-input v-model="form.allergyInfo" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
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

const tableData = ref<Patient[]>([])
const keyword = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = (): Patient => ({ idCard: '', name: '', gender: 1, birthDate: '', phone: '', address: '', allergyInfo: '' })
const form = reactive<Patient>(defaultForm())

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }, { pattern: /^\d{17}[\dXx]$/, message: '身份证号格式不正确', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
}

async function loadData() {
  try {
    const res = await listPatients()
    tableData.value = res.data.list || res.data as any
  } catch {}
}

async function handleSearch() {
  if (!keyword.value) { loadData(); return }
  try {
    const res = await searchPatients(keyword.value)
    tableData.value = res.data
  } catch {}
}

function openDialog(row?: Patient) {
  isEdit.value = !!row
  Object.assign(form, row ? { ...row } : defaultForm())
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
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
}

async function handleDelete(id: number) {
  try { await deletePatient(id); ElMessage.success('删除成功'); loadData() } catch {}
}

onMounted(loadData)
</script>