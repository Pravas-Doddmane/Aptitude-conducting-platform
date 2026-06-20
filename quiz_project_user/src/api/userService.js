import api from './axios';

export const fetchUserStats = () => api.get('/api/users/me/stats');