<template>
  <span class="animated-number">{{ displayValue }}</span>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'

interface Props {
  value: number
  duration?: number
  decimals?: number
  prefix?: string
  suffix?: string
  startOnMount?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  duration: 800,
  decimals: 0,
  prefix: '',
  suffix: '',
  startOnMount: true
})

const displayValue = ref(props.prefix + (props.startOnMount ? '0' : formatNumber(props.value)) + props.suffix)

function formatNumber(val: number): string {
  return val.toFixed(props.decimals)
}

function animateToValue(target: number) {
  const start = parseFloat(displayValue.value.replace(props.prefix, '').replace(props.suffix, '')) || 0
  const diff = target - start
  const startTime = performance.now()

  function step(currentTime: number) {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / props.duration, 1)
    // easeOutQuart
    const eased = 1 - Math.pow(1 - progress, 4)
    const current = start + diff * eased
    displayValue.value = props.prefix + formatNumber(current) + props.suffix

    if (progress < 1) {
      requestAnimationFrame(step)
    } else {
      displayValue.value = props.prefix + formatNumber(target) + props.suffix
    }
  }

  requestAnimationFrame(step)
}

watch(() => props.value, (newVal) => {
  animateToValue(newVal)
}, { immediate: false })

onMounted(() => {
  if (props.startOnMount) {
    animateToValue(props.value)
  }
})
</script>

<style scoped>
.animated-number {
  font-variant-numeric: tabular-nums;
  font-feature-settings: 'tnum';
}
</style>
