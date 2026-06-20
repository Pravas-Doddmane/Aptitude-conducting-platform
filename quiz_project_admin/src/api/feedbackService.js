import api from './axios'

export const getFeedbacks = async (params) => {
  const { data } = await api.get('/api/admin/feedback', { params })
  return data
}

export const updateFeedbackStatus = async (id, payload) => {
  const { data } = await api.patch(`/api/admin/feedback/${id}/status`, payload)
  return data
}
