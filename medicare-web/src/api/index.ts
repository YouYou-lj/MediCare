import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { useUserStore } from '../stores/user'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true,
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        useUserStore().clearUser()
        router.push('/login')
      }
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  (error) => {
    const message = getRequestErrorMessage(error)
    if (error.response?.status === 401) {
      useUserStore().clearUser()
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(new Error(message))
  }
)

function getRequestErrorMessage(error: any) {
  if (typeof error.response?.data === 'string' && error.response.data.includes('Invalid CORS request')) {
    return '跨域请求被后端拒绝，请确认后端 CORS 已允许当前前端地址'
  }
  if (error.response?.data?.message) {
    return error.response.data.message
  }
  if (error.response?.status === 403) {
    const url = error.config?.url ? `：${error.config.url}` : ''
    return `权限不足，当前账号无法执行该操作${url}`
  }
  if (error.response?.status === 500) {
    return '后端服务异常，请确认已启动最新后端并加载 application-secret.yml'
  }
  if (error.code === 'ERR_NETWORK' || error.message === 'Network Error') {
    return '后端服务未启动或网络连接失败'
  }
  return error.message || '网络错误'
}

export default request
