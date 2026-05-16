<template>
  <section class="page admin-after-sale-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">售后审核工作台</h2>
        <p class="page-subtitle">集中处理顾客提交的真实售后申请，审核动作会写入处理日志。</p>
      </div>
      <el-button :icon="Refresh" @click="reload">刷新</el-button>
    </div>

    <section class="metric-grid review-metrics">
      <div class="metric">
        <div class="metric-label">待审核</div>
        <div class="metric-value">{{ pendingCount }}</div>
        <p class="metric-note">提交后等待客服确认</p>
      </div>
      <div class="metric">
        <div class="metric-label">待补材料</div>
        <div class="metric-value">{{ moreEvidenceCount }}</div>
        <p class="metric-note">需要顾客继续上传凭证</p>
      </div>
      <div class="metric">
        <div class="metric-label">高优先级</div>
        <div class="metric-value">{{ highPriorityCount }}</div>
        <p class="metric-note">优先处理风险和投诉</p>
      </div>
      <div class="metric">
        <div class="metric-label">队列总量</div>
        <div class="metric-value">{{ total }}</div>
        <p class="metric-note">当前筛选条件下的售后单</p>
      </div>
    </section>

    <section class="workspace-grid">
      <div class="panel">
        <div class="panel-header">
          <div>
            <h3 class="panel-title">待处理队列</h3>
            <p class="panel-note">按状态、优先级和 SLA 找到需要跟进的售后申请。</p>
          </div>
          <div class="toolbar queue-toolbar">
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
            <h3 class="panel-title">售后处理台：{{ selected.applicationNo }}</h3>
            <p class="panel-note">{{ selected.orderNo }} · {{ selected.productName }}</p>
          </div>
          <StatusTag :value="selected.status" />
        </div>
        <div class="panel-body">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="顾客">
              <el-button link type="primary" @click="openCustomerProfile">{{ selected.userDisplayName || selected.userId }}</el-button>
            </el-descriptions-item>
            <el-descriptions-item label="售后类型"><StatusTag :value="selected.serviceType" /></el-descriptions-item>
            <el-descriptions-item label="申请金额">{{ money(selected.refundAmount) }}</el-descriptions-item>
            <el-descriptions-item label="风险等级"><StatusTag :value="selected.riskLevel" /></el-descriptions-item>
            <el-descriptions-item label="原因" :span="2">{{ selected.reasonText }}</el-descriptions-item>
            <el-descriptions-item label="AI 摘要" :span="2">{{ selected.aiSummary || '暂无 AI 摘要，按人工审核流程处理。' }}</el-descriptions-item>
          </el-descriptions>

          <AfterSaleDiagnosisPanel v-if="selected.diagnosis" :diagnosis="selected.diagnosis" />

          <div class="audit-box">
            <div class="audit-box-head">
              <div>
                <h4>凭证真实性审核</h4>
                <p>只展示风险信号和补证建议，不替代人工审核结论。</p>
              </div>
              <el-button type="primary" plain :loading="auditingAll" @click="auditAllEvidence">审核全部凭证</el-button>
            </div>
            <div v-if="!selected.evidences?.length" class="empty-evidence">暂无凭证材料</div>
            <div v-for="evidence in selected.evidences || []" :key="`audit-${evidence.id}`" class="audit-item">
              <div class="audit-item-meta">
                <StatusTag :value="evidence.evidenceType" />
                <strong>{{ evidence.content }}</strong>
                <el-button size="small" type="primary" :loading="auditingEvidenceId === evidence.id" @click="auditEvidence(evidence)">重新审核</el-button>
              </div>
              <div v-if="evidence.fileUrl" class="image-preview-box">
                <strong>图片凭证预览</strong>
                <img class="evidence-preview" :src="evidenceImageUrl(evidence.fileUrl)" alt="图片凭证预览" />
              </div>
              <EvidenceAuditPanel :audit="evidence.latestAudit" compact />
            </div>
          </div>

          <div class="risk-box">
            <div class="risk-box-head">
              <div>
                <h4>售后风险识别</h4>
                <p>融合用户历史、凭证审核、金额和 SLA，只作为人工审核辅助。</p>
              </div>
              <el-button type="primary" plain :loading="assessingRisk" @click="assessRisk">重新评估</el-button>
            </div>
            <AfterSaleRiskPanel :assessment="selected.riskAssessment" compact />
          </div>

          <div class="ticket-box">
            <div class="ticket-main">
              <div>
                <h4>关联客服工单</h4>
                <p v-if="selected.ticketNo">
                  <strong>{{ selected.ticketNo }}</strong>
                  <StatusTag :value="selected.ticketStatus" />
                </p>
                <p v-else>复杂投诉或异常申请可以转成人工工单，后续工单状态会回写到售后处理记录。</p>
              </div>
              <el-button type="primary" :icon="Ticket" :loading="creatingTicket" :disabled="Boolean(selected.ticketId)" @click="createLinkedTicket">
                {{ selected.ticketId ? '已关联工单' : '创建关联工单' }}
              </el-button>
            </div>
          </div>

          <div class="ai-copilot-box">
            <div class="copilot-header">
              <div>
                <h4>AI 副驾驶回复草稿</h4>
                <p>只生成建议，不自动改状态；管理员可以采纳或废弃并留下审计记录。</p>
              </div>
              <el-button type="primary" :icon="Promotion" :loading="generatingDraft" @click="generateDraft">生成回复草稿</el-button>
            </div>
            <div v-if="!replyDrafts.length" class="empty-evidence">暂无回复草稿</div>
            <div v-for="draft in replyDrafts" :key="draft.id" class="draft-item">
              <div class="draft-meta">
                <StatusTag :value="draft.sourceType" />
                <StatusTag :value="draft.status" />
                <StatusTag :value="draft.riskLevel" />
                <span>{{ draft.createdAt }}</span>
              </div>
              <p>{{ draft.draftContent }}</p>
              <div class="draft-foot">
                <small>{{ draft.knowledgeRefs || '本地售后模板' }} · {{ draft.aiModelName || draft.aiProvider || 'local-rule-template' }}</small>
                <div v-if="draft.status === 'DRAFT'" class="draft-actions">
                  <el-button size="small" type="success" :loading="savingDraft" @click="useDraft(draft)">采纳草稿</el-button>
                  <el-button size="small" type="danger" :loading="savingDraft" @click="discardDraft(draft)">废弃草稿</el-button>
                </div>
              </div>
            </div>
          </div>

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
                <el-button type="success" :icon="Check" :disabled="!canComplete" :loading="saving" @click="completeSelected">确认完成</el-button>
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
              <div v-if="evidence.fileUrl" class="image-preview-box">
                <strong>图片凭证预览</strong>
                <img class="evidence-preview" :src="evidenceImageUrl(evidence.fileUrl)" alt="图片凭证预览" />
              </div>
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
      <section v-else class="panel review-panel empty-review-panel">
        <EmptyState text="选择一条售后申请后开始处理" />
      </section>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { Check, Close, Promotion, Refresh, Search, Ticket } from '@element-plus/icons-vue'
import AfterSaleDiagnosisPanel from '../components/after-sale/AfterSaleDiagnosisPanel.vue'
import AfterSaleRiskPanel from '../components/after-sale/AfterSaleRiskPanel.vue'
import EvidenceAuditPanel from '../components/after-sale/EvidenceAuditPanel.vue'
import EmptyState from '../components/common/EmptyState.vue'
import StatusTag from '../components/common/StatusTag.vue'
import {
  assessAfterSaleRisk,
  approveAfterSale,
  completeAfterSale,
  createAfterSaleTicket,
  createEvidenceAudit,
  discardReplyDraft,
  generateReplyDraft,
  getAdminAfterSale,
  listReplyDrafts,
  pageAdminAfterSales,
  rejectAfterSale,
  requestAfterSaleEvidence,
  useReplyDraft
} from '../api/adminAfterSaleApi'

const query = reactive({ page: 1, pageSize: 10, keyword: '', status: '', priority: '' })
const route = useRoute()
const router = useRouter()
const applications = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const creatingTicket = ref(false)
const generatingDraft = ref(false)
const savingDraft = ref(false)
const auditingAll = ref(false)
const auditingEvidenceId = ref(null)
const assessingRisk = ref(false)
const selected = ref(null)
const replyDrafts = ref([])
const decisionForm = reactive({ approvedAmount: 0, remark: '' })

const pendingCount = computed(() => applications.value.filter(item => ['SUBMITTED', 'UNDER_REVIEW'].includes(item.status)).length)
const moreEvidenceCount = computed(() => applications.value.filter(item => item.status === 'NEED_MORE_EVIDENCE').length)
const highPriorityCount = computed(() => applications.value.filter(item => ['HIGH', 'URGENT'].includes(item.priority)).length)
const canReview = computed(() => selected.value && ['SUBMITTED', 'UNDER_REVIEW', 'NEED_MORE_EVIDENCE'].includes(selected.value.status))
const canComplete = computed(() => selected.value && !['SUBMITTED', 'UNDER_REVIEW', 'NEED_MORE_EVIDENCE', 'REJECTED', 'COMPLETED', 'CANCELLED'].includes(selected.value.status))

async function reload() {
  await loadApplications()
  if (selected.value) {
    selected.value = await getAdminAfterSale(selected.value.id)
    await loadReplyDrafts()
    hydrateDecisionForm()
  }
}

async function loadApplications() {
  loading.value = true
  try {
    const data = await pageAdminAfterSales(query)
    applications.value = data?.rows || []
    total.value = data?.total || 0
    const selectedStillVisible = selected.value && applications.value.some(item => item.id === selected.value.id)
    if (applications.value.length && !selectedStillVisible) {
      await selectApplication(applications.value[0])
    } else if (!applications.value.length) {
      selected.value = null
      replyDrafts.value = []
    }
  } finally {
    loading.value = false
  }
}

async function selectApplication(row) {
  selected.value = await getAdminAfterSale(row.id)
  await loadReplyDrafts()
  hydrateDecisionForm()
}

function openCustomerProfile() {
  if (!selected.value?.userId) {
    return
  }
  router.push({ path: '/admin/customers/profile', query: { userId: selected.value.userId } })
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

async function completeSelected() {
  saving.value = true
  try {
    selected.value = await completeAfterSale(selected.value.id, {
      remark: decisionForm.remark || '售后处理已完成，顾客可进行服务评价。'
    })
    ElMessage.success('售后处理已确认完成')
    hydrateDecisionForm()
    await loadApplications()
  } finally {
    saving.value = false
  }
}

async function createLinkedTicket() {
  if (!selected.value) {
    return
  }
  creatingTicket.value = true
  try {
    const ticket = await createAfterSaleTicket(selected.value.id, {
      remark: decisionForm.remark || '请客服结合售后申请、凭证和处理记录继续跟进。'
    })
    ElMessage.success(`已创建关联工单：${ticket.ticketNo}`)
    selected.value = await getAdminAfterSale(selected.value.id)
    await loadReplyDrafts()
    await loadApplications()
  } finally {
    creatingTicket.value = false
  }
}

async function auditEvidence(evidence) {
  if (!selected.value?.id || !evidence?.id) {
    return
  }
  auditingEvidenceId.value = evidence.id
  try {
    await createEvidenceAudit(evidence.id, { useAi: true })
    ElMessage.success('凭证审核已生成')
    selected.value = await getAdminAfterSale(selected.value.id)
  } finally {
    auditingEvidenceId.value = null
  }
}

async function auditAllEvidence() {
  if (!selected.value?.evidences?.length) {
    ElMessage.warning('暂无凭证可审核')
    return
  }
  auditingAll.value = true
  try {
    for (const evidence of selected.value.evidences) {
      await createEvidenceAudit(evidence.id, { useAi: true })
    }
    ElMessage.success('全部凭证已完成审核')
    selected.value = await getAdminAfterSale(selected.value.id)
  } finally {
    auditingAll.value = false
  }
}

async function assessRisk() {
  if (!selected.value?.id) {
    return
  }
  assessingRisk.value = true
  try {
    await assessAfterSaleRisk(selected.value.id, { useAi: true })
    ElMessage.success('售后风险评估已更新')
    selected.value = await getAdminAfterSale(selected.value.id)
    await loadApplications()
  } finally {
    assessingRisk.value = false
  }
}

async function loadReplyDrafts() {
  if (!selected.value?.id) {
    replyDrafts.value = []
    return
  }
  replyDrafts.value = await listReplyDrafts(selected.value.id)
}

async function generateDraft() {
  if (!selected.value) {
    return
  }
  generatingDraft.value = true
  try {
    await generateReplyDraft(selected.value.id, {
      remark: decisionForm.remark || '管理员请求 AI 副驾驶生成回复草稿。'
    })
    ElMessage.success('回复草稿已生成，等待管理员确认')
    selected.value = await getAdminAfterSale(selected.value.id)
    await loadReplyDrafts()
  } finally {
    generatingDraft.value = false
  }
}

async function useDraft(draft) {
  savingDraft.value = true
  try {
    await useReplyDraft(selected.value.id, draft.id, { remark: '管理员采纳该回复草稿。' })
    ElMessage.success('回复草稿已采纳')
    selected.value = await getAdminAfterSale(selected.value.id)
    await loadReplyDrafts()
  } finally {
    savingDraft.value = false
  }
}

async function discardDraft(draft) {
  savingDraft.value = true
  try {
    await discardReplyDraft(selected.value.id, draft.id, { remark: '管理员废弃该回复草稿。' })
    ElMessage.success('回复草稿已废弃')
    selected.value = await getAdminAfterSale(selected.value.id)
    await loadReplyDrafts()
  } finally {
    savingDraft.value = false
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

function evidenceImageUrl(fileUrl) {
  if (!fileUrl) {
    return ''
  }
  if (/^https?:\/\//i.test(fileUrl)) {
    return fileUrl
  }
  const apiBase = import.meta.env.VITE_API_BASE_URL || '/api'
  if (fileUrl.startsWith('/uploads/')) {
    return `${apiBase}${fileUrl}`
  }
  return fileUrl
}

onMounted(() => {
  query.keyword = route.query.keyword || ''
  loadApplications()
})
</script>

<style scoped>
.review-metrics {
  margin-bottom: 14px;
}

.review-metrics .metric {
  position: relative;
  overflow: hidden;
}

.review-metrics .metric::after {
  position: absolute;
  right: 14px;
  bottom: 12px;
  width: 38px;
  height: 4px;
  border-radius: 999px;
  background: var(--brand-soft);
  content: "";
}

.metric-note {
  margin: 8px 0 0;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.45;
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

.queue-toolbar {
  justify-content: flex-end;
}

.review-panel {
  align-self: start;
  max-height: calc(100vh - var(--header-height) - 116px);
  overflow: hidden;
}

.review-panel > .panel-body {
  max-height: calc(100vh - var(--header-height) - 178px);
  overflow: auto;
}

.empty-review-panel {
  display: grid;
  min-height: 420px;
  place-items: center;
}

.decision-box,
.ticket-box,
.ai-copilot-box,
.audit-box,
.risk-box,
.evidence-list,
.timeline-box {
  margin-top: 14px;
  padding: 14px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius);
  background: #fff;
}

.decision-box {
  border-color: rgb(37 99 235 / 22%);
  background: #f8fbff;
}

.ticket-box {
  border-color: rgb(217 119 6 / 20%);
  background: #fffbeb;
}

.ai-copilot-box {
  border-color: rgb(16 185 129 / 20%);
  background: #f7fefb;
}

.audit-box,
.risk-box {
  background: #fff;
}

.audit-box-head,
.risk-box-head,
.audit-item-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.audit-box-head h4 {
  margin: 0 0 6px;
  font-size: 15px;
}

.audit-box-head p,
.risk-box-head p {
  margin: 0;
  color: var(--text-muted);
  font-size: 12px;
}

.risk-box-head h4 {
  margin: 0 0 6px;
  font-size: 15px;
}

.audit-item {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: var(--surface-soft);
}

.audit-item-meta {
  margin-bottom: 10px;
}

.audit-item-meta strong {
  flex: 1;
  min-width: 0;
  color: var(--text);
  font-size: 13px;
  line-height: 1.5;
}

.copilot-header,
.ticket-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.copilot-header h4,
.ticket-main h4 {
  margin: 0 0 6px;
  font-size: 15px;
}

.copilot-header p,
.ticket-main p {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.6;
}

.ticket-main p {
  display: flex;
  align-items: center;
  gap: 8px;
}

.draft-item {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: var(--surface-soft);
}

.draft-meta,
.draft-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.draft-meta {
  justify-content: flex-start;
  margin-bottom: 8px;
}

.draft-meta span,
.draft-foot small {
  color: var(--text-muted);
  font-size: 12px;
}

.draft-item p {
  margin: 0 0 10px;
  color: var(--text);
  line-height: 1.7;
}

.draft-actions {
  display: flex;
  gap: 8px;
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

.evidence-preview {
  display: block;
  width: min(320px, 100%);
  max-height: 220px;
  margin: 10px 0;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  object-fit: contain;
  background: #fff;
}

.image-preview-box {
  margin: 10px 0;
}

.image-preview-box strong {
  display: block;
  margin-bottom: 6px;
  color: var(--text);
  font-size: 13px;
}

@media (max-width: 1180px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .review-panel,
  .review-panel > .panel-body {
    max-height: none;
  }
}
</style>
