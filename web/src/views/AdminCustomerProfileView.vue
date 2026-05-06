<template>
  <section class="page customer-profile-page">
    <div class="page-header">
      <div>
        <p class="page-subtitle">汇总客户订单、售后、工单和评价，识别重复投诉与低满意度风险。</p>
      </div>
      <div class="toolbar">
        <el-input-number v-model="queryUserId" :min="1" controls-position="right" />
        <el-button type="primary" :icon="Search" @click="loadProfile(queryUserId)">查询客户</el-button>
      </div>
    </div>

    <section v-if="profile" class="metric-grid profile-metrics">
      <div class="metric">
        <div class="metric-label">客户</div>
        <div class="metric-value">{{ profile.customer?.displayName || '-' }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">售后次数</div>
        <div class="metric-value">{{ profile.afterSaleCount }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">平均评分</div>
        <div class="metric-value">{{ averageRating }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">风险等级</div>
        <div class="metric-value"><StatusTag :value="profile.riskLevel" /></div>
      </div>
    </section>

    <section v-if="profile" class="profile-grid">
      <div class="panel">
        <div class="panel-header">
          <h3 class="panel-title">客户画像</h3>
          <StatusTag :value="profile.riskLevel" />
        </div>
        <div class="panel-body">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="账号">{{ profile.customer?.username }}</el-descriptions-item>
            <el-descriptions-item label="电话">{{ profile.customer?.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="订单数">{{ profile.orderCount }}</el-descriptions-item>
            <el-descriptions-item label="订单金额">{{ money(profile.totalOrderAmount) }}</el-descriptions-item>
            <el-descriptions-item label="进行中售后">{{ profile.activeAfterSaleCount }}</el-descriptions-item>
            <el-descriptions-item label="人工工单">{{ profile.ticketCount }}</el-descriptions-item>
            <el-descriptions-item label="评价数">{{ profile.reviewCount }}</el-descriptions-item>
            <el-descriptions-item label="平均评分">{{ averageRating }}</el-descriptions-item>
          </el-descriptions>
          <div class="risk-box">
            <h4>运营判断</h4>
            <p>{{ riskText }}</p>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-header"><h3 class="panel-title">服务评价</h3></div>
        <div class="panel-body review-list">
          <div v-if="!profile.reviews?.length" class="empty">暂无评价</div>
          <div v-for="review in profile.reviews || []" :key="review.id" class="review-item">
            <div>
              <el-rate :model-value="review.rating" disabled />
              <span>{{ review.applicationNo }}</span>
            </div>
            <strong>{{ review.tags || '无标签' }}</strong>
            <p>{{ review.comment || '暂无文字评价' }}</p>
          </div>
        </div>
      </div>
    </section>

    <section v-if="profile" class="panel">
      <div class="panel-header"><h3 class="panel-title">最近售后</h3></div>
      <div class="panel-body">
        <el-table :data="profile.recentAfterSales || []" height="280">
          <el-table-column prop="applicationNo" label="售后单" min-width="160" />
          <el-table-column prop="orderNo" label="订单号" min-width="150" />
          <el-table-column prop="productName" label="商品" min-width="180" show-overflow-tooltip />
          <el-table-column label="类型" width="100">
            <template #default="{ row }"><StatusTag :value="row.serviceType" /></template>
          </el-table-column>
          <el-table-column label="状态" width="130">
            <template #default="{ row }"><StatusTag :value="row.status" /></template>
          </el-table-column>
          <el-table-column label="风险" width="100">
            <template #default="{ row }"><StatusTag :value="row.riskLevel" /></template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <section v-if="profile" class="profile-grid bottom-grid">
      <div class="panel">
        <div class="panel-header"><h3 class="panel-title">最近订单</h3></div>
        <div class="panel-body compact-list">
          <div v-for="order in profile.recentOrders || []" :key="order.id" class="compact-row">
            <span>{{ order.orderNo }}</span>
            <strong>{{ order.productName }}</strong>
            <StatusTag :value="order.afterSaleStatus" />
          </div>
        </div>
      </div>
      <div class="panel">
        <div class="panel-header"><h3 class="panel-title">最近工单</h3></div>
        <div class="panel-body compact-list">
          <div v-if="!profile.recentTickets?.length" class="empty">暂无人工工单</div>
          <div v-for="ticket in profile.recentTickets || []" :key="ticket.id" class="compact-row">
            <span>{{ ticket.ticketNo }}</span>
            <strong>{{ ticket.customerIssue }}</strong>
            <StatusTag :value="ticket.status" />
          </div>
        </div>
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import StatusTag from '../components/common/StatusTag.vue'
import { getCustomerProfile } from '../api/customerProfileApi'

const route = useRoute()
const queryUserId = ref(Number(route.query.userId || 1))
const profile = ref(null)

const averageRating = computed(() => {
  if (profile.value?.averageRating === null || typeof profile.value?.averageRating === 'undefined') {
    return '-'
  }
  return Number(profile.value.averageRating).toFixed(1)
})

const riskText = computed(() => {
  const risk = profile.value?.riskLevel
  if (risk === 'HIGH') {
    return '该客户存在低评分、重复售后或多次人工工单信号，建议优先人工跟进并保留处理证据。'
  }
  if (risk === 'MEDIUM') {
    return '该客户存在一定售后或工单频次，建议客服在回复中明确下一步动作和时限。'
  }
  return '客户历史较稳定，可按标准售后流程处理。'
})

async function loadProfile(userId) {
  if (!userId) return
  profile.value = await getCustomerProfile(userId)
}

function money(value) {
  if (value === null || typeof value === 'undefined') {
    return '-'
  }
  return `￥${Number(value).toFixed(2)}`
}

onMounted(() => loadProfile(queryUserId.value))
</script>

<style scoped>
.profile-metrics {
  margin-bottom: 14px;
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.9fr);
  gap: 14px;
  margin-bottom: 14px;
}

.risk-box {
  margin-top: 14px;
  padding: 14px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: var(--surface-soft);
}

.risk-box h4 {
  margin: 0 0 6px;
}

.risk-box p,
.review-item p {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.6;
}

.review-item {
  padding: 12px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: var(--surface-soft);
}

.review-item + .review-item {
  margin-top: 10px;
}

.review-item div,
.compact-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.review-item span,
.compact-row span {
  color: var(--text-muted);
  font-size: 12px;
}

.review-item strong {
  display: block;
  margin: 6px 0;
}

.compact-list {
  display: grid;
  gap: 10px;
}

.compact-row {
  justify-content: space-between;
  padding: 10px 12px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: var(--surface-soft);
}

.compact-row strong {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty {
  color: var(--text-muted);
  font-size: 13px;
}

@media (max-width: 1120px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
