<template>
  <el-card>
    <template #header><span>基础数据管理</span></template>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="科室管理" name="dept">
        <div style="margin-bottom:12px"><el-button type="primary" @click="openDeptDialog()"><el-icon><Plus /></el-icon> 新增科室</el-button></div>
        <el-table :data="deptList" stripe border>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="科室名称" />
          <el-table-column prop="location" label="位置" />
          <el-table-column prop="phone" label="电话" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button text type="primary" @click="openDeptDialog(row)">编辑</el-button>
              <el-popconfirm title="确定删除?" @confirm="handleDeleteDept(row.id)"><template #reference><el-button text type="danger">删除</el-button></template></el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="医生管理" name="doctor">
        <div style="margin-bottom:12px">
          <el-select v-model="deptFilter" placeholder="筛选科室" clearable style="width:180px;margin-right:12px" @change="loadDoctors">
            <el-option v-for="d in deptList" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
          <el-button type="primary" @click="openDoctorDialog()"><el-icon><Plus /></el-icon> 新增医生</el-button>
        </div>
        <el-table :data="doctorList" stripe border>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="姓名" width="100" />
          <el-table-column prop="departmentName" label="科室" width="100" />
          <el-table-column prop="title" label="职称" width="120" />
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '在职' : '停用' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button text type="primary" @click="openDoctorDialog(row)">编辑</el-button>
              <el-popconfirm title="确定删除?" @confirm="handleDeleteDoctor(row.id)"><template #reference><el-button text type="danger">删除</el-button></template></el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="排班管理" name="schedule">
        <div style="margin-bottom:12px">
          <el-date-picker v-model="schedDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="margin-right:12px" @change="loadSchedules" />
          <el-select v-model="schedDeptFilter" placeholder="筛选科室" clearable style="width:180px;margin-right:12px" @change="loadSchedules">
            <el-option v-for="d in deptList" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
          <el-button type="primary" @click="openSchedDialog()"><el-icon><Plus /></el-icon> 新增排班</el-button>
        </div>
        <el-table :data="schedList" stripe border>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="doctorName" label="医生" width="100" />
          <el-table-column prop="departmentName" label="科室" width="100" />
          <el-table-column prop="workDate" label="日期" width="120" />
          <el-table-column prop="timeSlot" label="时段" width="80" />
          <el-table-column prop="totalSlots" label="总号源" width="80" />
          <el-table-column prop="remainSlots" label="剩余" width="80">
            <template #default="{ row }"><el-tag :type="row.remainSlots > 0 ? 'success' : 'danger'">{{ row.remainSlots }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button text type="primary" @click="openSchedDialog(row)">编辑</el-button>
              <el-popconfirm title="确定删除?" @confirm="handleDeleteSched(row.id)"><template #reference><el-button text type="danger">删除</el-button></template></el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 科室弹窗 -->
    <el-dialog v-model="deptDialogVisible" :title="deptIsEdit ? '编辑科室' : '新增科室'" width="450px" destroy-on-close>
      <el-form ref="deptFormRef" :model="deptForm" :rules="deptRules" label-width="80px">
        <el-form-item label="名称" prop="name"><el-input v-model="deptForm.name" /></el-form-item>
        <el-form-item label="位置"><el-input v-model="deptForm.location" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="deptForm.phone" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="deptDialogVisible=false">取消</el-button><el-button type="primary" @click="saveDept">保存</el-button></template>
    </el-dialog>

    <!-- 医生弹窗 -->
    <el-dialog v-model="doctorDialogVisible" :title="doctorIsEdit ? '编辑医生' : '新增医生'" width="500px" destroy-on-close>
      <el-form ref="doctorFormRef" :model="doctorForm" :rules="doctorRules" label-width="80px">
        <el-form-item label="姓名" prop="name"><el-input v-model="doctorForm.name" /></el-form-item>
        <el-form-item label="科室" prop="departmentId"><el-select v-model="doctorForm.departmentId" style="width:100%"><el-option v-for="d in deptList" :key="d.id" :label="d.name" :value="d.id" /></el-select></el-form-item>
        <el-form-item label="职称"><el-input v-model="doctorForm.title" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="doctorForm.status"><el-radio :value="1">在职</el-radio><el-radio :value="0">停用</el-radio></el-radio-group></el-form-item>
      </el-form>
      <template #footer><el-button @click="doctorDialogVisible=false">取消</el-button><el-button type="primary" @click="saveDoctor">保存</el-button></template>
    </el-dialog>

    <!-- 排班弹窗 -->
    <el-dialog v-model="schedDialogVisible" :title="schedIsEdit ? '编辑排班' : '新增排班'" width="500px" destroy-on-close>
      <el-form ref="schedFormRef" :model="schedForm" :rules="schedRules" label-width="80px">
        <el-form-item label="医生" prop="doctorId"><el-select v-model="schedForm.doctorId" style="width:100%"><el-option v-for="d in doctorList" :key="d.id" :label="`${d.name} (${d.departmentName})`" :value="d.id" /></el-select></el-form-item>
        <el-form-item label="日期" prop="workDate"><el-date-picker v-model="schedForm.workDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="时段" prop="timeSlot"><el-select v-model="schedForm.timeSlot" style="width:100%"><el-option label="上午" value="上午" /><el-option label="下午" value="下午" /><el-option label="晚上" value="晚上" /></el-select></el-form-item>
        <el-form-item label="总号源" prop="totalSlots"><el-input-number v-model="schedForm.totalSlots" :min="1" /></el-form-item>
        <el-form-item label="剩余号源" prop="remainSlots"><el-input-number v-model="schedForm.remainSlots" :min="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="schedDialogVisible=false">取消</el-button><el-button type="primary" @click="saveSched">保存</el-button></template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { listDepartments, createDepartment, updateDepartment, deleteDepartment } from '../../api/department'
import { listDoctors, createDoctor, updateDoctor, deleteDoctor } from '../../api/doctor'
import { listSchedules, createSchedule, updateSchedule, deleteSchedule } from '../../api/schedule'
import type { Department, Doctor, Schedule } from '../../types'

const activeTab = ref('dept')

// ===== 科室 =====
const deptList = ref<Department[]>([])
const deptDialogVisible = ref(false)
const deptIsEdit = ref(false)
const deptFormRef = ref<FormInstance>()
const deptForm = reactive<Department>({ name: '', location: '', phone: '' })
const deptRules = { name: [{ required: true, message: '请输入科室名称', trigger: 'blur' }] }

async function loadDepts() { try { const r = await listDepartments(); deptList.value = r.data } catch {} }
function openDeptDialog(row?: Department) { deptIsEdit.value = !!row; Object.assign(deptForm, row ? { ...row } : { name: '', location: '', phone: '' }); deptDialogVisible.value = true }
async function saveDept() { const v = await deptFormRef.value?.validate().catch(()=>false); if(!v) return; try { deptIsEdit.value && deptForm.id ? await updateDepartment(deptForm.id, deptForm) : await createDepartment(deptForm); ElMessage.success('保存成功'); deptDialogVisible.value = false; loadDepts() } catch {} }
async function handleDeleteDept(id: number) { try { await deleteDepartment(id); ElMessage.success('删除成功'); loadDepts() } catch {} }

// ===== 医生 =====
const doctorList = ref<Doctor[]>([])
const deptFilter = ref<number>()
const doctorDialogVisible = ref(false)
const doctorIsEdit = ref(false)
const doctorFormRef = ref<FormInstance>()
const doctorForm = reactive<Doctor>({ name: '', departmentId: 1, title: '', status: 1 })
const doctorRules = { name: [{ required: true, message: '请输入姓名', trigger: 'blur' }], departmentId: [{ required: true, message: '请选择科室', trigger: 'change' }] }

async function loadDoctors() { try { const r = await listDoctors(deptFilter.value); doctorList.value = r.data } catch {} }
function openDoctorDialog(row?: Doctor) { doctorIsEdit.value = !!row; Object.assign(doctorForm, row ? { ...row } : { name: '', departmentId: 1, title: '', status: 1 }); doctorDialogVisible.value = true }
async function saveDoctor() { const v = await doctorFormRef.value?.validate().catch(()=>false); if(!v) return; try { doctorIsEdit.value && doctorForm.id ? await updateDoctor(doctorForm.id, doctorForm) : await createDoctor(doctorForm); ElMessage.success('保存成功'); doctorDialogVisible.value = false; loadDoctors() } catch {} }
async function handleDeleteDoctor(id: number) { try { await deleteDoctor(id); ElMessage.success('删除成功'); loadDoctors() } catch {} }

// ===== 排班 =====
const schedList = ref<Schedule[]>([])
const schedDate = ref(new Date().toISOString().slice(0, 10))
const schedDeptFilter = ref<number>()
const schedDialogVisible = ref(false)
const schedIsEdit = ref(false)
const schedFormRef = ref<FormInstance>()
const schedForm = reactive<Schedule>({ doctorId: 1, workDate: '', timeSlot: '上午', totalSlots: 20, remainSlots: 20 })
const schedRules = { doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }], workDate: [{ required: true, message: '请选择日期', trigger: 'change' }], timeSlot: [{ required: true, message: '请选择时段', trigger: 'change' }] }

async function loadSchedules() { try { const r = await listSchedules(schedDate.value, schedDeptFilter.value); schedList.value = r.data } catch {} }
function openSchedDialog(row?: Schedule) { schedIsEdit.value = !!row; Object.assign(schedForm, row ? { ...row } : { doctorId: 1, workDate: '', timeSlot: '上午', totalSlots: 20, remainSlots: 20 }); schedDialogVisible.value = true }
async function saveSched() { const v = await schedFormRef.value?.validate().catch(()=>false); if(!v) return; try { schedIsEdit.value && schedForm.id ? await updateSchedule(schedForm.id, schedForm) : await createSchedule(schedForm); ElMessage.success('保存成功'); schedDialogVisible.value = false; loadSchedules() } catch {} }
async function handleDeleteSched(id: number) { try { await deleteSchedule(id); ElMessage.success('删除成功'); loadSchedules() } catch {} }

onMounted(async () => { await loadDepts(); await loadDoctors(); await loadSchedules() })
</script>