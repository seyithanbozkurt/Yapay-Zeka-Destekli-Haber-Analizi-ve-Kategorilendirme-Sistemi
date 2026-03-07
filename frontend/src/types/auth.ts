export interface LoginRequest {
  username: string
  password: string
}

export interface AuthResponse {
  token: string
  type: string
  username: string
}

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  timestamp: string
}
