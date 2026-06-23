<template>
  <div class="status-tag" :class="statusClass">
    <span class="status-dot" />
    <span class="status-text">{{ label }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type StatusType = 'success' | 'warning' | 'danger' | 'info' | 'primary' | 'default'

interface Props {
  type: StatusType
  label: string
}

const props = defineProps<Props>()

const statusClass = computed(() => `status-${props.type}`)
</script>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  border-radius: var(--radius-tag);
  font-size: var(--font-size-xs);
  font-weight: 500;
  line-height: 1.5;
  border: 1px solid transparent;
  white-space: nowrap;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-primary {
  background: var(--color-primary-light);
  color: var(--color-primary);
  border-color: var(--color-primary);
}
.status-primary .status-dot {
  background: var(--color-primary);
}

.status-success {
  background: var(--color-success-light);
  color: var(--color-success);
  border-color: var(--color-success);
}
.status-success .status-dot {
  background: var(--color-success);
}

.status-warning {
  background: var(--color-warning-light);
  color: var(--color-warning);
  border-color: var(--color-warning);
}
.status-warning .status-dot {
  background: var(--color-warning);
}

.status-danger {
  background: var(--color-danger-light);
  color: var(--color-danger);
  border-color: var(--color-danger);
}
.status-danger .status-dot {
  background: var(--color-danger);
}

.status-info {
  background: var(--color-info-light);
  color: var(--color-info);
  border-color: var(--color-info);
}
.status-info .status-dot {
  background: var(--color-info);
}

.status-default {
  background: var(--bg-hover);
  color: var(--text-muted);
  border-color: var(--border-color);
}
.status-default .status-dot {
  background: var(--text-muted);
}
</style>
