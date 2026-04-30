import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '../api/authApi'

const TOKEN_KEY = 'returns_assistant_token'
const USER_KEY = 'returns_assistant_user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref(loadUser())
  const loading = ref(false)

  const isLoggedIn = computed(() => Boolean(token.value))
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  async function login(username, password) {
    loading.value = true
    try {
      const data = await authApi.login({ username, password })
      token.value = data.token
      user.value = {
        userId: data.userId,
        username: data.username,
        displayName: data.displayName,
        role: data.role,
        expiresAt: data.expiresAt
      }
      localStorage.setItem(TOKEN_KEY, token.value)
      localStorage.setItem(USER_KEY, JSON.stringify(user.value))
      return data
    } finally {
      loading.value = false
    }
  }

  async function loadMe() {
    if (!token.value) {
      return null
    }
    const data = await authApi.getMe()
    user.value = {
      userId: data.userId,
      username: data.username,
      displayName: data.displayName,
      role: data.role,
      expiresAt: data.expiresAt
    }
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
    return user.value
  }

  async function logout() {
    try {
      if (token.value) {
        await authApi.logout()
      }
    } finally {
      clear()
    }
  }

  function clear() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return {
    token,
    user,
    loading,
    isLoggedIn,
    isAdmin,
    login,
    loadMe,
    logout,
    clear
  }
})

function loadUser() {
  try {
    const text = localStorage.getItem(USER_KEY)
    return text ? JSON.parse(text) : null
  } catch {
    return null
  }
}
