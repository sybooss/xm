<template>
  <div class="risk-panel" :class="{ compact }">
    <div class="risk-head">
      <div>
        <span class="eyebrow">售后风险识别</span>
        <h4>{{ assessment?.assessmentNo || '尚未评估' }}</h4>
        <p>{{ summaryText }}</p>
      </div>
      <div v-if="assessment" class="risk-score">
        <StatusTag :value="assessment.riskLevel" />
        <strong>{{ assessment.riskScore ?? 0 }}</strong>
      </div>
    </div>

    <template v-if="assessment">
      <div v-if="tagList.length" class="tag-list">
        <el-tag v-for="tag in tagList" :key="tag" size="small" effect="plain">
          {{ tag }}
        </el-tag>
      </div>

      <div class="reason-list">
        <div>
          <strong>风险原因</strong>
          <p>{{ assessment.riskReasons || '-' }}</p>
        </div>
        <div>
          <strong>建议动作</strong>
          <p>{{ assessment.suggestedAction || '-' }}</p>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import StatusTag from '../common/StatusTag.vue'

const props = defineProps({
  assessment: {
    type: Object,
    default: null
  },
  compact: {
    type: Boolean,
    default: false
  }
})

const tagList = computed(() => Array.isArray(props.assessment?.riskTagList)
  ? props.assessment.riskTagList.filter(Boolean)
  : splitText(props.assessment?.riskTags))

const summaryText = computed(() => {
  if (!props.assessment) {
    return '还没有生成风险评估，管理员可结合凭证审核结果重新评估。'
  }
  return props.assessment.aiSummary || '系统已根据用户历史、凭证审核、SLA 和订单金额生成风险信号。'
})

function splitText(value) {
  if (!value) {
    return []
  }
  return String(value)
    .split(/[,，；;\n]/)
    .map(item => item.trim())
    .filter(Boolean)
}
</script>

<style scoped>
.risk-panel {
  display: grid;
  gap: 10px;
}

.risk-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid #fecaca;
  border-radius: var(--radius);
  background: #fff1f2;
}

.risk-head h4 {
  margin: 0;
  font-size: 15px;
}

.risk-head p {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.eyebrow {
  display: block;
  margin-bottom: 4px;
  color: #be123c;
  font-size: 12px;
  font-weight: 700;
}

.risk-score {
  display: grid;
  justify-items: end;
  gap: 6px;
}

.risk-score strong {
  color: #be123c;
  font-size: 24px;
  line-height: 1;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.reason-list {
  display: grid;
  gap: 8px;
}

.reason-list > div {
  padding: 10px;
  border: 1px solid var(--line-soft);
  border-radius: 6px;
  background: #fff;
}

.reason-list strong {
  display: block;
  margin-bottom: 6px;
  color: var(--text);
  font-size: 13px;
}

.reason-list p {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.compact .risk-head {
  padding: 10px;
}
</style>
