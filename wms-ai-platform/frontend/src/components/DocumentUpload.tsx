import { useCallback, useRef, useState } from 'react'
import { UploadCloud, FileText, Loader2, CheckCircle, XCircle } from 'lucide-react'
import { uploadDocument } from '../api'
import type { UploadResponse } from '../types'

interface Props {
  onUploaded: (doc: UploadResponse) => void
  category: string
  warehouse?: string
}

const ACCEPTED = '.pdf,.docx,.doc,.xlsx,.xls,.md,.txt,.png,.jpg,.jpeg,.bmp'
const MAX_SIZE = 50 * 1024 * 1024 // 50MB

export default function DocumentUpload({ onUploaded, category, warehouse }: Props) {
  const [isDragging, setIsDragging] = useState(false)
  const [_uploading, setUploading] = useState(false)
  const [progress, setProgress] = useState<{ name: string; status: 'uploading' | 'done' | 'error'; message?: string }[]>([])
  const fileInputRef = useRef<HTMLInputElement>(null)

  const handleFiles = useCallback(
    async (files: FileList) => {
      setUploading(true)
      const results: typeof progress = []
      for (const file of Array.from(files)) {
        const item = { name: file.name, status: 'uploading' as const }
        results.push(item)
        setProgress([...results])
        try {
          if (file.size > MAX_SIZE) {
            results[results.length - 1] = { ...item, status: 'error', message: '文件超过 50MB' }
            setProgress([...results])
            continue
          }
          const res = await uploadDocument(file, category, warehouse)
          results[results.length - 1] = { ...item, status: 'done', message: `${res.chunks} 块, ${res.triples} 三元组` }
          onUploaded(res)
        } catch (err: any) {
          results[results.length - 1] = { ...item, status: 'error', message: err.message || '上传失败' }
        }
        setProgress([...results])
      }
      setUploading(false)
    },
    [category, warehouse, onUploaded],
  )

  const onDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault()
      e.stopPropagation()
      setIsDragging(false)
      if (e.dataTransfer.files.length > 0) {
        handleFiles(e.dataTransfer.files)
      }
    },
    [handleFiles],
  )

  return (
    <div className="space-y-4">
      {/* 拖拽区域 */}
      <div
        className={`relative border-2 border-dashed rounded-xl p-8 text-center transition-all cursor-pointer ${
          isDragging ? 'drop-zone-active' : 'border-gray-300 hover:border-gray-400'
        }`}
        onDragOver={(e) => { e.preventDefault(); setIsDragging(true) }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={onDrop}
        onClick={() => fileInputRef.current?.click()}
      >
        <input
          ref={fileInputRef}
          type="file"
          multiple
          accept={ACCEPTED}
          className="hidden"
          onChange={(e) => e.target.files && handleFiles(e.target.files)}
        />
        <div className="flex flex-col items-center gap-2">
          <UploadCloud className="w-12 h-12 text-gray-400" />
          <p className="text-sm font-medium text-gray-700">拖拽文件到此处，或点击上传</p>
          <p className="text-xs text-gray-500">支持 PDF / Word / Excel / Markdown / 图片，单文件最大 50MB</p>
        </div>
      </div>

      {/* 上传进度 */}
      {progress.length > 0 && (
        <div className="space-y-2">
          {progress.map((p, i) => (
            <div key={i} className="flex items-center gap-3 p-3 bg-white rounded-lg border border-gray-200">
              {p.status === 'uploading' && <Loader2 className="w-4 h-4 text-primary-600 animate-spin" />}
              {p.status === 'done' && <CheckCircle className="w-4 h-4 text-green-600" />}
              {p.status === 'error' && <XCircle className="w-4 h-4 text-red-500" />}
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 truncate">{p.name}</p>
                {p.message && <p className="text-xs text-gray-500">{p.message}</p>}
              </div>
              <FileText className="w-4 h-4 text-gray-400 shrink-0" />
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
