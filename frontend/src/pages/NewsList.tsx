import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { fetchNewsPage } from '../services/newsService'
import type { News } from '../types/news'
import { fetchAllCategories } from '../services/categoryService'
import { api } from '../services/api'

interface SourceItem {
  id: number
  name: string
}

function NewsList() {
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
            onChange={(e) => {
              const value = e.target.value
              setSearch(value)
              setPage(0)
              updateQueryParams({ search: value, page: 0 })
            }}
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
            onChange={(e) => {
              const value = e.target.value
              setSourceFilter(value)
              setPage(0)
              updateQueryParams({ source: value, page: 0 })
            }}
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
              setPage(0)
              updateQueryParams({ category: value, page: 0 })
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
            ) : news.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-4 py-6 text-center text-gray-500 text-sm">
                  Gösterilecek haber bulunamadı.
                </td>
              </tr>
            ) : (
              news.map((n) => (
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

      {!loading && totalPages > 1 && (
        <div className="mt-4 bg-white rounded-xl shadow p-4 flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm text-gray-600">
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
              className="px-3 py-2 rounded-lg border border-gray-300 text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-100"
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
              className="px-3 py-2 rounded-lg border border-gray-300 text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-100"
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
              className="ml-2 px-2 py-2 border border-gray-300 rounded-lg text-sm bg-white max-h-44 overflow-y-auto"
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
                className="w-16 px-2 py-2 border border-gray-300 rounded-lg text-sm"
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
                className="px-3 py-2 rounded-lg border border-gray-300 text-sm hover:bg-gray-100"
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

