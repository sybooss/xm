<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">订单管理</h2>
        <p class="page-subtitle">查询演示订单，辅助客服工作台完成订单上下文判断。</p>
      </div>
      <el-button :icon="Refresh" @click="loadOrders">刷新</el-button>
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
          <el-table-column prop="logisticsStatus" label="物流" width="120" />
          <el-table-column prop="afterSaleStatus" label="售后" width="120" />
          <el-table-column prop="signedAt" label="签收时间" width="180" />
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
        <StatusTag :value="selected.orderStatus" />
      </div>
      <div class="panel-body detail-grid">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="商品">{{ selected.productName }}</el-descriptions-item>
          <el-descriptions-item label="规格">{{ selected.skuName }}</el-descriptions-item>
          <el-descriptions-item label="金额">{{ selected.orderAmount }}</el-descriptions-item>
          <el-descriptions-item label="支付状态">{{ selected.payStatus }}</el-descriptions-item>
          <el-descriptions-item label="物流状态">{{ selected.logisticsStatus }}</el-descriptions-item>
          <el-descriptions-item label="售后状态">{{ selected.afterSaleStatus }}</el-descriptions-item>
        </el-descriptions>
        <div>
          <h4>售后记录</h4>
          <el-table :data="afterSales" size="small" empty-text="暂无售后记录">
            <el-table-column prop="afterSaleNo" label="售后单号" min-width="150" />
            <el-table-column prop="serviceType" label="类型" width="90" />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column prop="reason" label="原因" min-width="160" show-overflow-tooltip />
          </el-table>
        </div>
      </div>
    </section>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import StatusTag from '../components/common/StatusTag.vue'
import { listOrderAfterSales, pageOrders } from '../api/orderApi'

const query = reactive({ page: 1, pageSize: 10, keyword: '', orderStatus: '' })
const orders = ref([])
const total = ref(0)
const loading = ref(false)
const selected = ref(null)
const afterSales = ref([])

async function loadOrders() {
  loading.value = true
  try {
    const data = await pageOrders(query)
    orders.value = data?.rows || []
    total.value = data?.total || 0
    if (!selected.value && orders.value.length) {
      await selectOrder(orders.value[0])
    }
  } finally {
    loading.value = false
  }
}

async function selectOrder(row) {
  selected.value = row
  afterSales.value = await listOrderAfterSales(row.id)
}

onMounted(loadOrders)
</script>

<style scoped>
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
