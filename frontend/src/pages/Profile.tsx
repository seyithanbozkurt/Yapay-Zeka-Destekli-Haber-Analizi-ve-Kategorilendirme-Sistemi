import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'
import { changePassword } from '../services/authService'
import { ensureLegacyNewsActivityMigrated } from '../services/legacyNewsActivityMigration'
import {
  fetchReadHistory,
  fetchSavedNews,
  removeSavedNews,
  type UserReadHistoryItem,
  type UserSavedNewsItem,
} from '../services/userNewsActivityService'
import { fetchMyProfile } from '../services/userService'
import type { UserProfile } from '../types/user'

function Profile() {
  const { username } = useAuth()
  const { theme, toggleTheme } = useTheme()
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [savedNews, setSavedNews] = useState<UserSavedNewsItem[]>([])
  const [readHistory, setReadHistory] = useState<UserReadHistoryItem[]>([])
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [passwordMessage, setPasswordMessage] = useState('')
  const [passwordError, setPasswordError] = useState('')
  const [passwordLoading, setPasswordLoading] = useState(false)

  useEffect(() => {
    let isMounted = true

    const loadProfile = async () => {
      try {
        setLoading(true)
        setError('')
        const data = await fetchMyProfile()
        if (!isMounted) return
        setProfile(data)
      } catch {
        if (!isMounted) return
        setError('Profil bilgileri yüklenemedi. Lütfen daha sonra tekrar deneyin.')
      } finally {
        if (isMounted) setLoading(false)
      }
    }

    loadProfile()
    return () => {
      isMounted = false
    }
  }, [])

  useEffect(() => {
    let isMounted = true
    const loadActivity = async () => {
      try {
        await ensureLegacyNewsActivityMigrated()
        const [saved, history] = await Promise.all([fetchSavedNews(), fetchReadHistory()])
        if (!isMounted) return
        setSavedNews(saved)
        setReadHistory(history)
      } catch {
        if (!isMounted) return
        setSavedNews([])
        setReadHistory([])
      }
    }
    void loadActivity()
    return () => {
      isMounted = false
    }
  }, [])

  const displayName = useMemo(() => {
    const first = profile?.firstName?.trim() ?? ''
    const last = profile?.lastName?.trim() ?? ''
    const full = `${first} ${last}`.trim()
    return full || username || '-'
  }, [profile?.firstName, profile?.lastName, username])

  const initials = useMemo(() => {
    const source = displayName === '-' ? username ?? '' : displayName
    return source
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join('') || 'U'
  }, [displayName, username])

  const handlePasswordSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setPasswordError('')
    setPasswordMessage('')

    if (newPassword.length < 6) {
      setPasswordError('Yeni şifre en az 6 karakter olmalıdır.')
      return
    }

    try {
      setPasswordLoading(true)
      await changePassword({ currentPassword, newPassword })
      setPasswordMessage('Şifreniz başarıyla güncellendi.')
      setCurrentPassword('')
      setNewPassword('')
    } catch (e) {
      const message =
        typeof e === 'object' &&
        e !== null &&
        'response' in e &&
        typeof (e as { response?: { data?: { message?: string } } }).response?.data?.message ===
          'string'
          ? (e as { response?: { data?: { message?: string } } }).response!.data!.message!
          : 'Şifre değiştirilemedi. Lütfen tekrar deneyin.'
      setPasswordError(message)
    } finally {
      setPasswordLoading(false)
    }
  }

  const handleRemoveSaved = async (newsId: number) => {
    try {
      await removeSavedNews(newsId)
      setSavedNews((prev) => prev.filter((item) => item.newsId !== newsId))
    } catch {
      // Hata durumunda mevcut liste korunur.
    }
  }

  return (
    <div className={`min-h-screen p-6 ${theme === 'dark' ? 'bg-slate-950 text-slate-100' : 'bg-gray-50'}`}>
      <div className="mb-6">
        <h1 className={`text-2xl font-bold ${theme === 'dark' ? 'text-white' : 'text-gray-900'}`}>Profilim</h1>
        <p className={`text-sm mt-1 ${theme === 'dark' ? 'text-slate-300' : 'text-gray-600'}`}>
          Kişisel bilgiler, güvenlik ve tercihlerinizi buradan yönetin.
        </p>
      </div>

      {error && (
        <div className="mb-4 rounded-lg bg-red-50 text-red-700 px-4 py-3 text-sm">
          {error}
        </div>
      )}

      {loading ? (
        <div
          className={`rounded-xl shadow p-6 text-sm ${
            theme === 'dark' ? 'bg-slate-900 text-slate-300' : 'bg-white text-gray-500'
          }`}
        >
          Profil bilgileri yükleniyor...
        </div>
      ) : (
        <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-3">
          <div className={`rounded-xl shadow p-6 ${theme === 'dark' ? 'bg-slate-900' : 'bg-white'}`}>
            <h2 className={`text-lg font-semibold mb-4 ${theme === 'dark' ? 'text-white' : 'text-gray-900'}`}>
              Profil Fotoğrafı
            </h2>
            <div className="flex flex-col items-center text-center">
              <div className="h-28 w-28 rounded-full bg-gradient-to-br from-blue-500 to-indigo-600 text-white flex items-center justify-center text-3xl font-bold shadow">
                {initials}
              </div>
              <p className={`mt-4 text-base font-semibold ${theme === 'dark' ? 'text-white' : 'text-gray-900'}`}>
                {displayName}
              </p>
              <p className={`text-sm ${theme === 'dark' ? 'text-slate-400' : 'text-gray-500'}`}>
                @{profile?.username ?? username ?? '-'}
              </p>
              <button
                type="button"
                onClick={toggleTheme}
                className="mt-4 rounded-lg border border-gray-300 bg-white px-3 py-2 text-xs font-medium text-gray-700 transition hover:bg-gray-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100 dark:hover:bg-slate-700"
              >
                Tema: {theme === 'dark' ? 'Koyu' : 'Açık'} (Değiştir)
              </button>
            </div>
          </div>

          <div className={`rounded-xl shadow p-6 lg:col-span-2 ${theme === 'dark' ? 'bg-slate-900' : 'bg-white'}`}>
            <h2 className={`text-lg font-semibold mb-4 ${theme === 'dark' ? 'text-white' : 'text-gray-900'}`}>
              Genel Bilgiler
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="rounded-lg border border-gray-200 p-4 dark:border-slate-700">
                <p className="text-xs text-gray-500">İsim</p>
                <p className="mt-1 text-sm font-medium text-gray-900 dark:text-slate-100">
                  {profile?.firstName || '-'}
                </p>
              </div>
              <div className="rounded-lg border border-gray-200 p-4 dark:border-slate-700">
                <p className="text-xs text-gray-500">Soyisim</p>
                <p className="mt-1 text-sm font-medium text-gray-900 dark:text-slate-100">
                  {profile?.lastName || '-'}
                </p>
              </div>
              <div className="rounded-lg border border-gray-200 p-4 dark:border-slate-700">
                <p className="text-xs text-gray-500">Kullanıcı Adı</p>
                <p className="mt-1 text-sm font-medium text-gray-900 dark:text-slate-100">
                  {profile?.username || username || '-'}
                </p>
              </div>
              <div className="rounded-lg border border-gray-200 p-4 dark:border-slate-700">
                <p className="text-xs text-gray-500">Doğum Tarihi</p>
                <p className="mt-1 text-sm font-medium text-gray-900 dark:text-slate-100">
                  {profile?.birthDate ? new Date(profile.birthDate).toLocaleDateString('tr-TR') : '-'}
                </p>
              </div>
            </div>
          </div>

          <div className={`rounded-xl shadow p-6 lg:col-span-3 ${theme === 'dark' ? 'bg-slate-900' : 'bg-white'}`}>
            <h2 className={`text-lg font-semibold mb-4 ${theme === 'dark' ? 'text-white' : 'text-gray-900'}`}>
              İletişim
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="rounded-lg border border-gray-200 p-4 dark:border-slate-700">
                <p className="text-xs text-gray-500">E-posta</p>
                <p className="mt-1 text-sm font-medium text-gray-900 dark:text-slate-100">{profile?.email || '-'}</p>
              </div>
              <div className="rounded-lg border border-gray-200 p-4 dark:border-slate-700">
                <p className="text-xs text-gray-500">Telefon</p>
                <p className="mt-1 text-sm font-medium text-gray-900 dark:text-slate-100">Henüz eklenmedi</p>
              </div>
            </div>
          </div>

          <div className={`rounded-xl shadow p-6 lg:col-span-2 ${theme === 'dark' ? 'bg-slate-900' : 'bg-white'}`}>
            <h2 className={`text-lg font-semibold mb-4 ${theme === 'dark' ? 'text-white' : 'text-gray-900'}`}>
              Şifre Değiştir
            </h2>
            <form className="space-y-3" onSubmit={handlePasswordSubmit}>
              <div>
                <label className="block text-xs text-gray-500 mb-1" htmlFor="currentPassword">
                  Mevcut Şifre
                </label>
                <input
                  id="currentPassword"
                  type="password"
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
                  required
                />
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1" htmlFor="newPassword">
                  Yeni Şifre
                </label>
                <input
                  id="newPassword"
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
                  minLength={6}
                  required
                />
              </div>
              {passwordError && <p className="text-sm text-red-600">{passwordError}</p>}
              {passwordMessage && <p className="text-sm text-emerald-600">{passwordMessage}</p>}
              <button
                type="submit"
                disabled={passwordLoading}
                className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:bg-blue-400"
              >
                {passwordLoading ? 'Güncelleniyor...' : 'Şifreyi Güncelle'}
              </button>
            </form>
          </div>

          <div className={`rounded-xl shadow p-6 ${theme === 'dark' ? 'bg-slate-900' : 'bg-white'}`}>
            <h2 className={`text-lg font-semibold mb-4 ${theme === 'dark' ? 'text-white' : 'text-gray-900'}`}>
              Kaydedilenler
            </h2>
            <div className="max-h-[18rem] space-y-2 overflow-y-auto pr-1">
              {savedNews.length === 0 && <p className="text-sm text-gray-500">Henüz kaydedilen haber yok.</p>}
              {savedNews.slice(0, 6).map((item) => (
                <div
                  key={item.newsId}
                  className="rounded-lg border border-gray-200 p-3 text-sm dark:border-slate-700"
                >
                  <Link to={`/news/${item.newsId}`} className="font-medium text-blue-600 hover:underline">
                    {item.title}
                  </Link>
                  <p className="text-xs text-gray-500 mt-1">{item.sourceName}</p>
                  <button
                    type="button"
                    onClick={() => void handleRemoveSaved(item.newsId)}
                    className="mt-2 text-xs text-red-600 hover:underline"
                  >
                    Kaydedilenlerden kaldır
                  </button>
                </div>
              ))}
            </div>
          </div>

          <div className={`rounded-xl shadow p-6 lg:col-span-3 ${theme === 'dark' ? 'bg-slate-900' : 'bg-white'}`}>
            <h2 className={`text-lg font-semibold mb-4 ${theme === 'dark' ? 'text-white' : 'text-gray-900'}`}>
              Okuma Geçmişi
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {readHistory.length === 0 && <p className="text-sm text-gray-500">Henüz okuma geçmişi yok.</p>}
              {readHistory.slice(0, 10).map((item) => (
                <Link
                  key={`${item.newsId}-${item.lastViewedAt ?? item.publishedAt}`}
                  to={`/news/${item.newsId}`}
                  className="rounded-lg border border-gray-200 p-3 transition hover:border-blue-300 hover:bg-blue-50/30 dark:border-slate-700 dark:hover:bg-slate-800"
                >
                  <p className="text-sm font-medium text-gray-900 dark:text-slate-100 line-clamp-2">{item.title}</p>
                  <p className="mt-1 text-xs text-gray-500">
                    {item.sourceName}
                    {item.lastViewedAt ? ` • ${new Date(item.lastViewedAt).toLocaleString('tr-TR')}` : ''}
                  </p>
                </Link>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default Profile

