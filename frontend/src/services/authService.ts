import type {
  ApiResponse,
  AuthResponse,
  LoginRequest,
  RegisterRequest,
} from "../types/auth";

const API_BASE_URL = "http://localhost:8989/api/auth";

async function postAuth<TRequest>(
  endpoint: string,
  payload: TRequest,
): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  const result: ApiResponse<AuthResponse> = await response.json();

  if (!response.ok || !result.success) {
    throw new Error(result.message || "İşlem başarısız oldu");
  }

  return result.data;
}

export const authService = {
  login(payload: LoginRequest) {
    return postAuth("/login", payload);
  },

  register(payload: RegisterRequest) {
    return postAuth("/register", payload);
  },
};
