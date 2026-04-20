export type FeedbackType = 'POSITIVE' | 'NEGATIVE'

export interface UserFeedbackCreateRequest {
  newsId: number
  modelVersionId?: number
  currentPredictedCategoryId?: number
  userSelectedCategoryId: number
  feedbackType: FeedbackType
  comment?: string
}

