import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { fetchAllNews } from '../services/newsService'
import type { News } from '../types/news'

function NewsList() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const [news, setNews] = useState<News[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [sourceFilter, setSourceFilter] = useState('')
  const [categoryFilter, setCategoryFilter] = useState(searchParams.get('category') ?? '')

  useEffect(() => {
    let isMounted = true

    const load = async () => {
      try {
        setLoading(true)
        setError('')
        const data = await fetchAllNews()
        if (!isMounted) return
        setNews(data)
      } catch (e) {
        if (!isMounted) return
        setError('Haberler yüklenemedi. Lütfen daha sonra tekrar deneyin.')
      } finally {
        if (isMounted) {
          setLoading(false)
        }
      }
    }

    load()

    return () => {
      isMounted = false
    }
  }, [])

  useEffect(() => {
    setCategoryFilter(searchParams.get('category') ?? '')
  }, [searchParams])

  const sources = useMemo(
    () => Array.from(new Set(news.map((n) => n.sourceName))).sort(),
    [news],
  )

  const categories = useMemo(
    () =>
      Array.from(
        new Set(
          news.flatMap((n) => (Array.isArray(n.categoryNames) ? n.categoryNames : [])),
        ),
      ).sort(),
    [news],
  )

  const filteredNews = useMemo(
    () =>
      news.filter((n) => {
        const matchesSearch =
          !search ||
          n.title.toLowerCase().includes(search.toLowerCase()) ||
          n.content?.toLowerCase().includes(search.toLowerCase())
        const matchesSource = !sourceFilter || n.sourceName === sourceFilter
        const matchesCategory =
          !categoryFilter || (Array.isArray(n.categoryNames) && n.categoryNames.includes(categoryFilter))
        return matchesSearch && matchesSource && matchesCategory
      }),
    [news, search, sourceFilter, categoryFilter],
  )

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Haberler</h1>
        <p className="text-sm text-gray-600 mt-1">
          Sistemde bulunan haberleri listeleyin, arayın ve filtreleyin.
        </p>
      </div>

      {error && (
        <div className="mb-4 rounded-lg bg-red-50 text-red-700 px-4 py-3 text-sm">
          {error}
        </div>
      )}

      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex-1">
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Başlık veya içerikte ara..."
            className="w-full max-w-md px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition bg-white"
          />
        </div>
        <div className="flex items-center gap-2">
          <label className="text-sm text-gray-600" htmlFor="sourceFilter">
            Kaynak:
          </label>
          <select
            id="sourceFilter"
            value={sourceFilter}
            onChange={(e) => setSourceFilter(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-lg bg-white text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
          >
            <option value="">Tümü</option>
            {sources.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
        <div className="flex items-center gap-2">
          <label className="text-sm text-gray-600" htmlFor="categoryFilter">
            Kategori:
          </label>
          <select
            id="categoryFilter"
            value={categoryFilter}
            onChange={(e) => {
              const value = e.target.value
              setCategoryFilter(value)
              if (value) {
                setSearchParams({ category: value })
              } else {
                setSearchParams({})
              }
            }}
            className="px-3 py-2 border border-gray-300 rounded-lg bg-white text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
          >
            <option value="">Tümü</option>
            {categories.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Gorsel
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Başlık
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Kaynak
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Kategoriler
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Tarih
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {loading ? (
              <tr>
                <td colSpan={5} className="px-4 py-6 text-center text-gray-500 text-sm">
                  Haberler yükleniyor...
                </td>
              </tr>
            ) : filteredNews.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-4 py-6 text-center text-gray-500 text-sm">
                  Gösterilecek haber bulunamadı.
                </td>
              </tr>
            ) : (
              filteredNews.map((n) => (
                <tr
                  key={n.id}
                  className="hover:bg-gray-50 cursor-pointer"
                  onClick={() => navigate(`/news/${n.id}`)}
                >
                  <td className="px-4 py-3">
                    <div className="h-14 w-20 rounded-md overflow-hidden bg-gray-100">
                      {n.imageUrl ? (
                        <img
                          src={n.imageUrl}
                          alt={n.title}
                          className="w-full h-full object-cover"
                          loading="lazy"
                        />
                      ) : (
                        <div className="w-full h-full bg-gradient-to-r from-blue-100 to-indigo-100" />
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-900">
                    <div className="font-medium line-clamp-2">{n.title}</div>
                    {n.originalUrl && (
                      <a
                        href={n.originalUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="text-xs text-blue-600 hover:underline"
                      >
                        Habere git
                      </a>
                    )}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-700">{n.sourceName}</td>
                  <td className="px-4 py-3 text-xs text-gray-700">
                    {n.categoryNames?.length
                      ? n.categoryNames.join(', ')
                      : 'Kategori yok'}
                  </td>
                  <td className="px-4 py-3 text-xs text-gray-500">
                    {n.publishedAt
                      ? new Date(n.publishedAt).toLocaleString('tr-TR')
                      : '-'}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default NewsList

