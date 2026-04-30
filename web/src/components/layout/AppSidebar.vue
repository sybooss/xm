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
      <el-menu-item index="/dashboard">
        <el-icon><DataAnalysis /></el-icon>
        <span>系统总览</span>
      </el-menu-item>
      <el-menu-item index="/chat">
        <el-icon><ChatDotRound /></el-icon>
        <span>咨询工作台</span>
      </el-menu-item>
      <el-menu-item index="/knowledge">
        <el-icon><Collection /></el-icon>
        <span>知识库</span>
      </el-menu-item>
      <el-menu-item index="/orders">
        <el-icon><Tickets /></el-icon>
        <span>订单管理</span>
      </el-menu-item>
      <el-menu-item index="/service-tickets">
        <el-icon><Service /></el-icon>
        <span>人工工单</span>
      </el-menu-item>
      <el-menu-item index="/logs">
        <el-icon><Document /></el-icon>
        <span>日志中心</span>
      </el-menu-item>
      <el-menu-item index="/ai-test">
        <el-icon><Cpu /></el-icon>
        <span>AI 测试</span>
      </el-menu-item>
    </el-menu>
  </aside>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { ChatDotRound, Collection, Cpu, DataAnalysis, Document, Service, Tickets } from '@element-plus/icons-vue'

const route = useRoute()
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
