<template>
  <div class="login-container">
    <!-- 背景装饰 -->
    <div class="login-bg-grid" />
    <div class="login-bg-wave" />
    <div class="login-bg-circle login-bg-circle-1" />
    <div class="login-bg-circle login-bg-circle-2" />

    <div class="login-wrapper">
      <!-- 左侧系统介绍 -->
      <div class="login-brand">
        <div class="brand-logo">
          <el-icon :size="48"><FirstAidKit /></el-icon>
          <span class="brand-name">MediCare</span>
        </div>
        <h2 class="brand-title">智慧医疗门诊管理系统</h2>
        <p class="brand-desc">
          集患者管理、挂号预约、医生工作站、<br />
          病历管理、药品库存、处方管理于一体<br />
          的现代化医疗信息化平台。
        </p>
        <div class="brand-features">
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>患者全生命周期管理</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>智能挂号与排班系统</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>电子病历与处方管理</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>AI 智能医疗辅助</span>
          </div>
        </div>
      </div>

      <!-- 右侧登录卡片 -->
      <div class="login-card" :class="{ 'shake-animation': shake }">
        <div class="login-header">
          <h1 class="login-title">欢迎登录</h1>
          <p class="login-subtitle">请使用您的账号密码登录系统</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="0"
          @submit.prevent="handleLogin"
          class="login-form"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              size="large"
              class="login-input"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
              class="login-input"
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="login-btn"
              @click="handleLogin"
            >
              <el-icon v-if="!loading" class="btn-icon"><Right /></el-icon>
              <span>登 录</span>
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 小猫咪互动轨道 -->
        <div class="cat-track">
          <div
            v-for="(pos, idx) in catPositions"
            :key="idx"
            class="cat-slot"
            :class="{ 'active-slot': catPosition === idx }"
            @mouseenter="handleCatHover(idx)"
          >
            <div v-if="catPosition === idx" class="cat-wrapper" :class="{ jump: catJumping }">
              <div class="cat-ear left" />
              <div class="cat-ear right" />
              <div class="cat-head">
                <div class="cat-eye left" />
                <div class="cat-eye right" />
                <div class="cat-nose" />
                <div class="cat-mouth" />
                <div class="cat-whisker left" />
                <div class="cat-whisker right" />
              </div>
              <div class="cat-body" />
              <div class="cat-belly" />
              <div class="cat-tail" />
              <div class="cat-paw left" />
              <div class="cat-paw right" />
              <div class="cat-z" :class="{ show: catSleeping }">Z</div>
            </div>
          </div>
        </div>
        <p class="cat-hint">{{ catHint }}</p>
      </div>
    </div>

    <!-- 底部版权 -->
    <div class="login-footer">
      <p>MediCare 智慧医疗门诊管理系统 v1.0</p>
      <p>本项目由沈院士特别赞助开发</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { login } from '../../api/auth'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const shake = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: ['blur', 'change'] }],
  password: [{ required: true, message: '请输入密码', trigger: ['blur', 'change'] }],
}

// ===== 小猫咪状态 =====
const catPositions = ['左', '中', '右']
const catPosition = ref(1) // 0=左, 1=中, 2=右
const catJumping = ref(false)
const catSleeping = ref(false)
const catHint = ref('小猫咪正在陪伴你登录...')
let autoMoveTimer: ReturnType<typeof setInterval> | null = null
let sleepTimer: ReturnType<typeof setTimeout> | null = null

function moveCatTo(newPos: number) {
  if (newPos === catPosition.value) return
  catJumping.value = true
  catSleeping.value = false
  catHint.value = '小猫咪跳到了' + catPositions[newPos] + '边！'
  setTimeout(() => {
    catPosition.value = newPos
    catJumping.value = false
    // 跳跃后进入"打盹"状态
    if (sleepTimer) clearTimeout(sleepTimer)
    sleepTimer = setTimeout(() => {
      catSleeping.value = true
      catHint.value = '小猫咪在打盹 Zzz...'
    }, 2000)
  }, 200)
}

function handleCatHover(hoveredIdx: number) {
  if (hoveredIdx !== catPosition.value) return
  // 随机向左或向右移动，边界处理
  const directions = []
  if (catPosition.value > 0) directions.push(-1)
  if (catPosition.value < 2) directions.push(1)
  if (directions.length === 0) return
  const dir = directions[Math.floor(Math.random() * directions.length)]
  const newPos = catPosition.value + dir
  moveCatTo(newPos)
}

function autoMoveCat() {
  // 随机选择一个不同于当前的位置
  const candidates = [0, 1, 2].filter(i => i !== catPosition.value)
  const newPos = candidates[Math.floor(Math.random() * candidates.length)]
  moveCatTo(newPos)
}

onMounted(() => {
  // 初始随机位置
  catPosition.value = Math.floor(Math.random() * 3)
  // 自动移动：每 5~8 秒随机移动一次
  const randomInterval = () => 5000 + Math.floor(Math.random() * 3000)
  const scheduleNext = () => {
    autoMoveTimer = setTimeout(() => {
      autoMoveCat()
      scheduleNext()
    }, randomInterval())
  }
  scheduleNext()
})

onUnmounted(() => {
  if (autoMoveTimer) clearTimeout(autoMoveTimer)
  if (sleepTimer) clearTimeout(sleepTimer)
})

function triggerShake() {
  shake.value = true
  setTimeout(() => { shake.value = false }, 500)
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    triggerShake()
    return
  }
  loading.value = true
  try {
    const res = await login(form)
    userStore.setUser(res.data)
    ElMessage.success('登录成功')
    router.push('/')
  } catch {
    triggerShake()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #E8F4F8 0%, #F0F8F5 40%, #E0F0FB 100%);
  overflow: hidden;
}

/* 背景网格 */
.login-bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(15, 159, 143, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(15, 159, 143, 0.04) 1px, transparent 1px);
  background-size: 40px 40px;
  pointer-events: none;
}

/* 背景波浪装饰 */
.login-bg-wave {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 280px;
  background: linear-gradient(180deg, transparent 0%, rgba(15, 159, 143, 0.06) 100%);
  border-radius: 50% 50% 0 0 / 60px 60px 0 0;
  pointer-events: none;
}

/* 背景圆形装饰 */
.login-bg-circle {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}
.login-bg-circle-1 {
  width: 400px;
  height: 400px;
  top: -120px;
  right: -80px;
  background: radial-gradient(circle, rgba(22, 119, 255, 0.08) 0%, transparent 70%);
}
.login-bg-circle-2 {
  width: 300px;
  height: 300px;
  bottom: 80px;
  left: -60px;
  background: radial-gradient(circle, rgba(15, 159, 143, 0.1) 0%, transparent 70%);
}

/* 主容器 */
.login-wrapper {
  display: flex;
  align-items: center;
  gap: 80px;
  z-index: 1;
  padding: 40px;
  max-width: 1100px;
  width: 100%;
}

/* 左侧品牌区 */
.login-brand {
  flex: 1;
  max-width: 480px;
  color: var(--text-primary);
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}
.brand-logo .el-icon {
  color: var(--color-primary);
}
.brand-name {
  font-size: 36px;
  font-weight: 700;
  color: var(--color-primary);
  letter-spacing: 1px;
}

.brand-title {
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 16px;
  line-height: 1.3;
}
.brand-desc {
  font-size: 16px;
  color: var(--text-secondary);
  line-height: 1.8;
  margin: 0 0 32px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  color: var(--text-secondary);
}
.feature-item .el-icon {
  color: var(--color-primary);
  font-size: 18px;
}

/* 右侧登录卡片 */
.login-card {
  width: 400px;
  padding: 40px 36px;
  background: var(--bg-card);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card-hover);
  transition: box-shadow 0.3s ease;
}
.login-card:hover {
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.login-header {
  text-align: center;
  margin-bottom: 28px;
}
.login-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px;
}
.login-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: var(--radius-input);
  box-shadow: 0 0 0 1px var(--border-color) inset;
  transition: box-shadow 0.2s ease;
}
.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--color-primary) inset, 0 0 0 3px var(--color-primary-light);
}
.login-input :deep(.el-input__prefix) {
  color: var(--text-muted);
}

.login-btn {
  width: 100%;
  height: 44px;
  border-radius: var(--radius-button);
  font-size: 16px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: linear-gradient(135deg, var(--color-primary) 0%, #0D8D7E 100%);
  border: none;
  transition: all 0.2s ease;
}
.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(15, 159, 143, 0.3);
}
.btn-icon {
  font-size: 16px;
}

/* ===== 小猫咪互动轨道 ===== */
.cat-track {
  display: flex;
  justify-content: space-around;
  align-items: center;
  margin-top: 20px;
  padding: 16px 24px;
  background: var(--bg-toolbar);
  border-radius: var(--radius-card);
  border: 1px dashed var(--border-color);
  min-height: 100px;
  position: relative;
}

.cat-slot {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 80px;
  position: relative;
  cursor: default;
  outline: none;
  user-select: none;
  -webkit-user-select: none;
  border-radius: var(--radius-table);
}
/* ===== 小猫咪 CSS 绘制 ===== */
.cat-wrapper {
  position: relative;
  width: 72px;
  height: 72px;
  transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.cat-wrapper.jump {
  animation: catJump 0.4s ease;
}
@keyframes catJump {
  0% { transform: translateY(0) scale(1); }
  40% { transform: translateY(-12px) scale(1.05); }
  100% { transform: translateY(0) scale(1); }
}

.cat-head {
  position: absolute;
  width: 52px;
  height: 44px;
  background: #F5A623;
  border-radius: 50% 50% 45% 45%;
  top: 14px;
  left: 10px;
  z-index: 2;
  box-shadow: 0 2px 6px rgba(245, 166, 35, 0.3);
}

.cat-ear {
  position: absolute;
  width: 0;
  height: 0;
  border-left: 9px solid transparent;
  border-right: 9px solid transparent;
  border-bottom: 16px solid #F5A623;
  z-index: 1;
}
.cat-ear.left { top: 2px; left: 12px; transform: rotate(-15deg); }
.cat-ear.right { top: 2px; right: 12px; transform: rotate(15deg); }
.cat-ear::after {
  content: '';
  position: absolute;
  top: 5px;
  left: -5px;
  width: 0;
  height: 0;
  border-left: 5px solid transparent;
  border-right: 5px solid transparent;
  border-bottom: 9px solid #FF9999;
}

.cat-eye {
  position: absolute;
  width: 7px;
  height: 7px;
  background: #333;
  border-radius: 50%;
  top: 18px;
  z-index: 3;
  animation: catBlink 3s infinite;
}
.cat-eye.left { left: 13px; }
.cat-eye.right { right: 13px; }
@keyframes catBlink {
  0%, 90%, 100% { transform: scaleY(1); }
  95% { transform: scaleY(0.1); }
}

.cat-nose {
  position: absolute;
  width: 5px;
  height: 4px;
  background: #FF9999;
  border-radius: 50% 50% 50% 50% / 60% 60% 40% 40%;
  top: 26px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 3;
}

.cat-mouth {
  position: absolute;
  width: 10px;
  height: 5px;
  border: 2px solid transparent;
  border-bottom-color: #333;
  border-radius: 0 0 50% 50%;
  top: 28px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 3;
}

.cat-whisker {
  position: absolute;
  width: 12px;
  height: 1px;
  background: rgba(0,0,0,0.15);
  top: 24px;
  z-index: 3;
}
.cat-whisker.left { left: 2px; transform: rotate(-10deg); }
.cat-whisker.right { right: 2px; transform: rotate(10deg); }

.cat-body {
  position: absolute;
  width: 36px;
  height: 28px;
  background: #F5A623;
  border-radius: 50% 50% 40% 40%;
  top: 46px;
  left: 18px;
  z-index: 1;
}

.cat-belly {
  position: absolute;
  width: 20px;
  height: 18px;
  background: #FFF8E7;
  border-radius: 50%;
  top: 50px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 2;
}

.cat-tail {
  position: absolute;
  width: 7px;
  height: 24px;
  background: #F5A623;
  border-radius: 0 0 50% 50%;
  top: 48px;
  right: 6px;
  z-index: 0;
  transform-origin: top center;
  animation: catTailWag 2s ease-in-out infinite;
}
@keyframes catTailWag {
  0%, 100% { transform: rotate(-10deg); }
  50% { transform: rotate(15deg); }
}

.cat-paw {
  position: absolute;
  width: 9px;
  height: 7px;
  background: #F5A623;
  border-radius: 50% 50% 40% 40%;
  bottom: 2px;
  z-index: 3;
}
.cat-paw.left { left: 16px; }
.cat-paw.right { right: 16px; }

.cat-z {
  position: absolute;
  top: -6px;
  right: -2px;
  font-size: 13px;
  font-weight: bold;
  color: var(--color-primary);
  opacity: 0;
  z-index: 4;
}
.cat-z.show {
  animation: catZzz 2s ease-in-out infinite;
}
@keyframes catZzz {
  0% { opacity: 0; transform: translate(0, 0) scale(0.8); }
  30% { opacity: 1; transform: translate(4px, -8px) scale(1); }
  60% { opacity: 0.6; transform: translate(8px, -14px) scale(1.1); }
  100% { opacity: 0; transform: translate(12px, -20px) scale(1.2); }
}

.cat-hint {
  text-align: center;
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 8px;
  opacity: 0.7;
  min-height: 18px;
}

/* 错误抖动 */
.shake-animation {
  animation: shake 0.5s ease-in-out;
}
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-6px); }
  20%, 40%, 60%, 80% { transform: translateX(6px); }
}

/* 底部版权 */
.login-footer {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  text-align: center;
  z-index: 1;
}
.login-footer p {
  font-size: 15px;
  color: var(--text-muted);
  margin: 4px 0;
  opacity: 0.85;
}

/* 响应式 */
@media (max-width: 900px) {
  .login-wrapper {
    flex-direction: column;
    gap: 32px;
    padding: 24px;
  }
  .login-brand {
    text-align: center;
    max-width: 100%;
  }
  .brand-features {
    display: none;
  }
  .login-card {
    width: 100%;
    max-width: 400px;
  }
}

@media (max-width: 768px) {
  .login-brand {
    display: none;
  }
  .login-card {
    padding: 28px 24px;
  }
}
</style>
