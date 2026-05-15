import { api } from './api'
import type { ApiResponse } from '../types/auth'

export interface UserSavedNewsItem {
  newsId: number
  title: string
  sourceName: string
  publishedAt: string
  imageUrl?: string
  originalUrl?: string
  savedAt: string
}

export interface UserReadHistoryItem {
  newsId: number
  title: string
  sourceName: string
  publishedAt: string
  imageUrl?: string
  originalUrl?: string
  viewCount: number
  lastViewedAt: string
}

interface SavedNewsToggleResponse {
  saved: boolean
}

export async function fetchSavedNews(): Promise<UserSavedNewsItem[]> {
  const { data } = await api.get<ApiResponse<UserSavedNewsItem[]>>('/users/me/saved-news')
  return data.data ?? []
}

export async function toggleSavedNews(newsId: number): Promise<boolean> {
  const { data } = await api.post<ApiResponse<SavedNewsToggleResponse>>(`/users/me/saved-news/${newsId}/toggle`)
  return data.data?.saved ?? false
}

export async function getSavedNewsStatus(newsId: number): Promise<boolean> {
  const { data } = await api.get<ApiResponse<SavedNewsToggleResponse>>(`/users/me/saved-news/${newsId}/status`)
  return data.data?.saved ?? false
}

export async function removeSavedNews(newsId: number): Promise<void> {
  await api.delete(`/users/me/saved-news/${newsId}`)
}

export async function fetchReadHistory(): Promise<UserReadHistoryItem[]> {
  const { data } = await api.get<ApiResponse<UserReadHistoryItem[]>>('/users/me/read-history')
  return data.data ?? []
}

export async function markNewsAsRead(newsId: number): Promise<void> {
  await api.post(`/users/me/read-history/${newsId}`)
}

