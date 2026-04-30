<template>
  <div v-if="currentTraces.length" class="flow-list">
    <div v-for="trace in currentTraces" :key="trace.id" class="flow-item">
      <div class="flow-line">
        <span class="flow-dot" :class="trace.stepStatus?.toLowerCase()" />
        <div class="flow-main">
          <div class="flow-title-row">
            <strong>{{ stepLabel(trace.stepName, trace.detail.title) }}</strong>
            <StatusTag :value="trace.stepStatus" />
          </div>
          <p class="flow-summary">{{ trace.detail.summary || defaultSummary(trace) }}</p>
          <div class="flow-meta">
            <span v-if="trace.detail.intentCode">{{ trace.detail.intentCode }}</span>
            <span v-if="trace.detail.modelName">{{ trace.detail.modelName }}</span>
            <span v-if="trace.detail.hitCount !== undefined">命中 {{ trace.detail.hitCount }} 条</span>
            <span v-if="trace.detail.latencyMs !== undefined">{{ trace.detail.latencyMs }} ms</span>
            <span v-if="trace.detail.orderNo">{{ trace.detail.orderNo }}</span>
            <span v-if="trace.detail.ticketNo">{{ trace.detail.ticketNo }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
  <p v-else class="muted">暂无轨迹</p>
</template>

<script setup>
import { computed } from 'vue'
import StatusTag from '../common/StatusTag.vue'

const props = defineProps({
  traces: {
    type: Array,
    default: () => []
  }
})

const parsedTraces = computed(() =>
  props.traces.map(item => ({
    ...item,
    detail: parseDetail(item.detailJson)
  }))
)

const currentTraces = computed(() => {
  if (!parsedTraces.value.length) {
    return []
  }
  const maxMessageId = Math.max(...parsedTraces.value.map(item => Number(item.messageId) || 0))
  return parsedTraces.value.filter(item => Number(item.messageId) === maxMessageId)
})

function parseDetail(value) {
  if (!value) {
    return {}
  }
  if (typeof value === 'object') {
    return value
  }
  try {
    return JSON.parse(value)
  } catch {
    return { summary: value }
  }
}

function stepLabel(stepName, title) {
  if (title) {
    return title
  }
  const labels = {
    CONTEXT_RESOLVE: '多轮上下文解析',
    INTENT_RECOGNIZE: '意图识别',
    ORDER_CONTEXT: '订单上下文',
    KNOWLEDGE_RETRIEVAL: '知识库检索',
    BUSINESS_TOOL_CALLS: '业务工具调用',
    AI_GENERATION: 'AI 回答生成',
    HUMAN_TICKET_CHECK: '人工工单判定',
    TICKET_CREATED: '创建人工工单',
    FINAL_REPLY: '最终回复'
  }
  return labels[stepName] || stepName
}

function defaultSummary(trace) {
  if (trace.stepName === 'CONTEXT_RESOLVE') {
    return trace.stepStatus === 'SUCCESS' ? '检测到追问，已承接上一轮意图' : '本轮按独立问题处理'
  }
  if (trace.stepName === 'KNOWLEDGE_RETRIEVAL') {
    return trace.stepStatus === 'SUCCESS' ? '已找到可引用的知识库依据' : '未命中精确规则'
  }
  if (trace.stepName === 'TICKET_CREATED') {
    return trace.stepStatus === 'SUCCESS' ? '已生成待处理人工工单' : '无需新建工单'
  }
  return '流程节点已完成'
}
</script>

<style scoped>
.flow-list {
  display: grid;
  gap: 2px;
}

.flow-item {
  padding: 8px 0;
  border-bottom: 1px solid var(--line-soft);
}

.flow-item:last-child {
  border-bottom: 0;
}

.flow-line {
  display: grid;
  grid-template-columns: 16px 1fr;
  gap: 8px;
}

.flow-dot {
  width: 10px;
  height: 10px;
  margin-top: 6px;
  border-radius: 50%;
  background: var(--info);
}

.flow-dot.success {
  background: var(--success);
}

.flow-dot.failed {
  background: var(--danger);
}

.flow-dot.skipped {
  background: var(--text-muted);
}

.flow-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.flow-title-row strong {
  font-size: 13px;
}

.flow-summary {
  margin: 5px 0 0;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.55;
}

.flow-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.flow-meta span {
  padding: 2px 6px;
  border: 1px solid var(--line-soft);
  border-radius: 6px;
  background: var(--surface-soft);
  color: var(--text-muted);
  font-size: 12px;
}

.muted {
  color: var(--text-muted);
}
</style>
