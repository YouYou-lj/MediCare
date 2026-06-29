<template>
  <div class="workstation-view">
    <PageHeader title="医生工作站" subtitle="叫号、接诊与病历书写" />

    <div class="workstation-view__workspace">
      <el-card shadow="hover" class="workstation-view__panel">
        <template #header>
          <div class="workstation-view__panel-header">
            <div class="workstation-view__panel-title">
              <el-icon><User /></el-icon>
              <span>候诊列表</span>
            </div>
            <span class="workstation-view__panel-meta">当前 {{ waitingList.length }} 位</span>
          </div>
        </template>

        <div class="workstation-view__toolbar">
          <el-select
            v-model="selectedDoctorId"
            placeholder="选择医生"
            class="workstation-view__doctor-select"
            @change="loadWaiting"
          >
            <el-option v-for="d in doctorList" :key="d.id" :label="`${d.name} - ${d.departmentName}`" :value="d.id" />
          </el-select>
          <el-button :icon="Refresh" :loading="waitingLoading" @click="loadWaiting">刷新</el-button>
        </div>

        <div class="workstation-view__table-wrap">
          <el-table
            v-loading="waitingLoading"
            :data="waitingList"
            stripe
            border
            highlight-current-row
            height="100%"
            :default-sort="{ prop: 'code', order: 'ascending' }"
            :row-class-name="waitingRowClassName"
            @row-click="handleSelectReg"
          >
            <template #empty>
              <EmptyState icon="User" title="暂无候诊患者" description="请选择医生查看今日候诊患者" />
            </template>
            <el-table-column type="index" label="序号" width="60" align="center" fixed="left" />
            <el-table-column prop="code" label="ID" width="120" align="center" fixed="left" />
            <el-table-column prop="seqNo" label="排队号" width="76" fixed="left" align="center" />
            <el-table-column prop="patientName" label="患者" min-width="120" align="center" />
            <el-table-column prop="timeSlot" label="时段" min-width="90" align="center" />
            <el-table-column prop="status" label="状态" min-width="110" align="center">
              <template #default="{ row }">
                <StatusTag :type="waitingStatusType(row.status)" :label="registrationStatusText(row.status)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="116" fixed="right" align="center">
              <template #default="{ row }">
                <el-button size="small" type="primary" text @click.stop="handleSelectReg(row)">选择</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="workstation-view__footer">
          <div class="workstation-view__selection" :class="{ 'workstation-view__selection--empty': !selectedReg }">
            <template v-if="selectedReg">
              <span>{{ selectedReg.patientName || '-' }}</span>
              <span>{{ selectedReg.timeSlot || '-' }}</span>
              <StatusTag :type="waitingStatusType(selectedReg.status)" :label="registrationStatusText(selectedReg.status)" />
            </template>
            <template v-else>请选择候诊患者</template>
          </div>
          <div class="workstation-view__actions">
            <el-button type="primary" :icon="Bell" :disabled="!selectedReg || selectedReg.status !== 0 || !canUseWorkstation" @click="handleCall">叫号</el-button>
            <el-button type="success" :icon="CircleCheck" :disabled="!selectedReg || selectedReg.status !== 1 || !canUseWorkstation" @click="handleComplete">完成就诊</el-button>
          </div>
        </div>
      </el-card>

      <el-card shadow="hover" class="workstation-view__panel">
        <template #header>
          <div class="workstation-view__panel-header">
            <div class="workstation-view__panel-title">
              <el-icon><EditPen /></el-icon>
              <span>病历书写</span>
            </div>
            <span class="workstation-view__panel-meta">{{ selectedReg ? registrationStatusText(selectedReg.status) : '未选择患者' }}</span>
          </div>
        </template>

        <div class="workstation-view__record-wrap">
          <template v-if="selectedReg && selectedReg.status === 1">
            <div class="workstation-view__patient-strip">
              <span>患者: {{ selectedReg.patientName || '-' }}</span>
              <span>医生: {{ selectedReg.doctorName || '-' }}</span>
              <span>时段: {{ selectedReg.timeSlot || '-' }}</span>
            </div>
            <el-form ref="recordFormRef" :model="recordForm" label-width="90px" class="workstation-view__record-form">
            <el-divider content-position="left">主诉与现病史</el-divider>
            <el-form-item label="主诉"><el-input v-model="recordForm.chiefComplaint" type="textarea" :rows="2" /></el-form-item>
            <el-form-item label="现病史"><el-input v-model="recordForm.presentIllness" type="textarea" :rows="3" /></el-form-item>
            <el-divider content-position="left">检查与诊断</el-divider>
            <el-form-item label="既往史"><el-input v-model="recordForm.pastHistory" /></el-form-item>
            <el-form-item label="体格检查"><el-input v-model="recordForm.physicalExam" /></el-form-item>
            <el-form-item label="诊断"><el-input v-model="recordForm.diagnosis" /></el-form-item>
            <el-form-item label="医嘱"><el-input v-model="recordForm.advice" type="textarea" :rows="2" /></el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="DocumentChecked" :loading="saveLoading" :disabled="!canUseWorkstation" @click="saveRecord">保存病历</el-button>
              </el-form-item>
            </el-form>
          </template>
          <div v-else class="workstation-view__record-empty">
            <EmptyState
              icon="FirstAidKit"
              :title="selectedReg ? '等待叫号' : '请先选择患者'"
              :description="selectedReg ? '当前患者仍在候诊，点击左侧叫号后进入病历书写。' : '选择医生和候诊患者后，可在这里书写病历。'"
            />
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, CircleCheck, DocumentChecked, EditPen, Refresh, User } from '@element-plus/icons-vue'
import { listDoctors } from '../../api/doctor'
import { listRegistrations, callPatient, completeRegistration } from '../../api/registration'
import { createMedicalRecord } from '../../api/medical-record'
import type { Doctor, Registration, MedicalRecord } from '../../types'
import PageHeader from '../../components/PageHeader.vue'
import EmptyState from '../../components/EmptyState.vue'
import StatusTag from '../../components/StatusTag.vue'
import { usePermission } from '../../composables/usePermission'

const { canUseWorkstation } = usePermission()

const doctorList = ref<Doctor[]>([])
const selectedDoctorId = ref<number>()
const waitingList = ref<Registration[]>([])
const selectedReg = ref<Registration | null>(null)
const recordForm = reactive<Partial<MedicalRecord>>({ chiefComplaint:'', presentIllness:'', pastHistory:'', physicalExam:'', diagnosis:'', advice:'' })
const waitingLoading = ref(false)
const saveLoading = ref(false)

const registrationStatusText = (s: number) => ['候诊', '就诊中', '已完成', '已取消'][s] || '未知'
const waitingStatusType = (s: number): any => (s === 0 ? 'warning' : s === 1 ? 'primary' : 'success')

async function loadDoctors() {
  try { const r = await listDoctors(); doctorList.value = r.data } catch {}
}

async function loadWaiting() {
  if (!selectedDoctorId.value) {
    waitingList.value = []
    selectedReg.value = null
    return
  }
  waitingLoading.value = true
  try {
    const r = await listRegistrations(undefined, undefined)
    waitingList.value = (r.data as Registration[])
      .filter(reg => reg.doctorId === selectedDoctorId.value && (reg.status === 0 || reg.status === 1))
      .sort((a, b) => (a.id ?? 0) - (b.id ?? 0))
    if (selectedReg.value) {
      selectedReg.value = waitingList.value.find(reg => reg.id === selectedReg.value?.id) || null
    }
  } catch {} finally {
    waitingLoading.value = false
  }
}

function handleSelectReg(row: Registration) { selectedReg.value = row }

function waitingRowClassName({ row }: { row: Registration }) {
  return row.id === selectedReg.value?.id ? 'workstation-view__selected-row' : ''
}

async function handleCall() {
  if (!selectedReg.value) return
  try {
    await callPatient(selectedReg.value.id!)
    ElMessage.success('叫号成功')
    await loadWaiting()
  } catch {}
}

async function handleComplete() {
  if (!selectedReg.value) return
  try {
    await completeRegistration(selectedReg.value.id!)
    ElMessage.success('就诊完成')
    selectedReg.value = null
    resetRecordForm()
    await loadWaiting()
  } catch {}
}

async function saveRecord() {
  if (!selectedReg.value) return
  saveLoading.value = true
  try {
    await createMedicalRecord({ registrationId: selectedReg.value.id!, patientId: selectedReg.value.patientId, doctorId: selectedReg.value.doctorId || selectedDoctorId.value!, ...recordForm })
    ElMessage.success('病历保存成功')
  } catch {} finally {
    saveLoading.value = false
  }
}

function resetRecordForm() {
  Object.assign(recordForm, { chiefComplaint:'', presentIllness:'', pastHistory:'', physicalExam:'', diagnosis:'', advice:'' })
}

onMounted(loadDoctors)
</script>

<style scoped>
.workstation-view {
  height: max(560px, calc(100vh - var(--header-height) - var(--content-padding) * 2));
  display: flex;
  flex-direction: column;
  min-width: 0;
  animation: fadeIn 0.4s ease-out;
}

.workstation-view__workspace {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(360px, 0.9fr) minmax(0, 1.1fr);
  gap: var(--space-xl);
}

.workstation-view__panel {
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-card);
  overflow: hidden;
}

.workstation-view__panel :deep(.el-card__header) {
  flex-shrink: 0;
}

.workstation-view__panel :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: var(--space-lg);
}

.workstation-view__panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
}

.workstation-view__panel-title {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  font-weight: 600;
  color: var(--text-primary);
}

.workstation-view__panel-meta {
  color: var(--text-muted);
  font-size: var(--font-size-sm);
  white-space: nowrap;
}

.workstation-view__toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}

.workstation-view__toolbar :deep(.workstation-view__doctor-select) {
  flex: 1;
  min-width: 0;
}

.workstation-view__table-wrap {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.workstation-view__table-wrap :deep(.el-table) {
  width: 100%;
}

.workstation-view__table-wrap :deep(.workstation-view__selected-row > td) {
  background-color: var(--color-primary-light) !important;
}

.workstation-view__footer {
  flex-shrink: 0;
  margin-top: var(--space-lg);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
}

.workstation-view__selection {
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

.workstation-view__selection--empty {
  color: var(--text-muted);
}

.workstation-view__actions {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  flex-shrink: 0;
}

.workstation-view__record-wrap {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: var(--space-xs);
}

.workstation-view__record-empty {
  min-height: 100%;
  display: flex;
  align-items: center;
}

.workstation-view__patient-strip {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-md);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  margin-bottom: var(--space-md);
}

.workstation-view__record-form {
  min-width: 0;
}

.workstation-view__record-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

@media (max-width: 1180px) {
  .workstation-view {
    height: auto;
    min-height: max(560px, calc(100vh - var(--header-height) - var(--content-padding) * 2));
  }

  .workstation-view__workspace {
    grid-template-columns: 1fr;
    height: auto;
  }

  .workstation-view__panel {
    height: auto;
    min-height: 520px;
  }

  .workstation-view__table-wrap,
  .workstation-view__record-wrap {
    flex: none;
    height: clamp(360px, 46vh, 560px);
    min-height: 0;
  }
}

@media (max-width: 768px) {
  .workstation-view__toolbar,
  .workstation-view__footer,
  .workstation-view__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .workstation-view__table-wrap,
  .workstation-view__record-wrap {
    height: 320px;
  }

  .workstation-view__toolbar :deep(.el-button),
  .workstation-view__actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
