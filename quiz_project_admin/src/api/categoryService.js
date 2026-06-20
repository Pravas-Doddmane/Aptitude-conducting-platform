import api from './axios'

export const getCategories = async (params) => {
  const { data } = await api.get('/api/admin/categories', { params })
  return data
}

export const createCategory = async (payload) => {
  const { data } = await api.post('/api/admin/categories', payload)
  return data
}

export const updateCategory = async (id, payload) => {
  const { data } = await api.put(`/api/admin/categories/${id}`, payload)
  return data
}

export const deleteCategory = async (id) => {
  const { data } = await api.delete(`/api/admin/categories/${id}`)
  return data
}