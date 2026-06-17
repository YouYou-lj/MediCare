<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>药品库存</span>
          <div>
            <el-input v-model="keyword" placeholder="搜索药品名称/拼音码" style="width:220px;margin-right:12px" clearable @keyup.enter="loadData">
              <template #append><el-button @click="loadData"><el-icon><Search /></el-icon></el-button></template>
            </el-input>
            <el-button type="warning" @click="showLowStock">库存预警</el-button>
            <el-button type="primary" @click="openMedDialog()"><el-icon><Plus /></el-icon> 新增药品</el-button>
          </div>
        </div>
      </template>
      <el-table :data="tableData" stripe border>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="药品名称" width="150" />
        <el-table-column prop="spec" label="规格" width="120" />
        <el-table-column prop="unit" label="单位" width="60" />
        <el-table-column prop="stock" label="库存" width="80">
          <template #default="{ row }"><span :style="{color: row.stock<=row.safetyStock?'#f56c6c':''}">{{ row.stock }}</span></template>
        </el-table-column>
        <el-table-column prop="safetyStock" label="安全库存" width="90" />
        <el-table-column prop="price" label="零售价" width="80">
          <template #default="{ row }">¥{{ (row.price || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="expiryDate" label="有效期" width="110" />
        <el-table-column prop="manufacturer" label="厂家" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button text type="success" @click="openStockDialog(row, 'in')">入库</el-button>
            <el-button text type="warning" @click="openStockDialog(row, 'out')">出库</el-button>
            <el-button text type="primary" @click="openMedDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除?" @confirm="handleDelete(row.id)"><template #reference><el-button text type="danger">删除</el-button></template></el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 药品编辑弹窗 -->
    <el-dialog v-model="medDialogVisible" :title="medIsEdit ? '编辑药品' : '新增药品'" width="600px" destroy-on-close>
      <el-form ref="medFormRef" :model="medForm" :rules="medRules" label-width="90px">
        <el-form-item label="药品名称" prop="name"><el-input v-model="medForm.name" /></el-form-item>
        <el-form-item label="规格"><el-input v-model="medForm.spec" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="medForm.unit" style="width:120px" /></el-form-item>
        <el-form-item label="零售价"><el-input-number v-model="medForm.price" :precision="2" :min="0" /></el-form-item>
        <el-form-item label="安全库存"><el-input-number v-model="medForm.safetyStock" :min="0" /></el-form-item>
        <el-form-item label="拼音码"><el-input v-model="medForm.pinyinCode" /></el-form-item>
        <el-form-item label="生产厂家"><el-input v-model="medForm.manufacturer" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="medDialogVisible=false">取消</el-button><el-button type="primary" @click="saveMed">保存</el-button></template>
    </el-dialog>

    <!-- 出入库弹窗 -->
    <el-dialog v-model="stockDialogVisible" :title="stockType==='in'?'入库':'出库'" width="450px" destroy-on-close>
      <el-form ref="stockFormRef" :model="stockForm" :rules="stockRules" label-width="90px">
        <el-form-item label="药品"><el-input :model-value="stockMedicine?.name" disabled /></el-form-item>
        <el-form-item label="当前库存"><el-input :model-value="stockMedicine?.stock" disabled /></el-form-item>
        <el-form-item label="数量" prop="quantity"><el-input-number v-model="stockForm.quantity" :min="1" /></el-form-item>
        <el-form-item v-if="stockType==='in'" label="批号"><el-input v-model="stockForm.batchNo" /></el-form-item>
        <el-form-item v-if="stockType==='in'" label="有效期"><el-date-picker v-model="stockForm.expiryDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="stockForm.remark" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="stockDialogVisible=false">取消</el-button><el-button type="primary" @click="doStock">确认</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { listMedicines, listLowStock, createMedicine, updateMedicine, deleteMedicine, stockIn, stockOut } from '../../api/medicine'
import type { Medicine, StockRequest } from '../../types'

const tableData = ref<Medicine[]>([])
const keyword = ref('')
const medDialogVisible = ref(false)
const medIsEdit = ref(false)
const medFormRef = ref<FormInstance>()
const medForm = reactive<Medicine>({ name: '', spec: '', unit: '盒', stock: 0, safetyStock: 10, price: 0, pinyinCode: '', manufacturer: '', status: 1 })
const medRules = { name: [{ required: true, message: '请输入药品名称', trigger: 'blur' }] }

const stockDialogVisible = ref(false)
const stockType = ref<'in'|'out'>('in')
const stockMedicine = ref<Medicine | null>(null)
const stockFormRef = ref<FormInstance>()
const stockForm = reactive<StockRequest>({ quantity: 1, batchNo: '', expiryDate: '', remark: '' })
const stockRules = { quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }] }

async function loadData() { try { const r = await listMedicines(keyword.value); tableData.value = r.data } catch {} }
async function showLowStock() { try { const r = await listLowStock(); tableData.value = r.data } catch {} }

function openMedDialog(row?: Medicine) {
  medIsEdit.value = !!row
  Object.assign(medForm, row ? { ...row } : { name: '', spec: '', unit: '盒', stock: 0, safetyStock: 10, price: 0, pinyinCode: '', manufacturer: '', status: 1 })
  medDialogVisible.value = true
}
async function saveMed() {
  const v = await medFormRef.value?.validate().catch(() => false); if (!v) return
  try { medIsEdit.value && medForm.id ? await updateMedicine(medForm.id, medForm) : await createMedicine(medForm); ElMessage.success('保存成功'); medDialogVisible.value = false; loadData() } catch {}
}
async function handleDelete(id: number) { try { await deleteMedicine(id); ElMessage.success('删除成功'); loadData() } catch {} }

function openStockDialog(row: Medicine, type: 'in'|'out') {
  stockType.value = type; stockMedicine.value = row
  Object.assign(stockForm, { quantity: 1, batchNo: '', expiryDate: '', remark: '' })
  stockDialogVisible.value = true
}
async function doStock() {
  if (!stockMedicine.value) return
  try {
    stockType.value === 'in' ? await stockIn(stockMedicine.value.id!, stockForm) : await stockOut(stockMedicine.value.id!, stockForm)
    ElMessage.success(stockType.value === 'in' ? '入库成功' : '出库成功')
    stockDialogVisible.value = false; loadData()
  } catch {}
}

onMounted(loadData)
</script>