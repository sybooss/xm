<template>
  <section class="closure-page">
    <section class="closure-hero">
      <div class="hero-main">
        <h2>特色闭环中心</h2>
        <p>
          新增 14 个可落地的售后 AI 特色功能，每个功能都绑定真实数据、诊断判断、下一步动作、页面入口和验收证据，方便答辩时从“能看”讲到“能跑通”。
        </p>
        <div class="hero-actions">
          <el-button type="primary" :icon="Refresh" :loading="loading" @click="loadDashboard">刷新闭环</el-button>
          <el-button :icon="ChatDotRound" @click="$router.push('/chat')">演示咨询</el-button>
          <el-button :icon="DocumentChecked" @click="$router.push('/logs')">查看证据</el-button>
        </div>
      </div>
      <div class="hero-proof">
        <span>10+ 新增特色功能</span>
        <strong>{{ dashboard.closures.length }}</strong>
        <small>{{ closedCount }} 个已绑定闭环证据</small>
      </div>
    </section>

    <section class="closure-metrics">
      <article v-for="metric in dashboard.metrics" :key="metric.label" class="metric-card" :class="`tone-${metric.tone}`">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <p>{{ metric.detail }}</p>
      </article>
    </section>

    <section class="closure-layout">
      <div class="closure-list">
        <article
          v-for="item in dashboard.closures"
          :key="item.code"
          class="closure-card"
          :class="`tone-${item.tone}`"
        >
          <div class="card-index">{{ item.sequence }}</div>
          <div class="card-body">
            <div class="card-topline">
              <div>
                <h3>{{ item.title }}</h3>
                <span>{{ item.code }} · {{ item.capability }}</span>
              </div>
              <el-tag :type="tagType(item.tone)" effect="plain">{{ item.status }}</el-tag>
            </div>
            <p class="diagnosis">{{ item.diagnosis }}</p>
            <div class="signal-grid">
              <div>
                <small>实时信号</small>
                <b>{{ item.signal }}</b>
              </div>
              <div>
                <small>下一步动作</small>
                <b>{{ item.nextAction }}</b>
              </div>
              <div>
                <small>验收证据</small>
                <b>{{ item.evidence }}</b>
              </div>
            </div>
            <div class="card-footer">
              <span>{{ item.sourceInspiration }}</span>
              <button type="button" @click="$router.push(item.route)">
                进入闭环
                <el-icon><ArrowRight /></el-icon>
              </button>
            </div>
          </div>
          <div class="score-rail">
            <strong>{{ item.score }}</strong>
            <span>闭环分</span>
          </div>
        </article>
      </div>

      <aside class="closure-side">
        <section class="side-panel">
          <div class="side-title">
            <h3>演示路线</h3>
            <span>闭环脚本</span>
          </div>
          <ol class="demo-steps">
            <li v-for="step in dashboard.demoSteps" :key="step.sequence">
              <button type="button" @click="$router.push(step.route)">
                <strong>{{ step.sequence }}. {{ step.title }}</strong>
                <span>{{ step.proof }}</span>
              </button>
            </li>
          </ol>
        </section>

        <section class="side-panel">
          <div class="side-title">
            <h3>参考项目落点</h3>
            <span>借鉴但不照搬</span>
          </div>
          <div class="reference-list">
            <a v-for="item in dashboard.references" :key="item.name" :href="item.url" target="_blank" rel="noreferrer">
              <strong>{{ item.name }}</strong>
              <span>{{ item.borrowedIdea }}</span>
              <small>{{ item.localLanding }}</small>
            </a>
          </div>
        </section>
      </aside>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ArrowRight, ChatDotRound, DocumentChecked, Refresh } from '@element-plus/icons-vue'
import { getFeatureClosures } from '../api/operationApi'

const loading = ref(false)
const dashboard = reactive({
  metrics: [],
  closures: [],
  demoSteps: [],
  references: []
})

const closedCount = computed(() => dashboard.closures.filter(item => item.closedLoop).length)

function tagType(tone) {
  if (tone === 'success') return 'success'
  if (tone === 'warning') return 'warning'
  if (tone === 'danger') return 'danger'
  return 'primary'
}

async function loadDashboard() {
  loading.value = true
  try {
    const data = await getFeatureClosures()
    Object.assign(dashboard, {
      metrics: data.metrics || [],
      closures: data.closures || [],
      demoSteps: data.demoSteps || [],
      references: data.references || []
    })
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<style scoped>
.closure-page {
  display: grid;
  gap: 16px;
  min-height: calc(100vh - var(--header-height));
  padding: 22px;
}

.closure-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 18px;
  min-height: 320px;
  padding: 48px;
  border: 1px solid rgb(229 229 234 / 90%);
  border-radius: 8px;
  background:
    linear-gradient(120deg, rgb(255 255 255 / 98%), rgb(246 250 255 / 92%)),
    linear-gradient(180deg, rgb(255 255 255 / 100%), rgb(242 242 247 / 72%));
  box-shadow:
    0 28px 80px rgb(0 0 0 / 7%),
    inset 0 1px 0 rgb(255 255 255 / 96%);
}

.hero-main {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.hero-main h2 {
  max-width: 780px;
  margin: 0;
  color: #1d1d1f;
  font-size: 50px;
  font-weight: 800;
  line-height: 1.05;
}

.hero-main p {
  max-width: 820px;
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

.hero-proof {
  align-self: stretch;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 220px;
  padding: 28px;
  border: 1px solid rgb(210 210 215 / 80%);
  border-radius: 8px;
  background: rgb(255 255 255 / 82%);
}

.hero-proof span,
.hero-proof small {
  color: #6e6e73;
  font-size: 13px;
  font-weight: 700;
}

.hero-proof strong {
  margin: 12px 0;
  color: #0066cc;
  font-size: 74px;
  line-height: 0.95;
}

.closure-metrics {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  min-height: 128px;
  padding: 16px;
  border: 1px solid rgb(229 229 234 / 92%);
  border-radius: 8px;
  background: rgb(255 255 255 / 82%);
}

.metric-card span,
.metric-card p {
  color: #6e6e73;
  font-size: 12px;
  line-height: 1.5;
}

.metric-card strong {
  display: block;
  margin: 10px 0;
  color: #1d1d1f;
  font-size: 26px;
}

.tone-primary {
  box-shadow: inset 3px 0 0 #0066cc;
}

.tone-success {
  box-shadow: inset 3px 0 0 #059669;
}

.tone-warning {
  box-shadow: inset 3px 0 0 #d97706;
}

.tone-danger {
  box-shadow: inset 3px 0 0 #dc2626;
}

.tone-info {
  box-shadow: inset 3px 0 0 #0284c7;
}

.closure-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 16px;
  align-items: start;
}

.closure-list {
  display: grid;
  gap: 12px;
}

.closure-card {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) 88px;
  gap: 14px;
  min-height: 220px;
  padding: 16px;
  border: 1px solid rgb(229 229 234 / 92%);
  border-radius: 8px;
  background: rgb(255 255 255 / 82%);
  box-shadow: 0 18px 42px rgb(0 0 0 / 4%);
}

.card-index {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f2f2f7;
  color: #0066cc;
  font-size: 14px;
  font-weight: 800;
}

.card-body {
  min-width: 0;
}

.card-topline {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.card-topline h3 {
  margin: 0;
  color: #1d1d1f;
  font-size: 18px;
  line-height: 1.3;
}

.card-topline span,
.diagnosis,
.card-footer span {
  color: #6e6e73;
  font-size: 13px;
  line-height: 1.6;
}

.diagnosis {
  margin: 10px 0 0;
}

.signal-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.signal-grid div {
  min-height: 82px;
  padding: 12px;
  border: 1px solid rgb(229 229 234 / 86%);
  border-radius: 8px;
  background: rgb(251 251 253 / 78%);
}

.signal-grid small {
  display: block;
  color: #86868b;
  font-size: 12px;
}

.signal-grid b {
  display: block;
  margin-top: 6px;
  color: #1d1d1f;
  font-size: 13px;
  font-weight: 650;
  line-height: 1.55;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
}

.card-footer button,
.demo-steps button {
  border: 0;
  background: transparent;
  color: #0066cc;
  font-weight: 700;
  cursor: pointer;
}

.card-footer button {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 8px 0;
}

.score-rail {
  display: grid;
  place-items: center;
  align-self: stretch;
  border-left: 1px solid rgb(229 229 234 / 92%);
}

.score-rail strong {
  color: #1d1d1f;
  font-size: 28px;
}

.score-rail span {
  color: #86868b;
  font-size: 12px;
  font-weight: 700;
}

.closure-side {
  position: sticky;
  top: calc(var(--header-height) + 16px);
  display: grid;
  gap: 16px;
}

.side-panel {
  border: 1px solid rgb(229 229 234 / 92%);
  border-radius: 8px;
  background: rgb(255 255 255 / 82%);
  box-shadow: 0 18px 42px rgb(0 0 0 / 4%);
}

.side-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid rgb(229 229 234 / 82%);
}

.side-title h3 {
  margin: 0;
  color: #1d1d1f;
  font-size: 15px;
}

.side-title span {
  color: #86868b;
  font-size: 12px;
  font-weight: 700;
}

.demo-steps {
  display: grid;
  gap: 0;
  margin: 0;
  padding: 8px 16px 16px;
  list-style: none;
}

.demo-steps li {
  border-bottom: 1px solid rgb(229 229 234 / 72%);
}

.demo-steps li:last-child {
  border-bottom: 0;
}

.demo-steps button {
  width: 100%;
  padding: 13px 0;
  text-align: left;
}

.demo-steps strong,
.reference-list strong {
  display: block;
  color: #1d1d1f;
  font-size: 13px;
}

.demo-steps span,
.reference-list span,
.reference-list small {
  display: block;
  margin-top: 5px;
  color: #6e6e73;
  font-size: 12px;
  line-height: 1.45;
}

.reference-list {
  display: grid;
  padding: 8px 16px 16px;
}

.reference-list a {
  padding: 13px 0;
  border-bottom: 1px solid rgb(229 229 234 / 72%);
  text-decoration: none;
}

.reference-list a:last-child {
  border-bottom: 0;
}

@media (max-width: 1280px) {
  .closure-metrics {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .closure-layout,
  .closure-hero {
    grid-template-columns: 1fr;
  }

  .closure-side {
    position: static;
  }
}

@media (max-width: 820px) {
  .closure-page {
    padding: 14px;
  }

  .closure-hero {
    padding: 30px 22px;
  }

  .hero-main h2 {
    font-size: 36px;
  }

  .closure-metrics,
  .signal-grid {
    grid-template-columns: 1fr;
  }

  .closure-card {
    grid-template-columns: 1fr;
  }

  .score-rail {
    min-height: 70px;
    border-top: 1px solid rgb(229 229 234 / 92%);
    border-left: 0;
  }

  .card-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
