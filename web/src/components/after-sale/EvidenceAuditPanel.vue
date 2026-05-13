<template>
  <div class="audit-panel" :class="{ compact }">
    <div class="audit-head">
      <div>
        <span class="eyebrow">凭证真实性审核</span>
        <h4>{{ audit?.auditNo || '暂无审核结果' }}</h4>
        <p>{{ summaryText }}</p>
      </div>
      <div v-if="audit" class="audit-tags">
        <StatusTag :value="audit.auditStatus" />
        <StatusTag :value="audit.sufficiencyLevel" />
      </div>
    </div>

    <template v-if="audit">
      <div class="risk-grid">
        <div>
          <span>真实性风险</span>
          <StatusTag :value="audit.authenticityRisk" />
        </div>
        <div>
          <span>AI 生成风险</span>
          <StatusTag :value="audit.aiGeneratedRisk" />
        </div>
        <div>
          <span>篡改风险</span>
          <StatusTag :value="audit.tamperRisk" />
        </div>
      </div>

      <div v-if="requiredList.length" class="mini-block">
        <strong>建议补充</strong>
        <div class="tag-list">
          <el-tag v-for="item in requiredList" :key="item" size="small" effect="plain">
            {{ item }}
          </el-tag>
        </div>
      </div>

      <div v-if="showSignals" class="signal-list">
        <div>
          <strong>元数据信号</strong>
          <p>{{ audit.metadataSignal || '-' }}</p>
        </div>
        <div>
          <strong>视觉信号</strong>
          <p>{{ audit.visualSignal || '-' }}</p>
        </div>
        <div>
          <strong>水印/来源信号</strong>
          <p>{{ audit.watermarkSignal || '-' }}</p>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import StatusTag from '../common/StatusTag.vue'

const props = defineProps({
  audit: {
    type: Object,
    default: null
  },
  compact: {
    type: Boolean,
    default: false
  },
  showSignals: {
    type: Boolean,
    default: true
  }
})

const requiredList = computed(() => Array.isArray(props.audit?.requiredEvidenceList)
  ? props.audit.requiredEvidenceList.filter(Boolean)
  : splitText(props.audit?.requiredEvidence))

const summaryText = computed(() => {
  if (!props.audit) {
    return '还没有对该凭证进行风险审核。'
  }
  return props.audit.aiSummary || '系统已根据凭证类型、内容描述和售后上下文生成风险信号。'
})

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
.audit-panel {
  display: grid;
  gap: 10px;
}

.audit-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid #fed7aa;
  border-radius: var(--radius);
  background: #fff7ed;
}

.audit-head h4 {
  margin: 0;
  font-size: 15px;
}

.audit-head p {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.eyebrow {
  display: block;
  margin-bottom: 4px;
  color: #c2410c;
  font-size: 12px;
  font-weight: 700;
}

.audit-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.risk-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.risk-grid > div,
.mini-block,
.signal-list > div {
  padding: 10px;
  border: 1px solid var(--line-soft);
  border-radius: 6px;
  background: #fff;
}

.risk-grid span,
.mini-block strong,
.signal-list strong {
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

.signal-list {
  display: grid;
  gap: 8px;
}

.signal-list p {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.compact .audit-head {
  padding: 10px;
}

@media (max-width: 760px) {
  .risk-grid {
    grid-template-columns: 1fr;
  }
}
</style>
