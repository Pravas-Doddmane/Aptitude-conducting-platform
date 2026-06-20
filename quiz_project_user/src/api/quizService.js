import api from './axios';

export const fetchCategories = () => api.get('/api/categories');
export const fetchQuizzesByCategory = (categoryId) =>
  api.get(`/api/quizzes?categoryId=${categoryId}`);
export const fetchQuizById = (quizId) => api.get(`/api/quizzes/${quizId}`);