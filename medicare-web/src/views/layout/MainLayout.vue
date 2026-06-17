<template>
  <el-container class="main-layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <el-icon :size="24"><FirstAidKit /></el-icon>
        <span v-show="!isCollapse" class="logo-text">MediCare</span>
      </div>
      <el-menu
        :default-active="currentRoute"
        :collapse="isCollapse"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
          <Fold v-if="!isCollapse" /><Expand v-else />
        </el-icon>
        <div class="header-right">
          <el-text>{{ userStore.currentUser?.realName }} ({{ roleText }})</el-text>
          <el-button text @click="handleLogout">
            <el-icon><SwitchButton /></el-icon> 退出
          </el-button>
        </div>
      </el-header>
      <el-main class="content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)

const currentRoute = computed(() => '/' + route.path.split('/').filter(Boolean).slice(0, 2).join('/'))

const roleMap: Record<string, string> = { admin: '管理员', doctor: '医生', pharmacist: '药剂师' }
const roleText = computed(() => roleMap[userStore.currentUser?.role || ''] || '')

const allMenuItems = [
  { path: '/dashboard', title: '首页', icon: 'HomeFilled', roles: ['admin', 'doctor', 'pharmacist'] },
  { path: '/patients', title: '患者管理', icon: 'User', roles: ['admin', 'doctor'] },
  { path: '/basic-data', title: '基础数据', icon: 'Folder', roles: ['admin', 'doctor'] },
  { path: '/registration', title: '挂号预约', icon: 'Calendar', roles: ['admin'] },
  { path: '/workstation', title: '医生工作站', icon: 'Stethoscope', roles: ['admin', 'doctor'] },
  { path: '/medical-records', title: '病历管理', icon: 'Document', roles: ['admin', 'doctor'] },
  { path: '/pharmacy', title: '药品库存', icon: 'FirstAidKit', roles: ['admin', 'doctor', 'pharmacist'] },
  { path: '/prescriptions', title: '处方管理', icon: 'Notebook', roles: ['admin', 'doctor', 'pharmacist'] },
  { path: '/settings', title: '系统设置', icon: 'Setting', roles: ['admin', 'doctor', 'pharmacist'] },
]

const menuItems = computed(() => {
  const role = userStore.currentUser?.role || ''
  return allMenuItems.filter((item) => item.roles.includes(role))
})

function handleLogout() {
  userStore.clearUser()
  router.push('/login')
}
</script>

<style scoped>
.main-layout { height: 100vh; }
.sidebar {
  background: #304156;
  transition: width 0.3s;
  overflow: hidden;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 20px;
  font-weight: bold;
  border-bottom: 1px solid #3a4a5b;
}
.logo-text { white-space: nowrap; }
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e6e6e6;
  background: #fff;
  padding: 0 20px;
}
.collapse-btn { cursor: pointer; font-size: 20px; }
.header-right { display: flex; align-items: center; gap: 16px; }
.content { background: #f0f2f5; overflow-y: auto; }
.el-menu { border-right: none; }
</style>