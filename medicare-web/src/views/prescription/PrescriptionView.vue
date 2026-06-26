<template>
  <div class="prescription-view">
    <PageHeader title="处方管理" subtitle="为已就诊患者开立处方、确认取药与作废" />

    <div class="prescription-view__workspace">
      <el-card shadow="hover" class="prescription-view__panel">
        <template #header>
          <div class="prescription-view__panel-header">
            <div class="prescription-view__panel-title">
              <el-icon><User /></el-icon>
              <span>已就诊患者</span>
            </div>
            <span class="prescription-view__panel-meta">可开方 {{ completedRegs.length }} 位</span>
          </div>
        </template>

        <div class="prescription-view__toolbar">
          <el-button :icon="Refresh" :loading="regLoading" @click="loadRegs">刷新</el-button>
        </div>

        <div class="prescription-view__table-wrap">
          <el-table
            v-loading="regLoading"
            :data="displayCompletedRegs"
            stripe
            border
            highlight-current-row
            height="100%"
            :default-sort="{ prop: 'code', order: 'ascending' }"
            :row-class-name="patientRowClassName"
            @row-click="handleSelectPatient"
          >
            <template #empty>
              <EmptyState icon="User" title="暂无已就诊患者" description="医生完成就诊后此处显示" />
            </template>
            <el-table-column type="index" label="序号" width="60" align="center" fixed="left" />
            <el-table-column prop="code" label="ID" width="120" align="center" />
            <el-table-column prop="seqNo" label="排队号" width="76" align="center" />
            <el-table-column prop="patientName" label="患者" min-width="120" align="center" />
            <el-table-column prop="doctorName" label="医生" min-width="120" align="center">
              <template #header>
                <div class="table-filter-header">
                  <span>医生</span>
                  <el-dropdown trigger="click" :hide-on-click="false" popper-class="table-filter-dropdown">
                    <el-icon class="table-filter-icon" :class="{ 'is-active': activeFiltersReg.doctorName.length }"><Filter /></el-icon>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-checkbox-group v-model="activeFiltersReg.doctorName" class="table-filter-group">
                          <el-dropdown-item v-for="opt in doctorFilters" :key="opt.value">
                            <el-checkbox :label="opt.value">{{ opt.text }}</el-checkbox>
                          </el-dropdown-item>
                        </el-checkbox-group>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="departmentName" label="科室" min-width="120" align="center">
              <template #header>
                <div class="table-filter-header">
                  <span>科室</span>
                  <el-dropdown trigger="click" :hide-on-click="false" popper-class="table-filter-dropdown">
                    <el-icon class="table-filter-icon" :class="{ 'is-active': activeFiltersReg.departmentName.length }"><Filter /></el-icon>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-checkbox-group v-model="activeFiltersReg.departmentName" class="table-filter-group">
                          <el-dropdown-item v-for="opt in deptFilters" :key="opt.value">
                            <el-checkbox :label="opt.value">{{ opt.text }}</el-checkbox>
                          </el-dropdown-item>
                        </el-checkbox-group>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="timeSlot" label="时段" min-width="90" align="center">
              <template #header>
                <div class="table-filter-header">
                  <span>时段</span>
                  <el-dropdown trigger="click" :hide-on-click="false" popper-class="table-filter-dropdown">
                    <el-icon class="table-filter-icon" :class="{ 'is-active': activeFiltersReg.timeSlot.length }"><Filter /></el-icon>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-checkbox-group v-model="activeFiltersReg.timeSlot" class="table-filter-group">
                          <el-dropdown-item v-for="opt in timeSlotFilters" :key="opt.value">
                            <el-checkbox :label="opt.value">{{ opt.text }}</el-checkbox>
                          </el-dropdown-item>
                        </el-checkbox-group>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="116" fixed="right" align="center">
              <template #default="{ row }">
                <el-button size="small" type="primary" text @click.stop="handleSelectPatient(row)">选择</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="prescription-view__footer">
          <div class="prescription-view__selection" :class="{ 'prescription-view__selection--empty': !selectedReg }">
            <template v-if="selectedReg">
              <span>{{ selectedReg.patientName || '-' }}</span>
              <span>{{ selectedReg.doctorName || '-' }}</span>
              <span>{{ selectedReg.timeSlot || '-' }}</span>
            </template>
            <template v-else>请选择已就诊患者</template>
          </div>
        </div>
      </el-card>

      <el-card shadow="hover" class="prescription-view__panel">
        <template #header>
          <div class="prescription-view__panel-header">
            <div class="prescription-view__panel-title">
              <el-icon><Goods /></el-icon>
              <span>处方编辑</span>
            </div>
            <span class="prescription-view__panel-meta">
              {{ existingPrescription ? prescStatusText(existingPrescription.status) : selectedReg ? '待开立' : '未选择患者' }}
            </span>
          </div>
        </template>

        <div class="prescription-view__editor-wrap">
          <template v-if="selectedReg">
            <div class="prescription-view__patient-strip">
              <span>患者: {{ selectedReg.patientName || '-' }}</span>
              <span>医生: {{ selectedReg.doctorName || '-' }}</span>
              <span v-if="existingPrescription" class="prescription-view__status-inline">
                处方:
                <StatusTag :type="prescStatusType(existingPrescription.status)" :label="prescStatusText(existingPrescription.status)" />
              </span>
            </div>

            <div v-if="!existingPrescription" class="prescription-view__editor-section">
              <div class="prescription-view__medicine-toolbar">
                <el-input
                  v-model="medKw"
                  placeholder="搜索药品"
                  clearable
                  :prefix-icon="Search"
                  class="prescription-view__medicine-keyword"
                  @keyup.enter="searchMeds"
                />
                <el-select v-model="selectedMedId" placeholder="选择药品" filterable class="prescription-view__medicine-select">
                  <el-option v-for="m in medResults" :key="m.id" :label="`${m.name} (${m.spec}) ¥${m.price}`" :value="m.id" />
                </el-select>
                <el-input-number v-model="itemQty" :min="1" :max="999" class="prescription-view__quantity-input" />
                <el-input v-model="itemUsage" placeholder="用法用量" class="prescription-view__usage-input" />
                <el-button :icon="Search" @click="searchMeds">搜索</el-button>
                <el-button type="primary" :icon="Plus" @click="addItem">添加</el-button>
              </div>

              <div class="prescription-view__items-wrap">
                <el-table
                  :data="displaySortedItems"
                  stripe
                  border
                  height="100%"
                  :default-sort="{ prop: 'medicineId', order: 'ascending' }"
                >
                  <template #empty>
                    <EmptyState icon="Goods" title="暂无处方明细" description="搜索药品后添加到处方" />
                  </template>
                  <el-table-column type="index" label="序号" width="60" align="center" fixed="left" />
                  <el-table-column prop="medicineCode" label="ID" width="120" align="center" />
                  <el-table-column prop="medicineName" label="药品" min-width="150">
                    <template #header>
                      <div class="table-filter-header">
                        <span>药品</span>
                        <el-dropdown trigger="click" :hide-on-click="false" popper-class="table-filter-dropdown">
                          <el-icon class="table-filter-icon" :class="{ 'is-active': activeFiltersItems.medicineName.length }"><Filter /></el-icon>
                          <template #dropdown>
                            <el-dropdown-menu>
                              <el-checkbox-group v-model="activeFiltersItems.medicineName" class="table-filter-group">
                                <el-dropdown-item v-for="opt in itemNameFilters" :key="opt.value">
                                  <el-checkbox :label="opt.value">{{ opt.text }}</el-checkbox>
                                </el-dropdown-item>
                              </el-checkbox-group>
                            </el-dropdown-menu>
                          </template>
                        </el-dropdown>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="medicineSpec" label="规格" min-width="110" align="center" />
                  <el-table-column prop="quantity" label="数量" width="76" align="center" />
                  <el-table-column prop="unitPrice" label="单价" width="90" align="center">
                    <template #default="{ row }">{{ formatMoney(row.unitPrice) }}</template>
                  </el-table-column>
                  <el-table-column prop="amount" label="金额" width="90" align="center">
                    <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
                  </el-table-column>
                  <el-table-column prop="usageDesc" label="用法" min-width="160" />
                  <el-table-column label="操作" width="96" fixed="right" align="center">
                    <template #default="{ $index }">
                      <el-popconfirm title="确定移除该药品?" @confirm="removeItem($index)">
                        <template #reference><el-button size="small" type="danger" :icon="Delete">移除</el-button></template>
                      </el-popconfirm>
                    </template>
                  </el-table-column>
                </el-table>
              </div>

              <div class="prescription-view__editor-footer">
                <el-text>总金额: <strong>¥{{ totalAmount.toFixed(2) }}</strong></el-text>
                <el-button type="primary" :icon="DocumentChecked" :loading="saveLoading" :disabled="items.length === 0" @click="savePrescription">保存处方</el-button>
              </div>
            </div>

            <div v-else class="prescription-view__editor-section">
              <div class="prescription-view__items-wrap">
                <el-table
                  :data="displaySortedExistingItems"
                  stripe
                  border
                  height="100%"
                  :default-sort="{ prop: 'medicineId', order: 'ascending' }"
                >
                  <template #empty>
                    <EmptyState icon="Goods" title="暂无处方明细" />
                  </template>
                  <el-table-column type="index" label="序号" width="60" align="center" fixed="left" />
                  <el-table-column prop="code" label="ID" width="120" align="center" />
                  <el-table-column prop="medicineName" label="药品" min-width="150">
                    <template #header>
                      <div class="table-filter-header">
                        <span>药品</span>
                        <el-dropdown trigger="click" :hide-on-click="false" popper-class="table-filter-dropdown">
                          <el-icon class="table-filter-icon" :class="{ 'is-active': activeFiltersExisting.medicineName.length }"><Filter /></el-icon>
                          <template #dropdown>
                            <el-dropdown-menu>
                              <el-checkbox-group v-model="activeFiltersExisting.medicineName" class="table-filter-group">
                                <el-dropdown-item v-for="opt in existingItemNameFilters" :key="opt.value">
                                  <el-checkbox :label="opt.value">{{ opt.text }}</el-checkbox>
                                </el-dropdown-item>
                              </el-checkbox-group>
                            </el-dropdown-menu>
                          </template>
                        </el-dropdown>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="medicineSpec" label="规格" min-width="110" align="center" />
                  <el-table-column prop="quantity" label="数量" width="76" align="center" />
                  <el-table-column prop="unitPrice" label="单价" width="90" align="center">
                    <template #default="{ row }">{{ formatMoney(row.unitPrice) }}</template>
                  </el-table-column>
                  <el-table-column prop="usageDesc" label="用法" min-width="160" />
                  <el-table-column prop="amount" label="金额" width="90" fixed="right" align="center">
                    <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
                  </el-table-column>
                </el-table>
              </div>

              <div class="prescription-view__editor-footer">
                <el-text>总金额: <strong>¥{{ (existingPrescription.totalAmount || 0).toFixed(2) }}</strong></el-text>
                <div class="prescription-view__editor-actions">
                  <el-button v-if="existingPrescription.status === 0" type="success" :icon="CircleCheck" :loading="dispenseLoading" @click="handleDispense">确认取药</el-button>
                  <el-button v-if="existingPrescription.status === 0" type="danger" :icon="Delete" :loading="cancelLoading" @click="handleCancel">作废处方</el-button>
                </div>
              </div>
            </div>
          </template>

          <div v-else class="prescription-view__editor-empty">
            <EmptyState icon="Document" title="请先选择患者" description="在左侧点击已就诊患者开始开立处方" />
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, Delete, DocumentChecked, Filter, Goods, Plus, Refresh, Search, User } from '@element-plus/icons-vue'
import { listRegistrations } from '../../api/registration'
import { listMedicines } from '../../api/medicine'
import { getByRecord, createPrescription, dispensePrescription, cancelPrescription } from '../../api/prescription'
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
const formatMoney = (amount?: number) => `¥${Number(amount || 0).toFixed(2)}`

// 按 medicineId 升序展示处方明细
const sortedItems = computed(() =>
  [...items.value].sort((a, b) => (a.medicineId ?? 0) - (b.medicineId ?? 0))
)
const sortedExistingItems = computed(() =>
  [...(existingPrescription.value?.items || [])].sort((a, b) => (a.medicineId ?? 0) - (b.medicineId ?? 0))
)

function uniqueOptions<T extends Record<string, any>>(list: T[], key: keyof T) {
  const values = Array.from(new Set(list.map(item => item[key]).filter(v => v !== undefined && v !== null && v !== '')))
  return values.map(v => ({ text: String(v), value: v }))
}

const deptFilters = computed(() => uniqueOptions(completedRegs.value, 'departmentName'))
const doctorFilters = computed(() => uniqueOptions(completedRegs.value, 'doctorName'))
const timeSlotFilters = computed(() => uniqueOptions(completedRegs.value, 'timeSlot'))
const itemNameFilters = computed(() => uniqueOptions(items.value, 'medicineName'))
const existingItemNameFilters = computed(() => uniqueOptions(existingPrescription.value?.items || [], 'medicineName'))

const activeFiltersReg = ref({
  doctorName: [] as string[],
  departmentName: [] as string[],
  timeSlot: [] as string[],
})
const activeFiltersItems = ref({
  medicineName: [] as string[],
})
const activeFiltersExisting = ref({
  medicineName: [] as string[],
})

const displayCompletedRegs = computed(() => {
  return completedRegs.value.filter(row => {
    if (activeFiltersReg.value.doctorName.length && !activeFiltersReg.value.doctorName.includes(row.doctorName || '')) return false
    if (activeFiltersReg.value.departmentName.length && !activeFiltersReg.value.departmentName.includes(row.departmentName || '')) return false
    if (activeFiltersReg.value.timeSlot.length && !activeFiltersReg.value.timeSlot.includes(row.timeSlot || '')) return false
    return true
  })
})
const displaySortedItems = computed(() => {
  return sortedItems.value.filter(row => {
    if (activeFiltersItems.value.medicineName.length && !activeFiltersItems.value.medicineName.includes(row.medicineName || '')) return false
    return true
  })
})
const displaySortedExistingItems = computed(() => {
  return sortedExistingItems.value.filter(row => {
    if (activeFiltersExisting.value.medicineName.length && !activeFiltersExisting.value.medicineName.includes(row.medicineName || '')) return false
    return true
  })
})

async function loadRegs() {
  regLoading.value = true
  try {
    const r = await listRegistrations()
    completedRegs.value = (r.data as Registration[])
      .filter(reg => reg.status === 2)
      .sort((a, b) => (a.id ?? 0) - (b.id ?? 0))
    if (selectedReg.value && !completedRegs.value.some(reg => reg.id === selectedReg.value?.id)) {
      selectedReg.value = null
      existingPrescription.value = null
      items.value = []
    }
  } catch {} finally {
    regLoading.value = false
  }
}

async function handleSelectPatient(row: Registration) {
  selectedReg.value = row
  items.value = []
  existingPrescription.value = null
  medKw.value = ''
  medResults.value = []
  selectedMedId.value = undefined
  itemQty.value = 1
  itemUsage.value = '遵医嘱'
  try {
    const recs = await listMedicalRecords(row.patientId, row.id)
    const rec = (recs.data as any[])[0]
    if (rec) {
      try {
        const pr = await getByRecord(rec.id)
        existingPrescription.value = pr.data
      } catch {}
    }
  } catch {}
}

async function searchMeds() {
  try { const r = await listMedicines(medKw.value || undefined); medResults.value = r.data } catch {}
}

function addItem() {
  const med = medResults.value.find(m => m.id === selectedMedId.value)
  if (!med) { ElMessage.warning('请选择药品'); return }
  if (items.value.find(i => i.medicineId === med.id)) { ElMessage.warning('该药品已添加'); return }
  items.value.push({
    medicineId: med.id!, quantity: itemQty.value, usageDesc: itemUsage.value || '遵医嘱',
    unitPrice: med.price, amount: (med.price || 0) * itemQty.value,
    medicineName: med.name, medicineSpec: med.spec, medicineUnit: med.unit, medicineCode: med.code,
  })
  medKw.value = ''; selectedMedId.value = undefined; itemQty.value = 1; itemUsage.value = '遵医嘱'
}

function removeItem(index: number) {
  items.value.splice(index, 1)
}

function patientRowClassName({ row }: { row: Registration }) {
  return row.id === selectedReg.value?.id ? 'prescription-view__selected-row' : ''
}

async function savePrescription() {
  if (!selectedReg.value || items.value.length === 0) return
  const currentReg = selectedReg.value
  saveLoading.value = true
  try {
    const recs = await listMedicalRecords(currentReg.patientId, currentReg.id)
    const rec = (recs.data as any[])[0]
    if (!rec) { ElMessage.error('未找到病历记录'); return }
    if (!currentReg.doctorId) { ElMessage.error('缺少医生信息'); return }
    await createPrescription({
      prescription: { recordId: rec.id, patientId: currentReg.patientId, doctorId: currentReg.doctorId, status: 0 },
      items: items.value,
    })
    ElMessage.success('处方开立成功')
    items.value = []
    await handleSelectPatient(currentReg)
  } catch {} finally {
    saveLoading.value = false
  }
}

async function handleDispense() {
  if (!existingPrescription.value) return
  const currentReg = selectedReg.value
  dispenseLoading.value = true
  try {
    await dispensePrescription(existingPrescription.value.id!)
    ElMessage.success('取药成功')
    if (currentReg) await handleSelectPatient(currentReg)
  } catch {} finally {
    dispenseLoading.value = false
  }
}

async function handleCancel() {
  if (!existingPrescription.value) return
  const currentReg = selectedReg.value
  cancelLoading.value = true
  try {
    await cancelPrescription(existingPrescription.value.id!)
    ElMessage.success('处方已作废')
    if (currentReg) await handleSelectPatient(currentReg)
  } catch {} finally {
    cancelLoading.value = false
  }
}

onMounted(loadRegs)
</script>

<style scoped>
.prescription-view {
  height: max(560px, calc(100vh - var(--header-height) - var(--content-padding) * 2));
  display: flex;
  flex-direction: column;
  min-width: 0;
  animation: fadeIn 0.4s ease-out;
}

.prescription-view__workspace {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(360px, 0.9fr) minmax(0, 1.1fr);
  gap: var(--space-xl);
}

.prescription-view__panel {
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-card);
  overflow: hidden;
}

.prescription-view__panel :deep(.el-card__header) {
  flex-shrink: 0;
}

.prescription-view__panel :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: var(--space-lg);
}

.prescription-view__panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
}

.prescription-view__panel-title {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  font-weight: 600;
  color: var(--text-primary);
}

.prescription-view__panel-meta {
  color: var(--text-muted);
  font-size: var(--font-size-sm);
  white-space: nowrap;
}

.prescription-view__toolbar,
.prescription-view__medicine-toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex-wrap: wrap;
  margin-bottom: var(--space-lg);
}

.prescription-view__table-wrap,
.prescription-view__items-wrap {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.prescription-view__table-wrap :deep(.el-table),
.prescription-view__items-wrap :deep(.el-table) {
  width: 100%;
}

.prescription-view__table-wrap :deep(.prescription-view__selected-row > td) {
  background-color: var(--color-primary-light) !important;
}

.prescription-view__footer,
.prescription-view__editor-footer {
  flex-shrink: 0;
  margin-top: var(--space-lg);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
}

.prescription-view__selection {
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

.prescription-view__selection--empty {
  color: var(--text-muted);
}

.prescription-view__editor-wrap,
.prescription-view__editor-section {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.prescription-view__editor-empty {
  min-height: 100%;
  display: flex;
  align-items: center;
}

.prescription-view__patient-strip {
  flex-shrink: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-md);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  margin-bottom: var(--space-md);
}

.prescription-view__status-inline {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
}

.prescription-view__medicine-toolbar :deep(.prescription-view__medicine-keyword) {
  width: 170px;
  flex: 0 0 170px;
}

.prescription-view__medicine-toolbar :deep(.prescription-view__medicine-select) {
  width: 220px;
  flex: 0 0 220px;
}

.prescription-view__medicine-toolbar :deep(.prescription-view__quantity-input) {
  width: 120px;
  flex: 0 0 120px;
}

.prescription-view__medicine-toolbar :deep(.prescription-view__usage-input) {
  width: 150px;
  flex: 0 0 150px;
}

.prescription-view__editor-actions {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

@media (max-width: 1180px) {
  .prescription-view {
    height: auto;
    min-height: max(560px, calc(100vh - var(--header-height) - var(--content-padding) * 2));
  }

  .prescription-view__workspace {
    grid-template-columns: 1fr;
    height: auto;
  }

  .prescription-view__panel {
    height: auto;
    min-height: 520px;
  }

  .prescription-view__table-wrap,
  .prescription-view__items-wrap {
    flex: none;
    height: clamp(360px, 46vh, 560px);
    min-height: 0;
  }
}

@media (max-width: 768px) {
  .prescription-view__toolbar,
  .prescription-view__medicine-toolbar,
  .prescription-view__footer,
  .prescription-view__editor-footer,
  .prescription-view__editor-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .prescription-view__table-wrap,
  .prescription-view__items-wrap {
    height: 320px;
  }

  .prescription-view__medicine-toolbar :deep(.prescription-view__medicine-keyword),
  .prescription-view__medicine-toolbar :deep(.prescription-view__medicine-select),
  .prescription-view__medicine-toolbar :deep(.prescription-view__quantity-input),
  .prescription-view__medicine-toolbar :deep(.prescription-view__usage-input),
  .prescription-view__toolbar :deep(.el-button),
  .prescription-view__medicine-toolbar :deep(.el-button),
  .prescription-view__editor-footer :deep(.el-button) {
    width: 100%;
  }
}
</style>
