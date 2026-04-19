import { api } from './api'
import type { Category } from '../types/category'

export async function fetchAllCategories(): Promise<Category[]> {
  const { data } = await api.get<Category[]>('/categories')
  return Array.isArray(data) ? data : []
}

