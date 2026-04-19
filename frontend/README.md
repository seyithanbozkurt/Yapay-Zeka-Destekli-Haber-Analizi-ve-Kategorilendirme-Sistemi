# Haber Analizi Sistemi - Frontend

React + TypeScript + Vite ile geliştirilmiş frontend uygulaması.

## Kurulum

```bash
cd frontend
npm install
```

## Geliştirme

```bash
npm run dev
```

Uygulama http://localhost:5173 adresinde çalışır. API istekleri otomatik olarak backend'e (http://localhost:8989) proxy edilir.

## Build

```bash
npm run build
```

## Önizleme (Production)

```bash
npm run preview
```

## Teknolojiler

- **React 19** + **TypeScript**
- **Vite 7** - Build aracı
- **Tailwind CSS 4** - Stil framework
- **React Router 7** - Sayfa yönlendirme
- **Axios** - HTTP istekleri

## Proje Yapısı

```
src/
├── main.tsx          # Giriş noktası
├── App.tsx            # Ana uygulama + Router
├── index.css          # Tailwind import
├── pages/             # Sayfa bileşenleri
│   ├── Login.tsx
│   └── Dashboard.tsx
└── services/
    └── api.ts         # Axios instance (JWT, proxy)
```

## Backend Bağlantısı

Backend'in http://localhost:8989 adresinde çalışıyor olması gerekir. Geliştirme sırasında Vite proxy kullanılır (`/api` → `http://localhost:8989/api`).
