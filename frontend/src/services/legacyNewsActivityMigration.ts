import axios from 'axios'
import {
  fetchReadHistory,
  fetchSavedNews,
  getSavedNewsStatus,
  markNewsAsRead,
  toggleSavedNews,
} from './userNewsActivityService'

/** Eski profileStorage (localStorage) ile uyumlu anahtarlar */
const LEGACY_SAVED_KEY = 'news_saved_items'
const LEGACY_HISTORY_KEY = 'news_read_history'

function parseLegacyNewsIds(raw: string | null): number[] {
  if (!raw?.trim()) return []
  try {
    const parsed = JSON.parse(raw) as unknown
    if (!Array.isArray(parsed)) return []
    const ids: number[] = []
    for (const item of parsed) {
      if (item && typeof item === 'object' && 'id' in item) {
        const id = Number((item as { id: unknown }).id)
        if (Number.isFinite(id)) ids.push(id)
      }
    }
    return [...new Set(ids)]
  } catch {
    return []
  }
}

function isSkippableMigrationError(err: unknown): boolean {
  if (!axios.isAxiosError(err)) return false
  const status = err.response?.status
  return status === 404 || status === 400
}

let migrationInFlight: Promise<void> | null = null

/**
 * Tarayıcıdaki eski kaydedilenler / okuma geçmişi kayıtlarını API ile veritabanına aktarır;
 * tamamlanınca ilgili localStorage anahtarlarını siler. Aynı anda birden fazla çağrı tek işi paylaşır.
 */
export function ensureLegacyNewsActivityMigrated(): Promise<void> {
  if (!migrationInFlight) {
    migrationInFlight = runLegacyMigration().finally(() => {
      migrationInFlight = null
    })
  }
  return migrationInFlight
}

async function runLegacyMigration(): Promise<void> {
  const savedRaw = localStorage.getItem(LEGACY_SAVED_KEY)
  const historyRaw = localStorage.getItem(LEGACY_HISTORY_KEY)
  const savedIds = parseLegacyNewsIds(savedRaw)
  const historyIds = parseLegacyNewsIds(historyRaw)

  if (savedIds.length === 0 && historyIds.length === 0) {
    if (savedRaw !== null) localStorage.removeItem(LEGACY_SAVED_KEY)
    if (historyRaw !== null) localStorage.removeItem(LEGACY_HISTORY_KEY)
    return
  }

  let blockingError = false

  try {
    const [savedNews, readHistory] = await Promise.all([fetchSavedNews(), fetchReadHistory()])
    const savedSet = new Set(savedNews.map((x) => x.newsId))
    const historySet = new Set(readHistory.map((x) => x.newsId))

    for (const newsId of savedIds) {
      if (savedSet.has(newsId)) continue
      try {
        const already = await getSavedNewsStatus(newsId)
        if (!already) {
          await toggleSavedNews(newsId)
        }
        savedSet.add(newsId)
      } catch (err) {
        if (isSkippableMigrationError(err)) continue
        blockingError = true
        break
      }
    }

    if (!blockingError) {
      for (const newsId of historyIds) {
        if (historySet.has(newsId)) continue
        try {
          await markNewsAsRead(newsId)
          historySet.add(newsId)
        } catch (err) {
          if (isSkippableMigrationError(err)) continue
          blockingError = true
          break
        }
      }
    }
  } catch {
    blockingError = true
  }

  if (!blockingError) {
    localStorage.removeItem(LEGACY_SAVED_KEY)
    localStorage.removeItem(LEGACY_HISTORY_KEY)
  }
}
