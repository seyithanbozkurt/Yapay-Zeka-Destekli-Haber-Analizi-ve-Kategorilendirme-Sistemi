import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchAllCategories } from '../services/categoryService'
import { fetchAllNews } from '../services/newsService'
import type { Category } from '../types/category'
import type { News } from '../types/news'
import { useTheme } from '../context/ThemeContext'

function Categories() {
  const { theme } = useTheme()
  const isDark = theme === 'dark'
  const [categories, setCategories] = useState<Category[]>([])
  const [news, setNews] = useState<News[]>([])
  const [selectedCategory, setSelectedCategory] = useState<string>('Tümü')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let isMounted = true

    const loadData = async () => {
      try {
        setLoading(true)
        setError('')
        const [categoryData, newsData] = await Promise.all([
          fetchAllCategories(),
          fetchAllNews(),
        ])

        if (!isMounted) return

        setCategories(categoryData.filter((c) => c.active))
        setNews(newsData)
      } catch (e) {
        if (!isMounted) return
        setError('Kategori verileri yüklenemedi. Lütfen tekrar deneyin.')
      } finally {
        if (isMounted) setLoading(false)
      }
    }

    loadData()

    return () => {
      isMounted = false
    }
  }, [])

  const filteredNews = useMemo(() => {
    if (selectedCategory === 'Tümü') return news
    return news.filter((item) => item.categoryNames?.includes(selectedCategory))
  }, [news, selectedCategory])

  const categoryButtons = useMemo(
    () => ['Tümü', ...categories.map((c) => c.name)],
    [categories],
  )

  return (
    <div className="space-y-6">
      <div className={`rounded-xl shadow p-6 ${isDark ? 'bg-slate-900 border border-slate-700' : 'bg-white'}`}>
        <h1 className={`text-xl font-bold ${isDark ? 'text-white' : 'text-gray-900'}`}>Kategoriler</h1>
        <p className={`mt-2 text-sm ${isDark ? 'text-slate-300' : 'text-gray-600'}`}>
          Kategori seçerek ilgili haberleri listeleyebilirsiniz.
        </p>
      </div>

      {error && (
        <div className="rounded-lg bg-red-50 text-red-700 px-4 py-3 text-sm">
          {error}
        </div>
      )}

      <div className={`rounded-xl shadow p-4 ${isDark ? 'bg-slate-900 border border-slate-700' : 'bg-white'}`}>
        <p className={`text-sm font-medium mb-3 ${isDark ? 'text-slate-200' : 'text-gray-700'}`}>Kategori Seç</p>
        <div className="flex flex-wrap gap-2">
          {categoryButtons.map((categoryName) => (
            <button
              key={categoryName}
              onClick={() => setSelectedCategory(categoryName)}
              className={`px-3 py-2 rounded-lg text-sm font-medium border transition-colors ${
                selectedCategory === categoryName
                  ? 'bg-blue-100 text-blue-700 border-blue-300'
                  : isDark
                    ? 'bg-slate-900 text-slate-200 border-slate-700 hover:bg-slate-800'
                    : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-100'
              }`}
            >
              {categoryName}
            </button>
          ))}
        </div>
      </div>

      <div className={`rounded-xl shadow p-5 ${isDark ? 'bg-slate-900 border border-slate-700' : 'bg-white'}`}>
        <div className="flex items-center justify-between mb-4">
          <h2 className={`text-lg font-semibold ${isDark ? 'text-white' : 'text-gray-900'}`}>
            {selectedCategory === 'Tümü'
              ? 'Tüm Haberler'
              : `${selectedCategory} Haberleri`}
          </h2>
          <span className={`text-sm ${isDark ? 'text-slate-400' : 'text-gray-500'}`}>
            {loading ? 'Yükleniyor...' : `${filteredNews.length} haber`}
          </span>
        </div>

        {loading ? (
          <p className={`text-sm ${isDark ? 'text-slate-400' : 'text-gray-500'}`}>Haberler yükleniyor...</p>
        ) : filteredNews.length === 0 ? (
          <p className={`text-sm ${isDark ? 'text-slate-400' : 'text-gray-500'}`}>
            Seçilen kategori için haber bulunamadı.
          </p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
            {filteredNews.map((item) => (
              <Link
                key={item.id}
                to={`/news/${item.id}`}
                className={`rounded-xl overflow-hidden transition-shadow ${
                  isDark
                    ? 'border border-slate-700 bg-slate-900 hover:shadow-[0_8px_24px_rgba(0,0,0,0.35)]'
                    : 'border border-gray-200 hover:shadow-md'
                }`}
              >
                <div className={`h-32 ${isDark ? 'bg-slate-800' : 'bg-gray-100'}`}>
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
                  <p className={`mt-2 text-xs line-clamp-2 ${isDark ? 'text-slate-400' : 'text-gray-500'}`}>
                    {item.content || 'İçerik bulunamadı.'}
                  </p>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default Categories

