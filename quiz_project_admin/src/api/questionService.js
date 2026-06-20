import api from './axios'

export const getQuestions = async (params) => {
  const { data } = await api.get('/api/admin/questions', { params })
  return data
}

export const getQuestion = async (id) => {
  const { data } = await api.get(`/api/admin/questions/${id}`)
  return data
}

export const createQuestion = async (payload) => {
  const { data } = await api.post('/api/admin/questions', payload)
  return data
}

export const updateQuestion = async (id, payload) => {
  const { data } = await api.put(`/api/admin/questions/${id}`, payload)
  return data
}

export const deleteQuestion = async (id) => {
  const { data } = await api.delete(`/api/admin/questions/${id}`)
  return data
}