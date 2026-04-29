<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">日志中心</h2>
        <p class="page-subtitle">查看 AI 调用、知识检索和单次会话处理轨迹。</p>
      </div>
      <el-button :icon="Refresh" @click="refresh">刷新</el-button>
    </div>

    <section class="panel">
      <div class="panel-body">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="AI 调用日志" name="ai">
            <div class="toolbar tab-toolbar">
              <el-select v-model="aiQuery.status" placeholder="状态" clearable style="width: 150px">
                <el-option label="成功" value="SUCCESS" />
                <el-option label="失败" value="FAILED" />
                <el-option label="跳过" value="SKIPPED" />
              </el-select>
              <el-button :icon="Search" @click="loadAiLogs">查询</el-button>
            </div>
            <el-table :data="aiLogs" height="460">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="provider" label="Provider" width="160" />
              <el-table-column prop="modelName" label="模型" width="140" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }"><StatusTag :value="row.status" /></template>
              </el-table-column>
              <el-table-column prop="latencyMs" label="耗时(ms)" width="100" />
              <el-table-column prop="errorMessage" label="错误" min-width="240" show-overflow-tooltip />
              <el-table-column prop="createdAt" label="时间" width="180" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="知识检索日志" name="retrieval">
            <div class="toolbar tab-toolbar">
              <el-input v-model="retrievalQuery.keyword" placeholder="关键词" clearable style="width: 220px" />
              <el-button :icon="Search" @click="loadRetrievalLogs">查询</el-button>
            </div>
            <el-table :data="retrievalLogs" height="460">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="queryText" label="查询词" min-width="180" show-overflow-tooltip />
              <el-table-column prop="docTitleSnapshot" label="命中文档" min-width="220" show-overflow-tooltip />
              <el-table-column prop="rankNo" label="排序" width="90" />
              <el-table-column prop="score" label="分数" width="90" />
              <el-table-column prop="hitReason" label="命中原因" min-width="160" show-overflow-tooltip />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="处理轨迹" name="trace">
            <div class="toolbar tab-toolbar">
              <el-input-number v-model="traceSessionId" :min="1" placeholder="会话 ID" />
              <el-button :icon="Search" @click="loadTrace">查询轨迹</el-button>
            </div>
            <el-timeline class="trace-timeline">
              <el-timeline-item
                v-for="item in traces"
                :key="item.id"
                :type="item.stepStatus === 'SUCCESS' ? 'success' : 'info'"
                :timestamp="item.stepStatus"
              >
                <strong>{{ item.stepName }}</strong>
                <p>{{ item.detailJson }}</p>
              </el-timeline-item>
            </el-timeline>
            <EmptyState v-if="!traces.length" text="请输入会话 ID 查询处理轨迹" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </section>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import EmptyState from '../components/common/EmptyState.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { listTraces } from '../api/chatApi'
import { pageAiCallLogs, pageRetrievalLogs } from '../api/logApi'

const activeTab = ref('ai')
const aiQuery = reactive({ page: 1, pageSize: 20, status: '' })
const retrievalQuery = reactive({ page: 1, pageSize: 20, keyword: '' })
const aiLogs = ref([])
const retrievalLogs = ref([])
const traceSessionId = ref(null)
const traces = ref([])

async function loadAiLogs() {
  const data = await pageAiCallLogs(aiQuery)
  aiLogs.value = data?.rows || []
}

async function loadRetrievalLogs() {
  const data = await pageRetrievalLogs(retrievalQuery)
  retrievalLogs.value = data?.rows || []
}

async function loadTrace() {
  if (!traceSessionId.value) return
  traces.value = await listTraces(traceSessionId.value)
}

function refresh() {
  if (activeTab.value === 'ai') loadAiLogs()
  if (activeTab.value === 'retrieval') loadRetrievalLogs()
  if (activeTab.value === 'trace') loadTrace()
}

watch(activeTab, refresh)
onMounted(loadAiLogs)
</script>

<style scoped>
.tab-toolbar {
  margin-bottom: 12px;
}

.trace-timeline {
  margin-top: 12px;
}

.trace-timeline p {
  margin: 6px 0 0;
  color: var(--text-muted);
}
</style>
