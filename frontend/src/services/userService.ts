import { api } from './api'
import type { UserProfile } from '../types/user'

export async function fetchMyProfile(): Promise<UserProfile> {
  const { data } = await api.get<UserProfile>('/users/me')
  return data
}
