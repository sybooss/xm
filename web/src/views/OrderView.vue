<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">订单管理</h2>
        <p class="page-subtitle">查询演示订单，辅助客服工作台完成订单上下文判断。</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="openOrderDialog()">新增订单</el-button>
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
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已关闭" value="CLOSED" />
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
          <el-table-column label="物流" width="120">
            <template #default="{ row }"><StatusTag :value="row.logisticsStatus" /></template>
          </el-table-column>
          <el-table-column label="售后" width="130">
            <template #default="{ row }"><StatusTag :value="row.afterSaleStatus" /></template>
          </el-table-column>
          <el-table-column prop="signedAt" label="签收时间" width="180" />
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click.stop="openOrderDialog(row)">编辑</el-button>
              <el-button link type="danger" @click.stop="deleteSelectedOrder(row)">删除</el-button>
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
        <h3 class="panel-title">订单详情：{{ selected.orderNo }}</h3>
        <div class="header-actions">
          <StatusTag :value="selected.orderStatus" />
          <el-button type="primary" size="small" @click="openAfterSaleDialog()">新增售后</el-button>
        </div>
      </div>
      <div class="panel-body detail-grid">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="商品">{{ selected.productName }}</el-descriptions-item>
          <el-descriptions-item label="规格">{{ selected.skuName }}</el-descriptions-item>
          <el-descriptions-item label="金额">{{ selected.orderAmount }}</el-descriptions-item>
          <el-descriptions-item label="支付状态"><StatusTag :value="selected.payStatus" /></el-descriptions-item>
          <el-descriptions-item label="物流状态"><StatusTag :value="selected.logisticsStatus" /></el-descriptions-item>
          <el-descriptions-item label="售后状态"><StatusTag :value="selected.afterSaleStatus" /></el-descriptions-item>
        </el-descriptions>
        <div>
          <h4>售后记录</h4>
          <el-table :data="afterSales" size="small" empty-text="暂无售后记录">
            <el-table-column prop="afterSaleNo" label="售后单号" min-width="150" />
            <el-table-column label="类型" width="100">
              <template #default="{ row }"><StatusTag :value="row.serviceType" /></template>
            </el-table-column>
            <el-table-column label="状态" width="130">
              <template #default="{ row }"><StatusTag :value="row.status" /></template>
            </el-table-column>
            <el-table-column prop="reason" label="原因" min-width="160" show-overflow-tooltip />
            <el-table-column label="操作" width="130">
              <template #default="{ row }">
                <el-button link type="primary" @click="openAfterSaleDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="deleteSelectedAfterSale(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </section>

    <el-dialog v-model="orderDialogVisible" :title="orderForm.id ? '编辑订单' : '新增订单'" width="560px">
      <el-form :model="orderForm" label-width="96px">
        <el-form-item label="订单号"><el-input v-model="orderForm.orderNo" /></el-form-item>
        <el-form-item label="用户ID"><el-input-number v-model="orderForm.userId" :min="1" controls-position="right" /></el-form-item>
        <el-form-item label="商品"><el-input v-model="orderForm.productName" /></el-form-item>
        <el-form-item label="规格"><el-input v-model="orderForm.skuName" /></el-form-item>
        <el-form-item label="金额"><el-input-number v-model="orderForm.orderAmount" :min="0" :precision="2" controls-position="right" /></el-form-item>
        <el-form-item label="支付状态">
          <el-select v-model="orderForm.payStatus" style="width: 100%">
            <el-option label="未支付" value="UNPAID" />
            <el-option label="已支付" value="PAID" />
            <el-option label="退款中" value="REFUNDING" />
            <el-option label="已退款" value="REFUNDED" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="orderForm.orderStatus" style="width: 100%">
            <el-option label="待支付" value="PENDING_PAY" />
            <el-option label="已支付" value="PAID" />
            <el-option label="已发货" value="SHIPPED" />
            <el-option label="已签收" value="SIGNED" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流状态">
          <el-select v-model="orderForm.logisticsStatus" style="width: 100%">
            <el-option label="未发货" value="NOT_SHIPPED" />
            <el-option label="运输中" value="IN_TRANSIT" />
            <el-option label="已送达" value="DELIVERED" />
            <el-option label="异常" value="ABNORMAL" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="orderDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveOrder">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="afterSaleDialogVisible" :title="afterSaleForm.id ? '编辑售后' : '新增售后'" width="540px">
      <el-form :model="afterSaleForm" label-width="96px">
        <el-form-item label="售后类型">
          <el-select v-model="afterSaleForm.serviceType" style="width: 100%">
            <el-option label="退货" value="RETURN" />
            <el-option label="换货" value="EXCHANGE" />
            <el-option label="退款" value="REFUND" />
            <el-option label="投诉" value="COMPLAINT" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因"><el-input v-model="afterSaleForm.reason" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="afterSaleForm.status" style="width: 100%">
            <el-option label="已申请" value="APPLIED" />
            <el-option label="已批准" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
            <el-option label="待买家寄回" value="WAIT_BUYER_SEND" />
            <el-option label="待商家确认" value="WAIT_SELLER_CONFIRM" />
            <el-option label="退款中" value="REFUNDING" />
            <el-option label="已完成" value="FINISHED" />
          </el-select>
        </el-form-item>
        <el-form-item label="退款金额"><el-input-number v-model="afterSaleForm.refundAmount" :min="0" :precision="2" controls-position="right" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="afterSaleForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="afterSaleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAfterSale">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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

const query = reactive({ page: 1, pageSize: 10, keyword: '', orderStatus: '' })
const orders = ref([])
const total = ref(0)
const loading = ref(false)
const selected = ref(null)
const afterSales = ref([])
const orderDialogVisible = ref(false)
const afterSaleDialogVisible = ref(false)
const orderForm = reactive(defaultOrderForm())
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
      const fresh = orders.value.find(item => item.id === selected.value.id)
      if (fresh) {
        await selectOrder(fresh)
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

function openOrderDialog(row) {
  Object.assign(orderForm, defaultOrderForm(), row || {})
  if (!orderForm.orderNo) {
    orderForm.orderNo = generateOrderNo()
  }
  orderDialogVisible.value = true
}

async function saveOrder() {
  if (!orderForm.orderNo || !orderForm.productName) {
    ElMessage.warning('请填写订单号和商品名称')
    return
  }
  if (orderForm.id) {
    await updateOrder(orderForm.id, orderForm)
  } else {
    await createOrder(orderForm)
  }
  ElMessage.success('订单已保存')
  orderDialogVisible.value = false
  await loadOrders()
}

async function deleteSelectedOrder(row) {
  await ElMessageBox.confirm(`确定删除订单 ${row.orderNo} 吗？`, '删除订单', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteOrder(row.id)
  if (selected.value?.id === row.id) {
    selected.value = null
    afterSales.value = []
  }
  ElMessage.success('订单已删除')
  await loadOrders()
}

function openAfterSaleDialog(row) {
  if (!selected.value) {
    ElMessage.warning('请先选择订单')
    return
  }
  Object.assign(afterSaleForm, defaultAfterSaleForm(), row || { orderId: selected.value.id })
  afterSaleDialogVisible.value = true
}

async function saveAfterSale() {
  if (!afterSaleForm.reason) {
    ElMessage.warning('请填写售后原因')
    return
  }
  if (afterSaleForm.id) {
    await updateAfterSaleRecord(afterSaleForm.id, afterSaleForm)
  } else {
    await createAfterSaleRecord(afterSaleForm)
  }
  ElMessage.success('售后记录已保存')
  afterSaleDialogVisible.value = false
  await selectOrder(selected.value)
  await loadOrders()
}

async function deleteSelectedAfterSale(row) {
  await ElMessageBox.confirm(`确定删除售后记录 ${row.afterSaleNo} 吗？`, '删除售后', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteAfterSaleRecord(row.id)
  ElMessage.success('售后记录已删除')
  await selectOrder(selected.value)
  await loadOrders()
}

function defaultOrderForm() {
  return {
    id: null,
    orderNo: '',
    userId: 1,
    productName: '',
    skuName: '',
    orderAmount: 0,
    payStatus: 'PAID',
    orderStatus: 'SIGNED',
    logisticsStatus: 'DELIVERED',
    afterSaleStatus: 'NONE',
    paidAt: '2026-04-20T10:00:00',
    shippedAt: '2026-04-21T10:00:00',
    signedAt: '2026-04-22T10:00:00'
  }
}

function defaultAfterSaleForm() {
  return {
    id: null,
    orderId: selected.value?.id || null,
    serviceType: 'RETURN',
    reason: '',
    status: 'APPLIED',
    refundAmount: 0,
    remark: ''
  }
}

function generateOrderNo() {
  const now = new Date()
  const date = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
  return `DD${date}${String(Math.floor(Math.random() * 10000)).padStart(4, '0')}`
}

onMounted(loadOrders)
</script>

<style scoped>
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
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
  grid-template-columns: minmax(0, 1fr) minmax(340px, 0.8fr);
  gap: 14px;
}

h4 {
  margin: 0 0 10px;
  font-size: 14px;
}

@media (max-width: 980px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
