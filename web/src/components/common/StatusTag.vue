<template>
  <el-tag :type="tagType" effect="light" round>
    {{ label }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: {
    type: [String, Boolean],
    default: ''
  }
})

const normalized = computed(() => String(props.value ?? '').toUpperCase())

const label = computed(() => {
  const labels = {
    UP: '可用',
    SUCCESS: '成功',
    FAILED: '失败',
    SKIPPED: '跳过',
    THINKING: '思考中',
    AI_ENHANCED: 'AI 增强',
    FALLBACK: '本地兜底',
    ACTIVE: '进行中',
    CLOSED: '已关闭',
    ENABLED: '启用',
    DISABLED: '停用',
    PENDING: '待处理',
    PROCESSING: '处理中',
    RESOLVED: '已解决',
    NORMAL: '普通',
    LOW: '低',
    HIGH: '高',
    URGENT: '紧急',
    ADMIN: '管理员',
    CUSTOMER: '客户',
    UNPAID: '未支付',
    REFUNDING: '退款中',
    REFUNDED: '已退款',
    PENDING_PAY: '待支付',
    SIGNED: '已签收',
    PAID: '已支付',
    SHIPPED: '已发货',
    COMPLETED: '已完成',
    NOT_SHIPPED: '未发货',
    IN_TRANSIT: '运输中',
    DELIVERED: '已送达',
    ABNORMAL: '异常',
    NONE: '无售后',
    RETURN_APPLYING: '退货申请中',
    RETURNING: '退货中',
    EXCHANGE_APPLYING: '换货申请中',
    FINISHED: '已完成',
    REJECTED: '已拒绝',
    RETURN: '退货',
    EXCHANGE: '换货',
    REFUND: '退款',
    COMPLAINT: '投诉',
    APPLIED: '已申请',
    APPROVED: '已批准',
    WAIT_BUYER_SEND: '待买家寄回',
    WAIT_SELLER_CONFIRM: '待商家确认',
    AFTER_SALE: '售后中'
  }
  if (typeof props.value === 'boolean') {
    return props.value ? '是' : '否'
  }
  return labels[normalized.value] || props.value || '-'
})

const tagType = computed(() => {
  if (['UP', 'SUCCESS', 'AI_ENHANCED', 'ENABLED', 'RESOLVED', 'ADMIN', 'PAID', 'SIGNED', 'COMPLETED', 'DELIVERED', 'FINISHED', 'APPROVED'].includes(normalized.value)) return 'success'
  if (['FAILED', 'DISABLED', 'URGENT', 'ABNORMAL', 'REJECTED', 'COMPLAINT'].includes(normalized.value)) return 'danger'
  if (['SKIPPED', 'FALLBACK', 'CLOSED', 'LOW', 'NONE', 'UNPAID', 'NOT_SHIPPED'].includes(normalized.value)) return 'info'
  if (['THINKING', 'AFTER_SALE', 'PENDING', 'PROCESSING', 'HIGH', 'PENDING_PAY', 'SHIPPED', 'IN_TRANSIT', 'REFUNDING', 'RETURN_APPLYING', 'RETURNING', 'EXCHANGE_APPLYING', 'APPLIED', 'WAIT_BUYER_SEND', 'WAIT_SELLER_CONFIRM'].includes(normalized.value)) return 'warning'
  return 'primary'
})
</script>
