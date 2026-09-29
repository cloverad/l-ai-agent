<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="app-shell">
    <nav class="top-nav">
      <div class="brand">L-AI Travel</div>
      <div class="links">
        <RouterLink to="/travel">旅行管家</RouterLink>
        <RouterLink to="/manus">TravelManus</RouterLink>
      </div>
    </nav>
    <RouterView />
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background:
    radial-gradient(circle at 10% 10%, rgba(15, 118, 110, 0.08), transparent 40%),
    radial-gradient(circle at 90% 0%, rgba(29, 78, 216, 0.08), transparent 35%),
    #fafafa;
  color: #101828;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}
.top-nav {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid #eaecf0;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  position: sticky;
  top: 0;
  z-index: 10;
}
.brand {
  font-weight: 700;
  letter-spacing: 0.02em;
}
.links {
  display: flex;
  gap: 16px;
}
.links a {
  text-decoration: none;
  color: #475467;
  font-weight: 600;
  padding: 6px 10px;
  border-radius: 8px;
}
.links a.router-link-active {
  color: #0f766e;
  background: rgba(15, 118, 110, 0.1);
}
</style>
