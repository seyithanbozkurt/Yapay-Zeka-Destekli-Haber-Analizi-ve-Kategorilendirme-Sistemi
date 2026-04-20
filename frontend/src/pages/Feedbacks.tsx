import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { fetchAllFeedbacks } from '../services/feedbackService'
import type { UserFeedbackResponse } from '../services/feedbackService'

function Feedbacks() {
  const { username } = useAuth()
  const [feedbacks, setFeedbacks] = useState<UserFeedbackResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let isMounted = true

    const load = async () => {
      try {
        setLoading(true)
        setError('')
        const data = await fetchAllFeedbacks()
        if (!isMounted) return
        setFeedbacks(data)
      } catch {
        if (!isMounted) return
        setError('Geri bildirimler yüklenemedi. Lütfen tekrar deneyin.')
      } finally {
        if (isMounted) setLoading(false)
      }
    }

    load()

    return () => {
      isMounted = false
    }
  }, [])

  const myFeedbacks = useMemo(
    () => feedbacks.filter((item) => item.username === username),
    [feedbacks, username],
  )

  return (
    <div className="space-y-4">
      <div className="bg-white rounded-xl shadow p-6">
        <h1 className="text-xl font-bold text-gray-900">Geri Bildirimlerim</h1>
        <p className="mt-2 text-sm text-gray-600">
          Bu alanda daha önce gönderdiğiniz geri bildirimleri görüntüleyebilirsiniz.
        </p>
      </div>

      {error && (
        <div className="rounded-lg bg-red-50 text-red-700 px-4 py-3 text-sm">
          {error}
        </div>
      )}

      <div className="bg-white rounded-xl shadow overflow-hidden">
        <div className="px-4 py-3 border-b border-gray-200 text-sm text-gray-600">
          {loading ? 'Yükleniyor...' : `Toplam ${myFeedbacks.length} geri bildirim`}
        </div>

        {loading ? (
          <p className="px-4 py-6 text-sm text-gray-500">Geri bildirimler yükleniyor...</p>
        ) : myFeedbacks.length === 0 ? (
          <p className="px-4 py-6 text-sm text-gray-500">
            Henüz geri bildirim göndermediniz.
          </p>
        ) : (
          <div className="divide-y divide-gray-200">
            {myFeedbacks.map((item) => (
              <div key={item.id} className="p-4 space-y-2">
                <p className="text-sm font-semibold text-gray-900">{item.newsTitle}</p>
                <div className="flex flex-wrap gap-2 text-xs">
                  <span
                    className={`px-2 py-1 rounded-full font-medium ${
                      item.feedbackType === 'POSITIVE'
                        ? 'bg-green-50 text-green-700'
                        : 'bg-red-50 text-red-700'
                    }`}
                  >
                    {item.feedbackType}
                  </span>
                  <span className="px-2 py-1 rounded-full bg-blue-50 text-blue-700">
                    Secilen: {item.userSelectedCategoryName}
                  </span>
                  {item.currentPredictedCategoryName && (
                    <span className="px-2 py-1 rounded-full bg-gray-100 text-gray-700">
                      AI: {item.currentPredictedCategoryName}
                    </span>
                  )}
                </div>
                {item.comment && (
                  <p className="text-sm text-gray-700">
                    <span className="font-medium">Yorum:</span> {item.comment}
                  </p>
                )}
                {item.modelVersionName && (
                  <p className="text-xs text-gray-500">Model: {item.modelVersionName}</p>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default Feedbacks

