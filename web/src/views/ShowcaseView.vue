<template>
  <section class="showcase-page">
    <div class="showcase-shell">
      <main class="showcase-main">
        <section class="hero-panel">
          <div class="hero-copy">
            <h2>电商退换货 AI 客服系统</h2>
            <p>
              把用户咨询、意图识别、知识依据、订单上下文、AI 增强、工单升级和日志追踪压进一条可演示闭环，
              答辩时先跑通真实链路，再讲代码、数据和验证证据。
            </p>
            <div class="hero-actions">
              <el-button type="primary" :icon="ChatDotRound" size="large" @click="$router.push('/chat')">
                开始演示链路
              </el-button>
              <el-button :icon="DocumentChecked" size="large" @click="$router.push('/logs')">
                查看验证证据
              </el-button>
            </div>
          </div>

          <div class="hero-visual" aria-hidden="true">
            <div class="glass-stack stack-back"></div>
            <div class="glass-stack stack-mid">
              <el-icon><Tickets /></el-icon>
            </div>
            <div class="glass-cube">
              <el-icon><RefreshRight /></el-icon>
            </div>
            <div class="glass-stack stack-front">
              <el-icon><ChatDotRound /></el-icon>
            </div>
          </div>
        </section>

        <section class="panel flow-panel">
          <div class="panel-header">
            <h3 class="panel-title">演示流程</h3>
            <span class="panel-caption">从用户问题到证据留痕，形成能现场跑通的售后闭环。</span>
          </div>
          <div class="closed-loop-flow">
            <article v-for="(step, index) in demoSteps" :key="step.title" class="flow-node">
              <span class="node-index">{{ index + 1 }}</span>
              <el-icon><component :is="step.icon" /></el-icon>
              <h4>{{ step.title }}</h4>
              <p>{{ step.detail }}</p>
            </article>
          </div>
        </section>

        <section class="panel roadmap-panel">
          <div class="panel-header">
            <h3 class="panel-title">闭环特色功能</h3>
            <span class="panel-caption">10+ 个功能都要有页面入口、接口、数据或测试证据，避免只写在报告里。</span>
          </div>
          <div class="feature-board">
            <article v-for="item in featureRoadmap" :key="item.title" class="feature-card" :class="`feature-${item.state}`">
              <div class="feature-icon">
                <el-icon><component :is="item.icon" /></el-icon>
              </div>
              <div class="feature-copy">
                <div class="feature-title-row">
                  <h4>{{ item.title }}</h4>
                  <span class="feature-state">{{ item.stateLabel }}</span>
                </div>
                <p>{{ item.detail }}</p>
                <span class="evidence">{{ item.evidence }}</span>
              </div>
            </article>
          </div>
        </section>
      </main>

      <aside class="showcase-side">
        <section class="panel status-panel">
          <div class="panel-header compact">
            <h3 class="panel-title">系统状态</h3>
            <el-button :icon="Refresh" circle :loading="systemStore.loading" @click="loadStatus" />
          </div>
          <div class="status-list">
            <div class="status-row">
              <span>数据库</span>
              <StatusTag :value="systemStore.database.status" />
            </div>
            <div class="status-row">
              <span>AI 服务</span>
              <StatusTag :value="systemStore.ai.status" />
            </div>
            <div class="status-row">
              <span>当前模型</span>
              <strong>{{ systemStore.ai.modelName || 'local-rule-template' }}</strong>
            </div>
            <div class="status-row">
              <span>兜底策略</span>
              <strong>{{ systemStore.ai.fallbackEnabled ? '已启用' : '未开启' }}</strong>
            </div>
          </div>
        </section>

        <section class="panel side-panel">
          <div class="panel-header compact">
            <h3 class="panel-title">关键指标</h3>
          </div>
          <div class="metric-list">
            <div v-for="item in metrics" :key="item.label" class="metric-row">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>
                <small>{{ item.label }}</small>
                <strong>{{ item.value }}</strong>
              </span>
            </div>
          </div>
        </section>

        <section class="panel side-panel">
          <div class="panel-header compact">
            <h3 class="panel-title">版本路线图</h3>
          </div>
          <div class="version-timeline">
            <article v-for="item in versionPlan" :key="item.version" class="version-item" :class="{ done: item.done }">
              <span class="version-dot"></span>
              <strong>{{ item.version }}</strong>
              <p>{{ item.detail }}</p>
              <small>{{ item.date }}</small>
            </article>
          </div>
        </section>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { onMounted } from 'vue'
import {
  ChatDotRound,
  Collection,
  Connection,
  Cpu,
  DataAnalysis,
  Document,
  DocumentChecked,
  Finished,
  Lock,
  Operation,
  Refresh,
  RefreshRight,
  Search,
  Service,
  Tickets,
  TrendCharts,
  Van
} from '@element-plus/icons-vue'
import StatusTag from '../components/common/StatusTag.vue'
import { useSystemStore } from '../stores/systemStore'

const systemStore = useSystemStore()

const demoSteps = [
  { title: '用户咨询', detail: '绑定订单并提出退货、退款、物流或投诉问题。', icon: ChatDotRound },
  { title: '意图识别', detail: '结合本轮问题、历史上下文和订单状态判断售后意图。', icon: Cpu },
  { title: '知识依据', detail: '检索退换货规则，展示命中文档和排序依据。', icon: Collection },
  { title: '方案生成', detail: 'LangChain4j 增强回复，本地规则随时兜底。', icon: Operation },
  { title: '工单升级', detail: '投诉、人工客服和异常物流自动进入处理队列。', icon: Service },
  { title: '日志留痕', detail: 'AI 调用、检索记录和处理轨迹可复查。', icon: DocumentChecked }
]

const featureRoadmap = [
  {
    title: 'AI 流式客服',
    detail: 'SSE 进度事件 + 前端逐字回复，让等待过程可感知。',
    evidence: 'POST /chat-sessions/{id}/message-stream',
    state: 'done',
    stateLabel: '已完成',
    icon: ChatDotRound
  },
  {
    title: 'RAG 知识增强',
    detail: '回复前检索知识文档，页面展示命中依据和排序原因。',
    evidence: 'GET /knowledge-docs/search',
    state: 'done',
    stateLabel: '已完成',
    icon: Search
  },
  {
    title: '多轮上下文承接',
    detail: '追问退款、投诉时承接上轮订单和售后语境。',
    evidence: 'CONTEXT_RESOLVE 处理轨迹',
    state: 'done',
    stateLabel: '已完成',
    icon: Connection
  },
  {
    title: 'LangChain4j 工具调用',
    detail: '订单查询、知识检索、工单创建封装为业务工具。',
    evidence: 'AiBusinessToolService',
    state: 'done',
    stateLabel: '已完成',
    icon: Operation
  },
  {
    title: '智能工单升级',
    detail: '投诉、物流异常和人工客服诉求自动生成工单。',
    evidence: 'GET /service-tickets',
    state: 'done',
    stateLabel: '已完成',
    icon: Service
  },
  {
    title: '本地规则兜底',
    detail: '模型不可用时仍能稳定回答退换货核心问题。',
    evidence: 'FALLBACK / SKIPPED 状态',
    state: 'done',
    stateLabel: '已完成',
    icon: Finished
  },
  {
    title: '权限与安全控制',
    detail: '客户和管理员入口分离，后台写操作需要管理员身份。',
    evidence: 'AuthInterceptor + @OperatorAnno',
    state: 'done',
    stateLabel: '已完成',
    icon: Lock
  },
  {
    title: '日志诊断中心',
    detail: 'AI 调用、知识检索和处理轨迹聚合成可答辩证据。',
    evidence: 'AI 调用日志 / 检索日志 / 轨迹',
    state: 'done',
    stateLabel: '已完成',
    icon: Document
  },
  {
    title: '售后流程可视化',
    detail: '工单页展示 SLA 风险、时间线和下一步动作。',
    evidence: 'ServiceTicketView 时间线',
    state: 'done',
    stateLabel: '已完成',
    icon: TrendCharts
  },
  {
    title: 'Apple-like 展示中心',
    detail: '重构答辩首页、全局侧栏和顶部栏，提升首屏高级感。',
    evidence: 'ShowcaseView + 全局样式',
    state: 'active',
    stateLabel: '本轮优化',
    icon: DataAnalysis
  },
  {
    title: '数据分析看板',
    detail: '后续补充意图分布、工单状态、热门问题和处理效率。',
    evidence: '待新增统计接口与图表',
    state: 'planned',
    stateLabel: '计划中',
    icon: TrendCharts
  },
  {
    title: '多渠道接入',
    detail: '后续模拟网页、小程序、App 多入口接入同一售后链路。',
    evidence: '待新增渠道字段与筛选',
    state: 'planned',
    stateLabel: '计划中',
    icon: Van
  }
]

const metrics = [
  { label: '已落地特色', value: '10 项', icon: Finished },
  { label: '闭环流程节点', value: '6 步', icon: RefreshRight },
  { label: '验证脚本', value: '4 组', icon: DocumentChecked },
  { label: '下一版本', value: '数据看板', icon: DataAnalysis }
]

const versionPlan = [
  { version: 'V1.0', date: '2026-04-30', detail: '基础闭环、流式客服、工具调用、权限控制。', done: true },
  { version: 'V1.1', date: '2026-05-06', detail: 'Apple-like 展示中心与 10+ 特色功能路线图。', done: true },
  { version: 'V1.2', date: '下一轮', detail: '数据分析看板、统计接口和热门问题洞察。', done: false },
  { version: 'V1.3', date: '后续', detail: '多渠道接入、SLA 自动提醒和导出报告。', done: false }
]

function loadStatus() {
  systemStore.loadStatus().catch(() => {})
}

onMounted(loadStatus)
</script>

<style scoped>
.showcase-page {
  min-height: calc(100vh - var(--header-height));
  padding: 22px;
}

.showcase-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
}

.showcase-main,
.showcase-side {
  display: grid;
  align-content: start;
  gap: 16px;
  min-width: 0;
}

.hero-panel {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(340px, 0.72fr);
  gap: 18px;
  min-height: 326px;
  overflow: hidden;
  padding: 48px;
  border: 1px solid rgb(229 229 234 / 86%);
  border-radius: 8px;
  background:
    linear-gradient(112deg, rgb(255 255 255 / 96%) 0%, rgb(255 255 255 / 82%) 54%, rgb(239 246 255 / 82%) 100%),
    radial-gradient(circle at 76% 42%, rgb(10 132 255 / 14%), transparent 32%);
  box-shadow:
    0 24px 68px rgb(0 0 0 / 7%),
    inset 0 1px 0 rgb(255 255 255 / 92%);
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}

.hero-copy h2 {
  max-width: 760px;
  margin: 0;
  color: #1d1d1f;
  font-size: 44px;
  font-weight: 800;
  line-height: 1.08;
}

.hero-copy p {
  max-width: 720px;
  margin: 20px 0 0;
  color: #515154;
  font-size: 16px;
  line-height: 1.9;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 30px;
}

.hero-actions :deep(.el-button) {
  min-width: 150px;
  height: 44px;
}

.hero-visual {
  position: relative;
  min-height: 270px;
}

.glass-stack,
.glass-cube {
  position: absolute;
  display: grid;
  place-items: center;
  border: 1px solid rgb(255 255 255 / 76%);
  background: linear-gradient(145deg, rgb(255 255 255 / 74%), rgb(234 243 255 / 54%));
  box-shadow:
    0 28px 64px rgb(0 102 204 / 14%),
    inset 0 1px 0 rgb(255 255 255 / 80%);
  backdrop-filter: blur(16px);
}

.stack-back {
  inset: 16px 40px 46px 86px;
  border-radius: 8px;
  opacity: 0.56;
}

.stack-mid {
  right: 178px;
  bottom: 76px;
  width: 132px;
  height: 104px;
  border-radius: 8px;
  color: #7aa7e6;
  font-size: 38px;
}

.stack-front {
  right: 24px;
  bottom: 42px;
  width: 122px;
  height: 92px;
  border-radius: 8px;
  color: #0a84ff;
  font-size: 34px;
}

.glass-cube {
  right: 84px;
  bottom: 92px;
  width: 112px;
  height: 112px;
  border-radius: 8px;
  background: linear-gradient(145deg, #2f9bff, #0066cc);
  color: #ffffff;
  font-size: 48px;
  transform: rotate(-3deg);
}

.closed-loop-flow {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 0;
  padding: 20px;
}

.flow-node {
  position: relative;
  display: grid;
  align-content: start;
  justify-items: center;
  min-height: 154px;
  padding: 10px 12px;
  text-align: center;
}

.flow-node:not(:last-child)::after {
  position: absolute;
  top: 39px;
  right: -18px;
  width: 36px;
  height: 1px;
  background: linear-gradient(90deg, rgb(0 102 204 / 24%), rgb(0 102 204 / 48%));
  content: "";
}

.node-index {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 999px;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 12px;
  font-weight: 800;
}

.flow-node .el-icon {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  margin-top: 10px;
  border: 1px solid rgb(0 102 204 / 12%);
  border-radius: 999px;
  background: rgb(255 255 255 / 82%);
  color: var(--brand);
  font-size: 24px;
  box-shadow: 0 12px 28px rgb(0 102 204 / 8%);
}

.flow-node h4,
.feature-card h4 {
  margin: 12px 0 0;
  color: #1d1d1f;
  font-size: 15px;
  line-height: 1.3;
}

.flow-node p,
.feature-card p,
.version-item p {
  margin: 8px 0 0;
  color: #6e6e73;
  font-size: 13px;
  line-height: 1.65;
}

.feature-board {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  padding: 16px;
}

.feature-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  min-height: 148px;
  padding: 16px;
  border: 1px solid rgb(229 229 234 / 90%);
  border-radius: 8px;
  background: rgb(255 255 255 / 78%);
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 84%);
}

.feature-card.feature-active {
  border-color: rgb(0 102 204 / 32%);
  background: linear-gradient(180deg, rgb(255 255 255 / 88%), rgb(232 242 255 / 72%));
}

.feature-card.feature-planned {
  background: rgb(250 250 252 / 78%);
}

.feature-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border: 1px solid rgb(0 102 204 / 12%);
  border-radius: 8px;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 20px;
}

.feature-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.feature-title-row h4 {
  margin-top: 0;
}

.feature-state {
  flex: 0 0 auto;
  padding: 3px 7px;
  border-radius: 6px;
  background: rgb(52 199 89 / 11%);
  color: #16833a;
  font-size: 12px;
  font-weight: 700;
}

.feature-active .feature-state {
  background: rgb(0 102 204 / 12%);
  color: var(--brand);
}

.feature-planned .feature-state {
  background: rgb(134 134 139 / 13%);
  color: #6e6e73;
}

.evidence {
  display: block;
  margin-top: 10px;
  overflow: hidden;
  color: var(--brand);
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.panel-caption {
  color: var(--text-muted);
  font-size: 13px;
}

.compact {
  min-height: 46px;
}

.status-list,
.metric-list {
  display: grid;
  gap: 0;
  padding: 8px 16px 14px;
}

.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 48px;
  border-bottom: 1px solid rgb(229 229 234 / 72%);
  color: #515154;
  font-size: 13px;
}

.status-row:last-child {
  border-bottom: 0;
}

.status-row strong {
  min-width: 0;
  overflow: hidden;
  color: #1d1d1f;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  min-height: 62px;
  border-bottom: 1px solid rgb(229 229 234 / 72%);
}

.metric-row:last-child {
  border-bottom: 0;
}

.metric-row .el-icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 18px;
}

.metric-row small,
.version-item small {
  display: block;
  color: var(--text-muted);
  font-size: 12px;
}

.metric-row strong {
  display: block;
  margin-top: 3px;
  color: #1d1d1f;
  font-size: 20px;
  line-height: 1.1;
}

.version-timeline {
  display: grid;
  gap: 0;
  padding: 14px 16px 18px;
}

.version-item {
  position: relative;
  padding: 0 0 22px 24px;
  border-left: 1px solid rgb(210 210 215 / 82%);
}

.version-item:last-child {
  padding-bottom: 0;
}

.version-dot {
  position: absolute;
  top: 2px;
  left: -7px;
  width: 13px;
  height: 13px;
  border: 3px solid #ffffff;
  border-radius: 999px;
  background: #c7c7cc;
  box-shadow: 0 0 0 1px rgb(210 210 215 / 86%);
}

.version-item.done .version-dot {
  background: var(--brand);
  box-shadow: 0 0 0 1px rgb(0 102 204 / 34%);
}

.version-item strong {
  color: #1d1d1f;
  font-size: 14px;
}

@media (max-width: 1280px) {
  .showcase-shell,
  .hero-panel {
    grid-template-columns: 1fr;
  }

  .showcase-side {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1080px) {
  .feature-board {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .closed-loop-flow {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
  }

  .flow-node:not(:last-child)::after {
    display: none;
  }

  .showcase-side {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .showcase-page {
    padding: 14px;
  }

  .hero-panel {
    padding: 28px 22px;
  }

  .hero-copy h2 {
    font-size: 32px;
  }

  .hero-visual {
    min-height: 210px;
  }

  .feature-board,
  .closed-loop-flow {
    grid-template-columns: 1fr;
  }

  .panel-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
