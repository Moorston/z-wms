/**
 * WMS AI 平台前端类型定义
 */

// ===== 知识库文档 =====

export interface KnowledgeDocument {
  doc_id: string
  title: string
  category: string
  status: string
  file_type: string
  chunk_count: number
  char_count: number
  created_at: string
  created_by: string
  warehouse?: string
  tags?: string[]
}

export interface CategoryNode {
  name: string
  count: number
  children?: CategoryNode[]
}

// ===== 聊天 =====

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  sources?: SourceItem[]
  confidence?: number
  latency_ms?: number
  feedback?: 'like' | 'dislike'
}

export interface SourceItem {
  title: string
  content: string
  doc_id: string
  relevance_score: number
  sources: string[]
  path?: string
  entities?: string[]
  hop_count?: number
}

export interface ChatResponse {
  answer: string
  confidence: number
  latency_ms: number
  sources: SourceItem[]
  session_id: string
  model: string
  cached: boolean
}

// ===== SSE 流式事件 =====

export interface SSEMetaEvent {
  type: 'meta'
  sources: SourceItem[]
  session_id: string
}

export interface SSEChunkEvent {
  type: 'chunk'
  content: string
}

export interface SSEDoneEvent {
  type: 'done'
  answer: string
  confidence: number
}

export type SSEEvent = SSEMetaEvent | SSEChunkEvent | SSEDoneEvent

// ===== 上传 =====

export interface UploadResponse {
  doc_id: string
  title: string
  category: string
  status: string
  chunks: number
  triples: number
  file_type: string
  char_count: number
}

// ===== 监控 =====

export interface MonitorStats {
  total_queries: number
  avg_latency_ms: number
  cache_hit_rate: number
  retrieval_recall: number
  generation_fidelity: number
  error_rate: number
  active_sessions: number
}
