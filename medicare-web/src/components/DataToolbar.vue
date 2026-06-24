<template>
  <div class="data-toolbar">
    <div class="toolbar-left">
      <el-input
        v-if="searchPlaceholder"
        v-model="searchValue"
        :placeholder="searchPlaceholder"
        clearable
        class="toolbar-search"
        @input="handleSearch"
        @clear="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <slot name="filters" />
    </div>
    <div class="toolbar-right">
      <el-button v-if="showRefresh" :icon="Refresh" @click="$emit('refresh')">
        刷新
      </el-button>
      <el-button v-if="showAdd" type="primary" :icon="Plus" @click="$emit('add')">
        {{ addLabel }}
      </el-button>
      <slot name="extra" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'

interface Props {
  searchPlaceholder?: string
  searchModelValue?: string
  showRefresh?: boolean
  showAdd?: boolean
  addLabel?: string
}

const props = withDefaults(defineProps<Props>(), {
  searchPlaceholder: '',
  searchModelValue: '',
  showRefresh: false,
  showAdd: false,
  addLabel: '新增'
})

const emit = defineEmits<{
  (e: 'update:searchModelValue', value: string): void
  (e: 'search', value: string): void
  (e: 'refresh'): void
  (e: 'add'): void
}>()

const searchValue = ref(props.searchModelValue)

watch(() => props.searchModelValue, (val) => {
  searchValue.value = val
})

function handleSearch() {
  emit('update:searchModelValue', searchValue.value)
  emit('search', searchValue.value)
}
</script>

<style scoped>
.data-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: nowrap;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
  padding: var(--space-md) var(--space-lg);
  background: var(--bg-toolbar);
  border-radius: var(--radius-table);
  border: 1px solid var(--border-light);
  overflow: hidden;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex-wrap: nowrap;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  flex-shrink: 0;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.toolbar-search {
  width: 240px;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .data-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .toolbar-left,
  .toolbar-right {
    width: 100%;
  }
  .toolbar-search {
    width: 100%;
    flex-shrink: 1;
  }
  .toolbar-right {
    justify-content: flex-end;
  }
}
</style>
