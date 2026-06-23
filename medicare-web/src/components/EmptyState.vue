<template>
  <div class="empty-state">
    <div class="empty-icon">
      <el-icon :size="64" color="var(--text-placeholder)">
        <component :is="icon" />
      </el-icon>
    </div>
    <h3 class="empty-title">{{ title }}</h3>
    <p v-if="description" class="empty-desc">{{ description }}</p>
    <div v-if="$slots.action" class="empty-action">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

interface Props {
  icon?: Component | string
  title?: string
  description?: string
}

withDefaults(defineProps<Props>(), {
  icon: 'Inbox',
  title: '暂无数据',
  description: ''
})
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-3xl) var(--space-xl);
  text-align: center;
  width: 100%;
}

.empty-icon {
  margin-bottom: var(--space-lg);
  opacity: 0.6;
}

.empty-title {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--text-secondary);
  margin: 0 0 var(--space-xs);
  line-height: 1.4;
}

.empty-desc {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  margin: 0 0 var(--space-lg);
  line-height: 1.5;
  max-width: 400px;
}

.empty-action {
  margin-top: var(--space-sm);
}
</style>