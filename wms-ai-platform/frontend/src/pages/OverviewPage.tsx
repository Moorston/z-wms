import { useEffect, useState } from 'react'
import {
  BarChart3,
  FileText,
  Clock,
  Zap,
  Target,
  AlertCircle,
  Users,
  BrainCircuit,
  Database,
  Loader2,
} from 'lucide-react'
import { getMonitorStats, listDocuments, getCategories } from '../api'
import type { MonitorStats, CategoryNode } from '../types'

export default function OverviewPage() {
  const [stats, setStats] = useState<MonitorStats | null>(null)
  const [docCount, setDocCount] = useState(0)
  const [categories, setCategories] = useState<CategoryNode[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    void (async () => {
      setLoading(true)
      try {
        const [monitorStats, docs, cats] = await Promise.allSettled([
          getMonitorStats(),
          listDocuments(),
          getCategories(),
        ])
        if (monitorStats.status === 'fulfilled') setStats(monitorStats.value)
        if (docs.status === 'fulfilled') setDocCount(docs.value.length)
        if (cats.status === 'fulfilled') setCategories(cats.value)
      } finally {
        setLoading(false)
      }
    })()
  }, [])

  if (loading) {
    return (
      <div className="p-8 flex items-center justify-center h-full">
        <Loader2 className="w-8 h-8 text-primary-600 animate-spin" />
      </div>
    )
  }

  const metricCards = [
    {
      label: '文档总数',
      value: docCount,
      icon: FileText,
      color: 'bg-blue-50 text-blue-600',
    },
    {
      label: '总查询量',
      value: stats?.total_queries ?? 0,
      icon: Zap,
      color: 'bg-green-50 text-green-600',
    },
    {
      label: '平均延迟',
      value: stats?.avg_latency_ms != null ? `${stats.avg_latency_ms.toFixed(0)}ms` : '—',
      icon: Clock,
      color: 'bg-purple-50 text-purple-600',
    },
    {
      label: '缓存命中率',
      value: stats?.cache_hit_rate != null ? `${(stats.cache_hit_rate * 100).toFixed(1)}%` : '—',
      icon: Database,
      color: 'bg-amber-50 text-amber-600',
    },
    {
      label: '检索召回率',
      value: stats?.retrieval_recall != null ? `${(stats.retrieval_recall * 100).toFixed(1)}%` : '—',
      icon: Target,
      color: 'bg-red-50 text-red-600',
    },
    {
      label: '生成保真度',
      value: stats?.generation_fidelity != null ? `${(stats.generation_fidelity * 100).toFixed(1)}%` : '—',
      icon: BrainCircuit,
      color: 'bg-teal-50 text-teal-600',
    },
    {
      label: '错误率',
      value: stats?.error_rate != null ? `${(stats.error_rate * 100).toFixed(2)}%` : '—',
      icon: AlertCircle,
      color: 'bg-rose-50 text-rose-600',
    },
    {
      label: '活跃会话',
      value: stats?.active_sessions ?? 0,
      icon: Users,
      color: 'bg-indigo-50 text-indigo-600',
    },
  ]

  return (
    <div className="p-8 max-w-6xl mx-auto">
      <header className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">系统概览</h1>
        <p className="text-sm text-gray-500 mt-1">WMS 智能知识平台运行状态</p>
      </header>

      {/* 指标卡片 */}
      <div className="grid grid-cols-4 gap-4 mb-8">
        {metricCards.map(({ label, value, icon: Icon, color }) => (
          <div key={label} className="bg-white rounded-xl border border-gray-200 p-5">
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-medium text-gray-500">{label}</span>
              <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${color}`}>
                <Icon className="w-4 h-4" />
              </div>
            </div>
            <p className="text-2xl font-bold text-gray-900">{value}</p>
          </div>
        ))}
      </div>

      {/* 下方双栏 */}
      <div className="grid grid-cols-2 gap-6">
        {/* 分类分布 */}
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <h2 className="text-sm font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <BarChart3 className="w-4 h-4 text-gray-400" />
            知识库分类分布
          </h2>
          {categories.length === 0 ? (
            <p className="text-sm text-gray-400 py-4 text-center">暂无分类数据</p>
          ) : (
            <div className="space-y-3">
              {categories.map((cat) => {
                const total = docCount || 1
                const pct = Math.round((cat.count / total) * 100)
                return (
                  <div key={cat.name}>
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-sm text-gray-700">{cat.name}</span>
                      <span className="text-xs text-gray-500">{cat.count} 文档 · {pct}%</span>
                    </div>
                    <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-primary-500 rounded-full transition-all"
                        style={{ width: `${pct}%` }}
                      />
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>

        {/* 系统架构说明 */}
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <h2 className="text-sm font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <BarChart3 className="w-4 h-4 text-gray-400" />
            系统架构
          </h2>
          <div className="space-y-3">
            {[
              { icon: '📥', label: '文档解析', desc: 'PDF / Word / Excel / Markdown / 图片' },
              { icon: '✂️', label: '智能分块', desc: '递归/结构化分块，512 tokens + 64 overlap' },
              { icon: '🧮', label: '向量化', desc: 'BGE-M3 嵌入模型，多维向量空间' },
              { icon: '🔍', label: '混合检索', desc: '向量 + 全文 + 知识图谱 三路召回' },
              { icon: '⚡', label: '知识图谱', desc: '实体关系抽取，1-2 跳 BFS 多跳展开' },
              { icon: '📊', label: '重排序', desc: 'BGE Reranker 交叉编码精排' },
              { icon: '💬', label: 'LLM 生成', desc: '多级降级，缓存加速，置信度评分' },
              { icon: '🔄', label: '多轮对话', desc: '上下文改写、代词消解、查询扩展' },
            ].map((item) => (
              <div key={item.label} className="flex items-center gap-3">
                <span className="text-lg">{item.icon}</span>
                <div>
                  <span className="text-sm font-medium text-gray-900">{item.label}</span>
                  <span className="text-xs text-gray-500 ml-2">{item.desc}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
