<template>
  <div class="medicine-list">
    <PageHeader title="药品库存" subtitle="药品信息维护、库存预警与出入库管理" />

    <div class="medicine-list__workspace">
      <el-card shadow="hover" class="medicine-list__panel">
        <template #header>
          <div class="medicine-list__panel-header">
            <div class="medicine-list__panel-title">
              <el-icon><FirstAidKit /></el-icon>
              <span>药品列表</span>
            </div>
            <span class="medicine-list__panel-meta">共 {{ tableData.length }} 条记录</span>
          </div>
        </template>

        <div class="medicine-list__stats">
          <div class="medicine-list__stat">
            <span class="medicine-list__stat-label">当前结果</span>
            <strong>{{ tableData.length }}</strong>
          </div>
          <div class="medicine-list__stat">
            <span class="medicine-list__stat-label">低库存</span>
            <strong>{{ lowStockCount }}</strong>
          </div>
          <div class="medicine-list__stat">
            <span class="medicine-list__stat-label">库存总量</span>
            <strong>{{ totalStockCount }}</strong>
          </div>
        </div>

        <DataToolbar
          v-model:searchModelValue="keyword"
          search-placeholder="搜索药品名称/拼音码"
          show-refresh
          show-add
          add-label="新增药品"
          :add-disabled="!canManagePharmacy"
          @search="loadData"
          @refresh="loadData"
          @add="openMedDialog()"
        >
          <template #extra>
            <el-button type="warning" :icon="Warning" @click="showLowStock">库存预警</el-button>
          </template>
        </DataToolbar>

        <div class="medicine-list__table-wrap">
          <el-table
            v-loading="loading"
            :data="tableData"
            stripe
            border
            height="100%"
            row-key="id"
            highlight-current-row
            :default-sort="{ prop: 'code', order: 'ascending' }"
            @row-click="openPreview"
          >
            <template #empty>
              <EmptyState
                icon="Box"
                title="暂无药品数据"
                description="点击右上角“新增药品”按钮添加"
              />
            </template>
            <el-table-column type="index" label="序号" width="60" align="center" :resizable="false" />
            <el-table-column prop="code" label="ID" width="120" align="center" :resizable="false" />
            <el-table-column prop="name" label="药品名称" min-width="180" show-overflow-tooltip :resizable="false" />
            <el-table-column prop="spec" label="规格" min-width="150" show-overflow-tooltip :resizable="false" align="center" />
            <el-table-column prop="unit" label="单位" min-width="90" :resizable="false" align="center" />
            <el-table-column prop="stock" label="库存" min-width="130" align="center" class-name="medicine-list__tag-cell" :resizable="false">
              <template #default="{ row }">
                <StatusTag :type="stockStatusType(row)" :label="stockStatusLabel(row)" />
              </template>
            </el-table-column>
            <el-table-column prop="safetyStock" label="安全库存" min-width="120" :resizable="false" align="center" />
            <el-table-column prop="price" label="零售价" min-width="110" :resizable="false" align="center">
              <template #default="{ row }">{{ formatMoney(row.price) }}</template>
            </el-table-column>
            <el-table-column prop="expiryDate" label="有效期" min-width="130" :resizable="false" align="center">
              <template #default="{ row }">{{ displayText(row.expiryDate) }}</template>
            </el-table-column>
            <el-table-column prop="batchNo" label="批号" min-width="140" show-overflow-tooltip :resizable="false" align="center">
              <template #default="{ row }">{{ displayText(row.batchNo) }}</template>
            </el-table-column>
            <el-table-column prop="manufacturer" label="厂家" min-width="220" show-overflow-tooltip :resizable="false" />
            <el-table-column label="操作" width="220" align="center" :resizable="false">
              <template #default="{ row }">
                <div class="medicine-list__actions">
                  <div class="medicine-list__action-group">
                    <div class="medicine-list__action-row">
                      <el-button size="small" type="success" :disabled="!canManagePharmacy" @click.stop="openStockDialog(row, 'in')">入库</el-button>
                      <el-button size="small" type="warning" :disabled="!canManagePharmacy" @click.stop="openStockDialog(row, 'out')">出库</el-button>
                    </div>
                    <div class="medicine-list__action-row">
                      <el-button size="small" type="primary" :disabled="!canManagePharmacy" @click.stop="openMedDialog(row)">编辑</el-button>
                      <el-popconfirm title="确定删除该药品? 删除后不可恢复" :disabled="!canManagePharmacy" @confirm="handleDelete(row.id)">
                        <template #reference>
                          <el-button size="small" type="danger" :disabled="!canManagePharmacy" @click.stop>删除</el-button>
                        </template>
                      </el-popconfirm>
                    </div>
                  </div>
                  <el-button class="medicine-list__preview-btn" size="small" type="info" :icon="View" @click.stop="openPreview(row)">预览详情</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-card>
    </div>

    <el-dialog
      v-model="previewVisible"
      title="药品详情预览"
      width="720px"
      align-center
      class="medicine-list__preview-dialog"
      destroy-on-close
    >
      <template v-if="previewMedicine">
        <div class="medicine-list__preview-scroll">
          <div class="medicine-list__preview-header">
            <div class="medicine-list__medicine-card">
              <div class="medicine-list__avatar">
                <el-icon><FirstAidKit /></el-icon>
              </div>
              <div class="medicine-list__medicine-info">
                <strong>{{ previewMedicine.name }}</strong>
                <span>{{ displayText(previewMedicine.spec) }} · {{ displayText(previewMedicine.manufacturer) }}</span>
              </div>
            </div>

            <div class="medicine-list__stock-overview">
              <div class="medicine-list__stock-number">
                <span>当前库存</span>
                <strong>{{ previewMedicine.stock }}</strong>
              </div>
              <StatusTag :type="stockStatusType(previewMedicine)" :label="stockStatusLabel(previewMedicine)" />
            </div>

            <el-progress
              :percentage="stockPercentage(previewMedicine)"
              :status="stockProgressStatus(previewMedicine)"
              :stroke-width="10"
              class="medicine-list__progress"
            />

            <el-descriptions :column="2" border class="medicine-list__preview-summary">
              <el-descriptions-item label="安全库存">{{ previewMedicine.safetyStock }}</el-descriptions-item>
              <el-descriptions-item label="库存缺口">{{ stockGap(previewMedicine) }}</el-descriptions-item>
              <el-descriptions-item label="零售价">{{ formatMoney(previewMedicine.price) }}</el-descriptions-item>
              <el-descriptions-item label="有效期">{{ displayText(previewMedicine.expiryDate) }}</el-descriptions-item>
              <el-descriptions-item label="批号">{{ displayText(previewMedicine.batchNo) }}</el-descriptions-item>
              <el-descriptions-item label="拼音码">{{ displayText(previewMedicine.pinyinCode) }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="medicine-list__preview-body">
            <section v-for="item in previewMedicineSections" :key="item.label" class="medicine-list__section">
              <span>{{ item.label }}</span>
              <p>{{ displayText(item.value) }}</p>
            </section>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 药品编辑弹窗 -->
    <el-dialog v-model="medDialogVisible" :title="medIsEdit ? '编辑药品' : '新增药品'" width="600px" destroy-on-close>
      <el-form ref="medFormRef" :model="medForm" :rules="medRules" label-width="90px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-form-item label="药品名称" prop="name"><el-input v-model="medForm.name" /></el-form-item>
        <el-form-item label="规格"><el-input v-model="medForm.spec" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="medForm.unit" class="medicine-list__unit-input" /></el-form-item>
        <el-divider content-position="left">库存与价格</el-divider>
        <el-form-item label="零售价"><el-input-number v-model="medForm.price" :precision="2" :min="0" /></el-form-item>
        <el-form-item label="安全库存"><el-input-number v-model="medForm.safetyStock" :min="0" /></el-form-item>
        <el-divider content-position="left">其他</el-divider>
        <el-form-item label="拼音码"><el-input v-model="medForm.pinyinCode" /></el-form-item>
        <el-form-item label="生产厂家"><el-input v-model="medForm.manufacturer" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="medDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="medSaveLoading" :disabled="!canManagePharmacy" @click="saveMed">保存</el-button>
      </template>
    </el-dialog>

    <!-- 出入库弹窗 -->
    <el-dialog v-model="stockDialogVisible" :title="stockType === 'in' ? '入库' : '出库'" width="450px" destroy-on-close>
      <el-form ref="stockFormRef" :model="stockForm" :rules="stockRules" label-width="90px">
        <el-form-item label="药品"><el-input :model-value="stockMedicine?.name" disabled /></el-form-item>
        <el-form-item label="当前库存"><el-input :model-value="stockMedicine?.stock" disabled /></el-form-item>
        <el-form-item label="数量" prop="quantity"><el-input-number v-model="stockForm.quantity" :min="1" /></el-form-item>
        <el-form-item v-if="stockType === 'in'" label="批号"><el-input v-model="stockForm.batchNo" /></el-form-item>
        <el-form-item v-if="stockType === 'in'" label="有效期">
          <el-date-picker v-model="stockForm.expiryDate" type="date" value-format="YYYY-MM-DD" class="medicine-list__form-control" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="stockForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="stockSaveLoading" :disabled="!canManagePharmacy" @click="doStock">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { FirstAidKit, View, Warning } from '@element-plus/icons-vue'
import { listMedicines, listLowStock, createMedicine, updateMedicine, deleteMedicine, stockIn, stockOut } from '../../api/medicine'
import type { Medicine, StockRequest } from '../../types'
import PageHeader from '../../components/PageHeader.vue'
import DataToolbar from '../../components/DataToolbar.vue'
import EmptyState from '../../components/EmptyState.vue'
import StatusTag from '../../components/StatusTag.vue'
import { usePermission } from '../../composables/usePermission'

const { canManagePharmacy } = usePermission()

const tableData = ref<Medicine[]>([])
const keyword = ref('')
const loading = ref(false)
const previewVisible = ref(false)
const previewMedicine = ref<Medicine | null>(null)
const medDialogVisible = ref(false)
const medIsEdit = ref(false)
const medFormRef = ref<FormInstance>()
const medSaveLoading = ref(false)
const medForm = reactive<Medicine>({ name: '', spec: '', unit: '盒', stock: 0, safetyStock: 10, price: 0, pinyinCode: '', manufacturer: '', status: 1 })
const medRules = { name: [{ required: true, message: '请输入药品名称', trigger: 'blur' }] }

const stockDialogVisible = ref(false)
const stockType = ref<'in'|'out'>('in')
const stockMedicine = ref<Medicine | null>(null)
const stockFormRef = ref<FormInstance>()
const stockSaveLoading = ref(false)
const stockForm = reactive<StockRequest>({ quantity: 1, batchNo: '', expiryDate: '', remark: '' })
const stockRules = { quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }] }

const lowStockCount = computed(() => tableData.value.filter(isLowStock).length)
const totalStockCount = computed(() => tableData.value.reduce((total, item) => total + (item.stock || 0), 0))
const previewMedicineSections = computed(() => {
  const medicine = previewMedicine.value
  if (!medicine) return []
  return [
    { label: '规格', value: medicine.spec },
    { label: '单位', value: medicine.unit },
    { label: '生产厂家', value: medicine.manufacturer },
  ]
})

async function loadData() {
  loading.value = true
  try {
    const r = await listMedicines(keyword.value)
    setTableData(r.data || [])
  } catch {} finally {
    loading.value = false
  }
}

async function showLowStock() {
  loading.value = true
  try {
    const r = await listLowStock()
    setTableData(r.data || [])
  } catch {} finally {
    loading.value = false
  }
}

function setTableData(data: Medicine[]) {
  tableData.value = data.sort((a, b) => (a.id ?? 0) - (b.id ?? 0))
}

function openMedDialog(row?: Medicine) {
  medIsEdit.value = !!row
  Object.assign(medForm, row ? { ...row } : { name: '', spec: '', unit: '盒', stock: 0, safetyStock: 10, price: 0, pinyinCode: '', manufacturer: '', status: 1 })
  medDialogVisible.value = true
}

function openPreview(row: Medicine) {
  previewMedicine.value = row
  previewVisible.value = true
}

async function saveMed() {
  const valid = await medFormRef.value?.validate().catch(() => false)
  if (!valid) return
  medSaveLoading.value = true
  try {
    medIsEdit.value && medForm.id ? await updateMedicine(medForm.id, medForm) : await createMedicine(medForm)
    ElMessage.success('保存成功')
    medDialogVisible.value = false
    loadData()
  } catch {} finally {
    medSaveLoading.value = false
  }
}

async function handleDelete(id?: number) {
  if (!id) return
  try {
    await deleteMedicine(id)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

function openStockDialog(row: Medicine, type: 'in'|'out') {
  stockType.value = type
  stockMedicine.value = row
  Object.assign(stockForm, { quantity: 1, batchNo: '', expiryDate: '', remark: '' })
  stockDialogVisible.value = true
}

async function doStock() {
  const valid = await stockFormRef.value?.validate().catch(() => false)
  if (!valid || !stockMedicine.value) return
  stockSaveLoading.value = true
  try {
    stockType.value === 'in' ? await stockIn(stockMedicine.value.id!, stockForm) : await stockOut(stockMedicine.value.id!, stockForm)
    ElMessage.success(stockType.value === 'in' ? '入库成功' : '出库成功')
    stockDialogVisible.value = false
    loadData()
  } catch {} finally {
    stockSaveLoading.value = false
  }
}

function isLowStock(medicine: Medicine) {
  return (medicine.stock || 0) <= (medicine.safetyStock || 0)
}

function stockGap(medicine: Medicine) {
  return Math.max((medicine.safetyStock || 0) - (medicine.stock || 0), 0)
}

function stockPercentage(medicine: Medicine) {
  if (!medicine.safetyStock) return 100
  return Math.min(Math.round((medicine.stock / medicine.safetyStock) * 100), 100)
}

function stockProgressStatus(medicine: Medicine) {
  return isLowStock(medicine) ? 'exception' : 'success'
}

function stockStatusType(medicine: Medicine) {
  return isLowStock(medicine) ? 'danger' : 'success'
}

function stockStatusLabel(medicine: Medicine) {
  return `${medicine.stock || 0}${medicine.unit || ''}`
}

function formatMoney(value?: number) {
  return `¥${(value || 0).toFixed(2)}`
}

function displayText(value?: string | number | null) {
  if (value === undefined || value === null || value === '') return '-'
  return String(value)
}

onMounted(loadData)
</script>

<style scoped>
.medicine-list {
  height: max(560px, calc(100vh - var(--header-height) - var(--content-padding) * 2));
  display: flex;
  flex-direction: column;
  min-width: 0;
  animation: fadeIn 0.4s ease-out;
}

.medicine-list__workspace {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.medicine-list__panel {
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-card);
  overflow: hidden;
}

.medicine-list__panel :deep(.el-card__header) {
  flex-shrink: 0;
}

.medicine-list__panel :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: var(--space-lg);
}

.medicine-list__panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
}

.medicine-list__panel-title {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  color: var(--text-primary);
  font-weight: 600;
}

.medicine-list__panel-meta {
  color: var(--text-muted);
  font-size: var(--font-size-sm);
  white-space: nowrap;
}

.medicine-list__stats {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}

.medicine-list__stat {
  min-width: 0;
  padding: var(--space-md);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-table);
  background: var(--bg-toolbar);
}

.medicine-list__stat-label {
  display: block;
  margin-bottom: var(--space-xs);
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}

.medicine-list__stat strong {
  color: var(--text-primary);
  font-size: var(--font-size-2xl);
  line-height: 1.2;
}

.medicine-list__table-wrap {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.medicine-list__table-wrap :deep(.el-table) {
  width: 100%;
}

.medicine-list__actions {
  display: inline-flex;
  align-items: stretch;
  justify-content: center;
  gap: var(--space-xs);
}

.medicine-list__action-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.medicine-list__action-row {
  display: flex;
  justify-content: center;
  gap: var(--space-xs);
}

.medicine-list__actions :deep(.el-button) {
  min-width: 52px;
}

.medicine-list__preview-btn {
  height: auto !important;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: 4px 8px;
  line-height: 1.2;
}

.medicine-list :deep(.medicine-list__tag-cell .cell) {
  overflow: visible;
  text-overflow: clip;
  white-space: nowrap;
}

.medicine-list__preview-dialog :deep(.el-dialog) {
  height: 520px;
  max-height: 80vh;
  margin-top: auto !important;
  margin-bottom: auto !important;
  display: flex;
  flex-direction: column;
}

.medicine-list__preview-dialog :deep(.el-dialog__body) {
  flex: 1;
  padding: 0;
  overflow: hidden;
}

.medicine-list__preview-scroll {
  height: 100%;
  padding: var(--space-lg);
  overflow-y: auto;
  box-sizing: border-box;
}

.medicine-list__preview-header {
  padding-bottom: var(--space-lg);
  border-bottom: 1px solid var(--border-light);
}

.medicine-list__preview-body {
  padding-top: var(--space-lg);
  display: grid;
  gap: var(--space-md);
}

.medicine-list__preview-summary {
  margin-top: var(--space-lg);
}

.medicine-list__medicine-card {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md);
  margin-bottom: var(--space-lg);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-table);
  background: var(--bg-toolbar);
}

.medicine-list__avatar {
  flex: 0 0 44px;
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-table);
  color: var(--color-primary);
  background: var(--color-primary-light);
  font-size: var(--font-size-xl);
}

.medicine-list__medicine-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.medicine-list__medicine-info strong {
  color: var(--text-primary);
  font-size: var(--font-size-lg);
  line-height: 1.3;
}

.medicine-list__medicine-info span {
  color: var(--text-muted);
  font-size: var(--font-size-sm);
  line-height: 1.4;
  word-break: break-word;
}

.medicine-list__stock-overview {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-md);
}

.medicine-list__stock-number span {
  display: block;
  margin-bottom: var(--space-xs);
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}

.medicine-list__stock-number strong {
  color: var(--text-primary);
  font-size: var(--font-size-4xl);
  line-height: 1;
}

.medicine-list__progress {
  flex-shrink: 0;
  margin-bottom: var(--space-lg);
}

.medicine-list__identity {
  flex-shrink: 0;
  margin-bottom: var(--space-lg);
}

.medicine-list__section {
  min-width: 0;
  padding: var(--space-md);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-table);
  background: var(--bg-card);
}

.medicine-list__section span {
  display: block;
  margin-bottom: var(--space-xs);
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}

.medicine-list__section p {
  margin: 0;
  color: var(--text-primary);
  font-size: var(--font-size-sm);
  line-height: 1.7;
  word-break: break-word;
}

.medicine-list__unit-input {
  width: 120px;
}

.medicine-list :deep(.medicine-list__form-control) {
  width: 100%;
}

@media (max-width: 1180px) {
  .medicine-list {
    height: auto;
    min-height: max(560px, calc(100vh - var(--header-height) - var(--content-padding) * 2));
  }

  .medicine-list__workspace {
    height: auto;
  }

  .medicine-list__panel {
    min-height: 620px;
  }

  .medicine-list__table-wrap {
    flex: none;
    height: clamp(360px, 48vh, 560px);
    min-height: 0;
  }

  .medicine-list__preview-dialog :deep(.el-dialog) {
    height: 480px;
  }
}

@media (max-width: 768px) {
  .medicine-list__stats {
    grid-template-columns: 1fr;
  }

  .medicine-list__table-wrap {
    height: 320px;
  }

  .medicine-list__panel :deep(.el-card__body) {
    padding: var(--space-md);
  }
}
</style>
