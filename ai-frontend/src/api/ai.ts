import { API_BASE } from './config'

/** 旅行管家 SSE 对话 */
export function chatWithTravelAppSSE(message: string, chatId: string): EventSource {
  const url = `${API_BASE}/ai/travel_app/chat/sse?message=${encodeURIComponent(message)}&chatId=${encodeURIComponent(chatId)}`
  return new EventSource(url)
}

/** TravelManus 智能体 SSE */
export function chatWithManusSSE(message: string): EventSource {
  const url = `${API_BASE}/ai/manus/chat?message=${encodeURIComponent(message)}`
  return new EventSource(url)
}

export async function healthCheck(): Promise<string> {
  const res = await fetch(`${API_BASE}/health`)
  return res.text()
}

/**
 * 订阅 SSE：完成时回调 onDone；失败时 onError。
 * 注意：浏览器在服务端正常关闭 SSE 时也可能触发 onerror。
 */
export function bindEventSource(
  es: EventSource,
  handlers: {
    onMessage: (data: string) => void
    onDone: () => void
    onError: (message: string) => void
  },
): void {
  let received = false
  es.onmessage = (event) => {
    received = true
    handlers.onMessage(event.data ?? '')
  }
  es.onerror = () => {
    // readyState CLOSED 通常表示流结束
    if (es.readyState === EventSource.CLOSED) {
      handlers.onDone()
    } else if (!received) {
      handlers.onError('无法连接后端 SSE，请确认服务已启动（http://localhost:8123）')
    } else {
      handlers.onDone()
    }
    es.close()
  }
}
