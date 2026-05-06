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
import { useAuthStore } from '../../stores/authStore'
import { useSystemStore } from '../../stores/systemStore'

const authStore = useAuthStore()
const systemStore = useSystemStore()

onMounted(() => {
  if (authStore.isAdmin) {
    systemStore.loadStatus().catch(() => {})
    systemStore.loadEnums().catch(() => {})
  }
})
</script>

<style scoped>
.app-shell {
  display: flex;
  min-height: 100vh;
  background:
    radial-gradient(circle at 62% 0%, rgb(0 102 204 / 9%), transparent 32%),
    radial-gradient(circle at 92% 48%, rgb(52 199 89 / 7%), transparent 25%),
    var(--app-bg);
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
