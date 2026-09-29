<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import RichMessage from '@/components/RichMessage.vue'
import { createChatId } from '@/api/config'
import { bindEventSource, chatWithTravelAppSSE } from '@/api/ai'
import { chatStorageKey } from '@/utils/messageContent'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

const DEFAULT_GREETING: ChatMessage = {
  role: 'assistant',
  content:
    '你好，我是你的专属旅行管家。',
}

const chatId = ref(createChatId())
const input = ref('')
const loading = ref(false)
const messages = ref<ChatMessage[]>(loadMessages(chatId.value))
const listRef = ref<HTMLElement | null>(null)

function loadMessages(id: string): ChatMessage[] {
  try {
    const raw = sessionStorage.getItem(chatStorageKey(id))
    if (!raw) return [DEFAULT_GREETING]
    const parsed = JSON.parse(raw) as ChatMessage[]
    return Array.isArray(parsed) && parsed.length ? parsed : [DEFAULT_GREETING]
  } catch {
    return [DEFAULT_GREETING]
  }
}

function persistMessages() {
  sessionStorage.setItem(chatStorageKey(chatId.value), JSON.stringify(messages.value))
}

watch(messages, persistMessages, { deep: true })

async function scrollToBottom() {
  await nextTick()
  if (listRef.value) {
    listRef.value.scrollTop = listRef.value.scrollHeight
  }
}

function send() {
  const text = input.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  input.value = ''
  loading.value = true
  messages.value.push({ role: 'assistant', content: '' })
  const aiIndex = messages.value.length - 1
  scrollToBottom()

  const es = chatWithTravelAppSSE(text, chatId.value)
  bindEventSource(es, {
    onMessage: (data) => {
      messages.value[aiIndex].content += data
      scrollToBottom()
    },
    onDone: () => {
      loading.value = false
      if (!messages.value[aiIndex].content) {
        messages.value[aiIndex].content = '（未收到回复）'
      }
      persistMessages()
    },
    onError: (message) => {
      loading.value = false
      messages.value[aiIndex].content = message
      persistMessages()
    },
  })
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    send()
  }
}

onMounted(scrollToBottom)
</script>

<template>
  <section class="chat-page">
    <header class="chat-header">
      <h1>AI 旅行管家</h1>
      <p>多轮对话 · 行程建议 · 图片预览</p>
    </header>

    <div ref="listRef" class="chat-list">
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        class="bubble"
        :class="msg.role"
      >
        <div class="role">{{ msg.role === 'user' ? '我' : '管家' }}</div>
        <RichMessage :content="msg.content" />
      </div>
    </div>

    <footer class="chat-input">
      <textarea
        v-model="input"
        rows="2"
        placeholder=""
        :disabled="loading"
        @keydown="onKeydown"
      />
      <button :disabled="loading || !input.trim()" @click="send">
        {{ loading ? '思考中…' : '发送' }}
      </button>
    </footer>
  </section>
</template>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px);
  max-width: 860px;
  margin: 0 auto;
  padding: 16px;
}
.chat-header h1 {
  margin: 0;
  font-size: 1.5rem;
}
.chat-header p {
  margin: 6px 0 16px;
  color: #667085;
  font-size: 0.9rem;
}
.chat-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 4px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.bubble {
  max-width: 78%;
  padding: 10px 14px;
  border-radius: 14px;
  line-height: 1.55;
  word-break: break-word;
}
.bubble.user {
  align-self: flex-end;
  background: #0f766e;
  color: #fff;
}
.bubble.assistant {
  align-self: flex-start;
  background: #f2f4f7;
  color: #101828;
}
.role {
  font-size: 0.75rem;
  opacity: 0.7;
  margin-bottom: 4px;
}
.chat-input {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid #eaecf0;
}
textarea {
  resize: none;
  border: 1px solid #d0d5dd;
  border-radius: 10px;
  padding: 10px 12px;
  font: inherit;
}
button {
  border: none;
  border-radius: 10px;
  padding: 0 18px;
  background: #0f766e;
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}
button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
</style>
