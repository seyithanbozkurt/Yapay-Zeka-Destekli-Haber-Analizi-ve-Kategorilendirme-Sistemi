import { useEffect, useRef, useState } from 'react'
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
  const featuredScrollRef = useRef<HTMLDivElement | null>(null)
  const [isFeaturedHovered, setIsFeaturedHovered] = useState(false)

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
  const cards = news.slice(0, 15)
  const marketItems = [
    { label: 'DOLAR', value: '38.42', change: '+0.24%', positive: true },
    { label: 'EURO', value: '43.71', change: '+0.19%', positive: true },
    { label: 'ALTIN', value: '2,487', change: '+0.62%', positive: true },
    { label: 'BIST 100', value: '9,821', change: '-0.31%', positive: false },
    { label: 'PETROL', value: '84.10', change: '-0.12%', positive: false },
  ]

  useEffect(() => {
    if (loading || featured.length <= 1) return
    if (isFeaturedHovered) return

    const interval = window.setInterval(() => {
      const container = featuredScrollRef.current
      if (!container) return

      const maxScrollLeft = container.scrollWidth - container.clientWidth
      const step = Math.max(Math.floor(container.clientWidth * 0.72), 300)
      const nextScrollLeft = container.scrollLeft + step

      if (nextScrollLeft >= maxScrollLeft - 10) {
        container.scrollTo({ left: 0, behavior: 'smooth' })
        return
      }

      container.scrollTo({ left: nextScrollLeft, behavior: 'smooth' })
    }, 4000)

    return () => window.clearInterval(interval)
  }, [featured.length, loading, isFeaturedHovered])

  const scrollFeatured = (direction: 'left' | 'right') => {
    const container = featuredScrollRef.current
    if (!container) return
    const step = Math.max(Math.floor(container.clientWidth * 0.72), 300)
    const target =
      direction === 'left'
        ? Math.max(container.scrollLeft - step, 0)
        : Math.min(container.scrollLeft + step, container.scrollWidth - container.clientWidth)
    container.scrollTo({ left: target, behavior: 'smooth' })
  }

  return (
    <div className="space-y-8">
      <section className="bg-white rounded-xl shadow p-4">
        <div className="mb-3">
          <h2 className="text-sm font-semibold text-gray-900">Güncel Piyasa</h2>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3">
          {marketItems.map((item) => (
            <div key={item.label} className="rounded-lg border border-gray-200 bg-gray-50 px-3 py-2.5">
              <p className="text-[11px] text-gray-500">{item.label}</p>
              <p className="mt-1 text-base font-semibold text-gray-900">{item.value}</p>
              <p
                className={`text-xs font-medium mt-0.5 ${
                  item.positive ? 'text-emerald-600' : 'text-red-600'
                }`}
              >
                {item.change}
              </p>
            </div>
          ))}
        </div>
      </section>

      <section className="bg-white rounded-xl shadow p-4">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Genel Durum</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="bg-gray-50 rounded-xl border border-gray-200 p-3.5">
            <p className="text-xs text-gray-500">Toplam Haber</p>
            <p className="mt-1.5 text-2xl font-bold text-gray-900">
              {loading || !stats ? '—' : stats.newsCount}
            </p>
          </div>
          <div className="bg-gray-50 rounded-xl border border-gray-200 p-3.5">
            <p className="text-xs text-gray-500">Toplam Kategori</p>
            <p className="mt-1.5 text-2xl font-bold text-gray-900">
              {loading || !stats ? '—' : stats.categoryCount}
            </p>
          </div>
          <div className="bg-gray-50 rounded-xl border border-gray-200 p-3.5">
            <p className="text-xs text-gray-500">Toplam Kaynak</p>
            <p className="mt-1.5 text-2xl font-bold text-gray-900">
              {loading || !stats ? '—' : stats.sourceCount}
            </p>
          </div>
          <div className="bg-gray-50 rounded-xl border border-gray-200 p-3.5">
            <p className="text-xs text-gray-500">Toplam Geri Bildirim</p>
            <p className="mt-1.5 text-2xl font-bold text-gray-900">
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
          <div
            className="relative"
            onMouseEnter={() => setIsFeaturedHovered(true)}
            onMouseLeave={() => setIsFeaturedHovered(false)}
          >
            <button
              type="button"
              onClick={() => scrollFeatured('left')}
              className="hidden md:flex absolute left-2 top-1/2 -translate-y-1/2 z-10 h-10 w-10 items-center justify-center rounded-full bg-black/45 text-white hover:bg-black/60 transition"
              aria-label="Önceki haberler"
            >
              ‹
            </button>

            <div
              ref={featuredScrollRef}
              className="flex gap-5 overflow-x-auto pb-2 snap-x snap-mandatory scrollbar-thin"
            >
              {featured.map((item) => (
                <a
                  key={item.id}
                  href={item.originalUrl || `/news/${item.id}`}
                  target={item.originalUrl ? '_blank' : undefined}
                  rel={item.originalUrl ? 'noreferrer' : undefined}
                  className="relative min-w-[85%] md:min-w-[70%] lg:min-w-[58%] h-[280px] md:h-[360px] rounded-2xl overflow-hidden shadow snap-start group"
                >
                  <div className="h-full w-full bg-gray-100">
                    {item.imageUrl ? (
                      <img
                        src={item.imageUrl}
                        alt={item.title}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                        loading="lazy"
                      />
                    ) : (
                      <div className="w-full h-full bg-gradient-to-r from-blue-100 to-indigo-100 grid place-items-center text-base text-gray-700 font-medium">
                        Haber görseli yok
                      </div>
                    )}
                  </div>

                  <div className="absolute inset-0 bg-gradient-to-t from-black/65 via-black/20 to-transparent pointer-events-none" />
                  <div className="absolute left-0 right-0 bottom-0 p-5 text-white">
                    <p className="text-xs font-semibold uppercase tracking-wide text-blue-200">
                      {item.sourceName}
                    </p>
                    <p className="mt-2 text-base md:text-lg font-semibold line-clamp-2">{item.title}</p>
                  </div>
                </a>
              ))}
            </div>

            <button
              type="button"
              onClick={() => scrollFeatured('right')}
              className="hidden md:flex absolute right-2 top-1/2 -translate-y-1/2 z-10 h-10 w-10 items-center justify-center rounded-full bg-black/45 text-white hover:bg-black/60 transition"
              aria-label="Sonraki haberler"
            >
              ›
            </button>
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
              <div className="h-36 bg-gray-100">
                {item.imageUrl ? (
                  <img
                    src={item.imageUrl}
                    alt={item.title}
                    className="w-full h-full object-cover"
                    loading="lazy"
                  />
                ) : (
                  <div className="w-full h-full bg-gradient-to-r from-blue-100 to-indigo-100 grid place-items-center text-sm text-gray-600">
                    Haber gorseli
                  </div>
                )}
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

