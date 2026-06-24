<template>
  <el-container class="main-layout">
    <!-- 移动端遮罩 -->
    <div v-if="isMobile && !isCollapse" class="mobile-overlay" @click="isCollapse = true" />

    <el-aside
      :width="isCollapse ? '64px' : '220px'"
      class="sidebar"
      :class="{ 'sidebar-collapsed': isCollapse, 'sidebar-mobile': isMobile && !isCollapse }"
    >
      <div class="logo">
        <el-icon :size="24" class="logo-icon"><FirstAidKit /></el-icon>
        <span v-show="!isCollapse" class="logo-text">MediCare</span>
      </div>
      <el-menu
        :default-active="currentRoute"
        :collapse="isCollapse"
        router
        class="main-menu"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="main-content">
      <el-header class="header" :class="{ 'header-shadow': scrollTop > 10 }">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" /><Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentMenuTitle">{{ currentMenuTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <span class="header-date">{{ todayDate }}</span>
          <div class="header-user">
            <el-tag size="small" :type="roleTagType" class="role-tag">{{ roleText }}</el-tag>
            <span class="user-name">{{ userStore.currentUser?.realName }}</span>
          </div>
          <el-divider direction="vertical" />
          <el-button text class="logout-btn" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            <span>退出</span>
          </el-button>
        </div>
      </el-header>

      <el-main class="content" @scroll="handleScroll">
        <div class="page-container">
          <Transition name="fade-slide" mode="out-in">
            <router-view />
          </Transition>
        </div>
      </el-main>
    </el-container>
    <AiAssistantFloat />
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import AiAssistantFloat from '../../components/AiAssistantFloat.vue'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)
const isMobile = ref(false)
const scrollTop = ref(0)

const currentRoute = computed(() => {
  const parts = route.path.split('/').filter(Boolean)
  return parts.length > 0 ? '/' + parts[0] : '/dashboard'
})

const currentMenuTitle = computed(() => {
  const item = allMenuItems.find(i => i.path === currentRoute.value)
  return item?.title || ''
})

const roleMap: Record<string, string> = { admin: '管理员', doctor: '医生', pharmacist: '药剂师' }
const roleText = computed(() => roleMap[userStore.currentUser?.role || ''] || '')
const roleTagType = computed(() => {
  const map: Record<string, string> = { admin: 'primary', doctor: 'success', pharmacist: 'warning' }
  return map[userStore.currentUser?.role || ''] || 'info'
})

const todayDate = computed(() => dayjs().format('YYYY年MM月DD日'))

const allMenuItems = [
  { path: '/dashboard', title: '首页', icon: 'HomeFilled', roles: ['admin', 'doctor', 'pharmacist'] },
  { path: '/patients', title: '患者管理', icon: 'User', roles: ['admin', 'doctor'] },
  { path: '/basic-data', title: '基础数据', icon: 'Folder', roles: ['admin', 'doctor'] },
  { path: '/registration', title: '挂号预约', icon: 'Calendar', roles: ['admin'] },
  { path: '/workstation', title: '医生工作站', icon: 'Monitor', roles: ['admin', 'doctor'] },
  { path: '/medical-records', title: '病历管理', icon: 'Document', roles: ['admin', 'doctor'] },
  { path: '/pharmacy', title: '药品库存', icon: 'FirstAidKit', roles: ['admin', 'doctor', 'pharmacist'] },
  { path: '/prescriptions', title: '处方管理', icon: 'Notebook', roles: ['admin', 'doctor', 'pharmacist'] },
  { path: '/settings', title: '系统设置', icon: 'Setting', roles: ['admin', 'doctor', 'pharmacist'] },
  { path: '/knowledge-upload', title: '知识库上传', icon: 'UploadFilled', roles: ['admin'] },
  { path: '/knowledge-manage', title: '知识库管理', icon: 'Management', roles: ['admin'] },
]

const menuItems = computed(() => {
  const role = userStore.currentUser?.role || ''
  return allMenuItems.filter((item) => item.roles.includes(role))
})

function handleLogout() {
  userStore.clearUser()
  router.push('/login')
}

function handleScroll(e: Event) {
  const target = e.target as HTMLElement
  scrollTop.value = target.scrollTop
}

function checkMobile() {
  isMobile.value = window.innerWidth <= 768
  if (isMobile.value) isCollapse.value = true
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<style scoped>
.main-layout { height: 100vh; overflow: hidden; }

/* ===== 侧边栏 ===== */
.sidebar {
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border-color);
  box-shadow: var(--shadow-sidebar);
  transition: width 0.3s ease;
  z-index: 100;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid var(--divider-color);
  flex-shrink: 0;
}
.logo-icon {
  color: var(--color-primary);
  flex-shrink: 0;
}
.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
  letter-spacing: 0.5px;
}

.main-menu {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  border-right: none !important;
}

/* ===== 顶栏 ===== */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-card);
  border-bottom: 1px solid var(--divider-color);
  padding: 0 24px;
  height: var(--header-height);
  transition: box-shadow 0.2s ease;
  flex-shrink: 0;
}
.header-shadow {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.collapse-btn {
  cursor: pointer;
  font-size: 20px;
  color: var(--text-secondary);
  transition: color 0.2s ease;
  padding: 4px;
  border-radius: 4px;
}
.collapse-btn:hover {
  color: var(--color-primary);
  background: var(--bg-hover);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-date {
  font-size: 13px;
  color: var(--text-muted);
  white-space: nowrap;
}
.header-user {
  display: flex;
  align-items: center;
  gap: 8px;
}
.user-name {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}
.role-tag {
  font-weight: 500;
}
.logout-btn {
  color: var(--text-muted) !important;
}
.logout-btn:hover {
  color: var(--color-danger) !important;
}

/* ===== 内容区 ===== */
.content {
  background: var(--bg-page);
  overflow-y: auto;
  padding: 0;
}
.page-container {
  padding: var(--content-padding);
  max-width: var(--page-max-width);
  margin: 0 auto;
  min-height: calc(100vh - var(--header-height));
}

/* ===== 路由过渡动画 ===== */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.25s ease;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ===== 移动端抽屉 ===== */
.mobile-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 99;
}
.sidebar-mobile {
  position: fixed !important;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 100;
}

@media (max-width: 768px) {
  .header {
    padding: 0 12px;
  }
  .header-date {
    display: none;
  }
  .user-name {
    display: none;
  }
  .page-container {
    padding: 12px;
  }
}
</style>
