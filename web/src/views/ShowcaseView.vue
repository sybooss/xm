<template>
  <section class="operations-page">
    <div class="operations-shell">
      <main class="operations-main">
        <section class="ops-hero">
          <div class="ops-copy">
            <span class="ops-kicker">售后运营首页</span>
            <h2>退换货客服处理台</h2>
            <p>
              聚合咨询接待、售后审核、SLA 跟进、人工工单、知识库和服务日志，
              帮助客服按真实退换货流程处理每日问题。
            </p>
            <div class="ops-actions">
              <el-button type="primary" :icon="View" size="large" @click="$router.push('/admin/after-sales/review')">
                进入待处理队列
              </el-button>
              <el-button :icon="ChatDotRound" size="large" @click="$router.push('/chat')">
                打开咨询接待
              </el-button>
            </div>
          </div>

          <div class="ops-status-card">
            <div class="status-card-head">
              <span>运行状态</span>
              <el-button :icon="Refresh" circle :loading="systemStore.loading" @click="loadStatus" />
            </div>
            <div class="status-list">
              <div class="status-row">
                <span>数据库</span>
                <StatusTag :value="systemStore.database.status" />
              </div>
              <div class="status-row">
                <span>AI 辅助</span>
                <StatusTag :value="systemStore.ai.status" />
              </div>
              <div class="status-row">
                <span>当前模型</span>
                <strong>{{ systemStore.ai.modelName || 'local-rule-template' }}</strong>
              </div>
              <div class="status-row">
                <span>本地兜底</span>
                <strong>{{ systemStore.ai.fallbackEnabled ? '已启用' : '未开启' }}</strong>
              </div>
            </div>
          </div>
        </section>

        <section class="metric-grid ops-metrics">
          <div v-for="metric in queueMetrics" :key="metric.label" class="metric">
            <div class="metric-label">{{ metric.label }}</div>
            <div class="metric-value">{{ metric.value }}</div>
            <p class="metric-note">{{ metric.note }}</p>
          </div>
        </section>

        <section class="ops-grid">
          <div class="panel">
            <div class="panel-header">
              <div>
                <h3 class="panel-title">客服处理流</h3>
                <p class="panel-note">从用户咨询到售后完成，每一步都保留业务依据和服务记录。</p>
              </div>
            </div>
            <div class="workflow-list">
              <article v-for="(step, index) in workflowSteps" :key="step.title" class="workflow-step">
                <span class="step-index">{{ index + 1 }}</span>
                <div>
                  <h4>{{ step.title }}</h4>
                  <p>{{ step.detail }}</p>
                </div>
                <el-button link type="primary" @click="$router.push(step.path)">处理</el-button>
              </article>
            </div>
          </div>

          <div class="panel">
            <div class="panel-header">
              <div>
                <h3 class="panel-title">今日关注</h3>
                <p class="panel-note">优先处理会影响顾客体验和 SLA 的事项。</p>
              </div>
            </div>
            <div class="focus-list">
              <article v-for="item in focusItems" :key="item.title" class="focus-item">
                <div class="focus-icon">
                  <el-icon><component :is="item.icon" /></el-icon>
                </div>
                <div>
                  <h4>{{ item.title }}</h4>
                  <p>{{ item.detail }}</p>
                  <span>{{ item.action }}</span>
                </div>
              </article>
            </div>
          </div>
        </section>
      </main>

      <aside class="operations-side">
        <section class="panel">
          <div class="panel-header">
            <h3 class="panel-title">快捷入口</h3>
          </div>
          <div class="quick-entry-list">
            <button v-for="entry in quickEntries" :key="entry.path" type="button" @click="$router.push(entry.path)">
              <el-icon><component :is="entry.icon" /></el-icon>
              <span>{{ entry.label }}</span>
              <small>{{ entry.desc }}</small>
            </button>
          </div>
        </section>

        <section class="panel">
          <div class="panel-header">
            <h3 class="panel-title">质检依据</h3>
          </div>
          <div class="quality-list">
            <div v-for="item in qualityItems" :key="item.title" class="quality-item">
              <strong>{{ item.title }}</strong>
              <span>{{ item.detail }}</span>
            </div>
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
  Cpu,
  DataAnalysis,
  DocumentChecked,
  Refresh,
  Service,
  Tickets,
  TrendCharts,
  UserFilled,
  View
} from '@element-plus/icons-vue'
import StatusTag from '../components/common/StatusTag.vue'
import { useSystemStore } from '../stores/systemStore'

const systemStore = useSystemStore()

const queueMetrics = [
  { label: '待处理售后', value: '队列', note: '集中处理退货、换货、退款和投诉申请' },
  { label: 'SLA 风险', value: '预警', note: '优先跟进待补材料、超时和高优先级事项' },
  { label: '人工工单', value: '接管', note: '承接投诉、物流异常和复杂协商场景' },
  { label: '知识质检', value: '留痕', note: '回复依据、AI 状态和处理轨迹可追溯' }
]

const workflowSteps = [
  { title: '咨询接待', detail: '绑定订单、识别退换货意图，给出处理建议和知识依据。', path: '/chat' },
  { title: '售后审核', detail: '检查申请原因、凭证、风险等级和可通过金额。', path: '/admin/after-sales/review' },
  { title: 'SLA 跟进', detail: '跟踪待补材料、即将超时和高优先级售后。', path: '/admin/sla' },
  { title: '人工工单', detail: '处理投诉升级、物流异常和需要人工接管的会话。', path: '/service-tickets' },
  { title: '知识沉淀', detail: '维护退换货规则，复盘命中依据和客服回复质量。', path: '/knowledge' }
]

const focusItems = [
  { title: '优先处理高风险申请', detail: '金额较高、凭证不足或重复投诉的售后需要先复核。', action: '查看售后审核工作台', icon: View },
  { title: '监控商品质量预警', detail: '同一商品重复出现断连、破损、错发等问题时及时跟进。', action: '查看商品质量预警', icon: TrendCharts },
  { title: '复盘服务日志', detail: '检查 AI 辅助、知识命中和人工接管是否形成完整记录。', action: '查看服务日志', icon: DocumentChecked }
]

const quickEntries = [
  { label: '售后审核', desc: '待处理队列', path: '/admin/after-sales/review', icon: View },
  { label: '咨询接待', desc: '实时会话', path: '/chat', icon: ChatDotRound },
  { label: '人工工单', desc: '投诉接管', path: '/service-tickets', icon: Service },
  { label: '订单管理', desc: '订单上下文', path: '/orders', icon: Tickets },
  { label: '客户画像', desc: '历史风险', path: '/admin/customers/profile', icon: UserFilled },
  { label: '知识库', desc: '规则依据', path: '/knowledge', icon: Collection },
  { label: 'AI 质检', desc: '模型与兜底', path: '/ai-test', icon: Cpu },
  { label: '系统状态', desc: '接口与日志', path: '/dashboard', icon: DataAnalysis }
]

const qualityItems = [
  { title: '回复有依据', detail: '客服回复要能对应知识命中、订单状态或人工备注。' },
  { title: '风险不自动放行', detail: '图片风险、凭证不足和高额退款只做提醒，由人工确认。' },
  { title: '状态可追踪', detail: '审核、补材料、工单接管和完成动作都写入处理记录。' }
]

function loadStatus() {
  systemStore.loadStatus().catch(() => {})
}

onMounted(loadStatus)
</script>

<style scoped>
.operations-page {
  min-height: calc(100vh - var(--header-height));
  padding: 22px;
}

.operations-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
}

.operations-main,
.operations-side {
  display: grid;
  align-content: start;
  gap: 16px;
  min-width: 0;
}

.ops-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 0.42fr);
  gap: 16px;
  padding: 28px;
  border: 1px solid rgb(229 229 234 / 90%);
  border-radius: var(--radius);
  background: rgb(255 255 255 / 82%);
  box-shadow: 0 18px 46px rgb(0 0 0 / 5%);
}

.ops-copy {
  display: grid;
  align-content: center;
  min-width: 0;
}

.ops-kicker {
  color: var(--brand);
  font-size: 13px;
  font-weight: 800;
}

.ops-copy h2 {
  margin: 8px 0 0;
  color: #1d1d1f;
  font-size: 34px;
  line-height: 1.15;
}

.ops-copy p {
  max-width: 720px;
  margin: 14px 0 0;
  color: #515154;
  font-size: 15px;
  line-height: 1.8;
}

.ops-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.ops-status-card {
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius);
  background: var(--surface-soft);
}

.status-card-head,
.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.status-card-head {
  margin-bottom: 8px;
  color: #1d1d1f;
  font-size: 14px;
  font-weight: 800;
}

.status-list {
  display: grid;
}

.status-row {
  min-height: 42px;
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
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ops-metrics .metric {
  min-height: 112px;
}

.metric-note {
  margin: 8px 0 0;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.5;
}

.ops-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 0.74fr);
  gap: 16px;
}

.panel-note {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 12px;
}

.workflow-list,
.focus-list,
.quality-list {
  display: grid;
  gap: 0;
  padding: 8px 14px 14px;
}

.workflow-step {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-height: 82px;
  border-bottom: 1px solid var(--line-soft);
}

.workflow-step:last-child {
  border-bottom: 0;
}

.step-index {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 999px;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 12px;
  font-weight: 800;
}

.workflow-step h4,
.focus-item h4 {
  margin: 0;
  color: #1d1d1f;
  font-size: 14px;
}

.workflow-step p,
.focus-item p {
  margin: 5px 0 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.55;
}

.focus-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--line-soft);
}

.focus-item:last-child {
  border-bottom: 0;
}

.focus-icon {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--brand-soft);
  color: var(--brand);
}

.focus-item span {
  display: inline-block;
  margin-top: 8px;
  color: var(--brand);
  font-size: 12px;
  font-weight: 700;
}

.quick-entry-list {
  display: grid;
  gap: 8px;
  padding: 12px;
}

.quick-entry-list button {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr);
  gap: 6px 10px;
  align-items: center;
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #fff;
  color: var(--text);
  text-align: left;
  cursor: pointer;
}

.quick-entry-list button:hover {
  border-color: #bfdbfe;
  background: var(--brand-soft);
}

.quick-entry-list .el-icon {
  grid-row: span 2;
  color: var(--brand);
}

.quick-entry-list span {
  overflow: hidden;
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-entry-list small {
  color: var(--text-muted);
  font-size: 12px;
}

.quality-item {
  display: grid;
  gap: 5px;
  padding: 12px 0;
  border-bottom: 1px solid var(--line-soft);
}

.quality-item:last-child {
  border-bottom: 0;
}

.quality-item strong {
  color: #1d1d1f;
  font-size: 13px;
}

.quality-item span {
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.55;
}

@media (max-width: 1180px) {
  .operations-shell,
  .ops-hero,
  .ops-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .operations-page {
    padding: 14px;
  }

  .ops-hero {
    padding: 22px;
  }

  .ops-copy h2 {
    font-size: 28px;
  }

  .workflow-step {
    grid-template-columns: 28px minmax(0, 1fr);
  }

  .workflow-step .el-button {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
