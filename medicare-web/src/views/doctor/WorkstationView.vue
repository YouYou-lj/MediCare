<template>
  <el-card class="workstation-card">
    <template #header><span>医生工作站</span></template>
    <el-row :gutter="20">
      <el-col :span="10">
        <div style="margin-bottom:12px">
          <el-select v-model="selectedDoctorId" placeholder="选择医生" style="width:100%" @change="loadWaiting">
            <el-option v-for="d in doctorList" :key="d.id" :label="`${d.name} - ${d.departmentName}`" :value="d.id" />
          </el-select>
        </div>
        <el-table :data="waitingList" stripe border @row-click="handleSelectReg" highlight-current-row style="width:100%">
          <el-table-column prop="seqNo" label="序号" />
          <el-table-column prop="patientName" label="患者" />
          <el-table-column prop="status" label="状态">
            <template #default="{ row }"><el-tag :type="row.status===0?'warning':''">{{ ['候诊','就诊中','已完成'][row.status] }}</el-tag></template>
          </el-table-column>
        </el-table>
        <div style="margin-top:12px">
          <el-button type="primary" :disabled="!selectedReg || selectedReg.status!==0" @click="handleCall">叫号</el-button>
          <el-button type="success" :disabled="!selectedReg || selectedReg.status!==1" @click="handleComplete">完成就诊</el-button>
        </div>
      </el-col>
      <el-col :span="14">
        <el-card v-if="selectedReg && selectedReg.status===1">
          <template #header>病历书写</template>
          <el-form ref="recordFormRef" :model="recordForm" label-width="90px">
            <el-form-item label="主诉"><el-input v-model="recordForm.chiefComplaint" type="textarea" :rows="2" /></el-form-item>
            <el-form-item label="现病史"><el-input v-model="recordForm.presentIllness" type="textarea" :rows="3" /></el-form-item>
            <el-form-item label="既往史"><el-input v-model="recordForm.pastHistory" /></el-form-item>
            <el-form-item label="体格检查"><el-input v-model="recordForm.physicalExam" /></el-form-item>
            <el-form-item label="诊断"><el-input v-model="recordForm.diagnosis" /></el-form-item>
            <el-form-item label="医嘱"><el-input v-model="recordForm.advice" type="textarea" :rows="2" /></el-form-item>
            <el-form-item><el-button type="primary" @click="saveRecord">保存病历</el-button></el-form-item>
          </el-form>
        </el-card>
        <el-empty v-else description="请先叫号进入就诊" />
      </el-col>
    </el-row>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listDoctors } from '../../api/doctor'
import { listRegistrations, callPatient, completeRegistration } from '../../api/registration'
import { createMedicalRecord } from '../../api/medical-record'
import type { Doctor, Registration, MedicalRecord } from '../../types'

const doctorList = ref<Doctor[]>([])
const selectedDoctorId = ref<number>()
const waitingList = ref<Registration[]>([])
const selectedReg = ref<Registration | null>(null)
const recordForm = reactive<Partial<MedicalRecord>>({ chiefComplaint:'', presentIllness:'', pastHistory:'', physicalExam:'', diagnosis:'', advice:'' })

async function loadDoctors() { try { const r = await listDoctors(); doctorList.value = r.data } catch {} }
async function loadWaiting() {
  if (!selectedDoctorId.value) return
  try { const r = await listRegistrations(undefined, undefined); waitingList.value = (r.data as Registration[]).filter(reg => reg.doctorId === selectedDoctorId.value && (reg.status===0||reg.status===1)) } catch {}
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
  try {
    await createMedicalRecord({ registrationId: selectedReg.value.id!, patientId: selectedReg.value.patientId, doctorId: selectedReg.value.doctorId || selectedDoctorId.value!, ...recordForm })
    ElMessage.success('病历保存成功')
  } catch {}
}

onMounted(loadDoctors)
</script>

<style scoped>
.workstation-card {
  width: 80%;
}
.workstation-card :deep(.el-table) {
  width: 100%;
  table-layout: auto;
}
</style>