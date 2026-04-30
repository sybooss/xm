import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import ChatWorkbenchView from '../views/ChatWorkbenchView.vue'
import KnowledgeDocView from '../views/KnowledgeDocView.vue'
import OrderView from '../views/OrderView.vue'
import ServiceTicketView from '../views/ServiceTicketView.vue'
import LogCenterView from '../views/LogCenterView.vue'
import AiTestView from '../views/AiTestView.vue'
import LoginView from '../views/LoginView.vue'
import { useAuthStore } from '../stores/authStore'

const routes = [
  { path: '/', redirect: '/chat' },
  { path: '/login', name: 'login', component: LoginView, meta: { title: '登录', public: true } },
  { path: '/dashboard', name: 'dashboard', component: DashboardView, meta: { title: '系统总览' } },
  { path: '/chat', name: 'chat', component: ChatWorkbenchView, meta: { title: '咨询工作台' } },
  { path: '/knowledge', name: 'knowledge', component: KnowledgeDocView, meta: { title: '知识库' } },
  { path: '/orders', name: 'orders', component: OrderView, meta: { title: '订单管理' } },
  { path: '/service-tickets', name: 'service-tickets', component: ServiceTicketView, meta: { title: '人工工单' } },
  { path: '/logs', name: 'logs', component: LogCenterView, meta: { title: '日志中心' } },
  { path: '/ai-test', name: 'ai-test', component: AiTestView, meta: { title: 'AI 测试' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (to.meta.public) {
    if (to.path === '/login' && authStore.isLoggedIn) {
      return '/chat'
    }
    return true
  }
  if (!authStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
