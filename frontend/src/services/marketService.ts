import { api } from './api'
import type { ApiResponse } from '../types/auth'

export interface MarketItem {
  label: string
  value: string
  change: string
  positive: boolean
}

interface CoinGeckoAsset {
  try?: number
  try_24h_change?: number
  eur?: number
  eur_24h_change?: number
}

type CoinGeckoResponse = Record<string, CoinGeckoAsset | undefined>

interface ExternalMarketResponse {
  bist100: { value: number; changePercent: number } | null
  brent: { value: number; changePercent: number } | null
}

const TRY_FORMATTER = new Intl.NumberFormat('tr-TR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

const COMPACT_TRY_FORMATTER = new Intl.NumberFormat('tr-TR', {
  minimumFractionDigits: 0,
  maximumFractionDigits: 0,
})

const USD_FORMATTER = new Intl.NumberFormat('tr-TR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

const GOLD_TROY_OUNCE_GRAMS = 31.1034768

async function fetchJson<T>(url: string): Promise<T> {
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`Piyasa API hatasi (${response.status}): ${url}`)
  }
  return response.json() as Promise<T>
}

function formatPercent(value: number): string {
  if (!Number.isFinite(value)) return '0,00%'
  const sign = value >= 0 ? '+' : ''
  return `${sign}${value.toLocaleString('tr-TR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}%`
}

function isPositiveChange(change: string): boolean {
  return !change.startsWith('-')
}

function buildItem(
  label: string,
  value: number,
  change24h: number,
  formatter: Intl.NumberFormat = TRY_FORMATTER,
): MarketItem | null {
  if (!Number.isFinite(value)) return null
  const change = formatPercent(change24h)
  return {
    label,
    value: formatter.format(value),
    change,
    positive: isPositiveChange(change),
  }
}

interface CoinGeckoItems {
  usd: MarketItem | null
  eur: MarketItem | null
  btc: MarketItem | null
  eth: MarketItem | null
  gold: MarketItem | null
}

async function fetchCoinGeckoItems(): Promise<CoinGeckoItems> {
  const data = await fetchJson<CoinGeckoResponse>(
    'https://api.coingecko.com/api/v3/simple/price?ids=tether,bitcoin,ethereum,pax-gold&vs_currencies=try,eur&include_24hr_change=true',
  )

  const tether = data.tether
  const bitcoin = data.bitcoin
  const ethereum = data.ethereum
  const paxGold = data['pax-gold']

  const usd = buildItem('DOLAR', tether?.try ?? Number.NaN, tether?.try_24h_change ?? 0)

  let eur: MarketItem | null = null
  const tetherTry = tether?.try
  const tetherEur = tether?.eur
  if (
    tetherTry !== undefined &&
    tetherEur !== undefined &&
    Number.isFinite(tetherTry) &&
    Number.isFinite(tetherEur) &&
    tetherEur > 0
  ) {
    const currentEurTry = tetherTry / tetherEur
    const tryChange = tether?.try_24h_change ?? 0
    const eurChange = tether?.eur_24h_change ?? 0
    const yesterdayTetherTry = tetherTry / (1 + tryChange / 100)
    const yesterdayTetherEur = tetherEur / (1 + eurChange / 100)
    const yesterdayEurTry = yesterdayTetherEur > 0 ? yesterdayTetherTry / yesterdayTetherEur : currentEurTry
    const eurTryChange =
      yesterdayEurTry > 0 ? ((currentEurTry - yesterdayEurTry) / yesterdayEurTry) * 100 : 0
    eur = buildItem('EURO', currentEurTry, eurTryChange)
  }

  const goldGramTry =
    paxGold?.try !== undefined && Number.isFinite(paxGold.try)
      ? paxGold.try / GOLD_TROY_OUNCE_GRAMS
      : Number.NaN

  return {
    usd,
    eur,
    btc: buildItem(
      'BITCOIN',
      bitcoin?.try ?? Number.NaN,
      bitcoin?.try_24h_change ?? 0,
      COMPACT_TRY_FORMATTER,
    ),
    eth: buildItem(
      'ETHEREUM',
      ethereum?.try ?? Number.NaN,
      ethereum?.try_24h_change ?? 0,
      COMPACT_TRY_FORMATTER,
    ),
    gold: buildItem('ALTIN/GR', goldGramTry, paxGold?.try_24h_change ?? 0),
  }
}

async function fetchExternalIndicators(): Promise<{ bist: MarketItem | null; brent: MarketItem | null }> {
  const { data } = await api.get<ApiResponse<ExternalMarketResponse>>('/market/external')
  const payload = data.data ?? { bist100: null, brent: null }

  const bist = payload.bist100
    ? buildItem('BIST 100', payload.bist100.value, payload.bist100.changePercent, COMPACT_TRY_FORMATTER)
    : null
  const brent = payload.brent
    ? buildItem('PETROL', payload.brent.value, payload.brent.changePercent, USD_FORMATTER)
    : null

  return { bist, brent }
}

export async function fetchMarketItems(): Promise<MarketItem[]> {
  const [coinResult, externalResult] = await Promise.allSettled([
    fetchCoinGeckoItems(),
    fetchExternalIndicators(),
  ])

  const items: MarketItem[] = []

  if (coinResult.status === 'fulfilled') {
    if (coinResult.value.usd) items.push(coinResult.value.usd)
    if (coinResult.value.eur) items.push(coinResult.value.eur)
    if (coinResult.value.gold) items.push(coinResult.value.gold)
    if (coinResult.value.btc) items.push(coinResult.value.btc)
    if (coinResult.value.eth) items.push(coinResult.value.eth)
  } else {
    console.warn('[marketService] CoinGecko verisi alınamadı:', coinResult.reason)
  }

  if (externalResult.status === 'fulfilled') {
    if (externalResult.value.bist) items.push(externalResult.value.bist)
    if (externalResult.value.brent) items.push(externalResult.value.brent)
  } else {
    console.warn('[marketService] BIST/Petrol verisi alınamadı:', externalResult.reason)
  }

  if (items.length === 0) {
    throw new Error('Hiçbir piyasa verisi alınamadı')
  }

  return items
}
