import api from './axios';

const PROCTORING_BASE = import.meta.env.VITE_PROCTORING_API_URL || 'http://localhost:8000';

export const fetchProctoringConfig = () =>
  fetch(`${PROCTORING_BASE}/api/config`).then((res) => {
    if (!res.ok) throw new Error('Failed to load proctoring configuration');
    return res.json();
  });

export const initializeProctoringSession = (attemptId) =>
  api.post(`/api/proctoring/attempts/${attemptId}/session`);

export const getProctoringSession = (attemptId) =>
  api.get(`/api/proctoring/attempts/${attemptId}/session`);

export const endProctoringSession = (attemptId) =>
  api.post(`/api/proctoring/attempts/${attemptId}/session/end`);

export const recordProctoringEvent = (attemptId, event) =>
  api.post(`/api/proctoring/attempts/${attemptId}/events`, event);

export const recordProctoringViolation = (attemptId, violation) =>
  api.post(`/api/proctoring/attempts/${attemptId}/violations`, violation);

export const getViolationTimeline = (attemptId) =>
  api.get(`/api/proctoring/attempts/${attemptId}/violations/timeline`);

export const getProctoringWsUrl = () =>
  import.meta.env.VITE_PROCTORING_WS_URL || 'ws://localhost:8000/ws';
