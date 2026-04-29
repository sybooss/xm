import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 45000
})

request.interceptors.response.use(
  response => {
    const result = response.data
    if (!result || typeof result.code === 'undefined') {
      return result
    }
    if (result.code !== 1) {
      ElMessage.error(result.msg || '请求失败')
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
