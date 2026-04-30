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
    SIGNED: '已签收',
    PAID: '已支付',
    SHIPPED: '已发货',
    AFTER_SALE: '售后中'
  }
  if (typeof props.value === 'boolean') {
    return props.value ? '是' : '否'
  }
  return labels[normalized.value] || props.value || '-'
})

const tagType = computed(() => {
  if (['UP', 'SUCCESS', 'AI_ENHANCED', 'ENABLED', 'RESOLVED'].includes(normalized.value)) return 'success'
  if (['FAILED', 'DISABLED', 'URGENT'].includes(normalized.value)) return 'danger'
  if (['SKIPPED', 'FALLBACK', 'CLOSED', 'LOW'].includes(normalized.value)) return 'info'
  if (['THINKING', 'AFTER_SALE', 'PENDING', 'PROCESSING', 'HIGH'].includes(normalized.value)) return 'warning'
  return 'primary'
})
</script>
