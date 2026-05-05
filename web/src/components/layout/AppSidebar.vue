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
import { ChatDotRound, Collection, Cpu, DataAnalysis, Document, Monitor, Service, Tickets } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/authStore'

const route = useRoute()
const authStore = useAuthStore()
const menus = [
  { path: '/showcase', label: '答辩展示', icon: Monitor, adminOnly: true },
  { path: '/dashboard', label: '系统总览', icon: DataAnalysis, adminOnly: true },
  { path: '/chat', label: '咨询工作台', icon: ChatDotRound },
  { path: '/knowledge', label: '知识库', icon: Collection, adminOnly: true },
  { path: '/orders', label: '订单管理', icon: Tickets, adminOnly: true },
  { path: '/service-tickets', label: '人工工单', icon: Service, adminOnly: true },
  { path: '/logs', label: '日志中心', icon: Document, adminOnly: true },
  { path: '/ai-test', label: 'AI 测试', icon: Cpu, adminOnly: true }
]
const visibleMenus = computed(() => menus.filter(item => !item.adminOnly || authStore.isAdmin))
</script>

<style scoped>
.sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 20;
  width: var(--sidebar-width);
  background: var(--sidebar);
  color: white;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: var(--header-height);
  padding: 0 16px;
  border-bottom: 1px solid rgb(255 255 255 / 10%);
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: #2563eb;
  font-weight: 800;
}

.brand-title {
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
  color: #d1d5db;
}

.menu :deep(.el-menu-item.is-active) {
  background: rgb(37 99 235 / 18%);
  color: #ffffff;
}

.menu :deep(.el-menu-item:hover) {
  background: rgb(255 255 255 / 8%);
}

@media (max-width: 900px) {
  .sidebar {
    position: static;
    width: 100%;
    height: auto;
  }

  .menu {
    display: flex;
    overflow-x: auto;
  }
}
</style>
