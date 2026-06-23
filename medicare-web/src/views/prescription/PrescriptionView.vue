<template>
  <div class="prescription-view">
    <PageHeader title="处方管理" subtitle="为已就诊患者开立处方、确认取药与作废" />

    <el-row :gutter="20">
      <el-col :span="10">
        <el-card shadow="hover" class="data-card">
          <template #header><span class="card-title">👤 已就诊患者</span></template>
          <el-table v-loading="regLoading" :data="completedRegs" stripe border highlight-current-row @row-click="handleSelectPatient">
            <el-table-column prop="seqNo" label="序号" width="60" />
            <el-table-column prop="patientName" label="患者" />
            <el-table-column prop="doctorName" label="医生" />
          </el-table>
          <EmptyState v-if="!regLoading && completedRegs.length === 0" icon="User" title="暂无已就诊患者" description="医生完成就诊后此处显示" />
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="presc-header">
              <span class="card-title">📝 处方编辑</span>
              <el-text v-if="existingPrescription">
                已有处方:
                <StatusTag :type="prescStatusType(existingPrescription.status)" :label="prescStatusText(existingPrescription.status)" />
              </el-text>
            </div>
          </template>
          <div v-if="selectedReg">
            <el-text>患者: {{ selectedReg.patientName }} | 医生: {{ selectedReg.doctorName }}</el-text>
            <el-divider />
            <div v-if="!existingPrescription">
              <div class="presc-toolbar">
                <el-input v-model="medKw" placeholder="搜索药品" style="width:180px" @keyup.enter="searchMeds" />
                <el-select v-model="selectedMedId" placeholder="选择药品" style="width:220px">
                  <el-option v-for="m in medResults" :key="m.id" :label="`${m.name} (${m.spec}) ¥${m.price}`" :value="m.id" />
                </el-select>
                <el-input-number v-model="itemQty" :min="1" :max="999" style="width:120px" />
                <el-input v-model="itemUsage" placeholder="用法用量" style="width:150px" />
                <el-button type="primary" @click="addItem">添加</el-button>
              </div>
              <el-table :data="items" stripe border>
                <el-table-column prop="medicineName" label="药品" />
                <el-table-column prop="medicineSpec" label="规格" width="100" />
                <el-table-column prop="quantity" label="数量" width="60" />
                <el-table-column prop="unitPrice" label="单价" width="80">
                  <template #default="{ row }">¥{{ (row.unitPrice || 0).toFixed(2) }}</template>
                </el-table-column>
                <el-table-column prop="amount" label="金额" width="80">
                  <template #default="{ row }">¥{{ (row.amount || 0).toFixed(2) }}</template>
                </el-table-column>
                <el-table-column prop="usageDesc" label="用法" />
                <el-table-column label="操作" width="80" align="center">
                  <template #default="{ $index }">
                    <el-popconfirm title="确定移除该药品?" @confirm="items.splice($index, 1)">
                      <template #reference><el-button size="small" type="danger">移除</el-button></template>
                    </el-popconfirm>
                  </template>
                </el-table-column>
              </el-table>
              <div class="presc-footer">
                <el-text>总金额: <strong>¥{{ totalAmount.toFixed(2) }}</strong></el-text>
                <el-button type="primary" :loading="saveLoading" :disabled="items.length === 0" @click="savePrescription">保存处方</el-button>
              </div>
            </div>
            <div v-else>
              <el-table :data="existingPrescription.items || []" stripe border>
                <el-table-column prop="medicineName" label="药品" />
                <el-table-column prop="quantity" label="数量" width="60" />
                <el-table-column prop="unitPrice" label="单价" width="80">
                  <template #default="{ row }">¥{{ (row.unitPrice || 0).toFixed(2) }}</template>
                </el-table-column>
                <el-table-column prop="usageDesc" label="用法" />
              </el-table>
              <div class="presc-footer">
                <el-text>总金额: <strong>¥{{ (existingPrescription.totalAmount || 0).toFixed(2) }}</strong></el-text>
                <div>
                  <el-button v-if="existingPrescription.status === 0" type="success" :loading="dispenseLoading" @click="handleDispense">确认取药</el-button>
                  <el-button v-if="existingPrescription.status === 0" type="danger" :loading="cancelLoading" @click="handleCancel">作废处方</el-button>
                </div>
              </div>
            </div>
          </div>
          <EmptyState v-else icon="Document" title="请先选择患者" description="在左侧点击已就诊患者开始开立处方" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listRegistrations } from '../../api/registration'
import { listMedicines } from '../../api/medicine'
import { listPrescriptions, getByRecord, createPrescription, dispensePrescription, cancelPrescription } from '../../api/prescription'
import { listMedicalRecords } from '../../api/medical-record'
import type { Registration, Medicine, Prescription, PrescriptionItem } from '../../types'
import PageHeader from '../../components/PageHeader.vue'
import EmptyState from '../../components/EmptyState.vue'
import StatusTag from '../../components/StatusTag.vue'

const completedRegs = ref<Registration[]>([])
const selectedReg = ref<Registration | null>(null)
const existingPrescription = ref<Prescription | null>(null)
const medKw = ref('')
const medResults = ref<Medicine[]>([])
const selectedMedId = ref<number>()
const itemQty = ref(1)
const itemUsage = ref('遵医嘱')
const items = ref<PrescriptionItem[]>([])
const regLoading = ref(false)
const saveLoading = ref(false)
const dispenseLoading = ref(false)
const cancelLoading = ref(false)

const prescStatusText = (s: number) => ['待缴费', '已缴费', '已取药', '已作废'][s] || '未知'
const prescStatusType = (s: number): any => (['warning', 'primary', 'success', 'info'] as const)[s] || 'default'
const totalAmount = computed(() => items.value.reduce((sum, i) => sum + (i.amount || 0), 0))

async function loadRegs() {
  regLoading.value = true
  try {
    const r = await listRegistrations()
    completedRegs.value = (r.data as Registration[]).filter(reg => reg.status === 2)
  } catch {}
  regLoading.value = false
}

async function handleSelectPatient(row: Registration) {
  selectedReg.value = row
  items.value = []
  existingPrescription.value = null
  try {
    const recs = await listMedicalRecords(row.patientId, row.id)
    const rec = (recs.data as any[])[0]
    if (rec) {
      const pr = await getByRecord(rec.id)
      existingPrescription.value = pr.data
    }
  } catch {}
}

async function searchMeds() {
  if (!medKw.value) return
  try { const r = await listMedicines(medKw.value); medResults.value = r.data } catch {}
}

function addItem() {
  const med = medResults.value.find(m => m.id === selectedMedId.value)
  if (!med) { ElMessage.warning('请选择药品'); return }
  if (items.value.find(i => i.medicineId === med.id)) { ElMessage.warning('该药品已添加'); return }
  items.value.push({
    medicineId: med.id!, quantity: itemQty.value, usageDesc: itemUsage.value || '遵医嘱',
    unitPrice: med.price, amount: (med.price || 0) * itemQty.value,
    medicineName: med.name, medicineSpec: med.spec, medicineUnit: med.unit,
  })
  medKw.value = ''; selectedMedId.value = undefined; itemQty.value = 1; itemUsage.value = '遵医嘱'
}

async function savePrescription() {
  if (!selectedReg.value || items.value.length === 0) return
  saveLoading.value = true
  try {
    const recs = await listMedicalRecords(selectedReg.value.patientId, selectedReg.value.id)
    const rec = (recs.data as any[])[0]
    if (!rec) { ElMessage.error('未找到病历记录'); return }
    await createPrescription({
      prescription: { recordId: rec.id, patientId: selectedReg.value.patientId, doctorId: selectedReg.value.doctorId || selectedReg.value.doctorId!, status: 0 },
      items: items.value,
    })
    ElMessage.success('处方开立成功')
    items.value = []
    handleSelectPatient(selectedReg.value)
  } catch {}
  saveLoading.value = false
}

async function handleDispense() {
  if (!existingPrescription.value) return
  dispenseLoading.value = true
  try { await dispensePrescription(existingPrescription.value.id!); ElMessage.success('取药成功'); if (selectedReg.value) handleSelectPatient(selectedReg.value) } catch {}
  dispenseLoading.value = false
}

async function handleCancel() {
  if (!existingPrescription.value) return
  cancelLoading.value = true
  try { await cancelPrescription(existingPrescription.value.id!); ElMessage.success('处方已作废'); if (selectedReg.value) handleSelectPatient(selectedReg.value) } catch {}
  cancelLoading.value = false
}

onMounted(loadRegs)
</script>

<style scoped>
.prescription-view {
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
.presc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.presc-toolbar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  align-items: center;
}
.presc-footer {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
