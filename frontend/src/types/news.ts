export interface News {
  id: number
  sourceName: string
  title: string
  content: string
  originalUrl: string
  imageUrl?: string
  language: string
  publishedAt: string
  processed: boolean
  categoryNames: string[]
}

