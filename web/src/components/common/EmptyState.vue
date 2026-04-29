<template>
  <div class="empty-state">
    <el-icon :size="34"><Box /></el-icon>
    <div>{{ text }}</div>
  </div>
</template>

<script setup>
import { Box } from '@element-plus/icons-vue'

defineProps({
  text: {
    type: String,
    default: '暂无数据'
  }
})
</script>

<style scoped>
.empty-state {
  display: grid;
  place-items: center;
  min-height: 140px;
  gap: 8px;
  color: var(--text-muted);
  text-align: center;
}
</style>
