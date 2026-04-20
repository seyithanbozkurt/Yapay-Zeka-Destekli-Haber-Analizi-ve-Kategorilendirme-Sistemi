import { createContext, useContext, useState, useEffect, type ReactNode } from 'react'

interface AuthContextType {
  token: string | null
  username: string | null
  isAuthenticated: boolean
  login: (token: string, username: string) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextType | null>(null)

const TOKEN_KEY = 'token'
const USERNAME_KEY = 'username'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY))
  const [username, setUsername] = useState<string | null>(() => localStorage.getItem(USERNAME_KEY))

  useEffect(() => {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token)
    } else {
      localStorage.removeItem(TOKEN_KEY)
    }
  }, [token])

  useEffect(() => {
    if (username) {
      localStorage.setItem(USERNAME_KEY, username)
    } else {
      localStorage.removeItem(USERNAME_KEY)
    }
  }, [username])

  const login = (newToken: string, newUsername: string) => {
    setToken(newToken)
    setUsername(newUsername)
  }

  const logout = () => {
    setToken(null)
    setUsername(null)
  }

  return (
    <AuthContext.Provider
      value={{
        token,
        username,
        isAuthenticated: !!token,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
