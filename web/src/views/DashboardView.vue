<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">系统总览</h2>
        <p class="page-subtitle">查看后端、数据库和真实模型接入状态。</p>
      </div>
      <el-button type="primary" :icon="Refresh" :loading="systemStore.loading" @click="load">
        刷新
      </el-button>
    </div>

    <div class="metric-grid">
      <div class="metric">
        <div class="metric-label">后端应用</div>
        <div class="metric-value">{{ systemStore.status?.appName || '-' }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">数据库</div>
        <div class="metric-value">
          <StatusTag :value="systemStore.database.status" />
        </div>
      </div>
      <div class="metric">
        <div class="metric-label">AI 状态</div>
        <div class="metric-value">
          <StatusTag :value="systemStore.ai.status" />
        </div>
      </div>
      <div class="metric">
        <div class="metric-label">模型</div>
        <div class="metric-value">{{ systemStore.ai.modelName || '-' }}</div>
      </div>
    </div>

    <div class="dashboard-grid">
      <section class="panel">
        <div class="panel-header">
          <h3 class="panel-title">AI 接入信息</h3>
          <StatusTag :value="systemStore.ai.fallbackEnabled" />
        </div>
        <div class="panel-body">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="Provider">{{ systemStore.ai.provider || '-' }}</el-descriptions-item>
            <el-descriptions-item label="模型名">{{ systemStore.ai.modelName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="AI 启用">{{ systemStore.ai.enabled ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="Key 配置">{{ systemStore.ai.apiKeyConfigured ? '已配置' : '未配置' }}</el-descriptions-item>
            <el-descriptions-item label="Base URL">{{ systemStore.ai.baseUrlConfigured ? '已配置' : '未配置' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </section>

      <section class="panel">
        <div class="panel-header">
          <h3 class="panel-title">演示入口</h3>
        </div>
        <div class="panel-body quick-actions">
          <el-button type="primary" :icon="ChatDotRound" @click="$router.push('/chat')">咨询工作台</el-button>
          <el-button :icon="Cpu" @click="$router.push('/ai-test')">AI 测试</el-button>
          <el-button :icon="Collection" @click="$router.push('/knowledge')">知识库</el-button>
          <el-button :icon="Document" @click="$router.push('/logs')">日志中心</el-button>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { onMounted } from 'vue'
import { ChatDotRound, Collection, Cpu, Document, Refresh } from '@element-plus/icons-vue'
import StatusTag from '../components/common/StatusTag.vue'
import { useSystemStore } from '../stores/systemStore'

const systemStore = useSystemStore()
const load = () => systemStore.loadStatus().catch(() => {})

onMounted(load)
</script>

<style scoped>
.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 14px;
  margin-top: 14px;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.quick-actions .el-button {
  width: 100%;
  margin: 0;
}

@media (max-width: 980px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
