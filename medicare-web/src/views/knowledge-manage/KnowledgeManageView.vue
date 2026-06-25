<template>
  <div class="knowledge-manage">
    <PageHeader title="知识库管理" subtitle="查看、编辑、删除已上传的知识库文档，支持保存后自动重新检索。系统文件（灰暗行）仅可预览，不可修改。" />

    <el-card shadow="hover" class="data-card">
      <DataToolbar>
        <template #extra>
          <el-button :icon="Upload" :loading="systemUploading" type="warning" plain @click="handleSystemUploadClick">
            上传系统文件
          </el-button>
          <el-button :icon="DeleteFilled" :loading="clearingSystem" type="danger" plain @click="handleClearSystem">
            清空系统文件
          </el-button>
          <el-button :icon="Refresh" :loading="reindexing" @click="handleReindex">重建全部索引</el-button>
        </template>
      </DataToolbar>

      <div class="knowledge-manage__table-wrap">
        <el-table
          v-loading="loading"
          :data="displayDocuments"
          stripe
          border
          height="100%"
          style="width: 100%"
          :row-class-name="rowClassName"
          highlight-current-row
          :default-sort="{ prop: 'id', order: 'ascending' }"
          class="doc-table"
          @row-click="previewDocument"
        >
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column label="ID" width="124" align="center" class-name="knowledge-manage__tag-cell">
            <template #default="{ row }">
              <el-tag
                :type="row.isSystem ? 'info' : 'primary'"
                size="small"
                class="doc-tag"
              >
                {{ row.isSystem ? systemIndex(row) : userIndex(row) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="filename" label="文件名" show-overflow-tooltip min-width="180" />
          <el-table-column
            prop="sourceType"
            label="类型"
            width="124"
            align="center"
            class-name="knowledge-manage__tag-cell"
          >
            <template #header>
              <div class="table-filter-header">
                <span>类型</span>
                <el-dropdown trigger="click" :hide-on-click="false" popper-class="table-filter-dropdown">
                  <el-icon class="table-filter-icon" :class="{ 'is-active': activeFilters.sourceType.length }"><Filter /></el-icon>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-checkbox-group v-model="activeFilters.sourceType" class="table-filter-group">
                        <el-dropdown-item v-for="opt in typeFilters" :key="opt.value">
                          <el-checkbox :label="opt.value">{{ opt.text }}</el-checkbox>
                        </el-dropdown-item>
                      </el-checkbox-group>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
            <template #default="{ row }">
              <el-tag size="small" :effect="row.isSystem ? 'plain' : 'light'">{{ row.sourceType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="chunkCount" label="分块数" width="80" align="center" />
          <el-table-column label="来源" width="120" align="center">
            <template #header>
              <div class="table-filter-header">
                <span>来源</span>
                <el-dropdown trigger="click" :hide-on-click="false" popper-class="table-filter-dropdown">
                  <el-icon class="table-filter-icon" :class="{ 'is-active': activeFilters.isSystem.length }"><Filter /></el-icon>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-checkbox-group v-model="activeFilters.isSystem" class="table-filter-group">
                        <el-dropdown-item v-for="opt in sourceFilters" :key="String(opt.value)">
                          <el-checkbox :label="opt.value">{{ opt.text }}</el-checkbox>
                        </el-dropdown-item>
                      </el-checkbox-group>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
            <template #default="{ row }">
              <el-tag v-if="row.isSystem" type="warning" size="small" effect="plain">系统文件</el-tag>
              <el-tag v-else type="success" size="small" effect="plain">用户上传</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="更新时间" width="220" />
          <el-table-column label="操作" width="240" align="center" fixed="right">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button size="default" type="success" @click.stop="previewDocument(row)">预览</el-button>
                <template v-if="!row.isSystem">
                  <el-button size="default" type="warning" @click.stop="editDocument(row)">编辑</el-button>
                  <el-popconfirm title="确定删除该文档？删除后不可恢复" @confirm="deleteDocument(row)">
                    <template #reference><el-button size="default" type="danger" @click.stop>删除</el-button></template>
                  </el-popconfirm>
                </template>
                <template v-else>
                  <el-button size="default" disabled>编辑</el-button>
                  <el-button size="default" disabled>删除</el-button>
                </template>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <EmptyState
        v-if="!loading && displayDocuments.length === 0"
        icon="Document"
        title="暂无知识库文档"
        description="请前往「知识库上传」页面添加文档，或执行重建索引以扫描系统文件"
      />
    </el-card>

    <!-- 系统文件上传弹窗 -->
    <el-dialog v-model="systemUploadDialogVisible" title="上传系统文件" width="520px" destroy-on-close>
      <el-upload
        ref="systemUploadRef"
        drag
        :auto-upload="false"
        multiple
        directory
        :file-list="systemFileList"
        :on-change="handleSystemFileChange"
        :on-remove="handleSystemFileRemove"
        accept=".pdf,.doc,.docx,.txt,.md,.json"
        class="upload-drop"
      >
        <el-icon class="upload-drop-icon"><Upload /></el-icon>
        <div class="upload-drop-title">拖拽文件/文件夹到这里，或点击选择</div>
        <div class="upload-drop-tip">支持单个/多个文件上传和文件夹批量上传</div>
      </el-upload>
      <template #footer>
        <el-button @click="systemUploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="systemUploading" :disabled="systemSelectedFiles.length === 0" @click="handleSystemUploadSubmit">上传</el-button>
      </template>
    </el-dialog>

    <!-- 预览弹窗 -->
    <el-dialog
      v-model="previewDialogVisible"
      title="文档预览"
      width="720px"
      destroy-on-close
    >
      <div class="doc-info-bar">
        <el-tag size="small" type="info">{{ currentDoc?.filename }}</el-tag>
        <el-tag size="small" :type="currentDoc?.isSystem ? 'warning' : 'success'">
          {{ currentDoc?.isSystem ? '系统文件 · 只读' : '用户上传' }}
        </el-tag>
        <el-tag size="small">{{ currentDoc?.chunkCount }} 个分块</el-tag>
      </div>
      <div class="preview-readonly preview-readonly--markdown" v-html="renderedDocContent"></div>
      <template #footer>
        <el-button @click="previewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑文档"
      width="720px"
      destroy-on-close
    >
      <div class="doc-info-bar">
        <el-tag size="small" type="info">{{ currentDoc?.filename }}</el-tag>
        <el-tag size="small" type="success">{{ currentDoc?.sourceType }}</el-tag>
        <el-tag size="small">{{ currentDoc?.chunkCount }} 个分块</el-tag>
      </div>
      <el-input
        v-model="docContent"
        type="textarea"
        :rows="18"
        resize="none"
        placeholder="文档内容..."
      />
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="saveDocument">保存并重新检索</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import MarkdownIt from 'markdown-it'
import type { UploadFile, UploadFiles, UploadInstance, UploadRawFile, UploadUserFile } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DeleteFilled, Filter, Refresh, Upload } from '@element-plus/icons-vue'
import { createRagReindex } from '../../api/ai'
import {
  fetchAllKnowledgeDocuments,
  fetchKnowledgeDocument,
  updateKnowledgeDocument,
  deleteKnowledgeDocument,
  clearSystemKnowledgeDocuments,
  uploadSystemKnowledgeDocument
} from '../../api/knowledge'
import type { KnowledgeDocumentResponse } from '../../types'
import PageHeader from '../../components/PageHeader.vue'
import DataToolbar from '../../components/DataToolbar.vue'
import EmptyState from '../../components/EmptyState.vue'

const loading = ref(false)
const reindexing = ref(false)
const clearingSystem = ref(false)
const documents = ref<KnowledgeDocumentResponse[]>([])
const previewDialogVisible = ref(false)
const editDialogVisible = ref(false)
const currentDoc = ref<KnowledgeDocumentResponse | null>(null)
const docContent = ref('')
const saveLoading = ref(false)

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true
})

const renderedDocContent = computed(() => {
  if (!docContent.value) return ''
  return markdown.render(docContent.value)
})

const systemUploadDialogVisible = ref(false)
const systemUploadRef = ref<UploadInstance>()
const systemFileList = ref<UploadUserFile[]>([])
const systemSelectedFiles = ref<UploadRawFile[]>([])
const systemUploading = ref(false)

// 系统文件和用户文件分别编号
const systemFiles = computed(() => documents.value.filter(d => d.isSystem))
const userFiles = computed(() => documents.value.filter(d => !d.isSystem))

// 表头下拉筛选选项
const sourceFilters = [
  { text: '系统文件', value: true },
  { text: '用户上传', value: false },
]
const typeFilters = computed(() => {
  const types = new Set(documents.value.map(d => d.sourceType).filter(Boolean))
  return Array.from(types).map(t => ({ text: t, value: t }))
})

const activeFilters = ref<{
  sourceType: string[]
  isSystem: boolean[]
}>({
  sourceType: [],
  isSystem: [],
})

const displayDocuments = computed(() => {
  return documents.value.filter(d => {
    if (activeFilters.value.sourceType.length && !activeFilters.value.sourceType.includes(d.sourceType)) {
      return false
    }
    if (activeFilters.value.isSystem.length && !activeFilters.value.isSystem.includes(!!d.isSystem)) {
      return false
    }
    return true
  })
})

function systemIndex(row: KnowledgeDocumentResponse) {
  // 优先从标题中的 SYS-001 / SYS001 - filename 解析编号
  const match = row.filename?.match(/^SYS-?(\d{3})\s+-\s+/i)
  if (match) {
    return `SYS-${match[1]}`
  }
  // 对于 systemRAGFiles 等没有 SYS 前缀的系统文件，按系统文件列表顺序生成编号
  const idx = systemFiles.value.findIndex(d => d.id === row.id)
  if (idx >= 0) {
    return `SYS-${String(idx + 1).padStart(3, '0')}`
  }
  return 'SYS'
}

function userIndex(row: KnowledgeDocumentResponse) {
  const idx = userFiles.value.findIndex(d => d.id === row.id)
  return `UP-${String(idx + 1).padStart(3, '0')}`
}

function rowClassName({ row }: { row: KnowledgeDocumentResponse }) {
  return row.isSystem ? 'system-row' : ''
}

async function loadDocuments() {
  loading.value = true
  try {
    const res = await fetchAllKnowledgeDocuments()
    // 排序：用户上传在前，系统文件在后，同组内按 id 升序
    const docs = res.data || []
    documents.value = docs.sort((a: KnowledgeDocumentResponse, b: KnowledgeDocumentResponse) => {
      if (a.isSystem && !b.isSystem) return 1
      if (!a.isSystem && b.isSystem) return -1
      return (a.id ?? 0) - (b.id ?? 0)
    })
  } catch {
    ElMessage.error('加载文档失败')
  }
  loading.value = false
}

function handleSystemUploadClick() {
  systemUploadDialogVisible.value = true
  systemSelectedFiles.value = []
  systemFileList.value = []
}

function handleSystemFileChange(file: UploadFile, files: UploadFiles) {
  systemFileList.value = files
  systemSelectedFiles.value = files.map(f => f.raw).filter((f): f is UploadRawFile => !!f)
}

function handleSystemFileRemove(file: UploadFile, files: UploadFiles) {
  systemFileList.value = files
  systemSelectedFiles.value = files.map(f => f.raw).filter((f): f is UploadRawFile => !!f)
}

async function handleSystemUploadSubmit() {
  if (systemSelectedFiles.value.length === 0) return
  systemUploading.value = true
  try {
    const data = new FormData()
    systemSelectedFiles.value.forEach(file => {
      data.append('files', file)
    })
    const res = await uploadSystemKnowledgeDocument(data)
    ElMessage.success(res.data.message || '系统文件上传成功')
    systemUploadDialogVisible.value = false
    await loadDocuments()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '系统文件上传失败')
  }
  systemUploading.value = false
}

async function previewDocument(doc: KnowledgeDocumentResponse) {
  try {
    const res = await fetchKnowledgeDocument(doc.id!)
    currentDoc.value = doc
    docContent.value = res.data.content
    previewDialogVisible.value = true
  } catch {
    ElMessage.error('加载文档内容失败')
  }
}

async function editDocument(doc: KnowledgeDocumentResponse) {
  if (doc.isSystem) {
    ElMessage.warning('系统文件不可编辑')
    return
  }
  try {
    const res = await fetchKnowledgeDocument(doc.id!)
    currentDoc.value = doc
    docContent.value = res.data.content
    editDialogVisible.value = true
  } catch {
    ElMessage.error('加载文档内容失败')
  }
}

async function saveDocument() {
  if (!currentDoc.value) return
  if (currentDoc.value.isSystem) {
    ElMessage.warning('系统文件不可编辑')
    return
  }
  saveLoading.value = true
  try {
    await updateKnowledgeDocument(currentDoc.value.id!, docContent.value)
    ElMessage.success('文档已保存并重新检索')
    editDialogVisible.value = false
    await loadDocuments()
  } catch {
    ElMessage.error('保存失败')
  }
  saveLoading.value = false
}

async function deleteDocument(doc: KnowledgeDocumentResponse) {
  if (doc.isSystem) {
    ElMessage.warning('系统文件不可删除')
    return
  }
  try {
    await deleteKnowledgeDocument(doc.id!)
    ElMessage.success('删除成功')
    await loadDocuments()
  } catch {
    ElMessage.error('删除失败')
  }
}

async function handleReindex() {
  reindexing.value = true
  try {
    const res = await createRagReindex()
    ElMessage.success(`知识库已重建：${res.data.documentCount} 份文档，${res.data.chunkCount} 个片段`)
    await loadDocuments()
  } catch {
    ElMessage.error('重建失败')
  }
  reindexing.value = false
}

async function handleClearSystem() {
  try {
    await ElMessageBox.confirm(
      '确定清空所有系统文件（SYS）及其向量索引吗？用户上传文档不会被删除。',
      '清空系统文件',
      {
        confirmButtonText: '确定清空',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }
  clearingSystem.value = true
  try {
    await clearSystemKnowledgeDocuments()
    ElMessage.success('已清空所有系统文件及向量索引')
    await loadDocuments()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '清空系统文件失败')
  }
  clearingSystem.value = false
}

onMounted(() => {
  loadDocuments()
})
</script>

<style scoped>
.knowledge-manage {
  width: 100%;
  height: max(560px, calc(100vh - var(--header-height) - var(--content-padding) * 2));
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  min-width: 0;
  min-height: 0;
  animation: fadeIn 0.4s ease-out;
}

.knowledge-manage :deep(.data-card) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.knowledge-manage :deep(.data-card .el-card__body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: var(--space-lg);
}

.knowledge-manage__table-wrap {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.action-buttons {
  display: flex;
  flex-wrap: nowrap;
  justify-content: center;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.doc-info-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.preview-readonly {
  white-space: pre-wrap;
  line-height: 1.6;
  max-height: 480px;
  overflow-y: auto;
  padding: 16px;
  background: var(--bg-light);
  border-radius: 8px;
  font-size: 13px;
  color: var(--text-primary);
  border: 1px solid var(--border-light);
}

.preview-readonly--markdown {
  white-space: normal;
}

.preview-readonly--markdown :deep(h1),
.preview-readonly--markdown :deep(h2),
.preview-readonly--markdown :deep(h3),
.preview-readonly--markdown :deep(h4),
.preview-readonly--markdown :deep(h5),
.preview-readonly--markdown :deep(h6) {
  margin-top: 16px;
  margin-bottom: 12px;
  color: var(--text-primary);
  line-height: 1.4;
}

.preview-readonly--markdown :deep(h1) { font-size: 1.6em; }
.preview-readonly--markdown :deep(h2) { font-size: 1.4em; }
.preview-readonly--markdown :deep(h3) { font-size: 1.2em; }
.preview-readonly--markdown :deep(h4),
.preview-readonly--markdown :deep(h5),
.preview-readonly--markdown :deep(h6) { font-size: 1.1em; }

.preview-readonly--markdown :deep(p) {
  margin: 0 0 12px 0;
  line-height: 1.7;
}

.preview-readonly--markdown :deep(ul),
.preview-readonly--markdown :deep(ol) {
  margin: 0 0 12px 0;
  padding-left: 24px;
}

.preview-readonly--markdown :deep(li) {
  margin-bottom: 4px;
  line-height: 1.7;
}

.preview-readonly--markdown :deep(code) {
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--bg-page);
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
}

.preview-readonly--markdown :deep(pre) {
  margin: 0 0 12px 0;
  padding: 12px;
  border-radius: 8px;
  background: var(--bg-page);
  overflow-x: auto;
}

.preview-readonly--markdown :deep(pre code) {
  padding: 0;
  background: transparent;
}

.preview-readonly--markdown :deep(blockquote) {
  margin: 0 0 12px 0;
  padding: 8px 16px;
  border-left: 4px solid var(--color-primary);
  background: var(--bg-toolbar);
  color: var(--text-secondary);
}

.preview-readonly--markdown :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 12px;
}

.preview-readonly--markdown :deep(th),
.preview-readonly--markdown :deep(td) {
  padding: 8px 12px;
  border: 1px solid var(--border-light);
  text-align: left;
}

.preview-readonly--markdown :deep(th) {
  background: var(--bg-toolbar);
  font-weight: 600;
}

.preview-readonly--markdown :deep(a) {
  color: var(--color-primary);
  text-decoration: none;
}

.preview-readonly--markdown :deep(a:hover) {
  text-decoration: underline;
}

.preview-readonly--markdown :deep(hr) {
  border: none;
  border-top: 1px solid var(--border-light);
  margin: 16px 0;
}

.preview-readonly--markdown :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
}

.upload-drop :deep(.el-upload-dragger) {
  min-height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  border-color: var(--border-color);
  background: var(--bg-page);
  transition: border-color 0.2s ease;
}

.upload-drop :deep(.el-upload-dragger:hover) {
  border-color: var(--color-primary);
}

.upload-drop-icon {
  margin-bottom: 12px;
  color: var(--color-primary);
  font-size: 40px;
}

.upload-drop-title {
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 600;
}

.upload-drop-tip {
  margin-top: 6px;
  color: var(--text-muted);
  font-size: 13px;
}

.doc-tag {
  font-family: 'Courier New', monospace;
  font-weight: 700;
  letter-spacing: 0.5px;
}

:deep(.knowledge-manage__tag-cell .cell) {
  overflow: visible;
  text-overflow: clip;
  white-space: nowrap;
}

/* 系统文件行样式 */
:deep(.system-row) {
  background-color: #f5f5f5 !important;
}

:deep(.system-row td) {
  color: #999 !important;
}

:deep(.system-row:hover td) {
  background-color: #eeeeee !important;
}

:deep(.system-row .el-tag) {
  opacity: 0.7;
}

</style>
