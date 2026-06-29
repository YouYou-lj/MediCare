<template>
  <el-container class="main-layout">
    <!-- 移动端遮罩 -->
    <div v-if="isMobile && !isCollapse" class="mobile-overlay" @click="isCollapse = true" />

    <el-aside
      :width="isCollapse ? '64px' : '240px'"
      class="sidebar"
      :class="{ 'sidebar-collapsed': isCollapse, 'sidebar-mobile': isMobile && !isCollapse }"
    >
      <div class="logo">
        <div class="logo-mark">
          <el-icon :size="24" class="logo-icon"><FirstAidKit /></el-icon>
        </div>
        <div v-show="!isCollapse" class="logo-copy">
          <span class="logo-text">MediCare</span>
          <span class="logo-subtitle">Clinic System</span>
        </div>
      </div>
      <el-menu
        :default-active="currentRoute"
        :collapse="isCollapse"
        router
        class="main-menu"
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="item.path"
          :index="item.path"
          :class="'menu-item-' + item.path.replace('/', '')"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
      <div v-show="!isCollapse" class="sidebar-status">
        <span class="sidebar-status__dot" />
        <span>系统运行正常</span>
      </div>
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
const roleText = computed(() => {
  if (userStore.currentUser?.id === 1) return '主管理员'
  return roleMap[userStore.currentUser?.role || ''] || ''
})
const roleTagType = computed(() => {
  const map: Record<string, string> = { admin: 'primary', doctor: 'success', pharmacist: 'warning' }
  return map[userStore.currentUser?.role || ''] || 'info'
})

const todayDate = computed(() => dayjs().format('YYYY年MM月DD日'))

const allMenuItems = [
  { path: '/dashboard', title: '首页', icon: 'HomeFilled' },
  { path: '/patients', title: '患者管理', icon: 'User' },
  { path: '/basic-data', title: '基础数据', icon: 'Folder' },
  { path: '/registration', title: '挂号预约', icon: 'Calendar' },
  { path: '/workstation', title: '医生工作站', icon: 'Monitor' },
  { path: '/medical-records', title: '病历管理', icon: 'Document' },
  { path: '/pharmacy', title: '药品库存', icon: 'FirstAidKit' },
  { path: '/prescriptions', title: '处方管理', icon: 'Notebook' },
  { path: '/settings', title: '系统设置', icon: 'Setting' },
  { path: '/knowledge-upload', title: '知识库上传', icon: 'UploadFilled' },
  { path: '/knowledge-manage', title: '知识库管理', icon: 'Management' },
]

const menuItems = computed(() => allMenuItems)

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
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(180deg, var(--bg-sidebar) 0%, var(--bg-toolbar) 100%);
  border-right: 1px solid var(--border-light);
  box-shadow: var(--shadow-sidebar);
  transition: width 0.3s ease;
  z-index: 100;
  display: flex;
  flex-direction: column;
}

.sidebar::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(90deg, var(--color-primary-light) 0, transparent 38%),
    linear-gradient(180deg, transparent 0%, var(--color-blue-light) 52%, transparent 100%);
  opacity: 0.72;
}

.sidebar::after {
  content: '';
  position: absolute;
  top: 86px;
  right: 0;
  bottom: 28px;
  width: 1px;
  background: linear-gradient(180deg, transparent 0%, var(--color-primary) 22%, var(--color-blue) 72%, transparent 100%);
  opacity: 0.35;
}

.logo {
  position: relative;
  z-index: 1;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: var(--space-md);
  padding: 0 var(--space-lg);
  border-bottom: 1px solid var(--border-light);
  flex-shrink: 0;
}
.logo-mark {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: var(--radius-card);
  color: var(--text-inverse);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  box-shadow: var(--shadow-card-hover);
}
.logo-icon {
  color: currentColor;
  flex-shrink: 0;
}
.logo-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}
.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
  letter-spacing: 0;
  line-height: 1.1;
}
.logo-subtitle {
  color: var(--text-muted);
  font-size: var(--font-size-xs);
  font-weight: 600;
  line-height: 1.2;
  text-transform: uppercase;
}

.main-menu {
  position: relative;
  z-index: 1;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  border-right: none !important;
  padding: var(--space-lg) var(--space-md);
  background: transparent !important;
  --el-menu-item-height: 44px;
}

.main-menu :deep(.el-menu-item) {
  --menu-accent: var(--color-primary);
  --menu-accent-soft: var(--color-primary-light);
  position: relative;
  height: 44px;
  margin: 0 0 var(--space-xs);
  padding: 0 var(--space-md) !important;
  border-radius: var(--radius-card) !important;
  color: var(--text-secondary);
  font-size: var(--font-size-base);
  font-weight: 600;
  line-height: 44px;
  transition:
    background 0.2s ease,
    color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.main-menu :deep(.el-menu-item::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 3px;
  border-radius: var(--radius-tag);
  background: var(--menu-accent);
  opacity: 0;
  transform: scaleY(0.4);
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.main-menu :deep(.el-menu-item .el-icon) {
  width: 28px;
  height: 28px;
  margin-right: var(--space-md);
  border-radius: var(--radius-button);
  color: var(--menu-accent);
  background: var(--menu-accent-soft);
  transition: background 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.main-menu :deep(.el-menu-item:hover) {
  color: var(--text-primary);
  background: var(--bg-card);
  box-shadow: var(--shadow-card);
  transform: translateX(2px);
}

.main-menu :deep(.el-menu-item:hover .el-icon) {
  color: var(--menu-accent);
  background: var(--menu-accent-soft);
}

.main-menu :deep(.el-menu-item.is-active) {
  color: var(--menu-accent) !important;
  background:
    linear-gradient(90deg, var(--menu-accent-soft) 0%, var(--bg-card) 72%);
  box-shadow: var(--shadow-card-hover);
}

.main-menu :deep(.el-menu-item.is-active::before) {
  opacity: 1;
  transform: scaleY(1);
}

.main-menu :deep(.el-menu-item.is-active .el-icon) {
  color: var(--text-inverse) !important;
  background: var(--menu-accent) !important;
  box-shadow: var(--shadow-card);
}

.main-menu :deep(.menu-item-dashboard) {
  --menu-accent: var(--color-primary);
  --menu-accent-soft: var(--color-primary-light);
}

.main-menu :deep(.menu-item-patients) {
  --menu-accent: var(--color-success);
  --menu-accent-soft: var(--color-success-light);
}

.main-menu :deep(.menu-item-basic-data) {
  --menu-accent: var(--color-blue);
  --menu-accent-soft: var(--color-blue-light);
}

.main-menu :deep(.menu-item-registration) {
  --menu-accent: var(--color-warning);
  --menu-accent-soft: var(--color-warning-light);
}

.main-menu :deep(.menu-item-workstation) {
  --menu-accent: var(--color-primary-dark);
  --menu-accent-soft: var(--color-primary-light);
}

.main-menu :deep(.menu-item-medical-records) {
  --menu-accent: var(--color-info);
  --menu-accent-soft: var(--color-info-light);
}

.main-menu :deep(.menu-item-pharmacy) {
  --menu-accent: var(--color-success);
  --menu-accent-soft: var(--color-success-light);
}

.main-menu :deep(.menu-item-prescriptions) {
  --menu-accent: var(--color-warning);
  --menu-accent-soft: var(--color-warning-light);
}

.main-menu :deep(.menu-item-settings) {
  --menu-accent: var(--text-secondary);
  --menu-accent-soft: var(--bg-hover);
}

.main-menu :deep(.menu-item-knowledge-upload) {
  --menu-accent: var(--color-blue);
  --menu-accent-soft: var(--color-blue-light);
}

.main-menu :deep(.menu-item-knowledge-manage) {
  --menu-accent: var(--color-primary);
  --menu-accent-soft: var(--color-primary-light);
}

.sidebar-collapsed .logo {
  justify-content: center;
  padding: 0 var(--space-md);
}

.sidebar-collapsed .logo-mark {
  width: 38px;
  height: 38px;
}

.sidebar-collapsed .main-menu {
  padding: var(--space-lg) var(--space-sm);
}

.sidebar-collapsed .main-menu :deep(.el-menu-item) {
  justify-content: center;
  padding: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
  transform: none;
}

.sidebar-collapsed .main-menu :deep(.el-menu-item .el-icon) {
  margin-right: 0;
  color: var(--menu-accent) !important;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  opacity: 0.7;
}

.sidebar-collapsed .main-menu :deep(.el-menu-item::before) {
  display: none;
}

.sidebar-collapsed .main-menu :deep(.el-menu-item:hover),
.sidebar-collapsed .main-menu :deep(.el-menu-item.is-active) {
  color: var(--menu-accent) !important;
  background: transparent !important;
  box-shadow: none !important;
}

.sidebar-collapsed .main-menu :deep(.el-menu-item:hover .el-icon) {
  color: var(--menu-accent) !important;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  opacity: 1;
  transform: scale(1.08);
}

.sidebar-collapsed .main-menu :deep(.el-menu-item.is-active .el-icon) {
  color: var(--text-inverse) !important;
  background: var(--menu-accent) !important;
  border: none !important;
  box-shadow: var(--shadow-card) !important;
  opacity: 1;
  transform: scale(1.18);
  filter: drop-shadow(0 4px 8px var(--menu-accent-soft));
}

.sidebar-status {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin: 0 var(--space-lg) var(--space-lg);
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-card);
  color: var(--text-muted);
  background: color-mix(in srgb, var(--bg-card) 86%, transparent);
  font-size: var(--font-size-xs);
  font-weight: 600;
  box-shadow: var(--shadow-card);
}

.sidebar-status__dot {
  position: relative;
  width: 8px;
  height: 8px;
  flex-shrink: 0;
  border-radius: var(--radius-circle);
  background: var(--color-success);
  box-shadow: 0 0 0 4px var(--color-success-light);
}

.sidebar-status__dot::after {
  content: '';
  position: absolute;
  inset: -5px;
  border: 1px solid var(--color-success);
  border-radius: inherit;
  opacity: 0;
  animation: sidebarPulse 2.6s ease-out infinite;
}

@keyframes sidebarPulse {
  0% {
    opacity: 0.45;
    transform: scale(0.7);
  }
  70%,
  100% {
    opacity: 0;
    transform: scale(1.6);
  }
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
  width: 100%;
  min-height: calc(100vh - var(--header-height));
  box-sizing: border-box;
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
  background: color-mix(in srgb, var(--text-primary) 32%, transparent);
  z-index: 99;
}
.sidebar-mobile {
  position: fixed !important;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 100;
}

@media (prefers-reduced-motion: reduce) {
  .sidebar,
  .main-menu :deep(.el-menu-item),
  .main-menu :deep(.el-menu-item::before),
  .main-menu :deep(.el-menu-item .el-icon),
  .sidebar-status__dot::after {
    animation: none;
    transition: none;
  }
}

@media (max-width: 768px) {
  .sidebar:not(.sidebar-mobile) {
    width: 64px !important;
    flex: 0 0 64px !important;
  }

  .sidebar:not(.sidebar-mobile) .logo-text {
    display: none;
  }

  .sidebar:not(.sidebar-mobile) .logo-subtitle,
  .sidebar:not(.sidebar-mobile) .sidebar-status {
    display: none;
  }

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
