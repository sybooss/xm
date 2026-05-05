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
          <small>{{ traceMetricHint }}</small>
        </article>
      </div>
    </section>

    <section class="health-panel panel">
      <div class="panel-header">
        <h3 class="panel-title">健康趋势与风险诊断</h3>
        <span class="panel-caption">基于最近日志样本自动归纳，便于现场快速说明系统运行状态。</span>
      </div>
      <div class="panel-body health-body">
        <article class="health-primary" :class="healthTone">
          <span>运行健康度</span>
          <strong>{{ healthLevel }}</strong>
          <small>{{ healthRationale }}</small>
        </article>
        <article class="health-card">
          <span>最近趋势</span>
          <strong>{{ trendLabel }}</strong>
          <small>{{ trendDetail }}</small>
        </article>
        <article class="health-card">
          <span>故障入口</span>
          <strong>{{ failureEntryLabel }}</strong>
          <small>{{ failureEntryDetail }}</small>
        </article>
      </div>
      <div class="risk-layout">
        <div class="risk-block">
          <div class="section-title">风险信号</div>
          <div class="risk-list">
            <div v-for="signal in riskSignals" :key="signal.title" class="risk-row" :class="signal.tone">
              <span>{{ signal.title }}</span>
              <p>{{ signal.detail }}</p>
            </div>
          </div>
        </div>
        <div class="risk-block">
          <div class="section-title">处置建议</div>
          <ol class="action-list">
            <li v-for="item in actionItems" :key="item">{{ item }}</li>
          </ol>
        </div>
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
import { getLogDiagnostics, pageAiCallLogs, pageRetrievalLogs } from '../api/logApi'

const activeTab = ref('ai')
const aiQuery = reactive({ page: 1, pageSize: 50, status: '' })
const retrievalQuery = reactive({ page: 1, pageSize: 50, keyword: '' })
const aiLogs = ref([])
const retrievalLogs = ref([])
const diagnostics = ref(null)
const traceSessionId = ref(null)
const traces = ref([])

async function loadDiagnostics() {
  diagnostics.value = await getLogDiagnostics()
}

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
  loadDiagnostics()
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

const diagnosticAi = computed(() => diagnostics.value?.ai || null)
const diagnosticRetrieval = computed(() => diagnostics.value?.retrieval || null)
const diagnosticTrace = computed(() => diagnostics.value?.trace || null)

const successCount = computed(() => diagnosticAi.value?.successCount ?? statusCount('SUCCESS'))
const skippedCount = computed(() => diagnosticAi.value?.skippedCount ?? statusCount('SKIPPED'))
const failedCount = computed(() => diagnosticAi.value?.failedCount ?? statusCount('FAILED'))

const aiSuccessRateLabel = computed(() => {
  if (diagnosticAi.value?.successRateLabel) return diagnosticAi.value.successRateLabel
  if (!aiLogs.value.length) return '-'
  return `${Math.round((successCount.value / aiLogs.value.length) * 100)}%`
})

const avgLatencyLabel = computed(() => {
  if (diagnosticAi.value?.averageLatencyLabel) return diagnosticAi.value.averageLatencyLabel
  const values = aiLogs.value.map(item => Number(item.latencyMs)).filter(value => Number.isFinite(value) && value > 0)
  if (!values.length) return '-'
  const avg = Math.round(values.reduce((sum, value) => sum + value, 0) / values.length)
  return `${avg} ms`
})

const avgLatencyValue = computed(() => {
  if (diagnosticAi.value?.averageLatencyMs) return diagnosticAi.value.averageLatencyMs
  const values = aiLogs.value.map(item => Number(item.latencyMs)).filter(value => Number.isFinite(value) && value > 0)
  if (!values.length) return 0
  return Math.round(values.reduce((sum, value) => sum + value, 0) / values.length)
})

const tokenTotal = computed(() => {
  if (typeof diagnosticAi.value?.totalTokens === 'number') return diagnosticAi.value.totalTokens
  return aiLogs.value.reduce((sum, item) => {
    return sum + Number(item.promptTokens || 0) + Number(item.completionTokens || 0)
  }, 0)
})

const primaryModel = computed(() => {
  if (diagnosticAi.value?.primaryModel) return diagnosticAi.value.primaryModel
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

function successRate(items) {
  if (!items.length) return null
  return items.filter(item => item.status === 'SUCCESS').length / items.length
}

const recentAiWindow = computed(() => aiLogs.value.slice(0, 10))
const previousAiWindow = computed(() => aiLogs.value.slice(10, 20))
const recentSuccessRate = computed(() => successRate(recentAiWindow.value))
const previousSuccessRate = computed(() => successRate(previousAiWindow.value))

const trendLabel = computed(() => {
  if (diagnosticAi.value?.trendLabel) return diagnosticAi.value.trendLabel
  if (recentSuccessRate.value === null) return '等待样本'
  if (previousSuccessRate.value === null) return '样本积累中'
  const delta = recentSuccessRate.value - previousSuccessRate.value
  if (delta >= 0.1) return '成功率上升'
  if (delta <= -0.1) return '成功率下降'
  return '趋势平稳'
})

const trendDetail = computed(() => {
  if (diagnosticAi.value?.trendDetail) return diagnosticAi.value.trendDetail
  if (recentSuccessRate.value === null) return '还没有 AI 调用日志，先完成一次聊天或 AI 测试。'
  const current = `${Math.round(recentSuccessRate.value * 100)}%`
  if (previousSuccessRate.value === null) return `最近 ${recentAiWindow.value.length} 条样本成功率 ${current}。`
  const previous = `${Math.round(previousSuccessRate.value * 100)}%`
  return `最近窗口 ${current}，上一窗口 ${previous}，用于判断模型链路是否波动。`
})

const healthLevel = computed(() => {
  if (diagnosticAi.value?.healthLevel) return diagnosticAi.value.healthLevel
  if (!aiLogs.value.length) return '等待样本'
  const failedRate = failedCount.value / aiLogs.value.length
  if (failedRate >= 0.3) return '需要关注'
  if (failedCount.value > 0) return '可用但有波动'
  if (skippedCount.value === aiLogs.value.length) return '兜底稳定'
  if ((recentSuccessRate.value || 0) >= 0.8) return '运行稳定'
  return '样本观察中'
})

const healthTone = computed(() => {
  if (diagnosticAi.value?.healthTone) return diagnosticAi.value.healthTone
  if (healthLevel.value === '需要关注') return 'danger'
  if (healthLevel.value === '可用但有波动' || healthLevel.value === '样本观察中') return 'warning'
  return 'success'
})

const healthRationale = computed(() => {
  if (diagnosticAi.value?.healthRationale) return diagnosticAi.value.healthRationale
  if (!aiLogs.value.length) return '日志页会在真实调用后自动形成健康判断。'
  if (healthLevel.value === '需要关注') return `最近样本中有 ${failedCount.value} 次失败，应优先检查模型网关和密钥配置。`
  if (healthLevel.value === '可用但有波动') return `存在 ${failedCount.value} 次失败，但主链路仍保留本地兜底。`
  if (healthLevel.value === '兜底稳定') return '当前全部走本地兜底，适合无模型环境稳定演示。'
  return '最近调用成功率和检索证据都可用于支撑系统稳定性说明。'
})

const failureEntryLabel = computed(() => {
  if (diagnosticAi.value?.failureEntryLabel) return diagnosticAi.value.failureEntryLabel
  const firstFailure = aiLogs.value.find(item => item.status === 'FAILED')
  if (!firstFailure) return '暂无失败'
  return firstFailure.sessionId ? `会话 ${firstFailure.sessionId}` : `日志 ${firstFailure.id}`
})

const failureEntryDetail = computed(() => {
  if (diagnosticAi.value?.failureEntryDetail) return diagnosticAi.value.failureEntryDetail
  const firstFailure = aiLogs.value.find(item => item.status === 'FAILED')
  if (!firstFailure) return '最近样本没有失败记录，可继续查看原始日志作为佐证。'
  return firstFailure.errorMessage || '失败日志缺少错误摘要，建议检查后端运行日志。'
})

const uniqueDocCount = computed(() => {
  if (typeof diagnosticRetrieval.value?.uniqueDocCount === 'number') return diagnosticRetrieval.value.uniqueDocCount
  return new Set(retrievalLogs.value.map(item => item.docTitleSnapshot).filter(Boolean)).size
})

const avgRetrievalScoreLabel = computed(() => {
  if (diagnosticRetrieval.value?.averageScoreLabel) return diagnosticRetrieval.value.averageScoreLabel
  const values = retrievalLogs.value.map(item => Number(item.score)).filter(value => Number.isFinite(value))
  if (!values.length) return '-'
  const avg = values.reduce((sum, value) => sum + value, 0) / values.length
  return avg.toFixed(2)
})

const topDocs = computed(() => {
  if (diagnosticRetrieval.value?.topDocs?.length) return diagnosticRetrieval.value.topDocs
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
  if (!traces.value.length && diagnosticTrace.value?.latestProgressLabel) return diagnosticTrace.value.latestProgressLabel
  if (!traces.value.length) return '-'
  const success = traces.value.filter(item => item.stepStatus === 'SUCCESS').length
  return `${success}/${traces.value.length}`
})

const traceMetricHint = computed(() => {
  if (traceSessionId.value) return `会话 ${traceSessionId.value}`
  if (diagnosticTrace.value?.latestSessionId) return `最近会话 ${diagnosticTrace.value.latestSessionId}`
  return '输入会话 ID 查看'
})

const riskSignals = computed(() => {
  if (diagnostics.value?.riskSignals?.length) return diagnostics.value.riskSignals
  const signals = []
  if (!aiLogs.value.length) {
    signals.push({
      title: '样本不足',
      detail: '还没有 AI 调用日志，无法判断模型链路稳定性。',
      tone: 'info'
    })
  }
  if (failedCount.value > 0) {
    signals.push({
      title: '模型调用失败',
      detail: `最近 ${aiLogs.value.length} 条样本中有 ${failedCount.value} 次失败，需要关注网关、密钥或模型名。`,
      tone: failedCount.value / Math.max(aiLogs.value.length, 1) >= 0.3 ? 'danger' : 'warning'
    })
  }
  if (aiLogs.value.length && skippedCount.value === aiLogs.value.length) {
    signals.push({
      title: '全部本地兜底',
      detail: '系统可稳定演示，但当前样本不能证明真实模型链路已经打通。',
      tone: 'warning'
    })
  }
  if (avgLatencyValue.value > 6000) {
    signals.push({
      title: '响应偏慢',
      detail: `平均耗时 ${avgLatencyValue.value} ms，演示时建议先确认 sub2api 或模型服务状态。`,
      tone: 'warning'
    })
  }
  if (!retrievalLogs.value.length) {
    signals.push({
      title: '缺少 RAG 证据',
      detail: '当前没有知识检索日志，答辩时难以展示回答依据链路。',
      tone: 'info'
    })
  } else if (retrievalLogs.value.length >= 5 && uniqueDocCount.value <= 1) {
    signals.push({
      title: '命中文档集中',
      detail: '最近检索主要集中在单一文档，可补充关键词或意图覆盖来提升召回解释力。',
      tone: 'warning'
    })
  }
  if (!signals.length) {
    signals.push({
      title: '暂无明显风险',
      detail: '最近样本中模型调用、知识命中和日志证据处于可演示状态。',
      tone: 'success'
    })
  }
  return signals
})

const actionItems = computed(() => {
  if (diagnostics.value?.actionItems?.length) return diagnostics.value.actionItems
  const items = []
  if (!aiLogs.value.length) {
    items.push('先在咨询工作台发送一轮售后问题，生成 AI 调用、检索和处理轨迹样本。')
  }
  if (failedCount.value > 0) {
    items.push('优先检查 OPENAI_BASE_URL、OPENAI_API_KEY、模型名和 sub2api 健康状态。')
  }
  if (skippedCount.value === aiLogs.value.length && aiLogs.value.length) {
    items.push('如果要展示真实模型能力，开启 AI 配置后重新执行 AI 测试和聊天烟测。')
  }
  if (!retrievalLogs.value.length || uniqueDocCount.value <= 1) {
    items.push('用知识库调试面板检索退货、退款、物流、投诉等问题，确认多类规则都能留下命中日志。')
  }
  items.push('演示时先展示健康趋势，再切到 AI 调用日志、知识检索日志和处理轨迹三类原始证据。')
  return items.slice(0, 4)
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

.health-panel {
  margin-bottom: 14px;
}

.health-body {
  display: grid;
  grid-template-columns: minmax(260px, 0.9fr) repeat(2, minmax(220px, 1fr));
  gap: 12px;
}

.health-primary,
.health-card {
  min-height: 116px;
  padding: 16px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
}

.health-primary span,
.health-card span,
.health-primary small,
.health-card small {
  display: block;
  color: var(--text-muted);
  font-size: 12px;
}

.health-primary strong,
.health-card strong {
  display: block;
  margin: 9px 0 7px;
  color: #111827;
  font-size: 23px;
  line-height: 1.1;
}

.health-primary {
  border-color: rgb(15 159 110 / 24%);
  background: linear-gradient(180deg, #ffffff, #f0fdf8);
}

.health-primary.warning {
  border-color: rgb(217 119 6 / 26%);
  background: linear-gradient(180deg, #ffffff, #fffbeb);
}

.health-primary.danger {
  border-color: rgb(220 38 38 / 24%);
  background: linear-gradient(180deg, #ffffff, #fef2f2);
}

.risk-layout {
  display: grid;
  grid-template-columns: minmax(0, 0.48fr) minmax(0, 0.52fr);
  gap: 14px;
  padding: 0 18px 18px;
}

.risk-block {
  min-width: 0;
}

.risk-list {
  display: grid;
  gap: 8px;
}

.risk-row {
  padding: 10px 12px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
}

.risk-row span {
  display: block;
  color: #111827;
  font-size: 13px;
  font-weight: 700;
}

.risk-row p {
  margin: 5px 0 0;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.6;
}

.risk-row.success {
  border-color: rgb(15 159 110 / 22%);
}

.risk-row.warning {
  border-color: rgb(217 119 6 / 24%);
}

.risk-row.danger {
  border-color: rgb(220 38 38 / 24%);
}

.action-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding-left: 20px;
  color: var(--text);
  font-size: 13px;
  line-height: 1.7;
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
  .diagnostic-layout,
  .health-body,
  .risk-layout {
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
