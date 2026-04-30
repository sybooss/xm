import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 45000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('returns_assistant_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  response => {
    const result = response.data
    if (!result || typeof result.code === 'undefined') {
      return result
    }
    if (result.code !== 1) {
      ElMessage.error(result.msg || '请求失败')
      if (result.msg?.includes('登录')) {
        localStorage.removeItem('returns_assistant_token')
        localStorage.removeItem('returns_assistant_user')
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      }
      return Promise.reject(result)
    }
    return result.data
  },
  error => {
    const message = error?.response?.data?.msg || error.message || '网络异常'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
