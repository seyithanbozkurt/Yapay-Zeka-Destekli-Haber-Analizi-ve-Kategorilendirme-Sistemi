export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  email: string
  firstName: string
  lastName: string
  birthDate: string
  password: string
}

export interface AuthResponse {
  token: string
  type?: string
  username: string
}

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  timestamp: string
}
