import api from './axios';

export const uploadProctoringImage = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return api.post('/api/uploads/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

export const toAbsoluteUploadUrl = (relativeUrl) => {
  if (!relativeUrl) return relativeUrl;
  if (relativeUrl.startsWith('http://') || relativeUrl.startsWith('https://')) {
    return relativeUrl;
  }
  const base = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');
  return `${base}${relativeUrl.startsWith('/') ? relativeUrl : `/${relativeUrl}`}`;
};
