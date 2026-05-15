import { api } from './api'
import type { UserFeedbackCreateRequest } from '../types/feedback'

export async function createFeedback(payload: UserFeedbackCreateRequest) {
  const { data } = await api.post('/user-feedback', payload)
  return data
}

export interface UserFeedbackResponse {
  id: number
  newsTitle: string
  username: string
  modelVersionName?: string
  currentPredictedCategoryName?: string
  userSelectedCategoryName: string
  feedbackType: string
  comment?: string
}

export async function fetchAllFeedbacks(): Promise<UserFeedbackResponse[]> {
  const { data } = await api.get<UserFeedbackResponse[]>('/user-feedback')
  return Array.isArray(data) ? data : []
}

