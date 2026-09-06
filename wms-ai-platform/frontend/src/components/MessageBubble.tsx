import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { Bot, User, ThumbsUp, ThumbsDown, Clock, Link } from 'lucide-react'
import type { ChatMessage } from '../types'

interface Props {
  message: ChatMessage
  messageId: number
  onLike: (id: number) => void
  onDislike: (id: number) => void
  isStreaming?: boolean
}

export default function MessageBubble({ message, messageId, onLike, onDislike, isStreaming }: Props) {
  const isUser = message.role === 'user'

  return (
    <div className={`animate-slide-in flex gap-3 ${isUser ? 'flex-row-reverse' : ''}`}>
      {/* 头像 */}
      <div
        className={`w-9 h-9 rounded-full flex items-center justify-center shrink-0 ${
          isUser ? 'bg-primary-600' : 'bg-gray-200'
        }`}
      >
        {isUser ? <User className="w-4 h-4 text-white" /> : <Bot className="w-4 h-4 text-gray-600" />}
      </div>

      {/* 消息内容 */}
      <div className="flex-1 max-w-[85%] space-y-2">
        <div
          className={`rounded-2xl px-4 py-3 text-sm leading-relaxed ${
            isUser
              ? 'bg-primary-600 text-white'
              : 'bg-white border border-gray-200 text-gray-800'
          } ${isStreaming ? 'typing-cursor' : ''}`}
        >
          {isUser ? (
            <p className="whitespace-pre-wrap">{message.content}</p>
          ) : (
            <div className="prose prose-sm max-w-none">
              <ReactMarkdown remarkPlugins={[remarkGfm]}>{message.content || '...'}</ReactMarkdown>
            </div>
          )}
        </div>

        {/* 引用来源 */}
        {message.sources && message.sources.length > 0 && (
          <div className="space-y-1.5">
            {message.sources.map((src, i) => (
              <div
                key={i}
                className="flex items-center gap-2 text-xs text-gray-500 bg-gray-50 rounded-lg px-3 py-2 border border-gray-100"
              >
                <Link className="w-3 h-3 shrink-0" />
                <span className="font-medium text-gray-700 truncate">{src.title}</span>
                <span className="shrink-0">
                  相关度 {(src.relevance_score * 100).toFixed(0)}%
                </span>
                {src.path && <span className="text-primary-600 truncate">{src.path}</span>}
              </div>
            ))}
          </div>
        )}

        {/* 元数据 + 反馈 */}
        {!isUser && message.confidence !== undefined && (
          <div className="flex items-center gap-3 text-xs text-gray-400">
            <span>置信度 {(message.confidence * 100).toFixed(0)}%</span>
            {message.latency_ms && (
              <span className="flex items-center gap-1">
                <Clock className="w-3 h-3" />
                {message.latency_ms}ms
              </span>
            )}
            {!isStreaming && (
              <div className="flex gap-1">
                <button
                  onClick={() => onLike(messageId)}
                  className={`p-1 rounded transition-colors ${
                    message.feedback === 'like' ? 'text-green-600 bg-green-50' : 'hover:text-green-600 hover:bg-green-50'
                  }`}
                  title="有帮助"
                >
                  <ThumbsUp className="w-3.5 h-3.5" />
                </button>
                <button
                  onClick={() => onDislike(messageId)}
                  className={`p-1 rounded transition-colors ${
                    message.feedback === 'dislike' ? 'text-red-600 bg-red-50' : 'hover:text-red-600 hover:bg-red-50'
                  }`}
                  title="没帮助"
                >
                  <ThumbsDown className="w-3.5 h-3.5" />
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
