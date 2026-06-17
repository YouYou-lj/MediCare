<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>今日挂号</template>
          <div class="stat-value">{{ stats.todayRegCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>候诊人数</template>
          <div class="stat-value warning">{{ stats.waitingCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>库存预警</template>
          <div class="stat-value danger">{{ stats.stockAlertCount }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card style="margin-top: 20px">
      <template #header>欢迎使用 MediCare 智慧医疗门诊管理系统</template>
      <el-empty description="请从左侧菜单选择功能模块" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDashboardStats } from '../../api/user'

const stats = ref({ todayRegCount: 0, waitingCount: 0, stockAlertCount: 0 })

onMounted(async () => {
  try {
    const res = await getDashboardStats()
    stats.value = res.data
  } catch {}
})
</script>

<style scoped>
.dashboard { padding: 0; }
.stat-value { font-size: 36px; font-weight: bold; color: #409eff; text-align: center; }
.stat-value.warning { color: #e6a23c; }
.stat-value.danger { color: #f56c6c; }
</style>