<template>
  <header class="header">
    <div>
      <h1>{{ title }}</h1>
      <p>Spring Boot + Vue 3 + MySQL + LangChain4j</p>
    </div>
    <div class="status-line">
      <el-button :icon="Refresh" circle @click="refresh" />
      <span class="status-item user-chip">
        {{ authStore.user?.displayName || authStore.user?.username }}
        <StatusTag :value="authStore.user?.role" />
      </span>
      <span class="status-item">
        数据库
        <StatusTag :value="systemStore.database.status" />
      </span>
      <span class="status-item">
        AI
        <StatusTag :value="systemStore.ai.status" />
      </span>
      <el-select
        v-model="selectedModel"
        class="model-select"
        size="small"
        filterable
        allow-create
        default-first-option
        :reserve-keyword="false"
        :loading="systemStore.modelSwitching"
        @change="changeModel"
      >
        <el-option
          v-for="model in systemStore.modelOptions"
          :key="model"
          :label="model"
          :value="model"
        />
      </el-select>
      <el-button size="small" @click="logout">退出</el-button>
    </div>
  </header>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import StatusTag from '../common/StatusTag.vue'
import { useAuthStore } from '../../stores/authStore'
import { useSystemStore } from '../../stores/systemStore'

const route = useRoute()
const router = useRouter()
const systemStore = useSystemStore()
const authStore = useAuthStore()
const selectedModel = ref('')
const title = computed(() => route.meta.title || '电商退换货智能客服系统')
const refresh = () => systemStore.loadStatus().catch(() => {})

watch(
  () => systemStore.selectedModelName,
  value => {
    selectedModel.value = value || ''
  },
  { immediate: true }
)

async function changeModel(modelName) {
  if (!modelName) return
  try {
    await systemStore.changeModel(modelName)
    ElMessage.success(`已切换模型：${modelName}`)
  } catch (error) {
    selectedModel.value = systemStore.selectedModelName
  }
}

async function logout() {
  await authStore.logout()
  await router.replace('/login')
}
</script>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  min-height: var(--header-height);
  padding: 8px 18px;
  border-bottom: 1px solid var(--line-soft);
  background: rgb(255 255 255 / 92%);
  backdrop-filter: blur(10px);
}

h1 {
  margin: 0;
  font-size: 18px;
  line-height: 1.25;
}

p {
  margin: 2px 0 0;
  color: var(--text-muted);
  font-size: 12px;
}

.status-line {
  display: flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
}

.status-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-muted);
  font-size: 13px;
}

.model-select {
  width: 152px;
}

@media (max-width: 720px) {
  .header {
    align-items: flex-start;
    flex-direction: column;
  }

  .status-line {
    flex-wrap: wrap;
  }
}
</style>
