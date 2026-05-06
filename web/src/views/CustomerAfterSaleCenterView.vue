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
        </div>
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
            <el-option label="图片链接" value="IMAGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="凭证内容">
          <el-input v-model="evidenceForm.content" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="填写物流单号、问题说明或凭证链接。" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="evidenceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="evidenceSubmitting" @click="submitEvidence">提交凭证</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import StatusTag from '../components/common/StatusTag.vue'
import { pageOrders } from '../api/orderApi'
import {
  addCustomerAfterSaleEvidence,
  createCustomerAfterSale,
  getCustomerAfterSale,
  pageCustomerAfterSales
} from '../api/customerAfterSaleApi'

const router = useRouter()
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
const applyDialogVisible = ref(false)
const evidenceDialogVisible = ref(false)
const applyForm = reactive(defaultApplyForm())
const evidenceForm = reactive(defaultEvidenceForm())

const activeAfterSaleCount = computed(() => afterSales.value.filter(item => !['REJECTED', 'COMPLETED', 'CANCELLED'].includes(item.status)).length)
const needCustomerActionCount = computed(() => afterSales.value.filter(item => ['NEED_MORE_EVIDENCE', 'WAIT_BUYER_SEND'].includes(item.status)).length)
const completedAfterSaleCount = computed(() => afterSales.value.filter(item => item.status === 'COMPLETED').length)

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
}

function openApplyDialog(order) {
  applyingOrder.value = order
  Object.assign(applyForm, defaultApplyForm(), {
    orderId: order.id,
    refundAmount: Number(order.orderAmount || 0)
  })
  applyDialogVisible.value = true
}

function openEvidenceDialog() {
  Object.assign(evidenceForm, defaultEvidenceForm())
  evidenceDialogVisible.value = true
}

async function submitApplication() {
  if (!applyingOrder.value) return
  if (!applyForm.reasonText) {
    ElMessage.warning('请填写问题说明')
    return
  }
  submitting.value = true
  try {
    const created = await createCustomerAfterSale({ ...applyForm, orderId: applyingOrder.value.id })
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
  if (!evidenceForm.content) {
    ElMessage.warning('请填写凭证内容')
    return
  }
  evidenceSubmitting.value = true
  try {
    await addCustomerAfterSaleEvidence(selectedAfterSale.value.id, { ...evidenceForm })
    ElMessage.success('凭证已提交')
    evidenceDialogVisible.value = false
    selectedAfterSale.value = await getCustomerAfterSale(selectedAfterSale.value.id)
  } finally {
    evidenceSubmitting.value = false
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
    content: ''
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
  min-width: 0;
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

.evidence-list h4 {
  margin: 0 0 12px;
  font-size: 14px;
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

@media (max-width: 1120px) {
  .workspace-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
