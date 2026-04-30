<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">人工工单</h2>
        <p class="page-subtitle">承接投诉、物流异常和手动转人工诉求，展示优先级、处理状态和接续建议。</p>
      </div>
      <el-button :icon="Refresh" @click="loadTickets">刷新</el-button>
    </div>

    <section class="panel">
      <div class="panel-header">
        <div class="toolbar">
          <el-input v-model="query.keyword" placeholder="工单号、订单号或问题" clearable style="width: 260px" @keyup.enter="loadTickets" />
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
          <el-select v-model="query.priority" placeholder="优先级" clearable style="width: 140px">
            <el-option label="普通" value="NORMAL" />
            <el-option label="高" value="HIGH" />
            <el-option label="紧急" value="URGENT" />
          </el-select>
          <el-button :icon="Search" @click="loadTickets">查询</el-button>
        </div>
      </div>

      <div class="panel-body">
        <el-table v-loading="loading" :data="tickets" height="460" @row-click="selectTicket">
          <el-table-column prop="ticketNo" label="工单号" width="175" />
          <el-table-column prop="orderNo" label="订单号" width="160" />
          <el-table-column prop="intentCode" label="意图" width="150" show-overflow-tooltip />
          <el-table-column label="优先级" width="95">
            <template #default="{ row }"><StatusTag :value="row.priority" /></template>
          </el-table-column>
          <el-table-column label="状态" width="105">
            <template #default="{ row }"><StatusTag :value="row.status" /></template>
          </el-table-column>
          <el-table-column prop="customerIssue" label="用户问题" min-width="240" show-overflow-tooltip />
          <el-table-column prop="assignedTo" label="处理人" width="120" />
          <el-table-column prop="createdAt" label="创建时间" width="180" />
        </el-table>
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          class="pager"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @current-change="loadTickets"
          @size-change="loadTickets"
        />
      </div>
    </section>

    <section v-if="selected" class="panel detail-panel">
      <div class="panel-header">
        <h3 class="panel-title">工单详情：{{ selected.ticketNo }}</h3>
        <div class="detail-actions">
          <el-select v-model="selected.status" size="small" style="width: 120px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
          <el-button size="small" type="primary" :icon="Check" @click="saveStatus">保存</el-button>
        </div>
      </div>
      <div class="panel-body ticket-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ selected.orderNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="会话号">{{ selected.sessionNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="优先级"><StatusTag :value="selected.priority" /></el-descriptions-item>
          <el-descriptions-item label="状态"><StatusTag :value="selected.status" /></el-descriptions-item>
          <el-descriptions-item label="用户问题" :span="2">{{ selected.customerIssue }}</el-descriptions-item>
          <el-descriptions-item label="AI 摘要" :span="2">{{ selected.aiSummary || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理建议" :span="2">{{ selected.suggestedAction || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </section>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Refresh, Search } from '@element-plus/icons-vue'
import StatusTag from '../components/common/StatusTag.vue'
import { pageTickets, updateTicket } from '../api/serviceTicketApi'

const query = reactive({ page: 1, pageSize: 10, keyword: '', status: '', priority: '' })
const tickets = ref([])
const total = ref(0)
const loading = ref(false)
const selected = ref(null)

async function loadTickets() {
  loading.value = true
  try {
    const data = await pageTickets(query)
    tickets.value = data?.rows || []
    total.value = data?.total || 0
    if (!selected.value && tickets.value.length) {
      selected.value = { ...tickets.value[0] }
    }
  } finally {
    loading.value = false
  }
}

function selectTicket(row) {
  selected.value = { ...row }
}

async function saveStatus() {
  await updateTicket(selected.value.id, selected.value)
  ElMessage.success('工单状态已更新')
  await loadTickets()
}

onMounted(loadTickets)
</script>

<style scoped>
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.detail-panel {
  margin-top: 14px;
}

.detail-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ticket-detail {
  line-height: 1.7;
}
</style>
