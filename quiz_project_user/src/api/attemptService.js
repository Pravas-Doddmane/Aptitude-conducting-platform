import api from './axios';

export const startAttempt = (quizId) => api.post(`/api/attempts/start/${quizId}`);
export const submitAttempt = (attemptId, answers) =>
  api.post(`/api/attempts/${attemptId}/submit`, { answers });
export const autoSubmitAttempt = (attemptId, answers = []) =>
  api.post(`/api/attempts/${attemptId}/auto-submit`, { answers });
export const fetchMyAttempts = () => api.get('/api/attempts/me');
export const fetchAttemptById = (attemptId) => api.get(`/api/attempts/${attemptId}`);
