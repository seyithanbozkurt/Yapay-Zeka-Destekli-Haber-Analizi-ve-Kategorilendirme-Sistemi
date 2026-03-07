import axios from 'axios'
import type { LoginRequest, ApiResponse, AuthResponse } from '../types/auth'

// Login için ayrı instance - 401 interceptor tetiklenmesin (hata mesajı gösterilebilsin)
const loginApi = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

export async function login(username: string, password: string): Promise<AuthResponse> {
  const { data } = await loginApi.post<ApiResponse<AuthResponse>>('/auth/login', {
    username,
    password,
  } as LoginRequest)

  if (!data.success || !data.data) {
    throw new Error(data.message || 'Giriş başarısız')
  }

  return data.data
}
