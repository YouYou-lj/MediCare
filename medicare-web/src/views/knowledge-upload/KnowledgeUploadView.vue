<template>
  <div class="knowledge-upload">
    <PageHeader
      title="知识库上传"
      subtitle="上传门诊制度、操作说明、培训材料或业务文档，系统会解析文本并写入 AI 知识库"
    >
      <el-button :loading="reindexing" type="primary" :icon="Refresh" :disabled="!isMainAdmin" @click="handleReindex">
        重建全部索引
      </el-button>
    </PageHeader>

    <div class="knowledge-upload__workspace">
      <el-card shadow="hover" class="knowledge-upload__panel knowledge-upload__panel--main">
        <template #header>
          <div class="knowledge-upload__panel-header">
            <div class="knowledge-upload__panel-title">
              <el-icon><UploadFilled /></el-icon>
              <span>文档上传</span>
            </div>
            <span class="knowledge-upload__panel-meta">队列 {{ batchQueue.length }} 个</span>
          </div>
        </template>

        <div class="knowledge-upload__upload-area">
          <el-upload
            ref="uploadRef"
            class="knowledge-upload__drop"
            drag
            multiple
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleFileChange"
            :accept="acceptedTypes"
          >
            <el-icon class="knowledge-upload__drop-icon"><UploadFilled /></el-icon>
            <div class="knowledge-upload__drop-title">拖拽文件到这里，或点击选择</div>
            <div class="knowledge-upload__drop-tip">支持 PDF、Word、Text、Markdown、PPTX，可多选文件</div>
          </el-upload>

          <div class="knowledge-upload__folder-row">
            <el-button type="primary" plain :icon="FolderOpened" @click="triggerFolderSelect">选择文件夹</el-button>
            <span class="knowledge-upload__folder-hint">自动筛选文件夹中支持的文件格式</span>
            <input
              ref="folderInputRef"
              type="file"
              webkitdirectory
              multiple
              class="knowledge-upload__folder-input"
              @change="handleFolderChange"
            />
          </div>

          <div class="knowledge-upload__format-hint">
            <div class="knowledge-upload__hint-copy">支持格式：PDF、DOC、DOCX、TXT、MD、PPTX</div>
            <div class="knowledge-upload__format-list" aria-label="支持上传格式">
              <span v-for="format in supportedFormats" :key="format" class="knowledge-upload__format">{{ format }}</span>
            </div>
          </div>
        </div>

        <div class="knowledge-upload__queue-wrap">
          <template v-if="batchQueue.length > 0">
            <div class="knowledge-upload__queue-header">
              <div class="knowledge-upload__queue-title">
                <el-icon><Files /></el-icon>
                <span>上传队列</span>
              </div>
              <div class="knowledge-upload__queue-stats">
                <el-tag size="small" type="info">{{ batchQueue.length }} 个文件</el-tag>
                <span v-if="batchSuccessCount > 0" class="knowledge-upload__stat knowledge-upload__stat--success">成功 {{ batchSuccessCount }}</span>
                <span v-if="batchFailCount > 0" class="knowledge-upload__stat knowledge-upload__stat--fail">失败 {{ batchFailCount }}</span>
              </div>
            </div>

            <div v-if="batchUploading" class="knowledge-upload__progress">
              <div class="knowledge-upload__progress-header">
                <span>正在上传 {{ batchCurrentIndex + 1 }} / {{ batchQueue.length }}</span>
                <span>{{ batchOverallPercent }}%</span>
              </div>
              <el-progress
                :percentage="batchOverallPercent"
                :status="batchFailCount > 0 && !batchUploading ? 'exception' : undefined"
                :stroke-width="8"
                :show-text="false"
              />
            </div>

            <div class="knowledge-upload__queue-list">
            <div
              v-for="(item, index) in batchQueue"
              :key="item.id"
              class="knowledge-upload__queue-item"
              :class="`knowledge-upload__queue-item--${item.status}`"
            >
                <div class="knowledge-upload__queue-index">{{ String(index + 1).padStart(2, '0') }}</div>
                <div class="knowledge-upload__queue-info">
                  <div class="knowledge-upload__queue-name" :title="item.name">{{ item.name }}</div>
                  <div class="knowledge-upload__queue-meta">
                    <span class="knowledge-upload__queue-size">{{ formatFileSize(item.size) }}</span>
                    <span v-if="item.result" class="knowledge-upload__queue-chunks">{{ item.result.chunkCount }} 个分块</span>
                    <span v-if="item.error" class="knowledge-upload__queue-error">{{ item.error }}</span>
                  </div>
                </div>
                <div class="knowledge-upload__queue-status">
                  <el-icon v-if="item.status === 'pending'" class="knowledge-upload__status knowledge-upload__status--pending"><Clock /></el-icon>
                  <el-icon v-if="item.status === 'uploading'" class="knowledge-upload__status knowledge-upload__status--uploading"><Loading /></el-icon>
                  <el-icon v-if="item.status === 'success'" class="knowledge-upload__status knowledge-upload__status--success"><CircleCheck /></el-icon>
                  <el-tooltip v-if="item.status === 'failed'" :content="item.error || '上传失败'" placement="top">
                    <el-icon class="knowledge-upload__status knowledge-upload__status--failed"><CircleClose /></el-icon>
                  </el-tooltip>
                </div>
                <div class="knowledge-upload__queue-actions">
                  <el-button
                    v-if="item.status === 'failed'"
                    type="primary"
                    link
                    size="small"
                    @click="retrySingle(index)"
                  >重试</el-button>
                  <el-button
                    v-if="item.status === 'pending' || item.status === 'failed'"
                    type="danger"
                    link
                    size="small"
                    @click="removeFromQueue(index)"
                  >移除</el-button>
                </div>
              </div>
            </div>
          </template>
          <div v-else class="knowledge-upload__queue-empty">
            <EmptyState icon="Files" title="上传队列为空" description="拖拽文件或选择文件夹后，将在这里显示待上传文档" />
          </div>
        </div>

        <div class="knowledge-upload__footer">
          <el-button :disabled="batchQueue.length === 0 || batchUploading" @click="clearQueue">
            清空队列
          </el-button>
          <el-button
            type="primary"
            :loading="batchUploading"
            :disabled="pendingCount === 0"
            :icon="Upload"
            @click="handleBatchUpload"
          >
            <span>{{ batchUploading ? '上传中...' : `上传全部 (${pendingCount})` }}</span>
          </el-button>
        </div>
      </el-card>

      <el-card shadow="hover" class="knowledge-upload__panel knowledge-upload__panel--side">
        <template #header>
          <div class="knowledge-upload__panel-header">
            <div class="knowledge-upload__panel-title">
              <el-icon><Document /></el-icon>
              <span>已上传文件</span>
            </div>
            <span class="knowledge-upload__panel-meta">最近 {{ uploadFiles.length }} 个</span>
          </div>
        </template>

        <p class="knowledge-upload__side-desc">上传成功的文件将显示 5 分钟后自动清空</p>

        <div class="knowledge-upload__uploaded-wrap">
          <div v-if="uploadFiles.length === 0" class="knowledge-upload__uploaded-empty">
            <EmptyState icon="Document" title="暂无上传文件" description="上传成功后会显示在这里" />
          </div>

          <div v-else class="knowledge-upload__uploaded-list">
            <div
              v-for="(file, index) in uploadFiles"
              :key="file.id"
              class="knowledge-upload__uploaded-item"
              :class="{ 'knowledge-upload__uploaded-item--system': file.isSystem }"
            >
              <div class="knowledge-upload__uploaded-index">{{ String(index + 1).padStart(2, '0') }}</div>
              <div class="knowledge-upload__uploaded-info">
                <div class="knowledge-upload__uploaded-name" :title="file.filename">{{ file.filename }}</div>
                <div class="knowledge-upload__uploaded-meta">
                  <span class="knowledge-upload__uploaded-type">{{ file.sourceType }}</span>
                  <span class="knowledge-upload__uploaded-chunks">{{ file.chunkCount }} 个分块</span>
                </div>
                <div class="knowledge-upload__uploaded-time">{{ formatTime(file.updateTime) }}</div>
              </div>
              <div class="knowledge-upload__uploaded-status">
                <el-icon v-if="file.status === 1" class="knowledge-upload__status knowledge-upload__status--success"><CircleCheck /></el-icon>
                <el-icon v-else class="knowledge-upload__status knowledge-upload__status--pending"><Warning /></el-icon>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadInstance } from 'element-plus'
import { CircleCheck, CircleClose, Clock, Document, Files, FolderOpened, Loading, Refresh, Upload, UploadFilled, Warning } from '@element-plus/icons-vue'
import { createKnowledgeDocument, fetchKnowledgeDocuments } from '../../api/knowledge'
import { createRagReindex } from '../../api/ai'
import type { KnowledgeDocumentResponse, KnowledgeUploadResponse } from '../../types'
import { useUserStore } from '../../stores/user'
import PageHeader from '../../components/PageHeader.vue'
import EmptyState from '../../components/EmptyState.vue'
import { usePermission } from '../../composables/usePermission'

/* ── 常量 ── */
const acceptedTypes = '.pdf,.doc,.docx,.txt,.text,.md,.pptx'
const supportedFormats = ['PDF', 'DOC', 'DOCX', 'TXT', 'MD', 'PPTX']
const acceptedExtensions = new Set(['.pdf', '.doc', '.docx', '.txt', '.text', '.md', '.pptx'])
const UPLOAD_SHOW_WINDOW_MS = 5 * 60 * 1000

/* ── 批量上传队列项 ── */
interface BatchUploadItem {
  id: string
  file: File
  name: string
  size: number
  status: 'pending' | 'uploading' | 'success' | 'failed'
  result?: KnowledgeUploadResponse
  error?: string
}

/* ── 状态 ── */
const userStore = useUserStore()
const { isMainAdmin } = usePermission()

const uploadRef = ref<UploadInstance>()
const folderInputRef = ref<HTMLInputElement>()
const batchQueue = ref<BatchUploadItem[]>([])
const batchUploading = ref(false)
const batchCurrentIndex = ref(-1)
const reindexing = ref(false)
const uploadFiles = ref<KnowledgeDocumentResponse[]>([])
let cancelled = false
let clearTimer: ReturnType<typeof setTimeout> | null = null

/* ── 计算属性 ── */
const pendingCount = computed(() => batchQueue.value.filter(i => i.status === 'pending').length)
const batchSuccessCount = computed(() => batchQueue.value.filter(i => i.status === 'success').length)
const batchFailCount = computed(() => batchQueue.value.filter(i => i.status === 'failed').length)
const batchOverallPercent = computed(() => {
  if (batchQueue.value.length === 0) return 0
  const done = batchSuccessCount.value + batchFailCount.value
  return Math.round((done / batchQueue.value.length) * 100)
})

/* ── 工具函数 ── */
function getFileExtension(name: string): string {
  const idx = name.lastIndexOf('.')
  return idx >= 0 ? name.substring(idx).toLowerCase() : ''
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function makeId(): string {
  return Date.now().toString(36) + Math.random().toString(36).substring(2, 8)
}

/* ── 文件选择（拖拽 / 多选） ── */
function handleFileChange(file: UploadFile) {
  if (!file.raw) return
  addFileToQueue(file.raw)
}

/* ── 文件夹选择 ── */
function triggerFolderSelect() {
  folderInputRef.value?.click()
}

function handleFolderChange(event: Event) {
  const input = event.target as HTMLInputElement
  const files = input.files
  if (!files || files.length === 0) return

  let addedCount = 0
  for (const file of Array.from(files)) {
    const ext = getFileExtension(file.name)
    if (acceptedExtensions.has(ext)) {
      addFileToQueue(file)
      addedCount++
    }
  }

  if (addedCount > 0) {
    ElMessage.success(`已添加 ${addedCount} 个文件到上传队列`)
  } else {
    ElMessage.warning('文件夹中未找到支持的文件格式')
  }
  // 重置 input，保证同一文件夹可以再次选择
  input.value = ''
}

/* ── 添加文件到队列（去重） ── */
function addFileToQueue(file: File) {
  const exists = batchQueue.value.some(
    item => item.name === file.name && item.size === file.size
  )
  if (exists) return

  batchQueue.value.push({
    id: makeId(),
    file,
    name: file.name,
    size: file.size,
    status: 'pending',
  })
}

/* ── 从队列移除单个文件 ── */
function removeFromQueue(index: number) {
  batchQueue.value.splice(index, 1)
}

/* ── 重试单个失败文件 ── */
async function retrySingle(index: number) {
  const item = batchQueue.value[index]
  if (!item || item.status !== 'failed') return
  item.status = 'pending'
  item.error = undefined
  item.result = undefined
  await uploadSingleItem(item)
  await loadUploadFiles()
}

/* ── 清空队列 ── */
function clearQueue() {
  if (batchUploading.value) return
  batchQueue.value = []
}

/* ── 批量上传 ── */
async function handleBatchUpload() {
  if (batchUploading.value) return

  const pendingItems = batchQueue.value.filter(i => i.status === 'pending')
  if (pendingItems.length === 0) return

  batchUploading.value = true
  cancelled = false
  batchCurrentIndex.value = 0

  for (let i = 0; i < pendingItems.length; i++) {
    if (cancelled) break
    batchCurrentIndex.value = i
    await uploadSingleItem(pendingItems[i])
  }

  batchUploading.value = false
  batchCurrentIndex.value = -1

  if (batchSuccessCount.value > 0) {
    ElMessage.success(`成功上传 ${batchSuccessCount.value} 个文档`)
    await loadUploadFiles()
  }
  if (batchFailCount.value > 0) {
    ElMessage.warning(`${batchFailCount.value} 个文件上传失败，可点击重试`)
  }
}

/* ── 上传单个文件 ── */
async function uploadSingleItem(item: BatchUploadItem) {
  item.status = 'uploading'
  item.error = undefined
  item.result = undefined

  try {
    const data = new FormData()
    data.append('file', item.file)
    const res = await createKnowledgeDocument(data)
    item.result = res.data
    item.status = 'success'
  } catch (error: any) {
    item.status = 'failed'
    // 尝试从后端响应中提取错误信息
    const msg = error?.response?.data?.message || error?.message || '上传失败'
    item.error = msg
  }
}

/* ── 重建索引 ── */
async function handleReindex() {
  reindexing.value = true
  try {
    const res = await createRagReindex()
    ElMessage.success(`知识库已重建：${res.data.documentCount} 份文档，${res.data.chunkCount} 个片段`)
    await loadUploadFiles()
  } finally {
    reindexing.value = false
  }
}

/* ── 加载已上传文件列表 ── */
async function loadUploadFiles() {
  try {
    const res = await fetchKnowledgeDocuments()
    const all = (res.data || []).filter((d: KnowledgeDocumentResponse) =>
      d.sourcePath?.startsWith('uploads/')
    )
    // 只保留最近 5 分钟内上传/更新的文件，实现“5 分钟后自动清空”
    uploadFiles.value = filterRecentUploadFiles(all)
    scheduleUploadFilesRefresh()
  } catch {
    // 静默失败
  }
}

/* ── 过滤出仍在 5 分钟展示窗口内的文件 ── */
function filterRecentUploadFiles(files: KnowledgeDocumentResponse[]) {
  const now = Date.now()
  return files
    .filter((f) => {
      const t = f.updateTime ? new Date(f.updateTime).getTime() : now
      return now - t < UPLOAD_SHOW_WINDOW_MS
    })
    .sort((a, b) => {
      const ta = a.updateTime ? new Date(a.updateTime).getTime() : now
      const tb = b.updateTime ? new Date(b.updateTime).getTime() : now
      return tb - ta
    })
}

/* ── 调度展示窗口过期检查，在最早过期的文件到期时刷新列表 ── */
function scheduleUploadFilesRefresh() {
  if (clearTimer) {
    clearTimeout(clearTimer)
    clearTimer = null
  }
  if (uploadFiles.value.length === 0) return

  const now = Date.now()
  let nextExpireAt = Infinity
  for (const f of uploadFiles.value) {
    const t = f.updateTime ? new Date(f.updateTime).getTime() : now
    const expireAt = t + UPLOAD_SHOW_WINDOW_MS
    if (expireAt > now && expireAt < nextExpireAt) {
      nextExpireAt = expireAt
    }
  }
  if (nextExpireAt === Infinity) return

  const delay = Math.max(0, nextExpireAt - now + 100)
  clearTimer = setTimeout(() => {
    uploadFiles.value = filterRecentUploadFiles(uploadFiles.value)
    scheduleUploadFilesRefresh()
  }, delay)
}

/* ── 时间格式化 ── */
function formatTime(time?: string | Date) {
  if (!time) return '-'
  const d = new Date(time)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))} 分钟前`
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / (60 * 60 * 1000))} 小时前`
  if (diff < 7 * 24 * 60 * 60 * 1000) return `${Math.floor(diff / (24 * 60 * 60 * 1000))} 天前`
  return d.toLocaleDateString('zh-CN')
}

onBeforeUnmount(() => {
  cancelled = true
  // 清除定时器
  if (clearTimer) {
    clearTimeout(clearTimer)
    clearTimer = null
  }
})

onMounted(() => {
  loadUploadFiles()
})
</script>

<style scoped>
/* Unified workspace layout */
.knowledge-upload {
  height: max(640px, calc(100vh - var(--header-height) - var(--content-padding) * 2));
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 0;
  animation: fadeIn 0.4s ease-out;
}

.knowledge-upload__workspace {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.65fr);
  gap: var(--space-xl);
}

.knowledge-upload__panel {
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 0;
  border-radius: var(--radius-card);
  overflow: hidden;
}

.knowledge-upload__panel :deep(.el-card__header) {
  flex-shrink: 0;
}

.knowledge-upload__panel :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: var(--space-lg);
}

.knowledge-upload__panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
}

.knowledge-upload__panel-title,
.knowledge-upload__queue-title {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  font-weight: 600;
  color: var(--text-primary);
}

.knowledge-upload__panel-meta {
  color: var(--text-muted);
  font-size: var(--font-size-sm);
  white-space: nowrap;
}

.knowledge-upload__upload-area {
  flex-shrink: 0;
}

.knowledge-upload__drop :deep(.el-upload-dragger) {
  min-height: 156px;
  border-radius: var(--radius-card);
  border-color: var(--border-color);
  background: var(--bg-page);
}

.knowledge-upload__drop :deep(.el-upload-dragger:hover) {
  border-color: var(--color-primary);
  transform: none;
}

.knowledge-upload__drop-icon {
  margin-bottom: var(--space-md);
  color: var(--color-primary);
  font-size: 42px;
}

.knowledge-upload__drop-title {
  color: var(--text-primary);
  font-size: var(--font-size-lg);
  font-weight: 700;
}

.knowledge-upload__drop-tip,
.knowledge-upload__folder-hint,
.knowledge-upload__side-desc {
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.knowledge-upload__folder-row {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-top: var(--space-md);
}

.knowledge-upload__folder-input {
  display: none;
}

.knowledge-upload__format-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  margin-top: var(--space-md);
  padding: var(--space-md) var(--space-lg);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-card);
  background: var(--bg-toolbar);
}

.knowledge-upload__hint-copy {
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.6;
}

.knowledge-upload__format-list,
.knowledge-upload__queue-stats,
.knowledge-upload__queue-meta,
.knowledge-upload__uploaded-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
}

.knowledge-upload__format {
  padding: var(--space-xs) var(--space-sm);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-button);
  color: var(--text-secondary);
  background: var(--bg-page);
  font-size: var(--font-size-xs);
  font-weight: 700;
}

.knowledge-upload__queue-wrap,
.knowledge-upload__uploaded-wrap {
  flex: 1;
  min-height: 128px;
  margin-top: var(--space-lg);
  display: flex;
  flex-direction: column;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-card);
  background: var(--bg-page);
  overflow: hidden;
}

.knowledge-upload__queue-empty,
.knowledge-upload__uploaded-empty {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
}

.knowledge-upload__queue-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  padding: var(--space-md) var(--space-lg);
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-card);
}

.knowledge-upload__queue-stats {
  align-items: center;
  justify-content: flex-end;
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}

.knowledge-upload__stat--success,
.knowledge-upload__queue-chunks {
  color: var(--color-success);
}

.knowledge-upload__stat--fail,
.knowledge-upload__queue-error {
  color: var(--color-danger);
}

.knowledge-upload__progress {
  flex-shrink: 0;
  margin: var(--space-md) var(--space-lg) 0;
  padding: var(--space-md);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-button);
  background: var(--bg-card);
}

.knowledge-upload__progress-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-sm);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.knowledge-upload__queue-list,
.knowledge-upload__uploaded-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  overflow-y: auto;
  padding: var(--space-md);
}

.knowledge-upload__queue-item,
.knowledge-upload__uploaded-item {
  display: flex;
  align-items: flex-start;
  gap: var(--space-md);
  padding: var(--space-md);
  border-radius: var(--radius-table);
  border: 1px solid var(--border-light);
  background: var(--bg-card);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.knowledge-upload__queue-item:hover,
.knowledge-upload__uploaded-item:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-card);
}

.knowledge-upload__queue-index,
.knowledge-upload__uploaded-index {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-button);
  color: var(--text-inverse);
  background: var(--text-muted);
  font-size: var(--font-size-xs);
  font-weight: 700;
}

.knowledge-upload__queue-item--uploading .knowledge-upload__queue-index,
.knowledge-upload__uploaded-index {
  background: var(--color-primary);
}

.knowledge-upload__queue-item--success .knowledge-upload__queue-index {
  background: var(--color-success);
}

.knowledge-upload__queue-item--failed .knowledge-upload__queue-index {
  background: var(--color-danger);
}

.knowledge-upload__queue-info,
.knowledge-upload__uploaded-info {
  flex: 1;
  min-width: 0;
}

.knowledge-upload__queue-name,
.knowledge-upload__uploaded-name {
  color: var(--text-primary);
  font-size: var(--font-size-sm);
  font-weight: 600;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.knowledge-upload__queue-meta,
.knowledge-upload__uploaded-meta {
  margin-top: var(--space-xs);
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}

.knowledge-upload__queue-size,
.knowledge-upload__queue-chunks,
.knowledge-upload__uploaded-type,
.knowledge-upload__uploaded-chunks {
  padding: 1px var(--space-sm);
  border-radius: var(--radius-input);
  background: var(--bg-toolbar);
}

.knowledge-upload__queue-error {
  max-width: 220px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.knowledge-upload__queue-status,
.knowledge-upload__uploaded-status {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding-top: var(--space-xs);
}

.knowledge-upload__status {
  font-size: var(--font-size-xl);
}

.knowledge-upload__status--pending {
  color: var(--text-muted);
}

.knowledge-upload__status--uploading {
  color: var(--color-primary);
  animation: knowledge-upload-spin 1s linear infinite;
}

.knowledge-upload__status--success {
  color: var(--color-success);
}

.knowledge-upload__status--failed {
  color: var(--color-danger);
}

.knowledge-upload__queue-actions {
  flex-shrink: 0;
  display: flex;
  gap: var(--space-xs);
  padding-top: 2px;
}

.knowledge-upload__side-desc {
  flex-shrink: 0;
  margin: 0;
}

.knowledge-upload__uploaded-time {
  margin-top: var(--space-xs);
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}

.knowledge-upload__footer {
  flex-shrink: 0;
  margin-top: var(--space-lg);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-md);
}

@keyframes knowledge-upload-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 1180px) {
  .knowledge-upload {
    height: auto;
    min-height: max(640px, calc(100vh - var(--header-height) - var(--content-padding) * 2));
  }

  .knowledge-upload__workspace {
    grid-template-columns: 1fr;
    height: auto;
  }

  .knowledge-upload__panel {
    height: auto;
    min-height: 560px;
  }

  .knowledge-upload__queue-wrap,
  .knowledge-upload__uploaded-wrap {
    flex: none;
    height: clamp(360px, 46vh, 560px);
    min-height: 0;
  }
}

@media (max-width: 768px) {
  .knowledge-upload__panel-header,
  .knowledge-upload__folder-row,
  .knowledge-upload__format-hint,
  .knowledge-upload__queue-header,
  .knowledge-upload__footer {
    align-items: stretch;
    flex-direction: column;
  }

  .knowledge-upload__queue-wrap,
  .knowledge-upload__uploaded-wrap {
    height: 320px;
  }

  .knowledge-upload__queue-item,
  .knowledge-upload__uploaded-item {
    gap: var(--space-sm);
    padding: var(--space-sm);
  }

  .knowledge-upload__queue-actions {
    flex-direction: column;
  }

  .knowledge-upload__footer :deep(.el-button),
  .knowledge-upload__folder-row :deep(.el-button) {
    width: 100%;
  }
}
</style>
