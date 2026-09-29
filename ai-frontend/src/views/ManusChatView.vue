<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import ManusMessage from '@/components/ManusMessage.vue'
import { bindEventSource, chatWithManusSSE } from '@/api/ai'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  streaming?: boolean
}

const input = ref('')
const loading = ref(false)
const messages = ref<ChatMessage[]>([
  {
    role: 'assistant',
    content:
      '我是 TravelManus。给我一个旅行目标，我会分步帮你查资料、整理行程，需要时生成 PDF 供你下载。',
  },
])
const listRef = ref<HTMLElement | null>(null)

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
  messages.value.push({ role: 'assistant', content: '', streaming: true })
  const aiIndex = messages.value.length - 1
  scrollToBottom()

  const es = chatWithManusSSE(text)
  bindEventSource(es, {
    onMessage: (data) => {
      messages.value[aiIndex].content += data
      scrollToBottom()
    },
    onDone: () => {
      loading.value = false
      messages.value[aiIndex].streaming = false
      if (!messages.value[aiIndex].content) {
        messages.value[aiIndex].content = '（未收到回复）'
      }
    },
    onError: (message) => {
      loading.value = false
      messages.value[aiIndex].streaming = false
      messages.value[aiIndex].content = message
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
      <h1>TravelManus 智能体</h1>
      <p>分步 · 思考 · PDF 下载</p>
    </header>

    <div ref="listRef" class="chat-list">
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        class="bubble"
        :class="msg.role"
      >
        <div class="role">{{ msg.role === 'user' ? '我' : 'Manus' }}</div>
        <div v-if="msg.role === 'user'" class="user-text">{{ msg.content }}</div>
        <ManusMessage v-else :content="msg.content" :streaming="msg.streaming" />
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
        {{ loading ? '执行中…' : '发送' }}
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
  max-width: 86%;
  padding: 10px 14px;
  border-radius: 14px;
  line-height: 1.55;
  word-break: break-word;
}
.bubble.user {
  align-self: flex-end;
  background: #1d4ed8;
  color: #fff;
}
.bubble.assistant {
  align-self: flex-start;
  background: #f2f4f7;
  color: #101828;
  max-width: 92%;
}
.role {
  font-size: 0.75rem;
  opacity: 0.7;
  margin-bottom: 4px;
}
.user-text {
  white-space: pre-wrap;
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
  background: #1d4ed8;
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}
button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
</style>
