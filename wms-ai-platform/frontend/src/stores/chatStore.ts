import { create } from 'zustand'
import type { ChatMessage } from '../types'
import { chatStream, sendFeedback } from '../api'

interface ChatState {
  messages: ChatMessage[]
  sessionId: string | null
  isLoading: boolean
  isStreaming: boolean
  error: string | null

  sendMessage: (question: string, warehouse?: string) => Promise<void>
  stopStreaming: () => void
  likeMessage: (messageId: number) => Promise<void>
  dislikeMessage: (messageId: number) => Promise<void>
  clearChat: () => void
}

let abortController: AbortController | null = null

export const useChatStore = create<ChatState>((set, get) => ({
  messages: [],
  sessionId: null,
  isLoading: false,
  isStreaming: false,
  error: null,

  sendMessage: async (question, warehouse) => {
    const { sessionId } = get()

    // 添加用户消息
    const userMsg: ChatMessage = {
      id: crypto.randomUUID(),
      role: 'user',
      content: question,
    }
    set((s) => ({
      messages: [...s.messages, userMsg],
      isLoading: true,
      error: null,
    }))

    try {
      abortController = new AbortController()
      const response = await chatStream(question, sessionId ?? undefined, warehouse)

      if (!response.ok || !response.body) {
        throw new Error(`HTTP ${response.status}`)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      const assistantId = crypto.randomUUID()
      const fullAnswer: string[] = []
      let sources: any[] = []
      let confidence = 0
      let newSessionId = sessionId

      // 创建空的 assistant 消息
      const assistantMsg: ChatMessage = {
        id: assistantId,
        role: 'assistant',
        content: '',
        sources: [],
        confidence: 0,
      }
      set((s) => ({ messages: [...s.messages, assistantMsg] }))

      let buffer = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          const trimmed = line.trim()
          if (!trimmed.startsWith('data:')) continue
          const data = trimmed.slice(5).trim()
          if (data === '[DONE]') continue

          try {
            const event = JSON.parse(data)
            if (event.type === 'meta') {
              sources = event.sources || []
              newSessionId = event.session_id || sessionId
            } else if (event.type === 'chunk') {
              fullAnswer.push(event.content)
              set((s) => ({
                isStreaming: true,
                messages: s.messages.map((m) =>
                  m.id === assistantId
                    ? { ...m, content: fullAnswer.join('') }
                    : m,
                ),
              }))
            } else if (event.type === 'done') {
              confidence = event.confidence
              set((s) => ({
                isLoading: false,
                isStreaming: false,
                sessionId: newSessionId,
                messages: s.messages.map((m) =>
                  m.id === assistantId
                    ? { ...m, content: event.answer, sources, confidence }
                    : m,
                ),
              }))
            }
          } catch {
            // 忽略解析错误
          }
        }
      }

      // 如果 done 事件没处理到，兜底
      if (get().isStreaming) {
        set({ isLoading: false, isStreaming: false })
      }
    } catch (err: any) {
      if (err.name === 'AbortError') {
        set({ isLoading: false, isStreaming: false })
      } else {
        set({
          isLoading: false,
          isStreaming: false,
          error: err.message || '请求失败，请稍后重试',
        })
      }
    }
  },

  stopStreaming: () => {
    abortController?.abort()
    set({ isLoading: false, isStreaming: false })
  },

  likeMessage: async (messageId) => {
    await sendFeedback(messageId, 'like')
    set((s) => ({
      messages: s.messages.map((m, i) =>
        i === messageId ? { ...m, feedback: 'like' } : m,
      ),
    }))
  },

  dislikeMessage: async (messageId) => {
    await sendFeedback(messageId, 'dislike')
    set((s) => ({
      messages: s.messages.map((m, i) =>
        i === messageId ? { ...m, feedback: 'dislike' } : m,
      ),
    }))
  },

  clearChat: () => set({ messages: [], sessionId: null, isLoading: false, isStreaming: false }),
}))
