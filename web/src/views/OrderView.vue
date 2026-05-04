<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">订单管理</h2>
        <p class="page-subtitle">维护演示订单和售后记录，为客服工作台提供可靠订单上下文。</p>
      </div>
      <div class="header-buttons">
        <el-button v-if="authStore.isAdmin" type="primary" :icon="Plus" @click="openOrderDialog()">新增订单</el-button>
        <el-button :icon="Refresh" @click="loadOrders">刷新</el-button>
      </div>
    </div>

    <section class="panel">
      <div class="panel-header">
        <div class="toolbar">
          <el-input v-model="query.keyword" placeholder="订单号或商品名" clearable style="width: 240px" @keyup.enter="loadOrders" />
          <el-select v-model="query.orderStatus" placeholder="订单状态" clearable style="width: 150px">
            <el-option label="已支付" value="PAID" />
            <el-option label="已发货" value="SHIPPED" />
            <el-option label="已签收" value="SIGNED" />
            <el-option label="售后中" value="AFTER_SALE" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
          <el-select v-model="query.afterSaleStatus" placeholder="售后状态" clearable style="width: 150px">
            <el-option label="无售后" value="NONE" />
            <el-option label="退货中" value="RETURN_APPLYING" />
            <el-option label="换货中" value="EXCHANGE_APPLYING" />
            <el-option label="退款中" value="REFUNDING" />
            <el-option label="已完成" value="FINISHED" />
            <el-option label="已拒绝" value="REJECTED" />
          </el-select>
          <el-button :icon="Search" @click="loadOrders">查询</el-button>
        </div>
      </div>
      <div class="panel-body">
        <el-table v-loading="loading" :data="orders" height="430" @row-click="selectOrder">
          <el-table-column prop="orderNo" label="订单号" width="170" />
          <el-table-column prop="productName" label="商品" min-width="220" show-overflow-tooltip />
          <el-table-column prop="skuName" label="规格" min-width="140" show-overflow-tooltip />
          <el-table-column prop="orderAmount" label="金额" width="100" />
          <el-table-column label="订单状态" width="110">
            <template #default="{ row }"><StatusTag :value="row.orderStatus" /></template>
          </el-table-column>
          <el-table-column label="物流" width="110">
            <template #default="{ row }"><StatusTag :value="row.logisticsStatus" /></template>
          </el-table-column>
          <el-table-column label="售后" width="120">
            <template #default="{ row }"><StatusTag :value="row.afterSaleStatus" /></template>
          </el-table-column>
          <el-table-column prop="signedAt" label="签收时间" width="180" />
          <el-table-column v-if="authStore.isAdmin" label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click.stop="openOrderDialog(row)">编辑</el-button>
              <el-button size="small" type="danger" @click.stop="deleteOrderRow(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          class="pager"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @current-change="loadOrders"
          @size-change="loadOrders"
        />
      </div>
    </section>

    <section v-if="selected" class="panel detail-panel">
      <div class="panel-header">
        <div>
          <h3 class="panel-title">订单详情：{{ selected.orderNo }}</h3>
          <p class="panel-note">{{ selected.productName }} · {{ selected.skuName }}</p>
        </div>
        <div class="detail-actions">
          <StatusTag :value="selected.orderStatus" />
          <el-button v-if="authStore.isAdmin" type="primary" :icon="Plus" @click="openAfterSaleDialog()">新增售后</el-button>
        </div>
      </div>
      <div class="panel-body detail-grid">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户ID">{{ selected.userId }}</el-descriptions-item>
          <el-descriptions-item label="金额">{{ selected.orderAmount }}</el-descriptions-item>
          <el-descriptions-item label="支付状态"><StatusTag :value="selected.payStatus" /></el-descriptions-item>
          <el-descriptions-item label="物流状态"><StatusTag :value="selected.logisticsStatus" /></el-descriptions-item>
          <el-descriptions-item label="售后状态"><StatusTag :value="selected.afterSaleStatus" /></el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ selected.createdAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ selected.paidAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="签收时间">{{ selected.signedAt || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div>
          <h4>售后记录</h4>
          <el-table :data="afterSales" size="small" empty-text="暂无售后记录">
            <el-table-column prop="afterSaleNo" label="售后单号" min-width="150" />
            <el-table-column label="类型" width="100">
              <template #default="{ row }"><StatusTag :value="row.serviceType" /></template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }"><StatusTag :value="row.status" /></template>
            </el-table-column>
            <el-table-column prop="refundAmount" label="退款" width="90" />
            <el-table-column prop="reason" label="原因" min-width="160" show-overflow-tooltip />
            <el-table-column v-if="authStore.isAdmin" label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openAfterSaleDialog(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="deleteAfterSaleRow(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </section>

    <el-dialog v-model="orderDialogVisible" :title="editingOrder?.id ? '编辑订单' : '新增订单'" width="640px" destroy-on-close>
      <el-form :model="orderForm" label-width="92px">
        <div class="form-grid">
          <el-form-item label="订单号"><el-input v-model="orderForm.orderNo" /></el-form-item>
          <el-form-item label="用户ID"><el-input-number v-model="orderForm.userId" :min="1" controls-position="right" /></el-form-item>
          <el-form-item label="商品"><el-input v-model="orderForm.productName" /></el-form-item>
          <el-form-item label="规格"><el-input v-model="orderForm.skuName" /></el-form-item>
          <el-form-item label="金额"><el-input-number v-model="orderForm.orderAmount" :min="0" :precision="2" controls-position="right" /></el-form-item>
          <el-form-item label="支付状态">
            <el-select v-model="orderForm.payStatus">
              <el-option label="待支付" value="UNPAID" />
              <el-option label="已支付" value="PAID" />
              <el-option label="已退款" value="REFUNDED" />
            </el-select>
          </el-form-item>
          <el-form-item label="订单状态">
            <el-select v-model="orderForm.orderStatus">
              <el-option label="已支付" value="PAID" />
              <el-option label="已发货" value="SHIPPED" />
              <el-option label="已签收" value="SIGNED" />
              <el-option label="售后中" value="AFTER_SALE" />
              <el-option label="已关闭" value="CLOSED" />
            </el-select>
          </el-form-item>
          <el-form-item label="物流状态">
            <el-select v-model="orderForm.logisticsStatus">
              <el-option label="未发货" value="NOT_SHIPPED" />
              <el-option label="运输中" value="IN_TRANSIT" />
              <el-option label="已签收" value="DELIVERED" />
              <el-option label="异常" value="ABNORMAL" />
            </el-select>
          </el-form-item>
          <el-form-item label="售后状态">
            <el-select v-model="orderForm.afterSaleStatus">
              <el-option label="无售后" value="NONE" />
              <el-option label="退货中" value="RETURN_APPLYING" />
              <el-option label="换货中" value="EXCHANGE_APPLYING" />
              <el-option label="退款中" value="REFUNDING" />
              <el-option label="已完成" value="FINISHED" />
              <el-option label="已拒绝" value="REJECTED" />
            </el-select>
          </el-form-item>
          <el-form-item label="支付时间"><el-date-picker v-model="orderForm.paidAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
          <el-form-item label="发货时间"><el-date-picker v-model="orderForm.shippedAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
          <el-form-item label="签收时间"><el-date-picker v-model="orderForm.signedAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="orderDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingOrder" @click="saveOrder">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="afterSaleDialogVisible" :title="editingAfterSale?.id ? '编辑售后记录' : '新增售后记录'" width="560px" destroy-on-close>
      <el-form :model="afterSaleForm" label-width="92px">
        <el-form-item label="订单">
          <span>{{ selected?.orderNo }} · {{ selected?.productName }}</span>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="afterSaleForm.serviceType">
            <el-radio-button label="RETURN">退货退款</el-radio-button>
            <el-radio-button label="EXCHANGE">换货</el-radio-button>
            <el-radio-button label="REFUND">仅退款</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="afterSaleForm.status">
            <el-option label="已提交" value="APPLIED" />
            <el-option label="审核通过" value="APPROVED" />
            <el-option label="待买家寄回" value="WAIT_BUYER_SEND" />
            <el-option label="待商家确认" value="WAIT_SELLER_CONFIRM" />
            <el-option label="退款中" value="REFUNDING" />
            <el-option label="已完成" value="FINISHED" />
            <el-option label="已拒绝" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="退款金额"><el-input-number v-model="afterSaleForm.refundAmount" :min="0" :precision="2" controls-position="right" /></el-form-item>
        <el-form-item label="原因"><el-input v-model="afterSaleForm.reason" type="textarea" :rows="3" maxlength="300" show-word-limit /></el-form-item>
        <el-form-item label="备注"><el-input v-model="afterSaleForm.remark" type="textarea" :rows="2" maxlength="300" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="afterSaleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingAfterSale" @click="saveAfterSale">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import StatusTag from '../components/common/StatusTag.vue'
import {
  createAfterSaleRecord,
  createOrder,
  deleteAfterSaleRecord,
  deleteOrder,
  listOrderAfterSales,
  pageOrders,
  updateAfterSaleRecord,
  updateOrder
} from '../api/orderApi'
import { useAuthStore } from '../stores/authStore'

const authStore = useAuthStore()
const query = reactive({ page: 1, pageSize: 10, keyword: '', orderStatus: '', afterSaleStatus: '' })
const orders = ref([])
const total = ref(0)
const loading = ref(false)
const selected = ref(null)
const afterSales = ref([])

const orderDialogVisible = ref(false)
const editingOrder = ref(null)
const savingOrder = ref(false)
const orderForm = reactive(defaultOrderForm())

const afterSaleDialogVisible = ref(false)
const editingAfterSale = ref(null)
const savingAfterSale = ref(false)
const afterSaleForm = reactive(defaultAfterSaleForm())

async function loadOrders() {
  loading.value = true
  try {
    const data = await pageOrders(query)
    orders.value = data?.rows || []
    total.value = data?.total || 0
    if (!selected.value && orders.value.length) {
      await selectOrder(orders.value[0])
    } else if (selected.value) {
      const latest = orders.value.find(item => item.id === selected.value.id)
      if (latest) {
        await selectOrder(latest)
      }
    }
  } finally {
    loading.value = false
  }
}

async function selectOrder(row) {
  selected.value = row
  afterSales.value = await listOrderAfterSales(row.id)
}

function openOrderDialog(row = null) {
  editingOrder.value = row
  Object.assign(orderForm, defaultOrderForm(), row || {})
  orderDialogVisible.value = true
}

async function saveOrder() {
  if (!orderForm.orderNo || !orderForm.productName) {
    ElMessage.warning('请填写订单号和商品名称')
    return
  }
  savingOrder.value = true
  try {
    const payload = { ...orderForm }
    if (editingOrder.value?.id) {
      await updateOrder(editingOrder.value.id, payload)
      ElMessage.success('订单已更新')
    } else {
      await createOrder(payload)
      ElMessage.success('订单已新增')
    }
    orderDialogVisible.value = false
    await loadOrders()
  } finally {
    savingOrder.value = false
  }
}

async function deleteOrderRow(row) {
  await ElMessageBox.confirm(`确定删除订单 ${row.orderNo} 吗？`, '删除订单', { type: 'warning' })
  await deleteOrder(row.id)
  ElMessage.success('订单已删除')
  if (selected.value?.id === row.id) {
    selected.value = null
    afterSales.value = []
  }
  await loadOrders()
}

function openAfterSaleDialog(row = null) {
  if (!selected.value) return
  editingAfterSale.value = row
  Object.assign(afterSaleForm, defaultAfterSaleForm(), row || {}, { orderId: selected.value.id })
  afterSaleDialogVisible.value = true
}

async function saveAfterSale() {
  if (!selected.value) return
  if (!afterSaleForm.reason) {
    ElMessage.warning('请填写售后原因')
    return
  }
  savingAfterSale.value = true
  try {
    const payload = { ...afterSaleForm, orderId: selected.value.id }
    if (editingAfterSale.value?.id) {
      await updateAfterSaleRecord(editingAfterSale.value.id, payload)
      ElMessage.success('售后记录已更新')
    } else {
      await createAfterSaleRecord(payload)
      ElMessage.success('售后记录已新增')
    }
    afterSaleDialogVisible.value = false
    afterSales.value = await listOrderAfterSales(selected.value.id)
    await loadOrders()
  } finally {
    savingAfterSale.value = false
  }
}

async function deleteAfterSaleRow(row) {
  await ElMessageBox.confirm(`确定删除售后记录 ${row.afterSaleNo} 吗？`, '删除售后记录', { type: 'warning' })
  await deleteAfterSaleRecord(row.id)
  ElMessage.success('售后记录已删除')
  afterSales.value = await listOrderAfterSales(selected.value.id)
  await loadOrders()
}

function defaultOrderForm() {
  return {
    orderNo: '',
    userId: 1,
    productName: '',
    skuName: '',
    orderAmount: 0,
    payStatus: 'PAID',
    orderStatus: 'SIGNED',
    logisticsStatus: 'DELIVERED',
    afterSaleStatus: 'NONE',
    paidAt: '',
    shippedAt: '',
    signedAt: ''
  }
}

function defaultAfterSaleForm() {
  return {
    orderId: null,
    serviceType: 'RETURN',
    reason: '',
    status: 'APPLIED',
    refundAmount: 0,
    remark: ''
  }
}

onMounted(loadOrders)
</script>

<style scoped>
.header-buttons,
.detail-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-note {
  margin: 2px 0 0;
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
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.9fr);
  gap: 14px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 14px;
}

.form-grid :deep(.el-input-number),
.form-grid :deep(.el-select),
.form-grid :deep(.el-date-editor) {
  width: 100%;
}

h4 {
  margin: 0 0 10px;
  font-size: 14px;
}

@media (max-width: 980px) {
  .detail-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
