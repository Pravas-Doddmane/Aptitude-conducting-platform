import api from './axios';

export const submitQuizFeedback = (quizId, rating, message) =>
  api.post('/api/feedback', { quizId, rating, message });

export const submitQuestionFeedback = (quizId, questionId, rating, message) =>
  api.post('/api/feedback', { quizId, questionId, rating, message });