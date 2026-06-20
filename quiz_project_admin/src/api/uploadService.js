import api from './axios'

export const uploadQuestionImage = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await api.post('/api/admin/uploads/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}
