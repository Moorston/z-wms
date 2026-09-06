import { useEffect, useRef, useState } from 'react'
import { MessageSquare, Trash2, Bot } from 'lucide-react'
import { useChatStore } from '../stores/chatStore'
import MessageBubble from '../components/MessageBubble'
import ChatInput from '../components/ChatInput'

const SUGGESTIONS = [
  '仓库 WH01 的库存情况如何？',
  'SKU123 在哪些库位？',
  'WAVE2025 的波次包含哪些订单？',
  '拣货 SOP 流程是什么？',
]

export default function ChatPage() {
  const { messages, isLoading, isStreaming, sessionId, error, sendMessage, stopStreaming, likeMessage, dislikeMessage, clearChat } =
    useChatStore()
  const [warehouse, setWarehouse] = useState('')
  const bottomRef = useRef<HTMLDivElement>(null)

  // 自动滚动到底部
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  return (
    <div className="flex flex-col h-full">
      {/* 顶部栏 */}
      <header className="flex items-center justify-between px-6 py-4 bg-white border-b border-gray-200">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-primary-600 rounded-xl flex items-center justify-center">
            <MessageSquare className="w-5 h-5 text-white" />
          </div>
          <div>
            <h1 className="text-lg font-bold text-gray-900">智能问答</h1>
            <p className="text-xs text-gray-500">基于知识库的 RAG 对话 · 多路召回 + 重排序 + 知识图谱</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          {sessionId && (
            <span className="text-xs text-gray-400">Session: {sessionId.slice(0, 8)}...</span>
          )}
          {messages.length > 0 && (
            <button
              onClick={clearChat}
              className="flex items-center gap-1 px-3 py-1.5 text-xs text-gray-500 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
            >
              <Trash2 className="w-3.5 h-3.5" />
              清空
            </button>
          )}
        </div>
      </header>

      {/* 消息列表 */}
      <div className="flex-1 overflow-auto p-6 space-y-6">
        {messages.length === 0 ? (
          /* 空状态 */
          <div className="flex flex-col items-center justify-center h-full text-center">
            <div className="w-16 h-16 bg-primary-50 rounded-2xl flex items-center justify-center mb-4">
              <Bot className="w-8 h-8 text-primary-600" />
            </div>
            <h2 className="text-lg font-semibold text-gray-900 mb-1">开始对话</h2>
            <p className="text-sm text-gray-500 mb-6 max-w-md">
              基于知识库的智能问答，支持多轮对话、混合检索（向量+全文+知识图谱）和引用溯源
            </p>
            <div className="grid grid-cols-2 gap-2 max-w-lg w-full">
              {SUGGESTIONS.map((s, i) => (
                <button
                  key={i}
                  onClick={() => sendMessage(s, warehouse || undefined)}
                  className="text-left text-sm text-gray-700 bg-white border border-gray-200 rounded-lg px-4 py-3 hover:border-primary-300 hover:bg-primary-50 transition-colors"
                >
                  {s}
                </button>
              ))}
            </div>
          </div>
        ) : (
          <>
            {messages.map((msg, i) => (
              <MessageBubble
                key={msg.id}
                message={msg}
                messageId={i}
                onLike={likeMessage}
                onDislike={dislikeMessage}
                isStreaming={isStreaming && i === messages.length - 1}
              />
            ))}
            <div ref={bottomRef} />
          </>
        )}

        {/* 错误提示 */}
        {error && (
          <div className="text-center">
            <p className="text-sm text-red-500">{error}</p>
          </div>
        )}
      </div>

      {/* 输入区 */}
      <ChatInput
        onSend={(q) => sendMessage(q, warehouse || undefined)}
        onStop={stopStreaming}
        isLoading={isLoading}
        warehouse={warehouse}
        onWarehouseChange={setWarehouse}
      />
    </div>
  )
}
