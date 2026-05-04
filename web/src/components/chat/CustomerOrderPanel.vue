<template>
  <section class="customer-orders">
    <div class="orders-toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索订单号或商品"
        clearable
        @keyup.enter="loadOrders"
      />
      <el-select v-model="query.afterSaleStatus" placeholder="售后状态" clearable>
        <el-option label="无售后" value="NONE" />
        <el-option label="退货中" value="RETURN_APPLYING" />
        <el-option label="换货中" value="EXCHANGE_APPLYING" />
        <el-option label="退款中" value="REFUNDING" />
        <el-option label="已完成" value="FINISHED" />
        <el-option label="已拒绝" value="REJECTED" />
      </el-select>
      <el-button :icon="Search" @click="loadOrders">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="orders" height="100%" @row-dblclick="selectOrder">
      <el-table-column prop="orderNo" label="订单号" width="168" />
      <el-table-column prop="productName" label="商品" min-width="180" show-overflow-tooltip />
      <el-table-column prop="skuName" label="规格" min-width="120" show-overflow-tooltip />
      <el-table-column label="订单状态" width="104">
        <template #default="{ row }"><StatusTag :value="row.orderStatus" /></template>
      </el-table-column>
      <el-table-column label="售后" width="120">
        <template #default="{ row }"><StatusTag :value="row.afterSaleStatus" /></template>
      </el-table-column>
      <el-table-column prop="signedAt" label="签收时间" width="168" />
      <el-table-column label="操作" width="176" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="selectOrder(row)">选择</el-button>
          <el-button size="small" type="primary" :disabled="hasActiveAfterSale(row)" @click="openApply(row)">
            申请售后
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="applyVisible" title="申请售后" width="460px" destroy-on-close>
      <el-form :model="applyForm" label-width="84px">
        <el-form-item label="订单">
          <span>{{ activeOrder?.orderNo }} · {{ activeOrder?.productName }}</span>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="applyForm.serviceType">
            <el-radio-button label="RETURN">退货退款</el-radio-button>
            <el-radio-button label="EXCHANGE">换货</el-radio-button>
            <el-radio-button label="REFUND">仅退款</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="原因">
          <el-input
            v-model="applyForm.reason"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            placeholder="请简要描述问题，例如商品质量、尺寸不合适、物流破损等"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { createOrderAfterSale, pageOrders } from '../../api/orderApi'
import { useAuthStore } from '../../stores/authStore'
import StatusTag from '../common/StatusTag.vue'

const emit = defineEmits(['select'])
const authStore = useAuthStore()

const query = reactive({
  page: 1,
  pageSize: 100,
  keyword: '',
  afterSaleStatus: ''
})
const orders = ref([])
const loading = ref(false)
const applyVisible = ref(false)
const submitting = ref(false)
const activeOrder = ref(null)
const applyForm = reactive({
  serviceType: 'RETURN',
  reason: ''
})

async function loadOrders() {
  loading.value = true
  try {
    const data = await pageOrders({
      ...query,
      userId: authStore.user?.role === 'ADMIN' ? undefined : authStore.user?.userId
    })
    orders.value = data?.rows || []
  } finally {
    loading.value = false
  }
}

function selectOrder(row) {
  emit('select', row)
}

function openApply(row) {
  activeOrder.value = row
  applyForm.serviceType = 'RETURN'
  applyForm.reason = ''
  applyVisible.value = true
}

async function submitApply() {
  if (!activeOrder.value) return
  if (!applyForm.reason.trim()) {
    ElMessage.warning('请填写售后原因')
    return
  }
  submitting.value = true
  try {
    await createOrderAfterSale(activeOrder.value.id, {
      serviceType: applyForm.serviceType,
      reason: applyForm.reason.trim()
    })
    ElMessage.success('售后申请已提交')
    applyVisible.value = false
    await loadOrders()
  } finally {
    submitting.value = false
  }
}

function hasActiveAfterSale(row) {
  return row.afterSaleStatus && !['NONE', 'REJECTED', 'FINISHED'].includes(row.afterSaleStatus)
}

onMounted(loadOrders)
</script>

<style scoped>
.customer-orders {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
  min-height: 0;
  height: 100%;
  padding: 14px;
  background: #f8fafc;
}

.orders-toolbar {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 150px auto;
  gap: 8px;
}

@media (max-width: 760px) {
  .orders-toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
