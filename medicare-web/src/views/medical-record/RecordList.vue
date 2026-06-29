<template>
  <div class="record-list">
    <PageHeader title="病历管理" subtitle="查看、编辑与删除已保存的病历记录" />

    <div class="record-list__workspace">
      <el-card shadow="hover" class="record-list__panel">
        <template #header>
          <div class="record-list__panel-header">
            <div class="record-list__panel-title">
              <el-icon><Document /></el-icon>
              <span>病历列表</span>
            </div>
            <span class="record-list__panel-meta">共 {{ totalRecordCount }} 份病历</span>
          </div>
        </template>

        <div class="record-list__stats">
          <div class="record-list__stat">
            <span class="record-list__stat-label">已诊断</span>
            <strong>{{ diagnosedRecordCount }}</strong>
          </div>
          <div class="record-list__stat">
            <span class="record-list__stat-label">接诊医生</span>
            <strong>{{ doctorCount }}</strong>
          </div>
          <div class="record-list__stat">
            <span class="record-list__stat-label">当前结果</span>
            <strong>{{ filteredList.length }}</strong>
          </div>
        </div>

        <DataToolbar
          v-model:searchModelValue="keyword"
          search-placeholder="搜索患者、医生、主诉或诊断"
          show-refresh
          @search="handleSearch"
          @refresh="loadData"
        />

        <div class="record-list__table-wrap">
          <el-table
            v-loading="loading"
            :data="filteredList"
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
                icon="Document"
                title="暂无病历数据"
                description="医生在接诊并保存病历后会显示在此"
              />
            </template>
            <el-table-column type="index" label="序号" width="60" align="center" :resizable="false" />
            <el-table-column prop="code" label="ID" width="120" align="center" :resizable="false" />
            <el-table-column prop="patientName" label="患者" min-width="120" :resizable="false" align="center" />
            <el-table-column prop="doctorName" label="医生" min-width="120" :resizable="false" align="center" />
            <el-table-column prop="chiefComplaint" label="主诉" min-width="220" show-overflow-tooltip :resizable="false" />
            <el-table-column prop="diagnosis" label="诊断" min-width="180" show-overflow-tooltip :resizable="false" />
            <el-table-column label="创建时间" min-width="190" :resizable="false">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="更新时间" min-width="190" :resizable="false">
              <template #default="{ row }">{{ formatTime(row.updateTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="220" align="center" :resizable="false">
              <template #default="{ row }">
                <div class="record-list__actions">
                  <el-button size="small" type="primary" :icon="View" @click.stop="openPreview(row)">预览</el-button>
                  <el-button size="small" type="warning" :icon="EditPen" :disabled="!canManageMedicalRecords" @click.stop="openEditDialog(row)">编辑</el-button>
                  <el-popconfirm title="确定删除该病历? 删除后不可恢复" :disabled="!canManageMedicalRecords" @confirm="handleDelete(row.id)">
                    <template #reference><el-button size="small" type="danger" :icon="Delete" :disabled="!canManageMedicalRecords" @click.stop>删除</el-button></template>
                  </el-popconfirm>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-card>
    </div>

    <el-dialog
      v-model="previewVisible"
      title="病历预览"
      width="720px"
      align-center
      class="record-list__preview-dialog"
      destroy-on-close
    >
      <template v-if="previewRecord">
        <div class="record-list__preview-header">
          <div class="record-list__patient">
            <div class="record-list__avatar">
              <el-icon><User /></el-icon>
            </div>
            <div class="record-list__patient-info">
              <strong>{{ displayText(previewRecord.patientName) }}</strong>
              <span>{{ displayText(previewRecord.doctorName) }} · {{ formatTime(previewRecord.createTime) }}</span>
            </div>
          </div>

          <el-descriptions :column="2" border class="record-list__preview-summary">
            <el-descriptions-item label="挂号ID">{{ previewRecord.registrationId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="患者ID">{{ previewRecord.patientId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="医生ID">{{ previewRecord.doctorId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatTime(previewRecord.updateTime) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="record-list__preview-body">
          <section v-for="item in previewRecordSections" :key="item.label" class="record-list__section">
            <span>{{ item.label }}</span>
            <p>{{ displayText(item.value) }}</p>
          </section>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑病历弹窗 -->
    <el-dialog
      v-model="editVisible"
      title="编辑病历"
      width="720px"
      align-center
      class="record-list__edit-dialog"
      destroy-on-close
    >
      <template v-if="editRecord">
        <div class="record-list__edit-strip">
          <span>患者: {{ displayText(editRecord.patientName) }}</span>
          <span>医生: {{ displayText(editRecord.doctorName) }}</span>
          <span>挂号ID: {{ editRecord.registrationId || '-' }}</span>
        </div>
        <el-form ref="editFormRef" :model="editForm" label-width="90px" class="record-list__edit-form">
          <el-form-item label="主诉"><el-input v-model="editForm.chiefComplaint" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="现病史"><el-input v-model="editForm.presentIllness" type="textarea" :rows="3" /></el-form-item>
          <el-form-item label="既往史"><el-input v-model="editForm.pastHistory" /></el-form-item>
          <el-form-item label="体格检查"><el-input v-model="editForm.physicalExam" /></el-form-item>
          <el-form-item label="诊断"><el-input v-model="editForm.diagnosis" /></el-form-item>
          <el-form-item label="医嘱"><el-input v-model="editForm.advice" type="textarea" :rows="2" /></el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" :disabled="!canManageMedicalRecords" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Delete, Document, EditPen, User, View } from '@element-plus/icons-vue'
import { listMedicalRecords, getMedicalRecord, updateMedicalRecord, deleteMedicalRecord } from '../../api/medical-record'
import type { MedicalRecord } from '../../types'
import { usePermission } from '../../composables/usePermission'
import PageHeader from '../../components/PageHeader.vue'
import DataToolbar from '../../components/DataToolbar.vue'
import EmptyState from '../../components/EmptyState.vue'

const { canManageMedicalRecords } = usePermission()

const recordList = ref<MedicalRecord[]>([])
const keyword = ref('')
const previewVisible = ref(false)
const previewRecord = ref<MedicalRecord | null>(null)
const loading = ref(false)
const editVisible = ref(false)
const editLoading = ref(false)
const editFormRef = ref<FormInstance>()
const editRecord = ref<MedicalRecord | null>(null)
const editForm = reactive<Partial<MedicalRecord>>({
  chiefComplaint: '',
  presentIllness: '',
  pastHistory: '',
  physicalExam: '',
  diagnosis: '',
  advice: '',
})

const filteredList = computed(() => {
  if (!keyword.value) return recordList.value
  const k = keyword.value.toLowerCase()
  return recordList.value.filter((record) => {
    return [
      record.patientName,
      record.doctorName,
      record.chiefComplaint,
      record.diagnosis,
    ].some(value => value?.toLowerCase().includes(k))
  })
})
const totalRecordCount = computed(() => recordList.value.length)
const diagnosedRecordCount = computed(() => recordList.value.filter(record => Boolean(record.diagnosis?.trim())).length)
const doctorCount = computed(() => new Set(recordList.value.map(record => record.doctorName).filter(Boolean)).size)
const previewRecordSections = computed(() => buildRecordSections(previewRecord.value))

async function loadData() {
  loading.value = true
  try {
    const r = await listMedicalRecords()
    recordList.value = (r.data || []).sort((a, b) => (a.id ?? 0) - (b.id ?? 0))
  } catch {} finally {
    loading.value = false
  }
}

function handleSearch() {}

function openPreview(row: MedicalRecord) {
  previewRecord.value = row
  previewVisible.value = true
}

async function openEditDialog(row: MedicalRecord) {
  if (!canManageMedicalRecords.value) {
    ElMessage.warning('无权编辑病历')
    return
  }
  try {
    const res = await getMedicalRecord(row.id!)
    editRecord.value = res.data
    Object.assign(editForm, {
      chiefComplaint: res.data.chiefComplaint || '',
      presentIllness: res.data.presentIllness || '',
      pastHistory: res.data.pastHistory || '',
      physicalExam: res.data.physicalExam || '',
      diagnosis: res.data.diagnosis || '',
      advice: res.data.advice || '',
    })
    editVisible.value = true
  } catch {
    ElMessage.error('加载病历失败')
  }
}

async function handleSave() {
  if (!editRecord.value) return
  editLoading.value = true
  try {
    await updateMedicalRecord(editRecord.value.id!, { ...editForm } as MedicalRecord)
    ElMessage.success('病历更新成功')
    editVisible.value = false
    loadData()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    editLoading.value = false
  }
}

async function handleDelete(id?: number) {
  if (!id) return
  try {
    await deleteMedicalRecord(id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    ElMessage.error('删除失败')
  }
}

function displayText(value?: string | number | null) {
  if (value === undefined || value === null || value === '') return '-'
  return String(value)
}

function formatTime(value?: string) {
  if (!value) return '-'
  const time = dayjs(value)
  return time.isValid() ? time.format('YYYY-MM-DD HH:mm') : value
}

function buildRecordSections(record: MedicalRecord | null) {
  if (!record) return []
  return [
    { label: '主诉', value: record.chiefComplaint },
    { label: '现病史', value: record.presentIllness },
    { label: '既往史', value: record.pastHistory },
    { label: '体格检查', value: record.physicalExam },
    { label: '诊断', value: record.diagnosis },
    { label: '医嘱', value: record.advice },
  ]
}

onMounted(loadData)
</script>

<style scoped>
.record-list {
  height: max(560px, calc(100vh - var(--header-height) - var(--content-padding) * 2));
  display: flex;
  flex-direction: column;
  min-width: 0;
  animation: fadeIn 0.4s ease-out;
}

.record-list__workspace {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.record-list__panel {
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-card);
  overflow: hidden;
}

.record-list__panel :deep(.el-card__header) {
  flex-shrink: 0;
}

.record-list__panel :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: var(--space-lg);
}

.record-list__panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
}

.record-list__panel-title {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  color: var(--text-primary);
  font-weight: 600;
}

.record-list__panel-meta {
  color: var(--text-muted);
  font-size: var(--font-size-sm);
  white-space: nowrap;
}

.record-list__stats {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}

.record-list__stat {
  min-width: 0;
  padding: var(--space-md);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-table);
  background: var(--bg-toolbar);
}

.record-list__stat-label {
  display: block;
  margin-bottom: var(--space-xs);
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}

.record-list__stat strong {
  color: var(--text-primary);
  font-size: var(--font-size-2xl);
  line-height: 1.2;
}

.record-list__table-wrap {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.record-list__table-wrap :deep(.el-table) {
  width: 100%;
}

.record-list__actions {
  display: flex;
  flex-wrap: nowrap;
  justify-content: center;
  align-items: center;
  gap: 0;
  white-space: nowrap;
}

.record-list__preview-dialog :deep(.el-dialog__body) {
  padding: 0;
  height: 70vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.record-list__edit-dialog :deep(.el-dialog__body) {
  padding: var(--space-lg);
  max-height: 70vh;
  overflow-y: auto;
}

.record-list__edit-strip {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
  padding: var(--space-md);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-table);
  background: var(--bg-toolbar);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.record-list__edit-form :deep(.el-textarea__inner) {
  resize: none;
}

.record-list__preview-header {
  flex-shrink: 0;
  padding: var(--space-lg);
  border-bottom: 1px solid var(--border-light);
}

.record-list__preview-body {
  flex: 1;
  min-height: 0;
  padding: var(--space-lg);
  overflow-y: auto;
  display: grid;
  gap: var(--space-md);
}

.record-list__preview-summary {
  margin-top: var(--space-lg);
}

.record-list__patient {
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

.record-list__avatar {
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

.record-list__patient-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.record-list__patient-info strong {
  color: var(--text-primary);
  font-size: var(--font-size-lg);
  line-height: 1.3;
}

.record-list__patient-info span {
  color: var(--text-muted);
  font-size: var(--font-size-sm);
  line-height: 1.4;
  word-break: break-word;
}

.record-list__section {
  min-width: 0;
  padding: var(--space-md);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-table);
  background: var(--bg-card);
}

.record-list__section span {
  display: block;
  margin-bottom: var(--space-xs);
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}

.record-list__section p {
  margin: 0;
  color: var(--text-primary);
  font-size: var(--font-size-sm);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1180px) {
  .record-list {
    height: auto;
    min-height: max(560px, calc(100vh - var(--header-height) - var(--content-padding) * 2));
  }

  .record-list__workspace {
    height: auto;
  }

  .record-list__panel {
    min-height: 620px;
  }

  .record-list__table-wrap {
    flex: none;
    height: clamp(360px, 48vh, 560px);
    min-height: 0;
  }

  .record-list__preview-dialog :deep(.el-dialog__body) {
    height: 80vh;
  }
}

@media (max-width: 768px) {
  .record-list__stats {
    grid-template-columns: 1fr;
  }

  .record-list__table-wrap {
    height: 320px;
  }

  .record-list__panel :deep(.el-card__body) {
    padding: var(--space-md);
  }

  .record-list__preview-dialog :deep(.el-dialog) {
    width: calc(100vw - var(--space-xl)) !important;
  }
}
</style>
