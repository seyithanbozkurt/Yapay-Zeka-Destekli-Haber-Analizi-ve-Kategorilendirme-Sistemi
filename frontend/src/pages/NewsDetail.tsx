import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { api } from '../services/api'
import type { ApiResponse } from '../types/auth'
import type { News } from '../types/news'
import type { NewsClassificationResult } from '../types/newsClassification'
import { fetchClassificationByNewsId } from '../services/newsClassificationService'

function NewsDetail() {
  const { id } = useParams<{ id: string }>()
  const [news, setNews] = useState<News | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [classification, setClassification] = useState<NewsClassificationResult | null>(null)
  const [classificationError, setClassificationError] = useState('')

  useEffect(() => {
    if (!id) return
    let isMounted = true

    const load = async () => {
      try {
        setLoading(true)
        setError('')
        const { data } = await api.get<ApiResponse<News>>(`/news/${id}`)
        if (!isMounted) return
        setNews(data.data ?? null)
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

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Haber Detayı</h1>
          <p className="text-sm text-gray-600 mt-1">
            Haber içeriğini ve sınıflandırma bilgilerini görüntüleyin.
          </p>
        </div>
        <Link
          to="/news"
          className="px-4 py-2 bg-gray-100 text-gray-800 rounded-lg text-sm font-medium hover:bg-gray-200 transition-colors"
        >
          ← Haberlere dön
        </Link>
      </div>

      {loading && (
        <div className="rounded-xl bg-white shadow p-6 text-sm text-gray-500">
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
          <div className="bg-white rounded-xl shadow p-6">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-xs uppercase tracking-wide text-gray-500">
                  {news.sourceName} • {news.language?.toUpperCase() ?? 'TR'}
                </p>
                <h2 className="mt-1 text-xl font-semibold text-gray-900">{news.title}</h2>
                {news.publishedAt && (
                  <p className="mt-1 text-xs text-gray-500">
                    {new Date(news.publishedAt).toLocaleString('tr-TR')}
                  </p>
                )}
              </div>
              {news.originalUrl && (
                <a
                  href={news.originalUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="text-sm text-blue-600 hover:underline"
                >
                  Orijinal habere git
                </a>
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
              <p className="mt-4 text-sm leading-relaxed text-gray-800 whitespace-pre-line">
                {news.content}
              </p>
            )}
          </div>

          <div className="bg-white rounded-xl shadow p-6">
            <h3 className="text-sm font-semibold text-gray-900">
              Yapay Zeka Sınıflandırma Sonucu
            </h3>
            {classificationError && !classification && (
              <p className="mt-2 text-sm text-gray-600">{classificationError}</p>
            )}
            {classification && (
              <div className="mt-3 space-y-2 text-sm text-gray-800">
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
        </div>
      )}
    </div>
  )
}

export default NewsDetail

