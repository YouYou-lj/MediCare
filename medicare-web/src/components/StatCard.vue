<template>
  <div class="stat-card" :class="{ 'stat-card-clickable': clickable, 'stat-card-alert': variant === 'alert' }">
    <div class="stat-icon" :style="iconStyle">
      <el-icon :size="28">
        <component :is="icon" />
      </el-icon>
    </div>
    <div class="stat-content">
      <div class="stat-label">{{ label }}</div>
      <div class="stat-value-wrapper">
        <AnimatedNumber :value="value" class="stat-value" />
        <span v-if="suffix" class="stat-suffix">{{ suffix }}</span>
      </div>
      <div v-if="trend !== undefined" class="stat-trend" :class="trend > 0 ? 'trend-up' : trend < 0 ? 'trend-down' : 'trend-flat'">
        <el-icon :size="12">
          <component :is="trend > 0 ? 'ArrowUp' : trend < 0 ? 'ArrowDown' : 'Minus'" />
        </el-icon>
        <span>{{ Math.abs(trend) }}%</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Component } from 'vue'
import AnimatedNumber from './AnimatedNumber.vue'

interface Props {
  icon: Component | string
  label: string
  value: number
  suffix?: string
  trend?: number
  color?: string
  clickable?: boolean
  variant?: 'default' | 'alert'
}

const props = withDefaults(defineProps<Props>(), {
  suffix: '',
  color: '#0F9F8F',
  clickable: false,
  variant: 'default'
})

const iconStyle = computed(() => ({
  backgroundColor: props.color + '15',
  color: props.color
}))
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
  padding: var(--space-xl);
  background: var(--bg-card);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  transition: all 0.2s ease;
  border: 1px solid transparent;
}
.stat-card:hover {
  box-shadow: var(--shadow-card-hover);
}
.stat-card-clickable {
  cursor: pointer;
}
.stat-card-clickable:hover {
  transform: translateY(-2px);
}
.stat-card-alert {
  border-color: var(--color-warning);
  background: linear-gradient(135deg, var(--bg-card) 0%, var(--color-warning-light) 100%);
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: var(--radius-circle);
  flex-shrink: 0;
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  line-height: 1.4;
}

.stat-value-wrapper {
  display: flex;
  align-items: baseline;
  gap: var(--space-xs);
}

.stat-value {
  font-size: var(--font-size-4xl);
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-suffix {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.stat-trend {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: var(--font-size-xs);
  font-weight: 500;
  padding: 2px 6px;
  border-radius: 10px;
}
.trend-up {
  color: var(--color-success);
  background: var(--color-success-light);
}
.trend-down {
  color: var(--color-danger);
  background: var(--color-danger-light);
}
.trend-flat {
  color: var(--text-muted);
  background: var(--bg-hover);
}

@media (max-width: 768px) {
  .stat-card {
    padding: var(--space-md);
  }
  .stat-icon {
    width: 44px;
    height: 44px;
  }
  .stat-value {
    font-size: var(--font-size-3xl);
  }
}
</style>
