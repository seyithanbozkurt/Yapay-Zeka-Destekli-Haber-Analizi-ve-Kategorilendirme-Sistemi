import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { fetchNewsPage } from '../services/newsService'
import type { News } from '../types/news'
import { fetchAllCategories } from '../services/categoryService'
import { api } from '../services/api'
import { useTheme } from '../context/ThemeContext'

interface SourceItem {
  id: number
  name: string
}

function NewsList() {
  const { theme } = useTheme()
  const isDark = theme === 'dark'
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const [sources, setSources] = useState<string[]>([])
  const [categories, setCategories] = useState<string[]>([])
  const [news, setNews] = useState<News[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [page, setPage] = useState(Math.max(Number(searchParams.get('page') ?? '1') - 1, 0))
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const PAGE_SIZE = 20
  const [jumpPage, setJumpPage] = useState('')
  const [search, setSearch] = useState(searchParams.get('search') ?? '')
  const [sourceFilter, setSourceFilter] = useState(searchParams.get('source') ?? '')
  const [categoryFilter, setCategoryFilter] = useState(searchParams.get('category') ?? '')

  useEffect(() => {
    const nextPage = Math.max(Number(searchParams.get('page') ?? '1') - 1, 0)
    const nextSearch = searchParams.get('search') ?? ''
    const nextSource = searchParams.get('source') ?? ''
    const nextCategory = searchParams.get('category') ?? ''

    setPage((prev) => (prev === nextPage ? prev : nextPage))
    setSearch((prev) => (prev === nextSearch ? prev : nextSearch))
    setSourceFilter((prev) => (prev === nextSource ? prev : nextSource))
    setCategoryFilter((prev) => (prev === nextCategory ? prev : nextCategory))
  }, [searchParams])

  const updateQueryParams = (next: {
    page?: number
    search?: string
    source?: string
    category?: string
  }) => {
    const nextPage = next.page ?? page
    const nextSearch = next.search ?? search
    const nextSource = next.source ?? sourceFilter
    const nextCategory = next.category ?? categoryFilter
    const params = new URLSearchParams()

    params.set('page', String(nextPage + 1))
    if (nextSearch.trim()) params.set('search', nextSearch.trim())
    if (nextSource) params.set('source', nextSource)
    if (nextCategory) params.set('category', nextCategory)

    setSearchParams(params, { replace: true })
  }

  useEffect(() => {
    let isMounted = true
    const loadFilterData = async () => {
      try {
        const [sourceRes, categoryRes] = await Promise.all([api.get<SourceItem[]>('/sources'), fetchAllCategories()])
        if (!isMounted) return
        setSources(Array.isArray(sourceRes.data) ? sourceRes.data.map((s) => s.name) : [])
        setCategories(Array.isArray(categoryRes) ? categoryRes.map((c) => c.name) : [])
      } catch {
        if (!isMounted) return
        setSources([])
        setCategories([])
      }
    }
    loadFilterData()
    return () => {
      isMounted = false
    }
  }, [])

  useEffect(() => {
    let isMounted = true

    const load = async () => {
      try {
        setLoading(true)
        setError('')
        const pageData = await fetchNewsPage(page, PAGE_SIZE, {
          search,
          sourceName: sourceFilter,
          categoryName: categoryFilter,
        })
        if (!isMounted) return
        setNews(pageData.content ?? [])
        setTotalPages(pageData.totalPages ?? 0)
        setTotalElements(pageData.totalElements ?? 0)
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
  }, [page, search, sourceFilter, categoryFilter])

  const pageNumbers = useMemo(
    () => Array.from({ length: totalPages }, (_, i) => i + 1),
    [totalPages],
  )

  const visiblePageButtons = useMemo(
    () =>
      pageNumbers.slice(
        Math.max(page + 1 - 2, 1) - 1,
        Math.min(page + 1 + 2, totalPages),
      ),
    [pageNumbers, page, totalPages],
  )

  return (
    <div className={`min-h-screen p-6 ${isDark ? 'bg-slate-950 text-slate-100' : 'bg-gray-50'}`}>
      <div className="mb-6">
        <h1 className={`text-2xl font-bold ${isDark ? 'text-white' : 'text-gray-900'}`}>Haberler</h1>
        <p className={`text-sm mt-1 ${isDark ? 'text-slate-300' : 'text-gray-600'}`}>
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
            onChange={(e) => {
              const value = e.target.value
              setSearch(value)
              setPage(0)
              updateQueryParams({ search: value, page: 0 })
            }}
            placeholder="Başlık veya içerikte ara..."
            className={`w-full max-w-md px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition ${
              isDark ? 'border-slate-700 bg-slate-900 text-slate-100' : 'border-gray-300 bg-white'
            }`}
          />
        </div>
        <div className="flex items-center gap-2">
          <label className={`text-sm ${isDark ? 'text-slate-300' : 'text-gray-600'}`} htmlFor="sourceFilter">
            Kaynak:
          </label>
          <select
            id="sourceFilter"
            value={sourceFilter}
            onChange={(e) => {
              const value = e.target.value
              setSourceFilter(value)
              setPage(0)
              updateQueryParams({ source: value, page: 0 })
            }}
            className={`px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none ${
              isDark ? 'border-slate-700 bg-slate-900 text-slate-100' : 'border-gray-300 bg-white'
            }`}
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
          <label className={`text-sm ${isDark ? 'text-slate-300' : 'text-gray-600'}`} htmlFor="categoryFilter">
            Kategori:
          </label>
          <select
            id="categoryFilter"
            value={categoryFilter}
            onChange={(e) => {
              const value = e.target.value
              setCategoryFilter(value)
              setPage(0)
              updateQueryParams({ category: value, page: 0 })
            }}
            className={`px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none ${
              isDark ? 'border-slate-700 bg-slate-900 text-slate-100' : 'border-gray-300 bg-white'
            }`}
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

      <div className={`rounded-xl shadow overflow-hidden ${isDark ? 'bg-slate-900 border border-slate-700' : 'bg-white'}`}>
        <table className={`min-w-full ${isDark ? 'divide-y divide-slate-700' : 'divide-y divide-gray-200'}`}>
          <thead className={isDark ? 'bg-slate-800' : 'bg-gray-50'}>
            <tr>
              <th className={`px-4 py-3 text-left text-xs font-medium uppercase tracking-wider ${isDark ? 'text-slate-300' : 'text-gray-500'}`}>
                Gorsel
              </th>
              <th className={`px-4 py-3 text-left text-xs font-medium uppercase tracking-wider ${isDark ? 'text-slate-300' : 'text-gray-500'}`}>
                Başlık
              </th>
              <th className={`px-4 py-3 text-left text-xs font-medium uppercase tracking-wider ${isDark ? 'text-slate-300' : 'text-gray-500'}`}>
                Kaynak
              </th>
              <th className={`px-4 py-3 text-left text-xs font-medium uppercase tracking-wider ${isDark ? 'text-slate-300' : 'text-gray-500'}`}>
                Kategoriler
              </th>
              <th className={`px-4 py-3 text-left text-xs font-medium uppercase tracking-wider ${isDark ? 'text-slate-300' : 'text-gray-500'}`}>
                Tarih
              </th>
            </tr>
          </thead>
          <tbody className={isDark ? 'bg-slate-900 divide-y divide-slate-700' : 'bg-white divide-y divide-gray-200'}>
            {loading ? (
              <tr>
                <td colSpan={5} className={`px-4 py-6 text-center text-sm ${isDark ? 'text-slate-400' : 'text-gray-500'}`}>
                  Haberler yükleniyor...
                </td>
              </tr>
            ) : news.length === 0 ? (
              <tr>
                <td colSpan={5} className={`px-4 py-6 text-center text-sm ${isDark ? 'text-slate-400' : 'text-gray-500'}`}>
                  Gösterilecek haber bulunamadı.
                </td>
              </tr>
            ) : (
              news.map((n) => (
                <tr
                  key={n.id}
                  className={`cursor-pointer ${isDark ? 'hover:bg-slate-800/60' : 'hover:bg-gray-50'}`}
                  onClick={() => navigate(`/news/${n.id}`)}
                >
                  <td className="px-4 py-3">
                    <div className={`h-14 w-20 rounded-md overflow-hidden ${isDark ? 'bg-slate-800' : 'bg-gray-100'}`}>
                      {n.imageUrl ? (
                        <img
                          src={n.imageUrl}
                          alt={n.title}
                          className="w-full h-full object-cover"
                          loading="lazy"
                        />
                      ) : (
                        <div className={isDark ? 'w-full h-full bg-gradient-to-r from-slate-800 to-slate-700' : 'w-full h-full bg-gradient-to-r from-blue-100 to-indigo-100'} />
                      )}
                    </div>
                  </td>
                  <td className={`px-4 py-3 text-sm ${isDark ? 'text-slate-100' : 'text-gray-900'}`}>
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
                  <td className={`px-4 py-3 text-sm ${isDark ? 'text-slate-300' : 'text-gray-700'}`}>{n.sourceName}</td>
                  <td className={`px-4 py-3 text-xs ${isDark ? 'text-slate-300' : 'text-gray-700'}`}>
                    {n.categoryNames?.length
                      ? n.categoryNames.join(', ')
                      : 'Kategori yok'}
                  </td>
                  <td className={`px-4 py-3 text-xs ${isDark ? 'text-slate-400' : 'text-gray-500'}`}>
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

      {!loading && totalPages > 1 && (
        <div className={`mt-4 rounded-xl shadow p-4 flex flex-wrap items-center justify-between gap-3 ${isDark ? 'bg-slate-900 border border-slate-700' : 'bg-white'}`}>
          <p className={`text-sm ${isDark ? 'text-slate-300' : 'text-gray-600'}`}>
            Toplam {totalElements} haber • Sayfa {page + 1} / {totalPages}
          </p>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => {
                const nextPage = Math.max(page - 1, 0)
                setPage(nextPage)
                updateQueryParams({ page: nextPage })
              }}
              disabled={page === 0}
              className={`px-3 py-2 rounded-lg border text-sm disabled:opacity-50 disabled:cursor-not-allowed ${
                isDark ? 'border-slate-700 text-slate-200 hover:bg-slate-800' : 'border-gray-300 hover:bg-gray-100'
              }`}
            >
              Onceki
            </button>

            {visiblePageButtons.map((pageNumber) => (
                <button
                  key={pageNumber}
                  type="button"
                  onClick={() => {
                    const nextPage = pageNumber - 1
                    setPage(nextPage)
                    updateQueryParams({ page: nextPage })
                  }}
                  className={`px-3 py-2 rounded-lg border text-sm ${
                    pageNumber - 1 === page
                      ? 'bg-blue-600 border-blue-600 text-white'
                      : isDark
                        ? 'border-slate-700 text-slate-200 hover:bg-slate-800'
                        : 'border-gray-300 text-gray-700 hover:bg-gray-100'
                  }`}
                >
                  {pageNumber}
                </button>
              ))}

            <button
              type="button"
              onClick={() => {
                const nextPage = Math.min(page + 1, totalPages - 1)
                setPage(nextPage)
                updateQueryParams({ page: nextPage })
              }}
              disabled={page >= totalPages - 1}
              className={`px-3 py-2 rounded-lg border text-sm disabled:opacity-50 disabled:cursor-not-allowed ${
                isDark ? 'border-slate-700 text-slate-200 hover:bg-slate-800' : 'border-gray-300 hover:bg-gray-100'
              }`}
            >
              Sonraki
            </button>

            <select
              value={page + 1}
              onChange={(e) => {
                const nextPage = Number(e.target.value) - 1
                setPage(nextPage)
                updateQueryParams({ page: nextPage })
              }}
              className={`ml-2 px-2 py-2 border rounded-lg text-sm max-h-44 overflow-y-auto ${
                isDark ? 'border-slate-700 bg-slate-900 text-slate-100' : 'border-gray-300 bg-white'
              }`}
              title="Sayfa sec"
            >
              {pageNumbers.map((p) => (
                <option key={p} value={p}>
                  Sayfa {p}
                </option>
              ))}
            </select>

            <div className="flex items-center gap-1 ml-2">
              <input
                type="number"
                min={1}
                max={Math.max(totalPages, 1)}
                value={jumpPage}
                onChange={(e) => setJumpPage(e.target.value)}
                placeholder="Git"
                className={`w-16 px-2 py-2 border rounded-lg text-sm ${
                  isDark ? 'border-slate-700 bg-slate-900 text-slate-100' : 'border-gray-300'
                }`}
              />
              <button
                type="button"
                onClick={() => {
                  const target = Number(jumpPage)
                  if (!Number.isFinite(target)) return
                  if (target < 1 || target > totalPages) return
                  const nextPage = target - 1
                  setPage(nextPage)
                  updateQueryParams({ page: nextPage })
                  setJumpPage('')
                }}
                className={`px-3 py-2 rounded-lg border text-sm ${
                  isDark ? 'border-slate-700 text-slate-200 hover:bg-slate-800' : 'border-gray-300 hover:bg-gray-100'
                }`}
              >
                Git
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default NewsList

