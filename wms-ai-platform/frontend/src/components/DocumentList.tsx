import { useState } from 'react'
import { FileText, Trash2, Search, Loader2, X } from 'lucide-react'
import { listDocuments, deleteDocument } from '../api'
import type { KnowledgeDocument } from '../types'

interface Props {
  initialDocuments?: KnowledgeDocument[]
  refreshKey?: number
}

export default function DocumentList({ initialDocuments = [], refreshKey = 0 }: Props) {
  const [documents, setDocuments] = useState<KnowledgeDocument[]>(initialDocuments)
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [deleting, setDeleting] = useState<string | null>(null)

  // 当 refreshKey 变化时重新加载
  if (loading || refreshKey > 0) {
    // 首次加载或手动刷新
    if (documents.length === 0 && !loading) {
      // noop
    }
  }

  // 手动管理加载逻辑
  if (loading && documents.length === 0) {
    void (async () => {
      setLoading(true)
      try {
        const docs = await listDocuments()
        setDocuments(docs)
      } catch (err) {
        console.error('Failed to load documents', err)
      } finally {
        setLoading(false)
      }
    })()
  }

  const handleDelete = async (docId: string) => {
    setDeleting(docId)
    try {
      await deleteDocument(docId)
      setDocuments((prev) => prev.filter((d) => d.doc_id !== docId))
    } finally {
      setDeleting(null)
    }
  }

  const filtered = search
    ? documents.filter(
        (d) =>
          d.title.toLowerCase().includes(search.toLowerCase()) ||
          d.doc_id.toLowerCase().includes(search.toLowerCase()),
      )
    : documents

  const statusBadge = (status: string) => {
    switch (status) {
      case 'active':
        return <span className="px-2 py-0.5 text-xs rounded-full bg-green-100 text-green-700">活跃</span>
      case 'processing':
        return <span className="px-2 py-0.5 text-xs rounded-full bg-blue-100 text-blue-700">处理中</span>
      case 'error':
        return <span className="px-2 py-0.5 text-xs rounded-full bg-red-100 text-red-700">错误</span>
      default:
        return <span className="px-2 py-0.5 text-xs rounded-full bg-gray-100 text-gray-600">{status}</span>
    }
  }

  const fileTypeIcon = (type: string) => {
    if (type === 'pdf') return <span className="text-red-500 text-xs font-bold">PDF</span>
    if (type === 'docx' || type === 'doc') return <span className="text-blue-500 text-xs font-bold">DOC</span>
    if (type === 'xlsx' || type === 'xls') return <span className="text-green-500 text-xs font-bold">XLS</span>
    if (type === 'md') return <span className="text-gray-500 text-xs font-bold">MD</span>
    if (type === 'txt') return <span className="text-gray-500 text-xs font-bold">TXT</span>
    if (type.startsWith('image')) return <span className="text-purple-500 text-xs font-bold">IMG</span>
    return <span className="text-gray-400 text-xs">{type.toUpperCase()}</span>
  }

  if (loading) {
    return (
      <div className="space-y-3">
        {[1, 2, 3].map((i) => (
          <div key={i} className="h-14 skeleton rounded-lg" />
        ))}
      </div>
    )
  }

  if (documents.length === 0) {
    return (
      <div className="text-center py-12 text-gray-400">
        <FileText className="w-12 h-12 mx-auto mb-3" />
        <p>暂无文档，上传文件以构建知识库</p>
      </div>
    )
  }

  return (
    <div className="space-y-3">
      {/* 搜索 */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="搜索文档..."
          className="w-full pl-10 pr-4 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
        />
        {search && (
          <button onClick={() => setSearch('')} className="absolute right-3 top-1/2 -translate-y-1/2">
            <X className="w-4 h-4 text-gray-400 hover:text-gray-600" />
          </button>
        )}
      </div>

      {/* 文档列表 */}
      <div className="space-y-2">
        {filtered.map((doc) => (
          <div key={doc.doc_id} className="flex items-center gap-4 p-4 bg-white rounded-lg border border-gray-200 hover:border-primary-200 transition-colors">
            <div className="w-10 h-10 bg-gray-100 rounded-lg flex items-center justify-center shrink-0">
              <FileText className="w-5 h-5 text-gray-500" />
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <h3 className="text-sm font-medium text-gray-900 truncate">{doc.title}</h3>
                {fileTypeIcon(doc.file_type)}
              </div>
              <div className="flex items-center gap-3 text-xs text-gray-500 mt-0.5">
                <span>{doc.category}</span>
                <span>·</span>
                <span>{doc.chunk_count} 块</span>
                <span>·</span>
                <span>{(doc.char_count / 1000).toFixed(1)}k 字符</span>
                <span>·</span>
                <span>{new Date(doc.created_at).toLocaleDateString()}</span>
                {doc.warehouse && <span>· {doc.warehouse}</span>}
              </div>
            </div>
            <div className="flex items-center gap-2 shrink-0">
              {statusBadge(doc.status)}
              <button
                onClick={() => handleDelete(doc.doc_id)}
                disabled={deleting === doc.doc_id}
                className="p-1.5 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded transition-colors disabled:opacity-50"
                title="删除文档"
              >
                {deleting === doc.doc_id ? <Loader2 className="w-4 h-4 animate-spin" /> : <Trash2 className="w-4 h-4" />}
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
