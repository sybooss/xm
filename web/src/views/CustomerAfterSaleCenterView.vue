<template>
  <section class="page customer-after-sale-page">
    <div class="page-header">
      <div>
        <p class="page-subtitle">围绕订单发起售后申请，查看审核进度和处理时间线。</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" :icon="Plus" :disabled="!selectedOrder" @click="openApplyDialog(selectedOrder)">申请售后</el-button>
        <el-button :icon="Refresh" @click="reloadAll">刷新</el-button>
      </div>
    </div>

    <section class="metric-grid customer-metrics">
      <div class="metric">
        <div class="metric-label">我的订单</div>
        <div class="metric-value">{{ orderTotal }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">进行中售后</div>
        <div class="metric-value">{{ activeAfterSaleCount }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">待我处理</div>
        <div class="metric-value">{{ needCustomerActionCount }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">已完成</div>
        <div class="metric-value">{{ completedAfterSaleCount }}</div>
      </div>
    </section>

    <section class="workspace-grid">
      <div class="panel">
        <div class="panel-header">
          <div>
            <h3 class="panel-title">我的订单</h3>
            <p class="panel-note">选择订单后可以发起退货、换货、退款或投诉。</p>
          </div>
          <div class="toolbar">
            <el-input v-model="orderQuery.keyword" clearable placeholder="订单号或商品名" style="width: 190px" @keyup.enter="loadOrders" />
            <el-button :icon="Search" @click="loadOrders">查询</el-button>
          </div>
        </div>
        <div class="panel-body">
          <el-table v-loading="orderLoading" :data="orders" height="360" highlight-current-row @row-click="selectOrder">
            <el-table-column prop="orderNo" label="订单号" min-width="150" />
            <el-table-column prop="productName" label="商品" min-width="180" show-overflow-tooltip />
            <el-table-column prop="orderAmount" label="金额" width="90" />
            <el-table-column label="订单状态" width="110">
              <template #default="{ row }"><StatusTag :value="row.orderStatus" /></template>
            </el-table-column>
            <el-table-column label="售后" width="120">
              <template #default="{ row }"><StatusTag :value="row.afterSaleStatus" /></template>
            </el-table-column>
            <el-table-column label="操作" width="112" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click.stop="openApplyDialog(row)">申请</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="orderQuery.page"
            v-model:page-size="orderQuery.pageSize"
            class="pager"
            layout="total, prev, pager, next"
            :total="orderTotal"
            @current-change="loadOrders"
          />
        </div>
      </div>

      <div class="panel">
        <div class="panel-header">
          <div>
            <h3 class="panel-title">我的售后</h3>
            <p class="panel-note">查看每个售后申请当前走到哪一步。</p>
          </div>
          <div class="toolbar">
            <el-select v-model="afterSaleQuery.status" clearable placeholder="状态" style="width: 150px" @change="loadAfterSales">
              <el-option label="已提交" value="SUBMITTED" />
              <el-option label="待补材料" value="NEED_MORE_EVIDENCE" />
              <el-option label="待寄回" value="WAIT_BUYER_SEND" />
              <el-option label="退款中" value="REFUNDING" />
              <el-option label="已驳回" value="REJECTED" />
              <el-option label="已完成" value="COMPLETED" />
            </el-select>
          </div>
        </div>
        <div class="panel-body">
          <el-table v-loading="afterSaleLoading" :data="afterSales" height="360" highlight-current-row @row-click="selectAfterSale">
            <el-table-column prop="applicationNo" label="售后单号" min-width="158" />
            <el-table-column prop="productName" label="商品" min-width="150" show-overflow-tooltip />
            <el-table-column label="类型" width="90">
              <template #default="{ row }"><StatusTag :value="row.serviceType" /></template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }"><StatusTag :value="row.status" /></template>
            </el-table-column>
            <el-table-column label="优先级" width="90">
              <template #default="{ row }"><StatusTag :value="row.priority" /></template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="afterSaleQuery.page"
            v-model:page-size="afterSaleQuery.pageSize"
            class="pager"
            layout="total, prev, pager, next"
            :total="afterSaleTotal"
            @current-change="loadAfterSales"
          />
        </div>
      </div>
    </section>

    <section v-if="selectedAfterSale" class="panel detail-panel">
      <div class="panel-header">
        <div>
          <h3 class="panel-title">售后详情：{{ selectedAfterSale.applicationNo }}</h3>
          <p class="panel-note">{{ selectedAfterSale.orderNo }} · {{ selectedAfterSale.productName }}</p>
        </div>
        <div class="detail-actions">
          <StatusTag :value="selectedAfterSale.status" />
          <el-button type="primary" @click="openEvidenceDialog">补充凭证</el-button>
          <el-button v-if="canReviewSelected" type="success" @click="openReviewDialog">评价服务</el-button>
          <el-button @click="goChat">在线咨询</el-button>
        </div>
      </div>
      <div class="panel-body detail-grid">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="售后类型"><StatusTag :value="selectedAfterSale.serviceType" /></el-descriptions-item>
          <el-descriptions-item label="优先级"><StatusTag :value="selectedAfterSale.priority" /></el-descriptions-item>
          <el-descriptions-item label="申请金额">{{ money(selectedAfterSale.refundAmount) }}</el-descriptions-item>
          <el-descriptions-item label="审核金额">{{ money(selectedAfterSale.approvedAmount) }}</el-descriptions-item>
          <el-descriptions-item label="下一步">{{ nextActionLabel(selectedAfterSale.status) }}</el-descriptions-item>
          <el-descriptions-item label="SLA 截止">{{ selectedAfterSale.slaDeadline || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请原因" :span="2">{{ selectedAfterSale.reasonText }}</el-descriptions-item>
        </el-descriptions>

        <div class="result-box">
          <div class="result-header">
            <h4>处理结果说明</h4>
            <StatusTag :value="selectedAfterSale.status" />
          </div>
          <p>{{ selectedAfterSale.customerResultSummary || resultSummary(selectedAfterSale) }}</p>
          <div v-if="selectedAfterSale.customerFinalReply" class="final-reply">
            <strong>客服最终回复</strong>
            <span>{{ selectedAfterSale.customerFinalReply }}</span>
          </div>
          <div class="next-action">
            <strong>下一步</strong>
            <span>{{ selectedAfterSale.customerNextAction || nextActionLabel(selectedAfterSale.status) }}</span>
          </div>
        </div>

        <div v-if="selectedAfterSale.diagnosis" class="diagnosis-detail-box">
          <AfterSaleDiagnosisPanel :diagnosis="selectedAfterSale.diagnosis" />
        </div>

        <div class="timeline-box">
          <h4>处理时间线</h4>
          <el-timeline>
            <el-timeline-item
              v-for="log in selectedAfterSale.processLogs || []"
              :key="log.id"
              :timestamp="log.createdAt"
            >
              <div class="timeline-title">
                <StatusTag :value="log.action" />
                <span>{{ log.operatorName || log.operatorRole }}</span>
              </div>
              <p>{{ log.remark }}</p>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
      <div class="evidence-list">
        <h4>凭证材料</h4>
        <div v-if="!selectedAfterSale.evidences?.length" class="empty-evidence">暂无凭证材料</div>
        <div v-for="evidence in selectedAfterSale.evidences || []" :key="evidence.id" class="evidence-item">
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
          <div class="evidence-actions">
            <el-button size="small" type="primary" plain :loading="auditingEvidenceId === evidence.id" @click="auditEvidence(evidence)">审核凭证</el-button>
          </div>
          <EvidenceAuditPanel
            v-if="evidence.latestAudit"
            class="evidence-audit"
            :audit="evidence.latestAudit"
            compact
            :show-signals="false"
          />
        </div>
      </div>
      <div v-if="selectedReview" class="review-box">
        <h4>我的评价</h4>
        <div class="review-summary">
          <el-rate :model-value="selectedReview.rating" disabled />
          <span>{{ selectedReview.tags || '未填写标签' }}</span>
        </div>
        <p>{{ selectedReview.comment || '暂无文字评价' }}</p>
      </div>
    </section>

    <el-dialog v-model="applyDialogVisible" title="申请售后" width="620px" destroy-on-close>
      <el-form :model="applyForm" label-width="94px">
        <el-form-item label="订单">
          <span>{{ applyingOrder?.orderNo }} · {{ applyingOrder?.productName }}</span>
        </el-form-item>
        <el-form-item label="售后类型">
          <el-radio-group v-model="applyForm.serviceType">
            <el-radio-button label="RETURN">退货退款</el-radio-button>
            <el-radio-button label="EXCHANGE">换货</el-radio-button>
            <el-radio-button label="REFUND">仅退款</el-radio-button>
            <el-radio-button label="COMPLAINT">投诉</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="原因类型">
          <el-select v-model="applyForm.reasonCode" placeholder="请选择原因">
            <el-option label="商品质量问题" value="QUALITY_PROBLEM" />
            <el-option label="错发漏发" value="WRONG_ITEM" />
            <el-option label="物流异常" value="LOGISTICS_ABNORMAL" />
            <el-option label="商家长时间不处理" value="MERCHANT_DELAY" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请金额">
          <el-input-number v-model="applyForm.refundAmount" :min="0" :max="Number(applyingOrder?.orderAmount || 0)" :precision="2" controls-position="right" />
        </el-form-item>
        <el-form-item label="问题说明">
          <el-input v-model="applyForm.reasonText" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="请说明问题、期望处理方式和已有凭证。" />
        </el-form-item>
        <el-form-item label="智能诊断">
          <div class="diagnosis-action">
            <el-button type="primary" plain :loading="diagnosing" @click="runDiagnosis">智能诊断</el-button>
            <span>提交前先生成处理路径、凭证要求和多方案建议。</span>
          </div>
        </el-form-item>
        <el-form-item v-if="currentDiagnosis" label="诊断结果">
          <div class="diagnosis-form-block">
            <AfterSaleDiagnosisPanel :diagnosis="currentDiagnosis" compact />
            <el-button
              v-if="currentDiagnosis.suggestedServiceType && currentDiagnosis.suggestedServiceType !== applyForm.serviceType && currentDiagnosis.suggestedServiceType !== 'REPAIR'"
              class="adopt-diagnosis"
              size="small"
              type="success"
              @click="adoptDiagnosisType"
            >
              采用推荐类型
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitApplication">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="evidenceDialogVisible" title="补充凭证" width="560px" destroy-on-close>
      <el-form :model="evidenceForm" label-width="92px">
        <el-form-item label="凭证类型">
          <el-select v-model="evidenceForm.evidenceType">
            <el-option label="文字说明" value="TEXT" />
            <el-option label="物流单号" value="LOGISTICS_NO" />
            <el-option label="图片凭证" value="IMAGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="上传图片">
          <div class="upload-area">
            <input ref="evidenceFileInput" type="file" accept="image/*" @change="handleEvidenceFileChange" />
            <el-button size="small" :loading="uploadingEvidence" @click="triggerEvidenceFileInput">选择图片</el-button>
            <span>{{ evidenceUploadName || '支持 jpg、png、webp、gif，最大 5MB' }}</span>
          </div>
          <div v-if="evidenceForm.fileUrl" class="image-preview-box">
            <strong>图片凭证预览</strong>
            <img class="upload-preview" :src="evidenceImageUrl(evidenceForm.fileUrl)" alt="图片凭证预览" />
          </div>
        </el-form-item>
        <el-form-item v-if="evidenceForm.evidenceType === 'IMAGE'" label="图片说明">
          <el-input v-model="evidenceForm.fileUrl" placeholder="图片上传成功后自动填入，也可以粘贴图片 URL" />
        </el-form-item>
        <el-form-item label="凭证内容">
          <el-input v-model="evidenceForm.content" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="说明图片中坏损/故障位置，或注明是否为实拍原图、是否含水印/EXIF/编辑痕迹。" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="evidenceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="evidenceSubmitting" @click="submitEvidence">提交凭证</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewDialogVisible" title="评价服务" width="560px" destroy-on-close>
      <el-form :model="reviewForm" label-width="92px">
        <el-form-item label="服务评分">
          <el-rate v-model="reviewForm.rating" />
        </el-form-item>
        <el-form-item label="评价标签">
          <el-input v-model="reviewForm.tags" maxlength="100" placeholder="例如：响应快、处理清楚、还需跟进" />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input v-model="reviewForm.comment" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="写下这次售后处理体验。" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import AfterSaleDiagnosisPanel from '../components/after-sale/AfterSaleDiagnosisPanel.vue'
import EvidenceAuditPanel from '../components/after-sale/EvidenceAuditPanel.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { pageOrders } from '../api/orderApi'
import {
  addCustomerAfterSaleEvidence,
  createEvidenceAudit,
  createAfterSaleDiagnosis,
  createCustomerAfterSale,
  createCustomerAfterSaleReview,
  getCustomerAfterSale,
  getCustomerAfterSaleReview,
  pageCustomerAfterSales,
  uploadEvidenceFile
} from '../api/customerAfterSaleApi'

const router = useRouter()
const route = useRoute()
const orderQuery = reactive({ page: 1, pageSize: 8, keyword: '' })
const afterSaleQuery = reactive({ page: 1, pageSize: 8, status: '' })
const orders = ref([])
const orderTotal = ref(0)
const afterSales = ref([])
const afterSaleTotal = ref(0)
const selectedOrder = ref(null)
const selectedAfterSale = ref(null)
const applyingOrder = ref(null)
const orderLoading = ref(false)
const afterSaleLoading = ref(false)
const submitting = ref(false)
const evidenceSubmitting = ref(false)
const uploadingEvidence = ref(false)
const reviewSubmitting = ref(false)
const diagnosing = ref(false)
const auditingEvidenceId = ref(null)
const applyDialogVisible = ref(false)
const evidenceDialogVisible = ref(false)
const reviewDialogVisible = ref(false)
const applyForm = reactive(defaultApplyForm())
const evidenceForm = reactive(defaultEvidenceForm())
const reviewForm = reactive(defaultReviewForm())
const selectedReview = ref(null)
const currentDiagnosis = ref(null)
const evidenceFileInput = ref(null)
const evidenceUploadName = ref('')
const maxEvidenceImageSize = 5 * 1024 * 1024

const activeAfterSaleCount = computed(() => afterSales.value.filter(item => !['REJECTED', 'COMPLETED', 'CANCELLED'].includes(item.status)).length)
const needCustomerActionCount = computed(() => afterSales.value.filter(item => ['NEED_MORE_EVIDENCE', 'WAIT_BUYER_SEND'].includes(item.status)).length)
const completedAfterSaleCount = computed(() => afterSales.value.filter(item => item.status === 'COMPLETED').length)
const canReviewSelected = computed(() => selectedAfterSale.value?.status === 'COMPLETED' && !selectedReview.value)

async function reloadAll() {
  await Promise.all([loadOrders(), loadAfterSales()])
}

async function loadOrders() {
  orderLoading.value = true
  try {
    const data = await pageOrders(orderQuery)
    orders.value = data?.rows || []
    orderTotal.value = data?.total || 0
    if (!selectedOrder.value && orders.value.length) {
      selectedOrder.value = orders.value[0]
    }
  } finally {
    orderLoading.value = false
  }
}

async function loadAfterSales() {
  afterSaleLoading.value = true
  try {
    const data = await pageCustomerAfterSales(afterSaleQuery)
    afterSales.value = data?.rows || []
    afterSaleTotal.value = data?.total || 0
    const focusId = Number(route.query.focus || 0)
    if (focusId) {
      await selectAfterSale({ id: focusId })
      return
    }
    if (!selectedAfterSale.value && afterSales.value.length) {
      await selectAfterSale(afterSales.value[0])
    }
  } finally {
    afterSaleLoading.value = false
  }
}

function selectOrder(row) {
  selectedOrder.value = row
}

async function selectAfterSale(row) {
  selectedAfterSale.value = await getCustomerAfterSale(row.id)
  selectedReview.value = await getCustomerAfterSaleReview(row.id)
}

function openApplyDialog(order) {
  applyingOrder.value = order
  currentDiagnosis.value = null
  Object.assign(applyForm, defaultApplyForm(), {
    orderId: order.id,
    refundAmount: Number(order.orderAmount || 0)
  })
  applyDialogVisible.value = true
}

async function runDiagnosis() {
  if (!applyingOrder.value) return
  if (!applyForm.reasonText) {
    ElMessage.warning('请先填写问题说明')
    return
  }
  diagnosing.value = true
  try {
    currentDiagnosis.value = await createAfterSaleDiagnosis({
      orderId: applyingOrder.value.id,
      issueText: applyForm.reasonText,
      serviceType: applyForm.serviceType,
      refundAmount: applyForm.refundAmount,
      useAi: true
    })
    if (currentDiagnosis.value?.suggestedServiceType && currentDiagnosis.value.suggestedServiceType !== 'REPAIR') {
      applyForm.serviceType = currentDiagnosis.value.suggestedServiceType
    }
    ElMessage.success('智能诊断已生成')
  } finally {
    diagnosing.value = false
  }
}

function adoptDiagnosisType() {
  if (!currentDiagnosis.value?.suggestedServiceType || currentDiagnosis.value.suggestedServiceType === 'REPAIR') {
    return
  }
  applyForm.serviceType = currentDiagnosis.value.suggestedServiceType
}

function openEvidenceDialog() {
  Object.assign(evidenceForm, defaultEvidenceForm())
  evidenceUploadName.value = ''
  evidenceDialogVisible.value = true
}

function openReviewDialog() {
  Object.assign(reviewForm, defaultReviewForm())
  reviewDialogVisible.value = true
}

async function submitApplication() {
  if (!applyingOrder.value) return
  if (!applyForm.reasonText) {
    ElMessage.warning('请填写问题说明')
    return
  }
  submitting.value = true
  try {
    const created = await createCustomerAfterSale({
      ...applyForm,
      orderId: applyingOrder.value.id,
      diagnosisId: currentDiagnosis.value?.id || null
    })
    ElMessage.success('售后申请已提交')
    applyDialogVisible.value = false
    selectedAfterSale.value = created
    await Promise.all([loadOrders(), loadAfterSales()])
    selectedAfterSale.value = await getCustomerAfterSale(created.id)
  } finally {
    submitting.value = false
  }
}

async function submitEvidence() {
  if (!selectedAfterSale.value) return
  if (!evidenceForm.content && !evidenceForm.fileUrl) {
    ElMessage.warning('请填写凭证内容或上传图片')
    return
  }
  if (evidenceForm.evidenceType === 'IMAGE' && !evidenceForm.fileUrl) {
    ElMessage.warning('请先上传图片或填写图片 URL')
    return
  }
  evidenceSubmitting.value = true
  try {
    const evidence = await addCustomerAfterSaleEvidence(selectedAfterSale.value.id, { ...evidenceForm })
    await createEvidenceAudit(evidence.id, { useAi: true })
    ElMessage.success('凭证已提交')
    evidenceDialogVisible.value = false
    selectedAfterSale.value = await getCustomerAfterSale(selectedAfterSale.value.id)
  } finally {
    evidenceSubmitting.value = false
  }
}

function triggerEvidenceFileInput() {
  evidenceFileInput.value?.click()
}

async function handleEvidenceFileChange(event) {
  const file = event.target.files?.[0]
  if (!file) {
    return
  }
  if (!file.type?.startsWith('image/')) {
    ElMessage.warning('只能上传图片凭证')
    event.target.value = ''
    return
  }
  if (file.size > maxEvidenceImageSize) {
    ElMessage.warning('图片凭证不能超过 5MB，请压缩后再上传')
    event.target.value = ''
    return
  }
  uploadingEvidence.value = true
  try {
    const uploaded = await uploadEvidenceFile(file)
    evidenceForm.evidenceType = 'IMAGE'
    evidenceForm.fileUrl = uploaded.fileUrl
    evidenceUploadName.value = uploaded.originalFilename
    if (!evidenceForm.content) {
      evidenceForm.content = `图片凭证：${uploaded.originalFilename}。请客服核对是否为商品实拍原图，并检查是否存在 AI 生成、水印缺失或二次编辑风险。`
    }
    ElMessage.success('图片凭证已上传')
  } finally {
    uploadingEvidence.value = false
    if (event.target) {
      event.target.value = ''
    }
  }
}

async function auditEvidence(evidence) {
  if (!evidence?.id || !selectedAfterSale.value) {
    return
  }
  auditingEvidenceId.value = evidence.id
  try {
    await createEvidenceAudit(evidence.id, { useAi: true })
    ElMessage.success('凭证审核已生成')
    selectedAfterSale.value = await getCustomerAfterSale(selectedAfterSale.value.id)
  } finally {
    auditingEvidenceId.value = null
  }
}

async function submitReview() {
  if (!selectedAfterSale.value) return
  if (!reviewForm.rating) {
    ElMessage.warning('请选择服务评分')
    return
  }
  reviewSubmitting.value = true
  try {
    selectedReview.value = await createCustomerAfterSaleReview(selectedAfterSale.value.id, { ...reviewForm })
    ElMessage.success('评价已提交')
    reviewDialogVisible.value = false
    selectedAfterSale.value = await getCustomerAfterSale(selectedAfterSale.value.id)
  } finally {
    reviewSubmitting.value = false
  }
}

function goChat() {
  router.push('/chat')
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

function nextActionLabel(status) {
  const map = {
    SUBMITTED: '等待管理员审核',
    UNDER_REVIEW: '管理员正在审核',
    NEED_MORE_EVIDENCE: '请按要求补充材料',
    APPROVED: '等待后续处理',
    WAIT_BUYER_SEND: '请寄回商品并保留物流凭证',
    WAIT_SELLER_RECEIVE: '等待商家收货',
    REFUNDING: '等待退款到账',
    EXCHANGING: '等待换货商品发出',
    REJECTED: '查看驳回原因，可重新整理材料',
    COMPLETED: '处理完成，可进行评价',
    CANCELLED: '申请已取消'
  }
  return map[status] || '查看处理记录'
}

function resultSummary(item) {
  if (!item) {
    return '请选择售后申请查看处理结果。'
  }
  if (item.status === 'COMPLETED') {
    return `处理完成，审核金额为 ${money(item.approvedAmount)}。`
  }
  if (item.status === 'REJECTED') {
    const rejectLog = [...(item.processLogs || [])].reverse().find(log => log.action === 'REJECT')
    return `申请已驳回。${rejectLog?.remark || '请查看处理时间线中的驳回原因。'}`
  }
  if (item.status === 'NEED_MORE_EVIDENCE') {
    const evidenceLog = [...(item.processLogs || [])].reverse().find(log => log.action === 'REQUEST_MORE_EVIDENCE')
    return `等待补充材料。${evidenceLog?.remark || '请补充客服要求的凭证。'}`
  }
  if (['WAIT_BUYER_SEND', 'REFUNDING', 'EXCHANGING'].includes(item.status)) {
    return `审核已通过，审核金额为 ${money(item.approvedAmount)}。`
  }
  return '当前申请仍在处理中，请关注时间线和下一步提示。'
}

function defaultApplyForm() {
  return {
    orderId: null,
    serviceType: 'RETURN',
    reasonCode: 'QUALITY_PROBLEM',
    reasonText: '',
    refundAmount: 0
  }
}

function defaultEvidenceForm() {
  return {
    evidenceType: 'TEXT',
    fileUrl: '',
    content: ''
  }
}

function defaultReviewForm() {
  return {
    rating: 5,
    tags: '响应快',
    comment: ''
  }
}

onMounted(reloadAll)
</script>

<style scoped>
.header-actions,
.detail-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.customer-metrics {
  margin-bottom: 14px;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 14px;
}

.panel-note {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 12px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.detail-panel {
  margin-top: 14px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 16px;
}

.timeline-box {
  grid-column: 1 / -1;
  min-width: 0;
}

.result-box {
  padding: 14px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: var(--surface-soft);
}

.diagnosis-detail-box {
  min-width: 0;
}

.result-header,
.final-reply,
.next-action {
  display: flex;
  gap: 10px;
}

.result-header {
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.result-header h4 {
  margin: 0;
  font-size: 14px;
}

.result-box p {
  margin: 0 0 10px;
  color: var(--text);
  line-height: 1.7;
}

.final-reply,
.next-action {
  align-items: flex-start;
  padding-top: 8px;
  border-top: 1px solid var(--line-soft);
  color: var(--text-muted);
  line-height: 1.6;
}

.final-reply + .next-action {
  margin-top: 8px;
}

.final-reply strong,
.next-action strong {
  flex: 0 0 78px;
  color: var(--text);
}

.timeline-box h4 {
  margin: 0 0 12px;
  font-size: 14px;
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

.evidence-list {
  margin-top: 14px;
  padding: 14px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius);
  background: #fff;
}

.review-box {
  margin-top: 14px;
  padding: 14px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius);
  background: #fff;
}

.review-box h4 {
  margin: 0 0 12px;
  font-size: 14px;
}

.review-summary {
  display: flex;
  align-items: center;
  gap: 12px;
}

.review-summary span {
  color: var(--text-muted);
  font-size: 13px;
}

.review-box p {
  margin: 8px 0 0;
  color: var(--text);
  line-height: 1.6;
}

.evidence-list h4 {
  margin: 0 0 12px;
  font-size: 14px;
}

.diagnosis-action {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-muted);
  font-size: 13px;
}

.diagnosis-form-block {
  display: grid;
  width: 100%;
  gap: 8px;
}

.adopt-diagnosis {
  justify-self: start;
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

.evidence-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.evidence-audit {
  margin-top: 10px;
}

.upload-area {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-muted);
  font-size: 13px;
}

.upload-area input {
  display: none;
}

.upload-preview,
.evidence-preview {
  display: block;
  width: min(320px, 100%);
  max-height: 220px;
  margin-top: 10px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  object-fit: contain;
  background: #fff;
}

.image-preview-box {
  margin-top: 10px;
}

.image-preview-box strong {
  display: block;
  margin-bottom: 6px;
  color: var(--text);
  font-size: 13px;
}

@media (max-width: 1120px) {
  .workspace-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
