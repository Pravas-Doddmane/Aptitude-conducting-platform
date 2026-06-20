import api from './axios'

export const getUsers = async (params) => {
  const { data } = await api.get('/api/admin/users', { params })
  return data
}

export const updateUserStatus = async (userId, status) => {
  const { data } = await api.patch('/api/admin/users/status', { userId, status })
  return data
}

export const blockUser = async (userId) => {
  const { data } = await api.post(`/api/admin/users/${userId}/block`)
  return data
}

export const unblockUser = async (userId) => {
  const { data } = await api.post(`/api/admin/users/${userId}/unblock`)
  return data
}
