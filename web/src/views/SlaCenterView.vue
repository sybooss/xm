<template>
  <section class="page sla-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">SLA 跟进</h2>
        <p class="page-subtitle">按 SLA 截止时间、优先级和补材料状态识别需要优先处理的售后申请。</p>
      </div>
      <el-button :icon="Refresh" @click="loadTasks">刷新</el-button>
    </div>

    <section class="metric-grid sla-metrics">
      <div class="metric">
        <div class="metric-label">风险任务</div>
        <div class="metric-value">{{ total }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">已超时</div>
        <div class="metric-value">{{ overdueCount }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">24小时内到期</div>
        <div class="metric-value">{{ dueSoonCount }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">高优先级</div>
        <div class="metric-value">{{ highPriorityCount }}</div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-header">
        <div class="toolbar">
          <el-input v-model="query.keyword" clearable placeholder="售后单号、订单号或商品名" style="width: 260px" @keyup.enter="loadTasks" />
          <el-select v-model="query.riskType" clearable placeholder="风险类型" style="width: 150px" @change="loadTasks">
            <el-option label="已超时" value="OVERDUE" />
            <el-option label="即将超时" value="DUE_SOON" />
            <el-option label="高优先级" value="HIGH_PRIORITY" />
            <el-option label="待顾客补材料" value="WAITING_CUSTOMER" />
          </el-select>
          <el-select v-model="query.status" clearable placeholder="状态" style="width: 150px" @change="loadTasks">
            <el-option label="已提交" value="SUBMITTED" />
            <el-option label="待补材料" value="NEED_MORE_EVIDENCE" />
            <el-option label="待寄回" value="WAIT_BUYER_SEND" />
            <el-option label="退款中" value="REFUNDING" />
          </el-select>
          <el-button :icon="Search" @click="loadTasks">查询</el-button>
        </div>
      </div>
      <div class="panel-body">
        <el-table v-loading="loading" :data="tasks" height="520">
          <el-table-column prop="applicationNo" label="售后单号" min-width="158" />
          <el-table-column prop="orderNo" label="订单号" min-width="150" />
          <el-table-column prop="productName" label="商品" min-width="180" show-overflow-tooltip />
          <el-table-column label="风险" width="110">
            <template #default="{ row }"><StatusTag :value="row.riskLabel" /></template>
          </el-table-column>
          <el-table-column label="评估等级" width="100">
            <template #default="{ row }"><StatusTag :value="row.assessmentRiskLevel || row.riskLevel" /></template>
          </el-table-column>
          <el-table-column label="风险分" width="90">
            <template #default="{ row }">{{ row.riskScore ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }"><StatusTag :value="row.status" /></template>
          </el-table-column>
          <el-table-column label="优先级" width="90">
            <template #default="{ row }"><StatusTag :value="row.priority" /></template>
          </el-table-column>
          <el-table-column prop="slaDeadline" label="SLA 截止" min-width="168" />
          <el-table-column label="剩余小时" width="100">
            <template #default="{ row }">{{ row.remainingHours ?? '-' }}</template>
          </el-table-column>
          <el-table-column prop="suggestedAction" label="风控建议" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作" width="128" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="goReview(row)">处理</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          class="pager"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @current-change="loadTasks"
          @size-change="loadTasks"
        />
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import StatusTag from '../components/common/StatusTag.vue'
import { pageSlaTasks } from '../api/slaApi'

const router = useRouter()
const query = reactive({ page: 1, pageSize: 10, keyword: '', riskType: '', status: '' })
const tasks = ref([])
const total = ref(0)
const loading = ref(false)

const overdueCount = computed(() => tasks.value.filter(item => item.riskLabel === '已超时').length)
const dueSoonCount = computed(() => tasks.value.filter(item => item.riskLabel === '即将超时').length)
const highPriorityCount = computed(() => tasks.value.filter(item => ['HIGH', 'URGENT'].includes(item.priority)).length)

async function loadTasks() {
  loading.value = true
  try {
    const data = await pageSlaTasks(query)
    tasks.value = data?.rows || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}

function goReview(row) {
  router.push({ path: '/admin/after-sales/review', query: { keyword: row.applicationNo } })
}

onMounted(loadTasks)
</script>

<style scoped>
.sla-metrics {
  margin-bottom: 14px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
