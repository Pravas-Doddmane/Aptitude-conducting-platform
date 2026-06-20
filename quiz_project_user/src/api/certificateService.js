import api from './axios';

export const downloadCertificate = (attemptId, format = 'pdf', customName = null) => {
  const params = new URLSearchParams({ format });
  if (customName && customName.trim()) {
    params.append('customName', customName.trim());
  }
  return api.get(`/api/certificates/attempts/${attemptId}?${params.toString()}`, {
    responseType: 'blob',
  });
};