import axios from 'axios'
import type {
  ApiResponse,
  AuthResponse,
  LoginRequest,
  RegisterRequest,
} from '../types/auth'

const publicApi = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

export async function login(username: string, password: string): Promise<AuthResponse> {
  const payload: LoginRequest = { username, password }
  const { data } = await publicApi.post<ApiResponse<AuthResponse>>('/auth/login', payload)
  return data.data
}

export async function register(payload: RegisterRequest): Promise<AuthResponse> {
  const { data } = await publicApi.post<ApiResponse<AuthResponse>>('/auth/register', payload)
  return data.data
}
