import { useState, useCallback } from 'react'
import DocumentUpload from '../components/DocumentUpload'
import DocumentList from '../components/DocumentList'
import type {} from '../types'

export default function DocumentsPage() {
  const [category, setCategory] = useState('general')
  const [warehouse, setWarehouse] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [tab, setTab] = useState<'upload' | 'list'>('upload')

  const handleUploaded = useCallback(() => {
    setRefreshKey((k) => k + 1)
    setTab('list')
  }, [])

  return (
    <div className="p-8 max-w-5xl mx-auto">
      <header className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">知识库管理</h1>
        <p className="text-sm text-gray-500 mt-1">上传文档，自动解析、分块、向量化、构建知识图谱</p>
      </header>

      {/* Tab 切换 */}
      <div className="flex gap-1 mb-6 bg-gray-100 rounded-lg p-1 w-fit">
        <button
          onClick={() => setTab('upload')}
          className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
            tab === 'upload' ? 'bg-white shadow text-gray-900' : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          上传文档
        </button>
        <button
          onClick={() => setTab('list')}
          className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
            tab === 'list' ? 'bg-white shadow text-gray-900' : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          文档列表
        </button>
      </div>

      {tab === 'upload' ? (
        <div className="space-y-6">
          {/* 分类和仓库配置 */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">分类</label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full px-3 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              >
                <option value="general">通用</option>
                <option value="warehouse">仓库管理</option>
                <option value="sop">SOP 流程</option>
                <option value="product">产品知识</option>
                <option value="safety">安全规范</option>
                <option value="equipment">设备知识</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">仓库（可选）</label>
              <input
                type="text"
                value={warehouse}
                onChange={(e) => setWarehouse(e.target.value)}
                placeholder="如：WH01, WH02"
                className="w-full px-3 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>
          </div>

          <DocumentUpload
            onUploaded={handleUploaded}
            category={category}
            warehouse={warehouse || undefined}
          />
        </div>
      ) : (
        <DocumentList refreshKey={refreshKey} />
      )}
    </div>
  )
}
