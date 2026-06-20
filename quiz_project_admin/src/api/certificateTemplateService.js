import api from './axios'

export const getCertificateTemplates = async () => {
  const { data } = await api.get('/api/admin/certificate-templates')
  return data
}

export const createCertificateTemplate = async (payload) => {
  const { data } = await api.post('/api/admin/certificate-templates', payload)
  return data
}

export const updateCertificateTemplate = async (id, payload) => {
  const { data } = await api.put(`/api/admin/certificate-templates/${id}`, payload)
  return data
}

export const uploadCertificateLogo = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await api.post('/api/admin/certificate-templates/upload-logo', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return data
}

export const deleteCertificateTemplate = async (id) => {
  const { data } = await api.delete(`/api/admin/certificate-templates/${id}`)
  return data
}
