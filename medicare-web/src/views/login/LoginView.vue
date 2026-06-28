<template>
  <div class="login-screen">
    <div class="login-screen__backdrop" aria-hidden="true" />
    <div class="login-screen__grid" aria-hidden="true" />
    <canvas ref="particleCanvas" class="login-screen__particles" aria-hidden="true" />

    <main class="login-screen__content">
      <section class="login-screen__brand" aria-labelledby="login-brand-title">
        <div class="login-screen__brand-logo">
          <el-icon :size="50"><FirstAidKit /></el-icon>
          <span>MediCare</span>
        </div>
        <h1 id="login-brand-title" class="login-screen__brand-title">智慧医疗门诊管理系统</h1>
        <p class="login-screen__brand-description">
          集患者管理、挂号预约、医生工作站、<br />
          病历管理、药品库存、处方管理于一体<br />
          的现代化医疗信息化平台。
        </p>
        <div class="login-screen__features">
          <div class="login-screen__feature">
            <el-icon><Check /></el-icon>
            <span>患者全生命周期管理</span>
          </div>
          <div class="login-screen__feature">
            <el-icon><Check /></el-icon>
            <span>智能挂号与排班系统</span>
          </div>
          <div class="login-screen__feature">
            <el-icon><Check /></el-icon>
            <span>电子病历与处方管理</span>
          </div>
          <div class="login-screen__feature">
            <el-icon><Check /></el-icon>
            <span>AI 智能医疗辅助</span>
          </div>
        </div>
      </section>

      <section
        class="login-screen__auth"
        :class="{ 'login-screen__auth--shake': isShaking }"
        aria-labelledby="login-form-title"
      >
        <header class="login-screen__header">
          <h2 id="login-form-title" class="login-screen__title">欢迎登录</h2>
          <p class="login-screen__subtitle">请使用您的账号密码登录系统</p>
        </header>

        <el-form
          ref="formRef"
          class="login-screen__form"
          :model="form"
          :rules="rules"
          label-width="0"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              class="login-screen__input"
              name="username"
              autocomplete="username"
              placeholder="请输入用户名"
              size="large"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              class="login-screen__input"
              name="password"
              autocomplete="current-password"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="captcha">
            <div class="login-screen__captcha">
              <button
                type="button"
                class="login-screen__captcha-challenge"
                aria-label="刷新算术验证码"
                @click="refreshCaptcha"
              >
                <span>{{ captchaQuestion }}</span>
                <el-icon><Refresh /></el-icon>
              </button>
              <el-input
                v-model="form.captcha"
                class="login-screen__captcha-input"
                name="captcha"
                inputmode="numeric"
                autocomplete="off"
                placeholder="请输入计算结果"
                size="large"
              >
                <template #prefix>
                  <el-icon><CircleCheck /></el-icon>
                </template>
              </el-input>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              native-type="submit"
              :loading="isLoading"
              class="login-screen__submit"
            >
              <el-icon v-if="!isLoading"><Right /></el-icon>
              <span>登 录</span>
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-screen__mascot-track" aria-hidden="true">
          <div
            v-for="(_, index) in catPositions"
            :key="index"
            class="login-screen__mascot-slot"
            @mouseenter="handleCatHover(index)"
          >
            <div
              v-if="catPosition === index"
              class="login-screen__mascot"
              :class="{ 'login-screen__mascot--jumping': isCatJumping }"
            >
              <img :src="companionCat" alt="" draggable="false" />
              <span v-if="isCatSleeping" class="login-screen__mascot-sleep">Zz</span>
            </div>
          </div>
        </div>
        <p class="login-screen__mascot-hint">{{ catHint }}</p>
      </section>
    </main>

    <footer class="login-screen__footer">
      <p>MediCare 智慧医疗门诊管理系统 v1.0</p>
      <p>本项目由沈院士特别赞助开发</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import companionCat from '../../assets/login/companion-cat.png'
import { login } from '../../api/auth'
import { useUserStore } from '../../stores/user'

interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  size: number
  type: 'dot' | 'hexagon'
  baseOpacity: number
  opacity: number
  color: string
  rotation: number
  rotationSpeed: number
  twinklePhase: number
  twinkleSpeed: number
  isRibbon: boolean
}

const PARTICLE_COUNT = 96
const RIBBON_PARTICLE_RATIO = 0.58
const CONNECTION_DISTANCE = 104
const FUSION_DISTANCE = 28
const INITIAL_CAT_MOVE_DELAY = 30000
const PARTICLE_COLORS = ['15, 159, 143', '44, 110, 232', '155, 217, 209']

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const particleCanvas = ref<HTMLCanvasElement | null>(null)
const isLoading = ref(false)
const isShaking = ref(false)

/**
 * 计算参考图中 S 形分子光带在指定纵坐标上的中心位置。
 */
function getRibbonX(y: number, width: number, height: number) {
  const progress = height > 0 ? y / height : 0.5
  return width * (0.52 + Math.sin((0.5 - progress) * Math.PI) * 0.2)
}

/**
 * 初始化背景粒子；多数粒子沿中央分子光带分布，其余粒子形成环境微光。
 */
function createParticles(width: number, height: number): Particle[] {
  return Array.from({ length: PARTICLE_COUNT }, (_, index) => {
    const isRibbon = index < PARTICLE_COUNT * RIBBON_PARTICLE_RATIO
    const y = Math.random() * height
    const ribbonX = getRibbonX(y, width, height)
    const color = PARTICLE_COLORS[Math.floor(Math.random() * PARTICLE_COLORS.length)] ?? PARTICLE_COLORS[0]

    return {
      x: isRibbon ? ribbonX + (Math.random() - 0.5) * Math.min(130, width * 0.1) : Math.random() * width,
      y,
      vx: (Math.random() - 0.5) * (isRibbon ? 0.18 : 0.32),
      vy: (Math.random() - 0.5) * 0.24 - 0.035,
      size: 1.1 + Math.random() * (isRibbon ? 3.4 : 2.2),
      type: Math.random() > 0.82 ? 'hexagon' : 'dot',
      baseOpacity: (isRibbon ? 0.22 : 0.1) + Math.random() * (isRibbon ? 0.34 : 0.18),
      opacity: 0,
      color,
      rotation: Math.random() * Math.PI * 2,
      rotationSpeed: (Math.random() - 0.5) * 0.008,
      twinklePhase: Math.random() * Math.PI * 2,
      twinkleSpeed: 0.00055 + Math.random() * 0.00065,
      isRibbon,
    }
  })
}

function drawHexagon(ctx: CanvasRenderingContext2D, particle: Particle) {
  ctx.save()
  ctx.translate(particle.x, particle.y)
  ctx.rotate(particle.rotation)
  ctx.beginPath()
  for (let side = 0; side < 6; side += 1) {
    const angle = (Math.PI / 3) * side
    const x = Math.cos(angle) * particle.size * 2.1
    const y = Math.sin(angle) * particle.size * 2.1
    if (side === 0) ctx.moveTo(x, y)
    else ctx.lineTo(x, y)
  }
  ctx.closePath()
  ctx.strokeStyle = `rgba(${particle.color}, ${particle.opacity})`
  ctx.lineWidth = 0.8
  ctx.stroke()
  ctx.restore()
}

function drawDot(ctx: CanvasRenderingContext2D, particle: Particle) {
  const radius = particle.size * (particle.isRibbon ? 1.18 : 1)
  const glow = ctx.createRadialGradient(particle.x, particle.y, 0, particle.x, particle.y, radius * 3.2)
  glow.addColorStop(0, `rgba(${particle.color}, ${particle.opacity})`)
  glow.addColorStop(0.32, `rgba(${particle.color}, ${particle.opacity * 0.46})`)
  glow.addColorStop(1, `rgba(${particle.color}, 0)`)
  ctx.fillStyle = glow
  ctx.beginPath()
  ctx.arc(particle.x, particle.y, radius * 3.2, 0, Math.PI * 2)
  ctx.fill()
}

/**
 * 启动低亮分子粒子动画，并在系统要求减少动态效果时退化为静态帧。
 */
function startParticleAnimation() {
  const canvas = particleCanvas.value
  const ctx = canvas?.getContext('2d')
  if (!canvas || !ctx) return

  const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  let width = window.innerWidth
  let height = window.innerHeight
  let particles: Particle[] = []
  let animationId = 0

  function resize() {
    const devicePixelRatio = Math.min(window.devicePixelRatio || 1, 2)
    width = window.innerWidth
    height = window.innerHeight
    canvas!.width = Math.round(width * devicePixelRatio)
    canvas!.height = Math.round(height * devicePixelRatio)
    canvas!.style.width = `${width}px`
    canvas!.style.height = `${height}px`
    ctx!.setTransform(devicePixelRatio, 0, 0, devicePixelRatio, 0, 0)
    particles = createParticles(width, height)
  }

  function renderFrame(time: number, shouldMove: boolean) {
    ctx!.clearRect(0, 0, width, height)

    particles.forEach((particle) => {
      if (shouldMove) {
        particle.x += particle.vx
        particle.y += particle.vy
        particle.rotation += particle.rotationSpeed
        if (particle.isRibbon) {
          const targetX = getRibbonX(particle.y, width, height)
          particle.x += (targetX - particle.x) * 0.0028
        }
      }

      if (particle.y < -24) particle.y = height + 24
      if (particle.y > height + 24) particle.y = -24
      if (particle.x < -24) particle.x = width + 24
      if (particle.x > width + 24) particle.x = -24

      const twinkle = 0.7 + Math.sin(time * particle.twinkleSpeed + particle.twinklePhase) * 0.3
      particle.opacity = particle.baseOpacity * twinkle
    })

    for (let firstIndex = 0; firstIndex < particles.length; firstIndex += 1) {
      const first = particles[firstIndex]
      if (!first) continue
      for (let secondIndex = firstIndex + 1; secondIndex < particles.length; secondIndex += 1) {
        const second = particles[secondIndex]
        if (!second || (first.isRibbon !== second.isRibbon && !first.isRibbon)) continue
        const deltaX = first.x - second.x
        const deltaY = first.y - second.y
        const distance = Math.hypot(deltaX, deltaY)

        if (distance > 0 && distance < CONNECTION_DISTANCE) {
          const connectionOpacity = (1 - distance / CONNECTION_DISTANCE) * (first.isRibbon ? 0.12 : 0.045)
          ctx!.strokeStyle = `rgba(${first.color}, ${connectionOpacity})`
          ctx!.lineWidth = first.isRibbon ? 0.8 : 0.55
          ctx!.beginPath()
          ctx!.moveTo(first.x, first.y)
          ctx!.lineTo(second.x, second.y)
          ctx!.stroke()
        }

        if (first.isRibbon && second.isRibbon && distance > 0 && distance < FUSION_DISTANCE) {
          const centerX = (first.x + second.x) / 2
          const centerY = (first.y + second.y) / 2
          const fusionOpacity = (1 - distance / FUSION_DISTANCE) * 0.12
          const fusion = ctx!.createRadialGradient(centerX, centerY, 0, centerX, centerY, FUSION_DISTANCE)
          fusion.addColorStop(0, `rgba(155, 217, 209, ${fusionOpacity})`)
          fusion.addColorStop(1, 'rgba(155, 217, 209, 0)')
          ctx!.fillStyle = fusion
          ctx!.beginPath()
          ctx!.arc(centerX, centerY, FUSION_DISTANCE, 0, Math.PI * 2)
          ctx!.fill()
        }
      }
    }

    particles.forEach((particle) => {
      if (particle.type === 'hexagon') drawHexagon(ctx!, particle)
      else drawDot(ctx!, particle)
    })
  }

  function animate(time: number) {
    renderFrame(time, true)
    animationId = window.requestAnimationFrame(animate)
  }

  resize()
  window.addEventListener('resize', resize)
  if (prefersReducedMotion) renderFrame(0, false)
  else animationId = window.requestAnimationFrame(animate)

  return () => {
    window.removeEventListener('resize', resize)
    if (animationId) window.cancelAnimationFrame(animationId)
  }
}

const form = reactive({
  username: '',
  password: '',
  captcha: '',
})

const captchaAnswer = ref(0)
const captchaQuestion = ref('')

function refreshCaptcha() {
  const first = 3 + Math.floor(Math.random() * 7)
  const second = 2 + Math.floor(Math.random() * 8)
  captchaAnswer.value = first + second
  captchaQuestion.value = `${first} + ${second} = ?`
  form.captcha = ''
}

function validateCaptcha(_rule: unknown, value: string, callback: (error?: Error) => void) {
  if (!value) {
    callback(new Error('请输入防人机校验结果'))
    return
  }
  if (Number(value) !== captchaAnswer.value) {
    callback(new Error('校验结果不正确，请重新计算'))
    refreshCaptcha()
    return
  }
  callback()
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: ['blur', 'change'] }],
  password: [{ required: true, message: '请输入密码', trigger: ['blur', 'change'] }],
  captcha: [{ validator: validateCaptcha, trigger: 'blur' }],
}

const catPositions = ['左', '中', '右']
const catPosition = ref(1)
const isCatJumping = ref(false)
const isCatSleeping = ref(false)
const catHint = ref('小猫咪正在陪伴你登录...')
let autoMoveTimer: ReturnType<typeof setTimeout> | null = null
let sleepTimer: ReturnType<typeof setTimeout> | null = null

function moveCatTo(newPosition: number) {
  if (newPosition === catPosition.value) return
  isCatJumping.value = true
  isCatSleeping.value = false
  catHint.value = `小猫咪跳到了${catPositions[newPosition]}边！`
  window.setTimeout(() => {
    catPosition.value = newPosition
    isCatJumping.value = false
    if (sleepTimer) window.clearTimeout(sleepTimer)
    sleepTimer = window.setTimeout(() => {
      isCatSleeping.value = true
      catHint.value = '小猫咪在打盹 Zzz...'
    }, 2000)
  }, 200)
}

function handleCatHover(hoveredIndex: number) {
  if (hoveredIndex !== catPosition.value) return
  const directions: number[] = []
  if (catPosition.value > 0) directions.push(-1)
  if (catPosition.value < 2) directions.push(1)
  const direction = directions[Math.floor(Math.random() * directions.length)]
  if (direction === undefined) return
  moveCatTo(catPosition.value + direction)
}

function moveCatAutomatically() {
  const candidates = [0, 1, 2].filter(position => position !== catPosition.value)
  const newPosition = candidates[Math.floor(Math.random() * candidates.length)]
  if (newPosition !== undefined) moveCatTo(newPosition)
}

function scheduleCatMovement(delay = 5000 + Math.floor(Math.random() * 3000)) {
  autoMoveTimer = window.setTimeout(() => {
    moveCatAutomatically()
    scheduleCatMovement()
  }, delay)
}

let stopParticles: (() => void) | undefined

onMounted(() => {
  refreshCaptcha()
  scheduleCatMovement(INITIAL_CAT_MOVE_DELAY)
  stopParticles = startParticleAnimation()
})

onUnmounted(() => {
  if (autoMoveTimer) window.clearTimeout(autoMoveTimer)
  if (sleepTimer) window.clearTimeout(sleepTimer)
  stopParticles?.()
})

function triggerShake() {
  isShaking.value = true
  window.setTimeout(() => {
    isShaking.value = false
  }, 500)
}

async function handleLogin() {
  if (isLoading.value) return

  const isValid = await formRef.value?.validate().catch(() => false)
  if (!isValid) {
    triggerShake()
    return
  }

  isLoading.value = true
  try {
    const response = await login(form)
    userStore.setUser(response.data)
    ElMessage.success('登录成功')
    router.push('/')
  } catch {
    triggerShake()
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.login-screen {
  --login-deep-teal: #0b5e61;
  --login-deep-teal-strong: #063f49;
  --login-teal: #0f9f8f;
  --login-aqua: #9bd9d1;
  --login-blue: #2c6ee8;
  --login-blue-soft: #66a6ed;
  --login-porcelain: #f6faf9;
  --login-porcelain-glass: rgba(246, 250, 249, 0.9);
  --login-ink: #142738;
  --login-muted: #71879a;
  --login-border: rgba(103, 145, 181, 0.28);
  --login-grid-dark: rgba(155, 217, 209, 0.065);
  --login-grid-light: rgba(44, 110, 232, 0.045);
  --login-focus-ring: rgba(44, 110, 232, 0.14);
  --login-shadow: rgba(12, 56, 73, 0.12);
  --login-white: #ffffff;

  position: relative;
  display: grid;
  min-height: 100vh;
  min-height: 100dvh;
  overflow: hidden;
  color: var(--login-ink);
  background: var(--login-porcelain);
  isolation: isolate;
}

.login-screen__backdrop,
.login-screen__grid,
.login-screen__particles {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.login-screen__backdrop {
  z-index: -3;
  background-image: url('../../assets/login/molecular-atrium-background.png');
  background-position: center;
  background-size: cover;
}

.login-screen__grid {
  z-index: -2;
  overflow: hidden;
  opacity: 0.58;
  background-image:
    linear-gradient(var(--login-grid-dark) 1px, transparent 1px),
    linear-gradient(90deg, var(--login-grid-light) 1px, transparent 1px);
  background-size: 44px 44px;
}

.login-screen__grid::after {
  position: absolute;
  inset: -45%;
  content: '';
  background: radial-gradient(
    circle at 52% 48%,
    rgba(155, 217, 209, 0.2) 0%,
    rgba(44, 110, 232, 0.075) 24%,
    transparent 53%
  );
  animation: login-grid-breathe 12s ease-in-out infinite alternate;
}

.login-screen__particles {
  z-index: -1;
}

.login-screen__content {
  display: grid;
  grid-template-columns: minmax(0, 470px) minmax(0, 436px);
  align-items: center;
  gap: 241px;
  width: min(1200px, calc(100% - 72px));
  min-height: calc(100vh - 132px);
  min-height: calc(100dvh - 132px);
  margin: 0 auto;
  padding: 36px 0 96px;
}

.login-screen__brand {
  position: relative;
  top: -44px;
  max-width: 470px;
  color: var(--login-white);
  text-shadow: 0 1px 2px var(--login-shadow);
}

.login-screen__brand-logo {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 28px;
  color: #2bd5c4;
  font-size: clamp(38px, 3.4vw, 50px);
  font-weight: 700;
  letter-spacing: 0.02em;
  line-height: 1;
}

.login-screen__brand-logo .el-icon {
  filter: drop-shadow(0 0 14px rgba(43, 213, 196, 0.16));
}

.login-screen__brand-title {
  margin: 0 0 22px;
  color: var(--login-white);
  font-size: clamp(28px, 2.5vw, 36px);
  font-weight: 680;
  letter-spacing: 0.015em;
  line-height: 1.28;
}

.login-screen__brand-description {
  margin: 0 0 34px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 16px;
  line-height: 1.9;
}

.login-screen__features {
  display: grid;
  gap: 14px;
}

.login-screen__feature {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.88);
  font-size: 15px;
}

.login-screen__feature .el-icon {
  color: #2bd5c4;
  font-size: 18px;
}

.login-screen__auth {
  position: relative;
  top: 82px;
  width: min(100%, 436px);
  justify-self: start;
  padding: 16px 0;
}

.login-screen__header {
  margin-bottom: 32px;
  text-align: center;
}

.login-screen__title {
  margin: 0 0 10px;
  color: var(--login-ink);
  font-size: 28px;
  font-weight: 680;
  letter-spacing: 0.02em;
}

.login-screen__subtitle {
  margin: 0;
  color: var(--login-muted);
  font-size: 14px;
}

.login-screen__form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-screen__form :deep(.el-input__wrapper) {
  min-height: 50px;
  padding-inline: 16px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 0 0 1px var(--login-border) inset;
  transition: background-color 0.2s ease, box-shadow 0.2s ease;
}

.login-screen__form :deep(.el-input__wrapper:hover) {
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 0 0 1px rgba(44, 110, 232, 0.38) inset;
}

.login-screen__form :deep(.el-input__wrapper.is-focus) {
  background: var(--login-white);
  box-shadow: 0 0 0 1px var(--login-blue) inset, 0 0 0 4px var(--login-focus-ring);
}

.login-screen__form :deep(.el-input__prefix) {
  color: #5d7b99;
  font-size: 16px;
}

.login-screen__captcha {
  display: grid;
  grid-template-columns: 146px minmax(0, 1fr);
  gap: 12px;
  width: 100%;
}

.login-screen__captcha-challenge {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  min-height: 50px;
  padding: 0 14px;
  border: 1px solid var(--login-border);
  border-radius: 6px;
  color: var(--login-ink);
  background: rgba(255, 255, 255, 0.7);
  font: inherit;
  font-size: 17px;
  font-weight: 700;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease, color 0.2s ease;
}

.login-screen__captcha-challenge:hover,
.login-screen__captcha-challenge:focus-visible {
  border-color: var(--login-blue);
  color: var(--login-blue);
  background: var(--login-white);
  outline: 4px solid var(--login-focus-ring);
  outline-offset: 0;
}

.login-screen__submit {
  width: 100%;
  height: 54px;
  border: 0;
  border-radius: 6px;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.28em;
  background: linear-gradient(105deg, #1858d6 0%, var(--login-blue) 58%, var(--login-blue-soft) 100%) !important;
  box-shadow: 0 10px 24px rgba(44, 110, 232, 0.18);
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.login-screen__submit:hover,
.login-screen__submit:focus-visible {
  transform: translateY(-1px);
  filter: saturate(1.08);
  box-shadow: 0 14px 28px rgba(44, 110, 232, 0.24);
}

.login-screen__mascot-track {
  position: relative;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  align-items: end;
  min-height: 150px;
  margin-top: 4px;
  border: 1px dashed rgba(103, 145, 181, 0.28);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.3);
  overflow: hidden;
}

.login-screen__mascot-slot {
  display: grid;
  min-width: 0;
  min-height: 148px;
  place-items: end center;
}

.login-screen__mascot {
  position: relative;
  width: 128px;
  height: 138px;
  transform-origin: 50% 100%;
  transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.login-screen__mascot img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  user-select: none;
}

.login-screen__mascot--jumping {
  animation: login-cat-jump 0.42s ease;
}

.login-screen__mascot-sleep {
  position: absolute;
  top: 6px;
  right: -7px;
  color: var(--login-blue);
  font-size: 13px;
  font-weight: 700;
  animation: login-cat-sleep 1.8s ease-in-out infinite;
}

.login-screen__mascot-hint {
  min-height: 18px;
  margin: 10px 0 0;
  color: var(--login-muted);
  font-size: 12px;
  text-align: center;
}

.login-screen__footer {
  position: absolute;
  bottom: 18px;
  left: 50%;
  z-index: 1;
  width: min(480px, calc(100% - 40px));
  color: #6e8295;
  text-align: center;
  transform: translateX(-50%);
}

.login-screen__footer p {
  margin: 3px 0;
  font-size: 13px;
}

.login-screen__auth--shake {
  animation: login-form-shake 0.5s ease-in-out;
}

@keyframes login-grid-breathe {
  0% {
    opacity: 0.38;
    transform: translate(-9%, -5%) scale(0.92);
  }
  50% {
    opacity: 0.7;
  }
  100% {
    opacity: 0.48;
    transform: translate(11%, 8%) scale(1.08);
  }
}

@keyframes login-cat-jump {
  0%,
  100% {
    transform: translateY(0) scale(1);
  }
  45% {
    transform: translateY(-14px) scale(1.04);
  }
}

@keyframes login-cat-sleep {
  0% {
    opacity: 0;
    transform: translate(0, 0) scale(0.85);
  }
  45% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: translate(10px, -18px) scale(1.12);
  }
}

@keyframes login-form-shake {
  0%,
  100% {
    transform: translateX(0);
  }
  20%,
  60% {
    transform: translateX(-7px);
  }
  40%,
  80% {
    transform: translateX(7px);
  }
}

@media (max-width: 1120px) {
  .login-screen__content {
    grid-template-columns: minmax(330px, 0.78fr) minmax(400px, 0.9fr);
    gap: 72px;
    width: min(980px, calc(100% - 56px));
  }

  .login-screen__brand-title {
    font-size: 29px;
  }

  .login-screen__brand,
  .login-screen__auth {
    top: 0;
  }
}

@media (max-width: 900px) {
  .login-screen {
    overflow: auto;
  }

  .login-screen__backdrop {
    background-position: 61% center;
  }

  .login-screen__backdrop::after {
    position: absolute;
    inset: 0;
    content: '';
    background: rgba(246, 250, 249, 0.74);
  }

  .login-screen__content {
    grid-template-columns: minmax(0, 1fr);
    width: min(520px, calc(100% - 40px));
    min-height: calc(100vh - 48px);
    min-height: calc(100dvh - 48px);
    padding: 36px 0 116px;
  }

  .login-screen__brand {
    display: none;
  }

  .login-screen__auth {
    width: 100%;
    justify-self: center;
    padding: 34px 34px 28px;
    border: 1px solid rgba(255, 255, 255, 0.72);
    border-radius: 12px;
    background: var(--login-porcelain-glass);
    box-shadow: 0 24px 60px var(--login-shadow);
    backdrop-filter: blur(16px);
  }
}

@media (max-width: 520px) {
  .login-screen__content {
    width: min(100% - 24px, 440px);
    padding-top: 22px;
  }

  .login-screen__auth {
    padding: 28px 20px 22px;
  }

  .login-screen__header {
    margin-bottom: 28px;
  }

  .login-screen__title {
    font-size: 24px;
  }

  .login-screen__captcha {
    grid-template-columns: 128px minmax(0, 1fr);
    gap: 8px;
  }

  .login-screen__captcha-challenge {
    padding-inline: 8px;
    font-size: 15px;
  }

  .login-screen__mascot-track {
    min-height: 132px;
  }

  .login-screen__mascot-slot {
    min-height: 130px;
  }

  .login-screen__mascot {
    width: 84px;
    height: 98px;
  }

  .login-screen__footer {
    position: absolute;
    bottom: 14px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-screen__grid::after,
  .login-screen__mascot--jumping,
  .login-screen__mascot-sleep,
  .login-screen__auth--shake {
    animation: none;
  }

  .login-screen__submit {
    transition: none;
  }
}
</style>
