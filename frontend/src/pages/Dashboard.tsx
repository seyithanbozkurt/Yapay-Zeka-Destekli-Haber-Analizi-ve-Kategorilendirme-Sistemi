import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { api } from '../services/api'

interface DashboardStats {
  newsCount: number
  categoryCount: number
  sourceCount: number
  feedbackCount: number
}

function Dashboard() {
  const { username, logout } = useAuth()
  const navigate = useNavigate()

  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  useEffect(() => {
    let isMounted = true

    const loadStats = async () => {
      try {
        setLoading(true)
        setError('')

        const [newsRes, categoryRes, sourceRes, feedbackRes] = await Promise.all([
          api.get('/news'),
          api.get('/categories'),
          api.get('/sources'),
          api.get('/user-feedback'),
        ])

        if (!isMounted) return

        setStats({
          newsCount: Array.isArray(newsRes.data?.data) ? newsRes.data.data.length : 0,
          categoryCount: Array.isArray(categoryRes.data) ? categoryRes.data.length : 0,
          sourceCount: Array.isArray(sourceRes.data) ? sourceRes.data.length : 0,
          feedbackCount: Array.isArray(feedbackRes.data) ? feedbackRes.data.length : 0,
        })
      } catch (e) {
        if (!isMounted) return
        setError('Dashboard istatistikleri yüklenemedi. Lütfen daha sonra tekrar deneyin.')
      } finally {
        if (isMounted) {
          setLoading(false)
        }
      }
    }

    loadStats()

    return () => {
      isMounted = false
    }
  }, [])

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
          <p className="text-sm text-gray-600 mt-1">
            Sistem durumu ve özet istatistikler
          </p>
        </div>
        <div className="flex items-center gap-4">
          <span className="text-gray-600">Hoş geldin, {username}</span>
          <button
            onClick={handleLogout}
            className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
          >
            Çıkış Yap
          </button>
        </div>
      </div>

      {error && (
        <div className="mb-4 rounded-lg bg-red-50 text-red-700 px-4 py-3 text-sm">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div className="bg-white rounded-xl shadow p-4">
          <p className="text-sm text-gray-500">Toplam Haber</p>
          <p className="mt-2 text-3xl font-bold text-gray-900">
            {loading || !stats ? '—' : stats.newsCount}
          </p>
        </div>

        <div className="bg-white rounded-xl shadow p-4">
          <p className="text-sm text-gray-500">Toplam Kategori</p>
          <p className="mt-2 text-3xl font-bold text-gray-900">
            {loading || !stats ? '—' : stats.categoryCount}
          </p>
        </div>

        <div className="bg-white rounded-xl shadow p-4">
          <p className="text-sm text-gray-500">Toplam Kaynak</p>
          <p className="mt-2 text-3xl font-bold text-gray-900">
            {loading || !stats ? '—' : stats.sourceCount}
          </p>
        </div>

        <div className="bg-white rounded-xl shadow p-4">
          <p className="text-sm text-gray-500">Toplam Geri Bildirim</p>
          <p className="mt-2 text-3xl font-bold text-gray-900">
            {loading || !stats ? '—' : stats.feedbackCount}
          </p>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow p-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-lg font-semibold text-gray-900">Haber Listesi</h2>
            <p className="text-sm text-gray-600">
              Tüm haberleri görmek ve filtrelemek için liste sayfasına gidin.
            </p>
          </div>
          <button
            onClick={() => navigate('/news')}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 transition-colors"
          >
            Haberlere Git
          </button>
        </div>
      </div>
    </div>
  )
}

export default Dashboard
