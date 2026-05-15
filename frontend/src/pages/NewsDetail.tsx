import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useParams, Link } from 'react-router-dom'
import { api } from '../services/api'
import type { ApiResponse } from '../types/auth'
import type { News } from '../types/news'
import type { NewsClassificationResult } from '../types/newsClassification'
import { fetchClassificationByNewsId } from '../services/newsClassificationService'
import { createFeedback } from '../services/feedbackService'
import type { FeedbackType } from '../types/feedback'
import { ensureLegacyNewsActivityMigrated } from '../services/legacyNewsActivityMigration'
import {
  getSavedNewsStatus,
  markNewsAsRead,
  toggleSavedNews as toggleSavedNewsApi,
} from '../services/userNewsActivityService'
import { useTheme } from '../context/ThemeContext'

interface CategoryItem {
  id: number
  name: string
}

function NewsDetail() {
  const { theme } = useTheme()
  const isDark = theme === 'dark'
  const { id } = useParams<{ id: string }>()
  const [news, setNews] = useState<News | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [classification, setClassification] = useState<NewsClassificationResult | null>(null)
  const [classificationError, setClassificationError] = useState('')
  const [categories, setCategories] = useState<CategoryItem[]>([])
  const [selectedCategoryId, setSelectedCategoryId] = useState('')
  const [feedbackType, setFeedbackType] = useState<FeedbackType>('POSITIVE')
  const [comment, setComment] = useState('')
  const [feedbackLoading, setFeedbackLoading] = useState(false)
  const [feedbackMessage, setFeedbackMessage] = useState('')
  const [feedbackError, setFeedbackError] = useState('')
  const [isSaved, setIsSaved] = useState(false)

  useEffect(() => {
    if (!id) return
    let isMounted = true

    const load = async () => {
      try {
        setLoading(true)
        setError('')
        const [{ data: newsData }, { data: categoryData }] = await Promise.all([
          api.get<ApiResponse<News>>(`/news/${id}`),
          api.get<CategoryItem[]>('/categories'),
        ])
        if (!isMounted) return
        const fetchedNews = newsData.data ?? null
        setNews(fetchedNews)
        if (fetchedNews) {
          try {
            await ensureLegacyNewsActivityMigrated()
            const [savedStatus] = await Promise.all([
              getSavedNewsStatus(fetchedNews.id),
              markNewsAsRead(fetchedNews.id),
            ])
            if (!isMounted) return
            setIsSaved(savedStatus)
          } catch {
            if (!isMounted) return
            setIsSaved(false)
          }
        }
        setCategories(Array.isArray(categoryData) ? categoryData : [])
        // Haber başarılı geldiyse sınıflandırma sonucunu da yükle
        try {
          const cls = await fetchClassificationByNewsId(id)
          if (!isMounted) return
          setClassification(cls)
          setClassificationError('')
        } catch {
          if (!isMounted) return
          setClassification(null)
          setClassificationError(
            'Sınıflandırma sonucu yüklenemedi veya bu haber için henüz oluşturulmamış.',
          )
        }
      } catch (e) {
        if (!isMounted) return
        setError('Haber detayları yüklenemedi. Lütfen daha sonra tekrar deneyin.')
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
  }, [id])

  const handleFeedbackSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (!id) return

    if (!selectedCategoryId) {
      setFeedbackError('Lütfen doğru olduğunu düşündüğünüz kategoriyi seçin.')
      return
    }

    try {
      setFeedbackLoading(true)
      setFeedbackError('')
      setFeedbackMessage('')

      await createFeedback({
        newsId: Number(id),
        userSelectedCategoryId: Number(selectedCategoryId),
        feedbackType,
        comment: comment.trim() ? comment.trim() : undefined,
      })

      setFeedbackMessage('Geri bildiriminiz başarıyla kaydedildi.')
      setComment('')
    } catch (e) {
      const message =
        typeof e === 'object' &&
        e !== null &&
        'response' in e &&
        typeof (e as { response?: { data?: { message?: string } } }).response?.data?.message ===
          'string'
          ? (e as { response?: { data?: { message?: string } } }).response!.data!.message!
          : 'Geri bildirim gönderilemedi. Lütfen tekrar deneyin.'
      setFeedbackError(message)
    } finally {
      setFeedbackLoading(false)
    }
  }

  const handleToggleSave = () => {
    if (!news) return
    void (async () => {
      try {
        const next = await toggleSavedNewsApi(news.id)
        setIsSaved(next)
      } catch {
        // Kaydetme işlemi başarısız olursa mevcut durum korunur.
      }
    })()
  }

  return (
    <div className={`min-h-screen p-6 ${isDark ? 'bg-slate-950 text-slate-100' : 'bg-gray-50'}`}>
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h1 className={`text-2xl font-bold ${isDark ? 'text-white' : 'text-gray-900'}`}>Haber Detayı</h1>
          <p className={`text-sm mt-1 ${isDark ? 'text-slate-300' : 'text-gray-600'}`}>
            Haber içeriğini ve sınıflandırma bilgilerini görüntüleyin.
          </p>
        </div>
        <Link
          to="/news"
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
            isDark ? 'bg-slate-800 text-slate-100 hover:bg-slate-700' : 'bg-gray-100 text-gray-800 hover:bg-gray-200'
          }`}
        >
          ← Haberlere dön
        </Link>
      </div>

      {loading && (
        <div className={`rounded-xl shadow p-6 text-sm ${isDark ? 'bg-slate-900 text-slate-400' : 'bg-white text-gray-500'}`}>
          Haber yükleniyor...
        </div>
      )}

      {error && !loading && (
        <div className="rounded-xl bg-red-50 text-red-700 px-4 py-3 text-sm">
          {error}
        </div>
      )}

      {!loading && news && (
        <div className="space-y-4">
          <div className={`rounded-xl shadow p-6 ${isDark ? 'bg-slate-900 border border-slate-700' : 'bg-white'}`}>
            {news.imageUrl && (
              <div className={`mb-4 rounded-xl overflow-hidden ${isDark ? 'bg-slate-800' : 'bg-gray-100'}`}>
                <img
                  src={news.imageUrl}
                  alt={news.title}
                  className="w-full h-72 object-cover"
                  loading="lazy"
                />
              </div>
            )}
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className={`text-xs uppercase tracking-wide ${isDark ? 'text-slate-400' : 'text-gray-500'}`}>
                  {news.sourceName} • {news.language?.toUpperCase() ?? 'TR'}
                </p>
                <h2 className={`mt-1 text-xl font-semibold ${isDark ? 'text-white' : 'text-gray-900'}`}>{news.title}</h2>
                {news.publishedAt && (
                  <p className={`mt-1 text-xs ${isDark ? 'text-slate-400' : 'text-gray-500'}`}>
                    {new Date(news.publishedAt).toLocaleString('tr-TR')}
                  </p>
                )}
              </div>
              {news.originalUrl && (
                <div className="flex items-center gap-3">
                  <button
                    type="button"
                    onClick={handleToggleSave}
                    className={`rounded-lg px-3 py-1.5 text-xs font-medium border transition ${
                      isSaved
                        ? isDark
                          ? 'bg-amber-900/30 text-amber-300 border-amber-700'
                          : 'bg-amber-50 text-amber-700 border-amber-300'
                        : isDark
                          ? 'bg-slate-900 text-slate-200 border-slate-600 hover:bg-slate-800'
                          : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                    }`}
                  >
                    {isSaved ? 'Kaydedildi' : 'Kaydet'}
                  </button>
                  <a
                    href={news.originalUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="text-sm text-blue-600 hover:underline"
                  >
                    Orijinal habere git
                  </a>
                </div>
              )}
            </div>

            {news.categoryNames?.length > 0 && (
              <div className="mt-3 flex flex-wrap gap-2">
                {news.categoryNames.map((c) => (
                  <span
                    key={c}
                    className="inline-flex items-center rounded-full bg-blue-50 px-3 py-1 text-xs font-medium text-blue-700"
                  >
                    {c}
                  </span>
                ))}
              </div>
            )}

            {news.content && (
              <p className={`mt-4 text-sm leading-relaxed whitespace-pre-line ${isDark ? 'text-slate-200' : 'text-gray-800'}`}>
                {news.content}
              </p>
            )}
          </div>

          <div className={`rounded-xl shadow p-6 ${isDark ? 'bg-slate-900 border border-slate-700' : 'bg-white'}`}>
            <h3 className={`text-sm font-semibold ${isDark ? 'text-slate-100' : 'text-gray-900'}`}>
              Yapay Zeka Sınıflandırma Sonucu
            </h3>
            {classificationError && !classification && (
              <p className={`mt-2 text-sm ${isDark ? 'text-slate-300' : 'text-gray-600'}`}>{classificationError}</p>
            )}
            {classification && (
              <div className={`mt-3 space-y-2 text-sm ${isDark ? 'text-slate-200' : 'text-gray-800'}`}>
                <p>
                  <span className="font-medium">Model: </span>
                  {classification.modelVersionName}
                </p>
                <p>
                  <span className="font-medium">Tahmin Edilen Kategori: </span>
                  {classification.predictedCategoryName}
                </p>
                <p>
                  <span className="font-medium">Güven Skoru: </span>
                  {Number(classification.predictionScore).toFixed(4)}
                </p>
                <p>
                  <span className="font-medium">Durum: </span>
                  {classification.active ? 'Aktif' : 'Pasif'}
                </p>
              </div>
            )}
          </div>

          <div className={`rounded-xl shadow p-6 ${isDark ? 'bg-slate-900 border border-slate-700' : 'bg-white'}`}>
            <h3 className={`text-sm font-semibold ${isDark ? 'text-slate-100' : 'text-gray-900'}`}>Geri Bildirim</h3>
            <p className={`mt-1 text-sm ${isDark ? 'text-slate-300' : 'text-gray-600'}`}>
              AI sonucunu doğru/yanlış olarak işaretleyin ve isterseniz yorum ekleyin.
            </p>

            <form className="mt-4 space-y-4" onSubmit={handleFeedbackSubmit}>
              <div>
                <p className={`text-sm font-medium mb-2 ${isDark ? 'text-slate-200' : 'text-gray-700'}`}>Geri Bildirim Tipi</p>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => setFeedbackType('POSITIVE')}
                    className={`px-3 py-2 rounded-lg text-sm font-medium border ${
                      feedbackType === 'POSITIVE'
                        ? 'bg-green-50 text-green-700 border-green-300'
                        : isDark
                          ? 'bg-slate-900 text-slate-200 border-slate-600'
                          : 'bg-white text-gray-700 border-gray-300'
                    }`}
                  >
                    Positive
                  </button>
                  <button
                    type="button"
                    onClick={() => setFeedbackType('NEGATIVE')}
                    className={`px-3 py-2 rounded-lg text-sm font-medium border ${
                      feedbackType === 'NEGATIVE'
                        ? 'bg-red-50 text-red-700 border-red-300'
                        : isDark
                          ? 'bg-slate-900 text-slate-200 border-slate-600'
                          : 'bg-white text-gray-700 border-gray-300'
                    }`}
                  >
                    Negative
                  </button>
                </div>
              </div>

              <div>
                <label htmlFor="category" className={`block text-sm font-medium mb-1 ${isDark ? 'text-slate-200' : 'text-gray-700'}`}>
                  Doğru Kategori
                </label>
                <select
                  id="category"
                  value={selectedCategoryId}
                  onChange={(e) => setSelectedCategoryId(e.target.value)}
                  className={`w-full px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none ${
                    isDark ? 'border-slate-700 bg-slate-800 text-slate-100' : 'border-gray-300 bg-white'
                  }`}
                  required
                >
                  <option value="">Kategori seçin</option>
                  {categories.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label htmlFor="comment" className={`block text-sm font-medium mb-1 ${isDark ? 'text-slate-200' : 'text-gray-700'}`}>
                  Yorum (opsiyonel)
                </label>
                <textarea
                  id="comment"
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  rows={3}
                  className={`w-full px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none ${
                    isDark ? 'border-slate-700 bg-slate-800 text-slate-100' : 'border-gray-300'
                  }`}
                  placeholder="Kısa bir açıklama yazabilirsiniz..."
                />
              </div>

              {feedbackError && (
                <p className="text-sm text-red-700 bg-red-50 px-3 py-2 rounded-lg">{feedbackError}</p>
              )}
              {feedbackMessage && (
                <p className="text-sm text-green-700 bg-green-50 px-3 py-2 rounded-lg">{feedbackMessage}</p>
              )}

              <button
                type="submit"
                disabled={feedbackLoading}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 disabled:bg-blue-400 transition-colors"
              >
                {feedbackLoading ? 'Gönderiliyor...' : 'Geri Bildirim Gönder'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

export default NewsDetail

