<template>
  <section class="page admin-after-sale-page">
    <div class="page-header">
      <div>
        <p class="page-subtitle">集中处理顾客提交的真实售后申请，审核动作会写入处理日志。</p>
      </div>
      <el-button :icon="Refresh" @click="reload">刷新</el-button>
    </div>

    <section class="metric-grid review-metrics">
      <div class="metric">
        <div class="metric-label">待审核</div>
        <div class="metric-value">{{ pendingCount }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">待补材料</div>
        <div class="metric-value">{{ moreEvidenceCount }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">高优先级</div>
        <div class="metric-value">{{ highPriorityCount }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">今日可处理</div>
        <div class="metric-value">{{ total }}</div>
      </div>
    </section>

    <section class="workspace-grid">
      <div class="panel">
        <div class="panel-header">
          <div class="toolbar">
            <el-input v-model="query.keyword" clearable placeholder="售后单号、订单号或商品名" style="width: 240px" @keyup.enter="loadApplications" />
            <el-select v-model="query.status" clearable placeholder="状态" style="width: 150px" @change="loadApplications">
              <el-option label="已提交" value="SUBMITTED" />
              <el-option label="审核中" value="UNDER_REVIEW" />
              <el-option label="待补材料" value="NEED_MORE_EVIDENCE" />
              <el-option label="待寄回" value="WAIT_BUYER_SEND" />
              <el-option label="已驳回" value="REJECTED" />
            </el-select>
            <el-select v-model="query.priority" clearable placeholder="优先级" style="width: 130px" @change="loadApplications">
              <el-option label="普通" value="NORMAL" />
              <el-option label="高" value="HIGH" />
              <el-option label="紧急" value="URGENT" />
            </el-select>
            <el-button :icon="Search" @click="loadApplications">查询</el-button>
          </div>
        </div>
        <div class="panel-body">
          <el-table v-loading="loading" :data="applications" height="500" highlight-current-row @row-click="selectApplication">
            <el-table-column prop="applicationNo" label="售后单号" min-width="158" />
            <el-table-column prop="orderNo" label="订单号" min-width="150" />
            <el-table-column prop="productName" label="商品" min-width="180" show-overflow-tooltip />
            <el-table-column label="类型" width="90">
              <template #default="{ row }"><StatusTag :value="row.serviceType" /></template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }"><StatusTag :value="row.status" /></template>
            </el-table-column>
            <el-table-column label="优先级" width="90">
              <template #default="{ row }"><StatusTag :value="row.priority" /></template>
            </el-table-column>
            <el-table-column prop="slaDeadline" label="SLA 截止" min-width="168" />
          </el-table>
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.pageSize"
            class="pager"
            layout="total, sizes, prev, pager, next"
            :total="total"
            @current-change="loadApplications"
            @size-change="loadApplications"
          />
        </div>
      </div>

      <section v-if="selected" class="panel review-panel">
        <div class="panel-header">
          <div>
            <h3 class="panel-title">审核处理台：{{ selected.applicationNo }}</h3>
            <p class="panel-note">{{ selected.orderNo }} · {{ selected.productName }}</p>
          </div>
          <StatusTag :value="selected.status" />
        </div>
        <div class="panel-body">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="顾客">{{ selected.userDisplayName || selected.userId }}</el-descriptions-item>
            <el-descriptions-item label="售后类型"><StatusTag :value="selected.serviceType" /></el-descriptions-item>
            <el-descriptions-item label="申请金额">{{ money(selected.refundAmount) }}</el-descriptions-item>
            <el-descriptions-item label="风险等级"><StatusTag :value="selected.riskLevel" /></el-descriptions-item>
            <el-descriptions-item label="原因" :span="2">{{ selected.reasonText }}</el-descriptions-item>
            <el-descriptions-item label="AI 摘要" :span="2">{{ selected.aiSummary || '暂无 AI 摘要，按人工审核流程处理。' }}</el-descriptions-item>
          </el-descriptions>

          <div class="decision-box">
            <div class="decision-header">
              <div>
                <h4>审核动作</h4>
                <p>通过后进入下一业务状态，驳回必须填写原因。</p>
              </div>
              <StatusTag :value="selected.priority" />
            </div>
            <el-form :model="decisionForm" label-width="86px">
              <el-form-item label="通过金额">
                <el-input-number v-model="decisionForm.approvedAmount" :min="0" :max="Number(selected.refundAmount || 0)" :precision="2" controls-position="right" />
              </el-form-item>
              <el-form-item label="处理备注">
                <el-input v-model="decisionForm.remark" type="textarea" :rows="3" maxlength="300" show-word-limit />
              </el-form-item>
              <div class="decision-actions">
                <el-button type="primary" :icon="Check" :disabled="!canReview" :loading="saving" @click="approveSelected">审核通过</el-button>
                <el-button type="warning" :disabled="!canReview" :loading="saving" @click="requestEvidenceSelected">要求补材料</el-button>
                <el-button type="danger" :icon="Close" :disabled="!canReview" :loading="saving" @click="rejectSelected">驳回申请</el-button>
              </div>
            </el-form>
          </div>

          <div class="evidence-list">
            <h4>凭证材料</h4>
            <div v-if="!selected.evidences?.length" class="empty-evidence">暂无凭证材料</div>
            <div v-for="evidence in selected.evidences || []" :key="evidence.id" class="evidence-item">
              <div>
                <StatusTag :value="evidence.evidenceType" />
                <strong>{{ evidence.uploadedByName || '顾客' }}</strong>
                <span>{{ evidence.createdAt }}</span>
              </div>
              <p>{{ evidence.content }}</p>
            </div>
          </div>

          <div class="timeline-box">
            <h4>处理记录</h4>
            <el-timeline>
              <el-timeline-item v-for="log in selected.processLogs || []" :key="log.id" :timestamp="log.createdAt">
                <div class="timeline-title">
                  <StatusTag :value="log.action" />
                  <span>{{ log.operatorName || log.operatorRole }}</span>
                </div>
                <p>{{ log.remark }}</p>
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
      </section>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { Check, Close, Refresh, Search } from '@element-plus/icons-vue'
import StatusTag from '../components/common/StatusTag.vue'
import {
  approveAfterSale,
  getAdminAfterSale,
  pageAdminAfterSales,
  rejectAfterSale,
  requestAfterSaleEvidence
} from '../api/adminAfterSaleApi'

const query = reactive({ page: 1, pageSize: 10, keyword: '', status: '', priority: '' })
const applications = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const selected = ref(null)
const decisionForm = reactive({ approvedAmount: 0, remark: '' })

const pendingCount = computed(() => applications.value.filter(item => ['SUBMITTED', 'UNDER_REVIEW'].includes(item.status)).length)
const moreEvidenceCount = computed(() => applications.value.filter(item => item.status === 'NEED_MORE_EVIDENCE').length)
const highPriorityCount = computed(() => applications.value.filter(item => ['HIGH', 'URGENT'].includes(item.priority)).length)
const canReview = computed(() => selected.value && ['SUBMITTED', 'UNDER_REVIEW', 'NEED_MORE_EVIDENCE'].includes(selected.value.status))

async function reload() {
  await loadApplications()
  if (selected.value) {
    selected.value = await getAdminAfterSale(selected.value.id)
    hydrateDecisionForm()
  }
}

async function loadApplications() {
  loading.value = true
  try {
    const data = await pageAdminAfterSales(query)
    applications.value = data?.rows || []
    total.value = data?.total || 0
    if (!selected.value && applications.value.length) {
      await selectApplication(applications.value[0])
    }
  } finally {
    loading.value = false
  }
}

async function selectApplication(row) {
  selected.value = await getAdminAfterSale(row.id)
  hydrateDecisionForm()
}

async function approveSelected() {
  saving.value = true
  try {
    selected.value = await approveAfterSale(selected.value.id, { ...decisionForm })
    ElMessage.success('售后申请已审核通过')
    hydrateDecisionForm()
    await loadApplications()
  } finally {
    saving.value = false
  }
}

async function rejectSelected() {
  if (!decisionForm.remark) {
    ElMessage.warning('驳回申请必须填写原因')
    return
  }
  saving.value = true
  try {
    selected.value = await rejectAfterSale(selected.value.id, { ...decisionForm })
    ElMessage.success('售后申请已驳回')
    hydrateDecisionForm()
    await loadApplications()
  } finally {
    saving.value = false
  }
}

async function requestEvidenceSelected() {
  if (!decisionForm.remark) {
    ElMessage.warning('要求补充材料必须填写说明')
    return
  }
  saving.value = true
  try {
    selected.value = await requestAfterSaleEvidence(selected.value.id, { ...decisionForm })
    ElMessage.success('已要求顾客补充材料')
    hydrateDecisionForm()
    await loadApplications()
  } finally {
    saving.value = false
  }
}

function hydrateDecisionForm() {
  decisionForm.approvedAmount = Number(selected.value?.refundAmount || 0)
  decisionForm.remark = selected.value?.status === 'REJECTED' ? '' : '资料完整，符合售后规则，审核通过。'
}

function money(value) {
  if (value === null || typeof value === 'undefined') {
    return '-'
  }
  return `￥${Number(value).toFixed(2)}`
}

onMounted(loadApplications)
</script>

<style scoped>
.review-metrics {
  margin-bottom: 14px;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(360px, 0.88fr);
  gap: 14px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.panel-note {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 12px;
}

.decision-box,
.evidence-list,
.timeline-box {
  margin-top: 14px;
  padding: 14px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius);
  background: #fff;
}

.decision-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.decision-header h4,
.timeline-box h4 {
  margin: 0 0 4px;
  font-size: 15px;
}

.decision-header p {
  margin: 0;
  color: var(--text-muted);
  font-size: 12px;
}

.decision-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.timeline-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  font-weight: 700;
}

.timeline-title + p {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.6;
}

.empty-evidence {
  color: var(--text-muted);
  font-size: 13px;
}

.evidence-item {
  padding: 12px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: var(--surface-soft);
}

.evidence-item + .evidence-item {
  margin-top: 10px;
}

.evidence-item div {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.evidence-item span {
  color: var(--text-muted);
  font-size: 12px;
}

.evidence-item p {
  margin: 0;
  color: var(--text);
  line-height: 1.6;
}

@media (max-width: 1180px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }
}
</style>
