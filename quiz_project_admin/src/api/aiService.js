import api from './axios'

export const generateQuestionsWithAi = async (payload) => {
  const { data } = await api.post('/api/admin/ai/questions/generate', payload)
  return data
}

export const generateQuestionsWithAiFromFile = async ({ payload, file }) => {
  const formData = new FormData()
  Object.entries(payload).forEach(([key, value]) => {
    formData.append(key, value)
  })
  formData.append('file', file)

  const { data } = await api.post('/api/admin/ai/questions/generate-from-file', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}
