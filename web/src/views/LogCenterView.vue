<template>
  <section class="log-page page">
    <div class="page-header">
      <div>
        <h2 class="page-title">日志诊断中心</h2>
        <p class="page-subtitle">把 AI 调用、知识命中和处理轨迹整理成答辩可讲的证据链。</p>
      </div>
      <el-button type="primary" :icon="Refresh" @click="refreshAll">刷新诊断</el-button>
    </div>

    <section class="diagnostic-hero panel">
      <div class="hero-copy">
        <span>最近 {{ aiLogs.length }} 条 AI 调用样本</span>
        <h3>AI 稳定性、RAG 命中与流程可解释性一屏核验</h3>
        <p>
          答辩时可以先看总览，再切到原始日志。成功、跳过和失败都会保留证据，
          方便说明系统既能接入真实模型，也能在模型不可用时稳定兜底。
        </p>
      </div>
      <div class="hero-metrics">
        <article class="hero-metric">
          <span>AI 成功率</span>
          <strong>{{ aiSuccessRateLabel }}</strong>
          <small>{{ successCount }} 次成功 / {{ aiLogs.length }} 条样本</small>
        </article>
        <article class="hero-metric">
          <span>平均耗时</span>
          <strong>{{ avgLatencyLabel }}</strong>
          <small>仅统计有耗时记录的调用</small>
        </article>
        <article class="hero-metric">
          <span>知识命中</span>
          <strong>{{ retrievalLogs.length }}</strong>
          <small>{{ uniqueDocCount }} 个文档被命中</small>
        </article>
        <article class="hero-metric">
          <span>轨迹步骤</span>
          <strong>{{ traceProgressLabel }}</strong>
          <small>{{ traceSessionId ? `会话 ${traceSessionId}` : '输入会话 ID 查看' }}</small>
        </article>
      </div>
    </section>

    <div class="diagnostic-layout">
      <section class="panel insight-panel">
        <div class="panel-header">
          <h3 class="panel-title">诊断总览</h3>
          <span class="panel-caption">最近日志聚合，不替代下方原始记录。</span>
        </div>
        <div class="panel-body insight-body">
          <div class="status-board">
            <div v-for="item in aiStatusBars" :key="item.status" class="status-row">
              <div class="status-row-head">
                <span>{{ item.label }}</span>
                <strong>{{ item.count }}</strong>
              </div>
              <div class="bar-track">
                <span class="bar-fill" :class="item.tone" :style="{ width: `${item.percent}%` }" />
              </div>
            </div>
          </div>

          <div class="evidence-summary">
            <article>
              <el-icon><Cpu /></el-icon>
              <div>
                <strong>{{ primaryModel }}</strong>
                <span>主模型 / Provider</span>
              </div>
            </article>
            <article>
              <el-icon><TrendCharts /></el-icon>
              <div>
                <strong>{{ tokenTotal }}</strong>
                <span>最近样本 token 总量</span>
              </div>
            </article>
            <article>
              <el-icon><Collection /></el-icon>
              <div>
                <strong>{{ avgRetrievalScoreLabel }}</strong>
                <span>平均检索分数</span>
              </div>
            </article>
          </div>

          <div class="top-docs">
            <div class="section-title">高频命中文档</div>
            <button
              v-for="doc in topDocs"
              :key="doc.title"
              class="doc-chip"
              type="button"
              @click="setRetrievalKeyword(doc.title)"
            >
              <span>{{ doc.title }}</span>
              <strong>{{ doc.count }}</strong>
            </button>
            <EmptyState v-if="!topDocs.length" text="暂无知识命中日志" />
          </div>
        </div>
      </section>

      <section class="panel log-panel">
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
              <el-table :data="aiLogs" height="430">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="sessionId" label="会话" width="86" />
                <el-table-column prop="provider" label="Provider" width="150" show-overflow-tooltip />
                <el-table-column prop="modelName" label="模型" width="150" show-overflow-tooltip />
                <el-table-column label="状态" width="100">
                  <template #default="{ row }"><StatusTag :value="row.status" /></template>
                </el-table-column>
                <el-table-column prop="latencyMs" label="耗时(ms)" width="100" />
                <el-table-column prop="requestSummary" label="请求摘要" min-width="210" show-overflow-tooltip />
                <el-table-column prop="errorMessage" label="错误" min-width="200" show-overflow-tooltip />
                <el-table-column prop="createdAt" label="时间" width="180" />
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="知识检索日志" name="retrieval">
              <div class="toolbar tab-toolbar">
                <el-input v-model="retrievalQuery.keyword" placeholder="关键词" clearable style="width: 220px" />
                <el-button :icon="Search" @click="loadRetrievalLogs">查询</el-button>
              </div>
              <el-table :data="retrievalLogs" height="430">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="sessionId" label="会话" width="86" />
                <el-table-column prop="queryText" label="查询词" min-width="180" show-overflow-tooltip />
                <el-table-column prop="docTitleSnapshot" label="命中文档" min-width="220" show-overflow-tooltip />
                <el-table-column prop="rankNo" label="排序" width="90" />
                <el-table-column label="分数" width="90">
                  <template #default="{ row }">{{ formatScore(row.score) }}</template>
                </el-table-column>
                <el-table-column prop="hitReason" label="命中原因" min-width="180" show-overflow-tooltip />
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="处理轨迹" name="trace">
              <div class="toolbar tab-toolbar">
                <el-input-number v-model="traceSessionId" :min="1" :controls="false" placeholder="会话 ID" />
                <el-button :icon="Search" @click="loadTrace">查询轨迹</el-button>
              </div>
              <div v-if="traces.length" class="trace-summary">
                <el-icon><Connection /></el-icon>
                <span>{{ traceProgressLabel }}，最后步骤：{{ traces[traces.length - 1]?.stepName }}</span>
              </div>
              <el-timeline class="trace-timeline">
                <el-timeline-item
                  v-for="item in traces"
                  :key="item.id"
                  :type="item.stepStatus === 'SUCCESS' ? 'success' : 'info'"
                  :timestamp="item.stepStatus"
                >
                  <strong>{{ item.stepName }}</strong>
                  <p>{{ formatTraceDetail(item.detailJson) }}</p>
                </el-timeline-item>
              </el-timeline>
              <EmptyState v-if="!traces.length" text="请输入会话 ID 查询处理轨迹" />
            </el-tab-pane>
          </el-tabs>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Collection, Connection, Cpu, Refresh, Search, TrendCharts } from '@element-plus/icons-vue'
import EmptyState from '../components/common/EmptyState.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { listTraces } from '../api/chatApi'
import { pageAiCallLogs, pageRetrievalLogs } from '../api/logApi'

const activeTab = ref('ai')
const aiQuery = reactive({ page: 1, pageSize: 50, status: '' })
const retrievalQuery = reactive({ page: 1, pageSize: 50, keyword: '' })
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

function refreshAll() {
  loadAiLogs()
  loadRetrievalLogs()
  if (traceSessionId.value) loadTrace()
}

function setRetrievalKeyword(keyword) {
  retrievalQuery.keyword = keyword
  activeTab.value = 'retrieval'
  loadRetrievalLogs()
}

function formatScore(score) {
  const value = Number(score)
  if (!Number.isFinite(value)) return '-'
  return value.toFixed(2)
}

function formatTraceDetail(value) {
  if (!value) return '-'
  try {
    const parsed = JSON.parse(value)
    return Object.entries(parsed)
      .map(([key, item]) => `${key}: ${item}`)
      .join('；')
  } catch {
    return value
  }
}

function statusCount(status) {
  return aiLogs.value.filter(item => item.status === status).length
}

const successCount = computed(() => statusCount('SUCCESS'))
const skippedCount = computed(() => statusCount('SKIPPED'))
const failedCount = computed(() => statusCount('FAILED'))

const aiSuccessRateLabel = computed(() => {
  if (!aiLogs.value.length) return '-'
  return `${Math.round((successCount.value / aiLogs.value.length) * 100)}%`
})

const avgLatencyLabel = computed(() => {
  const values = aiLogs.value.map(item => Number(item.latencyMs)).filter(value => Number.isFinite(value) && value > 0)
  if (!values.length) return '-'
  const avg = Math.round(values.reduce((sum, value) => sum + value, 0) / values.length)
  return `${avg} ms`
})

const tokenTotal = computed(() => {
  return aiLogs.value.reduce((sum, item) => {
    return sum + Number(item.promptTokens || 0) + Number(item.completionTokens || 0)
  }, 0)
})

const primaryModel = computed(() => {
  const first = aiLogs.value.find(item => item.modelName || item.provider)
  if (!first) return '-'
  return [first.provider, first.modelName].filter(Boolean).join(' / ')
})

const aiStatusBars = computed(() => {
  const total = aiLogs.value.length || 1
  return [
    { status: 'SUCCESS', label: '成功调用', count: successCount.value, tone: 'success' },
    { status: 'SKIPPED', label: '本地兜底/跳过', count: skippedCount.value, tone: 'info' },
    { status: 'FAILED', label: '失败调用', count: failedCount.value, tone: 'danger' }
  ].map(item => ({
    ...item,
    percent: item.count ? Math.max(4, Math.round((item.count / total) * 100)) : 0
  }))
})

const uniqueDocCount = computed(() => {
  return new Set(retrievalLogs.value.map(item => item.docTitleSnapshot).filter(Boolean)).size
})

const avgRetrievalScoreLabel = computed(() => {
  const values = retrievalLogs.value.map(item => Number(item.score)).filter(value => Number.isFinite(value))
  if (!values.length) return '-'
  const avg = values.reduce((sum, value) => sum + value, 0) / values.length
  return avg.toFixed(2)
})

const topDocs = computed(() => {
  const counts = new Map()
  retrievalLogs.value.forEach(item => {
    const title = item.docTitleSnapshot || '未命名文档'
    counts.set(title, (counts.get(title) || 0) + 1)
  })
  return Array.from(counts.entries())
    .map(([title, count]) => ({ title, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 5)
})

const traceProgressLabel = computed(() => {
  if (!traces.value.length) return '-'
  const success = traces.value.filter(item => item.stepStatus === 'SUCCESS').length
  return `${success}/${traces.value.length}`
})

watch(activeTab, refresh)
onMounted(refreshAll)
</script>

<style scoped>
.log-page {
  background:
    linear-gradient(180deg, rgb(255 255 255 / 88%), rgb(246 248 251 / 88%)),
    radial-gradient(circle at 92% 0%, rgb(37 99 235 / 8%), transparent 30%);
}

.diagnostic-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(420px, 0.85fr);
  gap: 20px;
  padding: 24px;
  margin-bottom: 14px;
  overflow: hidden;
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.hero-copy span {
  color: var(--brand);
  font-size: 13px;
  font-weight: 700;
}

.hero-copy h3 {
  max-width: 680px;
  margin: 10px 0 0;
  color: #0f172a;
  font-size: 28px;
  line-height: 1.2;
}

.hero-copy p {
  max-width: 720px;
  margin: 14px 0 0;
  color: var(--text-muted);
  font-size: 14px;
  line-height: 1.9;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.hero-metric {
  min-height: 104px;
  padding: 16px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: linear-gradient(180deg, #ffffff, #f8fbff);
}

.hero-metric span,
.hero-metric small {
  display: block;
  color: var(--text-muted);
  font-size: 12px;
}

.hero-metric strong {
  display: block;
  margin: 8px 0 6px;
  color: #111827;
  font-size: 24px;
  line-height: 1.1;
}

.diagnostic-layout {
  display: grid;
  grid-template-columns: minmax(300px, 0.36fr) minmax(0, 0.64fr);
  gap: 14px;
}

.panel-caption {
  color: var(--text-muted);
  font-size: 12px;
}

.insight-body {
  display: grid;
  gap: 18px;
}

.status-board {
  display: grid;
  gap: 14px;
}

.status-row {
  display: grid;
  gap: 8px;
}

.status-row-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--text);
  font-size: 13px;
}

.bar-track {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #eef2f7;
}

.bar-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
}

.bar-fill.success {
  background: #0f9f6e;
}

.bar-fill.info {
  background: #64748b;
}

.bar-fill.danger {
  background: #dc2626;
}

.evidence-summary {
  display: grid;
  gap: 10px;
}

.evidence-summary article {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 64px;
  padding: 12px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
}

.evidence-summary .el-icon {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  color: var(--brand);
  background: var(--brand-soft);
}

.evidence-summary strong,
.evidence-summary span {
  display: block;
}

.evidence-summary strong {
  color: #111827;
  font-size: 14px;
}

.evidence-summary span {
  margin-top: 3px;
  color: var(--text-muted);
  font-size: 12px;
}

.section-title {
  margin-bottom: 10px;
  color: #111827;
  font-size: 14px;
  font-weight: 700;
}

.top-docs {
  min-width: 0;
}

.doc-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  min-height: 42px;
  margin-bottom: 8px;
  padding: 9px 10px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  color: var(--text);
  background: #ffffff;
  cursor: pointer;
  text-align: left;
}

.doc-chip:hover {
  border-color: rgb(37 99 235 / 38%);
  background: #f8fbff;
}

.doc-chip span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-chip strong {
  color: var(--brand);
}

.tab-toolbar {
  margin-bottom: 12px;
}

.trace-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 38px;
  margin-bottom: 10px;
  padding: 8px 10px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  color: var(--text);
  background: #f8fbff;
  font-size: 13px;
}

.trace-timeline {
  margin-top: 12px;
}

.trace-timeline p {
  margin: 6px 0 0;
  color: var(--text-muted);
}

@media (max-width: 1180px) {
  .diagnostic-hero,
  .diagnostic-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hero-metrics {
    grid-template-columns: 1fr;
  }

  .diagnostic-hero {
    padding: 18px;
  }

  .hero-copy h3 {
    font-size: 22px;
  }
}
</style>
