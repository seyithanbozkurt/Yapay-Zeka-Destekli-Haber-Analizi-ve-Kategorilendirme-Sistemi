import { api } from './api'
import type { ApiResponse } from '../types/auth'
import type { News } from '../types/news'

export interface NewsPage {
  content: News[]
  totalPages: number
  totalElements: number
  number: number
  size: number
  first: boolean
  last: boolean
}

export async function fetchAllNews(): Promise<News[]> {
  const { data } = await api.get<ApiResponse<News[]>>('/news')
  return data.data ?? []
}

export interface NewsPageFilters {
  search?: string
  sourceName?: string
  categoryName?: string
}

export async function fetchNewsPage(
  page: number,
  size = 20,
  filters?: NewsPageFilters,
): Promise<NewsPage> {
  try {
    const { data } = await api.get<ApiResponse<NewsPage>>('/news/page', {
      params: {
        page,
        size,
        search: filters?.search || undefined,
        sourceName: filters?.sourceName || undefined,
        categoryName: filters?.categoryName || undefined,
      },
    })

    return (
      data.data ?? {
        content: [],
        totalPages: 0,
        totalElements: 0,
        number: page,
        size,
        first: true,
        last: true,
      }
    )
  } catch {
    // Fallback: backend'de /news/page aktif değilse eski /news endpoint'iyle devam et.
    const all = await fetchAllNews()
    const search = filters?.search?.toLowerCase().trim()
    const sourceName = filters?.sourceName
    const categoryName = filters?.categoryName

    const filtered = all.filter((item) => {
      const matchesSearch =
        !search ||
        item.title?.toLowerCase().includes(search) ||
        item.content?.toLowerCase().includes(search)
      const matchesSource = !sourceName || item.sourceName === sourceName
      const matchesCategory =
        !categoryName ||
        (Array.isArray(item.categoryNames) && item.categoryNames.includes(categoryName))
      return matchesSearch && matchesSource && matchesCategory
    })

    const start = Math.max(page, 0) * size
    const end = start + size
    const content = filtered.slice(start, end)
    const totalElements = filtered.length
    const totalPages = Math.max(Math.ceil(totalElements / size), 1)
    const currentPage = Math.min(Math.max(page, 0), totalPages - 1)

    return {
      content,
      totalPages,
      totalElements,
      number: currentPage,
      size,
      first: currentPage === 0,
      last: currentPage >= totalPages - 1,
    }
  }
}

