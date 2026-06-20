import api from './axios';

export const register = (payload) => api.post('/api/auth/register', payload);
export const sendRegistrationOtp = (email) => api.post('/api/auth/register/send-otp', { email });
export const verifyRegistrationOtp = (email, otp) => api.post('/api/auth/register/verify-otp', { email, otp });
export const login = (payload) => api.post('/api/auth/login', payload);
export const refreshToken = (refreshToken) => api.post('/api/auth/refresh', { refreshToken });
export const logout = (refreshToken) => api.post('/api/auth/logout', { refreshToken });
export const forgotPassword = (email) => api.post('/api/auth/forgot-password', { email });
export const resetPassword = (token, newPassword) =>
  api.post('/api/auth/reset-password', { token, newPassword });
