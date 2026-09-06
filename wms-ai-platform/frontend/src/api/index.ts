import axios from 'axios'
import type {
  ChatResponse,
  KnowledgeDocument,
  CategoryNode,
  UploadResponse,
  MonitorStats,
} from '../types'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/rag'

const http = axios.create({
  baseURL: BASE_URL,
  timeout: 120_000,
})

// ===== 文档管理 =====

export async function uploadDocument(
  file: File,
  category: string = 'general',
  warehouse?: string,
): Promise<UploadResponse> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('category', category)
  if (warehouse) formData.append('warehouse', warehouse)

  const res = await http.post<ApiResult<UploadResponse>>('/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return res.data.data
}

export async function listDocuments(
  category?: string,
  status?: string,
  limit = 100,
  offset = 0,
): Promise<KnowledgeDocument[]> {
  const res = await http.get<ApiResult<KnowledgeDocument[]>>('/documents', {
    params: { category, status, limit, offset },
  })
  return res.data.data
}

export async function deleteDocument(docId: string): Promise<void> {
  await http.delete(`/documents/${docId}`)
}

export async function getCategories(): Promise<CategoryNode[]> {
  const res = await http.get<ApiResult<CategoryNode[]>>('/categories')
  return res.data.data
}

// ===== 聊天 =====

export async function chat(
  question: string,
  sessionId?: string,
  warehouse?: string,
): Promise<ChatResponse> {
  const res = await http.post<ApiResult<ChatResponse>>('/chat', {
    question,
    session_id: sessionId,
    warehouse,
  })
  return res.data.data
}

/**
 * 流式聊天 — 返回 ReadableStream，消费者逐行解析 SSE
 */
export async function chatStream(
  question: string,
  sessionId?: string,
  warehouse?: string,
): Promise<Response> {
  return fetch(`${BASE_URL}/chat/stream`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ question, session_id: sessionId, warehouse }),
  })
}

// ===== 反馈 =====

export async function sendFeedback(
  messageId: number,
  feedback: 'like' | 'dislike',
  note?: string,
): Promise<void> {
  await http.post('/feedback', {
    message_id: messageId,
    feedback,
    feedback_note: note,
  })
}

// ===== 监控 =====

export async function getMonitorStats(): Promise<MonitorStats> {
  const res = await http.get<ApiResult<MonitorStats>>('/monitor/stats')
  return res.data.data
}

// ===== 工具 =====

interface ApiResult<T> {
  code: number
  message: string
  data: T
}
