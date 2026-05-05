<template>
  <section class="page">
    <div class="page-header">
      <div>
        <p class="page-subtitle">承接投诉、物流异常和手动转人工诉求，展示优先级、处理状态和接续建议。</p>
      </div>
      <el-button :icon="Refresh" @click="loadTickets">刷新</el-button>
    </div>

    <section class="panel">
      <div class="panel-header">
        <div class="toolbar">
          <el-input v-model="query.keyword" placeholder="工单号、订单号或问题" clearable style="width: 260px" @keyup.enter="loadTickets" />
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
          <el-select v-model="query.priority" placeholder="优先级" clearable style="width: 140px">
            <el-option label="普通" value="NORMAL" />
            <el-option label="高" value="HIGH" />
            <el-option label="紧急" value="URGENT" />
          </el-select>
          <el-button :icon="Search" @click="loadTickets">查询</el-button>
        </div>
      </div>

      <div class="panel-body">
        <el-table v-loading="loading" :data="tickets" height="460" @row-click="selectTicket">
          <el-table-column prop="ticketNo" label="工单号" width="175" />
          <el-table-column prop="orderNo" label="订单号" width="160" />
          <el-table-column prop="intentCode" label="意图" width="150" show-overflow-tooltip />
          <el-table-column label="优先级" width="95">
            <template #default="{ row }"><StatusTag :value="row.priority" /></template>
          </el-table-column>
          <el-table-column label="状态" width="105">
            <template #default="{ row }"><StatusTag :value="row.status" /></template>
          </el-table-column>
          <el-table-column prop="customerIssue" label="用户问题" min-width="240" show-overflow-tooltip />
          <el-table-column prop="assignedTo" label="处理人" width="120" />
          <el-table-column prop="createdAt" label="创建时间" width="180" />
        </el-table>
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          class="pager"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @current-change="loadTickets"
          @size-change="loadTickets"
        />
      </div>
    </section>

    <section v-if="selected" class="panel detail-panel">
      <div class="panel-header">
        <div>
          <h3 class="panel-title">工单详情：{{ selected.ticketNo }}</h3>
          <p class="detail-subtitle">把投诉转接、AI 摘要、处理建议和 SLA 风险集中到一个可演示闭环。</p>
        </div>
        <div class="detail-actions">
          <el-select v-model="selected.status" size="small" style="width: 120px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
          <el-button size="small" type="primary" :icon="Check" @click="saveStatus">保存</el-button>
        </div>
      </div>
      <div class="panel-body ticket-detail">
        <div class="ticket-hero">
          <div class="ticket-issue">
            <div class="eyebrow">客户诉求</div>
            <h4>{{ selected.customerIssue }}</h4>
            <p>{{ selected.aiSummary || '暂无 AI 摘要，客服可先依据用户原始描述处理。' }}</p>
          </div>
          <div class="sla-card" :class="slaLevelClass">
            <span>SLA 风险</span>
            <strong>{{ slaRisk.label }}</strong>
            <small>{{ slaRisk.detail }}</small>
          </div>
        </div>

        <div class="detail-grid">
          <el-descriptions class="detail-descriptions" :column="2" border>
            <el-descriptions-item label="订单号">{{ selected.orderNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="会话号">{{ selected.sessionNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="优先级"><StatusTag :value="selected.priority" /></el-descriptions-item>
            <el-descriptions-item label="状态"><StatusTag :value="selected.status" /></el-descriptions-item>
            <el-descriptions-item label="处理人" :span="2">{{ selected.assignedTo || '待分配客服' }}</el-descriptions-item>
            <el-descriptions-item label="处理建议" :span="2">{{ selected.suggestedAction || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="next-action">
            <div class="eyebrow">下一步动作</div>
            <h4>{{ nextAction.title }}</h4>
            <p>{{ nextAction.detail }}</p>
          </div>
        </div>

        <div class="ticket-flow">
          <div class="flow-header">
            <div>
              <div class="eyebrow">售后处理时间线</div>
              <h4>从 AI 转人工到闭环处理</h4>
            </div>
            <StatusTag :value="selected.status" />
          </div>
          <div class="flow-steps">
            <div v-for="step in timelineSteps" :key="step.key" class="flow-step" :class="step.state">
              <div class="step-dot"></div>
              <div>
                <strong>{{ step.title }}</strong>
                <span>{{ step.time }}</span>
                <p>{{ step.description }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Refresh, Search } from '@element-plus/icons-vue'
import StatusTag from '../components/common/StatusTag.vue'
import { pageTickets, updateTicket } from '../api/serviceTicketApi'

const query = reactive({ page: 1, pageSize: 10, keyword: '', status: '', priority: '' })
const tickets = ref([])
const total = ref(0)
const loading = ref(false)
const selected = ref(null)

const statusRank = {
  PENDING: 0,
  PROCESSING: 1,
  RESOLVED: 2,
  CLOSED: 3
}

const slaRisk = computed(() => {
  const priority = selected.value?.priority
  const status = selected.value?.status
  if (['RESOLVED', 'CLOSED'].includes(status)) {
    return { label: '已闭环', detail: '工单已完成处理，可作为答辩闭环证据。', level: 'closed' }
  }
  if (priority === 'URGENT') {
    return { label: '高风险', detail: '紧急工单需要优先分配客服并持续跟踪。', level: 'danger' }
  }
  if (priority === 'HIGH') {
    return { label: '需关注', detail: '高优先级投诉建议当天完成响应。', level: 'warning' }
  }
  return { label: '稳定', detail: '普通工单按标准售后流程推进。', level: 'stable' }
})

const slaLevelClass = computed(() => `sla-${slaRisk.value.level}`)

const nextAction = computed(() => {
  const ticket = selected.value || {}
  if (ticket.status === 'PENDING') {
    return {
      title: '分配客服并确认诉求',
      detail: ticket.assignedTo
        ? `${ticket.assignedTo} 已被记录为处理人，下一步确认用户诉求和订单状态。`
        : '建议先分配客服，再核对订单、售后记录和 AI 摘要。'
    }
  }
  if (ticket.status === 'PROCESSING') {
    return {
      title: '按建议动作推进处理',
      detail: ticket.suggestedAction || '结合会话上下文与售后规则，补充处理记录并同步用户。'
    }
  }
  if (ticket.status === 'RESOLVED') {
    return {
      title: '回访用户并沉淀知识',
      detail: '确认用户满意后，可将典型问题补充到知识库，提高后续自动答复质量。'
    }
  }
  return {
    title: '归档闭环证据',
    detail: '工单已关闭，可在日志中心和会话记录中追溯处理过程。'
  }
})

const timelineSteps = computed(() => {
  const ticket = selected.value || {}
  const rank = statusRank[ticket.status] ?? 0
  const createdAt = formatDateTime(ticket.createdAt)
  const updatedAt = formatDateTime(ticket.updatedAt)
  const resolvedAt = formatDateTime(ticket.resolvedAt || ticket.updatedAt)
  return [
    {
      key: 'created',
      title: 'AI 识别并转人工',
      time: createdAt,
      description: ticket.intentCode ? `识别意图 ${ticket.intentCode}，生成工单并保留原始问题。` : '生成工单并保留原始问题。',
      state: 'done'
    },
    {
      key: 'assigned',
      title: ticket.assignedTo ? '客服接单' : '等待客服接单',
      time: ticket.assignedTo ? updatedAt : '待分配',
      description: ticket.assignedTo ? `${ticket.assignedTo} 负责跟进该工单。` : '管理员可分配客服并开始处理。',
      state: rank >= 1 || ticket.assignedTo ? 'done' : 'active'
    },
    {
      key: 'processing',
      title: '售后方案处理中',
      time: rank >= 1 ? updatedAt : '待推进',
      description: ticket.suggestedAction || '按订单状态、售后规则和 AI 摘要形成处理方案。',
      state: rank >= 2 ? 'done' : rank === 1 ? 'active' : 'pending'
    },
    {
      key: 'closed',
      title: ticket.status === 'CLOSED' ? '工单关闭' : '结果确认',
      time: rank >= 2 ? resolvedAt : '待完成',
      description: rank >= 2 ? '处理结果已记录，可回到日志中心追溯证据。' : '完成退款、换货或投诉处理后关闭闭环。',
      state: rank >= 2 ? 'done' : 'pending'
    }
  ]
})

async function loadTickets() {
  loading.value = true
  try {
    const data = await pageTickets(query)
    tickets.value = data?.rows || []
    total.value = data?.total || 0
    if (!selected.value && tickets.value.length) {
      selected.value = { ...tickets.value[0] }
    }
  } finally {
    loading.value = false
  }
}

function selectTicket(row) {
  selected.value = { ...row }
}

async function saveStatus() {
  await updateTicket(selected.value.id, selected.value)
  ElMessage.success('工单状态已更新')
  await loadTickets()
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

onMounted(loadTickets)
</script>

<style scoped>
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.detail-panel {
  margin-top: 14px;
}

.detail-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-subtitle {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 12px;
}

.ticket-detail {
  line-height: 1.7;
}

.ticket-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 12px;
  margin-bottom: 12px;
}

.ticket-issue,
.sla-card,
.next-action,
.ticket-flow {
  border: 1px solid var(--line-soft);
  border-radius: var(--radius);
  background: linear-gradient(180deg, #fff, #f8fafc);
}

.ticket-issue {
  padding: 16px;
}

.ticket-issue h4,
.next-action h4,
.ticket-flow h4 {
  margin: 4px 0 8px;
  font-size: 16px;
  line-height: 1.45;
}

.ticket-issue p,
.next-action p,
.flow-step p {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
}

.eyebrow {
  color: var(--brand);
  font-size: 12px;
  font-weight: 700;
}

.sla-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  padding: 16px;
}

.sla-card span {
  color: var(--text-muted);
  font-size: 12px;
}

.sla-card strong {
  font-size: 24px;
  line-height: 1;
}

.sla-card small {
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.5;
}

.sla-danger {
  border-color: rgb(220 38 38 / 20%);
  background: linear-gradient(180deg, #fff7f7, #fff);
}

.sla-warning {
  border-color: rgb(217 119 6 / 20%);
  background: linear-gradient(180deg, #fffaf0, #fff);
}

.sla-stable {
  border-color: rgb(5 150 105 / 20%);
  background: linear-gradient(180deg, #f4fffb, #fff);
}

.sla-closed {
  border-color: rgb(37 99 235 / 18%);
  background: linear-gradient(180deg, #f6f9ff, #fff);
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(260px, 0.6fr);
  gap: 12px;
  align-items: stretch;
}

.detail-descriptions {
  min-width: 0;
}

.next-action {
  padding: 14px;
}

.ticket-flow {
  margin-top: 12px;
  padding: 14px;
}

.flow-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.flow-steps {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.flow-step {
  position: relative;
  min-height: 132px;
  padding: 14px 12px 12px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius);
  background: #fff;
}

.flow-step strong,
.flow-step span {
  display: block;
}

.flow-step strong {
  margin-top: 8px;
  font-size: 14px;
}

.flow-step span {
  margin: 4px 0 8px;
  color: var(--text-muted);
  font-size: 12px;
}

.step-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: var(--line);
}

.flow-step.done .step-dot {
  background: var(--success);
  box-shadow: 0 0 0 5px rgb(5 150 105 / 10%);
}

.flow-step.active {
  border-color: rgb(37 99 235 / 24%);
  box-shadow: 0 12px 28px rgb(37 99 235 / 8%);
}

.flow-step.active .step-dot {
  background: var(--brand);
  box-shadow: 0 0 0 5px rgb(37 99 235 / 12%);
}

.flow-step.pending {
  background: var(--surface-soft);
}

@media (max-width: 1080px) {
  .ticket-hero,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .flow-steps {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .flow-steps {
    grid-template-columns: 1fr;
  }
}
</style>
