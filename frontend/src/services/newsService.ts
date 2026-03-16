import { api } from './api'
import type { ApiResponse } from '../types/auth'
import type { News } from '../types/news'

export async function fetchAllNews(): Promise<News[]> {
  const { data } = await api.get<ApiResponse<News[]>>('/news')
  return data.data ?? []
}

