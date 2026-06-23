<template>
  <div class="ai-assistant">
    <el-button class="ai-assistant__button" circle title="AI 助手" @click="visible = true">
      <el-icon :size="22"><ChatDotRound /></el-icon>
    </el-button>
    <AiChatPanel v-model="visible" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ChatDotRound } from '@element-plus/icons-vue'
import AiChatPanel from './AiChatPanel.vue'

const visible = ref(false)
</script>

<style scoped>
.ai-assistant {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 1100;
}

.ai-assistant__button {
  width: 52px;
  height: 52px;
  color: #fff !important;
  border: none !important;
  background: linear-gradient(135deg, var(--color-primary), #1677ff) !important;
  box-shadow: 0 10px 24px rgba(15, 159, 143, 0.28);
  animation: aiPulse 2.8s ease-in-out infinite;
}

.ai-assistant__button:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 28px rgba(15, 159, 143, 0.34);
}

@keyframes aiPulse {
  0%,
  100% {
    box-shadow: 0 10px 24px rgba(15, 159, 143, 0.28);
  }
  50% {
    box-shadow: 0 12px 32px rgba(22, 119, 255, 0.42);
  }
}

@media (max-width: 768px) {
  .ai-assistant {
    right: 18px;
    bottom: 18px;
  }

  .ai-assistant__button {
    width: 48px;
    height: 48px;
  }
}
</style>
