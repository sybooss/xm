<template>
  <section class="operations-page">
    <section class="operations-hero">
      <div class="hero-copy">
        <h2>售后运营指挥中心</h2>
        <p>
          本轮新加 12 个特色功能，把会话、意图、工单、渠道、知识命中、订单风险和 AI 质量聚合成一个可答辩、可追踪、可验证的运营闭环。
        </p>
        <div class="hero-actions">
          <el-button type="primary" :icon="Refresh" :loading="loading" @click="loadInsights">刷新数据</el-button>
          <el-button :icon="Tickets" @click="$router.push('/service-tickets')">处理工单</el-button>
          <el-button :icon="Collection" @click="$router.push('/knowledge')">维护知识库</el-button>
        </div>
      </div>
      <div class="hero-panel">
        <div class="pulse-ring"></div>
        <div class="command-core">
          <strong>12</strong>
          <span>新增特色功能</span>
        </div>
      </div>
    </section>

    <section class="metric-grid operations-metrics">
      <article v-for="metric in insights.metrics" :key="metric.label" class="metric" :class="`metric-${metric.tone}`">
        <div class="metric-label">{{ metric.label }}</div>
        <div class="metric-value">{{ metric.value }}</div>
        <p>{{ metric.detail }}</p>
      </article>
    </section>

    <section class="panel">
      <div class="panel-header">
        <div>
          <h3 class="panel-title">答辩亮点矩阵</h3>
          <p class="panel-caption">每个新增特色功能都绑定入口、接口、证据和验收方式。</p>
        </div>
        <el-tag type="primary" effect="plain">本轮新增 {{ insights.newFeatures.length }} 项</el-tag>
      </div>
      <div class="feature-grid">
        <article v-for="feature in insights.newFeatures" :key="feature.sequence" class="feature-card">
          <span class="feature-index">{{ feature.sequence }}</span>
          <div>
            <div class="feature-title-row">
              <h4>{{ feature.title }}</h4>
              <span>{{ feature.category }}</span>
            </div>
            <p>{{ feature.detail }}</p>
            <dl>
              <div>
                <dt>入口</dt>
                <dd>{{ feature.route }}</dd>
              </div>
              <div>
                <dt>接口</dt>
                <dd>{{ feature.endpoint }}</dd>
              </div>
              <div>
                <dt>验收</dt>
                <dd>{{ feature.validation }}</dd>
              </div>
            </dl>
          </div>
        </article>
      </div>
    </section>

    <section class="insight-grid">
      <article class="panel insight-panel">
        <div class="panel-header compact">
          <h3 class="panel-title">意图热力雷达</h3>
        </div>
        <div class="rank-list">
          <div v-for="item in insights.intentInsights" :key="item.intentCode" class="rank-row">
            <span>
              <strong>{{ item.intentName }}</strong>
              <small>{{ item.intentCode }} · {{ item.suggestion }}</small>
            </span>
            <b>{{ item.shareLabel }}</b>
          </div>
        </div>
      </article>

      <article class="panel insight-panel">
        <div class="panel-header compact">
          <h3 class="panel-title">工单 SLA 风险队列</h3>
        </div>
        <div class="rank-list">
          <div v-for="item in insights.ticketInsights" :key="item.status" class="rank-row">
            <span>
              <strong>{{ item.status }}</strong>
              <small>{{ item.suggestedAction }}</small>
            </span>
            <b>{{ item.riskLabel }} · {{ item.count }}</b>
          </div>
        </div>
      </article>

      <article class="panel insight-panel">
        <div class="panel-header compact">
          <h3 class="panel-title">多渠道会话分布</h3>
        </div>
        <div class="rank-list">
          <div v-for="item in insights.channelInsights" :key="item.channel" class="rank-row">
            <span>
              <strong>{{ item.channel }}</strong>
              <small>{{ item.scenario }}</small>
            </span>
            <b>{{ item.shareLabel }}</b>
          </div>
        </div>
      </article>

      <article class="panel insight-panel">
        <div class="panel-header compact">
          <h3 class="panel-title">知识命中 Top 榜</h3>
        </div>
        <div class="rank-list">
          <div v-for="item in insights.knowledgeInsights" :key="item.title" class="rank-row">
            <span>
              <strong>{{ item.title }}</strong>
              <small>{{ item.action }}</small>
            </span>
            <b>{{ item.hitCount }} 次 · {{ item.scoreLabel }}</b>
          </div>
        </div>
      </article>

      <article class="panel insight-panel">
        <div class="panel-header compact">
          <h3 class="panel-title">订单风险扫描</h3>
        </div>
        <div class="risk-list">
          <div v-for="item in insights.orderRiskInsights" :key="item.orderNo" class="risk-row">
            <strong>{{ item.orderNo }}</strong>
            <span>{{ item.productName }} · {{ item.riskType }}</span>
            <p>{{ item.riskReason }}</p>
          </div>
        </div>
      </article>

      <article class="panel insight-panel">
        <div class="panel-header compact">
          <h3 class="panel-title">AI 运行质量摘要</h3>
        </div>
        <div class="ai-grid">
          <div v-for="item in insights.aiInsights" :key="item.label" class="ai-card" :class="`ai-${item.tone}`">
            <small>{{ item.label }}</small>
            <strong>{{ item.value }}</strong>
            <span>{{ item.detail }}</span>
          </div>
        </div>
      </article>
    </section>

    <section class="bottom-grid">
      <article class="panel">
        <div class="panel-header compact">
          <h3 class="panel-title">下一步动作清单</h3>
        </div>
        <div class="action-list">
          <button v-for="item in insights.actionItems" :key="item.title" class="action-row" type="button" @click="$router.push(item.route)">
            <span>
              <strong>{{ item.title }}</strong>
              <small>{{ item.owner }} · {{ item.detail }}</small>
            </span>
            <b>{{ item.priority }}</b>
          </button>
        </div>
      </article>

      <article class="panel">
        <div class="panel-header compact">
          <h3 class="panel-title">版本里程碑面板</h3>
        </div>
        <div class="version-timeline operations-version">
          <article v-for="item in insights.versionMilestones" :key="item.version" class="version-item" :class="{ done: item.done }">
            <span class="version-dot"></span>
            <strong>{{ item.version }}</strong>
            <p>{{ item.detail }}</p>
            <small>{{ item.date }}</small>
          </article>
        </div>
      </article>
    </section>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Collection, Refresh, Tickets } from '@element-plus/icons-vue'
import { getOperationInsights } from '../api/operationApi'

const loading = ref(false)
const insights = reactive({
  metrics: [],
  newFeatures: [],
  intentInsights: [],
  ticketInsights: [],
  channelInsights: [],
  knowledgeInsights: [],
  orderRiskInsights: [],
  aiInsights: [],
  actionItems: [],
  versionMilestones: []
})

async function loadInsights() {
  loading.value = true
  try {
    const data = await getOperationInsights()
    Object.assign(insights, {
      metrics: data.metrics || [],
      newFeatures: data.newFeatures || [],
      intentInsights: data.intentInsights || [],
      ticketInsights: data.ticketInsights || [],
      channelInsights: data.channelInsights || [],
      knowledgeInsights: data.knowledgeInsights || [],
      orderRiskInsights: data.orderRiskInsights || [],
      aiInsights: data.aiInsights || [],
      actionItems: data.actionItems || [],
      versionMilestones: data.versionMilestones || []
    })
  } finally {
    loading.value = false
  }
}

onMounted(loadInsights)
</script>

<style scoped>
.operations-page {
  display: grid;
  gap: 16px;
  min-height: calc(100vh - var(--header-height));
  padding: 22px;
}

.operations-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
  min-height: 330px;
  overflow: hidden;
  padding: 52px;
  border: 1px solid rgb(229 229 234 / 86%);
  border-radius: 8px;
  background:
    linear-gradient(112deg, rgb(255 255 255 / 97%) 0%, rgb(255 255 255 / 86%) 55%, rgb(232 244 255 / 88%) 100%),
    radial-gradient(circle at 82% 34%, rgb(0 102 204 / 16%), transparent 30%);
  box-shadow:
    0 28px 76px rgb(0 0 0 / 7%),
    inset 0 1px 0 rgb(255 255 255 / 92%);
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.hero-copy h2 {
  max-width: 760px;
  margin: 0;
  color: #1d1d1f;
  font-size: 48px;
  font-weight: 800;
  line-height: 1.06;
}

.hero-copy p {
  max-width: 760px;
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
  height: 44px;
}

.hero-panel {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 240px;
}

.pulse-ring {
  position: absolute;
  width: 238px;
  height: 238px;
  border: 1px solid rgb(0 102 204 / 18%);
  border-radius: 50%;
  background:
    radial-gradient(circle, rgb(0 102 204 / 12%) 0 18%, transparent 19%),
    conic-gradient(from 120deg, rgb(0 102 204 / 34%), rgb(52 199 89 / 20%), rgb(255 255 255 / 0), rgb(0 102 204 / 34%));
  filter: drop-shadow(0 28px 44px rgb(0 102 204 / 14%));
}

.command-core {
  position: relative;
  z-index: 1;
  display: grid;
  place-items: center;
  width: 152px;
  height: 152px;
  border: 1px solid rgb(255 255 255 / 86%);
  border-radius: 50%;
  background: rgb(255 255 255 / 72%);
  box-shadow:
    0 26px 62px rgb(0 102 204 / 18%),
    inset 0 1px 0 rgb(255 255 255 / 88%);
  backdrop-filter: blur(18px);
}

.command-core strong {
  color: #0066cc;
  font-size: 56px;
  line-height: 0.9;
}

.command-core span {
  color: #515154;
  font-size: 13px;
  font-weight: 700;
}

.operations-metrics .metric p {
  margin: 8px 0 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.55;
}

.metric-primary {
  background: linear-gradient(180deg, rgb(255 255 255 / 90%), rgb(232 242 255 / 76%));
}

.metric-warning {
  background: linear-gradient(180deg, rgb(255 255 255 / 90%), rgb(255 247 237 / 78%));
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  padding: 16px;
}

.feature-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  min-height: 196px;
  padding: 16px;
  border: 1px solid rgb(229 229 234 / 90%);
  border-radius: 8px;
  background: rgb(255 255 255 / 78%);
}

.feature-index {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 13px;
  font-weight: 800;
}

.feature-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.feature-title-row h4 {
  margin: 0;
  color: #1d1d1f;
  font-size: 15px;
}

.feature-title-row span {
  flex: 0 0 auto;
  color: var(--brand);
  font-size: 12px;
  font-weight: 700;
}

.feature-card p,
.risk-row p {
  margin: 8px 0 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.feature-card dl {
  display: grid;
  gap: 6px;
  margin: 12px 0 0;
}

.feature-card dl div {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  gap: 8px;
}

.feature-card dt,
.feature-card dd {
  margin: 0;
  font-size: 12px;
  line-height: 1.45;
}

.feature-card dt {
  color: var(--text-muted);
}

.feature-card dd {
  overflow: hidden;
  color: #1d1d1f;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.panel-caption {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 13px;
}

.insight-grid,
.bottom-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.rank-list,
.risk-list,
.action-list {
  display: grid;
  padding: 8px 16px 16px;
}

.rank-row,
.action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  min-height: 62px;
  border-bottom: 1px solid rgb(229 229 234 / 72%);
}

.rank-row:last-child,
.action-row:last-child {
  border-bottom: 0;
}

.rank-row span,
.action-row span {
  min-width: 0;
}

.rank-row strong,
.risk-row strong,
.action-row strong {
  display: block;
  color: #1d1d1f;
  font-size: 14px;
}

.rank-row small,
.action-row small {
  display: block;
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.45;
}

.rank-row b,
.action-row b {
  flex: 0 0 auto;
  color: var(--brand);
  font-size: 13px;
}

.risk-row {
  min-height: 86px;
  padding: 12px 0;
  border-bottom: 1px solid rgb(229 229 234 / 72%);
}

.risk-row:last-child {
  border-bottom: 0;
}

.risk-row span {
  display: block;
  margin-top: 4px;
  color: var(--brand);
  font-size: 12px;
  font-weight: 700;
}

.ai-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 16px;
}

.ai-card {
  min-height: 118px;
  padding: 14px;
  border: 1px solid rgb(229 229 234 / 90%);
  border-radius: 8px;
  background: rgb(255 255 255 / 78%);
}

.ai-card small,
.ai-card span {
  display: block;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.45;
}

.ai-card strong {
  display: block;
  margin: 8px 0;
  color: #1d1d1f;
  font-size: 24px;
}

.action-row {
  width: 100%;
  padding: 0;
  border-top: 0;
  border-right: 0;
  border-left: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.operations-version {
  padding: 16px 18px;
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

.version-item p {
  margin: 8px 0 0;
  color: #6e6e73;
  font-size: 13px;
  line-height: 1.65;
}

.version-item small {
  display: block;
  margin-top: 6px;
  color: var(--text-muted);
  font-size: 12px;
}

@media (max-width: 1180px) {
  .operations-hero,
  .insight-grid,
  .bottom-grid {
    grid-template-columns: 1fr;
  }

  .feature-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .operations-page {
    padding: 14px;
  }

  .operations-hero {
    padding: 30px 22px;
  }

  .hero-copy h2 {
    font-size: 34px;
  }

  .feature-grid,
  .ai-grid {
    grid-template-columns: 1fr;
  }
}
</style>
