<template>
  <section class="page product-issue-page">
    <div class="page-header">
      <div>
        <p class="page-subtitle">聚合售后、工单和评价，识别同一商品的集中质量问题。</p>
      </div>
      <el-button type="primary" :icon="Refresh" :loading="refreshing" @click="refreshAlerts">
        刷新预警
      </el-button>
    </div>

    <section class="metric-grid issue-metrics">
      <div class="metric">
        <div class="metric-label">开放预警</div>
        <div class="metric-value">{{ summary.openCount || 0 }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">高风险</div>
        <div class="metric-value">{{ summary.highCount || 0 }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">涉及商品</div>
        <div class="metric-value">{{ summary.productCount || 0 }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">样本售后</div>
        <div class="metric-value">{{ summary.sampleCount || 0 }}</div>
      </div>
    </section>

    <section v-if="summary.topAlert" class="panel top-alert">
      <div class="panel-header">
        <div>
          <h3 class="panel-title">当前最需要关注</h3>
          <p class="panel-note">{{ summary.topAlert.productName }} · {{ summary.topAlert.issueKeyword }}</p>
        </div>
        <StatusTag :value="summary.topAlert.alertLevel" />
      </div>
      <div class="panel-body top-alert-body">
        <div class="score-ring">{{ summary.topAlert.trendScore }}</div>
        <p>{{ summary.topAlert.suggestedAction }}</p>
      </div>
    </section>

    <section class="panel">
      <div class="panel-header">
        <div class="toolbar">
          <el-select v-model="query.days" style="width: 132px" @change="reload">
            <el-option label="最近 7 天" :value="7" />
            <el-option label="最近 30 天" :value="30" />
          </el-select>
          <el-select v-model="query.alertLevel" clearable placeholder="预警等级" style="width: 132px" @change="reload">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
          <el-input v-model="query.keyword" clearable placeholder="商品、关键词或建议动作" style="width: 260px" @keyup.enter="reload" />
          <el-button :icon="Search" @click="reload">查询</el-button>
        </div>
      </div>
      <div class="panel-body">
        <el-table v-loading="loading" :data="alerts" height="520">
          <el-table-column prop="productName" label="商品" min-width="190" show-overflow-tooltip />
          <el-table-column prop="issueKeyword" label="问题关键词" width="120" />
          <el-table-column label="等级" width="90">
            <template #default="{ row }"><StatusTag :value="row.alertLevel" /></template>
          </el-table-column>
          <el-table-column prop="trendScore" label="趋势分" width="90" />
          <el-table-column prop="applicationCount" label="售后数" width="90" />
          <el-table-column prop="ticketCount" label="工单数" width="90" />
          <el-table-column prop="lowRatingCount" label="低分评价" width="100" />
          <el-table-column label="退款金额" width="120">
            <template #default="{ row }">{{ money(row.refundAmount) }}</template>
          </el-table-column>
          <el-table-column prop="suggestedAction" label="运营建议" min-width="300" show-overflow-tooltip />
          <el-table-column label="操作" width="128" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="goReview(row)">看样本</el-button>
            </template>
          </el-table-column>
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="expand-content">
                <div>
                  <span class="expand-label">样本售后单：</span>
                  <el-tag v-for="id in row.sampleApplicationIdList || []" :key="id" class="sample-tag" @click="goReviewById(id, row)">
                    #{{ id }}
                  </el-tag>
                  <span v-if="!row.sampleApplicationIdList?.length">-</span>
                </div>
                <div class="sample-reasons">
                  <span class="expand-label">样本原因：</span>
                  <p v-for="reason in row.sampleReasonList || []" :key="reason">{{ reason }}</p>
                  <span v-if="!row.sampleReasonList?.length">暂无摘要</span>
                </div>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          class="pager"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @current-change="loadAlerts"
          @size-change="loadAlerts"
        />
      </div>
    </section>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import StatusTag from '../components/common/StatusTag.vue'
import {
  getProductIssueInsightSummary,
  pageProductIssueInsights,
  refreshProductIssueInsights
} from '../api/productIssueApi'

const router = useRouter()
const query = reactive({ page: 1, pageSize: 10, days: 7, alertLevel: '', keyword: '' })
const alerts = ref([])
const total = ref(0)
const summary = ref({})
const loading = ref(false)
const refreshing = ref(false)

async function loadAlerts() {
  loading.value = true
  try {
    const data = await pageProductIssueInsights(query)
    alerts.value = data?.rows || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadSummary() {
  summary.value = await getProductIssueInsightSummary({ days: query.days }) || {}
}

async function reload() {
  query.page = 1
  await Promise.all([loadAlerts(), loadSummary()])
}

async function refreshAlerts() {
  refreshing.value = true
  try {
    const result = await refreshProductIssueInsights({ days: query.days })
    ElMessage.success(`商品质量预警已刷新：${result?.refreshedCount || 0} 条`)
    await reload()
  } finally {
    refreshing.value = false
  }
}

function goReview(row) {
  router.push({ path: '/admin/after-sales/review', query: { keyword: row.productName } })
}

function goReviewById(_id, row) {
  router.push({ path: '/admin/after-sales/review', query: { keyword: row.productName } })
}

function money(value) {
  const number = Number(value || 0)
  return `￥${number.toFixed(2)}`
}

onMounted(async () => {
  await reload()
  if (!alerts.value.length) {
    await refreshAlerts()
  }
})
</script>

<style scoped>
.issue-metrics {
  margin-bottom: 14px;
}

.top-alert {
  margin-bottom: 14px;
}

.top-alert-body {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.score-ring {
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  border: 1px solid rgb(220 38 38 / 24%);
  border-radius: 50%;
  background: rgb(254 242 242);
  color: #b42318;
  font-size: 20px;
  font-weight: 800;
}

.expand-content {
  display: grid;
  gap: 10px;
  padding: 8px 18px;
  color: var(--text);
}

.expand-label {
  color: var(--muted);
  font-weight: 700;
}

.sample-tag {
  margin: 0 6px 6px 0;
  cursor: pointer;
}

.sample-reasons p {
  margin: 6px 0 0;
  color: var(--muted);
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

@media (max-width: 760px) {
  .top-alert-body {
    grid-template-columns: 1fr;
  }
}
</style>
