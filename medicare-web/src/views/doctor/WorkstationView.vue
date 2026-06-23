<template>
  <div class="workstation-view">
    <PageHeader title="医生工作站" subtitle="叫号、接诊与病历书写" />

    <el-row :gutter="20">
      <el-col :span="10">
        <el-card shadow="hover" class="data-card">
          <template #header><span class="card-title">📋 候诊列表</span></template>
          <div class="toolbar">
            <el-select v-model="selectedDoctorId" placeholder="选择医生" style="width:100%" @change="loadWaiting">
              <el-option v-for="d in doctorList" :key="d.id" :label="`${d.name} - ${d.departmentName}`" :value="d.id" />
            </el-select>
          </div>
          <el-table v-loading="waitingLoading" :data="waitingList" stripe border highlight-current-row @row-click="handleSelectReg" style="width:100%">
            <el-table-column prop="seqNo" label="序号" />
            <el-table-column prop="patientName" label="患者" />
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                <StatusTag :type="row.status === 0 ? 'warning' : 'primary'" :label="['候诊','就诊中','已完成'][row.status] || '未知'" />
              </template>
            </el-table-column>
          </el-table>
          <EmptyState v-if="!waitingLoading && waitingList.length === 0" icon="User" title="暂无候诊患者" description="请选择医生查看今日排班" />
          <div class="action-bar">
            <el-button type="primary" :disabled="!selectedReg || selectedReg.status !== 0" @click="handleCall">叫号</el-button>
            <el-button type="success" :disabled="!selectedReg || selectedReg.status !== 1" @click="handleComplete">完成就诊</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card v-if="selectedReg && selectedReg.status === 1" shadow="hover" class="data-card">
          <template #header><span class="card-title">📝 病历书写</span></template>
          <el-form ref="recordFormRef" :model="recordForm" label-width="90px">
            <el-divider content-position="left">主诉与现病史</el-divider>
            <el-form-item label="主诉"><el-input v-model="recordForm.chiefComplaint" type="textarea" :rows="2" /></el-form-item>
            <el-form-item label="现病史"><el-input v-model="recordForm.presentIllness" type="textarea" :rows="3" /></el-form-item>
            <el-divider content-position="left">检查与诊断</el-divider>
            <el-form-item label="既往史"><el-input v-model="recordForm.pastHistory" /></el-form-item>
            <el-form-item label="体格检查"><el-input v-model="recordForm.physicalExam" /></el-form-item>
            <el-form-item label="诊断"><el-input v-model="recordForm.diagnosis" /></el-form-item>
            <el-form-item label="医嘱"><el-input v-model="recordForm.advice" type="textarea" :rows="2" /></el-form-item>
            <el-form-item><el-button type="primary" :loading="saveLoading" @click="saveRecord">保存病历</el-button></el-form-item>
          </el-form>
        </el-card>
        <EmptyState v-else icon="FirstAidKit" title="请先叫号" description="在左侧选择医生并叫号后进入就诊" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listDoctors } from '../../api/doctor'
import { listRegistrations, callPatient, completeRegistration } from '../../api/registration'
import { createMedicalRecord } from '../../api/medical-record'
import type { Doctor, Registration, MedicalRecord } from '../../types'
import PageHeader from '../../components/PageHeader.vue'
import EmptyState from '../../components/EmptyState.vue'
import StatusTag from '../../components/StatusTag.vue'

const doctorList = ref<Doctor[]>([])
const selectedDoctorId = ref<number>()
const waitingList = ref<Registration[]>([])
const selectedReg = ref<Registration | null>(null)
const recordForm = reactive<Partial<MedicalRecord>>({ chiefComplaint:'', presentIllness:'', pastHistory:'', physicalExam:'', diagnosis:'', advice:'' })
const waitingLoading = ref(false)
const saveLoading = ref(false)

async function loadDoctors() {
  try { const r = await listDoctors(); doctorList.value = r.data } catch {}
}

async function loadWaiting() {
  if (!selectedDoctorId.value) return
  waitingLoading.value = true
  try { const r = await listRegistrations(undefined, undefined); waitingList.value = (r.data as Registration[]).filter(reg => reg.doctorId === selectedDoctorId.value && (reg.status === 0 || reg.status === 1)) } catch {}
  waitingLoading.value = false
}

function handleSelectReg(row: Registration) { selectedReg.value = row }

async function handleCall() {
  if (!selectedReg.value) return
  try { await callPatient(selectedReg.value.id!); ElMessage.success('叫号成功'); loadWaiting() } catch {}
}

async function handleComplete() {
  if (!selectedReg.value) return
  try { await completeRegistration(selectedReg.value.id!); ElMessage.success('就诊完成'); selectedReg.value = null; loadWaiting() } catch {}
}

async function saveRecord() {
  if (!selectedReg.value) return
  saveLoading.value = true
  try {
    await createMedicalRecord({ registrationId: selectedReg.value.id!, patientId: selectedReg.value.patientId, doctorId: selectedReg.value.doctorId || selectedDoctorId.value!, ...recordForm })
    ElMessage.success('病历保存成功')
  } catch {}
  saveLoading.value = false
}

onMounted(loadDoctors)
</script>

<style scoped>
.workstation-view {
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
  margin-bottom: 12px;
}
.action-bar {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}
</style>
