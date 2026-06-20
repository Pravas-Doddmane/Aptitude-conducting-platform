import api, { setTokens, clearTokens } from './axios'

export const loginAdmin = async (email, password) => {
  const { data } = await api.post('/api/auth/login', { email, password })
  setTokens(data.accessToken, data.refreshToken)
  return data
}

export const refreshAuth = async () => {
  const { data } = await api.post('/api/auth/refresh', {
    refreshToken: localStorage.getItem('refreshToken'),
  })
  setTokens(data.accessToken, data.refreshToken)
  return data
}

export const logoutUser = async () => {
  try {
    await api.post('/api/auth/logout', {
      refreshToken: localStorage.getItem('refreshToken'),
    })
  } finally {
    clearTokens()
  }
}