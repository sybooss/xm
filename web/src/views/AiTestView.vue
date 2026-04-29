<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">AI 测试</h2>
        <p class="page-subtitle">验证 LangChain4j 是否已通过本机 sub2api 调用真实模型。</p>
      </div>
      <el-button :icon="Refresh" @click="systemStore.loadStatus()">刷新状态</el-button>
    </div>

    <section class="panel">
      <div class="panel-header">
        <h3 class="panel-title">模型连通性</h3>
        <StatusTag :value="systemStore.ai.status" />
      </div>
      <div class="panel-body">
        <el-input
          v-model="prompt"
          type="textarea"
          :rows="5"
          resize="none"
          placeholder="输入测试提示词"
        />
        <div class="toolbar test-toolbar">
          <el-button type="primary" :icon="Promotion" :loading="loading" @click="runTest">
            发送测试
          </el-button>
          <el-button @click="prompt = '请用一句话说明退货申请流程。'">退货流程</el-button>
          <el-button @click="prompt = '请回复：后端已经通过 sub2api 调用真实模型。'">连通确认</el-button>
        </div>
      </div>
    </section>

    <section v-if="result" class="panel result-panel">
      <div class="panel-header">
        <h3 class="panel-title">测试结果</h3>
        <StatusTag :value="result.status" />
      </div>
      <div class="panel-body">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="是否使用模型">{{ result.used ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="是否兜底">{{ result.fallbackUsed ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="Provider">{{ result.provider }}</el-descriptions-item>
          <el-descriptions-item label="模型">{{ result.modelName }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ result.latencyMs }} ms</el-descriptions-item>
          <el-descriptions-item label="错误">{{ result.errorMessage || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div class="reply-box">{{ result.reply || '无回复内容' }}</div>
      </div>
    </section>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Promotion, Refresh } from '@element-plus/icons-vue'
import { testAi } from '../api/aiApi'
import StatusTag from '../components/common/StatusTag.vue'
import { useSystemStore } from '../stores/systemStore'

const systemStore = useSystemStore()
const prompt = ref('请回复：后端已经通过 sub2api 调用真实模型。')
const result = ref(null)
const loading = ref(false)

async function runTest() {
  loading.value = true
  try {
    result.value = await testAi({ prompt: prompt.value })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  systemStore.loadStatus().catch(() => {})
})
</script>

<style scoped>
.test-toolbar {
  margin-top: 12px;
}

.result-panel {
  margin-top: 14px;
}

.reply-box {
  min-height: 82px;
  margin-top: 14px;
  padding: 12px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius);
  background: var(--surface-soft);
  line-height: 1.75;
  white-space: pre-wrap;
}
</style>
