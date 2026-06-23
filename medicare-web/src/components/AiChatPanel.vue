<template>
  <el-drawer
    :model-value="modelValue"
    size="420px"
    direction="rtl"
    :with-header="false"
    custom-class="ai-chat-drawer"
    @close="$emit('update:modelValue', false)"
  >
    <div class="ai-chat">
      <header class="ai-chat__header">
        <div>
          <h3 class="ai-chat__title">MediCare AI 助手</h3>
        </div>
        <el-button circle text :icon="Close" @click="$emit('update:modelValue', false)" />
      </header>

      <div ref="messageListRef" class="ai-chat__messages">
        <div
          v-for="message in messages"
          :key="message.id"
          class="ai-chat__message"
          :class="`ai-chat__message--${message.role}`"
        >
          <div class="ai-chat__avatar">
            <el-icon>
              <component :is="message.role === 'user' ? 'User' : 'Service'" />
            </el-icon>
          </div>
          <div class="ai-chat__bubble">
            <div
              v-if="message.role === 'assistant' && message.content"
              class="ai-chat__content ai-chat__content--markdown"
              v-html="renderMarkdown(message.content)"
            />
            <div v-else-if="message.content" class="ai-chat__content">{{ message.content }}</div>
            <div v-else class="ai-chat__content ai-chat__content--thinking">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>正在思考...</span>
            </div>
            <div v-if="message.meta" class="ai-chat__meta">{{ message.meta }}</div>
          </div>
        </div>
      </div>

      <div class="ai-chat__quick">
        <el-button
          v-for="item in quickPrompts"
          :key="item"
          size="small"
          round
          @click="fillPrompt(item)"
        >
          {{ item }}
        </el-button>
      </div>

      <footer class="ai-chat__footer">
        <el-input
          v-model="input"
          type="textarea"
          :rows="3"
          resize="none"
          maxlength="1000"
          show-word-limit
          placeholder="请输入想咨询的问题，例如：如何完成挂号到取药流程？"
          @keydown.enter.exact.prevent="sendMessage"
        />
        <div class="ai-chat__actions">
          <span class="ai-chat__hint">Enter 发送，Shift + Enter 换行</span>
          <el-button type="primary" :loading="loading" :disabled="!input.trim() || isTyping" @click="sendMessage">
            发送
          </el-button>
        </div>
      </footer>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { useRoute } from 'vue-router'
import MarkdownIt from 'markdown-it'
import { Close, Loading, Service } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { createAiChat, createAiChatStream } from '../api/ai'

interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  meta?: string
}

defineProps<{
  modelValue: boolean
}>()

defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const route = useRoute()
const input = ref('')
const loading = ref(false)
const isTyping = ref(false)
const sessionId = ref(`chat-${Date.now()}`)
const messageListRef = ref<HTMLElement>()
const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true
})

// 流式输出状态
let textBuffer = ''      // 后端返回的文本缓冲区
let displayBuffer = ''   // 已显示的文本
let typingTimer: ReturnType<typeof setTimeout> | null = null
let currentAssistantMsg: ChatMessage | null = null
let isRevealing = false
let thinkTimer: ReturnType<typeof setTimeout> | null = null

const messages = ref<ChatMessage[]>([
  {
    id: 'welcome',
    role: 'assistant',
    content: '你好，我是 MediCare AI 助手。我可以帮你梳理系统操作流程和页面功能，随时为你解答疑问。'
  }
])

const quickPrompts = [
  '如何完成挂号到取药流程？',
  '药品库存预警如何处理？',
  '医生工作站怎么写病历？',
  '怎样为患者预约挂号？'
]

function fillPrompt(text: string) {
  input.value = text
}

/**
 * 自然流式输出 — 随机节奏"蹦"出文本，确保每次 Vue 都重新渲染
 */
function startTyping(assistantMessage: ChatMessage) {
  if (isRevealing) return
  currentAssistantMsg = assistantMessage
  isRevealing = true
  isTyping.value = true
  displayBuffer = ''

  async function revealNext() {
    if (!currentAssistantMsg || !isRevealing) return

    const targetLen = textBuffer.length
    const currentLen = displayBuffer.length

    if (currentLen < targetLen) {
      const remaining = targetLen - currentLen
      // 随机显示 5-30 个字符，忽多忽少，模拟自然"蹦"出
      const chunkSize = Math.max(5, Math.floor(Math.random() * Math.min(30, remaining)) + 1)
      displayBuffer = textBuffer.substring(0, currentLen + chunkSize)
      currentAssistantMsg.content = displayBuffer

      // 关键：强制 Vue 立即渲染，避免批量合并
      await nextTick()
      scrollToBottom()

      // 随机等待 30-100ms，忽快忽慢，有自然抖动
      const delay = 30 + Math.random() * 70
      typingTimer = setTimeout(() => revealNext(), Math.round(delay))
    } else if (loading.value) {
      // 后端还在输出，等更多内容
      typingTimer = setTimeout(() => revealNext(), 50)
    } else {
      // 全部完成，清理状态
      typingTimer = null
      isRevealing = false
      currentAssistantMsg = null
      isTyping.value = false
      textBuffer = ''
      displayBuffer = ''
    }
  }

  revealNext()
}

function stopTyping() {
  if (typingTimer) {
    clearTimeout(typingTimer)
    typingTimer = null
  }
  if (thinkTimer) {
    clearTimeout(thinkTimer)
    thinkTimer = null
  }
  if (currentAssistantMsg && textBuffer) {
    currentAssistantMsg.content = textBuffer
  }
  isRevealing = false
  currentAssistantMsg = null
  isTyping.value = false
  textBuffer = ''
  displayBuffer = ''
}

async function sendMessage() {
  const content = input.value.trim()
  if (!content || loading.value || isTyping.value) return

  // 添加用户消息
  messages.value.push({
    id: `${Date.now()}-user`,
    role: 'user',
    content
  })
  input.value = ''
  loading.value = true

  // 创建助理消息（初始空，显示loading动画）
  const assistantMessage: ChatMessage = {
    id: `${Date.now()}-assistant`,
    role: 'assistant',
    content: ''
  }
  messages.value.push(assistantMessage)
  textBuffer = ''
  displayBuffer = ''
  stopTyping()
  await scrollToBottom()

  const startTime = Date.now()

  try {
    const requestData = {
      message: content,
      sessionId: sessionId.value,
      context: {
        route: route.fullPath,
        routeName: String(route.name || '')
      }
    }

    // 固定 2 秒"思考"后启动流式显示
    thinkTimer = setTimeout(() => {
      if (!isRevealing && textBuffer.length > 0) {
        startTyping(assistantMessage)
      }
    }, 2000)

    await createAiChatStream(requestData, {
      onChunk: (text) => {
        textBuffer += text
        // 如果已经过了 2 秒（thinkTimer 已触发但未启动），现在启动
        if (!isRevealing && textBuffer.length > 0 && Date.now() - startTime >= 2000) {
          startTyping(assistantMessage)
        }
      },
      onDone: (meta) => {
        assistantMessage.meta = `${meta.provider} / ${meta.model}`
        loading.value = false
      },
      onError: (message) => {
        textBuffer = message || 'AI 调用失败，未生成回复。请查看页面提示或后端日志后重试。'
        loading.value = false
        if (!isRevealing) {
          startTyping(assistantMessage)
        }
      }
    })
  } catch (error) {
    if (!textBuffer) {
      try {
        const res = await createAiChat({
          message: content,
          sessionId: sessionId.value,
          context: {
            route: route.fullPath,
            routeName: String(route.name || '')
          }
        })
        textBuffer = res.data.answer
        assistantMessage.meta = `${res.data.provider} / ${res.data.model}`
        loading.value = false
        thinkTimer = setTimeout(() => startTyping(assistantMessage), 2000)
      } catch (fallbackError) {
        const msg = getErrorMessage(fallbackError)
        textBuffer = msg
        assistantMessage.meta = ''
        ElMessage.error(msg)
        loading.value = false
        thinkTimer = setTimeout(() => startTyping(assistantMessage), 2000)
      }
    } else {
      const msg = getErrorMessage(error) || '流式输出中断'
      textBuffer += `\n\n注意：${msg}`
      loading.value = false
      if (!isRevealing) {
        startTyping(assistantMessage)
      }
    }
  }

  // 等待流式输出完成
  await waitTypingComplete()
  loading.value = false
  await scrollToBottom()
}

function waitTypingComplete(): Promise<void> {
  return new Promise((resolve) => {
    const check = () => {
      if (!isRevealing) {
        textBuffer = ''
        displayBuffer = ''
        currentAssistantMsg = null
        isTyping.value = false
        resolve()
      } else {
        setTimeout(check, 80)
      }
    }
    check()
  })
}

function renderMarkdown(content: string) {
  return markdown.render(content)
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : 'AI 调用失败'
}

async function scrollToBottom() {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}
</script>

<style scoped>
.ai-chat {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg-page);
}

.ai-chat__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-lg);
  background: var(--bg-card);
  border-bottom: 1px solid var(--divider-color);
}

.ai-chat__title {
  margin: 0;
  font-size: var(--font-size-xl);
  color: var(--text-primary);
}

.ai-chat__messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-lg);
}

.ai-chat__message {
  display: flex;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
}

.ai-chat__message--user {
  flex-direction: row-reverse;
}

.ai-chat__avatar {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: var(--radius-circle);
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.ai-chat__message--user .ai-chat__avatar {
  color: var(--color-success);
  background: var(--color-success-light);
}

.ai-chat__bubble {
  max-width: 78%;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-card);
}

.ai-chat__message--user .ai-chat__bubble {
  color: #fff;
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.ai-chat__content {
  white-space: pre-wrap;
  line-height: 1.6;
  font-size: var(--font-size-sm);
}

.ai-chat__content--markdown {
  white-space: normal;
}

.ai-chat__content--markdown :deep(p) {
  margin: 0 0 8px;
}

.ai-chat__content--markdown :deep(p:last-child) {
  margin-bottom: 0;
}

.ai-chat__content--markdown :deep(ol),
.ai-chat__content--markdown :deep(ul) {
  margin: 8px 0;
  padding-left: 20px;
}

.ai-chat__content--markdown :deep(li + li) {
  margin-top: 4px;
}

.ai-chat__content--markdown :deep(strong) {
  font-weight: 700;
  color: var(--text-primary);
}

.ai-chat__content--markdown :deep(code) {
  padding: 2px 5px;
  border-radius: var(--radius-sm);
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.ai-chat__content--markdown :deep(blockquote) {
  margin: 8px 0;
  padding-left: 10px;
  color: var(--text-secondary);
  border-left: 3px solid var(--border-color);
}

.ai-chat__content--thinking {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  color: var(--text-secondary);
}

.ai-chat__meta {
  margin-top: 8px;
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}

.ai-chat__bubble--loading {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  color: var(--text-secondary);
}

.ai-chat__quick {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 0 var(--space-lg) var(--space-md);
}

.ai-chat__quick .el-button {
  justify-content: center;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ai-chat__footer {
  padding: var(--space-lg);
  background: var(--bg-card);
  border-top: 1px solid var(--divider-color);
}

.ai-chat__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  margin-top: var(--space-sm);
}

.ai-chat__hint {
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}

@media (max-width: 768px) {
  .ai-chat__bubble {
    max-width: 84%;
  }

  .ai-chat__hint {
    display: none;
  }

  .ai-chat__quick {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
