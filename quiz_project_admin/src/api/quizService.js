import api from './axios'

export const getQuizzes = async (params) => {
  const { data } = await api.get('/api/admin/quizzes', { params })
  return data
}

export const getQuiz = async (id) => {
  const { data } = await api.get(`/api/admin/quizzes/${id}`)
  return data
}

export const createQuiz = async (payload) => {
  const { data } = await api.post('/api/admin/quizzes', payload)
  return data
}

export const updateQuiz = async (id, payload) => {
  const { data } = await api.put(`/api/admin/quizzes/${id}`, payload)
  return data
}

export const deleteQuiz = async (id) => {
  const { data } = await api.delete(`/api/admin/quizzes/${id}`)
  return data
}

export const publishQuiz = async (id) => {
  const { data } = await api.post(`/api/admin/quizzes/${id}/publish`)
  return data
}

export const unpublishQuiz = async (id) => {
  const { data } = await api.post(`/api/admin/quizzes/${id}/unpublish`)
  return data
}