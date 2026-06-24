<template>
  <div class="knowledge-manage">
    <PageHeader title="知识库管理" subtitle="查看、编辑、删除已上传的知识库文档，支持保存后自动重新检索。系统文件（灰暗行）仅可预览，不可修改。" />

    <el-card shadow="hover" class="data-card">
      <DataToolbar
        show-add
        add-label="上传新文档"
        @add="handleUploadClick"
      >
        <template #extra>
          <el-button :icon="Refresh" :loading="reindexing" @click="handleReindex">重建全部索引</el-button>
        </template>
      </DataToolbar>

      <el-table
        v-loading="loading"
        :data="documents"
        stripe
        border
        :row-class-name="rowClassName"
        class="doc-table"
      >
        <el-table-column label="编号" width="90" align="center">
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
        <el-table-column prop="sourceType" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :effect="row.isSystem ? 'plain' : 'light'">{{ row.sourceType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="分块数" width="80" align="center" />
        <el-table-column label="来源" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isSystem" type="warning" size="small" effect="plain">系统文件</el-tag>
            <el-tag v-else type="success" size="small" effect="plain">用户上传</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="200" />
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="default" type="success" @click="previewDocument(row)">预览</el-button>
              <template v-if="!row.isSystem">
                <el-button size="default" type="warning" @click="editDocument(row)">编辑</el-button>
                <el-popconfirm title="确定删除该文档？删除后不可恢复" @confirm="deleteDocument(row)">
                  <template #reference><el-button size="default" type="danger">删除</el-button></template>
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

      <EmptyState
        v-if="!loading && documents.length === 0"
        icon="Document"
        title="暂无知识库文档"
        description="请前往「知识库上传」页面添加文档，或执行重建索引以扫描系统文件"
      />
    </el-card>

    <!-- 上传弹窗 -->
    <el-dialog v-model="uploadDialogVisible" title="上传新文档" width="520px" destroy-on-close>
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :limit="1"
        :file-list="fileList"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        accept=".pdf,.doc,.docx,.txt,.md,.json"
        class="upload-drop"
      >
        <el-icon class="upload-drop-icon"><Upload /></el-icon>
        <div class="upload-drop-title">拖拽文件到这里，或点击选择</div>
        <div class="upload-drop-tip">支持 PDF、DOC、DOCX、TXT、MD、JSON</div>
      </el-upload>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" :disabled="!selectedFile" @click="handleUploadSubmit">上传</el-button>
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
      <div class="preview-readonly">{{ docContent }}</div>
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
import type { UploadFile, UploadFiles, UploadInstance, UploadUserFile } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Upload } from '@element-plus/icons-vue'
import { createRagReindex } from '../../api/ai'
import {
  fetchAllKnowledgeDocuments,
  fetchKnowledgeDocument,
  updateKnowledgeDocument,
  deleteKnowledgeDocument,
  createKnowledgeDocument
} from '../../api/knowledge'
import type { KnowledgeDocumentResponse } from '../../types'
import PageHeader from '../../components/PageHeader.vue'
import DataToolbar from '../../components/DataToolbar.vue'
import EmptyState from '../../components/EmptyState.vue'

const loading = ref(false)
const reindexing = ref(false)
const documents = ref<KnowledgeDocumentResponse[]>([])
const uploadDialogVisible = ref(false)
const previewDialogVisible = ref(false)
const editDialogVisible = ref(false)
const currentDoc = ref<KnowledgeDocumentResponse | null>(null)
const docContent = ref('')
const saveLoading = ref(false)

const uploadRef = ref<UploadInstance>()
const fileList = ref<UploadUserFile[]>([])
const selectedFile = ref<File | null>(null)
const uploading = ref(false)

// 系统文件和用户文件分别编号
const systemFiles = computed(() => documents.value.filter(d => d.isSystem))
const userFiles = computed(() => documents.value.filter(d => !d.isSystem))

function systemIndex(row: KnowledgeDocumentResponse) {
  const idx = systemFiles.value.findIndex(d => d.id === row.id)
  return `SYS-${String(idx + 1).padStart(3, '0')}`
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
    // 排序：系统文件在前，用户文件在后，各自按更新时间倒序
    const docs = res.data || []
    documents.value = docs.sort((a: KnowledgeDocumentResponse, b: KnowledgeDocumentResponse) => {
      if (a.isSystem && !b.isSystem) return -1
      if (!a.isSystem && b.isSystem) return 1
      const ta = new Date(a.updateTime || 0).getTime()
      const tb = new Date(b.updateTime || 0).getTime()
      return tb - ta
    })
  } catch {
    ElMessage.error('加载文档失败')
  }
  loading.value = false
}

function handleUploadClick() {
  uploadDialogVisible.value = true
  selectedFile.value = null
  fileList.value = []
}

function handleFileChange(file: UploadFile, files: UploadFiles) {
  fileList.value = files.slice(-1)
  selectedFile.value = file.raw || null
}

function handleFileRemove() {
  selectedFile.value = null
}

async function handleUploadSubmit() {
  if (!selectedFile.value) return
  uploading.value = true
  try {
    const data = new FormData()
    data.append('file', selectedFile.value)
    await createKnowledgeDocument(data)
    ElMessage.success('上传成功')
    uploadDialogVisible.value = false
    await loadDocuments()
  } catch {
    ElMessage.error('上传失败')
  }
  uploading.value = false
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

onMounted(() => {
  loadDocuments()
})
</script>

<style scoped>
.knowledge-manage {
  animation: fadeIn 0.4s ease-out;
}

.data-card {
  border-radius: var(--radius-lg);
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
