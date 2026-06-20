import api from './axios';

export const captureReferenceImage = (attemptId, referenceImageUrl) =>
  api.post(`/api/identity-verification/attempts/${attemptId}/reference`, {
    referenceImageUrl,
  });

export const recordVerificationAttempt = (attemptId, payload) =>
  api.post(`/api/identity-verification/attempts/${attemptId}/verify`, payload);

export const getIdentityVerification = (attemptId) =>
  api.get(`/api/identity-verification/attempts/${attemptId}`);

export const getVerificationTimeline = (attemptId) =>
  api.get(`/api/identity-verification/attempts/${attemptId}/timeline`);
