import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true,
})

const AUTH_FAILURE_CODES = new Set(['401', 'A0230', 'A0231', 'A0301'])
let refreshPromise = null

const saveLoginResult = (data) => {
  if (!data?.token) throw new Error('刷新登录状态失败')
  localStorage.setItem('token', data.token)
  localStorage.setItem('tokenExpiresAt', String(Date.now() + Number(data.expiresIn || 0)))
  if (data.userInfo) {
    localStorage.setItem('userInfo', JSON.stringify(data.userInfo))
  }
  return data.token
}

export const refreshAccessToken = () => {
  if (!refreshPromise) {
    // 刷新令牌只由浏览器自动携带的 HttpOnly Cookie 提供，不发送请求体。
    refreshPromise = axios.post('/api/user/refresh', undefined, { withCredentials: true })
      .then((response) => {
        if (String(response.data?.code) !== '200') {
          throw new Error(response.data?.msg || '登录已经过期')
        }
        return saveLoginResult(response.data.data)
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

const clearExpiredSession = (requestUrl) => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('tokenExpiresIn')
  localStorage.removeItem('tokenExpiresAt')
  if (!requestUrl?.includes('/login') && window.location.pathname !== '/auth/login') {
    window.location.href = '/auth/login'
  }
}

export const ensureFreshAccessToken = async () => {
  const token = localStorage.getItem('token')
  const expiresAt = Number(localStorage.getItem('tokenExpiresAt') || 0)
  if (!token || (expiresAt > 0 && expiresAt - Date.now() <= 60_000)) {
    return refreshAccessToken()
  }
  return token
}

service.interceptors.request.use(
  async (config) => {
    const isAuthEntry = config.url?.includes('/user/login')
      || config.url?.includes('/user/add')
      || config.url?.includes('/user/refresh')
    let token = localStorage.getItem('token')
    const expiresAt = Number(localStorage.getItem('tokenExpiresAt') || 0)
    if (!isAuthEntry && token && expiresAt > 0 && expiresAt - Date.now() <= 60_000) {
      token = await refreshAccessToken()
    }
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

service.interceptors.response.use(
  async (response) => {
    const { data, config } = response
    if (data.code === '200') {
      return data.data
    }

    if (AUTH_FAILURE_CODES.has(String(data.code))) {
      const originalRequest = config
      const canRefresh = originalRequest
        && !originalRequest._retry
        && !originalRequest.url?.includes('/user/login')
        && !originalRequest.url?.includes('/user/refresh')
      if (canRefresh) {
        originalRequest._retry = true
        try {
          const token = await refreshAccessToken()
          originalRequest.headers.Authorization = `Bearer ${token}`
          return service(originalRequest)
        } catch (refreshError) {
          ElMessage.error(refreshError?.message || '登录已过期，请重新登录')
          clearExpiredSession(originalRequest.url)
          return Promise.reject(refreshError)
        }
      }
      ElMessage.error(data.msg || '登录过期，请重新登录')
      clearExpiredSession(config.url)
      return Promise.reject(new Error(data.msg || '登录过期'))
    }

    ElMessage.error(data.msg || data.message || '请求失败')
    return Promise.reject(data)
  },
  async (error) => {
    const status = error.response?.status
    const payload = error.response?.data
    const code = String(payload?.code ?? '')
    if (status === 401 || AUTH_FAILURE_CODES.has(code)) {
      const originalRequest = error.config
      const canRefresh = originalRequest
        && !originalRequest._retry
        && !originalRequest.url?.includes('/user/login')
        && !originalRequest.url?.includes('/user/refresh')
      if (canRefresh) {
        originalRequest._retry = true
        try {
          const token = await refreshAccessToken()
          originalRequest.headers.Authorization = `Bearer ${token}`
          return service(originalRequest)
        } catch (refreshError) {
          const message = refreshError?.message || '登录过期，请重新登录'
          ElMessage.error(message)
          clearExpiredSession(originalRequest.url)
          return Promise.reject(refreshError)
        }
      }
      const message = payload?.msg || '登录过期，请重新登录'
      ElMessage.error(message)
      clearExpiredSession(error.config?.url)
      return Promise.reject(new Error(message))
    }
    ElMessage.error(payload?.msg || error.message || '网络请求失败')
    return Promise.reject(error)
  },
)

export default service
