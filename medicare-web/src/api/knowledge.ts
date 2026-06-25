import request from './index'
import type { KnowledgeDocumentResponse, KnowledgeSystemUploadBatchResponse, KnowledgeUploadResponse, Result } from '../types'

export interface KnowledgeDocContent {
  id: number
  filename: string
  sourcePath: string
  sourceType: string
  content: string
  chunkCount: number
  status: number
  isSystem: boolean
  createTime: string
  updateTime: string
}

/** 查询已上传的文档（仅用户上传） */
export function fetchKnowledgeDocuments() {
  return request.get<any, Result<KnowledgeDocumentResponse[]>>('/knowledge/documents')
}

/** 查询所有知识库文档（含系统文件） */
export function fetchAllKnowledgeDocuments() {
  return request.get<any, Result<KnowledgeDocumentResponse[]>>('/knowledge/documents/all')
}

/** 获取文档详情及内容预览 */
export function fetchKnowledgeDocument(id: number) {
  return request.get<any, Result<KnowledgeDocContent>>(`/knowledge/documents/${id}`)
}

/** 获取文档详情及内容预览（AI 助手引用来源，登录用户即可访问） */
export function fetchKnowledgeDocumentPreview(id: number) {
  return request.get<any, Result<KnowledgeDocContent>>(`/knowledge/documents/${id}/preview`)
}

/** 上传文档 */
export function createKnowledgeDocument(data: FormData) {
  return request.post<any, Result<KnowledgeUploadResponse>>('/knowledge/documents/upload', data, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 120000
  })
}

/** AI 助手上传文件（登录用户即可使用） */
export function createAssistantKnowledgeDocument(data: FormData) {
  return request.post<any, Result<KnowledgeUploadResponse>>('/knowledge/documents/assistant-upload', data, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 120000
  })
}

/** 更新文档内容 */
export function updateKnowledgeDocument(id: number, content: string) {
  return request.put<any, Result<KnowledgeDocumentResponse>>(`/knowledge/documents/${id}`, { content })
}

/** 删除文档 */
export function deleteKnowledgeDocument(id: number) {
  return request.delete<any, Result<void>>(`/knowledge/documents/${id}`)
}

/** 清空所有系统文件及已索引的向量数据 */
export function clearSystemKnowledgeDocuments() {
  return request.delete<any, Result<void>>('/knowledge/documents/system/clear')
}

/** 上传系统文件（支持单个/多个文件和文件夹批量上传） */
export function uploadSystemKnowledgeDocument(data: FormData) {
  return request.post<any, Result<KnowledgeSystemUploadBatchResponse>>('/knowledge/documents/system/upload', data, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 300000
  })
}
