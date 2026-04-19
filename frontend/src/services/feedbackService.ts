import { api } from './api'
import type { UserFeedbackCreateRequest } from '../types/feedback'

export async function createFeedback(payload: UserFeedbackCreateRequest) {
  const { data } = await api.post('/user-feedback', payload)
  return data
}

