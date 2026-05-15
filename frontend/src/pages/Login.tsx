import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate, Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { login as loginApi, register as registerApi } from '../services/authService'

function Login() {
  const navigate = useNavigate()
  const { login, isAuthenticated } = useAuth()
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [email, setEmail] = useState('')
  const [birthDate, setBirthDate] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)

  if (isAuthenticated) {
    return <Navigate to="/home" replace />
  }

  const handleLoginSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    setLoading(true)

    try {
      const { token, username: user } = await loginApi(username, password)
      login(token, user)
      navigate('/home', { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Giriş yapılamadı. Lütfen tekrar deneyin.')
    } finally {
      setLoading(false)
    }
  }

  const handleRegisterSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    setLoading(true)

    try {
      const { username: createdUsername } = await registerApi({
        email: email.trim(),
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        birthDate,
        password,
      })
      setMode('login')
      setUsername(createdUsername)
      setSuccess(
        `Kayıt başarılı. Giriş yapmak için kullanıcı adınız: ${createdUsername}`,
      )
      setFirstName('')
      setLastName('')
      setEmail('')
      setBirthDate('')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Kayıt yapılamadı. Lütfen tekrar deneyin.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="w-full max-w-md">
        <div className="bg-white rounded-xl shadow-lg p-8">
          <div className="text-center mb-8">
            <h1 className="text-2xl font-bold text-gray-900">Haber Analizi Sistemi</h1>
            <p className="mt-1 text-sm text-gray-500">
              {mode === 'login' ? 'Devam etmek için giriş yapın' : 'Yeni hesap oluşturun'}
            </p>
          </div>

          <div className="mb-5 grid grid-cols-2 gap-2 rounded-lg bg-gray-100 p-1">
            <button
              type="button"
              onClick={() => {
                setMode('login')
                setError('')
                setSuccess('')
              }}
              className={`rounded-md px-3 py-2 text-sm font-medium transition ${
                mode === 'login'
                  ? 'bg-white text-blue-700 shadow-sm'
                  : 'text-gray-600 hover:text-gray-800'
              }`}
            >
              Giriş Yap
            </button>
            <button
              type="button"
              onClick={() => {
                setMode('register')
                setError('')
                setSuccess('')
              }}
              className={`rounded-md px-3 py-2 text-sm font-medium transition ${
                mode === 'register'
                  ? 'bg-white text-blue-700 shadow-sm'
                  : 'text-gray-600 hover:text-gray-800'
              }`}
            >
              Kayıt Ol
            </button>
          </div>

          <form
            onSubmit={mode === 'login' ? handleLoginSubmit : handleRegisterSubmit}
            className="space-y-5"
          >
            {error && (
              <div className="p-3 rounded-lg bg-red-50 text-red-700 text-sm">
                {error}
              </div>
            )}
            {success && (
              <div className="p-3 rounded-lg bg-green-50 text-green-700 text-sm">
                {success}
              </div>
            )}

            {mode === 'login' ? (
              <div>
                <label htmlFor="username" className="block text-sm font-medium text-gray-700 mb-1">
                  Kullanıcı Adı
                </label>
                <input
                  id="username"
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="Kullanıcı adınızı girin"
                  className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition"
                  required
                  autoComplete="username"
                  disabled={loading}
                />
              </div>
            ) : (
              <>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <div>
                    <label htmlFor="firstName" className="block text-sm font-medium text-gray-700 mb-1">
                      İsim
                    </label>
                    <input
                      id="firstName"
                      type="text"
                      value={firstName}
                      onChange={(e) => setFirstName(e.target.value)}
                      placeholder="İsim"
                      className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition"
                      required
                      disabled={loading}
                    />
                  </div>
                  <div>
                    <label htmlFor="lastName" className="block text-sm font-medium text-gray-700 mb-1">
                      Soyisim
                    </label>
                    <input
                      id="lastName"
                      type="text"
                      value={lastName}
                      onChange={(e) => setLastName(e.target.value)}
                      placeholder="Soyisim"
                      className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition"
                      required
                      disabled={loading}
                    />
                  </div>
                </div>

                <div>
                  <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
                    E-posta
                  </label>
                  <input
                    id="email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="ornek@mail.com"
                    className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition"
                    required
                    autoComplete="email"
                    disabled={loading}
                  />
                </div>

                <div>
                  <label htmlFor="birthDate" className="block text-sm font-medium text-gray-700 mb-1">
                    Doğum Tarihi
                  </label>
                  <input
                    id="birthDate"
                    type="date"
                    value={birthDate}
                    onChange={(e) => setBirthDate(e.target.value)}
                    className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition"
                    required
                    disabled={loading}
                  />
                </div>
              </>
            )}

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">
                Şifre
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition"
                required
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                disabled={loading}
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 px-4 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-medium rounded-lg transition focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
            >
              {loading
                ? mode === 'login'
                  ? 'Giriş yapılıyor...'
                  : 'Kayıt oluşturuluyor...'
                : mode === 'login'
                  ? 'Giriş Yap'
                  : 'Kayıt Ol'}
            </button>
          </form>
        </div>

        {mode === 'login' && (
          <p className="mt-4 text-center text-xs text-gray-500">
            Örnek: metehan / 123123123
          </p>
        )}
      </div>
    </div>
  )
}

export default Login
