import { useState, useRef, useEffect } from 'react'
import { Send, Square } from 'lucide-react'

interface Props {
  onSend: (question: string) => void
  onStop: () => void
  isLoading: boolean
  warehouse?: string
  onWarehouseChange: (v: string) => void
}

export default function ChatInput({ onSend, onStop, isLoading, warehouse, onWarehouseChange }: Props) {
  const [input, setInput] = useState('')
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  // 自动调整高度
  useEffect(() => {
    const ta = textareaRef.current
    if (ta) {
      ta.style.height = 'auto'
      ta.style.height = Math.min(ta.scrollHeight, 160) + 'px'
    }
  }, [input])

  const handleSend = () => {
    const q = input.trim()
    if (!q || isLoading) return
    onSend(q)
    setInput('')
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <div className="border-t border-gray-200 bg-white p-4 space-y-3">
      {/* 仓库选择 */}
      <div className="flex items-center gap-2">
        <label className="text-xs text-gray-500 shrink-0">仓库：</label>
        <input
          type="text"
          value={warehouse}
          onChange={(e) => onWarehouseChange(e.target.value)}
          placeholder="留空表示全局查询"
          className="flex-1 px-3 py-1.5 text-xs border border-gray-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-primary-500"
        />
      </div>

      {/* 输入框 */}
      <div className="flex items-end gap-3">
        <textarea
          ref={textareaRef}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="输入问题，Enter 发送，Shift+Enter 换行"
          rows={1}
          className="flex-1 px-4 py-3 text-sm border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
        />
        {isLoading ? (
          <button
            onClick={onStop}
            className="p-3 bg-red-500 text-white rounded-xl hover:bg-red-600 transition-colors"
            title="停止生成"
          >
            <Square className="w-5 h-5" />
          </button>
        ) : (
          <button
            onClick={handleSend}
            disabled={!input.trim()}
            className="p-3 bg-primary-600 text-white rounded-xl hover:bg-primary-700 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
            title="发送"
          >
            <Send className="w-5 h-5" />
          </button>
        )}
      </div>
    </div>
  )
}
