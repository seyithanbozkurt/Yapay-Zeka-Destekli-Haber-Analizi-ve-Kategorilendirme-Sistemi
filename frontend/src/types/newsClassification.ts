export interface NewsClassificationResult {
  id: number
  newsTitle: string
  modelVersionName: string
  predictedCategoryName: string
  predictionScore: string
  active: boolean
}

