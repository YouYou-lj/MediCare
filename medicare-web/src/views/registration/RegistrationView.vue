<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>号源列表</template>
          <div style="margin-bottom:12px">
            <el-date-picker v-model="queryDate" type="date" value-format="YYYY-MM-DD" style="margin-right:12px" @change="loadSchedules" />
            <el-select v-model="queryDeptId" placeholder="筛选科室" clearable style="width:180px" @change="loadSchedules">
              <el-option v-for="d in deptList" :key="d.id" :label="d.name" :value="d.id" />
            </el-select>
          </div>
          <el-table :data="schedList" stripe border @row-click="handleSelectSchedule" highlight-current-row>
            <el-table-column prop="doctorName" label="医生" width="80" />
            <el-table-column prop="departmentName" label="科室" width="80" />
            <el-table-column prop="timeSlot" label="时段" width="70" />
            <el-table-column prop="remainSlots" label="剩余" width="70">
              <template #default="{ row }"><el-tag :type="row.remainSlots > 0 ? 'success' : 'danger'">{{ row.remainSlots }}</el-tag></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>挂号记录</template>
          <el-table :data="regList" stripe border>
            <el-table-column prop="seqNo" label="序号" width="60" />
            <el-table-column prop="patientName" label="患者" width="80" />
            <el-table-column prop="doctorName" label="医生" width="80" />
            <el-table-column prop="timeSlot" label="时段" width="70" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }"><el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button v-if="row.status===0" text type="danger" size="small" @click="handleCancel(row.id)">取消</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top:16px">
            <el-button type="primary" :disabled="!selectedSchedule" @click="openRegDialog">挂号</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="regDialogVisible" title="选择患者" width="700px" destroy-on-close>
      <el-input v-model="patientKw" placeholder="搜索患者" style="margin-bottom:12px" @keyup.enter="searchPatients" />
      <el-table :data="patientList" stripe border @row-click="handleSelectPatient" highlight-current-row>
        <el-table-column prop="name" label="姓名" width="80" />
        <el-table-column prop="idCard" label="身份证号" width="170" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="allergyInfo" label="过敏史" />
      </el-table>
      <template #footer><el-button @click="regDialogVisible=false">取消</el-button><el-button type="primary" :disabled="!selectedPatient" @click="handleRegister">确认挂号</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listDepartments } from '../../api/department'
import { getAvailableSchedules } from '../../api/schedule'
import { listRegistrations, register, cancelRegistration } from '../../api/registration'
import { searchPatients } from '../../api/patient'
import type { Department, Schedule, Registration, Patient } from '../../types'

const deptList = ref<Department[]>([])
const schedList = ref<Schedule[]>([])
const regList = ref<Registration[]>([])
const queryDate = ref(new Date().toISOString().slice(0, 10))
const queryDeptId = ref<number>()
const selectedSchedule = ref<Schedule | null>(null)
const regDialogVisible = ref(false)
const patientKw = ref('')
const patientList = ref<Patient[]>([])
const selectedPatient = ref<Patient | null>(null)

const statusText = (s: number) => ['候诊','就诊中','已完成','已取消'][s] || '未知'
const statusTag = (s: number) => (['warning','','success','info'] as const)[s] || ''

async function loadSchedules() { try { const r = await getAvailableSchedules(queryDate.value, queryDeptId.value); schedList.value = r.data } catch {} }
async function loadRegs() { try { const r = await listRegistrations(queryDate.value); regList.value = r.data } catch {} }
function handleSelectSchedule(row: Schedule) { selectedSchedule.value = row }

function openRegDialog() { regDialogVisible.value = true; patientKw.value = ''; patientList.value = []; selectedPatient.value = null }

async function handleSearchPatients() { if (!patientKw.value) return; try { const r = await searchPatients(patientKw.value); patientList.value = r.data } catch {} }
function handleSelectPatient(row: Patient) { selectedPatient.value = row }

async function handleRegister() {
  if (!selectedSchedule.value || !selectedPatient.value) return
  try {
    await register({ patientId: selectedPatient.value.id!, scheduleId: selectedSchedule.value.id! })
    ElMessage.success('挂号成功')
    regDialogVisible.value = false
    loadSchedules(); loadRegs()
  } catch {}
}

async function handleCancel(id: number) {
  try { await cancelRegistration(id); ElMessage.success('取消成功'); loadRegs(); loadSchedules() } catch {}
}

onMounted(async () => { await listDepartments().then(r => deptList.value = r.data); loadSchedules(); loadRegs() })
</script>