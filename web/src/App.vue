<template>
  <router-view v-if="route.meta.public" />
  <AppLayout v-else />
</template>

<script setup>
import { useRoute } from 'vue-router'
import AppLayout from './components/layout/AppLayout.vue'

const route = useRoute()
</script>
