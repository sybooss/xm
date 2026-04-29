<template>
  <div class="app-shell">
    <AppSidebar />
    <main class="main">
      <AppHeader />
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import AppHeader from './AppHeader.vue'
import AppSidebar from './AppSidebar.vue'
import { useSystemStore } from '../../stores/systemStore'

const systemStore = useSystemStore()

onMounted(() => {
  systemStore.loadStatus().catch(() => {})
  systemStore.loadEnums().catch(() => {})
})
</script>

<style scoped>
.app-shell {
  display: flex;
  min-height: 100vh;
  background: var(--app-bg);
}

.main {
  min-width: 0;
  flex: 1;
  margin-left: var(--sidebar-width);
}

@media (max-width: 900px) {
  .app-shell {
    display: block;
  }

  .main {
    margin-left: 0;
  }
}
</style>
