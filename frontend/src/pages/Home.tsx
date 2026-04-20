import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchAllNews } from '../services/newsService'
import type { News } from '../types/news'
import { api } from '../services/api'

interface HomeStats {
  newsCount: number
  categoryCount: number
  sourceCount: number
  feedbackCount: number
}

function Home() {
  const [news, setNews] = useState<News[]>([])
  const [loading, setLoading] = useState(true)
  const [stats, setStats] = useState<HomeStats | null>(null)

  useEffect(() => {
    let isMounted = true
    const load = async () => {
      try {
        const [data, categoryRes, sourceRes, feedbackRes] = await Promise.all([
          fetchAllNews(),
          api.get('/categories'),
          api.get('/sources'),
          api.get('/user-feedback'),
        ])
        if (!isMounted) return
        setNews(data)
        setStats({
          newsCount: data.length,
          categoryCount: Array.isArray(categoryRes.data) ? categoryRes.data.length : 0,
          sourceCount: Array.isArray(sourceRes.data) ? sourceRes.data.length : 0,
          feedbackCount: Array.isArray(feedbackRes.data) ? feedbackRes.data.length : 0,
        })
      } finally {
        if (isMounted) setLoading(false)
      }
    }
    load()
    return () => {
      isMounted = false
    }
  }, [])

  const featured = news.slice(0, 8)
  const cards = news.slice(0, 6)

  return (
    <div className="space-y-8">
      <section className="bg-white rounded-xl shadow p-5">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Genel Durum</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="bg-gray-50 rounded-xl border border-gray-200 p-4">
            <p className="text-sm text-gray-500">Toplam Haber</p>
            <p className="mt-2 text-3xl font-bold text-gray-900">
              {loading || !stats ? '—' : stats.newsCount}
            </p>
          </div>
          <div className="bg-gray-50 rounded-xl border border-gray-200 p-4">
            <p className="text-sm text-gray-500">Toplam Kategori</p>
            <p className="mt-2 text-3xl font-bold text-gray-900">
              {loading || !stats ? '—' : stats.categoryCount}
            </p>
          </div>
          <div className="bg-gray-50 rounded-xl border border-gray-200 p-4">
            <p className="text-sm text-gray-500">Toplam Kaynak</p>
            <p className="mt-2 text-3xl font-bold text-gray-900">
              {loading || !stats ? '—' : stats.sourceCount}
            </p>
          </div>
          <div className="bg-gray-50 rounded-xl border border-gray-200 p-4">
            <p className="text-sm text-gray-500">Toplam Geri Bildirim</p>
            <p className="mt-2 text-3xl font-bold text-gray-900">
              {loading || !stats ? '—' : stats.feedbackCount}
            </p>
          </div>
        </div>
      </section>

      <section className="bg-white rounded-xl shadow p-5">
        <div className="flex items-center justify-between mb-3">
          <h1 className="text-xl font-bold text-gray-900">Son Dakika / Öne Çıkanlar</h1>
          <Link to="/news" className="text-sm text-blue-600 hover:underline">
            Tüm haberler
          </Link>
        </div>
        {loading ? (
          <p className="text-sm text-gray-500">Haberler yükleniyor...</p>
        ) : (
          <div className="flex gap-4 overflow-x-auto pb-2">
            {featured.map((item) => (
              <Link
                key={item.id}
                to={`/news/${item.id}`}
                className="min-w-[300px] bg-gray-50 border border-gray-200 rounded-xl p-4 hover:bg-gray-100 transition-colors"
              >
                <p className="text-xs text-blue-700 font-medium">{item.sourceName}</p>
                <p className="mt-1 text-sm font-semibold text-gray-900 line-clamp-2">{item.title}</p>
              </Link>
            ))}
          </div>
        )}
      </section>

      <section className="bg-white rounded-xl shadow p-5">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Günün Haberleri</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {cards.map((item) => (
            <Link
              key={item.id}
              to={`/news/${item.id}`}
              className="border border-gray-200 rounded-xl overflow-hidden hover:shadow-md transition-shadow"
            >
              <div className="h-36 bg-gradient-to-r from-blue-100 to-indigo-100 grid place-items-center text-sm text-gray-600">
                Haber görsel alanı
              </div>
              <div className="p-4">
                <p className="text-xs text-gray-500">{item.sourceName}</p>
                <h3 className="mt-1 text-sm font-semibold text-gray-900 line-clamp-2">{item.title}</h3>
              </div>
            </Link>
          ))}
        </div>
      </section>
    </div>
  )
}

export default Home

