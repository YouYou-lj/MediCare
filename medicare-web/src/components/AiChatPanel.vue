<template>
  <el-drawer
    :model-value="modelValue"
    size="760px"
    direction="rtl"
    :with-header="false"
    custom-class="ai-chat-drawer"
    @close="$emit('update:modelValue', false)"
  >
    <div class="ai-chat-wrapper">
      <!-- 左侧：会话历史 + 检索来源 侧边栏 -->
      <div class="side-panel" :class="{ collapsed: !sidePanelOpen }">
        <div class="side-panel__toggle" @click="sidePanelOpen = !sidePanelOpen">
          <el-icon><ArrowRight v-if="!sidePanelOpen" /><ArrowLeft v-else /></el-icon>
        </div>

        <!-- 展开状态：标签页切换 -->
        <div v-if="sidePanelOpen" class="side-panel__content">
          <el-tabs v-model="activeSideTab" class="side-tabs">
            <el-tab-pane label="会话" name="sessions">
              <div class="side-panel__body">
                <el-button class="side-panel__new" type="primary" text :icon="Plus" @click="createNewSession">
                  新会话
                </el-button>
                <div v-if="sessions.length === 0" class="side-panel__empty">暂无历史会话</div>
                <div
                  v-for="s in sessions"
                  :key="s.id"
                  class="session-item"
                  :class="{ active: currentSession?.id === s.id }"
                  @click="selectSession(s)"
                >
                  <el-icon class="session-item__icon"><ChatLineRound /></el-icon>
                  <div class="session-item__info">
                    <div class="session-item__title" :title="s.title">{{ s.title || '新会话' }}</div>
                    <div class="session-item__time">{{ formatSessionTime(s.updateTime) }}</div>
                  </div>
                  <el-icon class="session-item__delete" @click.stop="removeSession(s)"><Delete /></el-icon>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="检索" name="references">
              <div class="side-panel__body">
                <div v-if="currentReferences.length === 0" class="side-panel__empty">暂无检索来源</div>
                <div
                  v-for="(ref, idx) in currentReferences"
                  :key="ref.id || idx"
                  class="ref-item"
                  @click="openReference(ref)"
                >
                  <el-icon class="ref-item__icon"><Document /></el-icon>
                  <div class="ref-item__info">
                    <div class="ref-item__title" :title="ref.title">{{ ref.title || '未命名来源' }}</div>
                    <div class="ref-item__type">{{ ref.type || 'reference' }}</div>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>

        <!-- 折叠状态：角标图标切换 -->
        <div v-else class="side-panel__icons">
          <div
            class="side-icon"
            :class="{ active: activeSideTab === 'sessions' }"
            @click="openSideTab('sessions')"
          >
            <el-icon><ChatLineRound /></el-icon>
            <span v-if="sessions.length > 0" class="side-icon__badge">{{ sessions.length }}</span>
          </div>
          <div
            class="side-icon"
            :class="{ active: activeSideTab === 'references' }"
            @click="openSideTab('references')"
          >
            <el-icon><Link /></el-icon>
            <span v-if="currentReferences.length > 0" class="side-icon__badge">{{ currentReferences.length }}</span>
          </div>
        </div>
      </div>

      <!-- 中间：AI 聊天面板 -->
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
            <div class="ai-chat__bubble" @click="handleCitationClick($event, message)">
              <div
                v-if="message.role === 'assistant' && message.content"
                class="ai-chat__content ai-chat__content--markdown"
                v-html="renderMarkdown(message.content, message.references)"
              />
              <div v-else-if="message.content" class="ai-chat__content">{{ message.content }}</div>
              <div v-else class="ai-chat__content ai-chat__content--thinking">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>正在思考...</span>
              </div>
              <div v-if="message.meta" class="ai-chat__meta">
                <span>{{ message.meta }}</span>
                <div v-if="message.references && message.references.length > 0" class="ai-chat__refs">
                  <span class="ai-chat__refs-label">来源：</span>
                  <span
                    v-for="(ref, idx) in message.references"
                    :key="ref.id || idx"
                    class="ai-chat__ref-link"
                    @click.stop="openReference(ref)"
                  >
                    [{{ idx + 1 }}] {{ ref.title || '未命名来源' }}
                  </span>
                </div>
              </div>
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

      <!-- 引用内容预览弹窗（只读） -->
      <el-dialog
        v-model="previewVisible"
        :title="previewTitle"
        width="700px"
        top="8vh"
        :close-on-click-modal="true"
        class="ref-preview-dialog"
      >
        <div class="ref-preview-content">{{ previewContent }}</div>
        <template #footer>
          <el-button @click="previewVisible = false">关闭</el-button>
        </template>
      </el-dialog>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { nextTick, ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import MarkdownIt from 'markdown-it'
import {
  Close, Loading, Service, ArrowRight, ArrowLeft, Link, Document, User, Plus, Delete, ChatLineRound
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElDialog } from 'element-plus'
import dayjs from 'dayjs'
import { createAiChat, createAiChatStream, fetchChatSessions, fetchChatMessages, deleteChatSession } from '../api/ai'
import { fetchKnowledgeDocumentPreview } from '../api/knowledge'
import type { AiChatSession, AiChatMessage, AiReference } from '../types'

interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  meta?: string
  references?: Reference[]
}

interface Reference {
  id?: string
  type?: string
  title?: string
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
const sessions = ref<AiChatSession[]>([])
const currentSession = ref<AiChatSession | null>(null)
const sessionId = ref(generateSessionKey())
const messageListRef = ref<HTMLElement>()
const sidePanelOpen = ref(true)
const activeSideTab = ref<'sessions' | 'references'>('sessions')
const previewVisible = ref(false)
const previewTitle = ref('')
const previewContent = ref('')
const markdown = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true
})

// 流式输出状态
let textBuffer = ''
let displayBuffer = ''
let typingTimer: ReturnType<typeof setTimeout> | null = null
let currentAssistantMsg: ChatMessage | null = null
let isRevealing = false
let thinkTimer: ReturnType<typeof setTimeout> | null = null

const messages = ref<ChatMessage[]>([])

const currentReferences = computed<Reference[]>(() => {
  const lastAssistant = messages.value
    .filter(m => m.role === 'assistant')
    .pop()
  return lastAssistant?.references || []
})

const quickPrompts = [
  '如何完成挂号到取药流程？',
  '药品库存预警如何处理？',
  '医生工作站怎么写病历？',
  '怎样为患者预约挂号？'
]

onMounted(() => {
  loadSessions().then(() => {
    if (!currentSession.value) {
      createNewSession()
    }
  })
})

function generateSessionKey() {
  return `chat-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`
}

function openSideTab(tab: 'sessions' | 'references') {
  activeSideTab.value = tab
  sidePanelOpen.value = true
}

function pushWelcome() {
  messages.value.push({
    id: 'welcome',
    role: 'assistant',
    content: '你好，我是 MediCare AI 助手。我可以帮你梳理系统操作流程和页面功能，随时为你解答疑问。'
  })
}

async function loadSessions() {
  try {
    const res = await fetchChatSessions()
    sessions.value = res.data || []
    if (!currentSession.value && sessions.value.length > 0) {
      await selectSession(sessions.value[0])
    }
  } catch {
    sessions.value = []
  }
}

async function selectSession(session: AiChatSession) {
  currentSession.value = session
  sessionId.value = session.sessionKey
  messages.value = []
  stopTyping()
  loading.value = false
  isTyping.value = false

  try {
    const res = await fetchChatMessages(session.id)
    const list: AiChatMessage[] = res.data || []
    messages.value = list.map(m => ({
      id: String(m.id),
      role: m.role as 'user' | 'assistant',
      content: m.content
    }))
    if (messages.value.length === 0) {
      pushWelcome()
    }
  } catch {
    pushWelcome()
  }
  await scrollToBottom()
}

function createNewSession() {
  currentSession.value = null
  sessionId.value = generateSessionKey()
  messages.value = []
  pushWelcome()
  input.value = ''
  stopTyping()
  activeSideTab.value = 'sessions'
}

async function removeSession(session: AiChatSession) {
  try {
    await ElMessageBox.confirm('确定删除该会话？历史消息将无法恢复。', '提示', { type: 'warning' })
    await deleteChatSession(session.id)
    sessions.value = sessions.value.filter(s => s.id !== session.id)
    if (currentSession.value?.id === session.id) {
      if (sessions.value.length > 0) {
        await selectSession(sessions.value[0])
      } else {
        createNewSession()
      }
    }
    ElMessage.success('删除成功')
  } catch {
    // 取消删除或删除失败时忽略
  }
}

function formatSessionTime(time?: string) {
  return time ? dayjs(time).format('MM-DD HH:mm') : ''
}

function fillPrompt(text: string) {
  input.value = text
}

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
      const chunkSize = Math.max(5, Math.floor(Math.random() * Math.min(30, remaining)) + 1)
      displayBuffer = textBuffer.substring(0, currentLen + chunkSize)
      currentAssistantMsg.content = displayBuffer

      await nextTick()
      scrollToBottom()

      const delay = 30 + Math.random() * 70
      typingTimer = setTimeout(() => revealNext(), Math.round(delay))
    } else if (loading.value) {
      typingTimer = setTimeout(() => revealNext(), 50)
    } else {
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

  messages.value.push({
    id: `${Date.now()}-user`,
    role: 'user',
    content
  })
  input.value = ''
  loading.value = true

  const assistantMessage: ChatMessage = {
    id: `${Date.now()}-assistant`,
    role: 'assistant',
    content: '',
    references: []
  }
  messages.value.push(assistantMessage)
  textBuffer = ''
  displayBuffer = ''
  stopTyping()
  await scrollToBottom()

  const startTime = Date.now()
  const isNewSession = !currentSession.value

  try {
    const requestData = {
      message: content,
      sessionId: currentSession.value?.sessionKey || sessionId.value,
      context: {
        route: route.fullPath,
        routeName: String(route.name || '')
      }
    }

    thinkTimer = setTimeout(() => {
      if (!isRevealing && textBuffer.length > 0) {
        startTyping(assistantMessage)
      }
    }, 2000)

    await createAiChatStream(requestData, {
      onChunk: (text) => {
        textBuffer += text
        if (!isRevealing && Date.now() - startTime >= 2000) {
          startTyping(assistantMessage)
        }
      },
      onReferences: (refs) => {
        assistantMessage.references = refs
        if (refs.length > 0) {
          activeSideTab.value = 'references'
        }
      },
      onReferencesError: (message) => {
        ElMessage.warning(`检索失败：${message}`)
      },
      onDone: (meta) => {
        assistantMessage.meta = `${meta.provider} / ${meta.model}`
        if (meta.sessionId) {
          sessionId.value = meta.sessionId
        }
        if (meta.references && meta.references.length > 0) {
          assistantMessage.references = meta.references
        }
        loading.value = false
      },
      onError: (message) => {
        textBuffer = message || 'AI 调用失败，未生成回复。请查看页面提示或后端日志后重试。'
        loading.value = false
        if (!isRevealing) {
          thinkTimer = setTimeout(() => startTyping(assistantMessage), 100)
        }
      }
    })
  } catch (error) {
    if (!textBuffer) {
      try {
        const res = await createAiChat({
          message: content,
          sessionId: currentSession.value?.sessionKey || sessionId.value,
          context: {
            route: route.fullPath,
            routeName: String(route.name || '')
          }
        })
        textBuffer = res.data.answer
        assistantMessage.references = res.data.references || []
        if (res.data.sessionId) {
          sessionId.value = res.data.sessionId
        }
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
        thinkTimer = setTimeout(() => startTyping(assistantMessage), 100)
      }
    }
  }

  await waitTypingComplete()
  loading.value = false
  await scrollToBottom()

  // 对话完成后刷新会话列表，并定位到当前会话
  await loadSessions()
  if (isNewSession && !currentSession.value) {
    currentSession.value = sessions.value.find(s => s.sessionKey === sessionId.value) || null
  }
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

function renderMarkdown(content: string, references?: Reference[]) {
  // 将 [引用N] 渲染为上标可点击标签；只渲染有效引用，超出范围则保留原文但不生成点击标签
  const maxIdx = references?.length || 0
  const tagged = content.replace(/\[引用(\d+)\]/g, (match, num) => {
    const idx = parseInt(num, 10)
    if (idx >= 1 && idx <= maxIdx) {
      return `<sup class="ai-citation" data-idx="${idx}">${idx}</sup>`
    }
    return match
  })
  return markdown.render(tagged)
}

function handleCitationClick(event: MouseEvent, message: ChatMessage) {
  const target = event.target as HTMLElement
  if (target.classList.contains('ai-citation')) {
    event.preventDefault()
    event.stopPropagation()
    const idx = parseInt(target.dataset.idx || '0', 10) - 1
    const ref = message.references?.[idx]
    if (ref) {
      openReference(ref)
    } else {
      ElMessage.warning('未找到对应来源')
    }
  }
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

async function openReference(ref: Reference) {
  if (!ref.id) {
    ElMessage.warning('该来源暂无预览')
    return
  }
  try {
    const res = await fetchKnowledgeDocumentPreview(Number(ref.id))
    const data = res.data
    previewTitle.value = data.filename || ref.title || '来源预览'
    previewContent.value = data.content || ''
    previewVisible.value = true
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '来源预览加载失败'
    ElMessage.error(message)
  }
}
</script>

<style scoped>
.ai-chat-wrapper {
  display: flex;
  height: 100%;
  background: var(--bg-page);
}

/* ===== 左侧整合侧边栏 ===== */
.side-panel {
  width: 220px;
  flex-shrink: 0;
  border-right: 1px solid var(--divider-color);
  background: var(--bg-card);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  overflow: hidden;
}

.side-panel.collapsed {
  width: 44px;
}

.side-panel__toggle {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid var(--divider-color);
  cursor: pointer;
  color: var(--text-secondary);
  transition: background 0.2s, color 0.2s;
  flex-shrink: 0;
}

.side-panel__toggle:hover {
  background: var(--bg-hover);
  color: var(--color-primary);
}

.side-panel__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.side-panel__content :deep(.el-tabs) {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

.side-panel__content :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 12px;
  flex-shrink: 0;
}

.side-panel__content :deep(.el-tabs__content) {
  flex: 1;
  overflow: hidden;
}

.side-panel__content :deep(.el-tab-pane) {
  height: 100%;
  overflow: hidden;
}

.side-panel__body {
  height: 100%;
  overflow-y: auto;
  padding: 12px;
}

.side-panel__new {
  justify-content: flex-start;
  margin-bottom: 10px;
  font-size: 13px;
  width: 100%;
}

.side-panel__empty {
  padding: 24px 4px;
  text-align: center;
  color: var(--text-muted);
  font-size: 12px;
}

.side-panel__icons {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 12px;
  gap: 12px;
}

.side-icon {
  position: relative;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  cursor: pointer;
  color: var(--text-secondary);
  background: var(--bg-page);
  transition: background 0.2s, color 0.2s;
}

.side-icon:hover,
.side-icon.active {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.side-icon__badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: var(--color-danger);
  color: #fff;
  font-size: 10px;
  line-height: 16px;
  text-align: center;
}

.session-item,
.ref-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 6px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
  margin-bottom: 6px;
}

.session-item:hover,
.session-item.active,
.ref-item:hover {
  background: var(--color-primary-light);
}

.session-item__icon,
.ref-item__icon {
  color: var(--color-primary);
  flex-shrink: 0;
}

.session-item__info,
.ref-item__info {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.session-item__title,
.ref-item__title {
  font-size: 12px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-item__time,
.ref-item__type {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}

.session-item__delete {
  flex-shrink: 0;
  color: var(--text-muted);
  opacity: 0;
  transition: opacity 0.2s, color 0.2s;
}

.session-item:hover .session-item__delete {
  opacity: 1;
}

.session-item__delete:hover {
  color: var(--color-danger);
}

/* ===== 中间 AI 聊天面板 ===== */
.ai-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
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

.ai-chat__refs {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.ai-chat__refs-label {
  color: var(--text-secondary);
}

.ai-chat__ref-link {
  color: var(--color-primary);
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.ai-chat__ref-link:hover {
  color: var(--color-primary-dark);
}

.ai-chat__content--markdown :deep(.ai-citation) {
  display: inline-block;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
  margin: 0 2px;
  padding: 0 4px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 600;
  text-align: center;
  color: #fff;
  background: var(--color-primary);
  cursor: pointer;
  user-select: none;
  vertical-align: super;
}

.ai-chat__content--markdown :deep(.ai-citation:hover) {
  background: var(--color-primary-dark);
}

.ref-preview-dialog :deep(.el-dialog__body) {
  height: 60vh;
  max-height: 60vh;
  overflow-y: auto;
  padding: 16px 20px;
}

.ref-preview-dialog :deep(.el-dialog__footer) {
  padding: 12px 20px;
}

.ref-preview-content {
  white-space: pre-wrap;
  line-height: 1.7;
  font-size: var(--font-size-sm);
  color: var(--text-primary);
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
  .side-panel {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 10;
    box-shadow: 2px 0 8px rgba(0, 0, 0, 0.08);
  }

  .side-panel.collapsed {
    width: 0;
    border: none;
  }

  .side-panel.collapsed .side-panel__toggle {
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 24px;
    height: 48px;
    border-radius: 0 8px 8px 0;
    border: 1px solid var(--divider-color);
    border-left: none;
    background: var(--bg-card);
  }

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
