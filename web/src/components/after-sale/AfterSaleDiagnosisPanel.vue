<template>
  <div class="diagnosis-panel" :class="{ compact }">
    <div v-if="diagnosis" class="diagnosis-head">
      <div>
        <span class="eyebrow">售后前置诊断</span>
        <h4>{{ diagnosis.diagnosisNo || '诊断结果' }}</h4>
        <p>{{ diagnosis.reasonSummary }}</p>
      </div>
      <div class="diagnosis-tags">
        <StatusTag :value="diagnosis.suggestedServiceType" />
        <StatusTag :value="diagnosis.decisionLevel" />
      </div>
    </div>
    <p v-else class="muted">暂无前置诊断</p>

    <template v-if="diagnosis">
      <div v-if="evidenceList.length" class="mini-block">
        <strong>建议补充凭证</strong>
        <div class="tag-list">
          <el-tag v-for="item in evidenceList" :key="item" size="small" effect="plain">
            {{ item }}
          </el-tag>
        </div>
      </div>

      <div v-if="optionList.length" class="option-list">
        <div v-for="option in optionList" :key="`${option.type}-${option.title}`" class="option-item">
          <div class="option-title">
            <StatusTag :value="option.type" />
            <strong>{{ option.title }}</strong>
          </div>
          <p>{{ option.detail }}</p>
          <small v-if="option.risk">风险提示：{{ option.risk }}</small>
        </div>
      </div>

      <div v-if="diagnosis.aiSummary" class="summary-box">
        {{ diagnosis.aiSummary }}
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import StatusTag from '../common/StatusTag.vue'

const props = defineProps({
  diagnosis: {
    type: Object,
    default: null
  },
  compact: {
    type: Boolean,
    default: false
  }
})

const evidenceList = computed(() => Array.isArray(props.diagnosis?.requiredEvidenceList)
  ? props.diagnosis.requiredEvidenceList.filter(Boolean)
  : splitText(props.diagnosis?.requiredEvidence))

const optionList = computed(() => Array.isArray(props.diagnosis?.solutionOptions)
  ? props.diagnosis.solutionOptions.filter(item => item?.title)
  : [])

function splitText(value) {
  if (!value) {
    return []
  }
  return String(value)
    .split(/[；;\n]/)
    .map(item => item.trim())
    .filter(Boolean)
}
</script>

<style scoped>
.diagnosis-panel {
  display: grid;
  gap: 10px;
}

.diagnosis-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid #bfdbfe;
  border-radius: var(--radius);
  background: #eff6ff;
}

.diagnosis-head h4 {
  margin: 0;
  font-size: 15px;
}

.diagnosis-head p {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.eyebrow {
  display: block;
  margin-bottom: 4px;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.diagnosis-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.mini-block {
  padding: 10px;
  border: 1px solid var(--line-soft);
  border-radius: 6px;
  background: #fff;
}

.mini-block strong {
  display: block;
  margin-bottom: 8px;
  color: var(--text);
  font-size: 13px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.option-list {
  display: grid;
  gap: 8px;
}

.option-item {
  padding: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.option-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.option-title strong {
  font-size: 13px;
}

.option-item p {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.option-item small {
  display: block;
  margin-top: 6px;
  color: #92400e;
}

.summary-box {
  padding: 10px;
  border: 1px dashed #93c5fd;
  border-radius: 6px;
  background: #f8fbff;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.compact .diagnosis-head {
  padding: 10px;
}
</style>
