<template>
  <el-card>
    <template #header><span>病历管理</span></template>
    <el-table :data="recordList" stripe border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="patientName" label="患者" width="100" />
      <el-table-column prop="doctorName" label="医生" width="100" />
      <el-table-column prop="chiefComplaint" label="主诉" show-overflow-tooltip />
      <el-table-column prop="diagnosis" label="诊断" show-overflow-tooltip />
      <el-table-column label="操作" width="100">
        <template #default="{ row }"><el-button text type="primary" @click="viewDetail(row)">详情</el-button></template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="detailVisible" title="病历详情" width="600px">
      <el-descriptions :column="1" border v-if="currentRecord">
        <el-descriptions-item label="患者">{{ currentRecord.patientName }}</el-descriptions-item>
        <el-descriptions-item label="医生">{{ currentRecord.doctorName }}</el-descriptions-item>
        <el-descriptions-item label="主诉">{{ currentRecord.chiefComplaint }}</el-descriptions-item>
        <el-descriptions-item label="现病史">{{ currentRecord.presentIllness }}</el-descriptions-item>
        <el-descriptions-item label="既往史">{{ currentRecord.pastHistory }}</el-descriptions-item>
        <el-descriptions-item label="体格检查">{{ currentRecord.physicalExam }}</el-descriptions-item>
        <el-descriptions-item label="诊断">{{ currentRecord.diagnosis }}</el-descriptions-item>
        <el-descriptions-item label="医嘱">{{ currentRecord.advice }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listMedicalRecords } from '../../api/medical-record'
import type { MedicalRecord } from '../../types'

const recordList = ref<MedicalRecord[]>([])
const detailVisible = ref(false)
const currentRecord = ref<MedicalRecord | null>(null)

async function loadData() { try { const r = await listMedicalRecords(); recordList.value = r.data } catch {} }
function viewDetail(row: MedicalRecord) { currentRecord.value = row; detailVisible.value = true }

onMounted(loadData)
</script>