import { api } from './api'
import type { NewsClassificationResult } from '../types/newsClassification'

export async function fetchClassificationByNewsId(
  newsId: string | number,
): Promise<NewsClassificationResult | null> {
  const { data } = await api.get<NewsClassificationResult[]>(
    `/news-classification-results/by-news/${newsId}`,
  )
  const results = data ?? []
  if (!Array.isArray(results) || results.length === 0) return null

  // Şimdilik ilk sonucu kullanıyoruz (gerekirse burada filtre/öncelik eklenebilir)
  return results[0]
}

