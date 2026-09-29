<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import RichMessage from '@/components/RichMessage.vue'
import { parseManusContent, pdfDownloadUrl } from '@/utils/manusContent'

const props = defineProps<{
  content: string
  /** 流式进行中时思考区默认展开 */
  streaming?: boolean
}>()

const section = computed(() => parseManusContent(props.content))
const expanded = ref(true)

watch(
  () => section.value.thinkDone,
  (done) => {
    if (done) expanded.value = false
  },
)

watch(
  () => props.streaming,
  (s) => {
    if (s) expanded.value = true
  },
)
</script>

<template>
  <div class="manus-msg">
    <div v-if="section.thinkLines.length" class="think-panel">
      <button type="button" class="think-toggle" @click="expanded = !expanded">
        <span class="chev">{{ expanded ? '▼' : '▶' }}</span>
        <span class="think-title">
          {{ section.thinkDone ? '思考过程（点击展开/收起）' : '正在思考…' }}
        </span>
        <span class="think-meta">{{ section.thinkLines.length }} 步</span>
      </button>
      <div v-show="expanded" class="think-body">
        <p v-for="(line, i) in section.thinkLines" :key="i" class="think-line">{{ line }}</p>
      </div>
    </div>

    <div v-if="section.answer" class="answer-panel">
      <RichMessage :content="section.answer" />
    </div>

    <div v-if="section.pdfs.length" class="pdf-list">
      <a
        v-for="name in section.pdfs"
        :key="name"
        class="pdf-card"
        :href="pdfDownloadUrl(name)"
        :download="name"
      >
        <span class="pdf-icon" aria-hidden="true">📄</span>
        <span class="pdf-meta">
          <span class="pdf-name">{{ name }}</span>
          <span class="pdf-hint">点击下载到本地</span>
        </span>
      </a>
    </div>

    <!-- 尚无结构化内容时原样展示（开场白等） -->
    <RichMessage
      v-if="!section.thinkLines.length && !section.answer && !section.pdfs.length"
      :content="content"
    />
  </div>
</template>

<style scoped>
.manus-msg {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.think-panel {
  border: 1px solid #e4e7ec;
  border-radius: 12px;
  background: #fafafa;
  overflow: hidden;
}
.think-toggle {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  border: none;
  background: #f2f4f7;
  padding: 8px 12px;
  cursor: pointer;
  font: inherit;
  text-align: left;
  color: #344054;
}
.chev {
  font-size: 0.7rem;
  color: #667085;
}
.think-title {
  flex: 1;
  font-size: 0.85rem;
  font-weight: 600;
}
.think-meta {
  font-size: 0.75rem;
  color: #98a2b3;
}
.think-body {
  padding: 10px 12px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.think-line {
  margin: 0;
  font-size: 0.88rem;
  line-height: 1.5;
  color: #475467;
}
.answer-panel {
  padding-top: 2px;
}
.pdf-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.pdf-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #d0d5dd;
  border-radius: 12px;
  background: #fff;
  text-decoration: none;
  color: inherit;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.pdf-card:hover {
  border-color: #1d4ed8;
  box-shadow: 0 2px 8px rgba(29, 78, 216, 0.12);
}
.pdf-icon {
  font-size: 1.75rem;
  line-height: 1;
}
.pdf-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.pdf-name {
  font-weight: 600;
  font-size: 0.95rem;
  color: #101828;
  word-break: break-all;
}
.pdf-hint {
  font-size: 0.78rem;
  color: #667085;
}
</style>
