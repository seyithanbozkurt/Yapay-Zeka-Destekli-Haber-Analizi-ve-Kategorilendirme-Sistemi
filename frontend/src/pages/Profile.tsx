import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { fetchMyProfile } from '../services/userService'
import type { UserProfile } from '../types/user'

function Profile() {
  const { username } = useAuth()
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

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

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Profilim</h1>
        <p className="text-sm text-gray-600 mt-1">Kişisel ve iletişim bilgilerinizi görüntüleyin.</p>
      </div>

      {error && (
        <div className="mb-4 rounded-lg bg-red-50 text-red-700 px-4 py-3 text-sm">
          {error}
        </div>
      )}

      {loading ? (
        <div className="rounded-xl bg-white shadow p-6 text-sm text-gray-500">
          Profil bilgileri yükleniyor...
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <div className="bg-white rounded-xl shadow p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Profil Fotoğrafı</h2>
            <div className="flex flex-col items-center text-center">
              <div className="h-28 w-28 rounded-full bg-gradient-to-br from-blue-500 to-indigo-600 text-white flex items-center justify-center text-3xl font-bold shadow">
                {initials}
              </div>
              <p className="mt-4 text-base font-semibold text-gray-900">{displayName}</p>
              <p className="text-sm text-gray-500">@{profile?.username ?? username ?? '-'}</p>
              <p className="mt-3 text-xs text-gray-500">
                Fotoğraf yükleme özelliği sonraki sürümde eklenecek.
              </p>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow p-6 lg:col-span-2">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Genel Bilgiler</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="rounded-lg border border-gray-200 p-4">
                <p className="text-xs text-gray-500">İsim</p>
                <p className="mt-1 text-sm font-medium text-gray-900">{profile?.firstName || '-'}</p>
              </div>
              <div className="rounded-lg border border-gray-200 p-4">
                <p className="text-xs text-gray-500">Soyisim</p>
                <p className="mt-1 text-sm font-medium text-gray-900">{profile?.lastName || '-'}</p>
              </div>
              <div className="rounded-lg border border-gray-200 p-4">
                <p className="text-xs text-gray-500">Kullanıcı Adı</p>
                <p className="mt-1 text-sm font-medium text-gray-900">{profile?.username || username || '-'}</p>
              </div>
              <div className="rounded-lg border border-gray-200 p-4">
                <p className="text-xs text-gray-500">Doğum Tarihi</p>
                <p className="mt-1 text-sm font-medium text-gray-900">
                  {profile?.birthDate ? new Date(profile.birthDate).toLocaleDateString('tr-TR') : '-'}
                </p>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow p-6 lg:col-span-3">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">İletişim</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="rounded-lg border border-gray-200 p-4">
                <p className="text-xs text-gray-500">E-posta</p>
                <p className="mt-1 text-sm font-medium text-gray-900">{profile?.email || '-'}</p>
              </div>
              <div className="rounded-lg border border-gray-200 p-4">
                <p className="text-xs text-gray-500">Telefon</p>
                <p className="mt-1 text-sm font-medium text-gray-900">Henüz eklenmedi</p>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default Profile

