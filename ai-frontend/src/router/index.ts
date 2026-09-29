import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/travel',
    },
    {
      path: '/travel',
      name: 'travel',
      component: () => import('@/views/TravelChatView.vue'),
      meta: { title: 'AI 旅行管家' },
    },
    {
      path: '/manus',
      name: 'manus',
      component: () => import('@/views/ManusChatView.vue'),
      meta: { title: 'TravelManus 智能体' },
    },
  ],
})

router.afterEach((to) => {
  document.title = (to.meta.title as string) || 'AI 旅行管家'
})

export default router
