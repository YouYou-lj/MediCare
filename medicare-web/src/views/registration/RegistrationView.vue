<template>
  <div class="registration-view">
    <PageHeader title="挂号预约" subtitle="选择号源并为患者办理挂号业务" />

    <div class="registration-view__workspace">
      <el-card shadow="hover" class="registration-view__panel">
        <el-tabs v-model="activeTab" class="registration-view__tabs" type="border-card">
          <el-tab-pane label="号源列表" name="schedules">
            <div class="registration-view__tab-content">
              <div class="registration-view__toolbar">
                <el-date-picker
                  v-model="queryDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  class="registration-view__date"
                  @change="handleDateChange"
                />
                <el-select
                  v-model="queryDeptId"
                  placeholder="筛选科室"
                  clearable
                  class="registration-view__select"
                  @change="loadSchedules"
                >
                  <el-option v-for="d in deptList" :key="d.id" :label="d.name" :value="d.id" />
                </el-select>
                <el-button :icon="Refresh" :loading="schedLoading" @click="loadSchedules">刷新</el-button>
              </div>

              <div class="registration-view__table-wrap">
                <el-table
                  v-loading="schedLoading"
                  :data="schedList"
                  stripe
                  border
                  highlight-current-row
                  height="100%"
                  :default-sort="{ prop: 'code', order: 'ascending' }"
                  :row-class-name="scheduleRowClassName"
                  @row-click="handleSelectSchedule"
                >
                  <el-table-column type="index" label="序号" width="60" align="center" :resizable="false" />
                  <el-table-column prop="code" label="ID" width="120" align="center" :resizable="false" />
                  <el-table-column prop="doctorName" label="医生" width="110" :resizable="false" align="center" />
                  <el-table-column prop="departmentName" label="科室" min-width="120" :resizable="false" align="center" />
                  <el-table-column prop="workDate" label="日期" min-width="120" :resizable="false" align="center" />
                  <el-table-column prop="timeSlot" label="时段" min-width="100" :resizable="false" align="center" />
                  <el-table-column prop="remainSlots" label="剩余号源" min-width="110" :resizable="false" align="center">
                    <template #default="{ row }">
                      <StatusTag :type="row.remainSlots > 0 ? 'success' : 'danger'" :label="String(row.remainSlots)" />
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="116" align="center" :resizable="false">
                    <template #default="{ row }">
                      <el-button size="small" type="primary" text @click.stop="handleSelectSchedule(row)">选择</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <EmptyState v-if="!schedLoading && schedList.length === 0" icon="Calendar" title="暂无可挂号源" description="请切换日期或科室查看" />
              </div>

              <div class="registration-view__footer">
                <div class="registration-view__selection" :class="{ 'registration-view__selection--empty': !selectedSchedule }">
                  <template v-if="selectedSchedule">
                    <span>{{ selectedSchedule.departmentName || '-' }}</span>
                    <span>{{ selectedSchedule.doctorName || '-' }}</span>
                    <span>{{ selectedSchedule.workDate }}</span>
                    <span>{{ selectedSchedule.timeSlot }}</span>
                  </template>
                  <template v-else>请选择一条号源</template>
                </div>
                <el-button type="primary" :disabled="!selectedSchedule || !canManageRegistration" :icon="Plus" @click="openRegDialog">挂号</el-button>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="挂号记录" name="records">
            <div class="registration-view__tab-content">
              <div class="registration-view__toolbar">
                <el-radio-group v-model="queryStatus" class="registration-view__status-filter" @change="loadRegs">
                  <el-radio-button :value="undefined">全部</el-radio-button>
                  <el-radio-button :value="0">候诊</el-radio-button>
                  <el-radio-button :value="1">就诊中</el-radio-button>
                  <el-radio-button :value="2">已完成</el-radio-button>
                  <el-radio-button :value="3">已取消</el-radio-button>
                </el-radio-group>
                <el-button :icon="Refresh" :loading="regLoading" @click="loadRegs">刷新</el-button>
              </div>

              <div class="registration-view__table-wrap">
                <el-table
                  v-loading="regLoading"
                  :data="regList"
                  stripe
                  border
                  height="100%"
                  :default-sort="{ prop: 'code', order: 'ascending' }"
                >
                  <el-table-column type="index" label="序号" width="60" align="center" :resizable="false" />
                  <el-table-column prop="code" label="ID" width="120" align="center" :resizable="false" />
                  <el-table-column prop="seqNo" label="排队号" width="76" :resizable="false" align="center" />
                  <el-table-column prop="patientName" label="患者" min-width="110" :resizable="false" align="center" />
                  <el-table-column prop="doctorName" label="医生" min-width="110" :resizable="false" align="center" />
                  <el-table-column prop="departmentName" label="科室" min-width="120" :resizable="false" align="center" />
                  <el-table-column prop="timeSlot" label="时段" min-width="90" :resizable="false" align="center" />
                  <el-table-column prop="fee" label="费用" min-width="90" :resizable="false" align="center">
                    <template #default="{ row }">{{ formatFee(row.fee) }}</template>
                  </el-table-column>
                  <el-table-column prop="status" label="状态" min-width="100" :resizable="false" align="center">
                    <template #default="{ row }">
                      <StatusTag :type="statusType(row.status)" :label="statusText(row.status)" />
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="116" align="center" :resizable="false">
                    <template #default="{ row }">
                      <div class="registration-view__actions" v-if="row.status === 0">
                        <el-popconfirm title="确定取消该挂号? 取消后号源将释放" :disabled="!canManageRegistration" @confirm="handleCancel(row.id)">
                          <template #reference><el-button size="small" type="danger" :disabled="!canManageRegistration">取消</el-button></template>
                        </el-popconfirm>
                      </div>
                      <span v-else class="registration-view__muted">-</span>
                    </template>
                  </el-table-column>
                </el-table>
                <EmptyState v-if="!regLoading && regList.length === 0" icon="Document" title="暂无挂号记录" description="选择号源并点击挂号" />
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>

    <el-dialog v-model="regDialogVisible" title="选择患者" width="700px" destroy-on-close>
      <el-input
        v-model="patientKw"
        placeholder="搜索患者姓名/身份证/手机号"
        class="registration-view__patient-search"
        clearable
        @input="handleSearchPatients"
      />
      <el-table
        v-loading="patientLoading"
        :data="patientList"
        stripe
        border
        highlight-current-row
        height="360"
        :default-sort="{ prop: 'code', order: 'ascending' }"
        @row-click="handleSelectPatient"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="code" label="ID" width="120" align="center" />
        <el-table-column prop="name" label="姓名" width="80" align="center" />
        <el-table-column prop="idCard" label="身份证号" width="170" />
        <el-table-column prop="phone" label="手机号" width="120" align="center" />
        <el-table-column prop="allergyInfo" label="过敏史" />
      </el-table>
      <EmptyState v-if="!patientLoading && patientList.length === 0" icon="User" title="未找到患者" description="请先在患者管理中建档" />
      <template #footer>
        <el-button @click="regDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="regBtnLoading" :disabled="!selectedPatient || !canManageRegistration" @click="handleRegister">确认挂号</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { listDepartments } from '../../api/department'
import { getAvailableSchedules } from '../../api/schedule'
import { listRegistrations, register, cancelRegistration } from '../../api/registration'
import { listPatients, searchPatients } from '../../api/patient'
import type { Department, Schedule, Registration, Patient } from '../../types'
import PageHeader from '../../components/PageHeader.vue'
import EmptyState from '../../components/EmptyState.vue'
import StatusTag from '../../components/StatusTag.vue'
import { usePermission } from '../../composables/usePermission'

const { canManageRegistration } = usePermission()

const deptList = ref<Department[]>([])
const schedList = ref<Schedule[]>([])
const regList = ref<Registration[]>([])
const queryDate = ref(new Date().toISOString().slice(0, 10))
const queryDeptId = ref<number>()
const queryStatus = ref<number | undefined>()
const activeTab = ref('schedules')
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
const formatFee = (fee?: number) => fee == null ? '-' : `¥${Number(fee).toFixed(2)}`

async function loadSchedules() {
  schedLoading.value = true
  try {
    const r = await getAvailableSchedules(queryDate.value, queryDeptId.value)
    schedList.value = r.data.sort((a, b) => (a.id ?? 0) - (b.id ?? 0))
    if (selectedSchedule.value && !schedList.value.some(item => item.id === selectedSchedule.value?.id)) {
      selectedSchedule.value = null
    }
  } catch {}
  schedLoading.value = false
}

async function loadRegs() {
  regLoading.value = true
  try { const r = await listRegistrations(queryDate.value, queryStatus.value); regList.value = r.data.sort((a, b) => (a.id ?? 0) - (b.id ?? 0)) } catch {}
  regLoading.value = false
}

function handleSelectSchedule(row: Schedule) { selectedSchedule.value = row }

function scheduleRowClassName({ row }: { row: Schedule }) {
  return row.id === selectedSchedule.value?.id ? 'registration-view__selected-row' : ''
}

function handleDateChange() {
  selectedSchedule.value = null
  loadSchedules()
  loadRegs()
}

async function openRegDialog() {
  regDialogVisible.value = true
  patientKw.value = ''
  selectedPatient.value = null
  patientLoading.value = true
  try { const r = await listPatients(); patientList.value = r.data.sort((a, b) => (a.id ?? 0) - (b.id ?? 0)) } catch {}
  patientLoading.value = false
}

async function handleSearchPatients() {
  patientLoading.value = true
  try {
    if (!patientKw.value.trim()) {
      const r = await listPatients(); patientList.value = r.data.sort((a, b) => (a.id ?? 0) - (b.id ?? 0))
    } else {
      const r = await searchPatients(patientKw.value.trim()); patientList.value = r.data.sort((a, b) => (a.id ?? 0) - (b.id ?? 0))
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
    selectedSchedule.value = null
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
  height: max(560px, calc(100vh - var(--header-height) - var(--content-padding) * 2));
  display: flex;
  flex-direction: column;
  min-width: 0;
  animation: fadeIn 0.4s ease-out;
}

.registration-view__workspace {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.registration-view__panel {
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-card);
  overflow: hidden;
}

.registration-view__panel :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 0;
}

.registration-view__tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.registration-view__tabs :deep(.el-tabs__header) {
  flex-shrink: 0;
  margin-bottom: 0;
}

.registration-view__tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.registration-view__tabs :deep(.el-tab-pane) {
  height: 100%;
  min-height: 0;
}

.registration-view__tab-content {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: var(--space-lg);
  box-sizing: border-box;
}

.registration-view__toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex-wrap: wrap;
  margin-bottom: var(--space-lg);
}

.registration-view__toolbar :deep(.registration-view__date) {
  width: 150px !important;
  flex: 0 0 150px;
}

.registration-view__toolbar :deep(.registration-view__select) {
  width: 180px;
  flex: 0 0 180px;
}

.registration-view__status-filter {
  flex: 1;
  min-width: 0;
}

.registration-view__table-wrap {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.registration-view__table-wrap :deep(.el-table) {
  width: 100%;
}

.registration-view__table-wrap :deep(.registration-view__selected-row > td) {
  background-color: var(--color-primary-light) !important;
}

.registration-view__actions {
  display: flex;
  flex-wrap: nowrap;
  justify-content: center;
  align-items: center;
  gap: 0;
  white-space: nowrap;
}

.registration-view__footer {
  flex-shrink: 0;
  margin-top: var(--space-lg);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
}

.registration-view__selection {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.registration-view__selection span {
  flex-shrink: 0;
}

.registration-view__selection--empty,
.registration-view__muted {
  color: var(--text-muted);
}

.registration-view__patient-search {
  margin-bottom: var(--space-md);
}

@media (max-width: 1180px) {
  .registration-view {
    height: auto;
    min-height: max(560px, calc(100vh - var(--header-height) - var(--content-padding) * 2));
  }

  .registration-view__workspace {
    height: auto;
  }

  .registration-view__panel {
    height: auto;
    min-height: 520px;
  }

  .registration-view__tabs {
    height: auto;
  }

  .registration-view__tabs :deep(.el-tabs__content) {
    height: auto;
    overflow: visible;
  }

  .registration-view__tabs :deep(.el-tab-pane) {
    height: auto;
  }

  .registration-view__tab-content {
    height: auto;
  }

  .registration-view__table-wrap {
    flex: none;
    height: clamp(360px, 46vh, 520px);
    min-height: 0;
  }
}

@media (max-width: 768px) {
  .registration-view__toolbar {
    align-items: stretch;
  }

  .registration-view__table-wrap {
    height: 320px;
  }

  .registration-view__toolbar :deep(.registration-view__date),
  .registration-view__toolbar :deep(.registration-view__select),
  .registration-view__toolbar :deep(.el-button) {
    width: 100%;
  }

  .registration-view__footer {
    align-items: stretch;
    flex-direction: column;
  }

  .registration-view__footer :deep(.el-button) {
    width: 100%;
  }
}
</style>
