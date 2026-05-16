<template>
  <div class="product-insight">
    <div v-if="hasInsight" class="product-head">
      <div>
        <span class="eyebrow">产品智能顾问</span>
        <h4>{{ insight.productName || '未识别商品' }}</h4>
        <p>{{ insight.positioning || '暂无商品定位' }}</p>
      </div>
      <el-tag :type="insight.hasProfile ? 'success' : 'info'" effect="plain">
        {{ insight.hasProfile ? '商品档案' : '品类兜底' }}
      </el-tag>
    </div>
    <p v-else class="muted">绑定订单并发送商品相关问题后显示产品洞察</p>

    <template v-if="hasInsight">
      <div v-if="concernTags.length" class="tag-row">
        <el-tag v-for="tag in concernTags" :key="tag.value" size="small" type="warning" effect="plain">
          {{ tag.label }}
        </el-tag>
      </div>

      <el-descriptions v-if="specEntries.length" :column="1" size="small" border class="specs">
        <el-descriptions-item v-for="item in specEntries" :key="item.key" :label="item.label">
          {{ item.value }}
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="topIssues.length" class="mini-block">
        <strong>常见原因</strong>
        <ul>
          <li v-for="item in topIssues" :key="item">{{ item }}</li>
        </ul>
      </div>

      <div v-if="topSteps.length" class="mini-block">
        <strong>排查建议</strong>
        <ol>
          <li v-for="item in topSteps" :key="item">{{ item }}</li>
        </ol>
      </div>

      <div v-if="insight.comparisonText" class="compare-box">
        <strong>同类对比</strong>
        <p>{{ insight.comparisonText }}</p>
      </div>

      <div v-if="insight.afterSaleAdvice" class="advice-box">
        <strong>售后建议</strong>
        <p>{{ insight.afterSaleAdvice }}</p>
      </div>

      <p v-if="insight.aiSummary" class="ai-summary">{{ insight.aiSummary }}</p>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  insight: {
    type: Object,
    default: () => ({})
  }
})

const concernLabels = {
  NOISE_CONTROL: '降噪体验',
  CONNECTION: '连接稳定',
  BATTERY: '续航电量',
  CHARGING: '充电异常',
  SINGLE_SIDE_AUDIO: '单耳无声',
  SOUND_QUALITY: '音质问题',
  KEY_FAILURE: '按键异常',
  LOCATION: '定位偏差',
  SCREEN: '屏幕触控',
  CAPACITY: '容量体验',
  GENERAL_PRODUCT_USAGE: '使用体验'
}

const specLabels = {
  bluetooth: '蓝牙',
  batteryLife: '续航',
  noiseControl: '降噪',
  waterResistance: '防护',
  charging: '充电',
  screen: '屏幕',
  location: '定位',
  health: '健康功能',
  switch: '轴体',
  layout: '键位',
  connection: '连接',
  lighting: '灯光',
  keycap: '键帽',
  capacity: '容量',
  input: '输入',
  output: '输出',
  power: '功率',
  protection: '保护'
}

const hasInsight = computed(() => Boolean(props.insight?.productName || props.insight?.localSummary))

const concernTags = computed(() => {
  const concerns = Array.isArray(props.insight?.matchedConcerns) ? props.insight.matchedConcerns : []
  return concerns.map(value => ({
    value,
    label: concernLabels[value] || value
  }))
})

const specEntries = computed(() => {
  const specs = props.insight?.specs || {}
  return Object.entries(specs)
    .filter(([, value]) => value !== null && value !== undefined && String(value).trim() !== '')
    .slice(0, 5)
    .map(([key, value]) => ({
      key,
      label: specLabels[key] || key,
      value
    }))
})

const topIssues = computed(() => takeList(props.insight?.commonIssues, 3))
const topSteps = computed(() => takeList(props.insight?.troubleshootingSteps, 4))

function takeList(value, limit) {
  if (!Array.isArray(value)) {
    return []
  }
  return value.filter(Boolean).slice(0, limit)
}
</script>

<style scoped>
.product-insight {
  display: grid;
  gap: 10px;
}

.product-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
  padding: 12px;
  border: 1px solid rgb(16 185 129 / 20%);
  border-radius: var(--radius);
  background: #f0fdf4;
}

.product-head > div {
  min-width: 0;
}

.product-head h4 {
  margin: 0;
  overflow-wrap: anywhere;
}

.product-head p,
.compare-box p,
.advice-box p,
.ai-summary {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.eyebrow {
  display: block;
  margin-bottom: 4px;
  color: #047857;
  font-size: 12px;
  font-weight: 700;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.specs {
  width: 100%;
}

.mini-block {
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--line-soft);
  border-radius: 6px;
  background: #fff;
}

.mini-block strong,
.compare-box strong,
.advice-box strong {
  display: block;
  color: var(--text);
  font-size: 13px;
}

.mini-block ul,
.mini-block ol {
  display: grid;
  gap: 6px;
  margin: 8px 0 0;
  padding-left: 18px;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.compare-box,
.advice-box {
  min-width: 0;
  padding: 10px;
  border-radius: 6px;
}

.compare-box {
  border: 1px solid #e0e7ff;
  background: #f8faff;
}

.advice-box {
  border: 1px solid #fde68a;
  background: #fffbeb;
}

.ai-summary {
  padding: 10px;
  border: 1px dashed rgb(16 185 129 / 35%);
  border-radius: 6px;
  background: #f7fefb;
}
</style>
