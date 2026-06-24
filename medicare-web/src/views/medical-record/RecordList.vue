<template>
  <div class="record-list">
    <PageHeader title="病历管理" subtitle="查看已保存的病历记录" />

    <el-card shadow="hover" class="data-card">
      <DataToolbar
        v-model:searchModelValue="keyword"
        search-placeholder="搜索患者姓名"
        show-refresh
        @search="handleSearch"
        @refresh="loadData"
      />

      <el-table v-loading="loading" :data="filteredList" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="patientName" label="患者" width="100" />
        <el-table-column prop="doctorName" label="医生" width="100" />
        <el-table-column prop="chiefComplaint" label="主诉" show-overflow-tooltip />
        <el-table-column prop="diagnosis" label="诊断" show-overflow-tooltip />
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" type="primary" @click="viewDetail(row)">详情</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState
        v-if="!loading && filteredList.length === 0"
        icon="Document"
        title="暂无病历数据"
        description="医生在接诊并保存病历后会显示在此"
      />
    </el-card>

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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { listMedicalRecords } from '../../api/medical-record'
import type { MedicalRecord } from '../../types'
import PageHeader from '../../components/PageHeader.vue'
import DataToolbar from '../../components/DataToolbar.vue'
import EmptyState from '../../components/EmptyState.vue'

const recordList = ref<MedicalRecord[]>([])
const keyword = ref('')
const detailVisible = ref(false)
const currentRecord = ref<MedicalRecord | null>(null)
const loading = ref(false)

const filteredList = computed(() => {
  if (!keyword.value) return recordList.value
  const k = keyword.value.toLowerCase()
  return recordList.value.filter(r => r.patientName?.toLowerCase().includes(k))
})

async function loadData() {
  loading.value = true
  try { const r = await listMedicalRecords(); recordList.value = r.data } catch {}
  loading.value = false
}

function handleSearch() {}

function viewDetail(row: MedicalRecord) { currentRecord.value = row; detailVisible.value = true }

onMounted(loadData)
</script>

<style scoped>
.record-list {
  animation: fadeIn 0.4s ease-out;
}
.data-card {
  border-radius: var(--radius-lg);
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
</style>
