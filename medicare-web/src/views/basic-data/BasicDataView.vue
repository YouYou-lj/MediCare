<template>
  <div class="basic-data">
    <PageHeader title="基础数据管理" subtitle="科室、医生与排班信息维护" />

    <el-card shadow="hover" class="basic-data__card">
      <el-tabs v-model="activeTab" type="border-card" class="basic-data__tabs">
        <el-tab-pane label="科室管理" name="dept">
          <div class="basic-data__pane">
            <DataToolbar show-refresh show-add add-label="新增科室" @refresh="loadDepts" @add="openDeptDialog()">
              <template #filters>
                <el-input v-model="deptKeyword" placeholder="搜索科室名称" clearable class="basic-data__filter-input" @input="filterDepts" />
              </template>
            </DataToolbar>
            <div class="basic-data__table-wrap">
              <el-table v-loading="deptLoading" :data="filteredDeptList" stripe border height="100%" :default-sort="{ prop: 'code', order: 'ascending' }">
                <template #empty>
                  <EmptyState icon="OfficeBuilding" title="暂无科室数据" description="点击右上角“新增科室”按钮添加" />
                </template>
                <el-table-column type="index" label="序号" width="60" align="center" fixed="left" />
                <el-table-column prop="code" label="ID" width="120" align="center" fixed="left" />
                <el-table-column prop="name" label="科室名称" min-width="150" fixed="left" />
                <el-table-column prop="location" label="位置" min-width="180" />
                <el-table-column prop="phone" label="电话" min-width="140" align="center" />
                <el-table-column label="操作" width="160" align="center" fixed="right">
                  <template #default="{ row }">
                    <div class="basic-data__actions">
                      <el-button size="small" type="primary" @click="openDeptDialog(row)">编辑</el-button>
                      <el-popconfirm title="确定删除该科室? 关联医生和排班将失效" @confirm="handleDeleteDept(row.id)">
                        <template #reference><el-button size="small" type="danger">删除</el-button></template>
                      </el-popconfirm>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="医生管理" name="doctor">
          <div class="basic-data__pane">
            <DataToolbar show-refresh show-add add-label="新增医生" @refresh="loadDoctors" @add="openDoctorDialog()">
              <template #filters>
                <el-select v-model="deptFilter" placeholder="筛选科室" clearable class="basic-data__filter-select" @change="loadDoctors">
                  <el-option v-for="d in deptList" :key="d.id" :label="d.name" :value="d.id" />
                </el-select>
              </template>
            </DataToolbar>
            <div class="basic-data__table-wrap">
              <el-table v-loading="doctorLoading" :data="doctorList" stripe border height="100%" :default-sort="{ prop: 'code', order: 'ascending' }">
                <template #empty>
                  <EmptyState icon="FirstAidKit" title="暂无医生数据" description="点击右上角“新增医生”按钮添加" />
                </template>
                <el-table-column type="index" label="序号" width="60" align="center" fixed="left" />
                <el-table-column prop="code" label="ID" width="120" align="center" fixed="left" />
                <el-table-column prop="name" label="姓名" min-width="120" fixed="left" align="center" />
                <el-table-column prop="departmentName" label="科室" min-width="130" align="center" />
                <el-table-column prop="title" label="职称" min-width="140" align="center" />
                <el-table-column prop="status" label="状态" min-width="110" align="center">
                  <template #default="{ row }">
                    <StatusTag :type="row.status === 1 ? 'success' : 'danger'" :label="row.status === 1 ? '在职' : '停用'" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="160" align="center" fixed="right">
                  <template #default="{ row }">
                    <div class="basic-data__actions">
                      <el-button size="small" type="primary" @click="openDoctorDialog(row)">编辑</el-button>
                      <el-popconfirm title="确定删除该医生? 关联排班将失效" @confirm="handleDeleteDoctor(row.id)">
                        <template #reference><el-button size="small" type="danger">删除</el-button></template>
                      </el-popconfirm>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="排班管理" name="schedule">
          <div class="basic-data__pane">
            <DataToolbar show-refresh show-add add-label="新增排班" @refresh="loadSchedules" @add="openSchedDialog()">
              <template #filters>
                <el-date-picker v-model="schedDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" class="basic-data__filter-date" @change="loadSchedules" />
                <el-select v-model="schedDeptFilter" placeholder="筛选科室" clearable class="basic-data__filter-select" @change="loadSchedules">
                  <el-option v-for="d in deptList" :key="d.id" :label="d.name" :value="d.id" />
                </el-select>
              </template>
            </DataToolbar>
            <div class="basic-data__table-wrap">
              <el-table v-loading="schedLoading" :data="schedList" stripe border height="100%" :default-sort="{ prop: 'code', order: 'ascending' }">
                <template #empty>
                  <EmptyState icon="Calendar" title="暂无排班数据" description="点击右上角“新增排班”按钮添加" />
                </template>
                <el-table-column type="index" label="序号" width="60" align="center" fixed="left" />
                <el-table-column prop="code" label="ID" width="120" align="center" fixed="left" />
                <el-table-column prop="doctorName" label="医生" min-width="120" fixed="left" align="center" />
                <el-table-column prop="departmentName" label="科室" min-width="130" align="center" />
                <el-table-column prop="workDate" label="日期" min-width="130" align="center" />
                <el-table-column prop="timeSlot" label="时段" min-width="100" align="center" />
                <el-table-column prop="totalSlots" label="总号源" min-width="100" align="center" />
                <el-table-column prop="remainSlots" label="剩余号源" min-width="110" align="center">
                  <template #default="{ row }">
                    <StatusTag :type="row.remainSlots > 0 ? 'success' : 'danger'" :label="String(row.remainSlots)" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="160" align="center" fixed="right">
                  <template #default="{ row }">
                    <div class="basic-data__actions">
                      <el-button size="small" type="primary" @click="openSchedDialog(row)">编辑</el-button>
                      <el-popconfirm title="确定删除该排班?" @confirm="handleDeleteSched(row.id)">
                        <template #reference><el-button size="small" type="danger">删除</el-button></template>
                      </el-popconfirm>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <!-- 科室弹窗 -->
      <el-dialog v-model="deptDialogVisible" :title="deptIsEdit ? '编辑科室' : '新增科室'" width="450px" destroy-on-close>
        <el-form ref="deptFormRef" :model="deptForm" :rules="deptRules" label-width="80px">
          <el-form-item label="名称" prop="name"><el-input v-model="deptForm.name" /></el-form-item>
          <el-form-item label="位置"><el-input v-model="deptForm.location" /></el-form-item>
          <el-form-item label="电话"><el-input v-model="deptForm.phone" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="deptDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="deptSaveLoading" @click="saveDept">保存</el-button>
        </template>
      </el-dialog>

      <!-- 医生弹窗 -->
      <el-dialog v-model="doctorDialogVisible" :title="doctorIsEdit ? '编辑医生' : '新增医生'" width="500px" destroy-on-close>
        <el-form ref="doctorFormRef" :model="doctorForm" :rules="doctorRules" label-width="80px">
          <el-form-item label="姓名" prop="name"><el-input v-model="doctorForm.name" /></el-form-item>
          <el-form-item label="科室" prop="departmentId">
            <el-select v-model="doctorForm.departmentId" class="basic-data__form-control"><el-option v-for="d in deptList" :key="d.id" :label="d.name" :value="d.id" /></el-select>
          </el-form-item>
          <el-form-item label="职称"><el-input v-model="doctorForm.title" /></el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="doctorForm.status"><el-radio :value="1">在职</el-radio><el-radio :value="0">停用</el-radio></el-radio-group>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="doctorDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="doctorSaveLoading" @click="saveDoctor">保存</el-button>
        </template>
      </el-dialog>

      <!-- 排班弹窗 -->
      <el-dialog v-model="schedDialogVisible" :title="schedIsEdit ? '编辑排班' : '新增排班'" width="500px" destroy-on-close>
        <el-form ref="schedFormRef" :model="schedForm" :rules="schedRules" label-width="80px">
          <el-form-item label="医生" prop="doctorId">
            <el-select v-model="schedForm.doctorId" class="basic-data__form-control"><el-option v-for="d in doctorList" :key="d.id" :label="`${d.name} (${d.departmentName})`" :value="d.id" /></el-select>
          </el-form-item>
          <el-form-item label="日期" prop="workDate"><el-date-picker v-model="schedForm.workDate" type="date" value-format="YYYY-MM-DD" class="basic-data__form-control" /></el-form-item>
          <el-form-item label="时段" prop="timeSlot">
            <el-select v-model="schedForm.timeSlot" class="basic-data__form-control"><el-option label="上午" value="上午" /><el-option label="下午" value="下午" /><el-option label="晚上" value="晚上" /></el-select>
          </el-form-item>
          <el-form-item label="总号源" prop="totalSlots"><el-input-number v-model="schedForm.totalSlots" :min="1" /></el-form-item>
          <el-form-item label="剩余号源" prop="remainSlots"><el-input-number v-model="schedForm.remainSlots" :min="0" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="schedDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="schedSaveLoading" @click="saveSched">保存</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { listDepartments, createDepartment, updateDepartment, deleteDepartment } from '../../api/department'
import { listDoctors, createDoctor, updateDoctor, deleteDoctor } from '../../api/doctor'
import { listSchedules, createSchedule, updateSchedule, deleteSchedule } from '../../api/schedule'
import type { Department, Doctor, Schedule } from '../../types'
import PageHeader from '../../components/PageHeader.vue'
import DataToolbar from '../../components/DataToolbar.vue'
import EmptyState from '../../components/EmptyState.vue'
import StatusTag from '../../components/StatusTag.vue'

const activeTab = ref('dept')

// ===== 科室 =====
const deptList = ref<Department[]>([])
const deptKeyword = ref('')
const deptLoading = ref(false)
const deptDialogVisible = ref(false)
const deptIsEdit = ref(false)
const deptFormRef = ref<FormInstance>()
const deptSaveLoading = ref(false)
const deptForm = reactive<Department>({ name: '', location: '', phone: '' })
const deptRules = { name: [{ required: true, message: '请输入科室名称', trigger: 'blur' }] }

const filteredDeptList = computed(() => {
  if (!deptKeyword.value) return deptList.value
  const k = deptKeyword.value.toLowerCase()
  return deptList.value.filter(d => d.name.toLowerCase().includes(k))
})

function filterDepts() {}

async function loadDepts() {
  deptLoading.value = true
  try { const r = await listDepartments(); deptList.value = (r.data || []).sort((a, b) => (a.id ?? 0) - (b.id ?? 0)) } catch {}
  deptLoading.value = false
}
function openDeptDialog(row?: Department) { deptIsEdit.value = !!row; Object.assign(deptForm, row ? { ...row } : { name: '', location: '', phone: '' }); deptDialogVisible.value = true }
async function saveDept() {
  const v = await deptFormRef.value?.validate().catch(() => false); if (!v) return
  deptSaveLoading.value = true
  try { deptIsEdit.value && deptForm.id ? await updateDepartment(deptForm.id, deptForm) : await createDepartment(deptForm); ElMessage.success('保存成功'); deptDialogVisible.value = false; loadDepts() } catch {}
  deptSaveLoading.value = false
}
async function handleDeleteDept(id: number) { try { await deleteDepartment(id); ElMessage.success('删除成功'); loadDepts() } catch {} }

// ===== 医生 =====
const doctorList = ref<Doctor[]>([])
const deptFilter = ref<number>()
const doctorLoading = ref(false)
const doctorDialogVisible = ref(false)
const doctorIsEdit = ref(false)
const doctorFormRef = ref<FormInstance>()
const doctorSaveLoading = ref(false)
const doctorForm = reactive<Doctor>({ name: '', departmentId: 1, title: '', status: 1 })
const doctorRules = { name: [{ required: true, message: '请输入姓名', trigger: 'blur' }], departmentId: [{ required: true, message: '请选择科室', trigger: 'change' }] }

async function loadDoctors() {
  doctorLoading.value = true
  try { const r = await listDoctors(deptFilter.value); doctorList.value = (r.data || []).sort((a, b) => (a.id ?? 0) - (b.id ?? 0)) } catch {}
  doctorLoading.value = false
}
function openDoctorDialog(row?: Doctor) { doctorIsEdit.value = !!row; Object.assign(doctorForm, row ? { ...row } : { name: '', departmentId: 1, title: '', status: 1 }); doctorDialogVisible.value = true }
async function saveDoctor() {
  const v = await doctorFormRef.value?.validate().catch(() => false); if (!v) return
  doctorSaveLoading.value = true
  try { doctorIsEdit.value && doctorForm.id ? await updateDoctor(doctorForm.id, doctorForm) : await createDoctor(doctorForm); ElMessage.success('保存成功'); doctorDialogVisible.value = false; loadDoctors() } catch {}
  doctorSaveLoading.value = false
}
async function handleDeleteDoctor(id: number) { try { await deleteDoctor(id); ElMessage.success('删除成功'); loadDoctors() } catch {} }

// ===== 排班 =====
const schedList = ref<Schedule[]>([])
const schedDate = ref(new Date().toISOString().slice(0, 10))
const schedDeptFilter = ref<number>()
const schedLoading = ref(false)
const schedDialogVisible = ref(false)
const schedIsEdit = ref(false)
const schedFormRef = ref<FormInstance>()
const schedSaveLoading = ref(false)
const schedForm = reactive<Schedule>({ doctorId: 1, workDate: '', timeSlot: '上午', totalSlots: 20, remainSlots: 20 })
const schedRules = { doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }], workDate: [{ required: true, message: '请选择日期', trigger: 'change' }], timeSlot: [{ required: true, message: '请选择时段', trigger: 'change' }] }

async function loadSchedules() {
  schedLoading.value = true
  try { const r = await listSchedules(schedDate.value, schedDeptFilter.value); schedList.value = (r.data || []).sort((a, b) => (a.id ?? 0) - (b.id ?? 0)) } catch {}
  schedLoading.value = false
}
function openSchedDialog(row?: Schedule) { schedIsEdit.value = !!row; Object.assign(schedForm, row ? { ...row } : { doctorId: 1, workDate: '', timeSlot: '上午', totalSlots: 20, remainSlots: 20 }); schedDialogVisible.value = true }
async function saveSched() {
  const v = await schedFormRef.value?.validate().catch(() => false); if (!v) return
  schedSaveLoading.value = true
  try { schedIsEdit.value && schedForm.id ? await updateSchedule(schedForm.id, schedForm) : await createSchedule(schedForm); ElMessage.success('保存成功'); schedDialogVisible.value = false; loadSchedules() } catch {}
  schedSaveLoading.value = false
}
async function handleDeleteSched(id: number) { try { await deleteSchedule(id); ElMessage.success('删除成功'); loadSchedules() } catch {} }

onMounted(async () => { await loadDepts(); await loadDoctors(); await loadSchedules() })
</script>

<style scoped>
.basic-data {
  height: max(560px, calc(100vh - var(--header-height) - var(--content-padding) * 2));
  display: flex;
  flex-direction: column;
  min-width: 0;
  animation: fadeIn 0.4s ease-out;
}

.basic-data__card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-card);
  overflow: hidden;
}

.basic-data__card :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 0;
}

.basic-data__tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: none;
  box-shadow: none;
}

.basic-data__tabs :deep(.el-tabs__header) {
  flex-shrink: 0;
}

.basic-data__tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  padding: var(--space-lg);
  overflow: hidden;
}

.basic-data__tabs :deep(.el-tab-pane) {
  height: 100%;
  min-height: 0;
}

.basic-data__pane {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.basic-data__table-wrap {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.basic-data__table-wrap :deep(.el-table) {
  width: 100%;
}

.basic-data__actions {
  display: flex;
  flex-wrap: nowrap;
  justify-content: center;
  align-items: center;
  gap: 0;
  white-space: nowrap;
}

.basic-data__filter-input,
.basic-data :deep(.basic-data__filter-select),
.basic-data :deep(.basic-data__filter-date) {
  width: 200px;
  flex: 0 0 200px;
}

.basic-data :deep(.basic-data__filter-select) {
  width: 180px;
  flex-basis: 180px;
}

.basic-data :deep(.basic-data__form-control) {
  width: 100%;
}

@media (max-width: 1180px) {
  .basic-data {
    height: auto;
    min-height: max(560px, calc(100vh - var(--header-height) - var(--content-padding) * 2));
  }

  .basic-data__card {
    min-height: 620px;
  }

  .basic-data__table-wrap {
    flex: none;
    height: clamp(360px, 48vh, 560px);
    min-height: 0;
  }
}

@media (max-width: 768px) {
  .basic-data__tabs :deep(.el-tabs__content) {
    padding: var(--space-md);
  }

  .basic-data__table-wrap {
    height: 320px;
  }

  .basic-data__filter-input,
  .basic-data :deep(.basic-data__filter-select),
  .basic-data :deep(.basic-data__filter-date) {
    width: 100%;
    flex-basis: auto;
  }
}
</style>
