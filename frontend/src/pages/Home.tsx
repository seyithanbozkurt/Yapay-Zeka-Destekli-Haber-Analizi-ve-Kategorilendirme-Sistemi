import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchAllNews } from '../services/newsService'
import type { News } from '../types/news'
import { api } from '../services/api'
import { useTheme } from '../context/ThemeContext'

interface HomeStats {
  newsCount: number
  categoryCount: number
  sourceCount: number
  feedbackCount: number
}

function Home() {
  const { theme } = useTheme()
  const isDark = theme === 'dark'
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

  const renderMarketRow = (keyPrefix: string) => (
    <div key={keyPrefix} className="flex shrink-0 items-center">
      {marketItems.map((item, i) => (
        <div key={`${keyPrefix}-${item.label}`} className="flex shrink-0 items-center">
          {i > 0 && <span className="mx-3 h-5 w-px shrink-0 bg-slate-600" aria-hidden />}
          <div className="flex items-baseline gap-1.5 pr-1">
            <span className="text-[10px] font-semibold text-slate-400">{item.label}</span>
            <span className="text-xs font-semibold tabular-nums text-white">{item.value}</span>
            <span
              className={`text-[10px] font-medium tabular-nums ${
                item.positive ? 'text-emerald-400' : 'text-red-400'
              }`}
            >
              {item.change}
            </span>
          </div>
        </div>
      ))}
    </div>
  )

  return (
    <div className="space-y-6">
      {/* Sadece piyasa: tam genişlik ticker: güncel haber bloğu ile asla yan yana değil */}
      <section
        className="bg-slate-900 text-slate-100 rounded-lg border border-slate-800 shadow-sm"
        aria-label="Güncel piyasa"
      >
        <div className="flex items-center gap-2 border-b border-slate-700/80 px-3 py-1.5 sm:px-4">
          <span className="shrink-0 text-[10px] font-bold uppercase tracking-wider text-amber-400/90">
            Piyasa
          </span>
          <span className="hidden h-4 w-px bg-slate-600 sm:block" aria-hidden />
          <p className="min-w-0 truncate text-[10px] text-slate-400">Örnek veri — canlı bağlantı yok</p>
        </div>
        <div className="overflow-hidden py-2">
          <div className="home-marquee-track items-center px-1">
            {renderMarketRow('t1')}
            {renderMarketRow('t2')}
          </div>
        </div>
      </section>

      {/* Genel durum: ayrı satır, tam genişlik */}
      <section
        className={`rounded-lg border px-3 py-2 shadow-sm sm:px-4 ${
          isDark ? 'border-slate-700 bg-slate-900' : 'border-gray-200/80 bg-white'
        }`}
      >
        <p className={`text-xs font-semibold uppercase tracking-wide ${isDark ? 'text-slate-400' : 'text-gray-400'}`}>
          Genel durum
        </p>
        <div
          className={`mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-sm sm:gap-x-4 ${
            isDark ? 'text-slate-300' : 'text-gray-600'
          }`}
        >
          <span className="inline-flex items-baseline gap-1">
            <span className={`font-semibold tabular-nums ${isDark ? 'text-white' : 'text-gray-900'}`}>
              {loading || !stats ? '—' : stats.newsCount}
            </span>
            <span className={isDark ? 'text-slate-400' : 'text-gray-500'}>haber</span>
          </span>
          <span className={isDark ? 'text-slate-600' : 'text-gray-300'} aria-hidden>
            ·
          </span>
          <span className="inline-flex items-baseline gap-1">
            <span className={`font-semibold tabular-nums ${isDark ? 'text-white' : 'text-gray-900'}`}>
              {loading || !stats ? '—' : stats.categoryCount}
            </span>
            <span className={isDark ? 'text-slate-400' : 'text-gray-500'}>kategori</span>
          </span>
          <span className={isDark ? 'text-slate-600' : 'text-gray-300'} aria-hidden>
            ·
          </span>
          <span className="inline-flex items-baseline gap-1">
            <span className={`font-semibold tabular-nums ${isDark ? 'text-white' : 'text-gray-900'}`}>
              {loading || !stats ? '—' : stats.sourceCount}
            </span>
            <span className={isDark ? 'text-slate-400' : 'text-gray-500'}>kaynak</span>
          </span>
          <span className={isDark ? 'text-slate-600' : 'text-gray-300'} aria-hidden>
            ·
          </span>
          <span className="inline-flex items-baseline gap-1">
            <span className={`font-semibold tabular-nums ${isDark ? 'text-white' : 'text-gray-900'}`}>
              {loading || !stats ? '—' : stats.feedbackCount}
            </span>
            <span className={isDark ? 'text-slate-400' : 'text-gray-500'}>geri bildirim</span>
          </span>
        </div>
      </section>

      <section
        className={`rounded-xl shadow p-5 ${isDark ? 'bg-slate-900 border border-slate-700' : 'bg-white'}`}
      >
        <div className="flex items-center justify-between mb-3">
          <h1 className={`text-xl font-bold ${isDark ? 'text-white' : 'text-gray-900'}`}>Son Dakika / Öne Çıkanlar</h1>
          <Link to="/news" className="text-sm text-blue-600 hover:underline">
            Tüm haberler
          </Link>
        </div>
        {loading ? (
          <p className={`text-sm ${isDark ? 'text-slate-400' : 'text-gray-500'}`}>Haberler yükleniyor...</p>
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
                  <div className={`h-full w-full ${isDark ? 'bg-slate-800' : 'bg-gray-100'}`}>
                    {item.imageUrl ? (
                      <img
                        src={item.imageUrl}
                        alt={item.title}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                        loading="lazy"
                      />
                    ) : (
                      <div
                        className={`w-full h-full grid place-items-center text-base font-medium ${
                          isDark
                            ? 'bg-gradient-to-r from-slate-800 to-slate-700 text-slate-200'
                            : 'bg-gradient-to-r from-blue-100 to-indigo-100 text-gray-700'
                        }`}
                      >
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

      <section
        className={`rounded-xl shadow p-5 ${isDark ? 'bg-slate-900 border border-slate-700' : 'bg-white'}`}
      >
        <h2 className={`text-lg font-semibold mb-4 ${isDark ? 'text-white' : 'text-gray-900'}`}>Günün Haberleri</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {cards.map((item) => (
            <Link
              key={item.id}
              to={`/news/${item.id}`}
              className={`rounded-xl overflow-hidden transition-shadow ${
                isDark
                  ? 'border border-slate-700 bg-slate-900 hover:shadow-[0_8px_24px_rgba(0,0,0,0.35)]'
                  : 'border border-gray-200 hover:shadow-md'
              }`}
            >
              <div className={`h-36 ${isDark ? 'bg-slate-800' : 'bg-gray-100'}`}>
                {item.imageUrl ? (
                  <img
                    src={item.imageUrl}
                    alt={item.title}
                    className="w-full h-full object-cover"
                    loading="lazy"
                  />
                ) : (
                  <div
                    className={`w-full h-full grid place-items-center text-sm ${
                      isDark
                        ? 'bg-gradient-to-r from-slate-800 to-slate-700 text-slate-300'
                        : 'bg-gradient-to-r from-blue-100 to-indigo-100 text-gray-600'
                    }`}
                  >
                    Haber gorseli
                  </div>
                )}
              </div>
              <div className="p-4">
                <p className={`text-xs ${isDark ? 'text-slate-400' : 'text-gray-500'}`}>{item.sourceName}</p>
                <h3 className={`mt-1 text-sm font-semibold line-clamp-2 ${isDark ? 'text-slate-100' : 'text-gray-900'}`}>
                  {item.title}
                </h3>
              </div>
            </Link>
          ))}
        </div>
      </section>
    </div>
  )
}

export default Home

