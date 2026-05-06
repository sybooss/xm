<template>
  <aside class="sidebar">
    <div class="brand">
      <div class="brand-mark">退</div>
      <div>
        <div class="brand-title">退换货客服</div>
        <div class="brand-subtitle">Returns Assistant</div>
      </div>
    </div>

    <el-menu :default-active="route.path" router class="menu">
      <el-menu-item
        v-for="item in visibleMenus"
        :key="item.path"
        :index="item.path"
      >
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </el-menu-item>
    </el-menu>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ChatDotRound, Collection, Connection, Cpu, DataAnalysis, Document, Files, Monitor, Operation, Service, Tickets, UserFilled, View } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/authStore'

const route = useRoute()
const authStore = useAuthStore()
const menus = [
  { path: '/showcase', label: '答辩展示', icon: Monitor, adminOnly: true },
  { path: '/dashboard', label: '系统总览', icon: DataAnalysis, adminOnly: true },
  { path: '/operations', label: '运营指挥', icon: Operation, adminOnly: true },
  { path: '/feature-closures', label: '特色闭环', icon: Connection, adminOnly: true },
  { path: '/admin/after-sales/review', label: '售后审核', icon: View, adminOnly: true },
  { path: '/admin/sla', label: 'SLA 中心', icon: DataAnalysis, adminOnly: true },
  { path: '/admin/customers/profile', label: '客户画像', icon: UserFilled, adminOnly: true },
  { path: '/customer/after-sales', label: '我的售后', icon: Files, customerOnly: true },
  { path: '/chat', label: '在线咨询', icon: ChatDotRound },
  { path: '/knowledge', label: '知识库', icon: Collection, adminOnly: true },
  { path: '/orders', label: '订单管理', icon: Tickets, adminOnly: true },
  { path: '/service-tickets', label: '人工工单', icon: Service, adminOnly: true },
  { path: '/logs', label: '日志中心', icon: Document, adminOnly: true },
  { path: '/ai-test', label: 'AI 测试', icon: Cpu, adminOnly: true }
]
const visibleMenus = computed(() => menus.filter(item => {
  if (item.adminOnly) {
    return authStore.isAdmin
  }
  if (item.customerOnly) {
    return !authStore.isAdmin
  }
  return true
}))
</script>

<style scoped>
.sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 20;
  width: var(--sidebar-width);
  border-right: 1px solid rgb(210 210 215 / 72%);
  background: rgb(255 255 255 / 78%);
  color: var(--text);
  backdrop-filter: blur(22px) saturate(150%);
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: var(--header-height);
  padding: 0 18px;
  border-bottom: 1px solid rgb(210 210 215 / 70%);
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: linear-gradient(145deg, #0a84ff, #0066cc);
  color: #ffffff;
  font-size: 15px;
  font-weight: 800;
  box-shadow: 0 12px 28px rgb(0 102 204 / 28%);
}

.brand-title {
  color: #1d1d1f;
  font-size: 15px;
  font-weight: 700;
}

.brand-subtitle {
  color: var(--sidebar-muted);
  font-size: 12px;
}

.menu {
  border-right: 0;
  background: transparent;
}

.menu :deep(.el-menu-item) {
  height: 46px;
  margin: 6px 10px;
  border-radius: 8px;
  color: #4c4c50;
  font-weight: 600;
}

.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgb(0 102 204 / 12%), rgb(255 255 255 / 84%));
  color: var(--brand);
  box-shadow:
    inset 0 0 0 1px rgb(0 102 204 / 13%),
    0 10px 24px rgb(0 0 0 / 4%);
}

.menu :deep(.el-menu-item:hover) {
  background: rgb(0 0 0 / 4%);
}

@media (max-width: 900px) {
  .sidebar {
    position: static;
    width: 100%;
    height: auto;
    overflow: hidden;
  }

  .menu {
    display: flex;
    overflow-x: auto;
    padding: 0 10px 8px;
    scrollbar-width: none;
  }

  .menu::-webkit-scrollbar {
    display: none;
  }

  .menu :deep(.el-menu-item) {
    flex: 0 0 auto;
    min-width: 124px;
    margin: 6px 4px;
  }
}
</style>
