<script setup lang="ts">
import { computed } from 'vue'
import { parseMessageParts } from '@/utils/messageContent'

const props = defineProps<{
  content: string
}>()

const parts = computed(() => parseMessageParts(props.content))

function onImgError(e: Event) {
  const el = e.target as HTMLImageElement
  el.style.display = 'none'
  const fallback = el.nextElementSibling as HTMLElement | null
  if (fallback) fallback.style.display = 'block'
}
</script>

<template>
  <div class="rich-content">
    <template v-for="(part, i) in parts" :key="i">
      <p v-if="part.type === 'text'" class="text">{{ part.text }}</p>

      <figure v-else-if="part.type === 'image'" class="image-wrap">
        <img
          :src="part.url"
          :alt="part.alt"
          loading="lazy"
          referrerpolicy="no-referrer"
          @error="onImgError"
        />
        <a class="img-fallback" :href="part.url" target="_blank" rel="noopener" style="display: none">
          打开图片
        </a>
        <figcaption v-if="part.alt && part.alt !== '图片'">{{ part.alt }}</figcaption>
      </figure>

      <div v-else-if="part.type === 'gallery'" class="gallery">
        <p v-if="part.note" class="gallery-note">⚠️ {{ part.note }}</p>
        <div class="gallery-grid">
          <a
            v-for="(item, j) in part.items"
            :key="j"
            class="card"
            :href="item.url"
            target="_blank"
            rel="noopener"
          >
            <img
              :src="item.thumb || item.url"
              :alt="item.credit || '图片'"
              loading="lazy"
              referrerpolicy="no-referrer"
              @error="onImgError"
            />
            <span class="img-fallback" style="display: none">查看链接</span>
            <span v-if="item.credit" class="credit">{{ item.credit }}</span>
          </a>
        </div>
      </div>

      <div v-else-if="part.type === 'table'" class="table-wrap">
        <table>
          <thead>
            <tr>
              <th v-for="(h, hi) in part.headers" :key="hi">{{ h }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, ri) in part.rows" :key="ri">
              <td v-for="(cell, ci) in row" :key="ci">{{ cell }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<style scoped>
.rich-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}
.image-wrap {
  margin: 0;
  padding: 0;
}
.image-wrap img,
.card img {
  display: block;
  width: 100%;
  max-height: 180px;
  object-fit: cover;
  border-radius: 10px;
  background: #e4e7ec;
}
.image-wrap img {
  max-width: min(100%, 420px);
  max-height: 260px;
  width: auto;
}
.image-wrap figcaption,
.credit {
  margin-top: 4px;
  font-size: 0.72rem;
  opacity: 0.7;
}
.gallery-note {
  margin: 0 0 6px;
  font-size: 0.85rem;
  color: #b54708;
}
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 8px;
}
.card {
  display: flex;
  flex-direction: column;
  text-decoration: none;
  color: inherit;
  background: #fff;
  border: 1px solid #eaecf0;
  border-radius: 12px;
  overflow: hidden;
  padding-bottom: 6px;
}
.card .credit {
  padding: 0 8px;
}
.img-fallback {
  font-size: 0.8rem;
  padding: 8px;
  color: #0f766e;
}
.table-wrap {
  overflow-x: auto;
  border: 1px solid #eaecf0;
  border-radius: 10px;
}
table {
  border-collapse: collapse;
  width: 100%;
  min-width: 280px;
  font-size: 0.85rem;
}
th,
td {
  border-bottom: 1px solid #eaecf0;
  padding: 8px 10px;
  text-align: left;
  min-width: 88px;
  word-break: break-word;
}
th {
  background: #f9fafb;
  font-weight: 600;
}
</style>
