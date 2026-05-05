import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const LoginView = () => import('../views/LoginView.vue')
const ShowcaseView = () => import('../views/ShowcaseView.vue')
const DashboardView = () => import('../views/DashboardView.vue')
const ChatWorkbenchView = () => import('../views/ChatWorkbenchView.vue')
const KnowledgeDocView = () => import('../views/KnowledgeDocView.vue')
const OrderView = () => import('../views/OrderView.vue')
const ServiceTicketView = () => import('../views/ServiceTicketView.vue')
const LogCenterView = () => import('../views/LogCenterView.vue')
const AiTestView = () => import('../views/AiTestView.vue')

const routes = [
  { path: '/', redirect: '/showcase' },
  { path: '/login', name: 'login', component: LoginView, meta: { title: '登录', public: true } },
  { path: '/showcase', name: 'showcase', component: ShowcaseView, meta: { title: '答辩展示中心', adminOnly: true } },
  { path: '/dashboard', name: 'dashboard', component: DashboardView, meta: { title: '系统总览', adminOnly: true } },
  { path: '/chat', name: 'chat', component: ChatWorkbenchView, meta: { title: '咨询工作台' } },
  { path: '/knowledge', name: 'knowledge', component: KnowledgeDocView, meta: { title: '知识库', adminOnly: true } },
  { path: '/orders', name: 'orders', component: OrderView, meta: { title: '订单管理', adminOnly: true } },
  { path: '/service-tickets', name: 'service-tickets', component: ServiceTicketView, meta: { title: '人工工单', adminOnly: true } },
  { path: '/logs', name: 'logs', component: LogCenterView, meta: { title: '日志中心', adminOnly: true } },
  { path: '/ai-test', name: 'ai-test', component: AiTestView, meta: { title: 'AI 测试', adminOnly: true } }
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
  if (to.meta.adminOnly && !authStore.isAdmin) {
    return '/chat'
  }
  return true
})

export default router
