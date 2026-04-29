import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getEnums, getSystemStatus, switchAiModel } from '../api/systemApi'

export const useSystemStore = defineStore('system', () => {
  const status = ref(null)
  const enums = ref({})
  const loading = ref(false)
  const modelSwitching = ref(false)

  const ai = computed(() => status.value?.ai || {})
  const database = computed(() => status.value?.database || {})
  const aiAvailable = computed(() => ai.value.status === 'UP')
  const modelOptions = computed(() => ai.value.modelOptions || [])
  const selectedModelName = computed(() => ai.value.selectedModelName || ai.value.modelName || '')

  async function loadStatus() {
    loading.value = true
    try {
      status.value = await getSystemStatus()
      return status.value
    } finally {
      loading.value = false
    }
  }

  async function loadEnums() {
    enums.value = await getEnums()
    return enums.value
  }

  async function changeModel(modelName) {
    if (!modelName || modelName === selectedModelName.value) {
      return ai.value
    }
    modelSwitching.value = true
    try {
      const data = await switchAiModel(modelName)
      status.value = {
        ...(status.value || {}),
        ai: data
      }
      return data
    } finally {
      modelSwitching.value = false
    }
  }

  return {
    status,
    enums,
    loading,
    modelSwitching,
    ai,
    database,
    aiAvailable,
    modelOptions,
    selectedModelName,
    loadStatus,
    loadEnums,
    changeModel
  }
})
