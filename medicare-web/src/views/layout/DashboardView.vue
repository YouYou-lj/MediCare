<template>
  <div class="dashboard">
    <PageHeader title="首页" :subtitle="welcomeSubtitle" />

    <div class="stat-grid">
      <StatCard
        icon="Calendar"
        label="今日挂号"
        :value="stats.todayRegCount"
        color="#0F9F8F"
      />
      <StatCard
        icon="Clock"
        label="候诊人数"
        :value="stats.waitingCount"
        color="#F59E0B"
      />
      <StatCard
        icon="Box"
        label="库存预警"
        :value="stats.stockAlertCount"
        color="#EF4444"
        :variant="stats.stockAlertCount > 0 ? 'alert' : 'default'"
      />
      <StatCard
        v-if="isDoctor"
        icon="CircleCheck"
        label="今日已接诊"
        :value="stats.completedCount"
        color="#3B82F6"
      />
      <StatCard
        v-if="isPharmacist"
        icon="Goods"
        label="待配药"
        :value="stats.pendingDispenseCount"
        color="#8B5CF6"
      />
    </div>

    <div class="dashboard-content">
      <div class="chart-row">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <span class="chart-title">📈 近7日挂号趋势</span>
          </template>
          <div ref="trendChartRef" class="chart-container" />
        </el-card>

        <el-card class="chart-card" shadow="hover">
          <template #header>
            <span class="chart-title">🏥 科室挂号分布</span>
          </template>
          <div ref="deptChartRef" class="chart-container" />
        </el-card>
      </div>

      <div class="chart-row">
        <el-card
          class="chart-card"
          shadow="hover"
        >
          <template #header>
            <span class="chart-title">⚠️ 低库存药品 Top5</span>
          </template>
          <div ref="stockChartRef" class="chart-container" />
        </el-card>

        <el-card class="chart-card" shadow="hover">
          <template #header>
            <span class="chart-title">⚡ 快捷操作</span>
          </template>
          <div class="quick-actions">
            <div
              v-for="action in quickActions"
              :key="action.path"
              class="action-item"
              @click="$router.push(action.path)"
            >
              <div class="action-icon" :style="{ background: action.color }">
                <el-icon :size="20" color="#fff">
                  <component :is="action.icon" />
                </el-icon>
              </div>
              <div class="action-info">
                <div class="action-title">{{ action.title }}</div>
                <div class="action-desc">{{ action.desc }}</div>
              </div>
              <el-icon class="action-arrow" color="#94a3b8">
                <ArrowRight />
              </el-icon>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, nextTick, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { getDashboardStats } from '../../api/user'
import PageHeader from '../../components/PageHeader.vue'
import StatCard from '../../components/StatCard.vue'
import * as echarts from 'echarts'
import {
  Calendar, Clock, Box, CircleCheck, Goods, TakeawayBox,
  ArrowRight, User, Document, FirstAidKit, DataLine, Setting
} from '@element-plus/icons-vue'

const route = useRoute()
const userStore = useUserStore()

const stats = ref({
  todayRegCount: 0,
  waitingCount: 0,
  stockAlertCount: 0,
  completedCount: 0,
  pendingDispenseCount: 0,
  regTrend: [] as { date: string; count: number }[],
  deptRegDistribution: [] as { deptName: string; count: number }[],
  lowStockTopN: [] as { medicineId: number; name: string; stock: number; safetyStock: number }[]
})

const isAdmin = computed(() => userStore.currentUser?.role === 'admin')
const isDoctor = computed(() => userStore.currentUser?.role === 'doctor')
const isPharmacist = computed(() => userStore.currentUser?.role === 'pharmacist')

const welcomeSubtitle = computed(() => {
  const role = userStore.currentUser?.role
  const name = userStore.currentUser?.realName || '用户'
  const roleMap: Record<string, string> = {
    admin: '管理员',
    doctor: '医生',
    pharmacist: '药师',
    registrar: '挂号员',
    nurse: '护士'
  }
  return `${name}，欢迎登录 MediCare 智慧医疗门诊管理系统 — 您当前以【${roleMap[role || '']}】身份访问`
})

const quickActions = computed(() => {
  const role = userStore.currentUser?.role
  const all = [
    { title: '患者挂号', desc: '为患者办理挂号业务', icon: 'User', color: '#0F9F8F', path: '/registration', roles: ['admin', 'registrar'] },
    { title: '医生工作站', desc: '接诊、开处方', icon: 'FirstAidKit', color: '#3B82F6', path: '/doctor', roles: ['admin', 'doctor'] },
    { title: '病历管理', desc: '查看与编辑病历', icon: 'Document', color: '#8B5CF6', path: '/records', roles: ['admin', 'doctor', 'nurse'] },
    { title: '药品库存', desc: '管理药品库存与预警', icon: 'Box', color: '#F59E0B', path: '/medicines', roles: ['admin', 'pharmacist'] },
    { title: '处方管理', desc: '查看待配药处方', icon: 'Goods', color: '#10B981', path: '/prescriptions', roles: ['admin', 'pharmacist', 'doctor'] },
    { title: '数据统计', desc: '查看业务数据报表', icon: 'DataLine', color: '#6366F1', path: '/dashboard', roles: ['admin', 'registrar'] },
    { title: '系统设置', desc: '基础数据与账号管理', icon: 'Setting', color: '#6B7280', path: '/basic-data', roles: ['admin'] }
  ]
  return all.filter(a => a.roles.includes(role || ''))
})

const trendChartRef = ref<HTMLElement>()
const deptChartRef = ref<HTMLElement>()
const stockChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let deptChart: echarts.ECharts | null = null
let stockChart: echarts.ECharts | null = null

function initTrendChart() {
  if (!trendChartRef.value || !stats.value.regTrend.length) return
  trendChart = echarts.init(trendChartRef.value)
  const dates = stats.value.regTrend.map(d => d.date)
  const counts = stats.value.regTrend.map(d => d.count)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: dates, axisLine: { lineStyle: { color: '#cbd5e1' } }, axisLabel: { color: '#64748b' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#e2e8f0' } }, axisLabel: { color: '#64748b' } },
    series: [{
      data: counts,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      itemStyle: { color: '#0F9F8F' },
      lineStyle: { width: 3, color: '#0F9F8F' },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(15, 159, 143, 0.3)' },
            { offset: 1, color: 'rgba(15, 159, 143, 0.01)' }
          ]
        }
      }
    }]
  })
}

function initDeptChart() {
  if (!deptChartRef.value || !stats.value.deptRegDistribution.length) return
  deptChart = echarts.init(deptChartRef.value)
  const colors = ['#0F9F8F', '#3B82F6', '#F59E0B', '#EF4444', '#8B5CF6', '#10B981', '#6366F1']
  deptChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { color: '#64748b', fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{c}人', color: '#334155', fontSize: 12 },
      labelLine: { length: 10, length2: 10 },
      data: stats.value.deptRegDistribution.map((d, i) => ({
        name: d.deptName,
        value: d.count,
        itemStyle: { color: colors[i % colors.length] }
      }))
    }]
  })
}

function initStockChart() {
  if (!stockChartRef.value || !stats.value.lowStockTopN.length) return
  stockChart = echarts.init(stockChartRef.value)
  const names = stats.value.lowStockTopN.map(d => d.name)
  const stocks = stats.value.lowStockTopN.map(d => d.stock)
  const safetyStocks = stats.value.lowStockTopN.map(d => d.safetyStock)
  stockChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['当前库存', '安全库存'], bottom: 0, textStyle: { color: '#64748b', fontSize: 12 } },
    grid: { left: 80, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#e2e8f0' } }, axisLabel: { color: '#64748b' } },
    yAxis: { type: 'category', data: names, axisLine: { lineStyle: { color: '#cbd5e1' } }, axisLabel: { color: '#64748b' } },
    series: [
      { name: '当前库存', type: 'bar', data: stocks, itemStyle: { color: '#EF4444', borderRadius: [0, 4, 4, 0] }, barWidth: 12 },
      { name: '安全库存', type: 'bar', data: safetyStocks, itemStyle: { color: '#cbd5e1', borderRadius: [0, 4, 4, 0] }, barWidth: 12 }
    ]
  })
}

onMounted(async () => {
  try {
    const res = await getDashboardStats()
    stats.value = res.data
    await nextTick()
    initTrendChart()
    initDeptChart()
    initStockChart()
  } catch (e) {
    console.error('获取统计数据失败', e)
  }
})

onUnmounted(() => {
  trendChart?.dispose()
  deptChart?.dispose()
  stockChart?.dispose()
})
</script>

<style scoped>
.dashboard {
  padding: 0;
  animation: fadeIn 0.4s ease-out;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-lg);
  margin-bottom: var(--space-xl);
}

.dashboard-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-xl);
}

.chart-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-lg);
}

.chart-card {
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.chart-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08) !important;
}

.chart-title {
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--text-primary);
}

.chart-container {
  width: 100%;
  height: 280px;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  padding: var(--space-sm) 0;
}

.action-item {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md) var(--space-lg);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.action-item:hover {
  background: var(--bg-light);
  border-color: var(--color-primary-10);
  transform: translateX(4px);
}

.action-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.action-info {
  flex: 1;
  min-width: 0;
}

.action-title {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-primary);
}

.action-desc {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  margin-top: 2px;
}

.action-arrow {
  flex-shrink: 0;
  transition: transform 0.2s ease;
}

.action-item:hover .action-arrow {
  transform: translateX(4px);
  color: var(--color-primary) !important;
}

@media (max-width: 768px) {
  .chart-row {
    grid-template-columns: 1fr;
  }

  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
