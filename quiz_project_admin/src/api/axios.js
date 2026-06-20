import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

// Store tokens in memory (access) and localStorage (refresh)
let accessToken = null
let refreshToken = localStorage.getItem('refreshToken') || null

export const setTokens = (access, refresh) => {
  accessToken = access
  refreshToken = refresh
  if (refresh) localStorage.setItem('refreshToken', refresh)
  else localStorage.removeItem('refreshToken')
}

export const clearTokens = () => {
  accessToken = null
  refreshToken = null
  localStorage.removeItem('refreshToken')
}

// Request interceptor to attach token
api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

// Response interceptor for token refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    if (error.response?.status === 401 && !originalRequest._retry && refreshToken) {
      originalRequest._retry = true
      try {
        const res = await axios.post(`${API_BASE_URL}/api/auth/refresh`, {
          refreshToken,
        })
        const { accessToken: newAccess, refreshToken: newRefresh } = res.data
        setTokens(newAccess, newRefresh)
        originalRequest.headers.Authorization = `Bearer ${newAccess}`
        return api(originalRequest)
      } catch (refreshError) {
        clearTokens()
        // Force logout
        window.location.href = '/login'
        return Promise.reject(refreshError)
      }
    }
    return Promise.reject(error)
  }
)

export { accessToken, refreshToken }
export default api