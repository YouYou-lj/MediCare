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
        <!-- 折叠状态：角标图标切换 -->
        <div v-if="!sidePanelOpen" class="side-panel__icons">
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
          <div class="side-icon side-icon--toggle" @click="sidePanelOpen = true">
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>

        <!-- 展开状态：紧凑标签页切换（标题栏与 tab 合并为一行） -->
        <template v-else>
          <div class="side-panel__header">
            <div class="side-panel__tabs">
              <div
                class="side-panel__tab"
                :class="{ active: activeSideTab === 'sessions' }"
                @click="activeSideTab = 'sessions'"
              >
                <el-icon><ChatLineRound /></el-icon>
                <span>会话</span>
                <span v-if="sessions.length > 0" class="side-panel__tab-badge">{{ sessions.length }}</span>
              </div>
              <div
                class="side-panel__tab"
                :class="{ active: activeSideTab === 'references' }"
                @click="activeSideTab = 'references'"
              >
                <el-icon><Link /></el-icon>
                <span>检索</span>
                <span v-if="currentReferences.length > 0" class="side-panel__tab-badge">{{ currentReferences.length }}</span>
              </div>
            </div>
            <div class="side-panel__collapse" @click="sidePanelOpen = false">
              <el-icon><ArrowLeft /></el-icon>
            </div>
          </div>

          <div class="side-panel__body">
            <div v-show="activeSideTab === 'sessions'">
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
            <div v-show="activeSideTab === 'references'">
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
          </div>
        </template>
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
              <div v-if="message.file" class="ai-chat__file-tag">
                <el-icon><Document /></el-icon>
                <span :title="message.file.filename">{{ message.file.filename }}</span>
              </div>
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
          <div v-if="uploadedFile" class="ai-chat__uploaded-file-bar">
            <div class="ai-chat__uploaded-file" :class="{ 'ai-chat__uploaded-file--parsing': uploadedFile.parsing }">
              <el-icon v-if="uploadedFile.parsing" class="is-loading"><Loading /></el-icon>
              <el-icon v-else><Document /></el-icon>
              <span class="ai-chat__uploaded-name" :title="uploadedFile.filename">
                {{ uploadedFile.parsing ? `正在解析《${uploadedFile.filename}》…` : `已就绪《${uploadedFile.filename}》` }}
              </span>
              <span v-if="!uploadedFile.parsing" class="ai-chat__uploaded-meta">{{ uploadedFile.chunkCount }} 个分块</span>
              <el-icon v-if="!uploadedFile.parsing" class="ai-chat__uploaded-close" @click="removeUploadedFile"><Close /></el-icon>
            </div>
          </div>
          <el-input
            v-model="input"
            type="textarea"
            :rows="3"
            resize="none"
            maxlength="1000"
            show-word-limit
            :placeholder="uploadedFile ? '请输入关于该文件的问题，例如：请总结这份文档的核心内容' : '请输入想咨询的问题，例如：如何完成挂号到取药流程？'"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <div class="ai-chat__actions">
            <div class="ai-chat__actions-left">
              <el-upload
                :auto-upload="false"
                :show-file-list="false"
                accept=".docx,.pdf,.md"
                :disabled="uploadedFile?.parsing || loading || isTyping"
                @change="handleFileUpload"
              >
                <el-button
                  class="ai-chat__upload-btn"
                  :icon="Upload"
                  :loading="uploadedFile?.parsing"
                  :disabled="uploadedFile?.parsing || loading || isTyping"
                  type="primary"
                  plain
                  size="small"
                >
                  上传文件
                </el-button>
              </el-upload>
              <span class="ai-chat__hint">支持docx、pdf、md文件</span>
            </div>
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
        <div class="ref-preview-content" v-html="renderPreviewMarkdown(previewContent)" />
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
  Close, Loading, Service, ArrowRight, ArrowLeft, Link, Document, User, Plus, Delete, ChatLineRound, Upload
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElDialog } from 'element-plus'
import dayjs from 'dayjs'
import { createAiChat, createAiChatStream, fetchChatSessions, fetchChatMessages, deleteChatSession } from '../api/ai'
import { createAssistantKnowledgeDocument, fetchKnowledgeDocumentPreview } from '../api/knowledge'
import type { AiChatSession, AiChatMessage, AiReference } from '../types'

interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  meta?: string
  references?: Reference[]
  file?: {
    filename: string
    sourcePath: string
  }
}

interface Reference {
  id?: string
  type?: string
  title?: string
}

interface UploadedFile {
  filename: string
  sourcePath: string
  chunkCount: number
  parsing: boolean
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
const previewMarkdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true
})

const uploadedFile = ref<UploadedFile | null>(null)

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
  const refs = lastAssistant?.references || []
  // 按 id 去重，同文档不同分块合并为一条
  const seen = new Set<string>()
  return refs.filter(r => {
    const key = r.id || r.title || ''
    if (!key || seen.has(key)) return false
    seen.add(key)
    return true
  })
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
      content: m.content,
      references: m.references || []
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

async function handleFileUpload(file: any) {
  const raw = file?.raw
  if (!raw) return
  const filename = raw.name || '上传文件'
  const lower = filename.toLowerCase()
  if (!lower.endsWith('.docx') && !lower.endsWith('.pdf') && !lower.endsWith('.md')) {
    ElMessage.warning('仅支持 docx、pdf、md 格式的文件')
    return
  }
  if (uploadedFile.value?.parsing || loading.value || isTyping.value) {
    ElMessage.warning('请等待当前操作完成后再上传')
    return
  }

  uploadedFile.value = { filename, sourcePath: '', chunkCount: 0, parsing: true }
  try {
    const form = new FormData()
    form.append('file', raw)
    const res = await createAssistantKnowledgeDocument(form)
    const data = res.data
    uploadedFile.value = {
      filename: data.filename,
      sourcePath: data.sourcePath,
      chunkCount: data.chunkCount,
      parsing: false
    }
  } catch (error: any) {
    uploadedFile.value = null
    const msg = error?.response?.data?.message || error?.message || '文件上传失败'
    ElMessage.error(msg)
  }
}

function removeUploadedFile() {
  uploadedFile.value = null
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
  const userInput = input.value.trim()
  if (!userInput || loading.value || isTyping.value) return

  const backendContent = userInput
  let fileSourcePath: string | undefined
  let file: { filename: string; sourcePath: string } | undefined
  if (uploadedFile.value) {
    file = {
      filename: uploadedFile.value.filename,
      sourcePath: uploadedFile.value.sourcePath,
    }
    fileSourcePath = uploadedFile.value.sourcePath
  }

  input.value = ''
  uploadedFile.value = null

  messages.value.push({
    id: `${Date.now()}-user`,
    role: 'user',
    content: userInput,
    file,
  })
  await scrollToBottom()

  await sendText(backendContent, fileSourcePath)
}

async function sendText(content: string, fileSourcePath?: string) {
  if (!content || loading.value || isTyping.value) return

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
      fileSourcePath,
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
          fileSourcePath,
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
  if (!content) return ''
  const maxIdx = references?.length || 0
  if (maxIdx === 0) {
    // 无引用时，移除所有 [引用N] 标记避免显示为纯文本
    const cleaned = content.replace(/\[引用\d+\]/g, '')
    return markdown.render(cleaned)
  }
  // 收集文本中出现的有效引用编号，按出现顺序重映射为连续序号
  const citationPattern = /\[引用(\d+)\]/g
  const remap = new Map<number, number>() // 原始编号 → 连续序号(1-based)
  let nextSeq = 1
  let m: RegExpExecArray | null
  while ((m = citationPattern.exec(content)) !== null) {
    const original = parseInt(m[1], 10)
    if (original >= 1 && original <= maxIdx && !remap.has(original)) {
      remap.set(original, nextSeq++)
    }
  }
  // 替换 [引用N] 为可点击上标：显示连续序号，data-idx 保持原始编号用于引用查找
  const tagged = content.replace(/\[引用(\d+)\]/g, (_match, num) => {
    const original = parseInt(num, 10)
    const seq = remap.get(original)
    if (seq !== undefined) {
      return `<sup class="ai-citation" data-idx="${original}">${seq}</sup>`
    }
    return ''
  })
  return markdown.render(tagged)
}

function renderPreviewMarkdown(content: string) {
  if (!content) {
    return '<p class="ref-preview-content__empty">暂无预览内容</p>'
  }
  return previewMarkdown.render(content)
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

/* ===== 左侧整合侧边栏（现代医疗风） ===== */
.side-panel {
  width: 232px;
  flex-shrink: 0;
  border-right: 1px solid var(--divider-color);
  background: linear-gradient(180deg, #ffffff 0%, #f8fbfd 100%);
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.side-panel.collapsed {
  width: 56px;
}

/* 展开状态：标题栏与 tab 合并为一行 */
.side-panel__header {
  height: 56px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 0 12px 0 14px;
  border-bottom: 1px solid var(--border-light);
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(8px);
}

.side-panel__tabs {
  flex: 1;
  min-width: 0;
  display: flex;
  gap: 4px;
  padding: 4px;
  background: var(--bg-toolbar);
  border-radius: 10px;
}

.side-panel__tab {
  flex: 1;
  min-width: 0;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 0 10px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.25s ease;
  white-space: nowrap;
  position: relative;
}

.side-panel__tab:hover {
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.7);
}

.side-panel__tab.active {
  color: var(--color-primary);
  background: #ffffff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  font-weight: 600;
}

.side-panel__tab .el-icon {
  font-size: 14px;
}

.side-panel__tab-badge {
  min-width: 15px;
  height: 15px;
  padding: 0 4px;
  border-radius: 8px;
  background: var(--color-danger);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  line-height: 15px;
  text-align: center;
}

.side-panel__collapse {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  cursor: pointer;
  color: var(--text-muted);
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.side-panel__collapse:hover {
  background: var(--color-primary-light);
  color: var(--color-primary);
  transform: translateX(-1px);
}

.side-panel__body {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
}

.side-panel__new {
  justify-content: flex-start;
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 500;
  width: 100%;
  height: 34px;
  border-radius: 8px;
  border: 1px dashed var(--border-color);
  color: var(--color-primary);
  background: rgba(15, 159, 143, 0.04);
  transition: all 0.2s ease;
}

.side-panel__new:hover {
  background: var(--color-primary-light);
  border-color: var(--color-primary);
}

.side-panel__empty {
  padding: 32px 8px;
  text-align: center;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.6;
}

/* 折叠状态：角标图标切换 */
.side-panel__icons {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px 0;
  gap: 14px;
}

.side-icon {
  position: relative;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  cursor: pointer;
  color: var(--text-secondary);
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;
}

.side-icon:hover,
.side-icon.active {
  color: var(--color-primary);
  background: var(--color-primary-light);
  box-shadow: 0 2px 6px rgba(15, 159, 143, 0.12);
}

.side-icon--toggle {
  margin-top: auto;
  margin-bottom: 4px;
  background: var(--bg-toolbar);
}

.side-icon--toggle:hover {
  background: var(--color-primary-light);
}

.side-icon__badge {
  position: absolute;
  top: -5px;
  right: -5px;
  min-width: 17px;
  height: 17px;
  padding: 0 4px;
  border-radius: 9px;
  background: var(--color-danger);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  line-height: 17px;
  text-align: center;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.12);
}

.session-item,
.ref-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 6px;
  background: #ffffff;
  border: 1px solid transparent;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
}

.session-item:hover,
.session-item.active,
.ref-item:hover {
  background: #ffffff;
  border-color: var(--color-primary-light);
  box-shadow: 0 2px 8px rgba(15, 159, 143, 0.08);
}

.session-item.active {
  background: var(--color-primary-light);
  border-color: rgba(15, 159, 143, 0.15);
}

.session-item__icon,
.ref-item__icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: var(--color-primary);
  background: rgba(15, 159, 143, 0.08);
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
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}

.session-item__time,
.ref-item__type {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 3px;
}

.session-item__delete {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 5px;
  color: var(--text-muted);
  opacity: 0;
  transition: all 0.2s ease;
}

.session-item:hover .session-item__delete {
  opacity: 1;
}

.session-item__delete:hover {
  color: var(--color-danger);
  background: var(--color-danger-light);
}

/* ===== 中间 AI 聊天面板 ===== */
.ai-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--bg-page);
}

.ai-chat__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-md) var(--space-lg);
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1px solid var(--border-light);
  backdrop-filter: blur(8px);
}

.ai-chat__title {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-chat__title::before {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-light);
}

.ai-chat__messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-lg);
}

.ai-chat__message {
  display: flex;
  gap: var(--space-sm);
  margin-bottom: var(--space-lg);
  animation: messageIn 0.35s ease;
}

@keyframes messageIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.ai-chat__message--user {
  flex-direction: row-reverse;
}

.ai-chat__avatar {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: var(--radius-circle);
  color: #ffffff;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  box-shadow: 0 2px 6px rgba(15, 159, 143, 0.2);
}

.ai-chat__message--user .ai-chat__avatar {
  background: linear-gradient(135deg, var(--color-success) 0%, #0d9488 100%);
  box-shadow: 0 2px 6px rgba(16, 185, 129, 0.2);
}

.ai-chat__bubble {
  max-width: 78%;
  padding: 12px 14px;
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid var(--border-light);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s ease;
}

.ai-chat__bubble:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.ai-chat__file-tag {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  margin-bottom: var(--space-sm);
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-button);
  background: rgba(15, 159, 143, 0.1);
  color: var(--color-primary-dark);
  font-size: var(--font-size-xs);
  max-width: 100%;
}

.ai-chat__file-tag span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 220px;
}

.ai-chat__message--user .ai-chat__file-tag {
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
}

.ai-chat__message--user .ai-chat__bubble {
  color: #fff;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  border-color: transparent;
  box-shadow: 0 3px 10px rgba(15, 159, 143, 0.2);
}

.ai-chat__content {
  white-space: pre-wrap;
  line-height: 1.65;
  font-size: var(--font-size-base);
  color: var(--text-primary);
}

.ai-chat__message--user .ai-chat__content {
  color: #ffffff;
}

.ai-chat__content--markdown {
  white-space: normal;
}

.ai-chat__content--markdown :deep(p) {
  margin: 0 0 10px;
}

.ai-chat__content--markdown :deep(p:last-child) {
  margin-bottom: 0;
}

.ai-chat__content--markdown :deep(ol),
.ai-chat__content--markdown :deep(ul) {
  margin: 10px 0;
  padding-left: 22px;
}

.ai-chat__content--markdown :deep(li + li) {
  margin-top: 5px;
}

.ai-chat__content--markdown :deep(strong) {
  font-weight: 700;
  color: var(--text-primary);
}

.ai-chat__message--user .ai-chat__content--markdown :deep(strong) {
  color: #ffffff;
}

.ai-chat__content--markdown :deep(code) {
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  color: var(--color-primary);
  background: var(--color-primary-light);
  font-size: 12px;
}

.ai-chat__message--user .ai-chat__content--markdown :deep(code) {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.2);
}

.ai-chat__content--markdown :deep(blockquote) {
  margin: 10px 0;
  padding: 8px 12px;
  color: var(--text-secondary);
  background: var(--bg-hover);
  border-left: 3px solid var(--color-primary);
  border-radius: 0 var(--radius-button) var(--radius-button) 0;
}

.ai-chat__content--thinking {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  color: var(--text-secondary);
}

.ai-chat__meta {
  margin-top: 10px;
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}

.ai-chat__refs {
  margin-top: 8px;
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
  text-decoration: none;
  padding: 2px 8px;
  border-radius: 10px;
  background: var(--color-primary-light);
  font-size: 12px;
  transition: all 0.2s ease;
}

.ai-chat__ref-link:hover {
  color: var(--color-primary-dark);
  background: rgba(15, 159, 143, 0.15);
}

.ai-chat__content--markdown :deep(.ai-citation) {
  display: inline-block;
  min-width: 17px;
  height: 17px;
  line-height: 17px;
  margin: 0 2px;
  padding: 0 5px;
  border-radius: 9px;
  font-size: 11px;
  font-weight: 600;
  text-align: center;
  color: #fff;
  background: var(--color-primary);
  cursor: pointer;
  user-select: none;
  vertical-align: super;
  transition: background 0.2s ease;
}

.ai-chat__content--markdown :deep(.ai-citation:hover) {
  background: var(--color-primary-dark);
}

:global(.ref-preview-dialog) {
  display: flex;
  flex-direction: column;
  height: min(76vh, 720px);
  max-height: calc(100vh - 16vh);
  overflow: hidden;
  border-radius: var(--radius-card);
}

:global(.ref-preview-dialog .el-dialog__header) {
  flex-shrink: 0;
  padding: var(--space-lg) var(--space-xl) var(--space-md);
  border-bottom: 1px solid var(--divider-color);
}

:global(.ref-preview-dialog .el-dialog__body) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding: var(--space-md) var(--space-xl);
}

:global(.ref-preview-dialog .el-dialog__footer) {
  flex-shrink: 0;
  padding: var(--space-md) var(--space-xl) var(--space-lg);
  border-top: 1px solid var(--divider-color);
}

.ref-preview-content {
  height: 100%;
  overflow-y: auto;
  padding: var(--space-md);
  line-height: 1.7;
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  background: var(--bg-toolbar);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-card);
}

.ref-preview-content :deep(p) {
  margin: 0 0 var(--space-sm);
}

.ref-preview-content :deep(p:last-child) {
  margin-bottom: 0;
}

.ref-preview-content :deep(h1),
.ref-preview-content :deep(h2),
.ref-preview-content :deep(h3),
.ref-preview-content :deep(h4) {
  margin: var(--space-lg) 0 var(--space-sm);
  line-height: 1.35;
  color: var(--text-primary);
}

.ref-preview-content :deep(h1:first-child),
.ref-preview-content :deep(h2:first-child),
.ref-preview-content :deep(h3:first-child),
.ref-preview-content :deep(h4:first-child) {
  margin-top: 0;
}

.ref-preview-content :deep(h1) {
  font-size: var(--font-size-2xl);
}

.ref-preview-content :deep(h2) {
  font-size: var(--font-size-xl);
}

.ref-preview-content :deep(h3) {
  font-size: var(--font-size-lg);
}

.ref-preview-content :deep(h4) {
  font-size: var(--font-size-md);
}

.ref-preview-content :deep(ul),
.ref-preview-content :deep(ol) {
  margin: var(--space-sm) 0;
  padding-left: var(--space-xl);
}

.ref-preview-content :deep(li + li) {
  margin-top: var(--space-xs);
}

.ref-preview-content :deep(blockquote) {
  margin: var(--space-md) 0;
  padding: var(--space-xs) var(--space-md);
  color: var(--text-secondary);
  background: var(--bg-card);
  border-left: 3px solid var(--color-primary);
  border-radius: var(--radius-button);
}

.ref-preview-content :deep(pre) {
  overflow-x: auto;
  margin: var(--space-md) 0;
  padding: var(--space-md);
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-button);
}

.ref-preview-content :deep(code) {
  padding: 2px 5px;
  color: var(--color-primary);
  background: var(--color-primary-light);
  border-radius: var(--radius-sm);
}

.ref-preview-content :deep(pre code) {
  padding: 0;
  color: var(--text-primary);
  background: transparent;
  border-radius: 0;
}

.ref-preview-content :deep(table) {
  width: 100%;
  margin: var(--space-md) 0;
  border-collapse: collapse;
  background: var(--bg-card);
}

.ref-preview-content :deep(th),
.ref-preview-content :deep(td) {
  padding: var(--space-sm);
  text-align: left;
  border: 1px solid var(--border-color);
}

.ref-preview-content :deep(th) {
  color: var(--text-secondary);
  background: var(--bg-hover);
  font-weight: 600;
}

.ref-preview-content :deep(a) {
  color: var(--text-link);
  text-decoration: none;
}

.ref-preview-content :deep(a:hover) {
  text-decoration: underline;
}

.ref-preview-content :deep(.ref-preview-content__empty) {
  color: var(--text-muted);
}

.ai-chat__quick {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 0 var(--space-lg) var(--space-md);
}

.ai-chat__quick .el-button {
  width: 100%;
  justify-content: center;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  height: 38px;
  padding: 0 14px;
  border-radius: 19px;
  border: 1px solid var(--border-color);
  background: #ffffff;
  color: var(--text-secondary);
  font-size: 13px;
  transition: all 0.2s ease;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.ai-chat__quick .el-button:hover {
  color: var(--color-primary);
  border-color: var(--color-primary-light);
  background: var(--color-primary-light);
  box-shadow: 0 3px 10px rgba(15, 159, 143, 0.1);
  transform: translateY(-1px);
}

.ai-chat__quick .el-button > span {
  display: block;
  width: 100%;
}

.ai-chat__footer {
  padding: var(--space-md) var(--space-lg) var(--space-lg);
  background: #ffffff;
  border-top: 1px solid var(--border-light);
}

.ai-chat__footer .el-textarea__inner {
  border-radius: 12px;
  padding: 12px 14px;
  background: var(--bg-toolbar);
  border: 1px solid var(--border-color);
  transition: all 0.2s ease;
  font-size: var(--font-size-base);
  line-height: 1.6;
}

.ai-chat__footer .el-textarea__inner:focus {
  background: #ffffff;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-light);
}

.ai-chat__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  margin-top: var(--space-md);
}

.ai-chat__actions-left {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.ai-chat__actions-left .el-upload {
  display: inline-flex;
}

.ai-chat__upload-btn {
  border-radius: 8px;
  padding: 0 12px;
  height: 32px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.ai-chat__upload-btn:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 3px 10px rgba(15, 159, 143, 0.12);
}

.ai-chat__uploaded-file-bar {
  margin-bottom: var(--space-md);
}

.ai-chat__uploaded-file {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-button);
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
  font-size: var(--font-size-sm);
  border: 1px solid rgba(15, 159, 143, 0.2);
  transition: all 0.2s ease;
}

.ai-chat__uploaded-file--parsing {
  background: var(--bg-hover);
  color: var(--text-secondary);
  border-color: var(--border-color);
}

.ai-chat__uploaded-name {
  max-width: 260px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ai-chat__uploaded-meta {
  color: var(--text-muted);
  font-size: var(--font-size-xs);
  padding-left: var(--space-xs);
  border-left: 1px solid var(--border-color);
}

.ai-chat__uploaded-close {
  margin-left: var(--space-xs);
  color: var(--text-muted);
  cursor: pointer;
  transition: color 0.2s ease;
}

.ai-chat__uploaded-close:hover {
  color: var(--color-danger);
}

.ai-chat__hint {
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}

.ai-chat__actions .el-button--primary {
  min-width: 80px;
  border-radius: 8px;
  height: 36px;
  font-weight: 500;
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
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }
}
</style>
