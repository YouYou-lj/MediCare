<template>
  <div class="knowledge-upload">
    <!-- 页面头部 -->
    <section class="knowledge-upload__header">
      <div class="knowledge-upload__title-group">
        <p class="knowledge-upload__eyebrow">RAG 文档知识库</p>
        <h1 class="knowledge-upload__title">知识库上传</h1>
        <p class="knowledge-upload__subtitle">
          上传门诊制度、操作说明、培训材料或业务文档，系统会解析文本并写入 AI 知识库。支持批量上传和文件夹上传。
        </p>
      </div>
      <el-button :loading="reindexing" class="knowledge-upload__reindex" type="primary" @click="handleReindex">
        <el-icon><Refresh /></el-icon>
        <span>重建全部索引</span>
      </el-button>
    </section>

    <!-- 两栏主体 -->
    <section class="knowledge-upload__body">
      <!-- 左栏 2/3：上传区域 -->
      <div class="knowledge-upload__panel knowledge-upload__panel--main">
        <div class="panel-title">
          <el-icon><UploadFilled /></el-icon>
          <span>文档上传</span>
        </div>

        <!-- 拖拽区 -->
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

        <!-- 文件夹上传按钮 -->
        <div class="knowledge-upload__folder-row">
          <el-button type="primary" plain @click="triggerFolderSelect">
            <el-icon><FolderOpened /></el-icon>
            <span>选择文件夹</span>
          </el-button>
          <span class="knowledge-upload__folder-hint">自动筛选文件夹中支持的文件格式</span>
          <!-- 隐藏的文件夹选择 input -->
          <input
            ref="folderInputRef"
            type="file"
            webkitdirectory
            multiple
            style="display: none"
            @change="handleFolderChange"
          />
        </div>

        <div class="knowledge-upload__format-hint">
          <div class="knowledge-upload__hint-copy">
            支持格式：PDF、DOC、DOCX、TXT、MD、PPTX
          </div>
          <div class="knowledge-upload__format-list" aria-label="支持上传格式">
            <span v-for="format in supportedFormats" :key="format" class="knowledge-upload__format">{{ format }}</span>
          </div>
        </div>

        <!-- 上传队列 -->
        <div v-if="batchQueue.length > 0" class="batch-queue">
          <div class="batch-queue__header">
            <span class="batch-queue__title">上传队列</span>
            <el-tag size="small" type="info">{{ batchQueue.length }} 个文件</el-tag>
            <span class="batch-queue__stats">
              <span v-if="batchSuccessCount > 0" class="batch-stat batch-stat--success">成功 {{ batchSuccessCount }}</span>
              <span v-if="batchFailCount > 0" class="batch-stat batch-stat--fail">失败 {{ batchFailCount }}</span>
            </span>
          </div>

          <!-- 整体进度条 -->
          <div v-if="batchUploading" class="batch-progress">
            <div class="batch-progress__header">
              <span class="batch-progress__text">正在上传 {{ batchCurrentIndex + 1 }} / {{ batchQueue.length }}</span>
              <span class="batch-progress__percent">{{ batchOverallPercent }}%</span>
            </div>
            <el-progress
              :percentage="batchOverallPercent"
              :status="batchFailCount > 0 && !batchUploading ? 'exception' : undefined"
              :stroke-width="8"
              :show-text="false"
            />
          </div>

          <!-- 文件列表 -->
          <div class="batch-queue__list">
            <div
              v-for="(item, index) in batchQueue"
              :key="item.id"
              class="batch-item"
              :class="`batch-item--${item.status}`"
            >
              <div class="batch-item__index">{{ String(index + 1).padStart(2, '0') }}</div>
              <div class="batch-item__info">
                <div class="batch-item__name" :title="item.name">{{ item.name }}</div>
                <div class="batch-item__meta">
                  <span class="batch-item__size">{{ formatFileSize(item.size) }}</span>
                  <span v-if="item.result" class="batch-item__chunks">{{ item.result.chunkCount }} 个分块</span>
                  <span v-if="item.error" class="batch-item__error">{{ item.error }}</span>
                </div>
              </div>
              <div class="batch-item__status">
                <!-- 等待中 -->
                <el-icon v-if="item.status === 'pending'" class="batch-status batch-status--pending"><Clock /></el-icon>
                <!-- 上传中 -->
                <el-icon v-if="item.status === 'uploading'" class="batch-status batch-status--uploading"><Loading /></el-icon>
                <!-- 成功 -->
                <el-icon v-if="item.status === 'success'" class="batch-status batch-status--success"><CircleCheck /></el-icon>
                <!-- 失败 -->
                <el-tooltip v-if="item.status === 'failed'" :content="item.error || '上传失败'" placement="top">
                  <el-icon class="batch-status batch-status--failed"><CircleClose /></el-icon>
                </el-tooltip>
              </div>
              <div class="batch-item__actions">
                <!-- 失败重试 -->
                <el-button
                  v-if="item.status === 'failed'"
                  type="primary"
                  link
                  size="small"
                  @click="retrySingle(index)"
                >重试</el-button>
                <!-- 移除（仅等待中或失败时可移除） -->
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
        </div>

        <!-- 操作按钮 -->
        <div class="knowledge-upload__actions">
          <el-button :disabled="batchQueue.length === 0 || batchUploading" @click="clearQueue">
            清空队列
          </el-button>
          <el-button
            type="primary"
            :loading="batchUploading"
            :disabled="pendingCount === 0"
            @click="handleBatchUpload"
          >
            <el-icon><Upload /></el-icon>
            <span>{{ batchUploading ? '上传中...' : `上传全部 (${pendingCount})` }}</span>
          </el-button>
        </div>
      </div>

      <!-- 右栏 1/3：已上传文件状态列表 -->
      <div class="knowledge-upload__panel knowledge-upload__panel--side">
        <div class="panel-title">
          <el-icon><Document /></el-icon>
          <span>已上传文件</span>
          <el-tag size="small" type="info" class="upload-count">{{ uploadFiles.length }} 个</el-tag>
        </div>
        <p class="side-desc">仅显示通过本页面上传的文档</p>

        <div v-if="uploadFiles.length === 0" class="knowledge-upload__empty">
          <el-empty description="暂无上传文件" :image-size="80" />
        </div>

        <div v-else class="upload-file-list">
          <div
            v-for="(file, index) in uploadFiles"
            :key="file.id"
            class="upload-file-item"
            :class="{ 'is-system': file.isSystem }"
          >
            <div class="upload-file-index">{{ String(index + 1).padStart(2, '0') }}</div>
            <div class="upload-file-info">
              <div class="upload-file-name" :title="file.filename">{{ file.filename }}</div>
              <div class="upload-file-meta">
                <span class="upload-file-type">{{ file.sourceType }}</span>
                <span class="upload-file-chunks">{{ file.chunkCount }} 个分块</span>
              </div>
              <div class="upload-file-time">{{ formatTime(file.updateTime) }}</div>
            </div>
            <div class="upload-file-status">
              <el-icon v-if="file.status === 1" class="status-icon status-success"><CircleCheck /></el-icon>
              <el-icon v-else class="status-icon status-pending"><Warning /></el-icon>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadInstance } from 'element-plus'
import { Document, Refresh, Upload, CircleCheck, CircleClose, Warning, Clock, Loading, FolderOpened } from '@element-plus/icons-vue'
import { createKnowledgeDocument, fetchKnowledgeDocuments } from '../../api/knowledge'
import { createRagReindex } from '../../api/ai'
import type { KnowledgeDocumentResponse, KnowledgeUploadResponse } from '../../types'

/* ── 常量 ── */
const acceptedTypes = '.pdf,.doc,.docx,.txt,.text,.md,.pptx'
const supportedFormats = ['PDF', 'DOC', 'DOCX', 'TXT', 'MD', 'PPTX']
const acceptedExtensions = new Set(['.pdf', '.doc', '.docx', '.txt', '.text', '.md', '.pptx'])

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
const uploadRef = ref<UploadInstance>()
const folderInputRef = ref<HTMLInputElement>()
const batchQueue = ref<BatchUploadItem[]>([])
const batchUploading = ref(false)
const batchCurrentIndex = ref(-1)
const reindexing = ref(false)
const uploadFiles = ref<KnowledgeDocumentResponse[]>([])
let cancelled = false

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
    uploadFiles.value = (res.data || []).filter((d: KnowledgeDocumentResponse) =>
      d.sourcePath?.startsWith('uploads/')
    )
  } catch {
    // 静默失败
  }
}

/* ── 时间格式化 ── */
function formatTime(time?: string | Date) {
  if (!time) return '-'
  const d = new Date(time)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))} 分钟前`
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / (24 * 60 * 60 * 1000))} 小时前`
  if (diff < 7 * 24 * 60 * 60 * 1000) return `${Math.floor(diff / (24 * 60 * 60 * 1000))} 天前`
  return d.toLocaleDateString('zh-CN')
}

onBeforeUnmount(() => {
  cancelled = true
})

onMounted(() => {
  loadUploadFiles()
})
</script>

<style scoped>
.knowledge-upload {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.knowledge-upload__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding: 28px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background:
    linear-gradient(120deg, var(--bg-card), var(--bg-page)),
    var(--bg-card);
  box-shadow: var(--shadow-card);
}

.knowledge-upload__title-group {
  min-width: 0;
}

.knowledge-upload__eyebrow {
  margin: 0 0 8px;
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 700;
}

.knowledge-upload__title {
  margin: 0;
  color: var(--text-primary);
  font-size: 28px;
  line-height: 1.25;
}

.knowledge-upload__subtitle {
  margin: 10px 0 0;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.knowledge-upload__reindex {
  flex-shrink: 0;
}

/* 两栏布局 */
.knowledge-upload__body {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  align-items: start;
}

.knowledge-upload__panel {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-card);
  box-shadow: var(--shadow-card);
  padding: 22px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.panel-title .el-icon {
  color: var(--color-primary);
  font-size: 18px;
}

.upload-count {
  margin-left: auto;
  font-weight: 400;
}

.side-desc {
  margin: -8px 0 16px;
  color: var(--text-muted);
  font-size: 12px;
}

/* 拖拽上传区 */
.knowledge-upload__drop {
  width: 100%;
}

.knowledge-upload__drop :deep(.el-upload-dragger) {
  min-height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  border-color: var(--border-color);
  background: var(--bg-page);
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.knowledge-upload__drop :deep(.el-upload-dragger:hover) {
  border-color: var(--color-primary);
  transform: translateY(-2px);
}

.knowledge-upload__drop-icon {
  margin-bottom: 14px;
  color: var(--color-primary);
  font-size: 44px;
}

.knowledge-upload__drop-title {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 700;
}

.knowledge-upload__drop-tip {
  margin-top: 8px;
  color: var(--text-muted);
  font-size: 13px;
}

/* 文件夹上传行 */
.knowledge-upload__folder-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.knowledge-upload__folder-hint {
  color: var(--text-muted);
  font-size: 12px;
}

/* 格式提示 */
.knowledge-upload__format-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 14px;
  padding: 14px 16px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-toolbar);
}

.knowledge-upload__hint-copy {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.knowledge-upload__format-list {
  display: flex;
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.knowledge-upload__format {
  padding: 6px 10px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  color: var(--text-secondary);
  background: var(--bg-page);
  font-size: 12px;
  font-weight: 700;
}

/* ── 批量上传队列 ── */
.batch-queue {
  margin-top: 18px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-page);
  padding: 16px;
}

.batch-queue__header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.batch-queue__title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.batch-queue__stats {
  margin-left: auto;
  display: flex;
  gap: 12px;
  font-size: 12px;
}

.batch-stat--success {
  color: var(--color-success, #67c23a);
}

.batch-stat--fail {
  color: var(--color-danger, #f56c6c);
}

/* 整体进度 */
.batch-progress {
  margin-bottom: 14px;
  padding: 10px 14px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-card);
}

.batch-progress__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.batch-progress__text {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.batch-progress__percent {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
}

/* 队列列表 */
.batch-queue__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 360px;
  overflow-y: auto;
  padding-right: 4px;
}

.batch-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 6px;
  border: 1px solid var(--border-light);
  background: var(--bg-card);
  transition: all 0.2s ease;
}

.batch-item:hover {
  border-color: var(--color-primary);
}

.batch-item__index {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: var(--text-muted);
}

.batch-item--pending .batch-item__index {
  background: var(--text-muted);
}

.batch-item--uploading .batch-item__index {
  background: var(--color-primary);
}

.batch-item--success .batch-item__index {
  background: var(--color-success, #67c23a);
}

.batch-item--failed .batch-item__index {
  background: var(--color-danger, #f56c6c);
}

.batch-item__info {
  flex: 1;
  min-width: 0;
}

.batch-item__name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}

.batch-item__meta {
  display: flex;
  gap: 8px;
  margin-top: 2px;
  font-size: 11px;
  color: var(--text-muted);
}

.batch-item__size {
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--bg-toolbar);
}

.batch-item__chunks {
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--bg-toolbar);
  color: var(--color-success, #67c23a);
}

.batch-item__error {
  color: var(--color-danger, #f56c6c);
  max-width: 200px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.batch-item__status {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.batch-status {
  font-size: 18px;
}

.batch-status--pending {
  color: var(--text-muted);
}

.batch-status--uploading {
  color: var(--color-primary);
  animation: spin 1s linear infinite;
}

.batch-status--success {
  color: var(--color-success, #67c23a);
}

.batch-status--failed {
  color: var(--color-danger, #f56c6c);
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.batch-item__actions {
  flex-shrink: 0;
  display: flex;
  gap: 4px;
}

/* 操作按钮 */
.knowledge-upload__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 18px;
}

/* 右侧文件列表 */
.knowledge-upload__empty {
  padding: 20px 0;
}

.upload-file-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 520px;
  overflow-y: auto;
  padding-right: 4px;
}

.upload-file-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 8px;
  border: 1px solid var(--border-light);
  background: var(--bg-page);
  transition: all 0.2s ease;
}

.upload-file-item:hover {
  border-color: var(--color-primary);
  box-shadow: 0 2px 8px rgba(15, 159, 143, 0.08);
}

.upload-file-index {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: var(--color-primary);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.upload-file-info {
  flex: 1;
  min-width: 0;
}

.upload-file-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}

.upload-file-meta {
  display: flex;
  gap: 8px;
  margin-top: 4px;
  font-size: 11px;
  color: var(--text-muted);
}

.upload-file-type {
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--bg-toolbar);
  font-weight: 500;
}

.upload-file-chunks {
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--bg-toolbar);
}

.upload-file-time {
  margin-top: 4px;
  font-size: 11px;
  color: var(--text-muted);
}

.upload-file-status {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding-top: 2px;
}

.status-icon {
  font-size: 18px;
}

.status-success {
  color: var(--color-success, #67c23a);
}

.status-pending {
  color: var(--color-warning, #e6a23c);
}

/* 响应式 */
@media (max-width: 960px) {
  .knowledge-upload__header {
    align-items: stretch;
    flex-direction: column;
  }

  .knowledge-upload__body {
    grid-template-columns: 1fr;
  }

  .knowledge-upload__format-hint {
    align-items: flex-start;
    flex-direction: column;
  }

  .knowledge-upload__format-list {
    justify-content: flex-start;
  }

  .upload-file-list,
  .batch-queue__list {
    max-height: 360px;
  }
}
</style>
