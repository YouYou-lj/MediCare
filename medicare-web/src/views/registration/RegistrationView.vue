<template>
  <div class="registration-view">
    <PageHeader title="挂号预约" subtitle="选择号源并为患者办理挂号业务" />

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover" class="data-card">
          <template #header><span class="card-title">📅 号源列表</span></template>
          <div class="toolbar">
            <el-date-picker v-model="queryDate" type="date" value-format="YYYY-MM-DD" @change="loadSchedules" />
            <el-select v-model="queryDeptId" placeholder="筛选科室" clearable style="width:180px" @change="loadSchedules">
              <el-option v-for="d in deptList" :key="d.id" :label="d.name" :value="d.id" />
            </el-select>
          </div>
          <el-table v-loading="schedLoading" :data="schedList" stripe border highlight-current-row @row-click="handleSelectSchedule">
            <el-table-column prop="doctorName" label="医生" width="80" />
            <el-table-column prop="departmentName" label="科室" width="80" />
            <el-table-column prop="timeSlot" label="时段" width="70" />
            <el-table-column prop="remainSlots" label="剩余" width="70">
              <template #default="{ row }">
                <StatusTag :type="row.remainSlots > 0 ? 'success' : 'danger'" :label="String(row.remainSlots)" />
              </template>
            </el-table-column>
          </el-table>
          <EmptyState v-if="!schedLoading && schedList.length === 0" icon="Calendar" title="暂无可挂号源" description="请切换日期或科室查看" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="data-card">
          <template #header><span class="card-title">📝 挂号记录</span></template>
          <el-table v-loading="regLoading" :data="regList" stripe border>
            <el-table-column prop="seqNo" label="序号" width="60" />
            <el-table-column prop="patientName" label="患者" width="80" />
            <el-table-column prop="doctorName" label="医生" width="80" />
            <el-table-column prop="timeSlot" label="时段" width="70" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <StatusTag :type="statusType(row.status)" :label="statusText(row.status)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <div class="action-buttons" v-if="row.status === 0">
                  <el-popconfirm title="确定取消该挂号? 取消后号源将释放" @confirm="handleCancel(row.id)">
                    <template #reference><el-button size="small" type="danger">取消</el-button></template>
                  </el-popconfirm>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <EmptyState v-if="!regLoading && regList.length === 0" icon="Document" title="暂无挂号记录" description="选择左侧号源并点击挂号" />
          <div class="reg-btn-wrap">
            <el-button type="primary" :disabled="!selectedSchedule" :icon="Plus" @click="openRegDialog">挂号</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="regDialogVisible" title="选择患者" width="700px" destroy-on-close>
      <el-input v-model="patientKw" placeholder="搜索患者姓名/身份证/手机号" style="margin-bottom:12px" clearable @input="handleSearchPatients" />
      <el-table v-loading="patientLoading" :data="patientList" stripe border highlight-current-row @row-click="handleSelectPatient">
        <el-table-column prop="name" label="姓名" width="80" />
        <el-table-column prop="idCard" label="身份证号" width="170" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="allergyInfo" label="过敏史" />
      </el-table>
      <EmptyState v-if="!patientLoading && patientList.length === 0" icon="User" title="未找到患者" description="请先在患者管理中建档" />
      <template #footer>
        <el-button @click="regDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="regBtnLoading" :disabled="!selectedPatient" @click="handleRegister">确认挂号</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listDepartments } from '../../api/department'
import { getAvailableSchedules } from '../../api/schedule'
import { listRegistrations, register, cancelRegistration } from '../../api/registration'
import { listPatients, searchPatients } from '../../api/patient'
import type { Department, Schedule, Registration, Patient } from '../../types'
import PageHeader from '../../components/PageHeader.vue'
import EmptyState from '../../components/EmptyState.vue'
import StatusTag from '../../components/StatusTag.vue'

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
const schedLoading = ref(false)
const regLoading = ref(false)
const patientLoading = ref(false)
const regBtnLoading = ref(false)

const statusText = (s: number) => ['候诊','就诊中','已完成','已取消'][s] || '未知'
const statusType = (s: number): any => (['warning','primary','success','info'] as const)[s] || 'default'

async function loadSchedules() {
  schedLoading.value = true
  try { const r = await getAvailableSchedules(queryDate.value, queryDeptId.value); schedList.value = r.data } catch {}
  schedLoading.value = false
}

async function loadRegs() {
  regLoading.value = true
  try { const r = await listRegistrations(queryDate.value); regList.value = r.data } catch {}
  regLoading.value = false
}

function handleSelectSchedule(row: Schedule) { selectedSchedule.value = row }

async function openRegDialog() {
  regDialogVisible.value = true
  patientKw.value = ''
  selectedPatient.value = null
  patientLoading.value = true
  try { const r = await listPatients(); patientList.value = r.data } catch {}
  patientLoading.value = false
}

async function handleSearchPatients() {
  patientLoading.value = true
  try {
    if (!patientKw.value.trim()) {
      const r = await listPatients(); patientList.value = r.data
    } else {
      const r = await searchPatients(patientKw.value.trim()); patientList.value = r.data
    }
  } catch {}
  patientLoading.value = false
}

function handleSelectPatient(row: Patient) { selectedPatient.value = row }

async function handleRegister() {
  if (!selectedSchedule.value || !selectedPatient.value) return
  regBtnLoading.value = true
  try {
    await register({ patientId: selectedPatient.value.id!, scheduleId: selectedSchedule.value.id! })
    ElMessage.success('挂号成功')
    regDialogVisible.value = false
    loadSchedules(); loadRegs()
  } catch {}
  regBtnLoading.value = false
}

async function handleCancel(id: number) {
  try { await cancelRegistration(id); ElMessage.success('取消成功'); loadRegs(); loadSchedules() } catch {}
}

onMounted(async () => { await listDepartments().then(r => deptList.value = r.data); loadSchedules(); loadRegs() })
</script>

<style scoped>
.registration-view {
  animation: fadeIn 0.4s ease-out;
}
.data-card {
  border-radius: var(--radius-lg);
  overflow: hidden;
}
.card-title {
  font-weight: 600;
  color: var(--text-primary);
}
.toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: nowrap;
  margin-bottom: 16px;
  overflow: hidden;
}
.action-buttons {
  display: flex;
  flex-wrap: nowrap;
  justify-content: center;
  align-items: center;
  gap: 0;
  white-space: nowrap;
}
.reg-btn-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
