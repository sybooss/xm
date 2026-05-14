<template>
  <div v-if="risk" class="image-risk-panel" :class="{ compact }">
    <div class="risk-head">
      <div>
        <span class="eyebrow">图片真实性预审</span>
        <strong>{{ risk.summary || '系统已完成聊天图片风险预审' }}</strong>
      </div>
      <div class="risk-tags">
        <StatusTag :value="risk.auditStatus" />
        <StatusTag :value="risk.aiGeneratedRisk" />
      </div>
    </div>

    <div class="risk-grid">
      <div>
        <span>真实性</span>
        <StatusTag :value="risk.authenticityRisk" />
      </div>
      <div>
        <span>AI 生成</span>
        <StatusTag :value="risk.aiGeneratedRisk" />
      </div>
      <div>
        <span>篡改</span>
        <StatusTag :value="risk.tamperRisk" />
      </div>
    </div>

    <div v-if="requiredList.length" class="risk-block">
      <span>建议补充</span>
      <div class="tag-list">
        <el-tag v-for="item in requiredList" :key="item" size="small" effect="plain">
          {{ item }}
        </el-tag>
      </div>
    </div>

    <div v-if="showSignals" class="signal-list">
      <p><strong>C2PA 内容凭证：</strong>{{ c2paLine }}</p>
      <p><strong>视觉模型：</strong>{{ visionLine }}</p>
      <p><strong>元数据：</strong>{{ risk.metadataSignal || '-' }}</p>
      <p><strong>视觉：</strong>{{ risk.visualSignal || '-' }}</p>
      <p><strong>来源：</strong>{{ risk.watermarkSignal || '-' }}</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import StatusTag from '../common/StatusTag.vue'

const props = defineProps({
  risk: {
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

const requiredList = computed(() => {
  if (Array.isArray(props.risk?.requiredEvidenceList) && props.risk.requiredEvidenceList.length) {
    return props.risk.requiredEvidenceList.filter(Boolean)
  }
  return splitText(props.risk?.requiredEvidence)
})

const visionLine = computed(() => {
  const status = props.risk?.visionStatus || 'SKIPPED'
  const model = props.risk?.visionModel ? ` · ${props.risk.visionModel}` : ''
  const signal = props.risk?.visionSignal ? ` · ${props.risk.visionSignal}` : ''
  return `${status}${model}${signal}`
})

const c2paLine = computed(() => {
  const status = props.risk?.c2paStatus || 'SKIPPED'
  const provider = props.risk?.c2paProvider ? ` · ${props.risk.c2paProvider}` : ''
  const generator = props.risk?.c2paGenerator ? ` · ${props.risk.c2paGenerator}` : ''
  const signal = props.risk?.c2paSignal ? ` · ${props.risk.c2paSignal}` : ''
  return `${status}${provider}${generator}${signal}`
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
.image-risk-panel {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid #fed7aa;
  border-radius: 6px;
  background: #fff7ed;
}

.risk-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.eyebrow {
  display: block;
  margin-bottom: 4px;
  color: #c2410c;
  font-size: 12px;
  font-weight: 700;
}

.risk-head strong {
  display: block;
  color: var(--text);
  font-size: 13px;
  line-height: 1.5;
}

.risk-tags,
.tag-list {
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
.risk-block {
  padding: 8px;
  border: 1px solid rgb(251 146 60 / 26%);
  border-radius: 6px;
  background: #fff;
}

.risk-grid span,
.risk-block span {
  display: block;
  margin-bottom: 6px;
  color: var(--text-muted);
  font-size: 12px;
}

.signal-list {
  display: grid;
  gap: 6px;
}

.signal-list p {
  margin: 0;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.55;
}

.signal-list strong {
  color: var(--text);
}

.compact {
  gap: 8px;
  padding: 10px;
}

@media (max-width: 760px) {
  .risk-head,
  .risk-grid {
    display: grid;
    grid-template-columns: 1fr;
  }

  .risk-tags {
    justify-content: flex-start;
  }
}
</style>
