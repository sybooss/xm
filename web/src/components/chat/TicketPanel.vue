<template>
  <div class="ticket-panel">
    <template v-if="ticket?.needed || ticket?.id">
      <div class="ticket-head">
        <strong>{{ ticket.ticketNo || '人工工单' }}</strong>
        <div class="ticket-tags">
          <StatusTag :value="ticket.priority" />
          <StatusTag :value="ticket.status" />
        </div>
      </div>
      <p class="ticket-text">{{ ticket.reason || '已进入人工客服处理队列' }}</p>
      <p v-if="ticket.suggestedAction" class="ticket-action">{{ ticket.suggestedAction }}</p>
    </template>
    <template v-else>
      <p class="muted">当前回复未触发人工转接。</p>
    </template>

    <el-button class="handoff-button" size="small" :icon="Service" :loading="creating" :disabled="!sessionId" @click="manualCreate">
      转人工
    </el-button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Service } from '@element-plus/icons-vue'
import StatusTag from '../common/StatusTag.vue'
import { createSessionTicket } from '../../api/serviceTicketApi'

const props = defineProps({
  ticket: {
    type: Object,
    default: () => ({})
  },
  sessionId: {
    type: [Number, String],
    default: ''
  },
  orderNo: {
    type: String,
    default: ''
  },
  lastQuestion: {
    type: String,
    default: ''
  },
  intentCode: {
    type: String,
    default: 'COMPLAINT_TRANSFER'
  }
})

const emit = defineEmits(['created'])
const creating = ref(false)

async function manualCreate() {
  if (!props.sessionId) {
    ElMessage.warning('请先选择或创建会话')
    return
  }
  creating.value = true
  try {
    const data = await createSessionTicket(props.sessionId, {
      orderNo: props.orderNo || undefined,
      intentCode: props.intentCode || 'COMPLAINT_TRANSFER',
      priority: 'HIGH',
      status: 'PENDING',
      customerIssue: props.lastQuestion || '用户请求人工客服处理',
      aiSummary: '客服工作台手动转人工，请人工客服接续当前会话。',
      suggestedAction: '核实订单和对话记录，联系用户确认诉求并记录处理结果。'
    })
    ElMessage.success(`已创建工单 ${data.ticketNo}`)
    emit('created', {
      created: true,
      needed: true,
      id: data.id,
      ticketNo: data.ticketNo,
      priority: data.priority,
      status: data.status,
      orderNo: data.orderNo,
      suggestedAction: data.suggestedAction,
      reason: '已手动创建人工客服工单'
    })
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.ticket-panel {
  display: grid;
  gap: 8px;
}

.ticket-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.ticket-tags {
  display: flex;
  gap: 6px;
}

.ticket-text,
.ticket-action {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.ticket-action {
  color: var(--text);
}

.handoff-button {
  justify-self: start;
}

.muted {
  margin: 0;
  color: var(--text-muted);
}
</style>
