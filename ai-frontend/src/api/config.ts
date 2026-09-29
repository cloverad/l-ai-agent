export const API_BASE = import.meta.env.VITE_API_BASE || '/api'

const CHAT_ID_KEY = 'travel-chat-id'

export function createChatId(): string {
  const existing = sessionStorage.getItem(CHAT_ID_KEY)
  if (existing) {
    return existing
  }
  const id = crypto.randomUUID()
  sessionStorage.setItem(CHAT_ID_KEY, id)
  return id
}

/** 开启新对话（清空当前会话缓存键） */
export function renewChatId(): string {
  const id = crypto.randomUUID()
  sessionStorage.setItem(CHAT_ID_KEY, id)
  return id
}
