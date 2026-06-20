import api from './axios'

export const getResultSummaries = async () => {
  const { data } = await api.get('/api/admin/results')
  return data
}

export const getQuizParticipantResults = async (quizId, filter = 'ALL') => {
  const { data } = await api.get(`/api/admin/results/${quizId}/participants`, {
    params: { filter },
  })
  return data
}
