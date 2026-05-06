<template>
  <el-tag class="status-tag" :type="tagType" effect="light" round>
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
    REFUNDED: '已退款',
    PENDING_PAY: '待支付',
    COMPLETED: '已完成',
    SIGNED: '已签收',
    PAID: '已支付',
    SHIPPED: '已发货',
    AFTER_SALE: '售后中',
    NOT_SHIPPED: '未发货',
    IN_TRANSIT: '运输中',
    DELIVERED: '已签收',
    ABNORMAL: '异常',
    NONE: '无售后',
    RETURN_APPLYING: '退货中',
    RETURNING: '退货寄回中',
    EXCHANGE_APPLYING: '换货中',
    REFUNDING: '退款中',
    FINISHED: '已完成',
    REJECTED: '已拒绝',
    APPLIED: '已提交',
    SUBMITTED: '已提交',
    UNDER_REVIEW: '审核中',
    NEED_MORE_EVIDENCE: '待补材料',
    APPROVED: '审核通过',
    WAIT_BUYER_SEND: '待买家寄回',
    WAIT_SELLER_RECEIVE: '待商家收货',
    WAIT_SELLER_CONFIRM: '待商家确认',
    EXCHANGING: '换货中',
    RETURN: '退货退款',
    EXCHANGE: '换货',
    REFUND: '仅退款',
    COMPLAINT: '投诉',
    SUBMIT: '提交申请',
    APPROVE: '审核通过',
    REJECT: '审核驳回',
    REQUEST_MORE_EVIDENCE: '要求补材料',
    SUPPLEMENT_EVIDENCE: '补充凭证',
    已超时: '已超时',
    即将超时: '即将超时',
    高优先级: '高优先级',
    待顾客补材料: '待顾客补材料',
    正常跟进: '正常跟进',
    未设置SLA: '未设置 SLA',
    CANCEL: '取消',
    CONFIRM: '确认完成',
    SYSTEM_MARK: '系统标记',
    '已验证': '已验证'
  }
  if (typeof props.value === 'boolean') {
    return props.value ? '是' : '否'
  }
  return labels[normalized.value] || props.value || '-'
})

const tagType = computed(() => {
  if (['UP', 'SUCCESS', 'AI_ENHANCED', 'ENABLED', 'RESOLVED', 'ADMIN', 'SIGNED', 'PAID', 'DELIVERED', 'FINISHED', 'APPROVED', 'COMPLETED', 'APPROVE', 'CONFIRM', '已验证'].includes(normalized.value)) return 'success'
  if (['FAILED', 'DISABLED', 'URGENT', 'ABNORMAL', 'REJECTED', '已超时'].includes(normalized.value)) return 'danger'
  if (['SKIPPED', 'FALLBACK', 'CLOSED', 'LOW', 'NONE', 'NOT_SHIPPED', 'UNPAID'].includes(normalized.value)) return 'info'
  if (['THINKING', 'AFTER_SALE', 'PENDING', 'PROCESSING', 'HIGH', 'IN_TRANSIT', 'RETURN_APPLYING', 'RETURNING', 'EXCHANGE_APPLYING', 'REFUNDING', 'APPLIED', 'SUBMITTED', 'UNDER_REVIEW', 'NEED_MORE_EVIDENCE', 'WAIT_BUYER_SEND', 'WAIT_SELLER_RECEIVE', 'WAIT_SELLER_CONFIRM', 'EXCHANGING', 'PENDING_PAY', 'SUBMIT', 'REQUEST_MORE_EVIDENCE', 'SUPPLEMENT_EVIDENCE', 'SYSTEM_MARK'].includes(normalized.value)) return 'warning'
  return 'primary'
})
</script>
