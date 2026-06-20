import api from './axios'

export const getDashboardAnalytics = async () => {
  const { data } = await api.get('/api/admin/analytics/dashboard')
  return data
}
